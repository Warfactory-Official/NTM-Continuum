package com.hbm.tileentity;

import com.hbm.Constants;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.blocks.ModEnergyNetBlocks;
import com.hbm.tileentity.machine.TileEntityBatteryTier;
import com.hbm.tileentity.machine.TileEntityMachineDetector;
import com.hbm.tileentity.machine.TileEntityTesla;
import com.hbm.tileentity.network.TileEntityCableClassic;
import com.hbm.tileentity.network.TileEntityCableGauge;
import com.hbm.tileentity.network.TileEntityCablePaintable;
import com.hbm.tileentity.network.TileEntityCableSwitch;
import com.hbm.tileentity.network.TileEntityCapacitor;
import com.hbm.tileentity.network.TileEntityConnector;
import com.hbm.tileentity.network.TileEntityConnectorSuper;
import com.hbm.tileentity.network.TileEntityDiode;
import com.hbm.tileentity.network.TileEntityHEVBattery;
import com.hbm.tileentity.network.TileEntityPylon;
import com.hbm.tileentity.network.TileEntityPylonLarge;
import com.hbm.tileentity.network.TileEntityPylonMedium;
import com.hbm.tileentity.network.TileEntitySubstation;

/** Feature-local block-entity types for the energy-network family. */
public final class ModEnergyNetBlockEntities {

    private ModEnergyNetBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    public static final RegistryObject<BlockEntityType<TileEntityPylon>> PYLON =
            BLOCK_ENTITIES.register("red_pylon", () -> BlockEntities.type(TileEntityPylon::new,
                    ModEnergyNetBlocks.red_pylon.get(), ModEnergyNetBlocks.red_pylon_steel.get()));
    public static final RegistryObject<BlockEntityType<TileEntityPylonMedium>> PYLON_MEDIUM =
            BLOCK_ENTITIES.register("red_pylon_medium", () -> BlockEntities.type(TileEntityPylonMedium::new,
                    ModEnergyNetBlocks.red_pylon_medium_wood.get(), ModEnergyNetBlocks.red_pylon_medium_wood_transformer.get(),
                    ModEnergyNetBlocks.red_pylon_medium_steel.get(), ModEnergyNetBlocks.red_pylon_medium_steel_transformer.get()));
    public static final RegistryObject<BlockEntityType<TileEntityPylonLarge>> PYLON_LARGE =
            BLOCK_ENTITIES.register("red_pylon_large", () -> BlockEntities.type(TileEntityPylonLarge::new,
                    ModEnergyNetBlocks.red_pylon_large.get()));
    public static final RegistryObject<BlockEntityType<TileEntitySubstation>> SUBSTATION =
            BLOCK_ENTITIES.register("substation", () -> BlockEntities.type(TileEntitySubstation::new,
                    ModEnergyNetBlocks.substation.get()));
    public static final RegistryObject<BlockEntityType<TileEntityConnector>> CONNECTOR =
            BLOCK_ENTITIES.register("red_connector", () -> BlockEntities.type(TileEntityConnector::new,
                    ModEnergyNetBlocks.red_connector.get()));
    public static final RegistryObject<BlockEntityType<TileEntityConnectorSuper>> CONNECTOR_SUPER =
            BLOCK_ENTITIES.register("red_connector_super", () -> BlockEntities.type(TileEntityConnectorSuper::new,
                    ModEnergyNetBlocks.red_connector_super.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCableClassic>> RED_CABLE_CLASSIC =
            BLOCK_ENTITIES.register("red_cable_classic", () -> BlockEntities.type(TileEntityCableClassic::new,
                    ModEnergyNetBlocks.red_cable_classic.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCablePaintable>> RED_CABLE_PAINTABLE =
            BLOCK_ENTITIES.register("red_cable_paintable", () -> BlockEntities.type(TileEntityCablePaintable::new,
                    ModEnergyNetBlocks.red_cable_paintable.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCableGauge>> CABLE_GAUGE =
            BLOCK_ENTITIES.register("red_cable_gauge", () -> BlockEntities.serverTicking(TileEntityCableGauge::new,
                    TileEntityCableGauge::serverTick, ModEnergyNetBlocks.red_cable_gauge.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCableSwitch>> CABLE_SWITCH =
            BLOCK_ENTITIES.register("cable_switch", () -> BlockEntities.type(TileEntityCableSwitch::new,
                    ModEnergyNetBlocks.cable_switch.get(), ModEnergyNetBlocks.cable_detector.get()));
    public static final RegistryObject<BlockEntityType<TileEntityDiode>> CABLE_DIODE =
            BLOCK_ENTITIES.register("cable_diode", () -> BlockEntities.serverTicking(TileEntityDiode::new,
                    TileEntityDiode::serverTick, ModEnergyNetBlocks.cable_diode.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineDetector>> MACHINE_DETECTOR =
            BLOCK_ENTITIES.register("machine_detector", () -> BlockEntities.serverTicking(TileEntityMachineDetector::new,
                    TileEntityMachineDetector::serverTick, ModEnergyNetBlocks.machine_detector.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCapacitor>> CAPACITOR =
            BLOCK_ENTITIES.register("capacitor", () -> BlockEntities.serverTicking(TileEntityCapacitor::new,
                    TileEntityCapacitor::serverTick, ModEnergyNetBlocks.capacitor_tantalium.get(),
                    ModEnergyNetBlocks.capacitor_schrabidate.get()));
    public static final RegistryObject<BlockEntityType<TileEntityBatteryTier>> BATTERY_TIER =
            BLOCK_ENTITIES.register("machine_battery_tier", () -> BlockEntities.serverTicking(TileEntityBatteryTier::new,
                    com.hbm.tileentity.machine.TileEntityMachineBattery::serverTick,
                    ModEnergyNetBlocks.machine_battery_potato.get(), ModEnergyNetBlocks.machine_lithium_battery.get(),
                    ModEnergyNetBlocks.machine_schrabidium_battery.get(), ModEnergyNetBlocks.machine_dineutronium_battery.get()));
    public static final RegistryObject<BlockEntityType<TileEntityHEVBattery>> HEV_BATTERY =
            BLOCK_ENTITIES.register("hev_battery", () -> BlockEntities.type(TileEntityHEVBattery::new,
                    ModEnergyNetBlocks.hev_battery.get()));
    public static final RegistryObject<BlockEntityType<TileEntityTesla>> TESLA =
            BLOCK_ENTITIES.register("tesla", () -> BlockEntities.serverTicking(TileEntityTesla::new,
                    TileEntityTesla::serverTick, ModEnergyNetBlocks.tesla.get()));

    public static void init() {}
}
