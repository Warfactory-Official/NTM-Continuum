package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IExplosionSFX;
import com.hbm.particle.helper.ParticleEffects;
import net.minecraft.world.level.Level;

/** The look a warhead wears: a knot of fireballs, sized by the caller. */
public class ExplosionEffectWeapon implements IExplosionSFX {

    private final int cloudCount;
    private final float cloudScale;
    private final float cloudSpeedMult;

    public ExplosionEffectWeapon(int cloudCount, float cloudScale, float cloudSpeedMult) {
        this.cloudCount = cloudCount;
        this.cloudScale = cloudScale;
        this.cloudSpeedMult = cloudSpeedMult;
    }

    @Override
    public void doEffect(ExplosionVNT explosion, Level world, double x, double y, double z, float size) {
        if (world.isClientSide()) return;
        ParticleEffects.explosionSmall(world, x, y, z, cloudCount, cloudScale, cloudSpeedMult);
    }
}
