package com.hbm.mixin;

// 26.1 only: the renderer this registers, MachineItemModelRenderer, is itself a 26.1-only class (the
// special-model-renderer system does not exist below), and so is the ID_MAPPER it patches. The
// matching entry in hbm.fabric.mixins.json is filtered out per node by processResources.
//? if fabric && >=26.1 {

/*import com.hbm.client.render.MachineItemModelRenderer;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^*
 * Fabric has no API for registering custom item special-model renderers (NeoForge uses
 * {@code RegisterSpecialModelRendererEvent}). The codec id→{@code MapCodec} map lives in the private
 * static {@code SpecialModelRenderers.ID_MAPPER}, populated once in {@code bootstrap()}; we append our
 * machine renderer at the tail. This is the one place a mixin is genuinely required (no vanilla/Fabric
 * hook exists).
 ^/
@Mixin(SpecialModelRenderers.class)
public class MixinSpecialModelRenderers {

    @Shadow
    @Final
    private static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends SpecialModelRenderer.Unbaked<?>>> ID_MAPPER;

    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void hbm$registerMachineRenderer(CallbackInfo ci) {
        ID_MAPPER.put(MachineItemModelRenderer.ID, MachineItemModelRenderer.Unbaked.MAP_CODEC);
    }
}
*///?}
