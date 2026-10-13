package com.hbm.saveddata;

import com.hbm.Constants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import dev.golem.core.world.CompatSavedData;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TomSaveData extends CompatSavedData<TomSaveData> {

    private static final String ID = Constants.MOD_ID + ":impact_data";
    private static final Codec<TomSaveData> CODEC = makeCodec();

    public float dust;
    public float fire;
    public boolean impact;

    public TomSaveData() {
    }

    private TomSaveData(float dust, float fire, boolean impact) {
        this.dust = dust;
        this.fire = fire;
        this.impact = impact;
    }

    private static final Map<ResourceKey<Level>, TomSaveData> PUBLISHED = new ConcurrentHashMap<>();

    public static TomSaveData get(ServerLevel level) {
        TomSaveData data = CompatSavedData.get(level, ID, TomSaveData::new, CODEC);
        PUBLISHED.put(level.dimension(), data);
        return data;
    }

    public static TomSaveData getExisting(ServerLevel level) {
        return CompatSavedData.getIfPresent(level, ID, TomSaveData::new, CODEC);
    }

    /**
     * The impact gate for a caller off the server thread. Worldgen features run on foreign pools - Distant
     * Horizons' and C2ME's - and {@code SavedDataStorage} keys a bare {@link java.util.HashMap} that its own
     * {@code get} WRITES to on a miss, so reaching the storage from one of those races the server thread.
     * Absent published data means the level never impacted.
     */
    public static boolean impact(ServerLevel level) {
        TomSaveData data = PUBLISHED.get(level.dimension());
        return data != null && data.impact;
    }

    /** {@link #impact}'s live instance for dust and fire; null = never impacted. */
    public static @Nullable TomSaveData published(ServerLevel level) {
        return PUBLISHED.get(level.dimension());
    }

    /** Server thread: republish without creating the data for a level that has none. */
    public static void publish(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            TomSaveData data = CompatSavedData.getIfPresent(level, ID, TomSaveData::new, CODEC);
            if (data != null) PUBLISHED.put(level.dimension(), data);
        }
    }

    public static void onServerStopping() {
        PUBLISHED.clear();
    }

    @Override
    protected Codec<TomSaveData> codec() {
        return CODEC;
    }

    private static Codec<TomSaveData> makeCodec() {
        return RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.optionalFieldOf("dust", 0F).forGetter(d -> d.dust),
                Codec.FLOAT.optionalFieldOf("fire", 0F).forGetter(d -> d.fire),
                Codec.BOOL.optionalFieldOf("impact", false).forGetter(d -> d.impact)
        ).apply(i, TomSaveData::new));
    }
}
