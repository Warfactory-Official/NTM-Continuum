package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockAbsorber;
import com.hbm.blocks.generic.BlockBobble;
import com.hbm.blocks.generic.BlockCanCrate;
import com.hbm.blocks.generic.BlockCap;
import com.hbm.blocks.generic.BlockCrate;
import com.hbm.blocks.generic.BlockDecoContainer;
import com.hbm.blocks.generic.BlockEmitter;
import com.hbm.blocks.generic.BlockGlyphid;
import com.hbm.blocks.generic.BlockGlyphidSpawner;
import com.hbm.blocks.generic.BlockPlushie;
import com.hbm.blocks.generic.BlockSnowglobe;
import com.hbm.blocks.generic.BlockStorageCrate;
import com.hbm.blocks.generic.BlockWoodStructure;
import com.hbm.blocks.generic.PartEmitter;
import com.hbm.blocks.machine.BlockHadronCoil;
import com.hbm.blocks.machine.BlockVendingMachine;
import com.hbm.blocks.machine.MachineFieldDisturber;
import com.hbm.blocks.machine.PinkCloudBroadcaster;
import com.hbm.blocks.machine.RadioRec;
import com.hbm.blocks.machine.Radiobox;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.block.ItemBlockDecoVariant;
import com.hbm.items.block.ItemBlockLockableCrate;
import dev.golem.core.Ids;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

/**
 * The storage crates, decorative props and simple blocks of the final content wave, as 1.7 {@code ModBlocks}
 * declared them: the supply and conserve crates, the tungsten crate and safe, the filing cabinet, the
 * bobbleheads, plushies and snowglobes, the vending machine, the corrupted broadcaster, the pest control box and
 * the FM radio, the light and particle emitters, the dense coils, the bottle-cap blocks, the pink log, the wooden
 * structures, the radiation absorbers, the glyphid hive blocks and the field jammer.
 *
 * <p>1.7 metadata subtypes that are really different blocks are flattened ({@code block_cap_<type>},
 * {@code rad_absorber[_red|_green|_pink]}, {@code wood_structure_<type>}); subtypes of one object keep one id and
 * carry the subtype in {@code hbm:item_variant} (bobblehead, plushie, snowglobe, vending machine, filing cabinet,
 * glyphid hive blocks).
 */
public final class ModDecoCrateBlocks {

    private ModDecoCrateBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** 1.7 {@code ModSoundTypes.flesh}: every sound is {@code hbm:block.flesh}, volume 0.5. */
    public static final SoundType FLESH_SOUND;

    static {
        SoundEvent flesh = SoundEvent.createVariableRangeEvent(Ids.of(Constants.MOD_ID, "block.flesh"));
        FLESH_SOUND = new SoundType(0.5F, 1.0F, flesh, flesh, flesh, flesh, flesh);
    }

