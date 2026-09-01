package com.hbm.tileentity.machine.solar;

import com.hbm.Constants;
import com.hbm.blocks.machine.solar.ModSolarBlocks;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Solar block-entity register; the central bootstrap must load this after {@link ModSolarBlocks}. */
public final class ModSolarBlockEntities {

    private ModSolarBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    public static final RegistryObject<BlockEntityType<TileEntitySolarBoiler>> SOLAR_BOILER =
            BLOCK_ENTITIES.register("machine_solar_boiler", () -> BlockEntities.type(
                    TileEntitySolarBoiler::new, ModSolarBlocks.machine_solar_boiler.get()));

    public static final RegistryObject<BlockEntityType<TileEntitySolarMirror>> SOLAR_MIRROR =
            BLOCK_ENTITIES.register("solar_mirror", () -> BlockEntities.type(
                    TileEntitySolarMirror::new, ModSolarBlocks.solar_mirror.get()));

    public static void init() {}
}
