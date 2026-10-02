package com.hbm.radio;

import com.hbm.registration.Loot;
import com.hbm.registration.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Shape;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;
import net.minecraft.world.item.Item;
import com.hbm.Constants;
import com.hbm.blocks.network.RadioAUTOCAL;
import com.hbm.blocks.network.RadioTelex;
import com.hbm.blocks.network.RadioTorchController;
import com.hbm.blocks.network.RadioTorchCounter;
import com.hbm.blocks.network.RadioTorchReceiver;
import com.hbm.blocks.network.RadioTorchReader;
import com.hbm.blocks.network.RadioTorchLogic;
import com.hbm.blocks.network.RadioTorchSender;
import com.hbm.creativetabs.HbmTab;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.level.block.SoundType;

/**
 * Feature-local block and block-item registry for Redstone over Radio, with each block's model, blockstate and
 * loot stated on its registration (Reg; emitted by Dejson at build time). The torches ride the radio torch's
 * element model over their 1.7 {@code rtty_*} sprite and their items are that flat sprite; TELEX and AUTOCAL are
 * renderer-drawn (their OBJ item definitions are typed data in {@code ObjItemDeclarations}).
 */
public final class ModRadioBlocks {
    private ModRadioBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /** Renderer-drawn folded machine: every state on the shared empty placeholder; drops itself. */
    private static final Preset FOLDED = Presets.rendered(Variants.product(
                    Variants.uniform("facing", "north", "east", "south", "west"),
                    Variants.uniform("cell", "false", "true")))
            .and(handle -> handle.loot(Loot.self()));

    public static final RegistryObject<RadioTorchSender> RADIO_TORCH_SENDER = REG.block(
            "radio_torch_sender", RadioTorchSender::new,
            properties -> properties.strength(0.1F, 10.0F).sound(SoundType.WOOD)
                    .noCollision().noOcclusion())
            .transform(switching("rtty_sender_off", "rtty_sender_on"));
    public static final RegistryObject<RadioTorchReceiver> RADIO_TORCH_RECEIVER = REG.block(
            "radio_torch_receiver", RadioTorchReceiver::new,
            properties -> properties.strength(0.1F, 10.0F).sound(SoundType.WOOD)
                    .noCollision().noOcclusion())
            .transform(switching("rtty_rec_off", "rtty_rec_on"));
    public static final RegistryObject<RadioTorchReader> RADIO_TORCH_READER = REG.block(
            "radio_torch_reader", RadioTorchReader::new,
            properties -> properties.strength(0.1F, 10.0F).sound(SoundType.WOOD)
                    .noCollision().noOcclusion())
            .transform(steady("rtty_reader"));
    public static final RegistryObject<RadioTorchLogic> RADIO_TORCH_LOGIC = REG.block(
            "radio_torch_logic", RadioTorchLogic::new,
            properties -> properties.strength(0.1F, 10.0F).sound(SoundType.WOOD)
                    .noCollision().noOcclusion())
            .transform(switching("rtty_logic_off", "rtty_logic_on"));
    public static final RegistryObject<RadioTorchCounter> RADIO_TORCH_COUNTER = REG.block(
            "radio_torch_counter", RadioTorchCounter::new,
            properties -> properties.strength(0.1F, 10.0F).sound(SoundType.WOOD)
                    .noCollision().noOcclusion())
            .transform(steady("rtty_counter"));
    public static final RegistryObject<RadioTorchController> RADIO_TORCH_CONTROLLER = REG.block(
            "radio_torch_controller", RadioTorchController::new,
            properties -> properties.strength(0.1F, 10.0F).sound(SoundType.WOOD)
                    .noCollision().noOcclusion())
            .transform(steady("rtty_controller"));
    public static final RegistryObject<RadioTelex> RADIO_TELEX = REG.block(
            "radio_telex", RadioTelex::new,
            properties -> properties.strength(3.0F, 10.0F).sound(SoundType.WOOD).noOcclusion())
            .transform(FOLDED);
    public static final RegistryObject<RadioAUTOCAL> RADIO_AUTOCAL = REG.block(
            "radio_autocal", RadioAUTOCAL::new,
            properties -> properties.strength(3.0F, 10.0F).sound(SoundType.METAL).noOcclusion())
            .transform(FOLDED);

