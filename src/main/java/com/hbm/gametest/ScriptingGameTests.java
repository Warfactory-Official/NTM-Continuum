package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.material.SmeltingBehavior;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.api.hazard.IHazardType;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.CrackingRecipes;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.items.special.ItemAutogen;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Holds the content {@code src/gametest/kubejs/startup_scripts/ntm_gametest.js} declares. Registered only with KubeJS present. */
public final class ScriptingGameTests {

    private ScriptingGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function CONTENT =
            TESTS.function("scripting_content", 20, () -> ScriptingGameTests::content);
    public static final GameTests.Function RECIPES =
            TESTS.function("scripting_recipes", 20, () -> ScriptingGameTests::recipes);
    public static final GameTests.Function FUELS =
            TESTS.function("scripting_fuels", 20, () -> ScriptingGameTests::fuels);
    public static final GameTests.Function HAZARDS =
            TESTS.function("scripting_hazards", 20, () -> ScriptingGameTests::hazards);

    public static void init() {}

    public static void content(GameTestHelper helper) {
        MaterialShapes gear = MaterialShapes.byName("GAMETEST_GEAR");
        NTMMaterial mat = Mats.matByName.get("gametestium");
        helper.assertTrue(gear != null && gear.quantity == 144 && "gt_gears".equals(gear.tagPlural), "scripted shape");
        helper.assertTrue(mat != null && mat.id.toString().equals("hbm:gametestium") && mat.solidColorLight == 0xE0E0FF
                && mat.smeltable == SmeltingBehavior.SMELTABLE && "Gametestium".equals(mat.getDisplayName()),
                "scripted material");
        assertAutogen(helper, gear, mat, "gt_gear_gametestium", "Gametestium Test Gear");
        assertAutogen(helper, MaterialShapes.CASTPLATE, mat, "plate_cast_gametestium", "Cast Gametestium Plate");
        assertAutogen(helper, gear, Mats.MAT_IRON, "gt_gear_iron", "Iron Test Gear");

        FluidType fluid = Fluids.byId("kubejs:gametest_fluid");
        helper.assertTrue(fluid != Fluids.NONE && Fluids.byIndex(fluid.index()) == fluid, "scripted fluid id");
        helper.assertTrue(fluid.temperature == 321 && fluid.getColor() == 0x40A040
                && fluid.getTexture().equals(Ids.of(Constants.MOD_ID, "textures/gui/fluids/custom_toxin.png")),
                "scripted fluid identity");
        FT_Corrosive corrosive = fluid.getTrait(FT_Corrosive.class);
        FluidCombustion.Combustible combustible = FluidCombustion.getCombustible(fluid);
        helper.assertTrue(corrosive != null && corrosive.getRating() == 33
                && FluidCombustion.getFlammableEnergy(fluid) == 12345
                && combustible != null && combustible.energyPerBucket() == 23456
                && combustible.grade() == FluidCombustion.FuelGrade.HIGH, "scripted fluid traits");
        helper.succeed();
    }

    private static void assertAutogen(GameTestHelper helper, MaterialShapes shape, NTMMaterial mat, String id, String name) {
        ItemAutogen item = ModAutogenItems.get(shape, mat);
        helper.assertTrue(item != null && BuiltInRegistries.ITEM.getKey(item).getPath().equals(id)
                && item.getName(new ItemStack(item)).getString().equals(name), "scripted autogen item " + id);
    }

