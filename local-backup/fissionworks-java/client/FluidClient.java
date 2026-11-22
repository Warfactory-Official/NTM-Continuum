package fissionworks.client;

import fissionworks.FissionWorks;
import fissionworks.ModFluids;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Gives the two gases a texture and tint so pipes and tanks can draw them. */
@EventBusSubscriber(modid = FissionWorks.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FluidClient {
    private FluidClient() {}

    private static IClientFluidTypeExtensions look(String texture, int tint) {
        ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(FissionWorks.ID, "block/" + texture);
        return new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return tex;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return tex;
            }

            @Override
            public int getTintColor() {
                return tint;
            }
        };
    }

    @SubscribeEvent
    public static void register(RegisterClientExtensionsEvent event) {
        event.registerFluidType(look("fluid_co2", 0xFFB8C4D0), ModFluids.CO2_TYPE.get());
        event.registerFluidType(look("fluid_steam", 0xFFE6F2FF), ModFluids.STEAM_TYPE.get());
    }
}
