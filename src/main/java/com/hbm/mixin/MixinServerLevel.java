package com.hbm.mixin;

import com.hbm.handler.radiation.RadiationSystemNT;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Flushes loaded-chunk radiation into {@code RadiationSavedData} before the level serialises, so autosave,
 * {@code /save-all} and shutdown all persist current radiation. The cross-loader hook (NeoForge has
 * {@code LevelEvent.Save} but Fabric has no level-save event) — one common mixin covers both.
 */
@Mixin(ServerLevel.class)
public class MixinServerLevel {

    @Inject(method = "save(Lnet/minecraft/util/ProgressListener;ZZ)V", at = @At("HEAD"))
    private void hbm$flushRadiation(ProgressListener progress, boolean flush, boolean skipSave, CallbackInfo ci) {
        RadiationSystemNT.flushForSave((ServerLevel) (Object) this);
    }
}
