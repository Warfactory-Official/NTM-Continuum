package com.hbm.radio;

import com.hbm.Constants;
import com.hbm.blocks.network.RadioAUTOCAL;
import com.hbm.blocks.network.RadioTelex;
import com.hbm.blocks.network.RadioTorchController;
import com.hbm.blocks.network.RadioTorchCounter;
import com.hbm.blocks.network.RadioTorchReceiver;
import com.hbm.blocks.network.RadioTorchReader;
import com.hbm.blocks.network.RadioTorchLogic;
import com.hbm.blocks.network.RadioTorchSender;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.level.block.SoundType;

/** Feature-local block and block-item registry for Redstone over Radio. */
public final class ModRadioBlocks {
    private ModRadioBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<RadioTorchSender> RADIO_TORCH_SENDER = BLOCKS.registerBlock(
            "radio_torch_sender", RadioTorchSender::new,
            properties -> properties.strength(0.1F, 10.0F).sound(SoundType.WOOD)
                    .noCollision().noOcclusion());
    public static final RegistryObject<RadioTorchReceiver> RADIO_TORCH_RECEIVER = BLOCKS.registerBlock(
            "radio_torch_receiver", RadioTorchReceiver::new,
            properties -> properties.strength(0.1F, 10.0F).sound(SoundType.WOOD)
                    .noCollision().noOcclusion());
    public static final RegistryObject<RadioTorchReader> RADIO_TORCH_READER = BLOCKS.registerBlock(
            "radio_torch_reader", RadioTorchReader::new,
            properties -> properties.strength(0.1F, 10.0F).sound(SoundType.WOOD)
                    .noCollision().noOcclusion());
    public static final RegistryObject<RadioTorchLogic> RADIO_TORCH_LOGIC = BLOCKS.registerBlock(
            "radio_torch_logic", RadioTorchLogic::new,
            properties -> properties.strength(0.1F, 10.0F).sound(SoundType.WOOD)
                    .noCollision().noOcclusion());
    public static final RegistryObject<RadioTorchCounter> RADIO_TORCH_COUNTER = BLOCKS.registerBlock(
            "radio_torch_counter", RadioTorchCounter::new,
            properties -> properties.strength(0.1F, 10.0F).sound(SoundType.WOOD)
                    .noCollision().noOcclusion());
    public static final RegistryObject<RadioTorchController> RADIO_TORCH_CONTROLLER = BLOCKS.registerBlock(
            "radio_torch_controller", RadioTorchController::new,
            properties -> properties.strength(0.1F, 10.0F).sound(SoundType.WOOD)
                    .noCollision().noOcclusion());
    public static final RegistryObject<RadioTelex> RADIO_TELEX = BLOCKS.registerBlock(
            "radio_telex", RadioTelex::new,
            properties -> properties.strength(3.0F, 10.0F).sound(SoundType.WOOD).noOcclusion());
    public static final RegistryObject<RadioAUTOCAL> RADIO_AUTOCAL = BLOCKS.registerBlock(
            "radio_autocal", RadioAUTOCAL::new,
            properties -> properties.strength(3.0F, 10.0F).sound(SoundType.METAL).noOcclusion());

    public static final RegistryObject<RadioBlockItem> RADIO_TORCH_SENDER_ITEM = ITEMS.registerItem(
            "radio_torch_sender", properties -> new RadioBlockItem(RADIO_TORCH_SENDER.get(),
                    dev.golem.core.world.Compat.blockDescription(properties)));
    public static final RegistryObject<RadioBlockItem> RADIO_TORCH_RECEIVER_ITEM = ITEMS.registerItem(
            "radio_torch_receiver", properties -> new RadioBlockItem(RADIO_TORCH_RECEIVER.get(),
                    dev.golem.core.world.Compat.blockDescription(properties)));
    public static final RegistryObject<RadioBlockItem> RADIO_TORCH_READER_ITEM = ITEMS.registerItem(
            "radio_torch_reader", properties -> new RadioBlockItem(RADIO_TORCH_READER.get(),
                    dev.golem.core.world.Compat.blockDescription(properties)));
    public static final RegistryObject<RadioBlockItem> RADIO_TORCH_LOGIC_ITEM = ITEMS.registerItem(
            "radio_torch_logic", properties -> new RadioBlockItem(RADIO_TORCH_LOGIC.get(),
                    dev.golem.core.world.Compat.blockDescription(properties)));
    public static final RegistryObject<RadioBlockItem> RADIO_TORCH_COUNTER_ITEM = ITEMS.registerItem(
            "radio_torch_counter", properties -> new RadioBlockItem(RADIO_TORCH_COUNTER.get(),
                    dev.golem.core.world.Compat.blockDescription(properties)));
    public static final RegistryObject<RadioBlockItem> RADIO_TORCH_CONTROLLER_ITEM = ITEMS.registerItem(
            "radio_torch_controller", properties -> new RadioBlockItem(RADIO_TORCH_CONTROLLER.get(),
                    dev.golem.core.world.Compat.blockDescription(properties)));
    public static final RegistryObject<RadioBlockItem> RADIO_TELEX_ITEM = ITEMS.registerItem(
            "radio_telex", properties -> new RadioBlockItem(RADIO_TELEX.get(),
                    dev.golem.core.world.Compat.blockDescription(properties)));
    public static final RegistryObject<RadioBlockItem> RADIO_AUTOCAL_ITEM = ITEMS.registerItem(
            "radio_autocal", properties -> new RadioBlockItem(RADIO_AUTOCAL.get(),
                    dev.golem.core.world.Compat.blockDescription(properties)));

    static {
        ModTabs.addTo(HbmTab.MACHINES, RADIO_TORCH_SENDER_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, RADIO_TORCH_RECEIVER_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, RADIO_TORCH_READER_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, RADIO_TORCH_LOGIC_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, RADIO_TORCH_COUNTER_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, RADIO_TORCH_CONTROLLER_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, RADIO_TELEX_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, RADIO_AUTOCAL_ITEM);
    }

    public static void init() {}
}
