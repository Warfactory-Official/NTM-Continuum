package com.hbm.gametest;

import com.google.gson.JsonElement;
import com.hbm.Constants;
import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.inventory.material.MaterialDistribution;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.CyclotronRecipes;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.inventory.recipes.loader.DataRecipeTable;
import com.hbm.inventory.recipes.loader.MaterialCodecs;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.RecipeText;
import com.hbm.items.ModItems;
import com.hbm.items.ModMachineItems;
import com.hbm.items.machine.ItemScraps;
import dev.golem.core.gametest.GameTests;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The item-processing machine tables as datapack tables (M2 R-C1): crucible alloying, material distribution, arc
 * furnace, centrifuge, crystallizer, cyclotron, electrolyser (metal), rotary furnace, SILEX and the shredder's explicit
 * rows load from {@code data/hbm/recipe/<table>}.
 */
public final class ItemProcessingTableGameTests {

    private ItemProcessingTableGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function TABLES =
            TESTS.function("recipe_table_item_processing", 200, () -> ItemProcessingTableGameTests::tables);

    public static void init() {}

    static String describeCrucible(CrucibleRecipes.CrucibleRecipe recipe) {
        return recipe.getInternalName() + " f=" + recipe.frequency + " icon=" + RecipeText.describe(recipe.getIcon())
                + " in=" + MaterialCodecs.describe(recipe.input) + " out=" + MaterialCodecs.describe(recipe.output);
    }

    /** SHA-256 over every loaded row's JSON (keys sorted, see {@link CanonicalJson}: 1.20.1's DFU orders a record's fields differently), in recipe-id order. */
    static <R> String hash(DataRecipeTable<R> table, HolderLookup.Provider registries) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (R row : table.rows()) digest.update((CanonicalJson.text(table.table.toJson(row, registries)) + "\n").getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : digest.digest()) out.append(String.format("%02x", b));
            return table.rows().size() + ":" + out;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // Permanent

    /**
     * Each table holds exactly the rows of the Java table (row count and a SHA-256 over every
     * row's JSON), every row survives its JSON, and spot rows carry 1.7's values.
     */
    public static void tables(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        List<String> failures = new ArrayList<>();
        expect(failures, CrucibleRecipes.TABLE, registries, EXPECTED_CRUCIBLE);
        expect(failures, MaterialDistribution.TABLE, registries, EXPECTED_MATERIAL_DISTRIBUTION);
        expect(failures, ArcFurnaceRecipes.TABLE, registries, EXPECTED_ARC_FURNACE);
        expect(failures, CentrifugeRecipes.TABLE, registries, EXPECTED_CENTRIFUGE);
        expect(failures, CrystallizerRecipes.TABLE, registries, EXPECTED_CRYSTALLIZER);
        expect(failures, CyclotronRecipes.TABLE, registries, EXPECTED_CYCLOTRON);
        expect(failures, ElectrolyserMetalRecipes.TABLE, registries, EXPECTED_ELECTROLYSER_METAL);
        expect(failures, RotaryFurnaceRecipes.TABLE, registries, EXPECTED_ROTARY_FURNACE);
        expect(failures, SILEXRecipes.TABLE, registries, EXPECTED_SILEX);
        expect(failures, ShredderRecipes.TABLE, registries, EXPECTED_SHREDDER);

        // spot rows, 1.7 values
        CrucibleRecipes.register();
        var steel = (CrucibleRecipes.CrucibleRecipe) CrucibleRecipes.INSTANCE.getRecipe("crucible.steel");
        check(failures, "crucible.steel", steel == null ? "-" : describeCrucible(steel),
                "crucible.steel f=20 icon=hbm:ingot_steel x1 in=[iron:16,carbon:24,flux:8,] out=[steel:16,]");
        check(failures, "crucible order", CrucibleRecipes.INSTANCE.recipeOrderedList.stream().map(GenericRecipe::getInternalName).toList().toString(),
                "[crucible.steel, crucible.hematite, crucible.malachite, crucible.redcopper, crucible.hss, crucible.ferro, "
                        + "crucible.tcalloy, crucible.cdalloy, crucible.bbronze, crucible.abronze, crucible.cmb, crucible.magtung, crucible.bscco]");
        check(failures, "material c:ores/iron", MaterialCodecs.describe(MaterialDistribution.tags().get("c:ores/iron")),
                "[iron:144,titanium:24,stone:162,]");
        check(failures, "material obsidian", MaterialCodecs.describe(MaterialDistribution.items().get(Items.OBSIDIAN)),
                "[obsidian:648,]");
        var flint = ArcFurnaceRecipes.getOutput(new ItemStack(Items.FLINT), false);
        check(failures, "arc flint", flint == null ? "-" : ArcFurnaceRecipes.describe(flint),
                "minecraft:flint x1 -> hbm:nugget_silicon x4 + [silicon:36,]");
        // 1.7's first row, the rare chunk (the table is in 1.7's line order)
        var first = CentrifugeRecipes.recipes.isEmpty() ? null : CentrifugeRecipes.recipes.get(0);
        check(failures, "centrifuge first", first == null ? "-" : CentrifugeRecipes.describe(first),
                "hbm:chunk_ore_rare x1 -> [hbm:powder_cobalt_tiny x2,hbm:powder_boron_tiny x2,hbm:powder_niobium_tiny x2,hbm:nugget_zirconium x3,]");
        CrystallizerRecipes.register();
        var malachite = CrystallizerRecipes.recipes.stream().filter(r -> r.output.getItem() == ModMachineItems.scraps.get()).findFirst();
        check(failures, "crystallizer malachite scraps", malachite.map(r -> RecipeText.describe(r.input)
                + " " + MaterialCodecs.describe(ItemScraps.getMats(r.output)) + " t=" + r.duration + " "
                + r.fluid.getKey() + "x" + r.acidAmount).orElse("-"), "#c:ingots/malachite x1 copper:72 t=300 sulfuric_acidx250");
        long compat = CrystallizerRecipes.TABLE.rows().stream().filter(r -> r.requires().isPresent()).count();
        check(failures, "crystallizer compat rows", String.valueOf(compat), "3");
        var cyc = CyclotronRecipes.recipes.isEmpty() ? null : CyclotronRecipes.recipes.get(0);
        check(failures, "cyclotron first", cyc == null ? "-" : CyclotronRecipes.describe(cyc),
                "hbm:part_lithium x1 + #c:dusts/lithium x1 -> hbm:powder_beryllium x1 am=50");
        var iron = ElectrolyserMetalRecipes.getRecipe(new ItemStack(ModItems.crystal_iron.get()));
        check(failures, "electrolyser crystal_iron", iron == null ? "-" : ElectrolyserMetalRecipes.describe(iron),
                "hbm:crystal_iron x1 -> iron:432 + titanium:144 + [hbm:powder_lithium_tiny x3,] t=600");
        RotaryFurnaceRecipes.register();
        var rot = RotaryFurnaceRecipes.recipes.isEmpty() ? null : RotaryFurnaceRecipes.recipes.get(0);
        check(failures, "rotary first", rot == null ? "-" : RotaryFurnaceRecipes.describe(rot),
                "in=#c:ingots/iron x1,#c:gems/coal x1, fluid=- -> steel:72 t=100 steam=100");
        var uranium = SILEXRecipes.getOutput(new ItemStack(ModItems.ingot_uranium.get()));
        check(failures, "silex uranium", uranium == null ? "-" : SILEXRecipes.describe(uranium),
                "#c:ingots/uranium x1 900/100 VISIBLE -> hbm:nugget_u235 x1@1 hbm:nugget_u238 x1@11"); // 1.7 U.ingot()
        check(failures, "shredder scrap", RecipeText.describe(
                ShredderRecipes.getShredderResult(new ItemStack(ModItems.scrap.get()))), "hbm:dust x1");

        helper.assertTrue(failures.isEmpty(), "item-processing tables differ from the conversion: " + failures);
        helper.succeed();
    }