    private static UnaryOperator<BlockBehaviour.Properties> iron(float hardness, float resistance) {
        return p -> p.strength(hardness, resistance).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    /** Renderer-drawn: nothing may cull or suffocate against the empty model. */
    private static UnaryOperator<BlockBehaviour.Properties> rendered(UnaryOperator<BlockBehaviour.Properties> base) {
        return p -> base.apply(p).noOcclusion().isSuffocating((s, w, pos) -> false).isViewBlocking((s, w, pos) -> false);
    }

    // ---- crates ----
    /** 1.7 {@code crate}: the supply crate, wood 5/10, consumable tab; a crowbar spills random supplies. */
    public static final RegistryObject<BlockCrate> crate = BLOCKS.registerBlock("crate",
            p -> new BlockCrate(p, BlockCrate.Kind.SUPPLY), p -> p.strength(5.0F, 10.0F).sound(SoundType.WOOD));
    public static final RegistryObject<BlockCanCrate> crate_can = BLOCKS.registerBlock("crate_can", BlockCanCrate::new,
            rendered(p -> p.strength(1.0F, 2.5F).sound(SoundType.WOOD)));
    public static final RegistryObject<BlockStorageCrate> crate_tungsten = BLOCKS.registerBlock("crate_tungsten",
            p -> new BlockStorageCrate(p, BlockStorageCrate.Kind.TUNGSTEN), iron(7.5F, 300.0F));
    public static final RegistryObject<BlockStorageCrate> safe = BLOCKS.registerBlock("safe",
            p -> new BlockStorageCrate(p, BlockStorageCrate.Kind.SAFE), iron(7.5F, 10000.0F));
    public static final RegistryObject<BlockDecoContainer> filing_cabinet = BLOCKS.registerBlock("filing_cabinet",
            BlockDecoContainer::new, rendered(iron(10.0F, 15.0F)));

    // ---- deco props ----
    /** 1.7: iron, hardness and resistance 0; drops itself to any tool (onBlockHarvested). */
    public static final RegistryObject<BlockBobble> bobblehead = BLOCKS.registerBlock("bobblehead", BlockBobble::new,
            rendered(p -> p.strength(0.0F, 0.0F).sound(SoundType.METAL)));
    /** 1.7: cloth sound, hardness 0 (the 500,000 resistance was overwritten by the later 0). */
    public static final RegistryObject<BlockPlushie> plushie = BLOCKS.registerBlock("plushie", BlockPlushie::new,
            rendered(p -> p.strength(0.0F, 0.0F).sound(SoundType.WOOL)));
    public static final RegistryObject<BlockSnowglobe> snowglobe = BLOCKS.registerBlock("snowglobe", BlockSnowglobe::new,
            rendered(p -> p.strength(0.0F, 0.0F).sound(SoundType.GLASS)));
    public static final RegistryObject<BlockVendingMachine> vending_machine = BLOCKS.registerBlock("vending_machine",
            BlockVendingMachine::new, rendered(iron(5.0F, 10.0F)));
    public static final RegistryObject<PinkCloudBroadcaster> broadcaster_pc = BLOCKS.registerBlock("broadcaster_pc",
            PinkCloudBroadcaster::new, rendered(iron(5.0F, 15.0F)));
    public static final RegistryObject<Radiobox> radiobox = BLOCKS.registerBlock("radiobox", Radiobox::new,
            rendered(iron(5.0F, 10.0F)));
    public static final RegistryObject<RadioRec> radiorec = BLOCKS.registerBlock("radiorec", RadioRec::new,
            rendered(iron(5.0F, 10.0F)));
    /** 1.7 isOpaqueCube false: a full cube that does not hide its neighbours. */
    public static final RegistryObject<BlockEmitter> deco_emitter = BLOCKS.registerBlock("deco_emitter", BlockEmitter::new,
            p -> iron(5.0F, 20.0F).apply(p).noOcclusion());
    public static final RegistryObject<PartEmitter> part_emitter = BLOCKS.registerBlock("part_emitter", PartEmitter::new,
            iron(5.0F, 20.0F));

    // ---- simple blocks ----
    public static final RegistryObject<BlockHadronCoil> hadron_coil_alloy = coil("hadron_coil_alloy", 10);
    public static final RegistryObject<BlockHadronCoil> hadron_coil_gold = coil("hadron_coil_gold", 25);
    public static final RegistryObject<BlockHadronCoil> hadron_coil_neodymium = coil("hadron_coil_neodymium", 50);
    public static final RegistryObject<BlockHadronCoil> hadron_coil_magtung = coil("hadron_coil_magtung", 100);
    public static final RegistryObject<BlockHadronCoil> hadron_coil_schrabidium = coil("hadron_coil_schrabidium", 250);
    public static final RegistryObject<BlockHadronCoil> hadron_coil_schrabidate = coil("hadron_coil_schrabidate", 500);
    public static final RegistryObject<BlockHadronCoil> hadron_coil_starmetal = coil("hadron_coil_starmetal", 1000);
    public static final RegistryObject<BlockHadronCoil> hadron_coil_chlorophyte = coil("hadron_coil_chlorophyte", 2500);
    public static final RegistryObject<BlockHadronCoil> hadron_coil_mese = coil("hadron_coil_mese", 10000);

    public static final Map<BlockCap.EnumCapBlock, RegistryObject<BlockCap>> CAP_BLOCKS = new EnumMap<>(BlockCap.EnumCapBlock.class);

    static {
        for (BlockCap.EnumCapBlock type : BlockCap.EnumCapBlock.values()) {
            CAP_BLOCKS.put(type, BLOCKS.registerBlock(capId(type), p -> new BlockCap(p, type), iron(5.0F, 10.0F)));
        }
    }

    /** 1.7 {@code BlockPinkLog}: an axis log, oak bark on the sides and the pink rings on the ends; no creative tab. */
    public static final RegistryObject<RotatedPillarBlock> pink_log = BLOCKS.registerBlock("pink_log",
            RotatedPillarBlock::new, p -> p.strength(0.5F).sound(SoundType.WOOD).ignitedByLava());

    public static final Map<BlockWoodStructure.EnumWoodStructure, RegistryObject<BlockWoodStructure>> WOOD_STRUCTURES =
            new EnumMap<>(BlockWoodStructure.EnumWoodStructure.class);

    static {
        for (BlockWoodStructure.EnumWoodStructure type : BlockWoodStructure.EnumWoodStructure.values()) {
            WOOD_STRUCTURES.put(type, BLOCKS.registerBlock(woodId(type), p -> new BlockWoodStructure(p, type),
                    p -> p.strength(5.0F, 15.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
        }
    }

    public static final Map<BlockAbsorber.EnumAbsorberTier, RegistryObject<BlockAbsorber>> ABSORBERS =
            new EnumMap<>(BlockAbsorber.EnumAbsorberTier.class);

    static {
        for (BlockAbsorber.EnumAbsorberTier tier : BlockAbsorber.EnumAbsorberTier.values()) {
            ABSORBERS.put(tier, BLOCKS.registerBlock(absorberId(tier), p -> new BlockAbsorber(p, tier),
                    p -> iron(5.0F, 10.0F).apply(p).randomTicks()));
        }
    }

    public static final RegistryObject<BlockGlyphid> glyphid_base = BLOCKS.registerBlock("glyphid_base",
            BlockGlyphid::new, p -> p.strength(0.5F).sound(FLESH_SOUND));
    public static final RegistryObject<BlockGlyphidSpawner> glyphid_spawner = BLOCKS.registerBlock("glyphid_spawner",
            BlockGlyphidSpawner::new, p -> p.strength(0.5F).sound(FLESH_SOUND));
    public static final RegistryObject<MachineFieldDisturber> field_disturber = BLOCKS.registerBlock("field_disturber",
            MachineFieldDisturber::new, iron(5.0F, 200.0F));

    // ---- items ----
    public static final RegistryObject<ItemBlockDecoVariant> bobblehead_item = variant("bobblehead", bobblehead,
            BlockBobble.BobbleType.VALUES.length, i -> Component.translatable("block.hbm.bobblehead"), null);
    public static final RegistryObject<ItemBlockDecoVariant> plushie_item = variant("plushie", plushie,
            BlockPlushie.PlushieType.VALUES.length, i -> {
                BlockPlushie.PlushieType type = BlockPlushie.PlushieType.of(i);
                return Component.translatable("block.hbm.plushie", type == BlockPlushie.PlushieType.NONE ? "" : type.label);
            }, i -> {
                BlockPlushie.PlushieType type = BlockPlushie.PlushieType.of(i);
                return type.inscription == null ? List.of() : List.of(Component.literal(type.inscription));
            });
    public static final RegistryObject<ItemBlockDecoVariant> snowglobe_item = variant("snowglobe", snowglobe,
            BlockSnowglobe.SnowglobeType.VALUES.length, i -> Component.translatable("block.hbm.snowglobe"), null);
    public static final RegistryObject<ItemBlockDecoVariant> vending_machine_item = variant("vending_machine",
            vending_machine, 2, i -> Component.translatable("block.hbm.vending_machine"), null);
    public static final RegistryObject<ItemBlockDecoVariant> filing_cabinet_item = variant("filing_cabinet",
            filing_cabinet, 2, i -> Component.translatable("block.hbm.filing_cabinet."
                    + BlockDecoContainer.CabinetType.of(i).getSerializedName()), null);
    public static final RegistryObject<ItemBlockDecoVariant> glyphid_base_item = variant("glyphid_base", glyphid_base,
            3, i -> Component.translatable("block.hbm.glyphid_base"), null);
    public static final RegistryObject<ItemBlockDecoVariant> glyphid_spawner_item = variant("glyphid_spawner",
            glyphid_spawner, 3, i -> Component.translatable("block.hbm.glyphid_spawner"), null);
    public static final RegistryObject<ItemBlockDecoVariant> deco_emitter_item = variant("deco_emitter", deco_emitter,
            1, i -> Component.translatable("block.hbm.deco_emitter"), i -> {
                List<Component> list = new ArrayList<>();
                BlockEmitter.addInformation(list);
                return list;
            });
    public static final RegistryObject<ItemBlockDecoVariant> part_emitter_item = variant("part_emitter", part_emitter,
            1, i -> Component.translatable("block.hbm.part_emitter"), i -> {
                List<Component> list = new ArrayList<>();
                PartEmitter.addInformation(list);
                return list;
            });

    static {
        // crates: 1.7 consumable tab (supply/conserve), machine tab (tungsten/safe), block tab (cabinet)
        plain("crate", crate, HbmTab.CONSUMABLES);
        plain("crate_can", crate_can, HbmTab.CONSUMABLES);
        lockable("crate_tungsten", crate_tungsten, 27);
        lockable("safe", safe, 15);
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> stacks(filing_cabinet_item, 0, 2));

        // props: the creative lists skip the NONE type
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> stacks(bobblehead_item, 1, BlockBobble.BobbleType.VALUES.length));
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> stacks(snowglobe_item, 1, BlockSnowglobe.SnowglobeType.VALUES.length));
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> stacks(plushie_item, 1, BlockPlushie.PlushieType.VALUES.length));
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> stacks(vending_machine_item, 0, 2));
        plain("broadcaster_pc", broadcaster_pc, HbmTab.MACHINES);
        plain("radiobox", radiobox, HbmTab.MACHINES);
        plain("radiorec", radiorec, HbmTab.MACHINES);
        ModTabs.addTo(HbmTab.BLOCKS, deco_emitter_item);
        ModTabs.addTo(HbmTab.BLOCKS, part_emitter_item);

