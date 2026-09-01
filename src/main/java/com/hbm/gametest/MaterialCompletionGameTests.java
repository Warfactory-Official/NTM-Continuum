package com.hbm.gametest;

import com.hbm.blocks.material.BlockConcreteColored;
import com.hbm.blocks.material.BlockConcreteColoredExt;
import com.hbm.blocks.material.BlockLightstone;
import com.hbm.blocks.material.MaterialCompletionRegistrations;
import com.hbm.blocks.generic.HbmStairBlock;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModFuelItems;
import com.hbm.inventory.material.Mats;
import com.hbm.items.material.FuelAdditiveItems;
import com.hbm.inventory.recipes.material.FuelAdditiveRecipes;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Focused contracts for the isolated material, colored-block, and fuel-additive completion package. */
public final class MaterialCompletionGameTests {

    private MaterialCompletionGameTests() {}

    /** Forward this feature-local suite from each loader; HbmGameTestFunctions remains untouched. */
    public static final GameTests.Suite TESTS = GameTests.suite("hbm");
    public static final GameTests.Function ENUM_ORDERS_AND_REGISTRY =
            TESTS.function("material_enum_orders_and_registry", 40,
                    () -> MaterialCompletionGameTests::enumOrdersCreativeAndPlacement);
    public static final GameTests.Function COLORED_STAIR_IDS_AND_TEXTURES =
            TESTS.function("material_colored_stair_ids_and_textures", 40,
                    () -> MaterialCompletionGameTests::coloredStairIdsAndTextures);
    public static final GameTests.Function LIGHTSTONE_ROUND_TRIP_AND_LIGHT =
            TESTS.function("material_lightstone_round_trip_and_light", 40,
                    () -> MaterialCompletionGameTests::lightstoneRoundTripAndLight);
    public static final GameTests.Function MATERIAL_IDENTITY_AND_SMELTING =
            TESTS.function("material_identity_and_smelting", 40,
                    () -> MaterialCompletionGameTests::materialIdentityAndSmelting);
    public static final GameTests.Function FUEL_ADDITIVE_VARIANTS =
            TESTS.function("material_fuel_additive_variants", 40,
                    () -> MaterialCompletionGameTests::fuelAdditiveVariants);

    public static void init() {}

    public static void enumOrdersCreativeAndPlacement(GameTestHelper helper) {
        helper.assertTrue(List.of(DyeColor.values()).equals(List.of(
                        DyeColor.WHITE, DyeColor.ORANGE, DyeColor.MAGENTA, DyeColor.LIGHT_BLUE,
                        DyeColor.YELLOW, DyeColor.LIME, DyeColor.PINK, DyeColor.GRAY,
                        DyeColor.LIGHT_GRAY, DyeColor.CYAN, DyeColor.PURPLE, DyeColor.BLUE,
                        DyeColor.BROWN, DyeColor.GREEN, DyeColor.RED, DyeColor.BLACK)),
                "Concrete colour order no longer matches legacy metadata");
        helper.assertTrue(BlockConcreteColoredExt.EnumConcreteType.values().length == 8,
                "Extended concrete must retain eight variants");
        helper.assertTrue(BlockLightstone.LightstoneType.values().length == 5,
                "Lightstone must retain five variants");
        helper.assertTrue(MaterialCompletionRegistrations.CONCRETE_COLORED_STAIRS.size() == 16
                        && MaterialCompletionRegistrations.CONCRETE_COLORED_EXT_STAIRS.size() == 8,
                "Flattened colored stair count changed");
        BlockConcreteColored block = MaterialCompletionRegistrations.CONCRETE_COLORED.get();
        for (int variant = 0; variant < DyeColor.values().length; variant++) {
            ItemStack stack = MaterialCompletionRegistrations.concreteColoredItem(variant);
            helper.assertTrue(ModDataComponents.ITEM_VARIANT.getOr(stack, -1) == variant,
                    "Concrete item lost variant " + variant);
            helper.assertTrue(BlockConcreteColored.variant(block.stateForItem(stack)) == variant,
                    "Concrete placement changed variant " + variant);
            ItemStack clone = block.getCloneItemStack((net.minecraft.world.level.BlockGetter) null,
                    BlockPos.ZERO, block.stateForVariant(variant));
            helper.assertTrue(ModDataComponents.ITEM_VARIANT.getOr(clone, -1) == variant,
                    "Concrete clone lost variant " + variant);
            ItemStack drop = block.getDrops(block.stateForVariant(variant), null).get(0);
            helper.assertTrue(ModDataComponents.ITEM_VARIANT.getOr(drop, -1) == variant,
                    "Concrete drop lost variant " + variant);
        }
        helper.succeed();
    }

