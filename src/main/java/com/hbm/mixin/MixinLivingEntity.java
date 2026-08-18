package com.hbm.mixin;

import com.hbm.entity.EntityHbmProps;
import com.hbm.entity.IHbmLivingDuck;
import com.hbm.handler.ClimbableRegistry;
import com.hbm.handler.EntityEffectHandler;
import net.minecraft.world.entity.LivingEntity;
//? if >=26.1 {
import dev.golem.core.data.ValueDataIn;
import dev.golem.core.data.ValueDataOut;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
//?} else {
/*import dev.golem.core.data.TagDataIn;
import dev.golem.core.data.TagDataOut;
import net.minecraft.nbt.CompoundTag;
*///?}
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

    // These two take vanilla's types, NOT the DataIn/DataOut facade. A mixin injector's parameters
    // must match the target method's descriptor exactly, so the facade cannot appear here — the
    // wrapping happens inside the body instead. This is also why these two injections will need a
    // per-version guard: the target's descriptor is ValueOutput on 26.1 and CompoundTag below it.
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    //? if >=26.1 {
    private void hbm$save(ValueOutput output, CallbackInfo ci) {
        hbm$props.save(new ValueDataOut(output).child("HbmLivingProps"));
    }
    //?} else {
    /*private void hbm$save(CompoundTag tag, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        hbm$props.save(new TagDataOut(tag, self.level().registryAccess()).child("HbmLivingProps"));
    }
    *///?}

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    //? if >=26.1 {
    private void hbm$load(ValueInput input, CallbackInfo ci) {
        new ValueDataIn(input).child("HbmLivingProps").ifPresent(hbm$props::load);
    }
    //?} else {
    /*private void hbm$load(CompoundTag tag, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        new TagDataIn(tag, self.level().registryAccess()).child("HbmLivingProps").ifPresent(hbm$props::load);
    }
    *///?}

    @Inject(method = "tick", at = @At("TAIL"))
    private void hbm$tick(CallbackInfo ci) {
        EntityEffectHandler.onUpdate((LivingEntity) (Object) this);
    }

    /** Power armour's jump sound — the stand-in for 1.12's {@code LivingJumpEvent}. */
    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void hbm$jump(CallbackInfo ci) {
        if ((Object) this instanceof net.minecraft.world.entity.player.Player player) {
            com.hbm.items.gear.ArmorFSB.handleJump(player);
        }
    }
}
