package com.hbm.tileentity.machine.solar;

import com.hbm.Constants;
import com.hbm.blocks.machine.solar.ModSolarBlocks;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.registration.Reg;

/** Solar block-entity register; the central bootstrap must load this after {@link ModSolarBlocks}. */
public final class ModSolarBlockEntities {

    private ModSolarBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCK_ENTITIES);

    public static final RegistryObject<BlockEntityType<TileEntitySolarBoiler>> SOLAR_BOILER =
            REG.blockEntity("machine_solar_boiler", () -> BlockEntities.serverTicking(TileEntitySolarBoiler::new, TileEntitySolarBoiler::serverTick, ModSolarBlocks.machine_solar_boiler.get()));

    public static final RegistryObject<BlockEntityType<TileEntitySolarMirror>> SOLAR_MIRROR =
            REG.blockEntity("solar_mirror", () -> BlockEntities.serverTicking(TileEntitySolarMirror::new, TileEntitySolarMirror::serverTick, ModSolarBlocks.solar_mirror.get()));

    public static void init() {}
}
