package com.hbm.client.particle;

import com.hbm.particle.helper.HbmEffectNT;
import dev.golem.core.data.Tags;
import dev.golem.core.render.Particles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;

/**
 * Binds each {@link HbmEffectNT} to the code that builds its particles.
 *
 * <p>Client only, and the reason {@link HbmEffectNT} itself is free of client types: the enum is the
 * shared vocabulary both sides name effects with, and this class is the half that only a client has.
 * Called once from each loader's client entrypoint.
 */
public final class HbmParticleHandlers {

    private HbmParticleHandlers() {}

    public static void register() {
        for (var entry : ParticleCreators.particleCreators.entrySet()) {
            HbmEffectNT effect = byCreatorKey(entry.getKey());
            if (effect == null) continue;
            var creator = entry.getValue();
            effect.setHandler((level, x, y, z, data) -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (!(level instanceof ClientLevel clientLevel)) return;
                creator.makeParticle(clientLevel, minecraft.player, clientLevel.getRandom(), x, y, z, data);
            });
        }
        HbmEffectNT.RBMKMush.setHandler((level, x, y, z, data) -> spawnRBMKMush(level, x, y, z,
                Tags.getFloatOr(data, "scale", 20F)));
        HbmEffectNT.RBMKMushSmall.setHandler((level, x, y, z, data) -> spawnRBMKMush(level, x, y, z,
                Tags.getFloatOr(data, "scale", 5F)));
        HbmEffectNT.MissileContrail.setHandler(HbmParticleHandlers::spawnContrail);
        HbmEffectNT.SawmillDamage.setHandler(HbmParticleHandlers::spawnSawmillDamage);
    }

    /**
     * One exhaust puff. A missile spawns these itself, client-side, so there is no packet to gate —
     * upstream's 350-block cut-off is the gate, and it matters: an ICBM's trail is spawned every tick
     * for the whole flight, and without it a player would be building particles for a missile several
     * thousand blocks away that they cannot see.
     */
    private static void spawnContrail(net.minecraft.world.level.Level level, double x, double y, double z,
            net.minecraft.nbt.CompoundTag data) {
        if (!(level instanceof ClientLevel clientLevel)) return;

        net.minecraft.world.entity.player.Player player = Minecraft.getInstance().player;
        if (player == null || player.distanceToSqr(x, y, z) > 350D * 350D) return;

        ParticleRocketFlame flame = new ParticleRocketFlame(clientLevel, x, y, z)
                .setScale(Tags.getFloatOr(data, "scale", 1F))
                .setMotion(Tags.getDoubleOr(data, "moX", 0D),
                        Tags.getDoubleOr(data, "moY", 0D),
                        Tags.getDoubleOr(data, "moZ", 0D));

        int maxAge = Tags.getIntOr(data, "maxAge", -1);
        if (maxAge > 0) flame.setMaxAge(maxAge);

        Particles.add(flame);
    }

    private static void spawnRBMKMush(net.minecraft.world.level.Level level, double x, double y, double z, float scale) {
        if (level instanceof ClientLevel clientLevel) {
            ParticleRBMKMush.spawn(clientLevel, x, y, z, scale);
            clientLevel.addParticle(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                    x, y, z, 0D, 0D, 0D);
        }
    }

    private static void spawnSawmillDamage(net.minecraft.world.level.Level level, double x, double y, double z,
                                           CompoundTag data) {
        if (!(level instanceof ClientLevel clientLevel)) return;
        int count = Tags.getIntOr(data, "count", 0);
        var random = clientLevel.getRandom();
        for (int i = 0; i < count; i++) {
            Particles.add(new ParticleLargeBlockDebris(clientLevel, x, y, z,
                    random.nextGaussian() * 0.1D,
                    random.nextGaussian() * 0.1D + 0.2D,
                    random.nextGaussian() * 0.1D,
                    Blocks.REDSTONE_BLOCK.defaultBlockState(),
                    50 + random.nextInt(50)));
        }
    }

    /**
     * Maps upstream's creator-table key onto the effect constant.
     *
     * <p>The two spellings differ ({@code "explosionSmall"} vs {@code ExplosionSmall}) because both
     * are upstream's and both are load-bearing — the string keys appear in old call sites, the enum
     * travels on the wire. Matching case-insensitively keeps one table instead of two.
     */
    private static HbmEffectNT byCreatorKey(String key) {
        for (HbmEffectNT effect : HbmEffectNT.values()) {
            if (effect.name().equalsIgnoreCase(key)) return effect;
        }
        return null;
    }
}
