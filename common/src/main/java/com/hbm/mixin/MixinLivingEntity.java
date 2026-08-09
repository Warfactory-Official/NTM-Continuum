package com.hbm.mixin;

import com.hbm.entity.EntityHbmProps;
import com.hbm.entity.IHbmLivingDuck;
import com.hbm.handler.ClimbableRegistry;
import com.hbm.handler.EntityEffectHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Common LivingEntity hooks (apply on both NeoForge and Fabric):
 *
 * <ul>
 * <li><b>Climbable</b>: makes HBM's {@link com.hbm.interfaces.IClimbable} AABBs (e.g. the fluid tank's
 * side ladder) act as ladders — the 1.12.2 Forge {@code CheckLadderEvent} has no cross-loader
 * equivalent.</li>
 * <li><b>HBM living props</b> ({@link IHbmLivingDuck} duck): every living entity carries an
 * {@link EntityHbmProps} (radiation/digamma/lung state) — the replacement for the old Forge entity
 * capability. Persisted under the {@code HbmLivingProps} child tag; ticked into
 * {@link EntityEffectHandler#onUpdate} — the stand-in for Forge's {@code LivingUpdateEvent} (Fabric
 * has no living-tick event, so a mixin is the only shared option).</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
public class MixinLivingEntity implements IHbmLivingDuck {

    @Unique
    private final EntityHbmProps hbm$props = new EntityHbmProps();

    @Override
    public EntityHbmProps hbm$getProps() {
        return hbm$props;
    }

    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void hbm$climbable(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level() != null && ClimbableRegistry.isEntityOnAny(self.level(), self)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void hbm$save(ValueOutput output, CallbackInfo ci) {
        hbm$props.save(output.child("HbmLivingProps"));
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void hbm$load(ValueInput input, CallbackInfo ci) {
        input.child("HbmLivingProps").ifPresent(hbm$props::load);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void hbm$tick(CallbackInfo ci) {
        EntityEffectHandler.onUpdate((LivingEntity) (Object) this);
    }
}
