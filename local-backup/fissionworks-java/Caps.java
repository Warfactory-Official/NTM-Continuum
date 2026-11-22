package fissionworks;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Exposes the ZIRNOX fluid ports to pipes and pumps, including Create's. */
@EventBusSubscriber(modid = FissionWorks.ID, bus = EventBusSubscriber.Bus.MOD)
public final class Caps {
    private Caps() {}

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FissionWorks.ZIRNOX_BE.get(),
                (be, side) -> be.port(side));
    }
}
