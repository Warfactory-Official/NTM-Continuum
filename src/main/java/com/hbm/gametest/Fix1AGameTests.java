package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm.inventory.recipes.HeatRecipes;
import com.hbm.items.ModItems;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Recipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Values computed from 1.7.
 * <ul>
 *   <li>Billets: 1.7 {@code MineralRecipes.addBillet(billet, ingot, nugget)} gives one billet back as six nuggets,
 *       three billets as two ingots and two ingots as three billets; nothing turns one billet into six ingots.</li>
 *   <li>Mercury: 1.7 {@code ingot_mercury} is registered as {@code nugget_mercury}, so the electrolyser outputs are
 *       1 / 2 / 8 of {@code hbm:nugget_mercury}, not 9 / 18 / 72.</li>
 *   <li>Fluid ids are the 1.7 positions (CHLORINE 89, DHC 153, FLUE 155, 157 fluids), AIRBLAST is 1,200 degrees,
 *       FLUE burns for 25,000, and steam has no FT_Heatable.</li>
 *   <li>The machines and rails that dropped nothing drop themselves.</li>
 * </ul>
 */
public final class Fix1AGameTests {

    private Fix1AGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function BILLETS =
            TESTS.function("fix1a_billet_recipes", 20, () -> Fix1AGameTests::billets);
    public static final GameTests.Function MERCURY =
            TESTS.function("fix1a_mercury_units", 20, () -> Fix1AGameTests::mercury);
    public static final GameTests.Function FLUIDS =
            TESTS.function("fix1a_fluid_ids_traits", 20, () -> Fix1AGameTests::fluids);
    public static final GameTests.Function DROPS =
            TESTS.function("fix1a_self_drops", 20, () -> Fix1AGameTests::drops);

