package com.hbm.blocks;

import dev.golem.core.dejson.When;
import dev.golem.core.dejson.Variant;
import dev.golem.core.dejson.States;
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
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.hbm.items.block.ItemBlockDecoVariant;
import com.hbm.items.block.ItemBlockLockableCrate;
import dev.golem.core.Ids;
import dev.golem.core.dejson.Dispatch;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;
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
 * {@code rad_absorber[_red|_green|_pink]}, {@code wood_structure_<type>}); subtypes of one object keep one block and
 * get one block item each, with ntm-next's ids (bobblehead, plushie, snowglobe, vending machine, filing cabinet,
 * glyphid hive blocks; History §19es-itemvariant).
 *
 * <p>Each block's model, blockstate, item model, loot and tags are stated on its registration (Reg; emitted by
 * Dejson at build time). The OBJ item definitions of the renderer-drawn props and the family's recipes stay in
 * {@link DecoCrateDeclarations}; the wooden structures' element models and multipart blockstates are authored.
 */
public final class ModDecoCrateBlocks {

    private ModDecoCrateBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /**
     * The family's custom sound type. A holder, not a field of this class: a {@code SoundType} only exists in a
     * running game, and this class also initialises in DejsonMain's plain JVM to record its declarations.
     */
    public static final class Sounds {
        private Sounds() {}

        /** 1.7 {@code ModSoundTypes.flesh}: every sound is {@code hbm:block.flesh}, volume 0.5. */
        public static final SoundType FLESH_SOUND;

        static {
            SoundEvent flesh = SoundEvent.createVariableRangeEvent(Ids.of(Constants.MOD_ID, "block.flesh"));
            FLESH_SOUND = new SoundType(0.5F, 1.0F, flesh, flesh, flesh, flesh, flesh);
        }
    }

    private static final Dispatch HORIZONTAL = Variants.uniform("facing", "north", "east", "south", "west");
    /** Renderer-drawn props that drop themselves through their own getDrops (subtype from the BE or state). */
    private static final Preset ROTATING_PROP = Presets.rendered(Variants.uniformRange("rotation", 0, 15))
            .and(handle -> handle.loot(Loot.nothing()));

    /** No item renderer in 1.7: the block texture, flat; renderer-drawn, drops itself, pickaxe. */
    private static Preset flatIconBox(Dispatch states) {
        return Presets.rendered(states).and(handle -> handle.loot(Loot.self()).tag(BlockTag.PICKAXE)
                .transform(Presets.sprite("hbm:blocks/" + handle.path())));
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
    public static final RegistryObject<BlockCrate> crate = REG.block("crate",
                    p -> new BlockCrate(p, BlockCrate.Kind.SUPPLY), p -> p.strength(5.0F, 10.0F).sound(SoundType.WOOD))
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.AXE);
    /** The conserve crate: renderer-drawn; its OBJ item is typed data in {@link DecoCrateDeclarations}. */
    public static final RegistryObject<BlockCanCrate> crate_can = REG.block("crate_can", BlockCanCrate::new,
                    rendered(p -> p.strength(1.0F, 2.5F).sound(SoundType.WOOD)))
            .transform(Presets.rendered(Variants.simple()))
            .loot(Loot.self())
            .tag(BlockTag.AXE);
    /** 1.7 BlockStorageCrate.getIcon: the top icon on top and bottom, the side icon all round. */
    public static final RegistryObject<BlockStorageCrate> crate_tungsten = REG.block("crate_tungsten",
                    p -> new BlockStorageCrate(p, BlockStorageCrate.Kind.TUNGSTEN), iron(7.5F, 300.0F))
            .transform(Presets.cubeBottomTop("hbm:blocks/crate_tungsten_top", "hbm:blocks/crate_tungsten_side",
                            "hbm:blocks/crate_tungsten_top")
                    .states(Variants.product(HORIZONTAL, Variants.uniform("hot", "false", "true")))
                    .viaItemModel())
            .loot(Loot.container("minecraft:container", "minecraft:custom_data", "minecraft:custom_name"))
            .tag(BlockTag.PICKAXE);
    /** The safe door is on the face it was placed towards. */
    public static final RegistryObject<BlockStorageCrate> safe = REG.block("safe",
                    p -> new BlockStorageCrate(p, BlockStorageCrate.Kind.SAFE), iron(7.5F, 10000.0F))
            .transform(Presets.orientable("hbm:blocks/safe_front", "hbm:blocks/safe_side", "hbm:blocks/safe_side")
                    .states(Variants.product(Variants.facing(), Variants.uniform("hot", "false", "true")))
                    .viaItemModel())
            .loot(Loot.container("minecraft:container", "minecraft:custom_data", "minecraft:custom_name"))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockDecoContainer> filing_cabinet = REG.block("filing_cabinet",
                    BlockDecoContainer::new, rendered(iron(10.0F, 15.0F)))
            .transform(Presets.rendered(Variants.product(HORIZONTAL, Variants.uniform("type", "green", "steel"))))
            .loot(Loot.nothing())
            .tag(BlockTag.PICKAXE);

