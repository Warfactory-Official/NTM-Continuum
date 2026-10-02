package com.hbm.tileentity;

import com.hbm.Constants;
import com.hbm.registration.Reg;
import com.hbm.blocks.ModEnergyBedrockBlocks;
import com.hbm.tileentity.machine.TileEntityCharger;
import com.hbm.tileentity.network.TileEntityCableBox;
import com.hbm.tileentity.network.TileEntityConverterHeRf;
import com.hbm.tileentity.network.TileEntityConverterRfHe;
import com.hbm.tileentity.network.TileEntityWireCoated;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Block-entity types of the energy extras (cable box, coated wire, charging station, RF converters). */
public final class ModEnergyBedrockBlockEntities {

    private ModEnergyBedrockBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCK_ENTITIES);

    public static final RegistryObject<BlockEntityType<TileEntityCableBox>> ENERGY_CABLE_BOX =
            REG.blockEntity("red_cable_box", () -> BlockEntities.type(TileEntityCableBox::new,
                    ModEnergyBedrockBlocks.cableBoxes()));
    public static final RegistryObject<BlockEntityType<TileEntityWireCoated>> ENERGY_WIRE_COATED =
            REG.blockEntity("red_wire_coated", () -> BlockEntities.type(TileEntityWireCoated::new,
                    ModEnergyBedrockBlocks.red_wire_coated.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCharger>> CHARGER =
            REG.blockEntity("charger", () -> BlockEntities.ticking(TileEntityCharger::new,
                    TileEntityCharger::clientTick, TileEntityCharger::serverTick, ModEnergyBedrockBlocks.charger.get()));
    public static final RegistryObject<BlockEntityType<TileEntityConverterHeRf>> CONVERTER_HE_RF =
            REG.blockEntity("machine_converter_he_rf", () -> BlockEntities.serverTicking(TileEntityConverterHeRf::new,
                    TileEntityConverterHeRf::serverTick, ModEnergyBedrockBlocks.machine_converter_he_rf.get()));
    public static final RegistryObject<BlockEntityType<TileEntityConverterRfHe>> CONVERTER_RF_HE =
            REG.blockEntity("machine_converter_rf_he", () -> BlockEntities.serverTicking(TileEntityConverterRfHe::new,
                    TileEntityConverterRfHe::serverTick, ModEnergyBedrockBlocks.machine_converter_rf_he.get()));

    public static void init() {}
}
