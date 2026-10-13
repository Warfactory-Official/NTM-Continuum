package com.hbm.items.weapon.sedna.factory;

import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.effect.EntityFireLingering;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.BulletImpact;
import java.util.function.BiConsumer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The secret .358-00 beam ammunition definitions from 1.7's {@code XFactory35800}. Both aberrators fire
 * through the standard {@code Lego.LAMBDA_NOWEAR_FIRE}, whose BEAM branch spawns the hitscan beam; the round alone
 * decides whether the impact is plain or black lightning.
 */
public final class XFactory35800 {

    public static BulletConfig p35800;
    public static BulletConfig p35800_bl;

    private XFactory35800() {}

    /**
     * 1.7 {@code LAMBDA_BLACK_IMPACT}: a living target gains 200 ticks of black fire, a block hit leaves a 7.5x2 black
     * lingering fire for 200 ticks, then the standard beam hit applies.
     */
    public static final BiConsumer<EntityBulletBeamBase, HitResult> LAMBDA_BLACK_IMPACT = (beam, hit) -> {
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            HbmLivingProps.addBlackFire(living, 200);
        }
        if (hit instanceof BlockHitResult && hit.getType() == HitResult.Type.BLOCK) {
            EntityFireLingering fire = new EntityFireLingering(beam.level()).setArea(7.5F, 2F).setDuration(200)
                    .setFireType(EntityFireLingering.TYPE_BLACK);
            fire.setPos(hit.getLocation());
            beam.level().addFreshEntity(fire);
        }
        BulletImpact.standardBeamHit(beam, hit);
    };

    public static void init() {
        if (p35800 != null) return;
        p35800 = BulletConfigs.register("p35800", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_secret_p35_800"))
                .setArmorPiercing(0.5F).setThresholdNegation(50F)
                .setBeam().setSpread(0F).setLife(3).setRenderRotations(false)
                .setOnBeamImpact(BulletImpact::standardBeamHit));

        p35800_bl = BulletConfigs.register("p35800_bl", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_secret_p35_800_bl"))
                .setArmorPiercing(0.5F).setThresholdNegation(50F)
                .setBeam().setSpread(0F).setLife(3).setRenderRotations(false)
                .setOnBeamImpact(LAMBDA_BLACK_IMPACT));
    }

    public static BulletConfig[] all() {
        return new BulletConfig[] {p35800, p35800_bl};
    }
}
