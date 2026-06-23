package com.hbm.items;

import com.hbm.Constants;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModDataComponents {

    private ModDataComponents() {}

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Constants.MOD_ID);

    public static final RegistryObject<DataComponentType<Integer>> FLUID_ID =
            COMPONENTS.register("fluid_id", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    public static final RegistryObject<DataComponentType<Integer>> FLUID_AMOUNT =
            COMPONENTS.register("fluid_amount", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    public static final RegistryObject<DataComponentType<String>> MATERIAL =
            COMPONENTS.register("material", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static final RegistryObject<DataComponentType<Long>> BATTERY_CHARGE =
            COMPONENTS.register("battery_charge", () -> DataComponentType.<Long>builder()
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG)
                    .build());

    public static void init() {}
}
