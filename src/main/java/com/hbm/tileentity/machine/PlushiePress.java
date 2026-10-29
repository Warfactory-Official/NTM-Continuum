package com.hbm.tileentity.machine;

import com.hbm.api.block.IToolable.ToolType;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.deco.TileEntityPressablePlushie;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;

/** The authored press/plushie stroke and hand-drill operations, shared by both presses. */
public final class PlushiePress {

    private PlushiePress() {
    }

    public static SoundEvent sound(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos.above()) instanceof TileEntityPressablePlushie plushie)
                || !plushie.isPressable()) return ModSounds.PRESS_OPERATE.get();
        if (plushie.isHundun()) {
            level.destroyBlock(pos, false);
            new ExplosionVNT(level, pos, 5F)
                    .setEntityProcessor(new EntityProcessorCrossSmooth(1D, 50F))
                    .setPlayerProcessor(new PlayerProcessorStandard())
                    .setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F))
                    .explode();
        }
        return plushie.pressSound();
    }

    public static boolean drill(Level level, BlockPos pos, boolean cell, ToolType tool) {
        if (tool != ToolType.HAND_DRILL || !cell) return false;
        BlockMultiblockCore.withoutTeardown(() -> level.removeBlock(pos, false));
        return true;
    }
}
