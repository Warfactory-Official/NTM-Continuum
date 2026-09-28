package com.hbm.blocks.generic;

import com.hbm.items.ModItems;
import com.hbm.tileentity.deco.TileEntityDecoBlockAltF;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code DecoBlockAlt}, of which only {@code statue_elb_f} was ever registered: the "#undef" easter-egg statue
 * holding a watch and a gun ({@link TileEntityDecoBlockAltF}, drawn by {@code RenderDecoBlockAlt}). A half-slab
 * base, full light, unbreakable; a red key (or a cracked one, which it uses up) pops it out as an item. The
 * placement metadata 2/5/3/4 for yaw quadrants 0-3 is the horizontal facing here.
 */
public class DecoBlockAlt extends CompatBlock implements TickingBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** 1.7 setBlockBounds(0, 0, 0, 1, 8 * 0.0625, 1), for both the outline and the collision box. */
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);

    public DecoBlockAlt(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDecoBlockAltF(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** 1.7 onBlockPlacedBy: i = floor(yaw * 4 / 360 + 0.5) & 3 -> metadata 2, 5, 3, 4. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        float yaw = context.getPlayer() == null ? 0F : context.getPlayer().getYRot();
        int i = Mth.floor(yaw * 4.0F / 360.0F + 0.5D) & 3;
        Direction facing = switch (i) {
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            default -> Direction.NORTH;
        };
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        boolean cracked = stack.getItem() == ModItems.key_red_cracked.get();
        if (stack.getItem() == ModItems.key_red.get() || cracked) {
            if (cracked) stack.shrink(1);
            level.destroyBlock(pos, true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        // 1.7 answered true on the client for any click and false on the server without a key.
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** Structure rotation and mirroring turn the horizontal facing, as vanilla's directional blocks do. */
    @Override
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