    public static void init() {}

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Ids.parse(id));
    }

    private static ItemStack craft(GameTestHelper helper, int w, int h, ItemStack... stacks) {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < w * h; i++) grid.add(i < stacks.length ? stacks[i] : ItemStack.EMPTY);
        return Recipes.craftingResult(helper.getLevel(), w, h, grid).orElse(ItemStack.EMPTY);
    }

    private static void expect(GameTestHelper helper, ItemStack result, String id, int count, String what) {
        helper.assertTrue(result.is(item(id)) && result.getCount() == count,
                what + ": expected " + count + "x " + id + ", got " + result);
    }

    public static void billets(GameTestHelper helper) {
        String[][] samples = {
                {"hbm:billet_cobalt", "hbm:ingot_cobalt", "hbm:nugget_cobalt"},
                {"hbm:billet_uranium", "hbm:ingot_uranium", "hbm:nugget_uranium"},
                {"hbm:billet_pu239", "hbm:ingot_pu239", "hbm:nugget_pu239"},
                {"hbm:billet_schrabidium_fuel", "hbm:ingot_schrabidium_fuel", "hbm:nugget_schrabidium_fuel"},
                {"hbm:billet_nuclear_waste", "hbm:nuclear_waste", "hbm:nuclear_waste_tiny"},
        };
        for (String[] s : samples) {
            ItemStack billet = new ItemStack(item(s[0]));
            ItemStack ingot = new ItemStack(item(s[1]));
            ItemStack nugget = new ItemStack(item(s[2]));
            expect(helper, craft(helper, 3, 3, billet.copy()), s[2], 6, "1 billet");
            expect(helper, craft(helper, 3, 3, billet.copy(), billet.copy(), billet.copy()), s[1], 2, "3 billets");
            expect(helper, craft(helper, 3, 3, ingot.copy(), ingot.copy()), s[0], 3, "2 ingots");
            expect(helper, craft(helper, 3, 3, nugget.copy(), nugget.copy(), nugget.copy(), nugget.copy(),
                    nugget.copy(), nugget.copy()), s[0], 1, "6 nuggets");
            // The old port's 9x loop: six ingots made one billet. 1.7 has no such recipe.
            ItemStack six = craft(helper, 3, 3, ingot.copy(), ingot.copy(), ingot.copy(), ingot.copy(),
                    ingot.copy(), ingot.copy());
            helper.assertTrue(!six.is(item(s[0])), "6 ingots must not craft " + s[0] + ", got " + six);
        }
        helper.succeed();
    }

    public static void mercury(GameTestHelper helper) {
        ElectrolyserFluidRecipes.register();
        ElectrolyserMetalRecipes.register();
        Item hg = ModItems.nugget_mercury.get();
        ItemStack[] vitriol = ElectrolyserFluidRecipes.getRecipe(Fluids.VITRIOL).byproduct;
        helper.assertTrue(vitriol[1].is(hg) && vitriol[1].getCount() == 1, "vitriol mercury x1, got " + vitriol[1]);
        ItemStack[] gold = ElectrolyserMetalRecipes.getRecipe(new ItemStack(ModItems.crystal_gold.get())).byproduct;
        helper.assertTrue(gold[1].is(hg) && gold[1].getCount() == 2, "crystal_gold mercury x2, got " + gold[1]);
        ItemStack[] star = ElectrolyserMetalRecipes.getRecipe(new ItemStack(ModItems.crystal_starmetal.get())).byproduct;
        helper.assertTrue(star[1].is(hg) && star[1].getCount() == 8, "crystal_starmetal mercury x8, got " + star[1]);
        ItemStack[] cent = CentrifugeRecipes.getOutput(new ItemStack(ModItems.crystal_starmetal.get()));
        helper.assertTrue(cent != null && cent[3].is(hg) && cent[3].getCount() == 5,
                "centrifuged crystal_starmetal gives 5 mercury (1.7 CentrifugeRecipes:329)");
        expect(helper, craft(helper, 3, 3, new ItemStack(item("hbm:pellet_rtg_depleted_mercury"))),
                "hbm:nugget_mercury", 2, "depleted mercury RTG pellet");
        helper.succeed();
    }

    public static void fluids(GameTestHelper helper) {
        Fluids.init();
        helper.assertTrue(Fluids.getAll().length == 157, "1.7 has 157 fluids, have " + Fluids.getAll().length);
        helper.assertTrue(Fluids.legacyId(Fluids.RADIOSOLVENT) == 88 && Fluids.legacyId(Fluids.CHLORINE) == 89
                        && Fluids.legacyId(Fluids.SMOKE) == 120 && Fluids.legacyId(Fluids.DHC) == 153 && Fluids.legacyId(Fluids.FLUE) == 155,
                "1.7 ids: RADIOSOLVENT 88, CHLORINE 89, SMOKE 120, DHC 153, FLUE 155; have "
                        + Fluids.legacyId(Fluids.CHLORINE) + "/" + Fluids.legacyId(Fluids.SMOKE) + "/" + Fluids.legacyId(Fluids.FLUE));
        helper.assertTrue(Fluids.fromName("HYDRAZINE") == Fluids.NONE, "HYDRAZINE is not a 1.7 fluid");
        helper.assertTrue(Fluids.AIRBLAST.temperature == 1_200, "AIRBLAST 1.7 setTemp(1_200)");
        helper.assertTrue(FluidCombustion.getFlammableEnergy(Fluids.FLUE) == 25_000L, "FLUE FT_Flammable(25_000)");
        helper.assertTrue(Fluids.STEAM.getTrait(FT_Heatable.class) == null
                        && Fluids.HOTSTEAM.getTrait(FT_Heatable.class) == null
                        && Fluids.SUPERHOTSTEAM.getTrait(FT_Heatable.class) == null
                        && Fluids.WATER.getTrait(FT_Heatable.class) != null,
                "1.7: water is heatable, the steam family is not");
        helper.assertTrue(!HeatRecipes.hasBoilerRecipe(Fluids.STEAM) && HeatRecipes.hasBoilerRecipe(Fluids.WATER),
                "the boiler heats water, not steam");
        helper.succeed();
    }

    public static void drops(GameTestHelper helper) {
        String[] ids = {"hbm:machine_turbine_gas", "hbm:machine_electrolyser", "hbm:furnace_iron",
                "hbm:machine_gascent", "hbm:machine_furnace_brick_off", "hbm:geiger", "hbm:machine_armor_table",
                "hbm:rbmk_terminal", "hbm:rail_large_straight", "hbm:rail_narrow_curve"};
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        for (String id : ids) {
            Block block = BuiltInRegistries.BLOCK.getOptional(Ids.parse(id)).orElseThrow();
            List<ItemStack> drops = Block.getDrops(block.defaultBlockState(), level, pos, null, null, pick);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(block.asItem()) && drops.get(0).getCount() == 1,
                    id + " must drop itself once (1.7 default getItemDropped), got " + drops);
        }
        helper.succeed();
    }
}
