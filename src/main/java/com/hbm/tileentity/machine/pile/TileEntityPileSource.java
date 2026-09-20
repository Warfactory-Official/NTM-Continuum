package com.hbm.tileentity.machine.pile;

import com.hbm.tileentity.ModNuclearMiscBlockEntities;
import com.hbm.blocks.ModNuclearMiscBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** The legacy pile neutron source (Ra226Be = 1 neutron per ray, bred plutonium = 2), twelve rays a tick. */
@Deprecated
public class TileEntityPileSource extends TileEntityPileBase {

    public TileEntityPileSource(BlockPos pos, BlockState state) {
        super(ModNuclearMiscBlockEntities.PILE_SOURCE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPileSource pile) {
        int n = state.getBlock() == ModNuclearMiscBlocks.BLOCK_GRAPHITE_SOURCE.get() ? 1 : 2;
        for (int i = 0; i < 12; i++) pile.castRay(n);
    }

    @Override public void writeSnapshot(CompoundTag tag) {}
    @Override public void readSnapshot(CompoundTag tag) {}
}
