package com.hbm.mixin;

import com.hbm.handler.ClimbableRegistry;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void hbm$climbable(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level() != null && ClimbableRegistry.isEntityOnAny(self.level(), self)) {
            cir.setReturnValue(true);
        }
    }
}
