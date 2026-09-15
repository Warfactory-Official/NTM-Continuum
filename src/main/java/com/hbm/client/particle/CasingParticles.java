package com.hbm.client.particle;

import com.hbm.particle.helper.CasingCreator;
import dev.golem.core.client.ClientResourceReloads;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.render.WorldEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/** Client lifetime owner for source spent-casing particles. */
public final class CasingParticles {
    private static final CasingParticleGroup GROUP = new CasingParticleGroup();
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

    public static void spawn(com.hbm.packet.CasingEjectPayload payload) {
        spawn(new CasingCreator.Event(payload.x(), payload.y(), payload.z(), payload.mX(), payload.mY(), payload.mZ(),
                payload.yaw(), payload.pitch(), payload.mPitch(), payload.mYaw(), payload.casing(), payload.smoking(),
                payload.smokeLife(), payload.smokeLift(), payload.nodeLife(), false, false));
    }

    public static void spawn(com.hbm.packet.GunCasingEjectPayload payload) {
        spawn(new CasingCreator.Event(payload.x(), payload.y(), payload.z(), payload.mX(), payload.mY(), payload.mZ(),
                payload.yaw(), payload.pitch(), payload.mPitch(), payload.mYaw(), payload.casing(), payload.smoking(),
                payload.smokeLife(), payload.smokeLift(), payload.nodeLife(), true, payload.legacy()));
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
            var config = com.hbm.particle.SpentCasing.fromName(event.casing());
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