    // ---- deco props ----
    /** 1.7: iron, hardness and resistance 0; drops itself to any tool (onBlockHarvested). */
    public static final RegistryObject<BlockBobble> bobblehead = REG.block("bobblehead", BlockBobble::new,
                    rendered(p -> p.strength(0.0F, 0.0F).sound(SoundType.METAL)))
            .transform(ROTATING_PROP);
    /** 1.7: cloth sound, hardness 0 (the 500,000 resistance was overwritten by the later 0). */
    public static final RegistryObject<BlockPlushie> plushie = REG.block("plushie", BlockPlushie::new,
                    rendered(p -> p.strength(0.0F, 0.0F).sound(SoundType.WOOL)))
            .transform(ROTATING_PROP);
    public static final RegistryObject<BlockSnowglobe> snowglobe = REG.block("snowglobe", BlockSnowglobe::new,
                    rendered(p -> p.strength(0.0F, 0.0F).sound(SoundType.GLASS)))
            .transform(ROTATING_PROP);
    public static final RegistryObject<BlockVendingMachine> vending_machine = REG.block("vending_machine",
                    BlockVendingMachine::new, rendered(iron(5.0F, 10.0F)))
            .transform(Presets.rendered(Variants.product(HORIZONTAL, Variants.uniform("cell", "false", "true"),
                    Variants.uniform("snacks", "false", "true"))))
            .loot(Loot.nothing())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<PinkCloudBroadcaster> broadcaster_pc = REG.block("broadcaster_pc",
                    PinkCloudBroadcaster::new, rendered(iron(5.0F, 15.0F)))
            .transform(flatIconBox(HORIZONTAL));
    public static final RegistryObject<Radiobox> radiobox = REG.block("radiobox", Radiobox::new,
                    rendered(iron(5.0F, 10.0F)))
            .transform(flatIconBox(Variants.product(HORIZONTAL, Variants.uniform("enabled", "false", "true"))));
    public static final RegistryObject<RadioRec> radiorec = REG.block("radiorec", RadioRec::new,
                    rendered(iron(5.0F, 10.0F)))
            .transform(flatIconBox(HORIZONTAL));
    /** 1.7 isOpaqueCube false: a full cube that does not hide its neighbours. */
    public static final RegistryObject<BlockEmitter> deco_emitter = REG.block("deco_emitter", BlockEmitter::new,
                    p -> iron(5.0F, 20.0F).apply(p).noOcclusion())
            .transform(Presets.cubeAll("hbm:blocks/emitter")
                    .states(Variants.uniform("facing", "down", "up", "north", "south", "west", "east")).noItem())
            .transform(Presets.shares("hbm:block/deco_emitter"))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<PartEmitter> part_emitter = REG.block("part_emitter", PartEmitter::new,
                    iron(5.0F, 20.0F))
            .transform(Presets.cubeAll("hbm:blocks/part_top").noItem())
            .transform(Presets.shares("hbm:block/part_emitter"))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

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
            String cap = "hbm:blocks/" + capId(type);
            // 1.7 BlockCap: 128 of its caps, no fortune; silk touch keeps the block
            CAP_BLOCKS.put(type, REG.block(capId(type), p -> new BlockCap(p, type), iron(5.0F, 10.0F))
                    .transform(Presets.cubeBottomTop(cap + "_top", cap, cap + "_top").viaItemModel())
                    .loot(Loot.oreNoFortune("hbm:cap_" + type.name().toLowerCase(java.util.Locale.US), 128, 128))
                    .tag(BlockTag.PICKAXE));
        }
    }

    /** 1.7 {@code BlockPinkLog}: an axis log, oak bark on the sides and the pink rings on the ends; no creative tab. */
    public static final RegistryObject<RotatedPillarBlock> pink_log = REG.block("pink_log",
                    RotatedPillarBlock::new, p -> p.strength(0.5F).sound(SoundType.WOOD).ignitedByLava())
            .transform(Presets.column("hbm:blocks/pink_log", "minecraft:block/oak_log").states(Variants.axis()).viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.AXE);

    public static final Map<BlockWoodStructure.EnumWoodStructure, RegistryObject<BlockWoodStructure>> WOOD_STRUCTURES =
            new EnumMap<>(BlockWoodStructure.EnumWoodStructure.class);

    static {
        for (BlockWoodStructure.EnumWoodStructure type : BlockWoodStructure.EnumWoodStructure.values()) {
            // the element models and multipart blockstate (their joins) are authored; the item shows the block model
            Reg.Handle<BlockWoodStructure> wood = REG.block(woodId(type), p -> new BlockWoodStructure(p, type),
                            p -> p.strength(5.0F, 15.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava())
                    .transform(Presets.shares("hbm:block/" + woodId(type)))
                    .loot(Loot.self())
                    .tag(BlockTag.AXE);
            if (type == BlockWoodStructure.EnumWoodStructure.SCAFFOLD) wood.tag(BlockTag.CLIMBABLE);
            wood.andThen(handle -> woodStructureStates(type));
            WOOD_STRUCTURES.put(type, wood);
        }
    }

    /** The absorber sprites, in {@code EnumAbsorberTier} order. */
    private static final String[] ABSORBER_TEXTURES = {"absorber", "absorber_red", "absorber_green", "absorber_pink"};

    public static final Map<BlockAbsorber.EnumAbsorberTier, RegistryObject<BlockAbsorber>> ABSORBERS =
            new EnumMap<>(BlockAbsorber.EnumAbsorberTier.class);

    static {
        for (BlockAbsorber.EnumAbsorberTier tier : BlockAbsorber.EnumAbsorberTier.values()) {
            ABSORBERS.put(tier, REG.block(absorberId(tier), p -> new BlockAbsorber(p, tier),
                            p -> iron(5.0F, 10.0F).apply(p).randomTicks())
                    .transform(Presets.cubeAll("hbm:blocks/" + ABSORBER_TEXTURES[tier.ordinal()]).viaItemModel())
                    .loot(Loot.self())
                    .tag(BlockTag.PICKAXE));
        }
    }

    /** 1.7 BlockGlyphid: two textures per subtype, chosen per position; drops itself through getDrops. */
    public static final RegistryObject<BlockGlyphid> glyphid_base = REG.block("glyphid_base",
                    BlockGlyphid::new, p -> p.strength(0.5F).sound(Sounds.FLESH_SOUND))
            .model(d -> d.models(Models.group()
                            .and("_normal_0", Models.cubeAll("hbm:blocks/glyphid_base"))
                            .and("_normal_1", Models.cubeAll("hbm:blocks/glyphid_base_alt"))
                            .and("_infested_0", Models.cubeAll("hbm:blocks/glyphid_base_infested"))
                            .and("_infested_1", Models.cubeAll("hbm:blocks/glyphid_base_infested_alt"))
                            .and("_rad_0", Models.cubeAll("hbm:blocks/glyphid_base_rad"))
                            .and("_rad_1", Models.cubeAll("hbm:blocks/glyphid_base_rad_alt")))
                    .states(Variants.product(Variants.select("type", "normal", "_normal", "infested", "_infested",
                            "rad", "_rad"), Variants.pool(2))))
            .icon(d -> d.models(Models.blockItem("hbm:block/glyphid_base_normal_0")))
            .loot(Loot.nothing());
    public static final RegistryObject<BlockGlyphidSpawner> glyphid_spawner = REG.block("glyphid_spawner",
                    BlockGlyphidSpawner::new, p -> p.strength(0.5F).sound(Sounds.FLESH_SOUND))
            .model(d -> d.models(Models.group()
                            .and("_normal", Models.cubeAll("hbm:blocks/glyphid_eggs_alt"))
                            .and("_infested", Models.cubeAll("hbm:blocks/glyphid_eggs_infested"))
                            .and("_rad", Models.cubeAll("hbm:blocks/glyphid_eggs_rad")))
                    .states(Variants.select("type", "normal", "_normal", "infested", "_infested", "rad", "_rad")))
            .icon(d -> d.models(Models.blockItem("hbm:block/glyphid_spawner_normal")))
            .loot(Loot.nothing());
    public static final RegistryObject<MachineFieldDisturber> field_disturber = REG.block("field_disturber",
                    MachineFieldDisturber::new, iron(5.0F, 200.0F))
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    // ---- items ----
    // 1.7's metadata items, one block item per subtype with ntm-next's ids (History §19es-itemvariant); index = metadata.
    public static final List<RegistryObject<ItemBlockDecoVariant>> bobblehead_items = variants(
            DecoCrateDeclarations.trinketIds("bobblehead", BlockBobble.BobbleType.VALUES), bobblehead,
            i -> Component.translatable("block.hbm.bobblehead"), null);
    public static final List<RegistryObject<ItemBlockDecoVariant>> plushie_items = variants(
            DecoCrateDeclarations.trinketIds("plushie", BlockPlushie.PlushieType.VALUES), plushie, i -> {
                BlockPlushie.PlushieType type = BlockPlushie.PlushieType.of(i);
                return Component.translatable("block.hbm.plushie", type == BlockPlushie.PlushieType.NONE ? "" : type.label);
            }, i -> {
                BlockPlushie.PlushieType type = BlockPlushie.PlushieType.of(i);
                return type.inscription == null ? List.of() : List.of(Component.literal(type.inscription));
            });
    public static final List<RegistryObject<ItemBlockDecoVariant>> snowglobe_items = variants(
            DecoCrateDeclarations.trinketIds("snowglobe", BlockSnowglobe.SnowglobeType.VALUES), snowglobe,
            i -> Component.translatable("block.hbm.snowglobe"), null);
    public static final List<RegistryObject<ItemBlockDecoVariant>> vending_machine_items = variants(
            new String[] {"vending_machine", "vending_machine_snacks"}, vending_machine,
            i -> Component.translatable("block.hbm.vending_machine"), null);
    public static final List<RegistryObject<ItemBlockDecoVariant>> filing_cabinet_items = variants(
            new String[] {"filing_cabinet_green", "filing_cabinet_steel"}, filing_cabinet,
            i -> Component.translatable("block.hbm.filing_cabinet." + BlockDecoContainer.CabinetType.of(i).getSerializedName()),
            null);
    public static final List<RegistryObject<ItemBlockDecoVariant>> glyphid_base_items = variants(
            new String[] {"glyphid_base", "glyphid_base_infested", "glyphid_base_rad"}, glyphid_base,
            i -> Component.translatable("block.hbm.glyphid_base"), null);
    public static final List<RegistryObject<ItemBlockDecoVariant>> glyphid_spawner_items = variants(
            new String[] {"glyphid_spawner", "glyphid_spawner_infested", "glyphid_spawner_rad"}, glyphid_spawner,
            i -> Component.translatable("block.hbm.glyphid_spawner"), null);
    public static final RegistryObject<ItemBlockDecoVariant> deco_emitter_item = variants(new String[] {"deco_emitter"},
            deco_emitter, i -> Component.translatable("block.hbm.deco_emitter"), i -> {
                List<Component> list = new ArrayList<>();
                BlockEmitter.addInformation(list);
                return list;
            }).get(0);
    public static final RegistryObject<ItemBlockDecoVariant> part_emitter_item = variants(new String[] {"part_emitter"},
            part_emitter, i -> Component.translatable("block.hbm.part_emitter"), i -> {
                List<Component> list = new ArrayList<>();
                PartEmitter.addInformation(list);
                return list;
            }).get(0);

    static {
        // the hive blocks' other members draw their block models (the base ids' icons are on their blocks)
        for (String type : new String[] {"infested", "rad"}) {
            dev.golem.core.dejson.Dejson.item(Constants.MOD_ID, "glyphid_base_" + type)
                    .models(Models.blockItem("hbm:block/glyphid_base_" + type + "_0")).register();
            dev.golem.core.dejson.Dejson.item(Constants.MOD_ID, "glyphid_spawner_" + type)
                    .models(Models.blockItem("hbm:block/glyphid_spawner_" + type)).register();
        }
    }

    static {
        // crates: 1.7 consumable tab (supply/conserve), machine tab (tungsten/safe), block tab (cabinet)
        plain("crate", crate, HbmTab.CONSUMABLES);
        plain("crate_can", crate_can, HbmTab.CONSUMABLES);
        lockable("crate_tungsten", crate_tungsten, 27);
        lockable("safe", safe, 15);
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> stacks(filing_cabinet_items, 0, 2));

        // props: the creative lists skip the NONE type
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> stacks(bobblehead_items, 1, BlockBobble.BobbleType.VALUES.length));
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> stacks(snowglobe_items, 1, BlockSnowglobe.SnowglobeType.VALUES.length));
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> stacks(plushie_items, 1, BlockPlushie.PlushieType.VALUES.length));
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> stacks(vending_machine_items, 0, 2));
        plain("broadcaster_pc", broadcaster_pc, HbmTab.MACHINES);
        plain("radiobox", radiobox, HbmTab.MACHINES);
        plain("radiorec", radiorec, HbmTab.MACHINES);
        ModTabs.addTo(HbmTab.BLOCKS, deco_emitter_item);
        ModTabs.addTo(HbmTab.BLOCKS, part_emitter_item);

        for (RegistryObject<BlockHadronCoil> coil : new RegistryObject[]{hadron_coil_alloy, hadron_coil_gold,
                hadron_coil_neodymium, hadron_coil_magtung, hadron_coil_schrabidium, hadron_coil_schrabidate,
                hadron_coil_starmetal, hadron_coil_chlorophyte, hadron_coil_mese}) {
            String id = ((Reg.Handle<?>) coil).path();
            RegistryObject<ItemBlockDecoVariant> item = REG.item(id,
                    p -> new ItemBlockDecoVariant(coil.get(), p, 0, i -> Component.translatable("block.hbm." + id), i -> {
                        List<Component> list = new ArrayList<>();
                        BlockHadronCoil.addInformation(coil.get().factor, list);
                        return list;
                    }), Compat::blockDescription);
            ModTabs.addTo(HbmTab.MACHINES, item);
        }
        for (RegistryObject<BlockCap> cap : CAP_BLOCKS.values()) plain(((Reg.Handle<?>) cap).path(), cap, HbmTab.BLOCKS);
        plain("pink_log", pink_log, null);
        for (RegistryObject<BlockWoodStructure> wood : WOOD_STRUCTURES.values())
            plain(((Reg.Handle<?>) wood).path(), wood, HbmTab.BLOCKS);
        for (RegistryObject<BlockAbsorber> absorber : ABSORBERS.values())
            plain(((Reg.Handle<?>) absorber).path(), absorber, HbmTab.MACHINES);
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> stacks(glyphid_base_items, 0, 3));
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> stacks(glyphid_spawner_items, 0, 3));
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

    /** A dense coil: one texture all round, its item showing the block model; drops itself, pickaxe. */
    private static Reg.Handle<BlockHadronCoil> coil(String id, int factor) {
        return REG.block(id, p -> new BlockHadronCoil(p, factor), iron(5.0F, 10.0F))
                .transform(Presets.cubeAll().noItem())
                .transform(Presets.shares("hbm:block/" + id))
                .loot(Loot.self())
                .tag(BlockTag.PICKAXE);
    }

    private static void plain(String id, RegistryObject<? extends Block> block, HbmTab tab) {
        Reg.Handle<BlockItem> item = REG.blockItem(block, Compat::blockDescription);
        if (tab != null) item.tab(tab);
    }

    private static void lockable(String id, RegistryObject<? extends Block> block, int slots) {
        REG.item(id, p -> new ItemBlockLockableCrate(block.get(), p, slots), Compat::blockDescription)
                .tab(HbmTab.MACHINES);
    }

    /**
     * One block item per id (index = 1.7 metadata) over {@code block}. Member 0 is registered last, so the block's own
     * item ({@code Block.asItem}) stays the metadata-0 item on every loader.
     */
    private static List<RegistryObject<ItemBlockDecoVariant>> variants(String[] ids, Supplier<? extends Block> block,
                                                                      IntFunction<Component> name,
                                                                      IntFunction<List<Component>> tooltip) {
        @SuppressWarnings("unchecked")
        RegistryObject<ItemBlockDecoVariant>[] items = new RegistryObject[ids.length];
        for (int k = 1; k <= ids.length; k++) {
            int variant = k % ids.length;
            items[variant] = REG.item(ids[variant], p -> new ItemBlockDecoVariant(block.get(), p, variant, name, tooltip),
                    Compat::blockDescription);
        }
        List<RegistryObject<ItemBlockDecoVariant>> members = List.of(items);
        ItemBlockDecoVariant.Families.record(block, members);
        return members;
    }

    private static List<ItemStack> stacks(List<RegistryObject<ItemBlockDecoVariant>> items, int from, int to) {
        return IntStream.range(from, to).mapToObj(i -> new ItemStack(items.get(i).get())).toList();
    }

    /** The wooden structures' multipart blockstates (their joins); the element models are authored. */
    private static void woodStructureStates(BlockWoodStructure.EnumWoodStructure type) {
        switch (type) {
            case CEILING -> {
                States.multipart(Constants.MOD_ID, "wood_structure_ceiling")
                        .part(new Variant("hbm:block/wood_structure_ceiling"))
                        .register();
            }
            case ROOF -> {
                States.multipart(Constants.MOD_ID, "wood_structure_roof")
                        .part(new Variant("hbm:block/wood_structure_roof_base"))
                        .part(When.of("west", "true"), new Variant("hbm:block/wood_structure_roof_west"))
                        .part(When.of("east", "true"), new Variant("hbm:block/wood_structure_roof_east"))
                        .register();
            }
            case SCAFFOLD -> {
                States.multipart(Constants.MOD_ID, "wood_structure_scaffold")
                        .part(new Variant("hbm:block/wood_structure_scaffold_posts"))
                        .part(When.of("up", "true"), new Variant("hbm:block/wood_structure_scaffold_post_tops"))
                        .part(When.of("up", "false"), new Variant("hbm:block/wood_structure_scaffold_top"))
                        .part(When.of("west", "false"), new Variant("hbm:block/wood_structure_scaffold_west"))
                        .part(When.of("east", "false"), new Variant("hbm:block/wood_structure_scaffold_east"))
                        .part(When.of("north", "false"), new Variant("hbm:block/wood_structure_scaffold_north"))
                        .part(When.of("south", "false"), new Variant("hbm:block/wood_structure_scaffold_south"))
                        .register();
            }
        }
    }

    public static void init() {}
}
