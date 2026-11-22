package fissionworks;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** CO2 and super dense steam. Gases: no block and no bucket, they only live in tanks and pipes. */
public final class ModFluids {
    public static final DeferredRegister<FluidType> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, FissionWorks.ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, FissionWorks.ID);

    public static final DeferredHolder<FluidType, FluidType> CO2_TYPE = TYPES.register("co2", ModFluids::gas);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> CO2 =
            FLUIDS.register("co2", () -> new BaseFlowingFluid.Source(co2()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> CO2_FLOWING =
            FLUIDS.register("co2_flowing", () -> new BaseFlowingFluid.Flowing(co2()));

    public static final DeferredHolder<FluidType, FluidType> STEAM_TYPE =
            TYPES.register("super_dense_steam", ModFluids::gas);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> STEAM =
            FLUIDS.register("super_dense_steam", () -> new BaseFlowingFluid.Source(steam()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> STEAM_FLOWING =
            FLUIDS.register("super_dense_steam_flowing", () -> new BaseFlowingFluid.Flowing(steam()));

    private ModFluids() {}

    private static FluidType gas() {
        return new FluidType(FluidType.Properties.create().density(-100).viscosity(100).temperature(400)
                .canSwim(false).canDrown(false).canPushEntity(false));
    }

    private static BaseFlowingFluid.Properties co2() {
        return new BaseFlowingFluid.Properties(CO2_TYPE, CO2, CO2_FLOWING);
    }

    private static BaseFlowingFluid.Properties steam() {
        return new BaseFlowingFluid.Properties(STEAM_TYPE, STEAM, STEAM_FLOWING);
    }
}
