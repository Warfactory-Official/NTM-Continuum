package com.hbm.mixin;

import com.hbm.handler.ClimbableRegistry;
import com.hbm.handler.EntityEffectHandler;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.main.ModEventHandlerMobs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Common LivingEntity hooks (apply on both NeoForge and Fabric), each one a 1.12.2 Forge event that has no
 * cross-loader equivalent:
 *
 * <ul>
 * <li><b>Climbable</b>: makes HBM's {@link com.hbm.interfaces.IClimbable} AABBs (e.g. the fluid tank's
 * side ladder) act as ladders, matching the 1.12.2 Forge {@code CheckLadderEvent}.</li>
 * <li><b>Living tick</b>: {@link EntityEffectHandler#onUpdate}, the stand-in for Forge's
 * {@code LivingUpdateEvent} (Fabric has no living-tick event).</li>
 * <li><b>Jump</b>: power armour's jump sound, the stand-in for {@code LivingJumpEvent}.</li>
 * </ul>
 *
 * <p>The per-entity HBM state is a Golem Core {@code EntityData}, saved through the loaders' own attachment and
 * capability storage, not through this mixin; see {@link com.hbm.capability.HbmLivingProps#DATA}.
 */
@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void hbm$climbable(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level() != null && ClimbableRegistry.isEntityOnAny(self.level(), self)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void hbm$tickStart(CallbackInfo ci) {
        com.hbm.api.environment.Environment.livingTickStart((LivingEntity) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void hbm$tick(CallbackInfo ci) {
        EntityEffectHandler.onUpdate((LivingEntity) (Object) this);
        ModEventHandlerMobs.onLivingUpdate((LivingEntity) (Object) this);
    }

    /** Power armour's jump sound, the stand-in for 1.12's {@code LivingJumpEvent}. */
    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void hbm$jump(CallbackInfo ci) {
        if ((Object) this instanceof Player player) {
            ArmorFSB.handleJump(player);
        }
    }
}