    public static void coloredStairIdsAndTextures(GameTestHelper helper) {
        List<String> colours = List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
                "gray", "silver", "cyan", "purple", "blue", "brown", "green", "red", "black");
        for (int i = 0; i < colours.size(); i++) {
            RegistryObject<Block> stair = MaterialCompletionRegistrations.CONCRETE_COLORED_STAIRS.get(i);
            helper.assertTrue(stair.getId().equals(Ids.of("hbm", "concrete_colored_stairs_" + colours.get(i))),
                    "Wrong colored stair id at " + i);
            helper.assertTrue(MaterialCompletionRegistrations.concreteColoredStairTexture(i)
                            .equals("hbm:blocks/concrete_" + colours.get(i)),
                    "Wrong colored stair texture at " + i);
            helper.assertTrue(stair.get() instanceof HbmStairBlock, "Colored stair is not a vanilla stair block");
        }
        for (int i = 0; i < BlockConcreteColoredExt.EnumConcreteType.values().length; i++) {
            String name = BlockConcreteColoredExt.EnumConcreteType.values()[i].getSerializedName();
            helper.assertTrue(MaterialCompletionRegistrations.CONCRETE_COLORED_EXT_STAIRS.get(i).getId()
                            .equals(Ids.of("hbm", "concrete_colored_ext_stairs_" + name)),
                    "Wrong extended stair id at " + i);
            helper.assertTrue(MaterialCompletionRegistrations.concreteColoredExtStairTexture(i)
                            .equals("hbm:blocks/concrete_colored_ext." + name),
                    "Wrong extended stair texture at " + i);
        }
        helper.assertTrue(MaterialCompletionRegistrations.lightstoneStairTexture(false)
                        .equals("hbm:blocks/lightstone.tile")
                        && MaterialCompletionRegistrations.lightstoneStairTexture(true)
                        .equals("hbm:blocks/lightstone.bricks"),
                "Wrong lightstone stair texture mapping");
        helper.succeed();
    }

    public static void lightstoneRoundTripAndLight(GameTestHelper helper) {
        BlockLightstone block = MaterialCompletionRegistrations.LIGHTSTONE.get();
        for (int variant = 0; variant < BlockLightstone.LightstoneType.values().length; variant++) {
            ItemStack stack = MaterialCompletionRegistrations.lightstoneItem(variant);
            helper.assertTrue(BlockLightstone.variant(block.stateForItem(stack)) == variant,
                    "Lightstone placement changed variant " + variant);
            helper.assertTrue(block.stateForVariant(variant).getLightEmission() == 0,
                    "Legacy lightstone variant unexpectedly emits light: " + variant);
            ItemStack clone = block.getCloneItemStack((net.minecraft.world.level.BlockGetter) null,
                    BlockPos.ZERO, block.stateForVariant(variant));
            helper.assertTrue(ModDataComponents.ITEM_VARIANT.getOr(clone, -1) == variant,
                    "Lightstone clone lost variant " + variant);
            ItemStack drop = block.getDrops(block.stateForVariant(variant), null).get(0);
            helper.assertTrue(ModDataComponents.ITEM_VARIANT.getOr(drop, -1) == variant,
                    "Lightstone drop lost variant " + variant);
        }
        helper.succeed();
    }

    public static void materialIdentityAndSmelting(GameTestHelper helper) {
        helper.assertTrue(Mats.MAT_IRON.id == 2600 && Mats.MAT_IRON.name.equals("iron"),
                "Iron material identity changed");
        List<Mats.MaterialStack> iron = Mats.getSmeltingMaterialsFromItem(new ItemStack(Items.IRON_INGOT));
        helper.assertTrue(iron.size() == 1 && iron.get(0).material == Mats.MAT_IRON
                        && iron.get(0).amount == 72,
                "Iron ingot smelting sample changed");
        List<Mats.MaterialStack> coal = Mats.getSmeltingMaterialsFromItem(
                new ItemStack(ModFuelItems.powder_coal.get()));
        // The current shared tag lookup can also retain the explicit powder_coal entry. This leaf does
        // not own Mats deduplication, so assert the old conversion entry rather than collection size.
        helper.assertTrue(coal.stream().anyMatch(stack -> stack.material == Mats.MAT_CARBON
                        && stack.amount == 36)
                        && Mats.MAT_COAL.smeltsInto == Mats.MAT_CARBON
                        && Mats.MAT_COAL.convIn == 2 && Mats.MAT_COAL.convOut == 1,
                "Coal-to-carbon conversion sample changed");
        helper.succeed();
    }

    public static void fuelAdditiveVariants(GameTestHelper helper) {
        helper.assertTrue(FuelAdditiveItems.Variant.values().length == 2,
                "Fuel additive must retain exactly two variants");
        for (FuelAdditiveItems.Variant variant : FuelAdditiveItems.Variant.values()) {
            var item = FuelAdditiveItems.get(variant);
            helper.assertTrue(item.getClass().equals(com.hbm.items.material.FuelAdditiveItem.class),
                    "Fuel additive is not a plain feature item");
            helper.assertTrue(BuiltInRegistries.ITEM.getKey(item).equals(Ids.of("hbm", variant.id)),
                    "Wrong fuel additive id for " + variant);
        }
        helper.assertTrue(FuelAdditiveItems.get(FuelAdditiveItems.Variant.ANTIKNOCK)
                        != FuelAdditiveItems.get(FuelAdditiveItems.Variant.DEICER),
                "Flattened fuel additives must be distinct items");
        var chemical = FuelAdditiveRecipes.chemicalPlantRecipes();
        helper.assertTrue(chemical.size() == 2,
                "Fuel additive chemical catalogue must contain TEL and deicer");
        helper.assertTrue(chemical.get(0).getInternalName().equals("chem.tel")
                        && chemical.get(1).getInternalName().equals("chem.deicer")
                        && chemical.get(0).outputItem[0].getItem()
                        == FuelAdditiveItems.get(FuelAdditiveItems.Variant.ANTIKNOCK)
                        && chemical.get(1).outputItem[0].getItem()
                        == FuelAdditiveItems.get(FuelAdditiveItems.Variant.DEICER),
                "Fuel additive chemical recipe IDs changed");
        var mixer = FuelAdditiveRecipes.mixerRecipes();
        helper.assertTrue(mixer.size() == 3
                        && mixer.get(0).output() == com.hbm.inventory.fluid.Fluids.PETROIL_LEADED
                        && mixer.get(1).output() == com.hbm.inventory.fluid.Fluids.GASOLINE_LEADED
                        && mixer.get(2).output() == com.hbm.inventory.fluid.Fluids.COALGAS_LEADED
                        && mixer.get(0).input() == com.hbm.inventory.fluid.Fluids.PETROIL
                        && mixer.get(1).input() == com.hbm.inventory.fluid.Fluids.GASOLINE
                        && mixer.get(2).input() == com.hbm.inventory.fluid.Fluids.COALGAS
                        && mixer.stream().allMatch(recipe ->
                        recipe.additive() == FuelAdditiveItems.Variant.ANTIKNOCK
                                && recipe.outputAmount() == 12_000
                                && recipe.inputAmount() == 10_000
                                && recipe.processTime() == 40),
                "Leaded Mixer recipes changed or use the wrong additive variant");
        helper.succeed();
    }
}
