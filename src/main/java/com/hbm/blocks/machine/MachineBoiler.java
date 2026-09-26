package com.hbm.blocks.machine;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.api.material.MaterialStack;
import com.hbm.inventory.material.Mats;
import com.hbm.items.machine.ItemScraps;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code MachineBoiler} ({@code machine_boiler_off}, "Old Boiler"): a dead decoration block with no
 * tile, no menu and no creative tab - it survives only as a bunker worldgen prop. Only the {@code _off}
 * variant is registered upstream ({@code machine_boiler_on} is an ignored mapping), so it has no lit
 * state. Breaking it drops 3-6 steel scraps instead of itself.
 */
public final class MachineBoiler extends CompatBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public MachineBoiler(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** The old onBlockAdded facing correction against solid neighbours. */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide()) return;

        Direction facing = state.getValue(FACING);
        boolean north = Compat.isSolidRender(level.getBlockState(pos.north()), level, pos.north());
        boolean south = Compat.isSolidRender(level.getBlockState(pos.south()), level, pos.south());
        boolean west = Compat.isSolidRender(level.getBlockState(pos.west()), level, pos.west());
        boolean east = Compat.isSolidRender(level.getBlockState(pos.east()), level, pos.east());

        if (north && !south) facing = Direction.SOUTH;
        if (south && !north) facing = Direction.NORTH;
        if (west && !east) facing = Direction.EAST;
        if (east && !west) facing = Direction.WEST;

        if (facing != state.getValue(FACING)) level.setBlock(pos, state.setValue(FACING, facing), 2);
    }

    /**
     * 1.7 {@code onBlockPlacedBy} runs after {@code onBlockAdded}, so a player's facing wins over the
     * solid-neighbour correction; {@code setPlacedBy} runs after {@code onPlace} here for the same order.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer == null || level.isClientSide()) return;
        Direction facing = placer.getDirection().getOpposite();
        if (facing != state.getValue(FACING)) level.setBlock(pos, state.setValue(FACING, facing), 2);
    }

    /** 1.7 {@code getDrops}: 3 + rand(4) steel scraps. Player breaks only; the loot table is empty. */
    @Override
    protected void onPlayerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide() || playerPreventsBlockDrops(player)) return;
        int count = 3 + level.getRandom().nextInt(4);
        for (int i = 0; i < count; i++) {
            Block.popResource(level, pos, ItemScraps.create(new MaterialStack(Mats.MAT_STEEL, MaterialShapes.INGOT.quantity)));
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
