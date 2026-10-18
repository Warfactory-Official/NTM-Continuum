package com.hbm.uninos.networkproviders;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.DirPos;
import com.hbm.uninos.INetworkProvider;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 1.7 gave each {@link FluidType} one provider, so a port's type is its fluid. */
public final class FluidNetProvider implements INetworkProvider {

    private static final Map<FluidType, FluidNetProvider> PROVIDERS = new ConcurrentHashMap<>();

    public final FluidType type;

    private FluidNetProvider(FluidType type) {
        this.type = type;
    }

    /** 1.7 {@code FluidType.getNetworkProvider}. */
    public static FluidNetProvider of(FluidType type) {
        FluidNetProvider provider = PROVIDERS.get(type);
        return provider != null ? provider : PROVIDERS.computeIfAbsent(type, FluidNetProvider::new);
    }

    @Override
    public void spawnDebugParticles(ServerLevel level, DirPos pos, boolean receive) {
        PortDebugParticles.spawn(level, pos, receive, "fluid", type.getColor());
    }

    @Override
    public String toString() {
        return type.getRegistryName();
    }
}
