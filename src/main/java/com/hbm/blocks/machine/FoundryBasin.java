package com.hbm.blocks.machine;

import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityFoundryBasin;
import com.hbm.tileentity.machine.TileEntityFoundryCastingBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The large caster block (1.12.2 {@code foundry_basin}) — full block, takes size-1 molds, pour-only.
 * Drawn by the BER (the old {@code foundry_basin.obj}), so the block itself is invisible. */
public class FoundryBasin extends FoundryCastingBase {

    public FoundryBasin(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE; // OBJ drawn by the BER
    }

    @Override
    protected TileEntityFoundryCastingBase getTE(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileEntityFoundryBasin te ? te : null;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFoundryBasin(pos, state);
    }

}
