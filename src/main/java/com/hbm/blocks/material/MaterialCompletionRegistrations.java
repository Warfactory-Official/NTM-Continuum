package com.hbm.blocks.material;

import dev.golem.core.dejson.Variants;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Model;
import dev.golem.core.dejson.ItemModels;
import com.hbm.registration.Reg;
import com.hbm.registration.Presets;
import com.hbm.registration.Loot;
import com.hbm.registration.BlockTag;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.material.FuelAdditiveItems;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/** Isolated registrations for the metadata families omitted from the generated block registry. */
public final class MaterialCompletionRegistrations {

    private MaterialCompletionRegistrations() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /**
     * {@link BlockConcreteColored#legacyName}, spelled out: vanilla's {@code DyeColor} order with LIGHT_GRAY under
     * its 1.7 name "silver". Neither {@code DyeColor} nor the block class may be touched here, since this class also
     * initialises in DejsonMain's plain JVM (1.20.1 builds its registries the moment either loads).
     */
    private static final String[] LEGACY_DYE_NAMES = {"white", "orange", "magenta", "light_blue", "yellow", "lime",
            "pink", "gray", "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"};

    public static final RegistryObject<BlockConcreteColored> CONCRETE_COLORED = REG.block(
            "concrete_colored", BlockConcreteColored::new,
            p -> p.mapColor(state -> state.getValue(BlockConcreteColored.COLOR).getMapColor())
                    .strength(15.0F, 140.0F).sound(SoundType.STONE).requiresCorrectToolForDrops())
            .model(d -> d.models(Models.group()
                    .and("_black", Models.cubeAll("hbm:blocks/concrete_black"))
                    .and("_blue", Models.cubeAll("hbm:blocks/concrete_blue"))
                    .and("_brown", Models.cubeAll("hbm:blocks/concrete_brown"))
                    .and("_cyan", Models.cubeAll("hbm:blocks/concrete_cyan"))
                    .and("_gray", Models.cubeAll("hbm:blocks/concrete_gray"))
                    .and("_green", Models.cubeAll("hbm:blocks/concrete_green"))
                    .and("_light_blue", Models.cubeAll("hbm:blocks/concrete_light_blue"))
                    .and("_lime", Models.cubeAll("hbm:blocks/concrete_lime"))
                    .and("_magenta", Models.cubeAll("hbm:blocks/concrete_magenta"))
                    .and("_orange", Models.cubeAll("hbm:blocks/concrete_orange"))
                    .and("_pink", Models.cubeAll("hbm:blocks/concrete_pink"))
                    .and("_purple", Models.cubeAll("hbm:blocks/concrete_purple"))
                    .and("_red", Models.cubeAll("hbm:blocks/concrete_red"))
                    .and("_silver", Models.cubeAll("hbm:blocks/concrete_silver"))
                    .and("_white", Models.cubeAll("hbm:blocks/concrete_white"))
                    .and("_yellow", Models.cubeAll("hbm:blocks/concrete_yellow")))
                    .states(Variants.select("color", "white", "_white", "orange", "_orange", "magenta", "_magenta", "light_blue", "_light_blue", "yellow", "_yellow", "lime", "_lime", "pink", "_pink", "gray", "_gray", "light_gray", "_silver", "cyan", "_cyan", "purple", "_purple", "blue", "_blue", "brown", "_brown", "green", "_green", "red", "_red", "black", "_black")))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockConcreteColoredExt> CONCRETE_COLORED_EXT = REG.block(
            "concrete_colored_ext", BlockConcreteColoredExt::new,
            p -> p.strength(15.0F, 140.0F).sound(SoundType.STONE).requiresCorrectToolForDrops())
            .model(d -> d.models(Models.group()
                    .and("_bronze", Models.cubeAll("hbm:blocks/concrete_colored_ext.bronze"))
                    .and("_hazard", Models.cubeAll("hbm:blocks/concrete_colored_ext.hazard"))
                    .and("_indigo", Models.cubeAll("hbm:blocks/concrete_colored_ext.indigo"))
                    .and("_machine", Models.cubeAll("hbm:blocks/concrete_colored_ext.machine"))
                    .and("_machine_stripe", Models.parented("minecraft:block/cube", Slots.of("up", "hbm:blocks/concrete_colored_ext.machine")
                    .and("down", "hbm:blocks/concrete_colored_ext.machine")
                    .and("north", "hbm:blocks/concrete_colored_ext.machine_stripe")
                    .and("south", "hbm:blocks/concrete_colored_ext.machine_stripe")
                    .and("west", "hbm:blocks/concrete_colored_ext.machine_stripe")
                    .and("east", "hbm:blocks/concrete_colored_ext.machine_stripe")
                    .and("particle", "hbm:blocks/concrete_colored_ext.machine_stripe")))
                    .and("_pink", Models.cubeAll("hbm:blocks/concrete_colored_ext.pink"))
                    .and("_purple", Models.cubeAll("hbm:blocks/concrete_colored_ext.purple"))
                    .and("_sand", Models.cubeAll("hbm:blocks/concrete_colored_ext.sand")))
                    .states(Variants.select("type", "machine", "_machine", "machine_stripe", "_machine_stripe", "indigo", "_indigo", "purple", "_purple", "pink", "_pink", "hazard", "_hazard", "sand", "_sand", "bronze", "_bronze")))
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockLightstone> LIGHTSTONE = REG.block(
            "lightstone", BlockLightstone::new,
            p -> p.strength(20.0F, 20.0F).sound(SoundType.STONE).requiresCorrectToolForDrops())
            .model(d -> d.models(Models.group()
                    .and("_bricks", Models.cubeAll("hbm:blocks/lightstone.bricks"))
                    .and("_bricks_chiseled", Models.parented("minecraft:block/cube", Slots.of("up", "hbm:blocks/lightstone.bricks_chiseled.top")
                    .and("down", "hbm:blocks/lightstone.bricks_chiseled.top")
                    .and("north", "hbm:blocks/lightstone.bricks_chiseled")
                    .and("south", "hbm:blocks/lightstone.bricks_chiseled")
                    .and("west", "hbm:blocks/lightstone.bricks_chiseled")
                    .and("east", "hbm:blocks/lightstone.bricks_chiseled")
                    .and("particle", "hbm:blocks/lightstone.bricks_chiseled")))
                    .and("_chiseled", Models.parented("minecraft:block/cube", Slots.of("up", "hbm:blocks/lightstone.chiseled.top")
                    .and("down", "hbm:blocks/lightstone.chiseled.top")
                    .and("north", "hbm:blocks/lightstone.chiseled")
                    .and("south", "hbm:blocks/lightstone.chiseled")
                    .and("west", "hbm:blocks/lightstone.chiseled")
                    .and("east", "hbm:blocks/lightstone.chiseled")
                    .and("particle", "hbm:blocks/lightstone.chiseled")))
                    .and("_tile", Models.cubeAll("hbm:blocks/lightstone.tile"))
                    .and("_unrefined", Models.cubeAll("hbm:blocks/lightstone.unrefined")))
                    .states(Variants.select("type", "unrefined", "_unrefined", "tile", "_tile", "bricks", "_bricks", "bricks_chiseled", "_bricks_chiseled", "chiseled", "_chiseled")))
            .tag(BlockTag.PICKAXE);

