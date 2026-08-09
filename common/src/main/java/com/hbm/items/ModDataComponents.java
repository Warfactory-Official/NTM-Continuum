package com.hbm.items;

import com.hbm.Constants;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;

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

    public static void init() {}
}
