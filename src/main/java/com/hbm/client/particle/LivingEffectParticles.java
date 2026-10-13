package com.hbm.client.particle;

import com.hbm.particle.helper.HbmEffectNT;
import dev.golem.core.data.Tags;
import dev.golem.core.render.Particles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Client halves of 1.7 {@code ClientProxy.effectNT} types {@code sweat}, {@code vomit},
 * {@code radiation}, {@code exhaust}, {@code haze} and {@code ufo}.
 *
 * <p>1.7 drew sweat and vomit with vanilla {@code EntityBlockDustFX} and forced {@code particleMaxAge} to
 * 150..199 by reflection, so the dust falls and lies on the ground for seven to ten seconds. The port builds the
 * same vanilla block-dust particle ({@code ParticleTypes.BLOCK}, the modern {@code EntityDiggingFX}: gravity 1,
 * half size, 0.6 tint), then sets the exact motion ({@code EntityBlockDustFX} overwrote the random digging motion
 * with the given one) and the lifetime, as the crucible's {@code VanillaBurst_BlockDust} does.
 */
public final class LivingEffectParticles {

    private LivingEffectParticles() {}

    public static void register() {
        HbmEffectNT.Sweat.setHandler(LivingEffectParticles::sweat);
        HbmEffectNT.Vomit.setHandler(LivingEffectParticles::vomit);
        HbmEffectNT.Radiation.setHandler(LivingEffectParticles::radiation);
        HbmEffectNT.Exhaust.setHandler(LivingEffectParticles::exhaust);
        HbmEffectNT.Haze.setHandler((level, x, y, z, data) -> {
            if (level instanceof ClientLevel client) Particles.add(new ParticleHaze(client, x, y, z));
        });
        HbmEffectNT.Ufo.setHandler(MukeParticles::spawnUfo);
    }

    /**
     * 1.7 {@code sweat}: {@code count} block-dust particles of {@code block} at rest, anywhere in the entity's
     * bounding box grown by 0.2 sideways and on top. Radiation above 900 sweats redstone, digamma soul sand, oil
     * coal and a fat pigeon wool.
     */
    private static void sweat(Level level, double x, double y, double z, CompoundTag data) {
        if (!(level instanceof ClientLevel client)) return;
        Entity entity = client.getEntity(Tags.getIntOr(data, "entity", -1));
        if (!(entity instanceof LivingEntity)) return;
        Block block = BuiltInRegistries.BLOCK.byId(Tags.getIntOr(data, "block", 0));
        BlockParticleOption option = new BlockParticleOption(ParticleTypes.BLOCK, block.defaultBlockState());
        AABB box = entity.getBoundingBox();
        RandomSource rand = client.getRandom();
        int count = Tags.getIntOr(data, "count", 0);

        for (int i = 0; i < count; i++) {
            double ix = box.minX - 0.2 + (box.maxX - box.minX + 0.4) * rand.nextDouble();
            double iy = box.minY + (box.maxY - box.minY + 0.2) * rand.nextDouble();
            double iz = box.minZ - 0.2 + (box.maxZ - box.minZ + 0.4) * rand.nextDouble();
            blockDust(option, ix, iy, iz, 0, 0, 0, 150 + rand.nextInt(50));
        }
    }

    /**
     * 1.7 {@code vomit}: {@code count / (particleSetting + 1)} particles from the mouth along the look vector.
     * {@code normal} is lime or green hardened clay (1.7 stained clay metas 5/13, the modern lime/green terracotta),
     * {@code blood} redstone block, both as 150..199-tick block dust; {@code smoke} (coughed coal) is a vanilla
     * smoke puff at a fifth of its size living 10..19 ticks.
     *
     * <p>1.7's mouth height {@code posY - getYOffset() + getEyeHeight() + (player ? 1 : 0)} is the eye height for
     * every entity: a 1.7 player's {@code posY} was already 1.62 above the feet, its {@code getYOffset()} 1.12 and its
     * {@code getEyeHeight()} 0.12. The look vector is 1.7's {@code getLookVec}, the head-yaw view vector.
     */
    private static void vomit(Level level, double x, double y, double z, CompoundTag data) {
        if (!(level instanceof ClientLevel client)) return;
        Entity entity = client.getEntity(Tags.getIntOr(data, "entity", -1));
        if (!(entity instanceof LivingEntity)) return;

        int count = Tags.getIntOr(data, "count", 0) / (Particles.quality().ordinal() + 1);
        String mode = Tags.getStringOr(data, "mode", "");
        double ix = entity.getX();
        double iy = entity.getEyeY();
        double iz = entity.getZ();
        Vec3 vec = entity.getViewVector(1.0F);
        RandomSource rand = client.getRandom();

        for (int i = 0; i < count; i++) {
            if ("normal".equals(mode)) {
                Block clay = rand.nextBoolean() ? Blocks.LIME_TERRACOTTA : Blocks.GREEN_TERRACOTTA;
                blockDust(new BlockParticleOption(ParticleTypes.BLOCK, clay.defaultBlockState()), ix, iy, iz,
                        (vec.x + rand.nextGaussian() * 0.2) * 0.2, (vec.y + rand.nextGaussian() * 0.2) * 0.2,
                        (vec.z + rand.nextGaussian() * 0.2) * 0.2, 150 + rand.nextInt(50));
            }
            if ("blood".equals(mode)) {
                blockDust(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                        ix, iy, iz,
                        (vec.x + rand.nextGaussian() * 0.2) * 0.2, (vec.y + rand.nextGaussian() * 0.2) * 0.2,
                        (vec.z + rand.nextGaussian() * 0.2) * 0.2, 150 + rand.nextInt(50));
            }
            if ("smoke".equals(mode)) {
                // EntitySmokeFX(scale 0.2): modern SmokeParticle keeps 1.7's motion (random * 0.1 + given) and its
                // 0.75 base size, so only the 0.2 scale and the forced 10..19 tick life are applied here.
                Particle smoke = create(ParticleTypes.SMOKE, ix, iy, iz,
                        (vec.x + rand.nextGaussian() * 0.1) * 0.05, (vec.y + rand.nextGaussian() * 0.1) * 0.05,
                        (vec.z + rand.nextGaussian() * 0.1) * 0.05);
                if (smoke == null) continue;
                smoke.scale(0.2F);
                smoke.setLifetime(10 + rand.nextInt(10));
            }
        }
    }