    public static final List<RegistryObject<Block>> CONCRETE_COLORED_STAIRS = stairs(
            "concrete_colored_stairs", CONCRETE_COLORED, 16, 15.0F, 94.0F);
    public static final List<RegistryObject<Block>> CONCRETE_COLORED_EXT_STAIRS = stairs(
            "concrete_colored_ext_stairs", CONCRETE_COLORED_EXT, 8, 15.0F, 94.0F);
    public static final RegistryObject<Block> LIGHTSTONE_TILE_STAIRS = stair(
            "lightstone_tile_stairs", LIGHTSTONE, lightstoneStairTexture(false), 15.0F, 20.0F);
    public static final RegistryObject<Block> LIGHTSTONE_BRICKS_STAIRS = stair(
            "lightstone_bricks_stairs", LIGHTSTONE, lightstoneStairTexture(true), 15.0F, 20.0F);

    // Each item selects its variant through the item definition; the base model carries no overrides (below 1.21.4
    // legacy-assets writes them onto it from the same definition).
    public static final RegistryObject<MaterialCompletionBlockItem> CONCRETE_COLORED_ITEM = REG.item(
            "concrete_colored", p -> new MaterialCompletionBlockItem(CONCRETE_COLORED.get(), p,
                    "concrete_colored", 16, BlockConcreteColored::legacyName),
            Compat::blockDescription)
            .icon(d -> d.models(Model.parent("hbm:block/concrete_colored_white"))
                    .definition(ItemModels.select("hbm:item_variant")
                    .when("0", ItemModels.model("hbm:block/concrete_colored_white"))
                    .when("1", ItemModels.model("hbm:block/concrete_colored_orange"))
                    .when("2", ItemModels.model("hbm:block/concrete_colored_magenta"))
                    .when("3", ItemModels.model("hbm:block/concrete_colored_light_blue"))
                    .when("4", ItemModels.model("hbm:block/concrete_colored_yellow"))
                    .when("5", ItemModels.model("hbm:block/concrete_colored_lime"))
                    .when("6", ItemModels.model("hbm:block/concrete_colored_pink"))
                    .when("7", ItemModels.model("hbm:block/concrete_colored_gray"))
                    .when("8", ItemModels.model("hbm:block/concrete_colored_silver"))
                    .when("9", ItemModels.model("hbm:block/concrete_colored_cyan"))
                    .when("10", ItemModels.model("hbm:block/concrete_colored_purple"))
                    .when("11", ItemModels.model("hbm:block/concrete_colored_blue"))
                    .when("12", ItemModels.model("hbm:block/concrete_colored_brown"))
                    .when("13", ItemModels.model("hbm:block/concrete_colored_green"))
                    .when("14", ItemModels.model("hbm:block/concrete_colored_red"))
                    .when("15", ItemModels.model("hbm:block/concrete_colored_black"))
                    .fallback(ItemModels.model("hbm:block/concrete_colored_white"))));
    public static final RegistryObject<MaterialCompletionBlockItem> CONCRETE_COLORED_EXT_ITEM = REG.item(
            "concrete_colored_ext", p -> new MaterialCompletionBlockItem(CONCRETE_COLORED_EXT.get(), p,
                    "concrete_colored_ext", 8, i -> BlockConcreteColoredExt.type(i).getSerializedName()),
            Compat::blockDescription)
            .icon(d -> d.models(Model.parent("hbm:block/concrete_colored_ext_machine"))
                    .definition(ItemModels.select("hbm:item_variant")
                    .when("0", ItemModels.model("hbm:block/concrete_colored_ext_machine"))
                    .when("1", ItemModels.model("hbm:block/concrete_colored_ext_machine_stripe"))
                    .when("2", ItemModels.model("hbm:block/concrete_colored_ext_indigo"))
                    .when("3", ItemModels.model("hbm:block/concrete_colored_ext_purple"))
                    .when("4", ItemModels.model("hbm:block/concrete_colored_ext_pink"))
                    .when("5", ItemModels.model("hbm:block/concrete_colored_ext_hazard"))
                    .when("6", ItemModels.model("hbm:block/concrete_colored_ext_sand"))
                    .when("7", ItemModels.model("hbm:block/concrete_colored_ext_bronze"))
                    .fallback(ItemModels.model("hbm:block/concrete_colored_ext_machine"))));
    public static final RegistryObject<MaterialCompletionBlockItem> LIGHTSTONE_ITEM = REG.item(
            "lightstone", p -> new MaterialCompletionBlockItem(LIGHTSTONE.get(), p,
                    "lightstone", 5, i -> BlockLightstone.type(i).getSerializedName()),
            Compat::blockDescription)
            .icon(d -> d.models(Model.parent("hbm:block/lightstone_unrefined"))
                    .definition(ItemModels.select("hbm:item_variant")
                    .when("0", ItemModels.model("hbm:block/lightstone_unrefined"))
                    .when("1", ItemModels.model("hbm:block/lightstone_tile"))
                    .when("2", ItemModels.model("hbm:block/lightstone_bricks"))
                    .when("3", ItemModels.model("hbm:block/lightstone_bricks_chiseled"))
                    .when("4", ItemModels.model("hbm:block/lightstone_chiseled"))
                    .fallback(ItemModels.model("hbm:block/lightstone_unrefined"))));

