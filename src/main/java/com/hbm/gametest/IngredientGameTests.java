package com.hbm.gametest;

import com.hbm.inventory.RecipesCommon;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.PyroOvenRecipes;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ItemEnums.EnumPartType;
import com.hbm.items.ModBatteryItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBatteryPack;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.machine.ItemPlateFuel;
import dev.golem.core.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Registry, state, tag, and deferred-recipe contracts for the ingredient items. */
public final class IngredientGameTests {

    private IngredientGameTests() {}

    public static void registry(GameTestHelper helper) {
        for (EnumPartType type : EnumPartType.VALUES) {
            assertId(helper, ModIngredientItems.part(type), "part_generic_" + type.name().toLowerCase());
        }
        for (EnumAshType type : EnumAshType.VALUES) {
            assertId(helper, ModIngredientItems.ash(type), "powder_ash_" + type.name().toLowerCase());
        }
        for (ModIngredientItems.PlateFuelType type : ModIngredientItems.PlateFuelType.values()) {
            ItemPlateFuel fuel = ModIngredientItems.plateFuel(type);
            assertId(helper, fuel, type.id);
            helper.assertTrue(new ItemStack(fuel).getMaxStackSize() == 1, type.id + " must be unstackable");
            helper.assertTrue(fuel.lifeTime == type.lifeTime && fuel.function == type.function
                            && fuel.reactivity == type.reactivity,
                    type.id + " has drifted from the old plate-fuel constants");
        }
        helper.succeed();
    }

    public static void plateFuelBehavior(GameTestHelper helper) {
        ItemPlateFuel u233 = ModIngredientItems.plateFuel(ModIngredientItems.PlateFuelType.U233);
        ItemStack stack = new ItemStack(u233);
        int output = u233.react(helper.getLevel(), stack, 400);
        helper.assertTrue(output == 100, "U233 sqrt reaction at flux 400 must produce 100, got " + output);
        helper.assertTrue(ItemPlateFuel.getLifeTime(stack) == 400,
                "Non-passive plate fuel must accumulate input flux as life");
        helper.assertTrue(u233.getFunctionDesc().equals("f(x) = sqrt(x) * 50 / 10"),
                "U233 tooltip formula changed");

        ItemPlateFuel.setLifeTime(stack, 0);
        double freshRadiation = HazardSystem.getHazardLevelFromStack(stack, HazardRegistry.RADIATION);
        ItemPlateFuel.setLifeTime(stack, u233.lifeTime);
        double depletedRadiation = HazardSystem.getHazardLevelFromStack(stack, HazardRegistry.RADIATION);
        helper.assertTrue(Math.abs(freshRadiation - 5D) < 0.001D,
                "Fresh U233 plate radiation must be 5, got " + freshRadiation);
        helper.assertTrue(Math.abs(depletedRadiation - 195D) < 0.001D,
                "Depleted U233 plate radiation must be 195, got " + depletedRadiation);

        ItemPlateFuel ra226be = ModIngredientItems.plateFuel(ModIngredientItems.PlateFuelType.RA226BE);
        ItemStack passive = new ItemStack(ra226be);
        helper.assertTrue(ra226be.react(helper.getLevel(), passive, 400) == 30,
                "Ra226Be must remain a passive 30-flux source");
        helper.assertTrue(ItemPlateFuel.getLifeTime(passive) == 30,
                "Passive plate fuel must advance by reactivity, not input flux");

        ItemPlateFuel sa326 = ModIngredientItems.plateFuel(ModIngredientItems.PlateFuelType.SA326);
        helper.assertTrue(HazardSystem.getHazardLevelFromStack(new ItemStack(sa326), HazardRegistry.BLINDING) == 20D,
                "SA326 plate fuel must retain its blinding hazard");
        helper.succeed();
    }

    public static void tagsAndRecipes(GameTestHelper helper) {
        assertTag(helper, ModItems.sulfur.get(), "dusts/sulfur");
        assertTag(helper, ModItems.fluorite.get(), "dusts/fluorite");
        assertTag(helper, ModItems.niter.get(), "dusts/niter");
        assertTag(helper, ModItems.lithium.get(), "ingots/lithium");
        assertTag(helper, ModIngredientItems.ash(EnumAshType.SOOT), "dusts/ash");

        SILEXRecipes.register();
        SILEXRecipes.SILEXRecipe fullerene = SILEXRecipes.getOutput(ItemFluidIcon.make(Fluids.FULLERENE));
        helper.assertTrue(fullerene != null && fullerene.outputs.size() == 1
                        && fullerene.outputs.get(0).stack().is(ModIngredientItems.ash(EnumAshType.FULLERENE))
                        && fullerene.outputs.get(0).weight() == 1,
                "FULLERENE SILEX must produce one Fullerene ash output");

        boolean soot = false;
        for (PyroOvenRecipes.PyroOvenRecipe recipe : PyroOvenRecipes.getAll()) {
            if (recipe.outputItem != null && recipe.outputItem.is(ModIngredientItems.ash(EnumAshType.SOOT))) {
                soot = recipe.duration == 40 && recipe.inputItem instanceof RecipesCommon.TagStack tag
                        && tag.tag.equals(PyroOvenRecipes.TARS);
            }
        }
        helper.assertTrue(soot, "ANY_TAR pyrolysis must produce soot through the c:tars tag");

        ItemBatteryPack lithium = ModBatteryItems.PACKS.get(ItemBatteryPack.EnumBatteryPack.BATTERY_LITHIUM).get();
        ItemStack battery = new ItemStack(lithium);
        helper.assertTrue(lithium.getMaxCharge(battery) == 180_000_000L
                        && lithium.getChargeRate(battery) == 100_000L
                        && lithium.getDischargeRate(battery) == 10_000L,
                "Lithium battery capacity and rates must remain 1.12.2-compatible");
        helper.succeed();
    }

    /** 1.7 has no plate-fuel recipe at all (the anvil ones were 1.12.2 additions, removed in fix phase 3). */
    public static void plateFuelAnvilRecipes(GameTestHelper helper) {
        var recipes = AnvilRecipes.getConstruction();
        for (ModIngredientItems.PlateFuelType type : ModIngredientItems.PlateFuelType.values()) {
            Item item = ModIngredientItems.plateFuel(type);
            boolean found = recipes.stream().anyMatch(recipe -> recipe.output.stream()
                    .anyMatch(output -> output.stack.is(item)));
            helper.assertTrue(!found, "1.7 has no anvil recipe for " + type.id);
        }
        helper.succeed();
    }

    private static void assertId(GameTestHelper helper, Item item, String path) {
        Identifier expected = Ids.of("hbm", path);
        helper.assertTrue(expected.equals(BuiltInRegistries.ITEM.getKey(item)),
                "Expected item id " + expected + ", got "
                        + BuiltInRegistries.ITEM.getKey(item));
    }

    private static void assertTag(GameTestHelper helper, Item item, String path) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, Ids.of("c", path));
        helper.assertTrue(new ItemStack(item).is(tag), "Item is missing c:" + path + " membership");
    }
}
