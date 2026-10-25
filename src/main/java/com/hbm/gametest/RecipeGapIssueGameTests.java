package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.handler.jei.CombinationRecipeHandler;
import com.hbm.handler.jei.HbmJeiRecipeTypes;
import com.hbm.handler.jei.HbmRecipeViewer;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CombinationRecipes;
import com.hbm.inventory.recipes.CombinationRecipes.CombinationRecipe;
import com.hbm.items.ItemEnums.EnumCokeType;
import com.hbm.items.ModOilItems;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.viewer.ViewerCategory;
import dev.golem.core.world.Ingredients;
import dev.golem.core.world.Recipes;
import dev.golem.core.world.Recipes.CraftingMatch;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Recipe;

/**
 * Crafting recipes 1.7 has that recipe viewers did not list: latex in 1.7's {@code ANY_RUBBER}, the weapon mods and
 * the guns and tanks crafted through component recipes (EMI skips every vanilla {@code CustomRecipe}), the combination
 * furnace's viewer tab, and raw aluminium in fireclay.
 */
public final class RecipeGapIssueGameTests {

    private RecipeGapIssueGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function RUBBER =
            TESTS.function("recipe_gap_latex_is_any_rubber", 20, () -> RecipeGapIssueGameTests::latexIsAnyRubber);
    public static final GameTests.Function LISTED =
            TESTS.function("recipe_gap_component_recipes_listed", 20, () -> RecipeGapIssueGameTests::componentListed);
    public static final GameTests.Function COMBINATION =
            TESTS.function("recipe_gap_combination_tab", 20, () -> RecipeGapIssueGameTests::combinationTab);
    public static final GameTests.Function FIRECLAY =
            TESTS.function("recipe_gap_raw_aluminium_fireclay", 20, () -> RecipeGapIssueGameTests::rawFireclay);

    /** The weapon mods, guns and tank the reports named; all component recipes in the port. */
    private static final String[] COMPONENT_RECIPES = {
            "weapon_mod_generic_iron_damage", "weapon_mod_generic_iron_dura", "weapon_mod_generic_steel_dura",
            "weapon_mod_generic_tcalloy_dura", "weapon_mod_special_silencer", "weapon_mod_special_scope",
            "weapon_mod_special_saw", "weapon_mod_special_choke", "weapon_mod_special_bayonet",
            "weapon_mod_special_speedloader", "weapon_mod_special_greasegun", "weapon_mod_special_stack_mag",
            "gun_amat", "gun_stg77", "gun_lasrifle", "inf_water"};