    static {
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> IntStream.range(0, 16)
                .mapToObj(MaterialCompletionRegistrations::concreteColoredItem).toList());
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> IntStream.range(0, 8)
                .mapToObj(MaterialCompletionRegistrations::concreteColoredExtItem).toList());
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> IntStream.range(0, 5)
                .mapToObj(MaterialCompletionRegistrations::lightstoneItem).toList());
        for (RegistryObject<Block> stair : CONCRETE_COLORED_STAIRS) {
            REG.blockItem(stair, Compat::blockDescription).tab(HbmTab.BLOCKS);
        }
        for (RegistryObject<Block> stair : CONCRETE_COLORED_EXT_STAIRS) {
            REG.blockItem(stair, Compat::blockDescription).tab(HbmTab.BLOCKS);
        }
        REG.blockItem(LIGHTSTONE_TILE_STAIRS, Compat::blockDescription).tab(HbmTab.BLOCKS);
        REG.blockItem(LIGHTSTONE_BRICKS_STAIRS, Compat::blockDescription).tab(HbmTab.BLOCKS);
    }

    private static List<RegistryObject<Block>> stairs(String prefix, Supplier<? extends Block> base, int count,
                                                      float hardness, float resistance) {
        List<RegistryObject<Block>> stairs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // the variant names come from the blocks' own enums, read without initialising the block classes
            // (a Block subclass needs a running game; this class also initialises in DejsonMain's plain JVM)
            String id = prefix + "_" + (prefix.startsWith("concrete_colored_ext")
                    ? BlockConcreteColoredExt.EnumConcreteType.VALUES[i].getSerializedName()
                    : legacyDyeName(i));
            String texture = prefix.startsWith("concrete_colored_ext") ? concreteColoredExtStairTexture(i)
                    : concreteColoredStairTexture(i);
            stairs.add(stair(id, base, texture, hardness, resistance));
        }
        return stairs;
    }

    /** A stair over one texture; drops itself, pickaxe. */
    private static RegistryObject<Block> stair(String id, Supplier<? extends Block> base, String texture,
                                                float hardness, float resistance) {
        return REG.<Block>block(id, p -> new com.hbm.blocks.generic.HbmStairBlock(
                                base.get().defaultBlockState(), p),
                        p -> p.strength(hardness, resistance).sound(SoundType.STONE).requiresCorrectToolForDrops())
                .transform(Presets.shape(Models.stairsAll(texture)).states(Variants.stairs()))
                .loot(Loot.self())
                .tag(BlockTag.PICKAXE);
    }

    public static ItemStack concreteColoredItem(int variant) {
        return CONCRETE_COLORED_ITEM.get().of(variant);
    }

    public static ItemStack concreteColoredExtItem(int variant) {
        return CONCRETE_COLORED_EXT_ITEM.get().of(variant);
    }

    public static ItemStack lightstoneItem(int variant) {
        return LIGHTSTONE_ITEM.get().of(variant);
    }

    public static String concreteColoredStairTexture(int variant) {
        return "hbm:blocks/concrete_" + legacyDyeName(variant);
    }

    public static String concreteColoredExtStairTexture(int variant) {
        return "hbm:blocks/concrete_colored_ext."
                + BlockConcreteColoredExt.EnumConcreteType.VALUES[variant].getSerializedName();
    }

    private static String legacyDyeName(int variant) {
        return LEGACY_DYE_NAMES[Math.floorMod(variant, LEGACY_DYE_NAMES.length)];
    }

    public static String lightstoneStairTexture(boolean bricks) {
        return "hbm:blocks/lightstone." + (bricks ? "bricks" : "tile");
    }

    public static void init() {
        FuelAdditiveItems.init();
    }
}
