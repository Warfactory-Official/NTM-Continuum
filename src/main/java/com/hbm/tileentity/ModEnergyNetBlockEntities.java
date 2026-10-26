package com.hbm.tileentity;

import com.hbm.Constants;
import com.hbm.registration.Reg;
import com.hbm.tileentity.machine.TileEntityMachineBattery;
import com.hbm.tileentity.machine.storage.TileEntityMachineCapacitor;
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
import com.hbm.tileentity.network.TileEntityConnector;
import com.hbm.tileentity.network.TileEntityConnectorSuper;
import com.hbm.tileentity.network.TileEntityDiode;
import com.hbm.tileentity.network.TileEntityHEVBattery;
import com.hbm.tileentity.network.TileEntityNetworkSeparator;
import com.hbm.tileentity.network.TileEntityPylon;
import com.hbm.tileentity.network.TileEntityPylonLarge;
import com.hbm.tileentity.network.TileEntityPylonMedium;
import com.hbm.tileentity.network.TileEntitySubstation;

/** Feature-local block-entity types for the energy-network family. */
public final class ModEnergyNetBlockEntities {

    private ModEnergyNetBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCK_ENTITIES);

    public static final RegistryObject<BlockEntityType<TileEntityPylon>> PYLON =
            REG.blockEntity("red_pylon", () -> BlockEntities.type(TileEntityPylon::new,
                    ModEnergyNetBlocks.red_pylon.get(), ModEnergyNetBlocks.red_pylon_steel.get()));
    public static final RegistryObject<BlockEntityType<TileEntityPylonMedium>> PYLON_MEDIUM =
            REG.blockEntity("red_pylon_medium", () -> BlockEntities.type(TileEntityPylonMedium::new,
                    ModEnergyNetBlocks.red_pylon_medium_wood.get(), ModEnergyNetBlocks.red_pylon_medium_wood_transformer.get(),
                    ModEnergyNetBlocks.red_pylon_medium_steel.get(), ModEnergyNetBlocks.red_pylon_medium_steel_transformer.get()));
    public static final RegistryObject<BlockEntityType<TileEntityPylonLarge>> PYLON_LARGE =
            REG.blockEntity("red_pylon_large", () -> BlockEntities.type(TileEntityPylonLarge::new,
                    ModEnergyNetBlocks.red_pylon_large.get()));
    public static final RegistryObject<BlockEntityType<TileEntitySubstation>> SUBSTATION =
            REG.blockEntity("substation", () -> BlockEntities.type(TileEntitySubstation::new,
                    ModEnergyNetBlocks.substation.get()));
    public static final RegistryObject<BlockEntityType<TileEntityConnector>> CONNECTOR =
            REG.blockEntity("red_connector", () -> BlockEntities.type(TileEntityConnector::new,
                    ModEnergyNetBlocks.red_connector.get()));
    public static final RegistryObject<BlockEntityType<TileEntityConnectorSuper>> CONNECTOR_SUPER =
            REG.blockEntity("red_connector_super", () -> BlockEntities.type(TileEntityConnectorSuper::new,
                    ModEnergyNetBlocks.red_connector_super.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCableClassic>> RED_CABLE_CLASSIC =
            REG.blockEntity("red_cable_classic", () -> BlockEntities.type(TileEntityCableClassic::new,
                    ModEnergyNetBlocks.red_cable_classic.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCablePaintable>> RED_CABLE_PAINTABLE =
            REG.blockEntity("red_cable_paintable", () -> BlockEntities.type(TileEntityCablePaintable::new,
                    ModEnergyNetBlocks.red_cable_paintable.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCableGauge>> CABLE_GAUGE =
            REG.blockEntity("red_cable_gauge", () -> BlockEntities.serverTicking(TileEntityCableGauge::new,
                    TileEntityCableGauge::serverTick, ModEnergyNetBlocks.red_cable_gauge.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCableSwitch>> CABLE_SWITCH =
            REG.blockEntity("cable_switch", () -> BlockEntities.type(TileEntityCableSwitch::new,
                    ModEnergyNetBlocks.cable_switch.get(), ModEnergyNetBlocks.cable_detector.get()));
    public static final RegistryObject<BlockEntityType<TileEntityDiode>> CABLE_DIODE =
            REG.blockEntity("cable_diode", () -> BlockEntities.serverTicking(TileEntityDiode::new,
                    TileEntityDiode::serverTick, ModEnergyNetBlocks.cable_diode.get()));
    public static final RegistryObject<BlockEntityType<TileEntityNetworkSeparator>> NETWORK_SEPARATOR =
            REG.blockEntity("network_separator", () -> BlockEntities.serverTicking(TileEntityNetworkSeparator::new,
                    TileEntityNetworkSeparator::serverTick, ModEnergyNetBlocks.network_separator.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineDetector>> MACHINE_DETECTOR =
            REG.blockEntity("machine_detector", () -> BlockEntities.serverTicking(TileEntityMachineDetector::new,
                    TileEntityMachineDetector::serverTick, ModEnergyNetBlocks.machine_detector.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineCapacitor>> CAPACITOR =
            REG.blockEntity("capacitor", () -> BlockEntities.serverTicking(TileEntityMachineCapacitor::new,
                    TileEntityMachineCapacitor::serverTick, ModEnergyNetBlocks.machine_capacitor_copper.get()));

    public static final RegistryObject<BlockEntityType<TileEntityBatteryTier>> BATTERY_TIER =
            REG.blockEntity("machine_battery_tier", () -> BlockEntities.serverTicking(TileEntityBatteryTier::new,
                    TileEntityMachineBattery::serverTick,
                    ModEnergyNetBlocks.machine_battery_potato.get(), ModEnergyNetBlocks.machine_lithium_battery.get(),
                    ModEnergyNetBlocks.machine_schrabidium_battery.get(), ModEnergyNetBlocks.machine_dineutronium_battery.get()));
    public static final RegistryObject<BlockEntityType<TileEntityHEVBattery>> HEV_BATTERY =
            REG.blockEntity("hev_battery", () -> BlockEntities.type(TileEntityHEVBattery::new,
                    ModEnergyNetBlocks.hev_battery.get()));
    public static final RegistryObject<BlockEntityType<TileEntityTesla>> TESLA =
            REG.blockEntity("tesla", () -> BlockEntities.serverTicking(TileEntityTesla::new,
                    TileEntityTesla::serverTick, ModEnergyNetBlocks.tesla.get()));

    public static void init() {}
}