    public static void recipes(GameTestHelper helper) {
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        GenericRecipe added = AssemblyMachineRecipes.INSTANCE.getRecipe("gametest.assembly");
        helper.assertTrue(added != null && added.duration == 7 && added.power == 11
                && added.inputItem.length == 1 && added.inputItem[0].stacksize == 2
                && added.inputItem[0].matchesRecipe(new ItemStack(Items.DEAD_BUSH, 2), false)
                && added.outputItem[0].is(Items.DIAMOND), "scripted assembly recipe");
        helper.assertTrue(AssemblyMachineRecipes.INSTANCE.getRecipe("ass.pilenu") == null
                && AssemblyMachineRecipes.INSTANCE.recipeOrderedList.stream().noneMatch(r -> r.name.equals("ass.pilenu")),
                "scripted assembly removal");

        ChemicalPlantRecipes.INSTANCE.registerRecipes();
        GenericRecipe chem = ChemicalPlantRecipes.INSTANCE.getRecipe("gametest.chem");
        helper.assertTrue(chem != null && chem.inputFluid[0].type == Fluids.byId("kubejs:gametest_fluid")
                && chem.inputFluid[0].fill == 100 && chem.outputFluid[0].type == Fluids.WATER, "scripted chemical plant recipe");

        ItemStack shredded = ShredderRecipes.getShredderResult(new ItemStack(Items.DEAD_BUSH));
        helper.assertTrue(shredded.is(Items.DIAMOND) && shredded.getCount() == 3, "scripted shredder recipe");

        ItemStack[] centrifuged = CentrifugeRecipes.getOutput(new ItemStack(Items.TUFF));
        helper.assertTrue(centrifuged != null && centrifuged[0].is(Items.EMERALD)
                && centrifuged[1].is(Items.GOLD_NUGGET) && centrifuged[1].getCount() == 2, "scripted centrifuge recipe");

        CrackingRecipes.CrackingRecipe cracked = CrackingRecipes.getCracking(Fluids.byId("kubejs:gametest_fluid"));
        helper.assertTrue(cracked != null && cracked.left().type == Fluids.WATER && cracked.left().fill == 50
                && cracked.right().type == Fluids.STEAM && cracked.right().fill == 10, "scripted cracking recipe");
        helper.succeed();
    }

    public static void fuels(GameTestHelper helper) {
        var id = Ids.of(Constants.MOD_ID, "rbmk_fuel_gametest");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id), "scripted RBMK rod registered");
        ItemRBMKRod rod = (ItemRBMKRod) BuiltInRegistries.ITEM.getValue(id);
        helper.assertTrue(rod.yield == 12345678D && rod.reactivity == 21D && rod.heat == 0.5D && rod.meltingPoint == 1999D
                && rod.function == ItemRBMKRod.EnumBurnFunc.LINEAR && rod.depFunc == ItemRBMKRod.EnumDepleteFunc.STATIC
                && rod.nType == ItemRBMKRod.NType.FAST && rod.rType == ItemRBMKRod.NType.SLOW && rod.colorTint == 0x123456
                && "Gametest Fuel".equals(rod.fullName), "scripted RBMK rod stats");
        helper.assertTrue(rod.pellet instanceof ItemRBMKPellet pellet && !pellet.isXenonEnabled()
                && BuiltInRegistries.ITEM.getKey(pellet).getPath().equals("rbmk_pellet_gametest"), "scripted RBMK pellet link");
        helper.assertTrue(HazardSystem.getHazardLevelFromStack(new ItemStack(rod), HazardRegistry.RADIATION) > 0D,
                "scripted RBMK rod radiation");
        for (String item : new String[] {"watz_pellet_gametest", "watz_pellet_depleted_gametest", "rod_zirnox_gametest",
                "rod_zirnox_depleted_gametest", "waste_gametest", "pwr_fuel_gametest", "pwr_fuel_hot_gametest",
                "pwr_fuel_depleted_gametest", "plate_fuel_gametest", "pellet_rtg_gametest"}) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Ids.of(Constants.MOD_ID, item)), "scripted fuel item " + item);
        }
        var pwr = com.hbm.items.ModPwrItems.fuelOf(BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, "pwr_fuel_gametest")));
        helper.assertTrue(pwr != null && pwr.heatEmission == 7.5D && pwr.index >= 15, "scripted PWR fuel");
        var zirnox = com.hbm.items.machine.ZirnoxFuel.byName("gametest");
        helper.assertTrue(zirnox != null && zirnox.maxLife == 1234
                && BuiltInRegistries.ITEM.getKey(com.hbm.items.ModZirnoxItems.depleted(zirnox)).getPath().equals("rod_zirnox_depleted_gametest"),
                "scripted ZIRNOX fuel");
        helper.succeed();
    }

    public static void hazards(GameTestHelper helper) {
        helper.assertTrue(HazardSystem.getHazardLevelFromStack(new ItemStack(Items.DEAD_BUSH), HazardRegistry.RADIATION) == 7D,
                "scripted item hazard");
        IHazardType custom = HazardRegistry.getType(Ids.parse("kubejs:gametest"));
        helper.assertTrue(custom != null
                && HazardSystem.getHazardLevelFromStack(new ItemStack(Items.SCULK_VEIN), custom) == 2D,
                "scripted hazard type");
        helper.succeed();
    }
}
