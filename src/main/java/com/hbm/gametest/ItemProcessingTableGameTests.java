package com.hbm.gametest;

import com.google.gson.JsonElement;
import com.hbm.Constants;
import com.hbm.foundry.ArcFurnaceRecipes;
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
import dev.golem.core.gametest.GameTests;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/**
 * The item-processing machine tables as datapack tables (M2 R-C1): crucible alloying, material distribution, arc
 * furnace, centrifuge, crystallizer, cyclotron, electrolyser (metal), rotary furnace, SILEX and the shredder's explicit
 * rows load from {@code data/hbm/recipe/<table>} exactly as converted from the former Java tables on 2026-10-08.
 */
public final class ItemProcessingTableGameTests {

    private ItemProcessingTableGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function TABLES =
            TESTS.function("recipe_table_item_processing", 200, () -> ItemProcessingTableGameTests::tables);

    public static void init() {}

    static String describeCrucible(CrucibleRecipes.CrucibleRecipe recipe) {
        return recipe.getInternalName() + " f=" + recipe.frequency + " icon=" + com.hbm.inventory.recipes.loader.RecipeText.describe(recipe.getIcon())
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

    // ------------------------------------------------------------------------------------------------ permanent

    /**
     * Each table holds exactly the rows converted from the Java table on 2026-10-08 (row count and a SHA-256 over every
     * row's JSON; the conversion run compared every row of the former Java tables with the loaded data, field by field,
     * on 26.1 NeoForge), every row survives its JSON, and spot rows carry 1.7's values.
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
        check(failures, "material obsidian", MaterialCodecs.describe(MaterialDistribution.items().get(net.minecraft.world.item.Items.OBSIDIAN)),
                "[obsidian:648,]");
        var flint = ArcFurnaceRecipes.getOutput(new ItemStack(net.minecraft.world.item.Items.FLINT), false);
        check(failures, "arc flint", flint == null ? "-" : ArcFurnaceRecipes.describe(flint),
                "minecraft:flint x1 -> hbm:nugget_silicon x4 + [silicon:36,]");
        // 1.7's first row, the rare chunk (the table is in 1.7's line order since History §19fc)
        var first = CentrifugeRecipes.recipes.isEmpty() ? null : CentrifugeRecipes.recipes.get(0);
        check(failures, "centrifuge first", first == null ? "-" : CentrifugeRecipes.describe(first),
                "hbm:chunk_ore_rare x1 -> [hbm:powder_cobalt_tiny x2,hbm:powder_boron_tiny x2,hbm:powder_niobium_tiny x2,hbm:nugget_zirconium x3,]");
        CrystallizerRecipes.register();
        var malachite = CrystallizerRecipes.recipes.stream().filter(r -> r.output.getItem() == com.hbm.items.ModMachineItems.scraps.get()).findFirst();
        check(failures, "crystallizer malachite scraps", malachite.map(r -> com.hbm.inventory.recipes.loader.RecipeText.describe(r.input)
                + " " + MaterialCodecs.describe(com.hbm.items.machine.ItemScraps.getMats(r.output)) + " t=" + r.duration + " "
                + r.fluid.getKey() + "x" + r.acidAmount).orElse("-"), "#c:ingots/malachite x1 copper:72 t=300 sulfuric_acidx250");
        long compat = CrystallizerRecipes.TABLE.rows().stream().filter(r -> r.requires().isPresent()).count();
        check(failures, "crystallizer compat rows", String.valueOf(compat), "3");
        var cyc = CyclotronRecipes.recipes.isEmpty() ? null : CyclotronRecipes.recipes.get(0);
        check(failures, "cyclotron first", cyc == null ? "-" : CyclotronRecipes.describe(cyc),
                "hbm:part_lithium x1 + #c:dusts/lithium x1 -> hbm:powder_beryllium x1 am=50");
        var iron = ElectrolyserMetalRecipes.getRecipe(new ItemStack(com.hbm.items.ModItems.crystal_iron.get()));
        check(failures, "electrolyser crystal_iron", iron == null ? "-" : ElectrolyserMetalRecipes.describe(iron),
                "hbm:crystal_iron x1 -> iron:432 + titanium:144 + [hbm:powder_lithium_tiny x3,] t=600");
        RotaryFurnaceRecipes.register();
        var rot = RotaryFurnaceRecipes.recipes.isEmpty() ? null : RotaryFurnaceRecipes.recipes.get(0);
        check(failures, "rotary first", rot == null ? "-" : RotaryFurnaceRecipes.describe(rot),
                "in=#c:ingots/iron x1,#c:gems/coal x1, fluid=- -> steel:72 t=100 steam=100");
        var uranium = SILEXRecipes.getOutput(new ItemStack(com.hbm.items.ModItems.ingot_uranium.get()));
        check(failures, "silex uranium", uranium == null ? "-" : SILEXRecipes.describe(uranium),
                "#c:ingots/uranium x1 900/100 VISIBLE -> hbm:nugget_u235 x1@1 hbm:nugget_u238 x1@11"); // 1.7 U.ingot()
        check(failures, "shredder scrap", com.hbm.inventory.recipes.loader.RecipeText.describe(
                ShredderRecipes.getShredderResult(new ItemStack(com.hbm.items.ModItems.scrap.get()))), "hbm:dust x1");

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

    // Re-pinned 2026-10-08 when the hash moved to key-sorted JSON (CanonicalJson); the rows are the same: the raw JSON of
    // every table still hashed to the previous pins on 26.1 NeoForge. Centrifuge, crystallizer and SILEX re-pinned 2026-10-12
    // when the tables became Java sources in 1.7's line order (History §19fc): fields equal, only "order" changed. Arc
    // furnace, centrifuge, crystallizer and SILEX re-pinned again for the 1.7 behaviour pass (§19fc): ore names as tags, the
    // LBSM centrifuge rows, the crystallizer's exact calcium and sawdust items, KEY_SAND as c:sands. Shredder re-pinned after the
    // audit (§19fc): 1.7's wildcard steel_scaffold also shreds the three coloured scaffolds.
    private static final String EXPECTED_CRUCIBLE =
            "13:16a916835a5adb64587bf5a8f085f4704b659f6a3541e82ca15865530fdef281";
    private static final String EXPECTED_MATERIAL_DISTRIBUTION =
            "43:8d8d6efeb499f2297406b4b922a29f241ac800c4e8fc250f51c1325f15ff7e1b";
    private static final String EXPECTED_ARC_FURNACE =
            "79:be19d879fabc231449a78a8771ebf0a2ebbe33ab556d4a152e842990b7bd0a37";
    private static final String EXPECTED_CENTRIFUGE =
            "160:85789af01024284d868e94bb451b8185e69d907e5319dab6120071b923caee1b";
    private static final String EXPECTED_CRYSTALLIZER =
            "332:183c0f94bbd1ad8568280b5100efe3ef7ad298e95e4a5dd34b50ce2979a8a423";
    private static final String EXPECTED_CYCLOTRON =
            "42:5050b64e4c5dcf8c8e758f87ee0eb121119ce386210b824af548970cbfc5dbf3";
    private static final String EXPECTED_ELECTROLYSER_METAL =
            "36:461204e37a0b4dc36998a9af5b13a1016430c5c2b01c76010b2c0e116e856e5a";
    private static final String EXPECTED_ROTARY_FURNACE =
            "12:d37aa199ec5e4b1ed89be2f7c30fa5d7d7193074fcd2181a8546f802584385aa";
    private static final String EXPECTED_SILEX =
            "301:634886440f7395c720d7d6eeff14121453885f1b6ad55841af19f1ac13c0b46f";
    private static final String EXPECTED_SHREDDER =
            "330:629e83c8683eab1b9c9e4d9f247c619877d14634ccd5f84ddf11eaa7ddc6b407";
}
