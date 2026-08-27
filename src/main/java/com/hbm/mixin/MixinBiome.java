package com.hbm.mixin;

import com.hbm.world.biome.ModBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Restores the old crater grass colour noise for data-defined biomes.
 *
 * <p><b>Why a mixin</b> (AGENTS.md §7): vanilla exposes no loader-neutral registration hook for a custom
 * position-dependent biome grass colour. The biome JSON can supply only one fixed override (and the two
 * built-in modifiers do not use the crater colours). The loader event and access-widener rungs therefore
 * cannot reach this calculation. This is the smallest client-only injection: one return-value hook on the
 * existing coordinate-based vanilla method; no redirect or overwrite.
 */
@Mixin(Biome.class)
public class MixinBiome {

    @Inject(method = "getGrassColor", at = @At("HEAD"), cancellable = true)
    private void hbm$craterGrassColor(double x, double z, CallbackInfoReturnable<Integer> cir) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        Integer color = ModBiomes.grassColor(level.registryAccess(), (Biome) (Object) this, x, z);
        if (color != null) cir.setReturnValue(color);
    }
}
