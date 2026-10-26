package com.hbm.mixin;

import com.hbm.handler.DroppedItemHooks;
import com.hbm.hazard.HazardSystem;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs dropped-item hazards after vanilla's item tick. No loader-neutral per-ItemEntity tick event exists
 * on both Fabric and NeoForge, so this is the smallest common hook and replaces the old ASM hook.
 */
@Mixin(ItemEntity.class)
public class MixinItemEntity {

    @Inject(method = "tick", at = @At("HEAD"))
    private void hbm$tickStart(CallbackInfo ci) {
        DroppedItemHooks.beforeTick((ItemEntity) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void hbm$tick(CallbackInfo ci) {
        HazardSystem.updateDroppedItem((ItemEntity) (Object) this);
    }
}