    public static final RegistryObject<RadioBlockItem> RADIO_TORCH_SENDER_ITEM = REG.item(
            "radio_torch_sender", (Item.Properties properties) -> new RadioBlockItem(RADIO_TORCH_SENDER.get(),
                    dev.golem.core.world.Compat.blockDescription(properties)))
            .transform(Presets.sprite("hbm:blocks/rtty_sender_on"))
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<RadioBlockItem> RADIO_TORCH_RECEIVER_ITEM = REG.item(
            "radio_torch_receiver", (Item.Properties properties) -> new RadioBlockItem(RADIO_TORCH_RECEIVER.get(),
                    dev.golem.core.world.Compat.blockDescription(properties)))
            .transform(Presets.sprite("hbm:blocks/rtty_rec_on"))
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<RadioBlockItem> RADIO_TORCH_READER_ITEM = REG.item(
            "radio_torch_reader", (Item.Properties properties) -> new RadioBlockItem(RADIO_TORCH_READER.get(),
                    dev.golem.core.world.Compat.blockDescription(properties)))
            .transform(Presets.sprite("hbm:blocks/rtty_reader"))
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<RadioBlockItem> RADIO_TORCH_LOGIC_ITEM = REG.item(
            "radio_torch_logic", (Item.Properties properties) -> new RadioBlockItem(RADIO_TORCH_LOGIC.get(),
                    dev.golem.core.world.Compat.blockDescription(properties)))
            .transform(Presets.sprite("hbm:blocks/rtty_logic_on"))
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<RadioBlockItem> RADIO_TORCH_COUNTER_ITEM = REG.item(
            "radio_torch_counter", (Item.Properties properties) -> new RadioBlockItem(RADIO_TORCH_COUNTER.get(),
                    dev.golem.core.world.Compat.blockDescription(properties)))
            .transform(Presets.sprite("hbm:blocks/rtty_counter"))
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<RadioBlockItem> RADIO_TORCH_CONTROLLER_ITEM = REG.item(
            "radio_torch_controller", (Item.Properties properties) -> new RadioBlockItem(RADIO_TORCH_CONTROLLER.get(),
                    dev.golem.core.world.Compat.blockDescription(properties)))
            .transform(Presets.sprite("hbm:blocks/rtty_controller"))
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<RadioBlockItem> RADIO_TELEX_ITEM = REG.item(
            "radio_telex", (Item.Properties properties) -> new RadioBlockItem(RADIO_TELEX.get(),
                    dev.golem.core.world.Compat.blockDescription(properties)))
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<RadioBlockItem> RADIO_AUTOCAL_ITEM = REG.item(
            "radio_autocal", (Item.Properties properties) -> new RadioBlockItem(RADIO_AUTOCAL.get(),
                    dev.golem.core.world.Compat.blockDescription(properties)))
            .tab(HbmTab.MACHINES);

    /** A torch that shows whether it is lit: two torch models over the off/on sprites. */
    private static Preset switching(String off, String on) {
        return Presets.shape(Models.group().and("_off", torch(off)).and("_on", torch(on)))
                .states(Variants.product(Variants.facingUp(), Variants.select("lit", "false", "_off", "true", "_on")))
                .noItem()
                .and(handle -> handle.loot(Loot.self()));
    }

    /** A torch with one sprite in either state. */
    private static Preset steady(String sprite) {
        return Presets.shape(torch(sprite))
                .states(Variants.product(Variants.facingUp(), Variants.uniform("lit", "false", "true")))
                .noItem()
                .and(handle -> handle.loot(Loot.self()));
    }

    private static Shape torch(String sprite) {
        return Models.parented("hbm:block/radio_torch_template", Slots.of("torch", "hbm:blocks/" + sprite));
    }

    public static void init() {}
}
