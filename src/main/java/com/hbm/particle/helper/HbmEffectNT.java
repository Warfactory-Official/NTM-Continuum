package com.hbm.particle.helper;

import com.hbm.packet.ParticlePayload;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

/**
 * The catalogue of HBM's own particle effects, and the server-side entry point for playing one.
 *
 * <p>An effect is a <i>name plus a bag of parameters</i>, not a particle: {@code ExplosionSmall}
 * spawns fireballs and block debris together, and the tag it carries says how big and how
 * fast. The server never touches a particle class — it names an effect here and
 * {@link #send} puts it on the wire; the client looks the name up in its handler table and builds
 * whatever that effect means (see {@code com.hbm.client.particle.HbmParticleHandlers}).
 *
 * <p><b>This class must stay server-safe.</b> It holds the handler as a plain {@link Object} and the
 * handler interface takes no client types, so nothing here drags {@code Minecraft} onto a dedicated
 * server's class path. That split is the port's own — 1.12.2 used {@code @SideOnly} to strip the
 * client half out of the server jar, which has no equivalent in a single source set.
 *
 * <p>Only the effects this port actually implements are listed. Upstream declares roughly a hundred;
 * adding one is a constant here plus a handler registration on the client, so the rest arrive with
 * the systems that emit them rather than as dead names.
 */
public enum HbmEffectNT {

    /** The flamethrower's cone of burning fuel. */
    Flamethrower,
    /** A small blast: a few fireballs plus tumbling block debris. */
    ExplosionSmall,
    /** Muzzle smoke and sparks from a gunpowder discharge. */
    BlackPowder,
    /** Drifting embers and ash, for things that are burning down rather than exploding. */
    Ashes;

    /**
     * The client handler for this effect, or null on a server (and on a client before
     * registration). Typed as {@link Object} so this class never references the client-only
     * {@code EffectHandler} functional type in a field signature.
     */
    private Object handler;

    /** Registers the client-side behaviour for this effect. Client only. */
    public void setHandler(EffectHandler handler) {
        this.handler = handler;
    }

    /** Whether a client handler has been registered. */
    public boolean hasHandler() {
        return this.handler != null;
    }

    /**
     * Runs this effect's client handler. Client only, and a no-op for an effect whose handler was
     * never registered — an unimplemented effect should be invisible, not a crash.
     */
    public void summonParticle(Level level, double x, double y, double z, @Nullable CompoundTag data) {
        if (this.handler == null) return;
        ((EffectHandler) this.handler).summonParticle(level, x, y, z, data == null ? new CompoundTag() : data);
    }

    /** Plays this effect for every player tracking the chunk containing the position. */
    public void send(Level level, double x, double y, double z, @Nullable CompoundTag data) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        GolemNetwork.sendToTracking(serverLevel, BlockPos.containing(x, y, z), ParticlePayload.CODEC,
                new ParticlePayload(this.name(), x, y, z, data == null ? new CompoundTag() : data));
    }

    /** Plays this effect at the centre of a block position. */
    public void send(Level level, BlockPos pos, @Nullable CompoundTag data) {
        this.send(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, data);
    }

    /** Resolves an effect by name, or null if this port does not implement it. */
    @Nullable
    public static HbmEffectNT byName(String name) {
        for (HbmEffectNT effect : values()) {
            if (effect.name().equals(name)) return effect;
        }
        return null;
    }

    /** What one effect does on the client. */
    @FunctionalInterface
    public interface EffectHandler {
        void summonParticle(Level level, double x, double y, double z, CompoundTag data);
    }
}
