package com.hbm.particle.helper;

import com.hbm.packet.ParticlePayload;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
    /** A large blast: a smoke plume and scoops of the ground itself thrown into the air. */
    ExplosionLarge,
    /** Muzzle smoke and sparks from a gunpowder discharge. */
    BlackPowder,
    /** Drifting embers and ash, for things that are burning down rather than exploding. */
    Ashes,
    /** The authored OBJ skeleton strip/fragment effect. */
    Skeleton,
    /** The authored meat/slime/metal giblet billboard effect. */
    Giblets,
    /** The full-size RBMK meltdown cloud. */
    RBMKMush,
    /** The old small-nuke wave, flare and delayed cloud effect. */
    Muke,
    /** The tiny-tot wave and immediate small-nuke cloud effect. */
    TinyTot,
    /** The smaller Watz/RBMK cloud. */
    RBMKMushSmall,
    /** One puff of a missile's exhaust trail. Spawned client-side, once per tick per nozzle. */
    MissileContrail,
    /** Redstone block debris thrown from a living entity hit by a running sawmill blade. */
    SawmillDamage,
    /** The drifting cooling-tower smoke puff. */
    Tower,
    /** One vanilla critical particle used while a refueler fills an item. */
    FluidFill,
    /** Exhaust from a fuelled jetpack. */
    Jetpack,
    /** Purple exhaust from the BJ winged power armour. */
    Jetpack_BJ,
    /** Cyan exhaust from the DNT nano suit. */
    Jetpack_DNS,
    /** The old block-destruction particle burst sent when rubble lands. */
    BlockBurst,
    /** The large explosion's drifting smoke cloud. */
    Smoke_Cloud,
    /** The large explosion's radial shock smoke. */
    Smoke_Shock,
    /** 1.7 smoke mode {@code radial}: ExSmoke thrown on a gaussian in all three axes (the naval mine). */
    Smoke_Radial,
    /** 1.7 smoke mode {@code foamSplash}: rising foam bubbles (the naval mine under water). */
    Smoke_FoamSplash,
    /** Direct client-side block dust used by the three legacy extinguisher streams. */
    VanillaExt_BlockDust,
    /**
     * The old {@code gasfire} puff sent by the miner lander's retro burn (ntm-next {@code GasFlamePayload}).
     * Tag: {@code moX/moY/moZ} motion, optional {@code scale} (6.5 when absent).
     */
    GasFlame,
    /**
     * Arc endpoints for a tesla/taint crab (ntm-next {@code TeslaArcPayload}). Tag: {@code entityID} and a
     * {@code targets} list of {@code x/y/z} doubles; the client hands them to the crab it names.
     */
    TeslaArc,
    /** The ICF's fusion flash (1.7 {@code hadron}): a growing, fading additive quad, every reacting tick. */
    Hadron,
    /** Original decontaminator's recoloured town-aura particle, emitted once per client tick. */
    DeconAura,
    /** 1.7 {@code bnuuy}: the diesel suit legs' twin smoke puffs behind the wearer. Tag: {@code player}. */
    Bnuuy,
    /** 1.7 {@code amat}: {@code ParticleAmatFlash}, a ten-tick white starburst. Tag: {@code scale} (explosion size). */
    AmatFlash,
    /**
     * 1.7 {@code properJolt}, sent to one player: sets its hurt time and hurt duration directly (hurt direction 0),
     * so the hurt-camera tilt lingers for {@code time} ticks. Tag: {@code time}, {@code maxTime}.
     */
    ProperJolt,
    /**
     * 1.7 {@code tau} (G-C): {@code count} white line sparks plus one hadron flash, both halved/shortened when
     * {@code small} - the RBMK lever's contact arc. Tag: {@code count} (byte), {@code small}.
     */
    Tau,
    /**
     * 1.7 {@code marker} (G-C): a {@code RenderOverhead} marker, a see-through outline of the block at the position
     * with an optional label. Tag: {@code color}, {@code label}, {@code expires} (ms, 0 = never), {@code dist}.
     */
    Marker,
    /** 1.7 {@code debug} (G-C): {@code ParticleText}, floating text. Tag: {@code text}, {@code color}, {@code scale}. */
    DebugText,
    /** 1.7 {@code launchSmoke} (G-C): one {@code ParticleSmokePlume}, played client-side by a launcher. Tag: {@code moX/moY/moZ}. */
    LaunchSmoke,
    /**
     * old-hbm {@code VanillaBurst_BlockDust}: {@code count} block-dust particles of block id {@code block}, each with
     * motion {@code gaussian * motion} (+0.2 up) and a 50..99 tick life. The crucible's killing hit.
     */
    VanillaBurst_BlockDust,
    /** 1.7 {@code deadleaf}: one {@code ParticleDeadLeaf} drifting down from {@code waste_leaves}. Client-side only. */
    DeadLeaf,
    /**
     * 1.7 {@code radFog}: one {@code ParticleRadiationFog}, rolled once a second over chunks holding more than
     * {@code fogRad} (History §19ej-6, {@code RadiationFog}).
     */
    RadFog,
    /**
     * 1.7 {@code sweat}: {@code count} 150..199-tick block-dust particles of block {@code block} (runtime block id)
     * scattered over entity {@code entity}'s box. Radiation, digamma, oil and the fat pigeon. Client:
     * {@code LivingEffectParticles}.
     */
    Sweat,
    /**
     * 1.7 {@code vomit}: {@code count} particles from entity {@code entity}'s mouth along its look; {@code mode}
     * {@code normal} (green clay dust), {@code blood} (redstone dust) or {@code smoke} (coughed coal).
     */
    Vomit,
    /** 1.7 {@code radiation}: {@code count} cyan town-aura flashes around the local player. Client-side only. */
    Radiation,
    /** 1.7 {@code exhaust}: {@code count} rocket-flame puffs spread by {@code width}; {@code mode} soyuz or meteor. */
    Exhaust,
    /** 1.7 {@code haze}: one {@code ParticleHaze}, the white-phosphorus haze. */
    Haze,
    /** 1.7 {@code ufo}: one lone {@code ParticleMukeCloud} at the foot of the UFO's tractor beam. */
    Ufo;

    /** 1.7 {@code createSendToThreadedPacket}: this effect for one player only. */
    public void sendTo(ServerPlayer player, double x, double y, double z, @Nullable CompoundTag data) {
        GolemNetwork.sendToPlayer(player, ParticlePayload.CODEC,
                new ParticlePayload(this.name(), x, y, z, data == null ? new CompoundTag() : data));
    }

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

    /** Sends an AuxParticle-equivalent effect to the source's explicit radius, not just its chunk. */
    public void send(Level level, double x, double y, double z, @Nullable CompoundTag data, double range) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        ParticlePayload payload = new ParticlePayload(this.name(), x, y, z,
                data == null ? new CompoundTag() : data);
        double radiusSquared = range * range;
        for (ServerPlayer player : serverLevel.players()) {
            // level.players() includes a GameTest's bare mock player without the channel; sendToPlayer skips it
            // (NeoForge would otherwise throw - crashed the 2026-10-02 full run from a leaking fluid tank).
            if (player.distanceToSqr(x, y, z) <= radiusSquared)
                GolemNetwork.sendToPlayer(player, ParticlePayload.CODEC, payload);
        }
    }

    /**
     * 1.7 {@code createAllAroundThreadedPacket} whose {@code TargetPoint} is not the effect's own position (e.g. the
     * haze puffs scattered around a shell but sent to everyone within 150 of the shell).
     */
    public void sendAround(Level level, double x, double y, double z, @Nullable CompoundTag data,
            double centreX, double centreY, double centreZ, double range) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        double radiusSquared = range * range;
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(centreX, centreY, centreZ) <= radiusSquared) this.sendTo(player, x, y, z, data);
        }
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
