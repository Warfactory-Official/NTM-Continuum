package com.hbm.tileentity.deco;

import com.hbm.blocks.generic.BlockPlushie;
import com.hbm.tileentity.ModDecoCrateBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code BlockPlushie.TileEntityPlushie}: the type, and the squish timer a client-side click sets and every
 * tick counts down (1.7 counted on both sides, but only the client ever set it).
 */
public class TileEntityPlushie extends TileEntityTrinket {

    public int squishTimer;

    public TileEntityPlushie(BlockPos pos, BlockState state) {
        super(ModDecoCrateBlockEntities.PLUSHIE.get(), pos, state);
    }

    @Override
    protected int typeCount() {
        return BlockPlushie.PlushieType.VALUES.length;
    }

    public BlockPlushie.PlushieType type() {
        return BlockPlushie.PlushieType.of(typeOrdinal());
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityPlushie plushie) {
        if (plushie.squishTimer > 0) plushie.squishTimer--;
    }
}
