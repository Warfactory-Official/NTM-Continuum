package com.hbm.uninos.networkproviders;

import com.hbm.lib.DirPos;
import com.hbm.uninos.INetworkProvider;
import net.minecraft.server.level.ServerLevel;

public final class PowerNetProvider implements INetworkProvider {

    public static final PowerNetProvider THE_PROVIDER = new PowerNetProvider();

    private PowerNetProvider() {}

    @Override
    public void spawnDebugParticles(ServerLevel level, DirPos pos, boolean receive) {
        PortDebugParticles.spawn(level, pos, receive, "power", -1);
    }

    @Override
    public String toString() {
        return "power";
    }
}
