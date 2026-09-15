package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletImpact;
import com.hbm.items.weapon.sedna.BulletImpact;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import java.util.function.BiConsumer;

/** The secret .358-00 beam ammunition definitions from original-hbm's XFactory35800. */
public final class XFactory35800 {

    public static BulletConfig p35800;
    public static BulletConfig p35800_bl;

    private XFactory35800() {}

    /** Standard beam damage is retained; the original black-fire side effect needs its absent effect API. */
    public static final BiConsumer<EntityBulletBeamBase, net.minecraft.world.phys.HitResult> LAMBDA_BLACK_IMPACT =
            BulletImpact::standardBeamHit;

    public static void init() {
        if (p35800 != null) return;
        p35800 = BulletConfigs.register("p35800", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_secret_p35_800"))
                .setArmorPiercing(0.5F).setThresholdNegation(50F)
                .setSpread(0F).setLife(3).setRenderRotations(false).setDoesPenetrate(true)
                .setBeam().setOnBeamImpact(BulletImpact::standardBeamHit));

        p35800_bl = BulletConfigs.register("p35800_bl", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_secret_p35_800_bl"))
                .setArmorPiercing(0.5F).setThresholdNegation(50F)
                .setSpread(0F).setLife(3).setRenderRotations(false).setDoesPenetrate(true)
                .setBeam().setOnBeamImpact(LAMBDA_BLACK_IMPACT));
    }

    public static BulletConfig[] all() {
        return new BulletConfig[] {p35800, p35800_bl};
    }

    /** Fires the shared hitscan entity; the black-fire add-on remains an explicit effect gap. */
    public static void fire(net.minecraft.world.item.ItemStack stack, ItemGunBaseNT.LambdaContext ctx,
            boolean black) {
        var receiver = ctx.config.getReceivers(stack)[0];
        var magazine = receiver.getMagazine(stack);
        BulletConfig config = magazine.getType(stack, ctx.inventory);
        if (config == null) return;

        ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, com.hbm.items.weapon.sedna.GunAnimation.CYCLE,
                ctx.configIndex);
        float damage = ItemGunBaseNT.getWear(stack, ctx.configIndex) >= ctx.config.getDurability(stack)
                ? 0F : receiver.getBaseDamage(stack) * Lego.getStandardWearDamage(
                        stack, ctx.config, ctx.configIndex) * config.damageMult;
        EntityBulletBeamBase beam = new EntityBulletBeamBase(ctx.entity.level(), config, damage);
        beam.thrower = ctx.entity;
        float yaw = ctx.entity.getYRot();
        float pitch = ctx.entity.getXRot();
        net.minecraft.world.phys.Vec3 offset = (ItemGunBaseNT.getIsAiming(stack)
                ? receiver.getProjectileOffsetScoped(stack) : receiver.getProjectileOffset(stack))
                .xRot(-pitch * ((float) Math.PI / 180F))
                .yRot(-yaw * ((float) Math.PI / 180F));
        beam.setPos(ctx.entity.getX() + offset.x, ctx.entity.getEyeY() + offset.y,
                ctx.entity.getZ() + offset.z);
        beam.setRotationsFromVector(new net.minecraft.world.phys.Vec3(
                -Math.sin(yaw * Math.PI / 180D) * Math.cos(pitch * Math.PI / 180D),
                -Math.sin(pitch * Math.PI / 180D),
                Math.cos(yaw * Math.PI / 180D) * Math.cos(pitch * Math.PI / 180D)));
        beam.setImpactCallback(black ? LAMBDA_BLACK_IMPACT : BulletImpact::standardBeamHit);
        ctx.entity.level().addFreshEntity(beam);
        beam.performHitscanExternal(250D);
        magazine.useUpAmmo(stack, ctx.inventory, 1);
        ItemGunBaseNT.setWear(stack, ctx.configIndex,
                Math.min(ItemGunBaseNT.getWear(stack, ctx.configIndex) + config.wear,
                        ctx.config.getDurability(stack)));
    }
}
