package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import java.util.function.BiConsumer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

/** The conventional 10 gauge rounds from 1.7's XFactory10ga. */
public final class XFactory10ga {

    public static BulletConfig g10;
    public static BulletConfig g10_shrapnel;
    public static BulletConfig g10_du;
    public static BulletConfig g10_slug;
    public static BulletConfig g10_explosive;

    private XFactory10ga() {}

    public static void init() {
        if (g10 != null) return;
        float spread = 0.035F;
        g10 = BulletConfigs.register("g10", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_g10"))
                .setProjectiles(10).setDamage(1F / 10F).setSpread(spread)
                .setRicochetAngle(15F).setThresholdNegation(5F));
        g10_shrapnel = BulletConfigs.register("g10_shrapnel", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_g10_shrapnel"))
                .setProjectiles(10).setDamage(1F / 10F).setSpread(spread)
                .setRicochetAngle(90F).setRicochetCount(15).setThresholdNegation(5F));
        g10_du = BulletConfigs.register("g10_du", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_g10_du"))
                .setProjectiles(10).setDamage(1F / 4F).setSpread(spread)
                .setRicochetAngle(15F).setThresholdNegation(10F)
                .setArmorPiercing(0.2F).setDoesPenetrate(true).setDamageFalloffByPen(false));
        g10_slug = BulletConfigs.register("g10_slug", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_g10_slug"))
                .setRicochetAngle(15F).setThresholdNegation(10F)
                .setArmorPiercing(0.1F).setDoesPenetrate(true));
        g10_explosive = BulletConfigs.register("g10_explosive", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_g10_explosive"))
                .setWear(3F).setProjectiles(10).setDamage(1F / 4F).setSpread(spread)
                .setOnImpact((bullet, hit) -> {
                    if (hit instanceof EntityHitResult entityHit && bullet.tickCount < 3
                            && entityHit.getEntity() == bullet.getThrower()) return;
                    Lego.tinyExplode(bullet, hit, 1.5F);
                    bullet.discard();
                }));
    }

    public static BulletConfig[] all() {
        return new BulletConfig[] {g10, g10_shrapnel, g10_du, g10_slug, g10_explosive};
    }

    /** The secondary trigger fires one barrel; primary uses the receiver's two-round cycle. */
    public static final BiConsumer<ItemStack,
            ItemGunBaseNT.LambdaContext> LAMBDA_DOUBLE_SECONDARY = (stack, ctx) -> {
        var receiver = ctx.config.getReceivers(stack)[0];
        var state = ItemGunBaseNT.getState(stack, ctx.configIndex);
        if (state == ItemGunBaseNT.GunState.IDLE) {
            if (receiver.getCanFire(stack).apply(stack, ctx)) {
                receiver.getOnFire(stack).accept(stack, ctx);
                if (receiver.getFireSound(stack) != null) {
                    ctx.entity.level().playSound(null, ctx.entity.getX(), ctx.entity.getY(), ctx.entity.getZ(),
                            receiver.getFireSound(stack), SoundSource.PLAYERS,
                            receiver.getFireVolume(stack), receiver.getFirePitch(stack));
                }
                ItemGunBaseNT.setState(stack, ctx.configIndex,
                        ItemGunBaseNT.GunState.COOLDOWN);
                ItemGunBaseNT.setTimer(stack, ctx.configIndex,
                        receiver.getDelayAfterFire(stack));
            } else if (receiver.getDoesDryFire(stack)) {
                ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack,
                        GunAnimation.CYCLE_DRY, ctx.configIndex);
                ItemGunBaseNT.setState(stack, ctx.configIndex,
                        ItemGunBaseNT.GunState.DRAWING);
                ItemGunBaseNT.setTimer(stack, ctx.configIndex,
                        receiver.getDelayAfterDryFire(stack));
            }
        }
        if (state == ItemGunBaseNT.GunState.RELOADING) {
            ItemGunBaseNT.setReloadCancel(stack, true);
        }
    };
}
