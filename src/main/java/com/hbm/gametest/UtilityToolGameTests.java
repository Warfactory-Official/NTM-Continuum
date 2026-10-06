package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.LemegetonRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.ModUtilityToolItems;
import com.hbm.items.tool.ItemPipette;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import dev.golem.core.Ids;
import dev.golem.core.data.ItemContents;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Contracts of the utility-tool wave: every id registered; the pipette's capacity, fill/empty, fizzle and
 * antimatter rules; the Lemegeton ladder; the containment box's squirt damping; and a duct's fluid copied and
 * pasted through {@code IFluidCopiable}.
 */
public final class UtilityToolGameTests {

    private UtilityToolGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function IDS =
            TESTS.function("utility_tools_ids", 20, () -> UtilityToolGameTests::ids);
    public static final GameTests.Function PIPETTE =
            TESTS.function("utility_tools_pipette", 20, () -> UtilityToolGameTests::pipette);
    public static final GameTests.Function LEMEGETON =
            TESTS.function("utility_tools_lemegeton", 20, () -> UtilityToolGameTests::lemegeton);
    public static final GameTests.Function CONTAINMENT =
            TESTS.function("utility_tools_containment", 20, () -> UtilityToolGameTests::containment);
    public static final GameTests.Function SETTINGS =
            TESTS.function("utility_tools_settings_duct", 40, () -> UtilityToolGameTests::settingsDuct);

    public static void init() {}

    public static void ids(GameTestHelper helper) {
        for (String id : ModUtilityToolItems.IDS) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Ids.of(Constants.MOD_ID, id)), "hbm:" + id + " is registered");
        }
        // 19 ids before the guide books, holotapes and clay tablets became one item per variant (History §19es-itemvariant):
        // book_guide 4 + holotape_image 18 + clay_tablet 2
        helper.assertTrue(ModUtilityToolItems.IDS.size() == 43, "43 item ids, got " + ModUtilityToolItems.IDS.size());
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(Ids.of(Constants.MOD_ID, "book_guide_block")),
                "the legacy manual block is registered");
        helper.succeed();
    }

    public static void pipette(GameTestHelper helper) {
        ItemPipette glass = ModUtilityToolItems.pipette.get();
        ItemStack stack = new ItemStack(glass);
        helper.assertTrue(glass.getCapacity(stack) == 1000 && glass.getFill(stack) == 0, "fresh: 0/1000");
        int rest = glass.tryFill(Fluids.WATER, 1500, stack);
        helper.assertTrue(rest == 500 && glass.getFill(stack) == 1000, "fills to capacity, returns the rest: " + rest);
        helper.assertTrue(glass.tryFill(Fluids.OIL, 10, stack) == 10, "refuses a second fluid");
        helper.assertTrue(glass.tryEmpty(Fluids.WATER, 400, stack) == 400 && glass.getFill(stack) == 600, "empties 400");
        helper.assertTrue(glass.tryEmpty(Fluids.WATER, 1000, stack) == 600 && glass.getType(stack) == Fluids.NONE,
                "empties the rest and forgets the fluid");

        ItemStack fizzle = new ItemStack(glass);
        glass.tryFill(Fluids.SULFURIC_ACID, 100, fizzle);
        helper.assertTrue(fizzle.isEmpty(), "the glass pipette fizzles on a corrosive fluid");
        ItemStack peroxide = new ItemStack(glass);
        glass.tryFill(Fluids.PEROXIDE, 100, peroxide);
        helper.assertTrue(!peroxide.isEmpty(), "peroxide is the exception");
        ItemStack boron = new ItemStack(ModUtilityToolItems.pipette_boron.get());
        ModUtilityToolItems.pipette_boron.get().tryFill(Fluids.SULFURIC_ACID, 100, boron);
        helper.assertTrue(!boron.isEmpty(), "the boron pipette holds acid");
        helper.assertTrue(!glass.acceptsFluid(Fluids.AMAT, new ItemStack(glass)), "no antimatter");

        ItemPipette lab = ModUtilityToolItems.pipette_laboratory.get();
        ItemStack labStack = new ItemStack(lab);
        helper.assertTrue(lab.getCapacity(labStack) == 50, "the laboratory pipette holds 50 mB");
        helper.succeed();
    }

    public static void lemegeton(GameTestHelper helper) {
        check(helper, new ItemStack(Items.IRON_INGOT), "hbm:ingot_steel");
        check(helper, new ItemStack(ModItems.ingot_uranium.get()), "hbm:ingot_u238");
        check(helper, new ItemStack(Items.GOLD_INGOT), "hbm:ingot_bismuth");
        check(helper, new ItemStack(Items.DIAMOND), "hbm:ingot_cft");
        helper.assertTrue(LemegetonRecipes.getRecipe(new ItemStack(Items.STICK)).isEmpty(), "a stick converts to nothing");
        helper.succeed();
    }

    private static void check(GameTestHelper helper, ItemStack in, String out) {
        ItemStack result = LemegetonRecipes.getRecipe(in);
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(result.getItem()).toString().equals(out),
                in + " -> " + out + ", got " + result);
    }

    public static void containment(GameTestHelper helper) {
        ItemStack ingot = new ItemStack(ModItems.ingot_uranium.get());
        double raw = HazardSystem.getHazardLevelFromStack(ingot, HazardRegistry.RADIATION);
        ItemStack box = new ItemStack(ModUtilityToolItems.containment_box.get());
        List<ItemStack> contents = new ArrayList<>();
        contents.add(ingot.copy());
        for (int i = 1; i < 20; i++) contents.add(ItemStack.EMPTY);
        ModUtilityToolItems.CONTAINMENT.set(box, new ItemContents(contents));
        double boxed = HazardSystem.getHazardLevelFromStack(box, HazardRegistry.RADIATION);
        double squirt = Math.sqrt(raw + 1D / ((raw + 2D) * (raw + 2D))) - 1D / (raw + 2D);
        helper.assertTrue(raw > 0 && Math.abs(boxed - squirt) < 1e-4, "boxed = squirt(" + raw + ") = " + squirt + ", got " + boxed);
        helper.succeed();
    }

    public static void settingsDuct(GameTestHelper helper) {
        BlockPos a = new BlockPos(1, 2, 1), b = new BlockPos(3, 2, 1);
        helper.setBlock(a, com.hbm.blocks.ModFluidNetBlocks.fluid_duct_neo.get());
        helper.setBlock(b, com.hbm.blocks.ModFluidNetBlocks.fluid_duct_neo.get());
        TileEntityFluidDuctNT ductA = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, a, TileEntityFluidDuctNT.class);
        TileEntityFluidDuctNT ductB = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, b, TileEntityFluidDuctNT.class);
        ductA.setFluidType(Fluids.WATER);
        CompoundTag settings = ductA.getSettings(helper.getLevel(), helper.absolutePos(a));
        helper.assertTrue(settings != null, "the duct has settings");
        ductB.pasteSettings(settings, 0, helper.getLevel(), dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL),
                helper.absolutePos(b));
        helper.assertTrue(ductB.getFluidType() == Fluids.WATER, "duct B took water, got " + ductB.getFluidType());
        helper.succeed();
    }
}
