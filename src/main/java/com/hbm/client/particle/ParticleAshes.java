package com.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;

/**
 * A flake of ash: dark, near-weightless, tumbling as it falls and lying flat once it lands, where it
 * occasionally lets off a wisp of smoke before fading.
 *
 * <p>Ported from 1.12.2 {@code ParticleAshes}. It is long-lived by particle standards (a full minute)
 * and fades over its last two seconds rather than winking out.
 */
public class ParticleAshes extends ParticleRotating {

    private final float baseScale;

    public ParticleAshes(ClientLevel level, double x, double y, double z, float scale) {
        super(level, x, y, z);

        this.lifetime = 1200 + this.random.nextInt(20);
        this.baseScale = scale * 0.9F + this.random.nextFloat() * 0.2F;
        this.setQuadSize(this.baseScale);

        this.gravity = 0.01F;

        float grey = this.random.nextFloat() * 0.1F + 0.1F;
        this.setColor(grey, grey, grey);
    }

    /** Ash that has come to rest lies flat rather than facing the viewer. */
    @Override
    protected boolean isFlatOnGround() {
        return this.onGround;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        this.yd -= this.gravity;

        // Airborne ash tumbles; landed ash holds whatever angle it settled at.
        if (!this.onGround) this.spinConstant(2F);

        this.xd *= 0.95D;
        this.yd *= 0.99D;
        this.zd *= 0.95D;

        boolean wasOnGround = this.onGround;
        this.move(this.xd, this.yd, this.zd);

        // On the tick it lands, snap to a random facing so a drift of ash does not settle in lockstep.
        if (!wasOnGround && this.onGround) this.roll = this.oRoll = this.random.nextFloat() * 360F;

        if (System.identityHashCode(this) % 5 == 0 && this.onGround && this.random.nextInt(15) == 0) {
            this.level.addParticle(ParticleTypes.SMOKE, this.x, this.y + 0.125, this.z, 0, 0.05, 0);
        }
    }

    @Override
    protected void updateForFrame(float partialTick) {
        float timeLeft = this.lifetime - (this.age + partialTick);
        this.setAlpha(timeLeft < 40 ? timeLeft / 40F : 1F);
    }
}
