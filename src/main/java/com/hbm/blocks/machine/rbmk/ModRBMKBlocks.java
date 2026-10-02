package com.hbm.blocks.machine.rbmk;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.blocks.generic.BlockLayering;
import com.hbm.registration.BlockTag;
import com.hbm.registration.Loot;
import com.hbm.registration.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Dispatch;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Placement;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.States;
import dev.golem.core.dejson.Variant;
import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RBMK's self-contained block and block-item registrations, with each block's model, blockstate and loot
 * stated on its registration (Reg; emitted by Dejson at build time).
 *
 * <p>Every block states its pickaxe membership on its chain.
 * Their item definitions are typed OBJ data in {@code ObjItemDeclarations}. The console peripherals'
 * items are OBJ definitions stated here (1.7 RBMK panel renderers' mesh, skin and animation).
 */
public final class ModRBMKBlocks {
    private ModRBMKBlocks() {}
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /** Every column state: lid none/concrete/glass by part core/middle/top. */
    private static final Dispatch COLUMN_STATES = Variants.product(
            Variants.uniform("lid", "none", "concrete", "glass"), Variants.uniform("part", "core", "middle", "top"));
    /** A column: every state drawn by the shared column model; drops itself even when blown up. */
    private static Preset columnModel(String model) {
        return Presets.rendered("hbm:block/rbmk/" + model + "_column_none", COLUMN_STATES)
                .and(handle -> handle.loot(Loot.selfAlways()));
    }
    /** A control column: the top part shows the rod's head ({@code <model>_column_top}). */
    private static Preset controlColumn(String model) {
        return handle -> {
            States.VariantFile states = States.variants(Constants.MOD_ID, handle.path());
            for (String part : new String[]{"core", "middle", "top"}) {
                String suffix = part.equals("top") ? "_column_top" : "_column_none";
                for (String lid : new String[]{"none", "concrete", "glass"}) {
                    states.variant("lid=" + lid + ",part=" + part, new Variant("hbm:block/rbmk/" + model + suffix));
                }
            }
            states.register();
            handle.loot(Loot.selfAlways());
        };
    }

    // rbmk_gauge's element sits at x 0..12, i.e. the model faces east; the other three facings turn it.
    private static final Dispatch EAST_MODEL = Variants.of(Set.of("facing"), Map.of(
            "facing=east", List.of(Placement.NONE),
            "facing=south", List.of(Placement.rotated(0, 90, false)),
            "facing=west", List.of(Placement.rotated(0, 180, false)),
            "facing=north", List.of(Placement.rotated(0, 270, false))));
    /** The console panels: the gauge's three-quarter element model with the display texture. */
    private static final Preset PANEL = Presets.shape(Models.parented("hbm:block/rbmk_gauge",
                    Slots.of("all", "hbm:blocks/rbmk/rbmk_display", "particle", "hbm:blocks/rbmk/rbmk_display")))
            .states(EAST_MODEL).noItem()
            .and(handle -> handle.loot(Loot.self()).tag(BlockTag.PICKAXE));

    public static final RegistryObject<RBMKRod> rbmk_fuel_rod = column("rbmk_fuel_rod", p -> new RBMKRod(p, false), true)
            .transform(columnModel("rbmk_element"))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKControl> rbmk_control = column("rbmk_control", p -> new RBMKControl(p, false), false)
            .transform(controlColumn("rbmk_control"))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKControl> rbmk_control_mod = column("rbmk_control_mod", p -> new RBMKControl(p, true), false)
            .transform(controlColumn("rbmk_control_mod"))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKControlAuto> rbmk_control_auto = column("rbmk_control_auto", RBMKControlAuto::new, false)
            .transform(controlColumn("rbmk_control_auto"))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKControl> rbmk_control_reasim = column("rbmk_control_reasim", p -> new RBMKControl(p, false), false)
            .transform(controlColumn("rbmk_control_reasim"))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKControlAuto> rbmk_control_reasim_auto = column("rbmk_control_reasim_auto", RBMKControlAuto::new, false)
            .transform(controlColumn("rbmk_control_reasim_auto"))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKBoiler> rbmk_boiler = column("rbmk_boiler", RBMKBoiler::new, false)
            .transform(columnModel("rbmk_boiler"))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKModerator> rbmk_moderator = column("rbmk_moderator", RBMKModerator::new, false)
            .transform(columnModel("rbmk_moderator"))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKReflector> rbmk_reflector = column("rbmk_reflector", RBMKReflector::new, false)
            .transform(columnModel("rbmk_reflector"))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKAbsorber> rbmk_absorber = column("rbmk_absorber", RBMKAbsorber::new, false)
            .transform(columnModel("rbmk_absorber"))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKBlank> rbmk_blank = column("rbmk_blank", RBMKBlank::new, false)
            .transform(columnModel("rbmk_blank"))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKRod> rbmk_fuel_rod_moderated = column("rbmk_fuel_rod_moderated", p -> new RBMKRod(p, true), true)
            .transform(columnModel("rbmk_element_mod"))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKRodReaSim> rbmk_fuel_rod_reasim = column("rbmk_fuel_rod_reasim", p -> new RBMKRodReaSim(p, false), true)
            .transform(columnModel("rbmk_element_reasim"))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKRodReaSim> rbmk_fuel_rod_reasim_moderated = column("rbmk_fuel_rod_reasim_moderated", p -> new RBMKRodReaSim(p, true), true)
            .transform(columnModel("rbmk_element_reasim_mod"))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKCooler> rbmk_cooler = column("rbmk_cooler", RBMKCooler::new, false)
            .transform(columnModel("rbmk_cooler"))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKStorage> rbmk_storage = column("rbmk_storage", RBMKStorage::new, false)
            .transform(columnModel("rbmk_storage"))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKHeater> rbmk_heater = column("rbmk_heater", RBMKHeater::new, false)
            .transform(columnModel("rbmk_heater"))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKOutgasser> rbmk_outgasser = column("rbmk_outgasser", RBMKOutgasser::new, false)
            .transform(columnModel("rbmk_outgasser"))
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<RBMKInlet> rbmk_steam_inlet = peripheral("rbmk_steam_inlet", RBMKInlet::new)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.selfAlways())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKOutlet> rbmk_steam_outlet = peripheral("rbmk_steam_outlet", RBMKOutlet::new)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.selfAlways())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKConsole> rbmk_console = nonColumn("rbmk_console", RBMKConsole::new)
            .transform(Presets.rendered(Variants.simple()))
            .loot(Loot.selfAlways())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKCraneConsole> rbmk_crane_console = nonColumn("rbmk_crane_console", RBMKCraneConsole::new)
            .transform(Presets.rendered(Variants.simple()))
            .loot(Loot.selfAlways())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKGauge> rbmk_gauge = nonColumn("rbmk_gauge", RBMKGauge::new)
            .andThen(handle -> States.variants(Constants.MOD_ID, handle.path())
                    .variant("facing=north", new Variant("hbm:block/rbmk_gauge", 0, 270, false))
                    .variant("facing=south", new Variant("hbm:block/rbmk_gauge", 0, 90, false))
                    .variant("facing=west", new Variant("hbm:block/rbmk_gauge", 0, 180, false))
                    .variant("facing=east", new Variant("hbm:block/rbmk_gauge"))
                    .register())
            .loot(Loot.selfAlways())
            .tag(BlockTag.PICKAXE);
    /** Its block model is authored; the item is the 1.7 inventory icon (slab + terminal OBJ). No loot table (as before). */
    public static final RegistryObject<RBMKTerminal> rbmk_terminal = nonColumn("rbmk_terminal", RBMKTerminal::new)
            .transform(Presets.statesOnly(Variants.facing()))
            // 1.7 RBMKTerminal.renderInventoryBlock: the mini-panel slab plus terminal.obj (icon-P2 2026-10-01)
            .transform(panelItem("terminal", "hbm:textures/models/network/terminal.png", "rbmk_terminal"))
            .tag(BlockTag.PICKAXE);
    // Console peripherals (1.7 ModBlocks 2120-2130): mini panels share the gauge's 3/30 iron block.
    public static final RegistryObject<RBMKMiniPanelBase> rbmk_display_blank = nonColumn("rbmk_display_blank", RBMKMiniPanelBase::new)
            .transform(PANEL).transform(panelItem("gauge", "hbm:textures/blocks/rbmk/rbmk_display.png", "rbmk_panel"));
    public static final RegistryObject<RBMKDisplay> rbmk_display = nonColumn("rbmk_display", RBMKDisplay::new)
            .transform(PANEL).transform(panelItem("gauge", "hbm:textures/blocks/rbmk/rbmk_display.png", "rbmk_panel"));
    public static final RegistryObject<RBMKKeyPad> rbmk_key_pad = nonColumn("rbmk_key_pad", RBMKKeyPad::new)
            .transform(PANEL).transform(panelItem("button", "hbm:textures/models/network/keypad.png", "rbmk_key_pad"));
    public static final RegistryObject<RBMKLever> rbmk_lever = nonColumn("rbmk_lever", RBMKLever::new)
            .transform(PANEL).transform(panelItem("lever", "hbm:textures/models/network/lever.png", "rbmk_lever"));
    public static final RegistryObject<RBMKNumitron> rbmk_numitron = nonColumn("rbmk_numitron", RBMKNumitron::new)
            .transform(PANEL).transform(panelItem("numitron", "hbm:textures/models/network/numitron.png", "rbmk_numitron"));
    // the graph's item reuses the numitron's mesh, skin and animation
    public static final RegistryObject<RBMKGraph> rbmk_graph = nonColumn("rbmk_graph", RBMKGraph::new)
            .transform(PANEL).transform(panelItem("numitron", "hbm:textures/models/network/numitron.png", "rbmk_numitron"));
    public static final RegistryObject<RBMKIndicator> rbmk_indicator = nonColumn("rbmk_indicator", RBMKIndicator::new)
            .transform(PANEL).transform(panelItem("indicator", "hbm:textures/models/network/indicator.png", "rbmk_indicator"));
    /** 1.7: Material.iron, hardness 50, resistance 60; a folded nine-tall column drawn by its renderer. */
    public static final RegistryObject<RBMKAutoloader> rbmk_autoloader = REG.block("rbmk_autoloader", RBMKAutoloader::new,
            p -> p.strength(50F, 60F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()
                    .isSuffocating((state, world, pos) -> false).isViewBlocking((state, world, pos) -> false))
            .transform(Presets.rendered(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                    Variants.uniform("cell", "false", "true"))))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .transform(ModRBMKBlocks::autoloaderItem);
    public static final RegistryObject<RBMKLoader> rbmk_loader = REG.block("rbmk_loader", RBMKLoader::new,
            p -> p.strength(50F, 60F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    // The debris' items are typed OBJ data (ObjItemDeclarations).
    public static final RegistryObject<RBMKDebrisBlock> rbmk_debris = debris("rbmk_debris", RBMKDebrisBlock::new)
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKDebrisBurning> rbmk_debris_burning = debris("rbmk_debris_burning", RBMKDebrisBurning::new)
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKDebrisRadiating> rbmk_debris_radiating = debris("rbmk_debris_radiating", RBMKDebrisRadiating::new)
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<RBMKDebrisDigamma> rbmk_debris_digamma = debris("rbmk_debris_digamma", RBMKDebrisDigamma::new)
            .tag(BlockTag.PICKAXE);
    /** Current graphite storage block, required by the moderator and modified-control recipes. */
    public static final RegistryObject<Block> block_graphite = REG.<Block>block("block_graphite", com.hbm.blocks.machine.pile.BlockGraphite::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(5F, 10F).sound(SoundType.STONE).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.selfAlways())
            .tag(BlockTag.PICKAXE);
    /** Hand-written because foam is a machine-safety block, not generated decoration content. */
    public static final RegistryObject<BlockLayering> foam_layer = REG.block("foam_layer",
            p -> new BlockLayering(p, true), p -> p.strength(0.1F).sound(net.minecraft.world.level.block.SoundType.SNOW).noOcclusion().randomTicks())
            .transform(layers("foam_layer", "hbm:block/block_foam"))
            .icon(d -> d.shares("hbm:item/foam_layer_height2"))
            .loot(Loot.selfAlways())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockLayering> sand_boron_layer = REG.block("sand_boron_layer",
            BlockLayering::new, p -> p.strength(0.1F).sound(net.minecraft.world.level.block.SoundType.SAND).noOcclusion().randomTicks())
            .transform(layers("sand_boron_layer", "hbm:block/sand_boron"))
            .icon(d -> d.shares("hbm:block/sand_boron_layer_height2"));

    private static <B extends Block> Reg.Handle<B> column(String id, java.util.function.Function<BlockBehaviour.Properties, B> factory, boolean fuel) { return REG.block(id, factory, p -> p.strength(3F, 30F).requiresCorrectToolForDrops()); }
    private static <B extends Block> Reg.Handle<B> peripheral(String id, java.util.function.Function<BlockBehaviour.Properties, B> factory) { return REG.block(id, factory, p -> p.strength(50F, 60F).sound(net.minecraft.world.level.block.SoundType.METAL).requiresCorrectToolForDrops()); }
    private static <B extends Block> Reg.Handle<B> nonColumn(String id, java.util.function.Function<BlockBehaviour.Properties, B> factory) { return REG.block(id, factory, p -> p.strength(3F, 30F).requiresCorrectToolForDrops().noOcclusion()); }
    private static <B extends Block> Reg.Handle<B> debris(String id, java.util.function.Function<BlockBehaviour.Properties, B> factory) {
        return REG.block(id, factory, p -> p.strength(50F, 600F).requiresCorrectToolForDrops().noOcclusion())
                .transform(Presets.cubeAll("hbm:blocks/rbmk/" + id).noItem())
                .loot(Loot.selfAlways());
    }

    /** A layered block: layers 1..7 are {@code <id>_height2..14}, a full stack is the full block's model. */
    private static Preset layers(String id, String full) {
        return handle -> {
            States.VariantFile states = States.variants(Constants.MOD_ID, handle.path());
            for (int layers = 1; layers <= 7; layers++) {
                states.variant("layers=" + layers, new Variant("hbm:block/" + id + "_height" + (layers * 2)));
            }
            states.variant("layers=8", new Variant(full));
            states.register();
        };
    }

    /** The OBJ a panel item draws: 1.7 RBMK panel renderers' mesh, skin and animation, framed like a block. */
    private static Preset panelItem(String mesh, String texture, String animation) {
        return handle -> {
            JsonObject model = new JsonObject();
            model.addProperty("type", "hbm:machine_obj");
            model.addProperty("obj", "hbm:models/rbmk/" + mesh + ".obj");
            model.addProperty("texture", texture);
            model.addProperty("animation", animation);
            model.addProperty("rawLegacyTransform", true);
            // 1.7 RBMKMiniPanelBase/RBMK<panel>.renderInventoryBlock in the 3D block-item frame. RBMKPanelIcon /
            // RBMKGaugeIcon build the icon about the block corner and turned a half turn from 1.7's
            // rotate(90) slab / rotate(-90) parts: back to the centre, then the half turn (icon-P2 2026-10-01)
            model.addProperty("frame", "block");
            model.addProperty("scale", 1.0);
            model.addProperty("yaw", 180.0);
            model.addProperty("ty", 0.0);
            model.add("itemPreTranslate", numbers(-0.5, -0.5, -0.5));
            JsonObject partTextures = new JsonObject();
            partTextures.addProperty("Panel", "hbm:textures/blocks/rbmk/rbmk_display.png");
            model.add("partTextures", partTextures);
            handle.icon(d -> d.shares("hbm:item/machine_obj_base"));
            Dejson.itemDefinition("hbm:item/" + handle.path()).add("model", model);
            Dejson.itemDefinition("hbm:item/" + handle.path()).addProperty("oversized_in_gui", true);
        };
    }

    /** RenderRBMKAutoloader's item pass: the raw OBJ, oversized in the GUI. */
    private static void autoloaderItem(Reg.Handle<?> handle) {
        JsonObject model = new JsonObject();
        model.addProperty("type", "hbm:machine_obj");
        model.addProperty("obj", "hbm:models/rbmk/autoloader.obj");
        model.addProperty("texture", "hbm:textures/models/machines/rbmk_autoloader.png");
        model.addProperty("scale", 1.0);
        model.addProperty("yaw", 180.0);
        model.addProperty("rawLegacyTransform", true);
        model.add("guiTranslate", numbers(0, -6, 0));
        model.addProperty("guiScale", 1.75);
        model.add("itemTranslate", numbers(0, 0, 0));
        model.add("itemPreTranslate", numbers(0, 0, 0));
        model.addProperty("ty", 0.0);
        JsonArray parts = new JsonArray();
        parts.add("Base");
        parts.add("Piston");
        model.add("parts", parts);
        handle.icon(d -> d.shares("hbm:item/machine_obj_base"));
        Dejson.itemDefinition("hbm:item/" + handle.path()).add("model", model);
        Dejson.itemDefinition("hbm:item/" + handle.path()).addProperty("oversized_in_gui", true);
    }

    private static JsonArray numbers(double... values) {
        JsonArray array = new JsonArray();
        for (double value : values) array.add(value);
        return array;
    }

    static {
        for (RegistryObject<? extends Block> block : List.of(rbmk_fuel_rod, rbmk_control, rbmk_control_mod, rbmk_control_auto, rbmk_control_reasim, rbmk_control_reasim_auto, rbmk_boiler, rbmk_moderator, rbmk_reflector, rbmk_absorber, rbmk_blank, rbmk_fuel_rod_moderated, rbmk_fuel_rod_reasim, rbmk_fuel_rod_reasim_moderated, rbmk_cooler, rbmk_storage, rbmk_heater, rbmk_outgasser, rbmk_steam_inlet, rbmk_steam_outlet, rbmk_console, rbmk_crane_console, rbmk_autoloader, rbmk_debris, rbmk_debris_burning, rbmk_debris_radiating, rbmk_debris_digamma, foam_layer)) {
            REG.blockItem(block).tab(HbmTab.MACHINES);
        }
        // The ROR peripherals, gauge, terminal and steam connector carry their 1.7 "$"-split descriptions.
        for (RegistryObject<? extends Block> block : List.of(rbmk_display_blank, rbmk_display, rbmk_key_pad, rbmk_lever,
                rbmk_gauge, rbmk_indicator, rbmk_numitron, rbmk_graph, rbmk_terminal, rbmk_loader)) {
            panelBlockItem(block);
        }
        REG.blockItem(block_graphite).tab(HbmTab.RESOURCES);
        REG.blockItem(sand_boron_layer).tab(HbmTab.BLOCKS);
    }

    @SuppressWarnings("unchecked")
    private static <B extends Block> void panelBlockItem(RegistryObject<B> block) {
        String id = ((Reg.Handle<B>) block).path();
        REG.<B, com.hbm.items.block.RBMKPanelBlockItem>blockItem(block,
                        (b, p) -> new com.hbm.items.block.RBMKPanelBlockItem(b, p, id), dev.golem.core.world.Compat::blockDescription)
                .tab(HbmTab.MACHINES);
    }
    public static void init() {}
}
