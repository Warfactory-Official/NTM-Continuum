package com.hbm.saveddata.satellites;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Watches the whole dimension for detonations. Where the ray scanner reports a running machine at its exact
 * position, this reports a burst anywhere with a deliberate error on it, up to ten kilometres for a small
 * one, five hundred blocks for the largest, so a survey tells you roughly where, and how big.
 */
public class SatelliteDetector extends Satellite {

    public static final MapCodec<SatelliteDetector> CODEC = MapCodec.unit(SatelliteDetector::new);

    public SatelliteDetector() {}

    @Override
    public SatelliteType type() {
        return SatelliteType.DETECTOR;
    }

    public static final String CMD_SURVEY = "survey";
    public static final String CMD_COUNT = "count";
    public static final String CMD_GETTYPE = "gettype";
    public static final String CMD_GETPOSITION = "getposition";

    public static final int DURATION_LOW = 15 * 20;
    public static final int DURATION_MEDIUM = 20 / 2;
    public static final int DURATION_HIGH = 60 * 20;

    public static final double INACCURACY_LOW = 10_000;
    public static final double INACCURACY_MEDIUM = 2_500;
    public static final double INACCURACY_HIGH = 500;

    public final List<RadiationBurst> cachedResults = new ArrayList<>();

    @Override public String getType() { return "UWB_EMISSION_DETECTOR"; }

    @Override
    public void onCommandImpl(Level level, String... cmd) {
        if (cmd.length <= 0) return;

        if (CMD_SURVEY.equals(cmd[0])) {
            cachedResults.clear();
            for (RadiationBurst burst : bursts) {
                if (burst.dimension.equals(level.dimension())) cachedResults.add(burst);
            }
            return;
        }

        if (CMD_COUNT.equals(cmd[0])) {
            this.tx = "" + cachedResults.size();
            return;
        }

        if (CMD_GETTYPE.equals(cmd[0]) && cmd.length == 2) {
            RadiationBurst burst = getBurstFromIndex(cmd[1]);
            this.tx = burst == null ? "" : burst.intensity.name();
            return;
        }

        if (CMD_GETPOSITION.equals(cmd[0]) && cmd.length == 2) {
            RadiationBurst burst = getBurstFromIndex(cmd[1]);
            this.tx = burst == null ? "" : burst.x + ";" + burst.z;
        }
    }

    public @Nullable RadiationBurst getBurstFromIndex(String cmd) {
        if (cachedResults.isEmpty()) return null;
        return cachedResults.get(IRORInteractive.parseInt(cmd, 1, cachedResults.size()) - 1);
    }

    public static final List<RadiationBurst> bursts = new ArrayList<>();

    public static void reportEvent(Level level, int lifetime, BurstIntensity intensity, double x, double z) {
        if (level == null || level.isClientSide()) return;
        bursts.add(new RadiationBurst(level, lifetime, intensity, (int) Math.floor(x), (int) Math.floor(z)));
    }

    public static void updateSystem(Level level) {
        bursts.removeIf(b -> b.dimension.equals(level.dimension()) && level.getGameTime() > b.expiresOn);
    }

    public static class RadiationBurst {

        public final ResourceKey<Level> dimension;
        public final long expiresOn;
        public final BurstIntensity intensity;
        public final int x;
        public final int z;

        public RadiationBurst(Level level, int lifetime, BurstIntensity intensity, int x, int z) {
            this.dimension = level.dimension();
            this.expiresOn = level.getGameTime() + lifetime;
            this.intensity = intensity;

            double inaccuracy = switch (intensity) {
                case LOW -> INACCURACY_LOW;
                case MEDIUM -> INACCURACY_MEDIUM;
                case HIGH -> INACCURACY_HIGH;
            };

            // 1.7: an unclamped gaussian, so a small burst can read tens of kilometres off
            this.x = (int) (x + level.getRandom().nextGaussian() * inaccuracy);
            this.z = (int) (z + level.getRandom().nextGaussian() * inaccuracy);
        }
    }

    public enum BurstIntensity { LOW, MEDIUM, HIGH }
}
