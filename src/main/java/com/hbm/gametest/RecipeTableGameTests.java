package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.FractionRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Machine recipes as datapack tables: the chemical plant table loads from {@code data/hbm/recipe/chemical_plant}
 * exactly as converted from the former Java table, and every row survives the file format and the client sync format.
 */
public final class RecipeTableGameTests {

    private RecipeTableGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function CHEMICAL_PLANT =
            TESTS.function("recipe_table_chemical_plant", 20, () -> RecipeTableGameTests::chemicalPlant);

    public static final GameTests.Function FRACTION =
            TESTS.function("recipe_table_fraction", 20, () -> RecipeTableGameTests::fraction);

    public static void init() {
        ScriptFieldsGameTests.init();
    }

    /** The fraction tower table from data/hbm/recipe/fraction equals 1.7's 19 rows (input: left amount, right amount). */
    public static void fraction(GameTestHelper helper) {
        String expected = "heavyoil:bitumen30,smear70 heavyoil_vacuum:smear40,heatingoil_vacuum60 smear:heatingoil60,lubricant40 "
                + "naphtha:heatingoil40,diesel60 naphtha_ds:xylene60,diesel_reform40 naphtha_crack:heatingoil30,diesel_crack70 "
                + "lightoil:diesel40,kerosene60 lightoil_ds:diesel_reform60,kerosene_reform40 lightoil_crack:kerosene70,petroleum30 "
                + "coaloil:coalgas30,oil70 coalcreosote:coaloil10,bitumen90 reformate:aromatics40,xylene60 "
                + "lightoil_vacuum:kerosene70,reformgas30 egg:cholesterol50,radiosolvent50 oil_coker:crackoil30,heatingoil70 "
                + "naphtha_coker:naphtha_crack75,lightoil_crack25 gas_coker:aromatics25,carbondioxide75 "
                + "chlorocalcite_mix:chlorocalcite_cleaned50,colloid50 bauxite_solution:redmud50,sodium_aluminate50";
        Set<String> want = new TreeSet<>(Arrays.asList(expected.split(" ")));
        Set<String> have = new TreeSet<>();
        for (var e : FractionRecipes.getAll().entrySet()) {
            have.add(e.getKey().getKey() + ":" + e.getValue().left().type.getKey() + e.getValue().left().fill + ","
                    + e.getValue().right().type.getKey() + e.getValue().right().fill);
        }
        helper.assertTrue(want.equals(have), "fraction table differs from 1.7: " + have);
        helper.succeed();
    }

    public static void chemicalPlant(GameTestHelper helper) {
        ChemicalPlantRecipes plant = ChemicalPlantRecipes.INSTANCE;
        plant.registerRecipes();
        // 77 rows from the Java table (1.7 curated, fuel additives, the generated 1.12.2 bulk)
        helper.assertTrue(plant.recipeOrderedList.size() == 77,
                "chemical plant table holds " + plant.recipeOrderedList.size() + " rows, expected 77");
        // 1.7's first row (the table is in 1.7's registration order)
        helper.assertTrue(plant.recipeOrderedList.get(0).getInternalName().equals("chem.hydrogen"),
                "chemical plant display order changed: first row " + plant.recipeOrderedList.get(0).getInternalName());
        String oxygen = GenericRecipeCodec.describe(plant.getRecipe("chem.oxygen"));
        helper.assertTrue(oxygen.equals("chem.oxygen d=20 p=400 in= fin=airx8000@0, out= fout=oxygenx500@0, icon=hbm:gas_fullx1{fluid=oxygen;amount=1000} "
                        + "named=true wrap=null pools=- group=null"),
                "chem.oxygen changed: " + oxygen);
        List<String> broken = new ArrayList<>();
        for (GenericRecipe recipe : plant.recipeOrderedList) {
            GenericRecipe copy = plant.table.fromJson(plant.table.toJson(recipe, helper.getLevel().registryAccess()),
                    helper.getLevel().registryAccess());
            copy.bindStacks();
            if (!GenericRecipeCodec.describe(copy).equals(GenericRecipeCodec.describe(recipe))) broken.add(recipe.getInternalName());
        }
        helper.assertTrue(broken.isEmpty(), "rows that do not survive their JSON: " + broken);
        helper.succeed();
    }
}
