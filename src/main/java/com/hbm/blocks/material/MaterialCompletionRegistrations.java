package com.hbm.blocks.material;

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

    public static final RegistryObject<BlockConcreteColored> CONCRETE_COLORED = BLOCKS.registerBlock(
            "concrete_colored", BlockConcreteColored::new,
            p -> p.mapColor(state -> state.getValue(BlockConcreteColored.COLOR).getMapColor())
                    .strength(15.0F, 140.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final RegistryObject<BlockConcreteColoredExt> CONCRETE_COLORED_EXT = BLOCKS.registerBlock(
            "concrete_colored_ext", BlockConcreteColoredExt::new,
            p -> p.strength(15.0F, 140.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final RegistryObject<BlockLightstone> LIGHTSTONE = BLOCKS.registerBlock(
            "lightstone", BlockLightstone::new,
            p -> p.strength(20.0F, 20.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());

    public static final List<RegistryObject<Block>> CONCRETE_COLORED_STAIRS = stairs(
            "concrete_colored_stairs", CONCRETE_COLORED, 16, 15.0F, 94.0F);
    public static final List<RegistryObject<Block>> CONCRETE_COLORED_EXT_STAIRS = stairs(
            "concrete_colored_ext_stairs", CONCRETE_COLORED_EXT, 8, 15.0F, 94.0F);
    public static final RegistryObject<Block> LIGHTSTONE_TILE_STAIRS = stairs(
            "lightstone_tile_stairs", LIGHTSTONE, 15.0F, 20.0F);
    public static final RegistryObject<Block> LIGHTSTONE_BRICKS_STAIRS = stairs(
            "lightstone_bricks_stairs", LIGHTSTONE, 15.0F, 20.0F);

    public static final RegistryObject<MaterialCompletionBlockItem> CONCRETE_COLORED_ITEM = ITEMS.registerItem(
            "concrete_colored", p -> new MaterialCompletionBlockItem(CONCRETE_COLORED.get(), p,
                    "concrete_colored", 16, BlockConcreteColored::legacyName),
            Compat::blockDescription);
    public static final RegistryObject<MaterialCompletionBlockItem> CONCRETE_COLORED_EXT_ITEM = ITEMS.registerItem(
            "concrete_colored_ext", p -> new MaterialCompletionBlockItem(CONCRETE_COLORED_EXT.get(), p,
                    "concrete_colored_ext", 8, i -> BlockConcreteColoredExt.type(i).getSerializedName()),
            Compat::blockDescription);
    public static final RegistryObject<MaterialCompletionBlockItem> LIGHTSTONE_ITEM = ITEMS.registerItem(
            "lightstone", p -> new MaterialCompletionBlockItem(LIGHTSTONE.get(), p,
                    "lightstone", 5, i -> BlockLightstone.type(i).getSerializedName()),
            Compat::blockDescription);

    static {
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> IntStream.range(0, 16)
                .mapToObj(MaterialCompletionRegistrations::concreteColoredItem).toList());
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> IntStream.range(0, 8)
                .mapToObj(MaterialCompletionRegistrations::concreteColoredExtItem).toList());
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> IntStream.range(0, 5)
                .mapToObj(MaterialCompletionRegistrations::lightstoneItem).toList());
        for (RegistryObject<Block> stair : CONCRETE_COLORED_STAIRS) {
            ModTabs.addTo(HbmTab.BLOCKS, ITEMS.registerBlockItem(stair.getId().getPath(), stair,
                    Compat::blockDescription));
        }
        for (RegistryObject<Block> stair : CONCRETE_COLORED_EXT_STAIRS) {
            ModTabs.addTo(HbmTab.BLOCKS, ITEMS.registerBlockItem(stair.getId().getPath(), stair,
                    Compat::blockDescription));
        }
        ModTabs.addTo(HbmTab.BLOCKS, ITEMS.registerBlockItem("lightstone_tile_stairs",
                LIGHTSTONE_TILE_STAIRS, Compat::blockDescription));
        ModTabs.addTo(HbmTab.BLOCKS, ITEMS.registerBlockItem("lightstone_bricks_stairs",
                LIGHTSTONE_BRICKS_STAIRS, Compat::blockDescription));
    }

    private static List<RegistryObject<Block>> stairs(String prefix, Supplier<? extends Block> base, int count,
                                                      float hardness, float resistance) {
        List<RegistryObject<Block>> stairs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = prefix + "_" + (prefix.startsWith("concrete_colored_ext")
                    ? BlockConcreteColoredExt.type(i).getSerializedName()
                    : BlockConcreteColored.legacyName(i));
            stairs.add(stair(id, base, hardness, resistance));
        }
        return stairs;
    }

    private static RegistryObject<Block> stairs(String id, Supplier<? extends Block> base,
                                                 float hardness, float resistance) {
        return stair(id, base, hardness, resistance);
    }

    private static RegistryObject<Block> stair(String id, Supplier<? extends Block> base,
                                                float hardness, float resistance) {
        return BLOCKS.registerBlock(id, p -> new com.hbm.blocks.generic.HbmStairBlock(
                        base.get().defaultBlockState(), p),
                p -> p.strength(hardness, resistance).sound(SoundType.STONE).requiresCorrectToolForDrops());
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
        return "hbm:blocks/concrete_" + BlockConcreteColored.legacyName(variant);
    }

    public static String concreteColoredExtStairTexture(int variant) {
        return "hbm:blocks/concrete_colored_ext." + BlockConcreteColoredExt.type(variant).getSerializedName();
    }

    public static String lightstoneStairTexture(boolean bricks) {
        return "hbm:blocks/lightstone." + (bricks ? "bricks" : "tile");
    }

    public static void init() {
        FuelAdditiveItems.init();
    }
}
