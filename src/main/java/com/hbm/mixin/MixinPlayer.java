package com.hbm.mixin;

import com.hbm.items.gear.ArmorFSB;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Power armour's hard landing: the stand-in for 1.12's {@code LivingFallEvent}.
 *
 * <p><b>Why this targets {@code Player} and not {@code LivingEntity}.</b> {@code Player.causeFallDamage}
 * <b>returns early whenever the player may fly</b>, i.e. in creative, and never calls {@code super}, so a
 * hook on {@code LivingEntity.causeFallDamage} never runs there. That early return is where Forge fires
 * {@code PlayerFlyableFallEvent}, which upstream subscribes to <i>alongside</i> {@code LivingFallEvent}.
 * Injecting at the head of the {@code Player} override covers both branches with one hook.
 *
 * <p>HEAD, because vanilla resets {@code fallDistance} immediately afterwards.
 *
 * <p>The two branches are a descriptor guard: the first parameter widened from {@code float} to
 * {@code double} at 26.1, and a mixin injector's parameters must match its target exactly. AGENTS.md §1
 * lists that as the one era-guard category that legitimately belongs in HBM.
 */
@Mixin(Player.class)
public class MixinPlayer {

    @Inject(method = "causeFallDamage", at = @At("HEAD"))
    //? if >=26.1 {
    private void hbm$fall(double fallDistance, float damageModifier,
            net.minecraft.world.damagesource.DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        ArmorFSB.handleFall((Player) (Object) this, (float) fallDistance);
    }
    //?} else {
    /*private void hbm$fall(float fallDistance, float damageModifier,
            net.minecraft.world.damagesource.DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        ArmorFSB.handleFall((Player) (Object) this, fallDistance);
    }
    *///?}
}
