package com.hbm.client.particle;

import com.hbm.blocks.generic.BlockGasBase;
import com.hbm.particle.helper.HbmEffectNT;
import dev.golem.core.data.Tags;
import dev.golem.core.render.Particles;
import dev.golem.core.world.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
        MukeParticles.register();
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
        HbmEffectNT.Muke.setHandler(MukeParticles::spawnMuke);
        HbmEffectNT.TinyTot.setHandler(MukeParticles::spawnTinyTot);
        HbmEffectNT.Skeleton.setHandler((level, x, y, z, data) -> {
            if (level instanceof ClientLevel client) DisintegrationParticles.skeleton(client,
                    client.getRandom(), x, y, z, Tags.getIntOr(data, "entityID", -1),
                    Tags.getFloatOr(data, "brightness", 1F), Tags.getBooleanOr(data, "gib", false));
        });
        HbmEffectNT.Giblets.setHandler((level, x, y, z, data) -> {
            if (level instanceof ClientLevel client) DisintegrationParticles.giblets(client,
                    client.getRandom(), x, y, z, Tags.getIntOr(data, "ent", -1),
                    Tags.getIntOr(data, "gibType", 0));
        });
        HbmEffectNT.MissileContrail.setHandler(HbmParticleHandlers::spawnContrail);
        HbmEffectNT.SawmillDamage.setHandler(HbmParticleHandlers::spawnSawmillDamage);
        HbmEffectNT.Jetpack.setHandler((level, x, y, z, data) -> spawnJetpack(level, data,
                Tags.getIntOr(data, "mode", 0) == 1 ? 3 : 0));
        HbmEffectNT.Jetpack_BJ.setHandler((level, x, y, z, data) -> spawnJetpack(level, data, 1));
        HbmEffectNT.Jetpack_DNS.setHandler((level, x, y, z, data) -> spawnJetpack(level, data, 2));
        HbmEffectNT.VanillaExt_BlockDust.setHandler(HbmParticleHandlers::spawnVanillaExtBlockDust);
        // 1.7 ClientProxy vanillaExt "cloud"; BlockGasBase asks for the local player through this supplier
        HbmEffectNT.VanillaExt_Cloud.setHandler((level, x, y, z, data) -> {
            if (!(level instanceof ClientLevel clientLevel)) return;
            ParticleGasCloud cloud = new ParticleGasCloud(clientLevel, x, y, z, Tags.getDoubleOr(data, "mX", 0D),
                    Tags.getDoubleOr(data, "mY", 0D), Tags.getDoubleOr(data, "mZ", 0D));
            if (data.contains("r")) cloud.reveal(Tags.getFloatOr(data, "r", 1F), Tags.getFloatOr(data, "g", 1F),
                    Tags.getFloatOr(data, "b", 1F));
            Particles.add(cloud);
        });
        BlockGasBase.localPlayer = () -> Minecraft.getInstance().player;
        HbmEffectNT.Hadron.setHandler((level, x, y, z, data) -> {
            if (level instanceof ClientLevel clientLevel) Particles.add(new ParticleHadron(clientLevel, x, y, z));
        });
        HbmEffectNT.Bnuuy.setHandler((level, x, y, z, data) -> spawnBnuuy(level, data));
        AmatFlashParticles.register();
        HbmEffectNT.AmatFlash.setHandler((level, x, y, z, data) -> AmatFlashParticles.spawn(level, x, y, z,
                Tags.getFloatOr(data, "scale", 1F)));
        HbmEffectNT.ProperJolt.setHandler((level, x, y, z, data) -> properJolt(
                Tags.getIntOr(data, "time", 0), Tags.getIntOr(data, "maxTime", 0)));
        // 1.7 ClientProxy "deadleaf": every leaf on "all" particles, half of them on "decreased", none on "minimal"
        HbmEffectNT.DeadLeaf.setHandler((level, x, y, z, data) -> {
            if (!(level instanceof ClientLevel clientLevel)) return;
            Particles.Quality quality = Particles.quality();
            if (quality == Particles.Quality.ALL
                    || (quality == Particles.Quality.DECREASED && level.getRandom().nextBoolean())) {
                Particles.add(new ParticleDeadLeaf(clientLevel, x, y, z));
            }
        });
        LivingEffectParticles.register(); // 1.7 sweat / vomit / radiation / exhaust / haze / ufo
        HbmEffectNT.RadFog.setHandler((level, x, y, z, data) -> {
            if (level instanceof ClientLevel clientLevel) Particles.add(new ParticleRadiationFog(clientLevel, x, y, z));
        });
        HbmEffectNT.DeconAura.setHandler((level, x, y, z, data) -> {
            var particle = Minecraft.getInstance().particleEngine.createParticle(
                    ParticleTypes.MYCELIUM, x, y, z, 0, 0, 0);
            if (particle == null) return;
            float color = 0.5F + level.getRandom().nextFloat() * 0.5F;
            Particles.color(particle, 0.8F * color, 0.9F * color, color);
            particle.setParticleSpeed(0, 0.04D, 0);
        });
    }

    /**
     * 1.7 ClientProxy {@code properJolt}: {@code hurtTime = time; maxHurtTime = maxTime; attackedAtYaw = 0}. With a
     * hurt time far beyond the duration the vanilla hurt-camera tilt keeps wobbling until the time runs out (the
     * knife injector sends 10,000-20,000 ticks over a 10,000 duration, and 0/0 once the wearer is down to 2 HP).
     * {@code animateHurt(0)} is the modern setter of the hurt direction, overwritten at once.
     */
    private static void properJolt(int time, int maxTime) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        player.animateHurt(0F);
        player.hurtTime = time;
        player.hurtDuration = maxTime;
    }

    /**
     * One exhaust puff. A missile spawns these itself, client-side, so there is no packet to gate.
     * Upstream's 350-block cut-off is the gate, and it matters: an ICBM's trail is spawned every tick
     * for the whole flight, and without it a player would be building particles for a missile several
     * thousand blocks away that they cannot see.
     */
    private static void spawnContrail(Level level, double x, double y, double z,
            CompoundTag data) {
        if (!(level instanceof ClientLevel clientLevel)) return;

        Player player = Minecraft.getInstance().player;
        if (player == null || player.distanceToSqr(x, y, z) > 350D * 350D) return;

        ParticleRocketFlame flame = new ParticleRocketFlame(clientLevel, x, y, z)
                .setScale(Tags.getFloatOr(data, "scale", 1F))
                .setMotion(Tags.getDoubleOr(data, "moX", 0D),
                        Tags.getDoubleOr(data, "moY", 0D),
                        Tags.getDoubleOr(data, "moZ", 0D));

        int maxAge = Tags.getIntOr(data, "maxAge", -1);
        if (maxAge > 0) flame.setMaxAge(maxAge);
        // hbm-space's "color" key (an addon API, used by NTM: Space's DCM geyser); absent, the puff is 1.7's
        if (data.contains("color")) {
            int color = Tags.getIntOr(data, "color", 0);
            flame.setCustomColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F);
        }

        Particles.add(flame);
    }

    private static void spawnRBMKMush(Level level, double x, double y, double z, float scale) {
        if (level instanceof ClientLevel clientLevel) {
            ParticleRBMKMush.spawn(clientLevel, x, y, z, scale);
            clientLevel.addParticle(ParticleTypes.EXPLOSION_EMITTER,
                    x, y, z, 0D, 0D, 0D);
        }
    }

    private static void spawnSawmillDamage(Level level, double x, double y, double z,
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
     * 1.7 ClientProxy "bnuuy": two small smoke puffs 0.6 behind the wearer's body, 0.275 either side, drifting
     * backwards at 0.025. 1.7 raised them by 0.25 when the <i>viewing</i> player sneaked (it read {@code player},
     * not the wearer); kept. {@code EntitySmokeFX}'s scale was forced to 0.5, against a default of about 1.125.
     */
    private static void spawnBnuuy(Level level, CompoundTag data) {
        if (!(level instanceof ClientLevel clientLevel)) return;
        if (Particles.quality() == Particles.Quality.MINIMAL) return;
        Entity entity = clientLevel.getEntity(Tags.getIntOr(data, "player", -1));
        if (!(entity instanceof Player p)) return;

        float angle = (float) -Math.toRadians(p.yBodyRot);
        Vec3 vec = new Vec3(0, 0, -0.6).yRot(angle);
        Vec3 offset = new Vec3(0.275, 0, 0).yRot(angle);

        double ix = p.getX() + vec.x;
        double iy = p.getY() + p.getEyeHeight() - 1 + 0.4;
        double iz = p.getZ() + vec.z;
        var viewer = Minecraft.getInstance().player;
        if (viewer != null && viewer.isShiftKeyDown()) iy += 0.25;

        vec = vec.normalize();
        double mX = vec.x * 0.025D;
        double mZ = vec.z * 0.025D;
        for (int i = 0; i < 2; i++) {
            double side = i == 0 ? -1 : 1;
            var fx = Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.SMOKE,
                    ix + offset.x * side, iy, iz + offset.z * side, mX, 0, mZ);
            if (fx != null) fx.scale(0.5F / 1.125F);
        }
    }

    /** The server-packeted exhaust used by the non-glider jetpack families. */
    private static void spawnJetpack(Level level, CompoundTag data, int variant) {
        if (!(level instanceof ClientLevel clientLevel)) return;
        if (Particles.quality() == Particles.Quality.MINIMAL && (variant == 1 || variant == 2)) return;
        Entity entity = clientLevel.getEntity(Tags.getIntOr(data, "player", -1));
        if (!(entity instanceof Player player)) return;

        float yaw = (float) Math.toRadians(player.yBodyRot);
        Vec3 side = new Vec3((float) Math.cos(yaw), 0D, (float) Math.sin(yaw));
        Vec3 back = new Vec3((float) Math.sin(yaw), 0D, (float) -Math.cos(yaw));
        double baseY = variant == 1 ? player.getEyeY() - 0.9375D
                : variant == 2 ? player.getY() - 0.5D : player.getEyeY() - 1D;
        double backOffset = variant == 0 || variant == 3 ? 0.25D : variant == 1 ? 0.3125D : 0D;
        Vec3 base = player.position().add(0D, baseY - player.getY(), 0D).add(back.scale(backOffset));
        double separation = 0.125D;

        for (int sign : new int[] {-1, 1}) {
            Vec3 position = base.add(side.scale(separation * sign));
            if (variant == 0 || variant == 3) {
                Vec3 flameVelocity = variant == 0
                        ? player.getDeltaMovement().add(0D, -0.4D, 0D)
                        : player.getDeltaMovement().subtract(player.getViewVector(1F).scale(0.2D));
                Vec3 smokeVelocity = variant == 0
                        ? player.getDeltaMovement().add(0D, -0.6D, 0D)
                        : player.getDeltaMovement().subtract(player.getViewVector(1F).scale(0.3D));
                clientLevel.addParticle(ParticleTypes.FLAME, position.x, position.y,
                        position.z, flameVelocity.x, flameVelocity.y, flameVelocity.z);
                clientLevel.addParticle(ParticleTypes.SMOKE, position.x, position.y,
                        position.z, smokeVelocity.x, smokeVelocity.y, smokeVelocity.z);
            } else if (variant == 1) {
                Particles.dust(clientLevel, position.x, position.y, position.z, 0.8F, 0.5F, 1F,
                        1F, player.getDeltaMovement().x, player.getDeltaMovement().y, player.getDeltaMovement().z);
            } else {
                Particles.dust(clientLevel, position.x, position.y, position.z, 0.01F, 1F, 1F,
                        1F, player.getDeltaMovement().x, player.getDeltaMovement().y, player.getDeltaMovement().z);
            }
        }
        if (variant != 0 && variant != 3 && Particles.quality() == Particles.Quality.ALL)
            spawnKickup(clientLevel, player, base);
    }

    private static void spawnKickup(ClientLevel level, Player player, Vec3 origin) {
        Vec3 target = origin.add(0D, -10D, 0D);
        BlockHitResult hit = Compat.clip(level, origin, target,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) return;

        Vec3 location = hit.getLocation();
        double distance = origin.distanceTo(location);
        int count = Math.max(0, (int) (10D - distance));
        Vec3 velocity = new Vec3(Math.max(0D, 0.75D - distance * 0.075D), 0D, 0D);
        var state = level.getBlockState(hit.getBlockPos());
        for (int i = 0; i < count; i++) {
            velocity = velocity.yRot(level.getRandom().nextFloat() * (float) (Math.PI * 2D));
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), location.x,
                    location.y + 0.1D, location.z, velocity.x, 0.1D, velocity.z);
        }
    }

    private static void spawnVanillaExtBlockDust(Level level, double x, double y,
            double z, CompoundTag data) {
        if (!(level instanceof ClientLevel clientLevel)) return;
        var block = BuiltInRegistries.BLOCK.byId(Tags.getIntOr(data, "block", -1));
        if (block == Blocks.AIR) return;
        clientLevel.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, block.defaultBlockState()), x, y, z,
                Tags.getDoubleOr(data, "mX", 0D), Tags.getDoubleOr(data, "mY", 0D),
                Tags.getDoubleOr(data, "mZ", 0D));
    }

    /**
     * Maps upstream's creator-table key onto the effect constant.
     *
     * <p>The two spellings differ ({@code "explosionSmall"} vs {@code ExplosionSmall}) because both
     * are upstream's and both are load-bearing: the string keys appear in old call sites, the enum
     * travels on the wire. Matching case-insensitively keeps one table instead of two.
     */
    private static HbmEffectNT byCreatorKey(String key) {
        for (HbmEffectNT effect : HbmEffectNT.values()) {
            if (effect.name().equalsIgnoreCase(key)) return effect;
        }
        return null;
    }
}