        for (RegistryObject<BlockHadronCoil> coil : new RegistryObject[]{hadron_coil_alloy, hadron_coil_gold,
                hadron_coil_neodymium, hadron_coil_magtung, hadron_coil_schrabidium, hadron_coil_schrabidate,
                hadron_coil_starmetal, hadron_coil_chlorophyte, hadron_coil_mese}) {
            String id = coil.getId().getPath();
            RegistryObject<ItemBlockDecoVariant> item = ITEMS.registerItem(id,
                    p -> new ItemBlockDecoVariant(coil.get(), p, 1, i -> Component.translatable("block.hbm." + id), i -> {
                        List<Component> list = new ArrayList<>();
                        BlockHadronCoil.addInformation(coil.get().factor, list);
                        return list;
                    }), Compat::blockDescription);
            ModTabs.addTo(HbmTab.MACHINES, item);
        }
        for (RegistryObject<BlockCap> cap : CAP_BLOCKS.values()) plain(cap.getId().getPath(), cap, HbmTab.BLOCKS);
        plain("pink_log", pink_log, null);
        for (RegistryObject<BlockWoodStructure> wood : WOOD_STRUCTURES.values())
            plain(wood.getId().getPath(), wood, HbmTab.BLOCKS);
        for (RegistryObject<BlockAbsorber> absorber : ABSORBERS.values())
            plain(absorber.getId().getPath(), absorber, HbmTab.MACHINES);
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> stacks(glyphid_base_item, 0, 3));
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> stacks(glyphid_spawner_item, 0, 3));
        plain("field_disturber", field_disturber, null);
    }

    public static String capId(BlockCap.EnumCapBlock type) {
        return "block_cap_" + type.name().toLowerCase(java.util.Locale.US);
    }

    public static String woodId(BlockWoodStructure.EnumWoodStructure type) {
        return "wood_structure_" + type.name().toLowerCase(java.util.Locale.US);
    }

    public static String absorberId(BlockAbsorber.EnumAbsorberTier tier) {
        return tier == BlockAbsorber.EnumAbsorberTier.BASE ? "rad_absorber"
                : "rad_absorber_" + tier.name().toLowerCase(java.util.Locale.US);
    }

    private static RegistryObject<BlockHadronCoil> coil(String id, int factor) {
        return BLOCKS.registerBlock(id, p -> new BlockHadronCoil(p, factor), iron(5.0F, 10.0F));
    }

    private static void plain(String id, RegistryObject<? extends Block> block, HbmTab tab) {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(id, block, Compat::blockDescription);
        if (tab != null) ModTabs.addTo(tab, item);
    }

    private static void lockable(String id, RegistryObject<? extends Block> block, int slots) {
        RegistryObject<ItemBlockLockableCrate> item = ITEMS.registerItem(id,
                p -> new ItemBlockLockableCrate(block.get(), p, slots), Compat::blockDescription);
        ModTabs.addTo(HbmTab.MACHINES, item);
    }

    private static RegistryObject<ItemBlockDecoVariant> variant(String id, Supplier<? extends Block> block, int count,
                                                                IntFunction<Component> name,
                                                                IntFunction<List<Component>> tooltip) {
        return ITEMS.registerItem(id, p -> new ItemBlockDecoVariant(block.get(), p, count, name, tooltip),
                Compat::blockDescription);
    }

    private static List<ItemStack> stacks(RegistryObject<ItemBlockDecoVariant> item, int from, int to) {
        return IntStream.range(from, to).mapToObj(i -> item.get().of(i)).toList();
    }

    public static void init() {}
}
