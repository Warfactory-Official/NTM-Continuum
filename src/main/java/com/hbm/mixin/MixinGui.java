package com.hbm.mixin;


import com.hbm.client.NukeScreenEffects;
//? if >=1.21.1 {
import net.minecraft.client.DeltaTracker;
//?}
import net.minecraft.client.gui.Gui;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The nuclear HUD shake: wraps {@link Gui#extractRenderState} in a pose push/translate/pop so every HUD
 * element wobbles for {@link NukeScreenEffects#SHAKE_DURATION} ms after a nearby detonation. Ported from
 * 1.12.2's {@code ModEventHandlerClient} GUI-shake block (which translated the in-progress overlay pose).
 *
 * <p>26.1 built the HUD by extracting element positions through the {@link GuiGraphicsExtractor} pose, so
 * shifting that pose during extraction moves the whole HUD — the cross-loader equivalent of the old GL
 * translate. Divergence from 1.12.2: the original only shifted from the hotbar onward (a GL-state leak);
 * this shifts the entire HUD.
 */
@Mixin(Gui.class)
public class MixinGui {

    // Latched at HEAD so the RETURN pop can't desync if the shake window expires mid-frame.
    @Unique private boolean hbm$shaking;

    // The injector's parameters and target name must match the target method's descriptor exactly, so
    // both differ per version: 26.1 extracts the HUD through a GuiGraphicsExtractor, older versions
    // render it directly. The pose type differs too — a Matrix3x2fStack versus a PoseStack.
    //? if >=26.1 {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void hbm$shakePush(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        hbm$shaking = NukeScreenEffects.isShakeActive();
        if (!hbm$shaking) return;
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) NukeScreenEffects.shakeOffsetX(), (float) NukeScreenEffects.shakeOffsetY());
    }
    //?} else if >=1.21.1 {
    /*@Inject(method = "render", at = @At("HEAD"))
    private void hbm$shakePush(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        hbm$shaking = NukeScreenEffects.isShakeActive();
        if (!hbm$shaking) return;
        graphics.pose().pushPose();
        graphics.pose().translate(NukeScreenEffects.shakeOffsetX(), NukeScreenEffects.shakeOffsetY(), 0D);
    }
    *///?} else {
    /*@Inject(method = "render", at = @At("HEAD"))
    private void hbm$shakePush(GuiGraphicsExtractor graphics, float partialTick, CallbackInfo ci) {
        hbm$shaking = NukeScreenEffects.isShakeActive();
        if (!hbm$shaking) return;
        graphics.pose().pushPose();
        graphics.pose().translate(NukeScreenEffects.shakeOffsetX(), NukeScreenEffects.shakeOffsetY(), 0D);
    }
    *///?}

    //? if >=26.1 {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void hbm$shakePop(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!hbm$shaking) return;
        hbm$shaking = false;
        graphics.pose().popMatrix();
    }
    //?} else if >=1.21.1 {
    /*@Inject(method = "render", at = @At("RETURN"))
    private void hbm$shakePop(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!hbm$shaking) return;
        hbm$shaking = false;
        graphics.pose().popPose();
    }
    *///?} else {
    /*@Inject(method = "render", at = @At("RETURN"))
    private void hbm$shakePop(GuiGraphicsExtractor graphics, float partialTick, CallbackInfo ci) {
        if (!hbm$shaking) return;
        hbm$shaking = false;
        graphics.pose().popPose();
    }
    *///?}
}
