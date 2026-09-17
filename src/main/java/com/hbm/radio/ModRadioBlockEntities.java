package com.hbm.radio;

import com.hbm.Constants;
import com.hbm.tileentity.network.TileEntityRadioAUTOCAL;
import com.hbm.tileentity.network.TileEntityRadioTelex;
import com.hbm.tileentity.network.TileEntityRadioTorchController;
import com.hbm.tileentity.network.TileEntityRadioTorchCounter;
import com.hbm.tileentity.network.TileEntityRadioTorchReceiver;
import com.hbm.tileentity.network.TileEntityRadioTorchSender;
import com.hbm.tileentity.network.TileEntityRadioTorchReader;
import com.hbm.tileentity.network.TileEntityRadioTorchLogic;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Feature-local block-entity types for Redstone over Radio. */
public final class ModRadioBlockEntities {
    private ModRadioBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    public static final RegistryObject<BlockEntityType<TileEntityRadioTorchSender>> RADIO_TORCH_SENDER =
            BLOCK_ENTITIES.register("radio_torch_sender", () -> BlockEntities.serverTicking(
                    TileEntityRadioTorchSender::new,
                    (level, pos, state, be) -> be.serverTick(), ModRadioBlocks.RADIO_TORCH_SENDER.get()));
    public static final RegistryObject<BlockEntityType<TileEntityRadioTorchReceiver>> RADIO_TORCH_RECEIVER =
            BLOCK_ENTITIES.register("radio_torch_receiver", () -> BlockEntities.serverTicking(
                    TileEntityRadioTorchReceiver::new,
                    (level, pos, state, be) -> be.serverTick(), ModRadioBlocks.RADIO_TORCH_RECEIVER.get()));
    public static final RegistryObject<BlockEntityType<TileEntityRadioTorchReader>> RADIO_TORCH_READER =
            BLOCK_ENTITIES.register("radio_torch_reader", () -> BlockEntities.serverTicking(
                    TileEntityRadioTorchReader::new,
                    (level, pos, state, be) -> be.serverTick(), ModRadioBlocks.RADIO_TORCH_READER.get()));
    public static final RegistryObject<BlockEntityType<TileEntityRadioTorchLogic>> RADIO_TORCH_LOGIC =
            BLOCK_ENTITIES.register("radio_torch_logic", () -> BlockEntities.serverTicking(
                    TileEntityRadioTorchLogic::new,
                    (level, pos, state, be) -> be.serverTick(), ModRadioBlocks.RADIO_TORCH_LOGIC.get()));
    public static final RegistryObject<BlockEntityType<TileEntityRadioTorchCounter>> RADIO_TORCH_COUNTER =
            BLOCK_ENTITIES.register("radio_torch_counter", () -> BlockEntities.serverTicking(
                    TileEntityRadioTorchCounter::new,
                    (level, pos, state, be) -> be.serverTick(), ModRadioBlocks.RADIO_TORCH_COUNTER.get()));
    public static final RegistryObject<BlockEntityType<TileEntityRadioTorchController>> RADIO_TORCH_CONTROLLER =
            BLOCK_ENTITIES.register("radio_torch_controller", () -> BlockEntities.serverTicking(
                    TileEntityRadioTorchController::new,
                    (level, pos, state, be) -> be.serverTick(), ModRadioBlocks.RADIO_TORCH_CONTROLLER.get()));
    public static final RegistryObject<BlockEntityType<TileEntityRadioTelex>> RADIO_TELEX =
            BLOCK_ENTITIES.register("radio_telex", () -> BlockEntities.serverTicking(
                    TileEntityRadioTelex::new,
                    (level, pos, state, be) -> be.serverTick(), ModRadioBlocks.RADIO_TELEX.get()));
    public static final RegistryObject<BlockEntityType<TileEntityRadioAUTOCAL>> RADIO_AUTOCAL =
            BLOCK_ENTITIES.register("radio_autocal", () -> BlockEntities.serverTicking(
                    TileEntityRadioAUTOCAL::new,
                    (level, pos, state, be) -> be.serverTick(), ModRadioBlocks.RADIO_AUTOCAL.get()));

    public static void init() {}
}
