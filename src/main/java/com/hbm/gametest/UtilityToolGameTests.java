package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.LemegetonRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.ModUtilityToolItems;
import com.hbm.items.special.ItemGlitch;
import com.hbm.items.tool.ItemPipette;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import dev.golem.core.Ids;
import dev.golem.core.data.ItemContents;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Effects;
import dev.golem.core.world.ItemStacks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Contracts of the utility tools: every id registered; the pipette's capacity, fill/empty, fizzle and
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
    public static final GameTests.Function GLITCH =
            TESTS.function("utility_tools_glitch", 20, () -> UtilityToolGameTests::glitch);
    public static final GameTests.Function CONTAINMENT =
            TESTS.function("utility_tools_containment", 20, () -> UtilityToolGameTests::containment);
    public static final GameTests.Function SETTINGS =
            TESTS.function("utility_tools_settings_duct", 40, () -> UtilityToolGameTests::settingsDuct);
    public static final GameTests.Function TAB_STACKS =
            TESTS.function("utility_tools_tab_stacks_distinct", 20, () -> UtilityToolGameTests::tabStacksDistinct);

    public static void init() {}

    public static void ids(GameTestHelper helper) {
        for (String id : ModUtilityToolItems.IDS) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Ids.of(Constants.MOD_ID, id)), "hbm:" + id + " is registered");
        }
        // 21 other ids, plus one item per variant of the guide books, holotapes and clay tablets:
        // book_guide 4 + holotape_image 18 + clay_tablet 2
        helper.assertTrue(ModUtilityToolItems.IDS.size() == 45, "45 item ids, got " + ModUtilityToolItems.IDS.size());
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
        helper.setBlock(a, ModFluidNetBlocks.fluid_duct_neo.get());
        helper.setBlock(b, ModFluidNetBlocks.fluid_duct_neo.get());
        TileEntityFluidDuctNT ductA = GameTestHelpers.blockEntity(helper, a, TileEntityFluidDuctNT.class);
        TileEntityFluidDuctNT ductB = GameTestHelpers.blockEntity(helper, b, TileEntityFluidDuctNT.class);
        ductA.setFluidType(Fluids.WATER);
        CompoundTag settings = ductA.getSettings(helper.getLevel(), helper.absolutePos(a));
        helper.assertTrue(settings != null, "the duct has settings");
        ductB.pasteSettings(settings, 0, helper.getLevel(), GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL),
                helper.absolutePos(b));
        helper.assertTrue(ductB.getFluidType() == Fluids.WATER, "duct B took water, got " + ductB.getFluidType());
        helper.succeed();
    }

    /** Each of 1.7's glitch outcomes that stays in the player's hands; the ones that drop boxcars or fire are left out. */
    private static void glitch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int roll = 0; roll < 31; roll++) {
            if (roll == 2 || roll == 3 || roll == 4 || roll == 11 || roll == 12 || roll == 14 || roll >= 27 && roll <= 29) continue;
            Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
            ItemGlitch.effect(roll, level, player);
            if (roll == 7) {
                helper.assertTrue(player.getInventory().countItem(ModBlocks.block_meteor_treasure.get().asItem()) == 10,
                        "roll 7 gives ten meteor treasure blocks");
            }
            if (roll == 24) helper.assertTrue(Effects.has(player, Effects.resistance()), "roll 24 gives resistance");
            if (roll == 30) {
                helper.assertTrue(player.getInventory().countItem(ModItems.plate_saturnite.get()) == 1, "roll 30 gives a saturnite plate");
            }
        }
        Player hurt = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ItemGlitch.effect(2, level, hurt);
        helper.assertTrue(hurt.isDeadOrDying(), "roll 2 kills with radiation");
        helper.succeed();
    }

    /** A creative tab refuses to build when two of its stacks are equal, and JEI then lists none of that tab. */
    public static void tabStacksDistinct(GameTestHelper helper) {
        int identifiers = 0;
        for (HbmTab tab : HbmTab.values()) {
            List<ItemStack> stacks = ModTabs.displayStacks(ModTabs.get(tab).getKey());
            for (int i = 0; i < stacks.size(); i++) {
                ItemStack stack = stacks.get(i);
                if (stack.is(ModUtilityToolItems.fluid_identifier_multi.get())) identifiers++;
                for (int j = i + 1; j < stacks.size(); j++) {
                    helper.assertFalse(ItemStacks.sameItemAndData(stack, stacks.get(j)),
                            tab + " lists " + stack + " twice, at " + i + " and " + j);
                }
            }
        }
        helper.assertTrue(identifiers > 100, "one multi identifier per fluid, got " + identifiers);
        helper.succeed();
    }
}
