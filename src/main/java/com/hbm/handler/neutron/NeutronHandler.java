package com.hbm.handler.neutron;

import com.hbm.handler.neutron.NeutronNodeWorld.StreamWorld;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

/** Server-tick driver for queued neutron streams. */
public final class NeutronHandler {
    private static int ticks;
    private NeutronHandler() {}
    public static void init() {}
    public static void onServerTick(MinecraftServer server) {
        boolean clear = ticks >= 20;
        if (clear) ticks = 0;
        ticks++;
        NeutronNodeWorld.removeEmptyWorlds();
        for (Map.Entry<Level, StreamWorld> entry : NeutronNodeWorld.streamWorlds.entrySet()) {
            Level world = entry.getKey();
            RBMKNeutronHandler.reflectorEfficiency = RBMKDials.getReflectorEfficiency(world);
            RBMKNeutronHandler.absorberEfficiency = RBMKDials.getAbsorberEfficiency(world);
            RBMKNeutronHandler.moderatorEfficiency = RBMKDials.getModeratorEfficiency(world);
            RBMKNeutronHandler.columnHeight = RBMKDials.getColumnHeight(world) + 1;
            RBMKNeutronHandler.fluxRange = RBMKDials.getFluxRange(world);
            entry.getValue().runStreamInteractions(world);
            entry.getValue().removeAllStreams();
            if (clear) entry.getValue().cleanNodes();
        }
    }
}
