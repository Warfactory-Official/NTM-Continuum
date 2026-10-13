package com.hbm.packet;

import com.hbm.Constants;
import com.hbm.config.ClientConfig;
import dev.golem.core.Ids;
import dev.golem.core.world.Sounds;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * S2C positional gun sound. Positions use the vanilla sound-packet fixed-point format and the seed is
 * carried so local and observer clients choose the same member from a sound pool.
 */
public record GunSoundPayload(boolean legacy, String soundId, SoundSource source, int x, int y, int z,
        float volume, float pitch, long seed) implements GolemPayload {

    public static final GolemCodec<GunSoundPayload> CODEC = GolemCodec.of(GunSoundPayload.class,
            Ids.of(Constants.MOD_ID, "gun_sound"),
            (payload, buffer) -> {
                buffer.writeBoolean(payload.legacy);
                buffer.writeUtf(payload.soundId);
                buffer.writeVarInt(payload.source.ordinal());
                buffer.writeInt(payload.x);
                buffer.writeInt(payload.y);
                buffer.writeInt(payload.z);
                buffer.writeFloat(payload.volume);
                buffer.writeFloat(payload.pitch);
                buffer.writeLong(payload.seed);
            },
            buffer -> {
                boolean legacy = buffer.readBoolean();
                String soundId = buffer.readUtf(256);
                int source = buffer.readVarInt();
                SoundSource[] values = SoundSource.values();
                SoundSource category = source >= 0 && source < values.length ? values[source] : SoundSource.PLAYERS;
                return new GunSoundPayload(legacy, soundId, category, buffer.readInt(), buffer.readInt(),
                        buffer.readInt(), buffer.readFloat(), buffer.readFloat(), buffer.readLong());
            });

    public GunSoundPayload(boolean legacy, SoundEvent sound, SoundSource source, double x, double y, double z,
            float volume, float pitch, long seed) {
        this(legacy, idOf(sound), source, (int) (x * 8.0), (int) (y * 8.0), (int) (z * 8.0), volume, pitch, seed);
    }

    public GunSoundPayload(boolean legacy, Holder<SoundEvent> sound, SoundSource source, double x, double y, double z,
            float volume, float pitch, long seed) {
        this(legacy, sound.value(), source, x, y, z, volume, pitch, seed);
    }

    public static void send(ServerLevel level, boolean legacy, SoundEvent sound, SoundSource source,
            double x, double y, double z, float volume, float pitch, long seed) {
        if (sound == null) return;
        GunSoundPayload payload = new GunSoundPayload(legacy, sound, source, x, y, z, volume, pitch, seed);
        double radius = sound.getRange(volume);
        double radiusSquared = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) <= radiusSquared)
                GolemNetwork.sendToPlayer(player, CODEC, payload);
        }
    }

    public static void handleClient(GunSoundPayload payload, GolemNetwork.ClientContext context) {
        if (payload.legacy != ClientConfig.GUN_ANIMS_LEGACY || context.level() == null) return;
        SoundEvent sound;
        try {
            sound = Sounds.get(Ids.parse(payload.soundId));
        } catch (IllegalArgumentException invalidId) {
            return;
        }
        if (sound == null) return;
        context.level().playSeededSound(context.player(), payload.x / 8.0D, payload.y / 8.0D,
                payload.z / 8.0D, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), payload.source,
                payload.volume, payload.pitch, payload.seed);
    }

    private static String idOf(SoundEvent sound) {
        Identifier id = BuiltInRegistries.SOUND_EVENT.getKey(sound);
        return id == null ? "minecraft:empty" : id.toString();
    }

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
