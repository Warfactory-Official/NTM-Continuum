package com.hbm.blocks.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.machine.TileEntityBatteryTier;

/**
 * The legacy battery tiers ({@code machine_battery_potato} 10k, {@code machine_lithium_battery} 50M,
 * {@code machine_schrabidium_battery} 25G, {@code machine_dineutronium_battery} 1T): 1.7's
 * {@code MachineBattery(Material, maxPower)} over the ported {@link BlockMachineBattery}, with the front
 * texture on a horizontal facing (1.7 picked the side away from an adjacent solid block; this faces the placer).
 */
public class MachineBatteryTier extends BlockMachineBattery {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public final long maxPower;

    public MachineBatteryTier(Properties properties, long maxPower) {
        super(properties);
        this.maxPower = maxPower;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityBatteryTier(pos, state);
    }

    @Override
    public long maxPower() {
        return maxPower;
    }
}
