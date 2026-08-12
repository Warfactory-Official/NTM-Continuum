package com.hbm.items;

import com.hbm.Constants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;

/** HBM's custom item data components (the 26.1 replacement for item NBT / metadata). */
public final class ModDataComponents {

    private ModDataComponents() {}

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Constants.MOD_ID);

    /** The {@link com.hbm.inventory.fluid.FluidType} ID carried by a fluid-identifier item. */
    public static final RegistryObject<DataComponentType<Integer>> FLUID_ID =
            COMPONENTS.register("fluid_id", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    /** The fluid amount (mB) held by a variable-fill fluid container (universal fluid tank / barrel). The
     * {@link #FLUID_ID} component carries which fluid; this carries how much. */
    public static final RegistryObject<DataComponentType<Integer>> FLUID_AMOUNT =
            COMPONENTS.register("fluid_amount", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    /** The material (snake-case name) carried by an autogen metal-shape item
     * ({@link com.hbm.items.special.ItemAutogen}) — the 26.1 replacement for its old metadata=material-id. */
    public static final RegistryObject<DataComponentType<String>> MATERIAL =
            COMPONENTS.register("material", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    /** The material amount (quanta, 1/72 ingot) held by a foundry scraps item
     * ({@link com.hbm.items.machine.ItemScraps}) — the {@link #MATERIAL} component carries which material. */
    public static final RegistryObject<DataComponentType<Integer>> MAT_AMOUNT =
            COMPONENTS.register("mat_amount", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    /** Stored energy (HE) carried by an {@link com.hbm.api.energymk2.IBatteryItem} (the old NBT "charge"). */
    public static final RegistryObject<DataComponentType<Long>> BATTERY_CHARGE =
            COMPONENTS.register("battery_charge", () -> DataComponentType.<Long>builder()
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG)
                    .build());

    /** Extra radiation (RAD/s) attached to an arbitrary stack — the old {@code hfrHazRadiation} NBT float
     * read by {@code HazardTransformerRadiationNBT}. */
    public static final RegistryObject<DataComponentType<Float>> HAZARD_RADIATION =
            COMPONENTS.register("hazard_radiation", () -> DataComponentType.<Float>builder()
                    .persistent(Codec.FLOAT)
                    .networkSynchronized(ByteBufCodecs.FLOAT)
                    .build());

    /** Number of neutron-exposure operations already performed by an active ZIRNOX fuel rod. */
    public static final RegistryObject<DataComponentType<Integer>> ZIRNOX_LIFE =
            COMPONENTS.register("zirnox_life", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    /** Whether one of the nine ZIRNOX spent-fuel item stacks still requires pool cooling. */
    public static final RegistryObject<DataComponentType<Boolean>> FUEL_WASTE_HOT =
            COMPONENTS.register("fuel_waste_hot", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    /** The single filter installed in an {@link com.hbm.api.item.IGasMask}. */
    public static final RegistryObject<DataComponentType<ItemStack>> GAS_MASK_FILTER =
            COMPONENTS.register("gas_mask_filter", () -> DataComponentType.<ItemStack>builder()
                    .persistent(ItemStack.CODEC)
                    .networkSynchronized(ItemStack.OPTIONAL_STREAM_CODEC)
                    .build());

    public static void init() {}
}
