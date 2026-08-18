package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/**
 * S2C: play one of HBM's own particle effects at a world position — the 26.1 equivalent of the old
 * {@code AuxParticlePacketNT}.
 *
 * <p>HBM's particles are deliberately <i>not</i> registered as vanilla {@code ParticleType}s. One
 * effect can mean "spawn 40 particles of four different classes, coloured from this recipe and aimed
 * along this vector", and the parameters ride along as an arbitrary {@link CompoundTag}. Vanilla
 * {@code ParticleOptions} cannot express that without one registered type (and one codec) per effect,
 * so the effect is named here and the client rebuilds the particles from the tag.
 *
 * <p>Two deliberate deviations from 1.12.2:
 *
 * <ul>
 *   <li>The effect is keyed by <b>name</b>, not by enum ordinal. Upstream wrote the ordinal, which
 *       silently remaps every effect the moment someone inserts an entry mid-enum — the same trap the
 *       gun wave hit with index-keyed bullet configs, and the same fix.
 *   <li>Delivery is by <b>chunk tracking</b> rather than upstream's {@code TargetPoint} radius. The
 *       port has no radius-based sender; {@code sendToTracking} is what the BlockEntity sync already
 *       uses, and a player who has the chunk loaded is the population that could see the effect
 *       anyway. Effects with a range far larger than a chunk view would need a real radius sender.
 * </ul>
 */
public record ParticlePayload(String effect, double x, double y, double z, CompoundTag data)
        implements GolemPayload {

    public static final GolemCodec<ParticlePayload> CODEC = GolemCodec.of(ParticlePayload.class,
            Ids.of(Constants.MOD_ID, "particle"),
            (payload, buffer) -> {
                buffer.writeUtf(payload.effect);
                buffer.writeDouble(payload.x);
                buffer.writeDouble(payload.y);
                buffer.writeDouble(payload.z);
                buffer.writeNbt(payload.data);
            },
            buffer -> new ParticlePayload(buffer.readUtf(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readDouble(), buffer.readNbt()));

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
