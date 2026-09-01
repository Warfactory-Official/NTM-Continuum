package com.hbm.blocks.machine;

import dev.golem.core.registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

/**
 * Generic horizontal-facing machine block backed by a {@link BlockEntity}. This is the scaffolding
 * base for ported HBM machines: a placeable, directional, BlockEntity-carrying block. It deliberately
 * carries no processing logic, ticker or GUI — those land per machine in a later wave (see
 * IMPLEMENTATION.md §18). Plain {@link Block} + {@link EntityBlock} (not BaseEntityBlock, which would
 * force a {@code codec()} and INVISIBLE render), mirroring {@link BlockMachineBattery}.
 *
 * <p>Each machine supplies its own BlockEntity via a {@link BlockEntities.Factory}, so no per-machine
 * block subclass is needed for the scaffold.
 */
public class BlockMachine extends dev.golem.core.world.CompatBlock implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final BlockEntities.Factory<?> factory;

    public BlockMachine(Properties properties, BlockEntities.Factory<?> factory) {
        super(properties);
        this.factory = factory;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.create(pos, state);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
}