    public static void init() {}

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Ids.parse(id));
    }

    private static ItemStack tagged(GameTestHelper helper, String tag) {
        List<Item> items = Ingredients.tagItems(tag);
        helper.assertTrue(!items.isEmpty(), "#" + tag + " has members");
        return new ItemStack(items.get(0));
    }

    private static List<ItemStack> grid(ItemStack... stacks) {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(i < stacks.length ? stacks[i] : ItemStack.EMPTY);
        return grid;
    }

    private static CraftingMatch match(GameTestHelper helper, List<ItemStack> grid, String id) {
        CraftingMatch found = Recipes.craftingMatches(helper.getLevel(), 3, 3, grid).stream()
                .filter(m -> m.id().equals(id)).findFirst().orElse(null);
        helper.assertTrue(found != null, id + " matches its 1.7 grid");
        return found;
    }

    private static void latexIsAnyRubber(GameTestHelper helper) {
        Item latex = item("hbm:ingot_biorubber");
        helper.assertTrue(Ingredients.tagItems("c:ingots/any_rubber").contains(latex),
                "1.7 AnyRubber = Latex + Rubber: ingot_biorubber is in #c:ingots/any_rubber");
        ItemStack plate = tagged(helper, "c:plates/iron");
        // 1.7 iv_empty x4: S / I / S, S = ANY_RUBBER.ingot()
        List<ItemStack> iv = grid(new ItemStack(latex), ItemStack.EMPTY, ItemStack.EMPTY, plate, ItemStack.EMPTY,
                ItemStack.EMPTY, new ItemStack(latex));
        match(helper, iv, "hbm:iv_empty");
        ItemStack bags = Recipes.craftingResult(helper.getLevel(), 3, 3, iv).orElse(ItemStack.EMPTY);
        helper.assertTrue(bags.is(item("hbm:iv_empty")) && bags.getCount() == 4, "latex crafts 4 IV bags, got " + bags);
        helper.succeed();
    }

    private static void componentListed(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        for (String path : COMPONENT_RECIPES) {
            if (!Recipes.hasRecipe(helper.getLevel(), Constants.MOD_ID, path)) missing.add(path);
        }
        helper.assertTrue(missing.isEmpty(), "every reported recipe is loaded; missing " + missing);
        helper.assertTrue(Recipes.hasRecipe(helper.getLevel(), Constants.MOD_ID, "gun_minigun"), "gun_minigun is loaded");

        // 1.7 silencer: P / B / P, P = ANY_PLASTIC.ingot(), B = STEEL.lightBarrel()
        ItemStack plastic = tagged(helper, "c:ingots/any_plastic");
        List<ItemStack> silencer = grid(plastic, ItemStack.EMPTY, ItemStack.EMPTY, tagged(helper, "c:barrels_light/steel"),
                ItemStack.EMPTY, ItemStack.EMPTY, plastic.copy());
        // 1.7 inf_water: 222 / 131 / 222, 1 = 1,000 mB water, 2 = AL.plate(), 3 = diamond
        ItemStack al = tagged(helper, "c:plates/aluminium");
        List<ItemStack> water = grid(al, al.copy(), al.copy(), new ItemStack(Items.WATER_BUCKET),
                new ItemStack(Items.DIAMOND), new ItemStack(Items.WATER_BUCKET), al.copy(), al.copy(), al.copy());
        for (CraftingMatch found : List.of(match(helper, silencer, "hbm:weapon_mod_special_silencer"),
                match(helper, water, "hbm:inf_water"))) {
            // EMI's crafting page takes any crafting recipe that is neither special nor a vanilla CustomRecipe
            helper.assertTrue(!(found.recipe() instanceof CustomRecipe), found.id() + " is not a vanilla CustomRecipe");
            helper.assertTrue(!((Recipe<?>) found.recipe()).isSpecial(), found.id() + " is not special");
        }
        helper.succeed();
    }

    private static void combinationTab(GameTestHelper helper) {
        List<ViewerCategory<?>> categories = new ArrayList<>();
        HbmRecipeViewer.INSTANCE.categories(added -> categories.addAll(List.of(added)));
        helper.assertTrue(categories.stream().anyMatch(c -> c.type().equals(HbmJeiRecipeTypes.COMBINATION)),
                "the combination furnace has a viewer category");
        List<CombinationRecipe> rows = CombinationRecipeHandler.buildRecipes();
        helper.assertTrue(!rows.isEmpty() && rows.size() == CombinationRecipes.TABLE.rows().size(),
                "one viewer row per hbm:combination row: " + rows.size());
        // 1.7: COAL.gem() -> coke (coal) + 100 mB coal creosote
        CombinationRecipe coal = CombinationRecipes.getOutput(new ItemStack(Items.COAL));
        helper.assertTrue(coal != null && coal.itemOutput().is(ModOilItems.coke(EnumCokeType.COAL))
                        && coal.fluidOutput().type == Fluids.COALCREOSOTE && coal.fluidOutput().fill == 100,
                "coal gives coal coke and 100 mB coal creosote");
        Constants.LOG.info("[viewer] combination rows: {}", rows.size());
        helper.succeed();
    }

    private static void rawFireclay(GameTestHelper helper) {
        ItemStack clay = new ItemStack(Items.CLAY_BALL);
        List<ItemStack> fireclay = grid(clay, clay.copy(), clay.copy(), new ItemStack(item("hbm:raw_aluminium")));
        match(helper, fireclay, "hbm:ball_fireclay_raw");
        ItemStack out = Recipes.craftingResult(helper.getLevel(), 3, 3, fireclay).orElse(ItemStack.EMPTY);
        helper.assertTrue(out.is(item("hbm:ball_fireclay")) && out.getCount() == 4, "raw aluminium makes 4 fireclay, got " + out);
        helper.succeed();
    }
}
