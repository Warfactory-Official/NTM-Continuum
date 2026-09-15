package com.hbm.packet;

import com.hbm.Constants;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.items.weapon.sedna.BulletConfigs;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/** S2C state for the short-lived projectile visuals whose gameplay state is server-only. */
public record ProjectileStatePayload(int entityId, String configId,
        double headingX, double headingY, double headingZ,
        double endpointX, double endpointY, double endpointZ,
        double beamLength, float yaw, float pitch) implements GolemPayload {

    public static final GolemCodec<ProjectileStatePayload> CODEC = GolemCodec.of(ProjectileStatePayload.class,
            Ids.of(Constants.MOD_ID, "projectile_state"),
            (payload, buffer) -> {
                buffer.writeVarInt(payload.entityId);
                buffer.writeUtf(payload.configId);
                buffer.writeDouble(payload.headingX);
                buffer.writeDouble(payload.headingY);
                buffer.writeDouble(payload.headingZ);
                buffer.writeDouble(payload.endpointX);
                buffer.writeDouble(payload.endpointY);
                buffer.writeDouble(payload.endpointZ);
                buffer.writeDouble(payload.beamLength);
                buffer.writeFloat(payload.yaw);
                buffer.writeFloat(payload.pitch);
            },
            buffer -> new ProjectileStatePayload(buffer.readVarInt(), buffer.readUtf(),
                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readDouble(), buffer.readFloat(), buffer.readFloat()));
    private static final Map<Integer, ProjectileStatePayload> PENDING = new ConcurrentHashMap<>();
    private static final double MIN_BEAM_LENGTH = 1.0E-6D;

    public static ProjectileStatePayload beam(EntityBulletBeamBase beam) {
        return new ProjectileStatePayload(beam.getId(),
                beam.config == null || beam.config.id == null ? "" : beam.config.id.toString(),
                beam.headingX, beam.headingY, beam.headingZ,
                beam.beamEndX, beam.beamEndY, beam.beamEndZ,
                beam.beamLength, beam.getYRot(), beam.getXRot());
    }

    /** Replays authoritative beam state to the players who can see the beam's chunk. */
    public static void send(EntityBulletBeamBase beam) {
        if (!(beam.level() instanceof ServerLevel level)) return;
        GolemNetwork.sendToTracking(level, BlockPos.containing(beam.getX(), beam.getY(), beam.getZ()), CODEC,
                beam(beam));
    }

    /** Applies state after the vanilla entity spawn has made the entity addressable on the client. */
    public void apply(Level level) {
        if (level == null) return;
        Entity entity = level.getEntity(entityId);
        if (!(entity instanceof EntityBulletBeamBase beam)) {
            PENDING.put(entityId, this);
            return;
        }
        applyTo(beam);
    }

    /** Handles a transport ordering edge where the custom payload beats the vanilla entity spawn. */
    public static void applyPending(EntityBulletBeamBase beam) {
        ProjectileStatePayload pending = PENDING.remove(beam.getId());
        if (pending != null) pending.applyTo(beam);
    }

    private void applyTo(EntityBulletBeamBase beam) {
        if (!Double.isFinite(beamLength) || beamLength <= MIN_BEAM_LENGTH
                || !Double.isFinite(headingX) || !Double.isFinite(headingY) || !Double.isFinite(headingZ)
                || !Double.isFinite(endpointX) || !Double.isFinite(endpointY) || !Double.isFinite(endpointZ)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            Constants.LOG.warn("Rejected invalid projectile state for beam entity {}", entityId);
            return;
        }
        beam.applyRenderState(this, BulletConfigs.get(configId));
    }

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
