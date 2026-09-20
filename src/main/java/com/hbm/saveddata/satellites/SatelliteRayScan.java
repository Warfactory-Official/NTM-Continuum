package com.hbm.saveddata.satellites;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.items.machine.ItemSatellite;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Objects;

/**
 * Listens for the things that emit hard radiation: arc flashes, running reactors, particle beams, radar and
 * radio. Machines report themselves here as they run, and a survey from orbit picks up everything inside a
 * 250-block circle of the satellite's target.
 */
public class SatelliteRayScan extends Satellite {

    public static final int MAX_SCAN_RANGE = 250;

    public static final String CMD_SURVEY = "survey";
    public static final String CMD_COUNT = "count";
    public static final String CMD_GETINFO = "getinfo";
    public static final String CMD_GETPOSITION = "getposition";

    public final List<RayEvent> cachedResults = new ArrayList<>();

    public static final MapCodec<SatelliteRayScan> CODEC = MapCodec.unit(SatelliteRayScan::new);

    public SatelliteRayScan() {
        super(Interfaces.SAT_PANEL, Set.of(InterfaceActions.HAS_MAP), Set.of());
    }

    @Override
    public SatelliteType type() {
        return SatelliteType.RAY_SCAN;
    }

    @Override public String getType() { return "NB_RAY_SCANNER"; }

    @Override
    public Component[] getInfo(Level level) {
        return new Component[]{Component.translatable(ItemSatellite.nameKey(EnumSatType.RAY_SCAN))};
    }

    @Override
    public void onCommandImpl(Level level, String... cmd) {
        if (cmd.length <= 0) return;

        if (CMD_SURVEY.equals(cmd[0])) {
            this.cachedResults.clear();
            for (Map.Entry<DimPos, RayEvent> entry : rayEvents.entrySet()) {
                DimPos pos = entry.getKey();
                if (!pos.dimension().equals(level.dimension())) continue;
                int dx = pos.x() - this.targetX;
                int dz = pos.z() - this.targetZ;
                if (dx * dx + dz * dz <= MAX_SCAN_RANGE * MAX_SCAN_RANGE) cachedResults.add(entry.getValue());
            }
            return;
        }

        if (CMD_COUNT.equals(cmd[0])) {
            this.tx = "" + cachedResults.size();
            return;
        }

        if (CMD_GETINFO.equals(cmd[0]) && cmd.length == 2) {
            RayEvent event = getEventFromIndex(cmd[1]);
            this.tx = event == null ? "" : event.info;
            return;
        }

        if (CMD_GETPOSITION.equals(cmd[0]) && cmd.length == 2) {
            RayEvent event = getEventFromIndex(cmd[1]);
            this.tx = event == null ? "" : event.x + ";" + event.z;
        }
    }

    public @Nullable RayEvent getEventFromIndex(String cmd) {
        if (cachedResults.isEmpty()) return null;
        return cachedResults.get(IRORInteractive.parseInt(cmd, 1, cachedResults.size()) - 1);
    }

    @Override
    public float[] getColor() {
        return new float[]{0.4F, 1.0F, 0.8F};
    }

    // --- the world's emission log ---

    public static final LinkedHashMap<DimPos, RayEvent> rayEvents = new LinkedHashMap<>();

    /** A machine announcing itself. The newest report for a position replaces the last. */
    public static void reportEvent(Level level, int x, int y, int z, String info, int lifetime) {
        if (level == null || level.isClientSide()) return;
        rayEvents.put(new DimPos(x, y, z, level.dimension()), new RayEvent(level, lifetime, x, z, info));
    }

    public static void updateSystem(Level level) {
        rayEvents.entrySet().removeIf(entry -> entry.getKey().dimension().equals(level.dimension())
                && level.getGameTime() > entry.getValue().expiresOn);
    }

    public record DimPos(int x, int y, int z, ResourceKey<Level> dimension) {
        @Override
        public boolean equals(Object o) {
            return o instanceof DimPos other && x == other.x && y == other.y && z == other.z
                    && dimension.equals(other.dimension);
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z, dimension);
        }
    }

    public static class RayEvent {

        public static final String INFO_ARC_FLASH = "ARC_FLASH";
        public static final String INFO_NUCLEAR = "NEUTRON_EMISSION";
        public static final String INFO_PARTICLE = "HIGH_ENERGY_PARTICLES";
        public static final String INFO_RADAR = "RADAR_WAVES";
        public static final String INFO_RADIO = "RADIO_WAVES";

        public final long expiresOn;
        public final String info;
        public final int x;
        public final int z;

        public RayEvent(Level level, int lifetime, int x, int z, String info) {
            this.expiresOn = level.getGameTime() + lifetime;
            this.x = x;
            this.z = z;
            this.info = info;
        }
    }
}
