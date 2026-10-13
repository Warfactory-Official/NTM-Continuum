package com.hbm.blocks.generic;

import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPools;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code BlockRedBrickKeyhole}, the "Strange Red Room Brick" one wall of every Red Room carries. The keyhole
 * is drawn on the face its facing names ({@code brick_red_top} if that is up or down, like the Red Room brick),
 * plain brick elsewhere; placed by hand it faces its placer (1.7's piston rule). Drops nothing.
 *
 * <p>The red key turned in it (a cracked one is used up) carves the black room behind it: a 9x5x9 box of plain
 * Red Room brick left open towards the keyhole, a pedestal in the middle with a black slab, and on each of the
 * four pedestals around it, each a coin flip, a black part. A red door replaces the keyhole and the block under it.
 */
public class BlockRedBrickKeyhole extends CompatBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public BlockRedBrickKeyhole(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        return BlockKeyhole.useKey(level, pos, player, stack, hit, true);
    }

    protected static void generateRoom(Level level, int x, int y, int z, Direction dir) {
        int size = 9;
        int height = 5;
        int width = size / 2;
        BlockState plain = BlockKeyhole.redBrickPlain();

        // Outer edges, top and bottom
        for (int i = -width; i <= width; i++) {
            BlockKeyhole.set(level, x + i, y, z + width, plain);
            BlockKeyhole.set(level, x + i, y, z - width, plain);
            BlockKeyhole.set(level, x + width, y, z + i, plain);
            BlockKeyhole.set(level, x - width, y, z + i, plain);
            BlockKeyhole.set(level, x + i, y + height - 1, z + width, plain);
            BlockKeyhole.set(level, x + i, y + height - 1, z - width, plain);
            BlockKeyhole.set(level, x + width, y + height - 1, z + i, plain);
            BlockKeyhole.set(level, x - width, y + height - 1, z + i, plain);
        }

        for (int i = 1; i <= height - 2; i++) {
            // Outer edges, sides
            BlockKeyhole.set(level, x + width, y + i, z + width, plain);
            BlockKeyhole.set(level, x + width, y + i, z - width, plain);
            BlockKeyhole.set(level, x - width, y + i, z + width, plain);
            BlockKeyhole.set(level, x - width, y + i, z - width, plain);

            // Walls: every side but the one the key was turned from (1.7 compared the face to POS_X/NEG_X/...)
            for (int j = -width + 1; j <= width - 1; j++) {
                if (dir != Direction.EAST) BlockKeyhole.set(level, x + width, y + i, z + j, plain);
                if (dir != Direction.WEST) BlockKeyhole.set(level, x - width, y + i, z + j, plain);
                if (dir != Direction.SOUTH) BlockKeyhole.set(level, x + j, y + i, z + width, plain);
                if (dir != Direction.NORTH) BlockKeyhole.set(level, x + j, y + i, z - width, plain);
            }
        }

        for (int i = -width + 1; i <= width - 1; i++) {
            for (int j = -width + 1; j <= width - 1; j++) {
                // Floor and ceiling
                BlockKeyhole.set(level, x + i, y, z + j, plain);
                BlockKeyhole.set(level, x + i, y + height - 1, z + j, plain);
                for (int k = 1; k <= height - 2; k++) BlockKeyhole.set(level, x + i, y + k, z + j, Blocks.AIR.defaultBlockState());
            }
        }

        spawnPedestalItem(level, x, y + 1, z, ItemPools.POOL_BLACK_SLAB);
        if (level.getRandom().nextBoolean()) spawnPedestalItem(level, x + 2, y + 1, z, ItemPools.POOL_BLACK_PART);
        if (level.getRandom().nextBoolean()) spawnPedestalItem(level, x - 2, y + 1, z, ItemPools.POOL_BLACK_PART);
        if (level.getRandom().nextBoolean()) spawnPedestalItem(level, x, y + 1, z + 2, ItemPools.POOL_BLACK_PART);
        if (level.getRandom().nextBoolean()) spawnPedestalItem(level, x, y + 1, z - 2, ItemPools.POOL_BLACK_PART);

        BlockKeyhole.clearDrops(level, x, y, z, size, height);
    }

    /** 1.7: a pedestal holding one draw of {@code pool}, count rolled ({@code ItemPool.getStack}). */
    public static void spawnPedestalItem(Level level, int x, int y, int z, String pool) {
        BlockPos at = new BlockPos(x, y, z);
        level.setBlock(at, ModDungeonBlocks.pedestal.get().defaultBlockState(), 3);
        if (level.getBlockEntity(at) instanceof BlockPedestal.TileEntityPedestal pedestal) {
            pedestal.setItem(ItemPool.getStack(pool, level.getRandom()));
        }
    }
}
