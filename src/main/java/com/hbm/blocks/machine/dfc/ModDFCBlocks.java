package com.hbm.blocks.machine.dfc;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Variants;
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
import net.minecraft.world.level.block.state.BlockBehaviour;

/** The dark fusion core: the reaction chamber, the three things that aim at it, and what goes inside. */
public final class ModDFCBlocks {

    private ModDFCBlocks() {}

    /** One real-world hour of stabiliser time at full power, the old {@code 60 * 60 * 60 * 20 * 100}. */
    public static final long LENS_LIFETIME = 60L * 60L * 60L * 20L * 100L;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /** The aimed components: a cube of their own icon turned to any of the six facings; items are OBJ (ObjItemDeclarations). */
    private static final Preset AIMED = Presets.cubeAllCutout().states(Variants.facingAll()).noItem()
            .and(handle -> handle.loot(Loot.self()));

    /** 1.7 renders the core invisible; its item is the flat {@code dfc_core} sprite (see {@link #dfc_core_item}). */
    public static final RegistryObject<CoreCore> dfc_core = REG.block(
            "dfc_core", CoreCore::new, ModDFCBlocks::machine)
            .transform(Presets.cubeAllCutout().noItem())
            .loot(Loot.self()).tag(BlockTag.PICKAXE);

    public static final RegistryObject<CoreComponent> dfc_emitter = REG.block(
            "dfc_emitter", p -> new CoreComponent(TileEntityCoreEmitter::new,
                    () -> ModBlockEntities.DFC_EMITTER.get(), TileEntityCoreEmitter::serverTick, p),
            ModDFCBlocks::machine)
            .transform(AIMED).tag(BlockTag.PICKAXE);
    public static final RegistryObject<CoreComponent> dfc_receiver = REG.block(
            "dfc_receiver", p -> new CoreComponent(TileEntityCoreReceiver::new,
                    () -> ModBlockEntities.DFC_RECEIVER.get(), TileEntityCoreReceiver::serverTick, p),
            ModDFCBlocks::machine)
            .transform(AIMED).tag(BlockTag.PICKAXE);
    public static final RegistryObject<CoreComponent> dfc_injector = REG.block(
            "dfc_injector", p -> new CoreComponent(TileEntityCoreInjector::new,
                    () -> ModBlockEntities.DFC_INJECTOR.get(), TileEntityCoreInjector::serverTick, p),
            ModDFCBlocks::machine)
            .transform(AIMED).tag(BlockTag.PICKAXE);
    public static final RegistryObject<CoreComponent> dfc_stabilizer = REG.block(
            "dfc_stabilizer", p -> new CoreComponent(TileEntityCoreStabilizer::new,
                    () -> ModBlockEntities.DFC_STABILIZER.get(), TileEntityCoreStabilizer::serverTick, p),
            ModDFCBlocks::machine)
            .transform(AIMED).tag(BlockTag.PICKAXE);

    // The core is the one dark fusion block with no model of its own: it is RenderShape.INVISIBLE, as
    // 1.7's getRenderType() == -1, and 1.7 has no item renderer for it either, so the icon is the flat
    // sprite that render type falls back to. Its art is a drawn orb, not a tileable face: the four
    // aimed components next to it do have OBJ icons, declared as machine models.
    public static final RegistryObject<BlockItem> dfc_core_item = REG.blockItem(dfc_core)
            .transform(Presets.sprite("hbm:blocks/dfc_core"))
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> dfc_emitter_item = REG.blockItem(dfc_emitter).tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> dfc_receiver_item = REG.blockItem(dfc_receiver).tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> dfc_injector_item = REG.blockItem(dfc_injector).tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> dfc_stabilizer_item = REG.blockItem(dfc_stabilizer).tab(HbmTab.MACHINES);

    public static final RegistryObject<ItemLens> ams_lens =
            REG.item("ams_lens", p -> new ItemLens(LENS_LIFETIME, p), p -> p.stacksTo(1))
                    .transform(Presets.sprite()).tab(HbmTab.CONTROL);

    public static final RegistryObject<ItemAMSCore> ams_core_sing = REG.item("ams_core_sing",
            p -> new ItemAMSCore(500, 0.8F, 1.5F, p), p -> p.stacksTo(1).rarity(Rarity.UNCOMMON))
            .transform(Presets.sprite()).tab(HbmTab.CONTROL);
    public static final RegistryObject<ItemAMSCore> ams_core_wormhole = REG.item("ams_core_wormhole",
            p -> new ItemAMSCore(650, 1.5F, 0.8F, p), p -> p.stacksTo(1).rarity(Rarity.UNCOMMON))
            .transform(Presets.sprite()).tab(HbmTab.CONTROL);
    public static final RegistryObject<ItemAMSCore> ams_core_eyeofharmony = REG.item("ams_core_eyeofharmony",
            p -> new ItemAMSCore(800, 1.5F, 2.0F, p), p -> p.stacksTo(1).rarity(Rarity.UNCOMMON))
            .transform(Presets.sprite()).tab(HbmTab.CONTROL);
    public static final RegistryObject<ItemAMSCore> ams_core_thingy = REG.item("ams_core_thingy",
            p -> new ItemAMSCore(2500, 0.7F, 0.7F, true, p), p -> p.stacksTo(1).rarity(Rarity.EPIC))
            .transform(Presets.sprite()).tab(HbmTab.CONSUMABLES);

    private static BlockBehaviour.Properties machine(
            BlockBehaviour.Properties p) {
        return p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion();
    }

    /** Class-load hook so the registers are populated during bootstrap. */
    public static void init() {}
}