    private static void check(List<String> failures, String name, String have, String want) {
        if (!want.equals(have)) failures.add(name + ": " + have);
    }

    /** Row count and hash equal {@code expected} ({@code "<count>:<sha256>"}) and every row survives its JSON. */
    private static <R> void expect(List<String> failures, DataRecipeTable<R> table, HolderLookup.Provider registries, String expected) {
        String have = hash(table, registries);
        if (!have.equals(expected)) failures.add(table.table.id() + " rows " + have);
        for (R row : table.rows()) {
            JsonElement json = table.table.toJson(row, registries);
            JsonElement again = table.table.toJson(table.table.fromJson(json, registries), registries);
            if (!json.equals(again)) failures.add(table.table.id() + " row does not survive its JSON: " + json);
        }
    }

    // Pins are SHA-256 hashes of each table's key-sorted JSON (CanonicalJson), in 1.7's line order, with ntm-next's
    // raw ore rows (material distribution, centrifuge, crystallizer, shredder) after their ore's row.
    private static final String EXPECTED_CRUCIBLE =
            "13:16a916835a5adb64587bf5a8f085f4704b659f6a3541e82ca15865530fdef281";
    private static final String EXPECTED_MATERIAL_DISTRIBUTION =
            "54:9ccdf0a9ebc7982f724a54afe2b1d83cab32895bd39aa5a4334ce141aab66700";
    private static final String EXPECTED_ARC_FURNACE =
            "79:27f6f8737eeccb2f41680fb8fce5ab0530ff1bd96f931769ad6a1739b78c985e";
    private static final String EXPECTED_CENTRIFUGE =
            "179:05edb92a12e60a6837b8945e425869fb08c470ba0a4f70d15bd6c3901aa8f6ee";
    private static final String EXPECTED_CRYSTALLIZER =
            "346:d7177e57e99bb7a3f2ba972e9b1e24a43feea582c4b8a44b78f2165281f621e1";
    private static final String EXPECTED_CYCLOTRON =
            "42:5050b64e4c5dcf8c8e758f87ee0eb121119ce386210b824af548970cbfc5dbf3";
    private static final String EXPECTED_ELECTROLYSER_METAL =
            "36:461204e37a0b4dc36998a9af5b13a1016430c5c2b01c76010b2c0e116e856e5a";
    private static final String EXPECTED_ROTARY_FURNACE =
            "13:29813af06f9064fa562f3bea790abe8be409a2092bb1a9c68a6edd19dbf7cb9c";
    private static final String EXPECTED_SILEX =
            "301:634886440f7395c720d7d6eeff14121453885f1b6ad55841af19f1ac13c0b46f";
    private static final String EXPECTED_SHREDDER =
            "333:be4443000a813f22bc26d1d09d97ec6d3466d256ebc5af2845624d08c9972163";
}
