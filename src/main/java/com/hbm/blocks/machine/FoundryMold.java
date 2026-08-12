package com.hbm.blocks.machine;

import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityFoundryCastingBase;
import com.hbm.tileentity.machine.TileEntityFoundryMold;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** The small caster block (1.12.2 {@code foundry_mold}) — half-height, takes size-0 molds. */
public class FoundryMold extends FoundryCastingBase {

    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 0.5D, 1);

    public FoundryMold(Properties properties) {
        super(properties);
    }

    @Override
    protected TileEntityFoundryCastingBase getTE(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileEntityFoundryMold te ? te : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFoundryMold(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : FoundryChannel.createTickerHelper(type, ModBlockEntities.FOUNDRY_MOLD.get(), TileEntityFoundryCastingBase::serverTick);
    }
}
