package com.hbm.tileentity;

import com.hbm.Constants;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.platform.Services;
import com.hbm.platform.services.BlockEntityFactory;
import com.hbm.tileentity.machine.rbmk.*;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** RBMK types are deliberately isolated until the complete reactor block wave is wired into bootstrap. */
public final class ModRBMKBlockEntities {
    private ModRBMKBlockEntities() {}
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> RegistryObject<BlockEntityType<T>> type(String id, BlockEntityFactory<T> factory, RegistryObject<? extends net.minecraft.world.level.block.Block>... blocks){return BLOCK_ENTITIES.register(id,()->Services.PLATFORM.blockEntityType(factory,java.util.Arrays.stream(blocks).map(RegistryObject::get).toArray(net.minecraft.world.level.block.Block[]::new)));}
    public static final RegistryObject<BlockEntityType<BlockEntityRBMKRod>> RBMK_ROD=type("rbmk_fuel_rod",BlockEntityRBMKRod::new,ModRBMKBlocks.rbmk_fuel_rod,ModRBMKBlocks.rbmk_fuel_rod_moderated);
    public static final RegistryObject<BlockEntityType<BlockEntityRBMKRodReaSim>> RBMK_ROD_REASIM=type("rbmk_fuel_rod_reasim",BlockEntityRBMKRodReaSim::new,ModRBMKBlocks.rbmk_fuel_rod_reasim,ModRBMKBlocks.rbmk_fuel_rod_reasim_moderated);
    public static final RegistryObject<BlockEntityType<BlockEntityRBMKControlManual>> RBMK_CONTROL=type("rbmk_control",BlockEntityRBMKControlManual::new,ModRBMKBlocks.rbmk_control,ModRBMKBlocks.rbmk_control_mod,ModRBMKBlocks.rbmk_control_reasim);
    public static final RegistryObject<BlockEntityType<BlockEntityRBMKControlAuto>> RBMK_CONTROL_AUTO=type("rbmk_control_auto",BlockEntityRBMKControlAuto::new,ModRBMKBlocks.rbmk_control_auto,ModRBMKBlocks.rbmk_control_reasim_auto);
    public static final RegistryObject<BlockEntityType<BlockEntityRBMKBoiler>> RBMK_BOILER=type("rbmk_boiler",BlockEntityRBMKBoiler::new,ModRBMKBlocks.rbmk_boiler);
    public static final RegistryObject<BlockEntityType<BlockEntityRBMKModerator>> RBMK_MODERATOR=type("rbmk_moderator",BlockEntityRBMKModerator::new,ModRBMKBlocks.rbmk_moderator);
    public static final RegistryObject<BlockEntityType<BlockEntityRBMKReflector>> RBMK_REFLECTOR=type("rbmk_reflector",BlockEntityRBMKReflector::new,ModRBMKBlocks.rbmk_reflector);
    public static final RegistryObject<BlockEntityType<BlockEntityRBMKAbsorber>> RBMK_ABSORBER=type("rbmk_absorber",BlockEntityRBMKAbsorber::new,ModRBMKBlocks.rbmk_absorber);
    public static final RegistryObject<BlockEntityType<BlockEntityRBMKBlank>> RBMK_BLANK=type("rbmk_blank",BlockEntityRBMKBlank::new,ModRBMKBlocks.rbmk_blank);
    public static final RegistryObject<BlockEntityType<BlockEntityRBMKCooler>> RBMK_COOLER=type("rbmk_cooler",BlockEntityRBMKCooler::new,ModRBMKBlocks.rbmk_cooler);
    public static final RegistryObject<BlockEntityType<BlockEntityRBMKStorage>> RBMK_STORAGE=type("rbmk_storage",BlockEntityRBMKStorage::new,ModRBMKBlocks.rbmk_storage);
    public static final RegistryObject<BlockEntityType<BlockEntityRBMKHeater>> RBMK_HEATER=type("rbmk_heater",BlockEntityRBMKHeater::new,ModRBMKBlocks.rbmk_heater);
    public static final RegistryObject<BlockEntityType<BlockEntityRBMKOutgasser>> RBMK_OUTGASSER=type("rbmk_outgasser",BlockEntityRBMKOutgasser::new,ModRBMKBlocks.rbmk_outgasser);
    public static final RegistryObject<BlockEntityType<BlockEntityRBMKInlet>> RBMK_INLET=type("rbmk_steam_inlet",BlockEntityRBMKInlet::new,ModRBMKBlocks.rbmk_steam_inlet);
    public static final RegistryObject<BlockEntityType<BlockEntityRBMKOutlet>> RBMK_OUTLET=type("rbmk_steam_outlet",BlockEntityRBMKOutlet::new,ModRBMKBlocks.rbmk_steam_outlet);
    public static final RegistryObject<BlockEntityType<BlockEntityRBMKConsole>> RBMK_CONSOLE=type("rbmk_console",BlockEntityRBMKConsole::new,ModRBMKBlocks.rbmk_console);
    public static final RegistryObject<BlockEntityType<BlockEntityCraneConsole>> RBMK_CRANE_CONSOLE=type("rbmk_crane_console",BlockEntityCraneConsole::new,ModRBMKBlocks.rbmk_crane_console);
    public static final RegistryObject<BlockEntityType<BlockEntityRBMKGauge>> RBMK_GAUGE=type("rbmk_gauge",BlockEntityRBMKGauge::new,ModRBMKBlocks.rbmk_gauge);
    public static final RegistryObject<BlockEntityType<BlockEntityRBMKDebris>> RBMK_DEBRIS=type("rbmk_debris",BlockEntityRBMKDebris::new,ModRBMKBlocks.rbmk_debris,ModRBMKBlocks.rbmk_debris_burning,ModRBMKBlocks.rbmk_debris_radiating,ModRBMKBlocks.rbmk_debris_digamma);
    public static void init() {}
}
