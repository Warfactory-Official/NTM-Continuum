package com.hbm.client.particle;

import dev.golem.core.Ids;
import dev.golem.core.render.CompatParticle;
import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 1.7 {@code EntityCloudFX} as the 1.7 {@code ClientProxy} "vanillaExt" / "cloud" effect builds it: the vanilla cloud
 * puff (2.5x scaled, 0.75 * 2.5 particle scale, grey 0.7..1.0, life {@code (int) (8 / (r * 0.8 + 0.3)) * 2.5} ticks,
 * the {@code generic_7 .. generic_0} sprite run, motion * 0.96 a tick, pulled down onto a player within 2 blocks,
 * ground friction 0.7) and, with {@link #reveal}, the ash-glasses gas form (1.7 {@code BlockGasBase.randomDisplayTick}):
 * the gas colour plus one random 0..0.1 brightening for all three channels, a fixed 7.5 particle scale (a 0.75-block
 * half-edge; 1.7 wrote it into {@code field_70569_a} by reflection) and no motion.
 *
 * <p>A port of the 1.7 class rather than vanilla's {@code PlayerCloudParticle}, which has no physics and whose size
 * cannot be set from outside; this one keeps 1.7's {@code noClip = false}.
 */
public final class ParticleGasCloud extends CompatParticle {

    /** Ash-glasses gas puffs built on this client since start (harness query {@code hbm_dosimeter_gas}). */
    public static final AtomicLong REVEALED = new AtomicLong();

    private final TextureAtlasSprite[] sprites = new TextureAtlasSprite[8];
    /** 1.7 {@code field_70569_a}: the particle size before the age ramp, here as the quad half-edge. */
    private float baseSize;

    public ParticleGasCloud(ClientLevel level, double x, double y, double z, double mx, double my, double mz) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D, Particles.sprite(Ids.of("minecraft", "generic_7")));
        for (int i = 0; i < 8; i++) sprites[i] = Particles.sprite(Ids.of("minecraft", "generic_" + i));
        setLayer(ParticleLayer.TRANSLUCENT);
        this.xd *= 0.10000000149011612D;
        this.yd *= 0.10000000149011612D;
        this.zd *= 0.10000000149011612D;
        this.xd += mx;
        this.yd += my;
        this.zd += mz;
        float grey = 1.0F - this.random.nextFloat() * 0.3F;
        setColor(grey, grey, grey);
        // particleScale *= 0.75F; particleScale *= 2.5F (modern quadSize is 0.1 * 1.7's particleScale)
        setQuadSize(quadSize() * 0.75F * 2.5F);
        this.baseSize = quadSize();
        this.lifetime = (int) (8.0D / (this.random.nextFloat() * 0.8D + 0.3D));
        this.lifetime = (int) (this.lifetime * 2.5F);
        this.hasPhysics = true; // 1.7 noClip = false
        this.gravity = 0.0F;
        setSprite(sprites[spriteIndex()]);
    }

    /** The ash-glasses form: gas colour plus one shared 0..0.1 brightening, scale 7.5, at rest. */
    public ParticleGasCloud reveal(float r, float g, float b) {
        float rng = this.random.nextFloat() * 0.1F;
        setColor(r + rng, g + rng, b + rng);
        this.baseSize = 0.1F * 7.5F;
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        REVEALED.incrementAndGet();
        return this;
    }

    private int spriteIndex() {
        return Mth.clamp(7 - this.age * 8 / Math.max(1, this.lifetime), 0, 7);
    }

    @Override
    public float getQuadSize(float partialTick) {
        float ramp = Mth.clamp((this.age + partialTick) / this.lifetime * 32.0F, 0.0F, 1.0F);
        return this.baseSize * ramp;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            remove();
        }
        setSprite(sprites[spriteIndex()]);
        move(this.xd, this.yd, this.zd);
        this.xd *= 0.9599999785423279D;
        this.yd *= 0.9599999785423279D;
        this.zd *= 0.9599999785423279D;
        Player player = this.level.getNearestPlayer(this.x, this.y, this.z, 2.0D, false);
        if (player != null && this.y > player.getY()) {
            this.y += (player.getY() - this.y) * 0.2D;
            this.yd += (player.getDeltaMovement().y - this.yd) * 0.2D;
            setPos(this.x, this.y, this.z);
        }
        if (this.onGround) {
            this.xd *= 0.699999988079071D;
            this.zd *= 0.699999988079071D;
        }
    }
}
