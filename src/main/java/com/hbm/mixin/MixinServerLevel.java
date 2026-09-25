package com.hbm.mixin;

import com.hbm.handler.radiation.RadiationSystemNT;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProgressListener;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Writes loaded-chunk radiation to its per-chunk store before the level serialises, so autosave,
 * {@code /save-all} and shutdown all persist current radiation. The cross-loader hook (NeoForge has
 * {@code LevelEvent.Save} but Fabric has no level-save event) — one common mixin covers both.
 *
 * <p>{@code flush} is passed on rather than assumed: vanilla waits for the disk only when the caller
 * asked it to, and an autosave that does not is the one that must not stall the server thread.
 */
@Mixin(ServerLevel.class)
public class MixinServerLevel {

    @Inject(method = "save(Lnet/minecraft/util/ProgressListener;ZZ)V", at = @At("HEAD"))
    private void hbm$flushRadiation(ProgressListener progress, boolean flush, boolean skipSave, CallbackInfo ci) {
        RadiationSystemNT.flushForSave((ServerLevel) (Object) this, flush);
    }

    //? if >=26.1 {
    @Inject(method = "updatePOIOnBlockStateChange", at = @At("HEAD"))
    private void hbm$radiationBlockChanged(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
        RadiationSystemNT.onBlockChanged((ServerLevel) (Object) this, pos, oldState, newState);
    }
    //?} else {
    /*@Inject(method = "onBlockStateChange", at = @At("HEAD"))
    private void hbm$radiationBlockChanged(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
        RadiationSystemNT.onBlockChanged((ServerLevel) (Object) this, pos, oldState, newState);
    }
    *///?}
}
