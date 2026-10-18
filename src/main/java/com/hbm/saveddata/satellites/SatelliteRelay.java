package com.hbm.saveddata.satellites;

import com.hbm.items.machine.ItemSatellite.EnumSatType;
import com.hbm.tileentity.network.RTTYSystem;
import com.mojang.serialization.MapCodec;
import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code SatelliteRelay}: {@code relay <dimension> <channel> <signal...>} broadcasts the signal on that
 * channel in another dimension, configured and sent in one command so no tick is lost changing channels.
 */
public class SatelliteRelay extends Satellite {

    public static final MapCodec<SatelliteRelay> CODEC = MapCodec.unit(SatelliteRelay::new);

    public static final String CMD_RELAY = "relay";

    public SatelliteRelay() {}

    @Override
    public SatelliteType type() {
        return SatelliteType.RELAY;
    }

    @Override public String getType() { return "DIMENSIONAL_RELAY"; }

    @Override
    public Component[] getInfo(Level level) {
        return new Component[] { nameOf(EnumSatType.RELAY) };
    }

    @Override
    public void onCommandImpl(Level level, String... cmd) {
        if (cmd.length <= 0) return;

        if (cmd[0].equals(CMD_RELAY) && cmd.length > 3 && level instanceof ServerLevel server) {
            ServerLevel target = dimension(server, cmd[1]);

            if (target != null) {
                StringBuilder signal = new StringBuilder();
                for (int i = 3; i < cmd.length; i++) {
                    if (i > 3) signal.append(" ");
                    signal.append(cmd[i]);
                }
                RTTYSystem.broadcast(target, cmd[2], signal.toString());
            }
        }
    }

    // port: dimensions have no numbers; 1.7's 0, -1 and 1 still name the vanilla three, anything else is an id
    private static @Nullable ServerLevel dimension(ServerLevel level, String name) {
        ResourceKey<Level> key = switch (name) {
            case "0" -> Level.OVERWORLD;
            case "-1" -> Level.NETHER;
            case "1" -> Level.END;
            default -> name.matches("([a-z0-9_.-]+:)?[a-z0-9_./-]+")
                    ? ResourceKey.create(Registries.DIMENSION, Ids.parse(name)) : null;
        };
        return key == null ? null : level.getServer().getLevel(key);
    }
}