    /**
     * 1.7 {@code radiation}, run on the client for a player above 600 RAD every tick: {@code count} cyan
     * {@code EntityAuraFX} (the modern {@code ParticleTypes.MYCELIUM} town aura) scattered around the viewing player
     * and flung on a unit gaussian ({@code setVelocity} replaced the aura's slow drift; drag 0.99, no collision).
     * 1.7's client {@code player.posY} was eye level, so the scatter is centred on the eyes.
     */
    private static void radiation(Level level, double x, double y, double z, CompoundTag data) {
        if (!(level instanceof ClientLevel client)) return;
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        RandomSource rand = client.getRandom();
        int count = Tags.getIntOr(data, "count", 0);

        for (int i = 0; i < count; i++) {
            Particle flash = create(ParticleTypes.MYCELIUM,
                    player.getX() + rand.nextGaussian() * 4,
                    player.getEyeY() + rand.nextGaussian() * 2,
                    player.getZ() + rand.nextGaussian() * 4, 0, 0, 0);
            if (flash == null) continue;
            Particles.color(flash, 0F, 0.75F, 1F);
            flash.setParticleSpeed(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian());
        }
    }

    /**
     * 1.7 {@code exhaust}: {@code count} (at least one) {@code ParticleRocketFlame} puffs spread by {@code width},
     * skipped beyond 350 blocks. {@code soyuz} spreads them flat and drops them at 0.25..0.75 down; {@code meteor}
     * (the crashing hunter chopper) spreads them in all three axes at rest.
     */
    private static void exhaust(Level level, double x, double y, double z, CompoundTag data) {
        if (!(level instanceof ClientLevel client)) return;
        Player player = Minecraft.getInstance().player;
        if (player == null || player.distanceToSqr(x, y, z) > 350D * 350D) return;

        String mode = Tags.getStringOr(data, "mode", "");
        int count = Math.max(1, Tags.getIntOr(data, "count", 0));
        double width = Tags.getDoubleOr(data, "width", 0D);
        RandomSource rand = client.getRandom();

        for (int i = 0; i < count; i++) {
            if ("soyuz".equals(mode)) {
                ParticleRocketFlame fx = new ParticleRocketFlame(client,
                        x + rand.nextGaussian() * width, y, z + rand.nextGaussian() * width);
                fx.setMotion(0D, -0.75 + rand.nextDouble() * 0.5, 0D);
                Particles.add(fx);
            }
            if ("meteor".equals(mode)) {
                Particles.add(new ParticleRocketFlame(client, x + rand.nextGaussian() * width,
                        y + rand.nextGaussian() * width, z + rand.nextGaussian() * width));
            }
        }
    }

    private static void blockDust(ParticleOptions option, double x, double y, double z,
            double motionX, double motionY, double motionZ, int lifetime) {
        Particle particle = create(option, x, y, z, motionX, motionY, motionZ);
        if (particle == null) return;
        particle.setParticleSpeed(motionX, motionY, motionZ);
        particle.setLifetime(lifetime);
    }

    /** Builds and adds a vanilla particle, or null when its provider declines (air) or no level exists. */
    private static Particle create(ParticleOptions option, double x, double y, double z,
            double motionX, double motionY, double motionZ) {
        if (Minecraft.getInstance().level == null) return null;
        return Minecraft.getInstance().particleEngine.createParticle(option, x, y, z, motionX, motionY, motionZ);
    }
}
