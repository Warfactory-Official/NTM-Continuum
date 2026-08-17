package com.hbm.mixin;

// 26.1 only: ItemModel.Unbaked and the ID_MAPPER this patches are both 26.1-era types. The matching
// entry in hbm.fabric.mixins.json is filtered out per node by processResources.
//? if fabric && >=26.1 {

/*import com.hbm.client.render.weapon.GunItemModel;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^*
 * Fabric has no API for registering a custom item-model type (NeoForge uses
 * {@code RegisterItemModelsEvent}). The id→{@code MapCodec} map is the private static
 * {@code ItemModels.ID_MAPPER}, populated once in {@code bootstrap()}; the gun model is appended at the
 * tail. Sibling of {@link MixinSpecialModelRenderers}, and required for the same reason: no vanilla or
 * Fabric hook exists.
 ^/
@Mixin(ItemModels.class)
public class MixinItemModels {

    @Shadow
    @Final
    private static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ItemModel.Unbaked>> ID_MAPPER;

    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void hbm$registerGunModel(CallbackInfo ci) {
        ID_MAPPER.put(dev.golem.core.Ids.of("hbm", "gun"), GunItemModel.Unbaked.MAP_CODEC);
    }
}
*///?}
