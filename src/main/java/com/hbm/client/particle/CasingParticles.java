package com.hbm.client.particle;

import com.hbm.packet.CasingEjectPayload;
import com.hbm.packet.GunCasingEjectPayload;
import com.hbm.particle.SpentCasing;
import com.hbm.particle.helper.CasingCreator;
import dev.golem.core.data.Tags;
import dev.golem.core.client.ClientResourceReloads;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.render.WorldEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import java.util.List;
import java.util.Random;
import net.minecraft.nbt.CompoundTag;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Client lifetime owner for source spent-casing particles. */
public final class CasingParticles {
    private static final CasingParticleGroup GROUP = new CasingParticleGroup();
    /** 1.7 {@code CasingEjector.rand}: one shared generator for the motion rolls. */
    private static final Random EJECT_RANDOM = new Random();
    private static boolean initialized;
    private static ClientLevel lastLevel;

    private CasingParticles() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        WorldEffects.init();
        ClientTicks.onEndTick(CasingParticles::tick);
        WorldEffects.onBegin(camera -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) GROUP.submit(level, camera);
        });
        ClientResourceReloads.register("hbm", "casing_particles", GROUP::reload);
    }

    public static void spawn(CasingCreator.Event event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;
        SpentCasingLookup.add(level, event, GROUP);
    }

    public static void spawn(CasingEjectPayload payload) {
        spawn(new CasingCreator.Event(payload.x(), payload.y(), payload.z(), payload.mX(), payload.mY(), payload.mZ(),
                payload.yaw(), payload.pitch(), payload.mPitch(), payload.mYaw(), payload.casing(), payload.smoking(),
                payload.smokeLife(), payload.smokeLift(), payload.nodeLife(), false, false));
    }

    public static void spawn(GunCasingEjectPayload payload) {
        spawn(new CasingCreator.Event(payload.x(), payload.y(), payload.z(), payload.mX(), payload.mY(), payload.mZ(),
                payload.yaw(), payload.pitch(), payload.mPitch(), payload.mYaw(), payload.casing(), payload.smoking(),
                payload.smokeLife(), payload.smokeLift(), payload.nodeLife(), true, payload.legacy()));
    }

    /**
     * 1.7 {@code CasingEjector.spawnCasing}, which rolls its motion with the pitch factor on both axes and leaves the
     * casing's previous position at the unshifted spawn point.
     */
    public static void eject(double x, double y, double z, CompoundTag data) {
        ClientLevel level = Minecraft.getInstance().level;
        SpentCasing config = SpentCasing.fromName(Tags.getStringOr(data, "name", ""));
        if (level == null || config == null || config.getColors() == null) return;
        float pitch = Tags.getFloatOr(data, "pitch", 0F);
        float yaw = Tags.getFloatOr(data, "yaw", 0F);
        float yawFactor = Tags.getFloatOr(data, "yawFactor", 0F);
        float pitchFactor = Tags.getFloatOr(data, "pitchFactor", 0F);
        Random rand = EJECT_RANDOM;
        float rolledPitch = pitch + (float) rand.nextGaussian() * pitchFactor;
        float rolledYaw = yaw + (float) rand.nextGaussian() * pitchFactor;
        Vector4f motion = new Vector4f(
                (float) (Tags.getDoubleOr(data, "mX", 0D) + rand.nextGaussian() * pitchFactor),
                (float) (Tags.getDoubleOr(data, "mY", 0D) + rand.nextGaussian() * pitchFactor),
                (float) (Tags.getDoubleOr(data, "mZ", 0D) + rand.nextGaussian() * pitchFactor), 1F);
        new Matrix4f().rotateY(-rolledYaw).rotateX(rolledPitch).transform(motion);

        ParticleSpentCasing casing = new ParticleSpentCasing(level, x, y, z, motion.x, motion.y, motion.z,
                (float) (level.getRandom().nextGaussian() * 5F), (float) (level.getRandom().nextGaussian() * 10F),
                config, false, 0, 0D, 0);
        Vector4f offset = new Vector4f((float) Tags.getDoubleOr(data, "oX", 0D), (float) Tags.getDoubleOr(data, "oY", 0D),
                (float) Tags.getDoubleOr(data, "oZ", 0D), 1F);
        new Matrix4f().rotateY(-yaw).rotateX(pitch).transform(offset);
        casing.x += offset.x;
        casing.y += offset.y;
        casing.z += offset.z;
        casing.rotationPitch = casing.prevRotationPitch = (float) Math.toDegrees(pitch);
        casing.rotationYaw = casing.prevRotationYaw = (float) Math.toDegrees(yaw);
        GROUP.add(casing);
    }

    public static List<ParticleSpentCasing> live() {
        return GROUP.live();
    }

    private static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            if (lastLevel != level) {
                GROUP.clear();
                lastLevel = level;
            }
            GROUP.tick(level);
        } else {
            GROUP.clear();
            lastLevel = null;
        }
    }

    private static final class SpentCasingLookup {
        private static void add(ClientLevel level, CasingCreator.Event event, CasingParticleGroup group) {
            var config = SpentCasing.fromName(event.casing());
            if (config == null || config.getColors() == null) return;
            int smokeLife = event.smokeLife();
            double smokeLift = event.smokeLift();
            int nodeLife = event.nodeLife();
            if (event.smoking() && smokeLife <= 0 && config.getSmokeDuration() > 0) {
                smokeLife = config.getSmokeDuration();
                smokeLift = config.getSmokeLift();
                nodeLife = config.getSmokeNodeLife();
            }
            ParticleSpentCasing particle = new ParticleSpentCasing(level, event.x(), event.y(), event.z(),
                    event.mX(), event.mY(), event.mZ(), event.mPitch(), event.mYaw(), config,
                    event.smoking(), smokeLife, smokeLift, nodeLife);
            particle.rotationYaw = particle.prevRotationYaw = event.yaw();
            particle.rotationPitch = particle.prevRotationPitch = event.pitch();
            group.add(particle);
        }
    }
}
