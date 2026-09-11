package com.hbm.saveddata.satellites;

import com.hbm.items.machine.ItemSatellite;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import com.hbm.tileentity.network.RTTYSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Carries a radio signal into another dimension. One command does the whole job — pick the dimension, pick
 * the channel, and send — because splitting it would cost a tick between configuring and transmitting.
 *
 * <p>Deviation from CE: the dimension is named rather than numbered. Upstream takes an integer dimension id
 * and asks {@code DimensionManager} for it; 26.1 keys dimensions by identifier, so the command takes one
 * (bare names resolve in {@code minecraft:}), and there is no integer that could stand in for it.
 */
public class SatelliteRelay extends Satellite {

    public static final String CMD_RELAY = "relay";

    public SatelliteRelay() {
        this.satIface = Interfaces.NONE;
    }

    @Override public String getType() { return "DIMENSIONAL_RELAY"; }

    @Override
    public Component[] getInfo(Level level) {
        return new Component[]{Component.translatable(ItemSatellite.nameKey(EnumSatType.RELAY))};
    }

    @Override
    public void onCommandImpl(Level level, String... cmd) {
        if (cmd.length <= 3 || !CMD_RELAY.equals(cmd[0])) return;
        if (!(level instanceof ServerLevel server)) return;

        Identifier id = Identifier.tryParse(cmd[1]);
        if (id == null) return;

        ServerLevel target = server.getServer().getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id));
        if (target == null) return;

        StringBuilder signal = new StringBuilder();
        for (int i = 3; i < cmd.length; i++) {
            if (i > 3) signal.append(" ");
            signal.append(cmd[i]);
        }

        RTTYSystem.broadcast(target, cmd[2], signal.toString());
    }

    @Override
    public float[] getColor() {
        return new float[]{0.0F, 0.0F, 0.0F};
    }
}
