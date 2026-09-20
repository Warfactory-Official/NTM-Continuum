package com.hbm.blocks.machine.dfc;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.machine.ItemLens;
import com.hbm.items.special.ItemAMSCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.dfc.TileEntityCore;
import com.hbm.tileentity.machine.dfc.TileEntityCoreEmitter;
import com.hbm.tileentity.machine.dfc.TileEntityCoreInjector;
import com.hbm.tileentity.machine.dfc.TileEntityCoreReceiver;
import com.hbm.tileentity.machine.dfc.TileEntityCoreStabilizer;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;

/** The dark fusion core: the reaction chamber, the three things that aim at it, and what goes inside. */
public final class ModDFCBlocks {

    private ModDFCBlocks() {}

    /** One real-world hour of stabiliser time at full power, the old {@code 60 * 60 * 60 * 20 * 100}. */
    public static final long LENS_LIFETIME = 60L * 60L * 60L * 20L * 100L;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<CoreCore> dfc_core = BLOCKS.registerBlock(
            "dfc_core", CoreCore::new, ModDFCBlocks::machine);

    public static final RegistryObject<CoreComponent> dfc_emitter = BLOCKS.registerBlock(
            "dfc_emitter", p -> new CoreComponent(TileEntityCoreEmitter::new,
                    () -> ModBlockEntities.DFC_EMITTER.get(), TileEntityCoreEmitter::serverTick, p),
            ModDFCBlocks::machine);
    public static final RegistryObject<CoreComponent> dfc_receiver = BLOCKS.registerBlock(
            "dfc_receiver", p -> new CoreComponent(TileEntityCoreReceiver::new,
                    () -> ModBlockEntities.DFC_RECEIVER.get(), TileEntityCoreReceiver::serverTick, p),
            ModDFCBlocks::machine);
    public static final RegistryObject<CoreComponent> dfc_injector = BLOCKS.registerBlock(
            "dfc_injector", p -> new CoreComponent(TileEntityCoreInjector::new,
                    () -> ModBlockEntities.DFC_INJECTOR.get(), TileEntityCoreInjector::serverTick, p),
            ModDFCBlocks::machine);
    public static final RegistryObject<CoreComponent> dfc_stabilizer = BLOCKS.registerBlock(
            "dfc_stabilizer", p -> new CoreComponent(TileEntityCoreStabilizer::new,
                    () -> ModBlockEntities.DFC_STABILIZER.get(), TileEntityCoreStabilizer::serverTick, p),
            ModDFCBlocks::machine);

    public static final RegistryObject<BlockItem> dfc_core_item = ITEMS.registerBlockItem("dfc_core", dfc_core);
    public static final RegistryObject<BlockItem> dfc_emitter_item = ITEMS.registerBlockItem("dfc_emitter", dfc_emitter);
    public static final RegistryObject<BlockItem> dfc_receiver_item = ITEMS.registerBlockItem("dfc_receiver", dfc_receiver);
    public static final RegistryObject<BlockItem> dfc_injector_item = ITEMS.registerBlockItem("dfc_injector", dfc_injector);
    public static final RegistryObject<BlockItem> dfc_stabilizer_item = ITEMS.registerBlockItem("dfc_stabilizer", dfc_stabilizer);

    public static final RegistryObject<ItemLens> ams_lens =
            ITEMS.registerItem("ams_lens", p -> new ItemLens(LENS_LIFETIME, p), p -> p.stacksTo(1));

    public static final RegistryObject<ItemAMSCore> ams_core_sing = ITEMS.registerItem("ams_core_sing",
            p -> new ItemAMSCore(500, 0.8F, 1.5F, p), p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final RegistryObject<ItemAMSCore> ams_core_wormhole = ITEMS.registerItem("ams_core_wormhole",
            p -> new ItemAMSCore(650, 1.5F, 0.8F, p), p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final RegistryObject<ItemAMSCore> ams_core_eyeofharmony = ITEMS.registerItem("ams_core_eyeofharmony",
            p -> new ItemAMSCore(800, 1.5F, 2.0F, p), p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final RegistryObject<ItemAMSCore> ams_core_thingy = ITEMS.registerItem("ams_core_thingy",
            p -> new ItemAMSCore(2500, 0.7F, 0.7F, true, p), p -> p.stacksTo(1).rarity(Rarity.EPIC));

    static {
        ModTabs.addTo(HbmTab.MACHINES, dfc_core_item);
        ModTabs.addTo(HbmTab.MACHINES, dfc_emitter_item);
        ModTabs.addTo(HbmTab.MACHINES, dfc_receiver_item);
        ModTabs.addTo(HbmTab.MACHINES, dfc_injector_item);
        ModTabs.addTo(HbmTab.MACHINES, dfc_stabilizer_item);
        ModTabs.addTo(HbmTab.CONTROL, ams_lens);
        ModTabs.addTo(HbmTab.CONTROL, ams_core_sing);
        ModTabs.addTo(HbmTab.CONTROL, ams_core_wormhole);
        ModTabs.addTo(HbmTab.CONTROL, ams_core_eyeofharmony);
        ModTabs.addTo(HbmTab.CONSUMABLES, ams_core_thingy);
    }

    private static net.minecraft.world.level.block.state.BlockBehaviour.Properties machine(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties p) {
        return p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion();
    }

    /** Class-load hook so the registers are populated during bootstrap. */
    public static void init() {}
}
