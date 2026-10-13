package com.hbm.api.data;

import com.hbm.Constants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.data.DataMaps;
import dev.golem.core.data.DataMaps.DataMap;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

/**
 * Per-fluid values outside {@code fluid_property}, as fluid data maps ({@code data/<ns>/value_maps/fluid/...}; every HBM
 * fluid is a vanilla fluid {@code hbm:<key>}, water and lava are vanilla's).
 */
public final class FluidDataMaps {

    private FluidDataMaps() {}

    private static <V> DataMap<Fluid, V> fluid(String name, Codec<V> codec) {
        return DataMaps.map(FluidDataMaps.class, Constants.MOD_ID, name, "fluid", () -> BuiltInRegistries.FLUID, codec);
    }

    /**
     * Which portable containers a fluid comes in and their colours (1.7 {@code FluidType.addContainers}:
     * {@code CD_Canister(color)}, {@code CD_Gastank(bottleColor, labelColor)}).
     */
    public static final DataMap<Fluid, Containers> FLUID_CONTAINERS = fluid("fluid_containers", Containers.CODEC);

    public record Containers(Optional<Integer> canister, Optional<Integer> gasBottle, Optional<Integer> gasLabel) {
        public static final Codec<Containers> CODEC = RecordCodecBuilder.create(i -> i.group(
                ReactorDataMaps.COLOR.optionalFieldOf("canister").forGetter(Containers::canister),
                ReactorDataMaps.COLOR.optionalFieldOf("gas_bottle").forGetter(Containers::gasBottle),
                ReactorDataMaps.COLOR.optionalFieldOf("gas_label").forGetter(Containers::gasLabel)
        ).apply(i, Containers::new));
    }

    /** Fluids the autosaw and the thresher burn ({@code true}; 1.7 {@code TileEntityMachineAutosaw.acceptedFuels}). */
    public static final DataMap<Fluid, Boolean> AUTOSAW_FUEL = fluid("autosaw_fuel", Codec.BOOL);

    /**
     * Gas turbine: the fuel it burns per tick at full throttle, in mB (1.7 {@code TileEntityMachineTurbineGas.fuelMaxCons}:
     * natural gas 50, syngas 10, oxyhydrogen 100, reform gas 5). A fuel without a value burns 5 (1.7's default, in code).
     */
    public static final DataMap<Fluid, Double> GAS_TURBINE_FUEL = fluid("gas_turbine_fuel_consumption", Codec.DOUBLE);

    /**
     * A fixed fluid container (1.7 {@code FluidContainerRegistry}), keyed by the full item: {@code amount} mB of
     * {@code fluid} (a fluid registry name), and the item crafting leaves ({@code empty}; absent when the item is
     * used up).
     */
    public static final DataMap<Item, Container> FLUID_CONTAINER = DataMaps.map(FluidDataMaps.class,
            Constants.MOD_ID, "fluid_container", "item", () -> BuiltInRegistries.ITEM, Container.CODEC);

    public record Container(String fluid, int amount, Optional<String> empty) {
        public static final Codec<Container> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("fluid").forGetter(Container::fluid),
                Codec.INT.fieldOf("amount").forGetter(Container::amount),
                Codec.STRING.optionalFieldOf("empty").forGetter(Container::empty)
        ).apply(i, Container::new));
    }

    /** Loads the class at mod construction (from {@link HbmDataMaps#declare()}). */
    public static void declare() {}
}
