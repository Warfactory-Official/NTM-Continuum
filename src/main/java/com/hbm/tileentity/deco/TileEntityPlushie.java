package com.hbm.tileentity.deco;

import net.minecraft.sounds.SoundEvent;
import com.hbm.blocks.generic.BlockPlushie;
import com.hbm.tileentity.ModDecoCrateBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code BlockPlushie.TileEntityPlushie}: the type, and the squish timer a client-side click sets and every
 * tick counts down (1.7 counted on both sides, but only the client ever set it).
 */
public class TileEntityPlushie extends TileEntityPressablePlushie {

    public TileEntityPlushie(BlockPos pos, BlockState state) {
        super(ModDecoCrateBlockEntities.PLUSHIE.get(), pos, state);
    }

    @Override
    protected Enum<?>[] typeValues() {
        return BlockPlushie.PlushieType.VALUES;
    }

    public BlockPlushie.PlushieType type() {
        return BlockPlushie.PlushieType.of(typeOrdinal());
    }

    @Override
    public BlockPlushie.Plushie kind() {
        return type();
    }

    @Override
    public void setKind(BlockPlushie.Plushie kind) {
        setTypeOrdinal(((BlockPlushie.PlushieType) kind).ordinal());
    }

    @Override
    public boolean isPressable() {
        return ((BlockPlushie) getBlockState().getBlock()).isPressable();
    }

    @Override
    public SoundEvent pressSound() {
        return ((BlockPlushie) getBlockState().getBlock()).pressSound();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityPlushie plushie) {
        TileEntityPressablePlushie.clientTick(level, pos, state, plushie);
    }
}
