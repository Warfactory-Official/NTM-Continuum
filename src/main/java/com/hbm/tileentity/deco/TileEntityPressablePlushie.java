package com.hbm.tileentity.deco;

import com.hbm.blocks.generic.BlockPlushie.Plushie;
import com.hbm.blocks.generic.BlockPlushie.PlushieType;
import com.hbm.tileentity.machine.TileEntityMachineEPress;
import com.hbm.tileentity.machine.TileEntityMachinePress;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Shared transient animation state of the base and addon plushies. */
public abstract class TileEntityPressablePlushie extends TileEntityTrinket {

    public int squishTimer;
    public float miseryFactor;
    public float lastMiseryFactor;

    protected TileEntityPressablePlushie(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract Plushie kind();

    public abstract void setKind(Plushie kind);

    public abstract boolean isPressable();

    public abstract SoundEvent pressSound();

    public final boolean isHundun() {
        return kind() == PlushieType.HUNDUN;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityPressablePlushie plushie) {
        if (plushie.squishTimer > 0) plushie.squishTimer--;
        plushie.lastMiseryFactor = plushie.miseryFactor;
        int progress = 0;
        if (plushie.isPressable()) {
            var below = level.getBlockEntity(pos.below());
            if (below instanceof TileEntityMachinePress press) progress = press.progress;
            else if (below instanceof TileEntityMachineEPress press) progress = press.progress;
        }
        // A removed press releases a surviving plushie instead of leaving its last compression behind.
        plushie.miseryFactor = Mth.clamp((progress - 100F) / 100F, 0F, 1F);
    }
}
