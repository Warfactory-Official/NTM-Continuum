package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.machine.ItemMold;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Holds the content {@code src/gametest/kubejs/startup_scripts/ntm_materials.js} declares. Registered only with KubeJS present. */
public final class ScriptingMaterialsGameTests {

    private ScriptingMaterialsGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function ITEMS =
            TESTS.function("scripting_materials_items", 20, () -> ScriptingMaterialsGameTests::items);
    public static final GameTests.Function SMELTING =
            TESTS.function("scripting_materials_smelting", 20, () -> ScriptingMaterialsGameTests::smelting);
    public static final GameTests.Function MOLDS =
            TESTS.function("scripting_materials_molds", 20, () -> ScriptingMaterialsGameTests::molds);
    public static final GameTests.Function HAZARDS =
            TESTS.function("scripting_materials_hazards", 20, () -> ScriptingMaterialsGameTests::hazards);
    public static final GameTests.Function WORLDGEN =
            TESTS.function("scripting_materials_worldgen", 20, () -> ScriptingMaterialsGameTests::worldgen);

    public static void init() {}

    public static void items(GameTestHelper helper) {
        NTMMaterial mat = Mats.matByName.get("gt_mithril");
        helper.assertTrue(mat != null && mat.id == 990100, "scripted material");
        for (String id : new String[] {"ingot_gt_mithril", "nugget_gt_mithril", "powder_gt_mithril", "plate_gt_mithril",
                "billet_gt_mithril", "block_gt_mithril", "ore_gt_mithril", "ore_deepslate_gt_mithril", "gem_gt_cinder",
                "ore_gt_cinder"}) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Ids.of(Constants.MOD_ID, id)), "minted item " + id);
        }
        for (String id : new String[] {"block_gt_mithril", "ore_gt_mithril", "ore_deepslate_gt_mithril", "ore_gt_cinder"}) {
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(Ids.of(Constants.MOD_ID, id)), "minted block " + id);
        }
        helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(Ids.of(Constants.MOD_ID, "ingot_gt_cinder")), "unrequested shape");
        helper.succeed();
    }

    public static void smelting(GameTestHelper helper) {
        NTMMaterial mat = Mats.matByName.get("gt_mithril");
        assertMelts(helper, new ItemStack(Items.ANCIENT_DEBRIS), List.of(new Mats.MaterialStack(mat, 72),
                new Mats.MaterialStack(Mats.MAT_IRON, 8)), "explicit smelting entry");
        assertMelts(helper, stack("ingot_gt_mithril"), List.of(new Mats.MaterialStack(mat, MaterialShapes.INGOT.quantity)), "minted ingot");
        assertMelts(helper, stack("block_gt_mithril"), List.of(new Mats.MaterialStack(mat, MaterialShapes.BLOCK.quantity)), "minted block");
        assertMelts(helper, stack("ore_deepslate_gt_mithril"), List.of(new Mats.MaterialStack(mat, 144),
                new Mats.MaterialStack(Mats.MAT_STONE, 18)), "minted ore");
        helper.assertTrue(Mats.getMaterialsFromItem(stack("gem_gt_cinder")).isEmpty(), "non-smeltable material");
        helper.succeed();
    }

    private static void assertMelts(GameTestHelper helper, ItemStack stack, List<Mats.MaterialStack> expected, String what) {
        List<Mats.MaterialStack> melted = Mats.getMaterialsFromItem(stack);
        boolean same = melted.size() == expected.size();
        for (int i = 0; same && i < melted.size(); i++) {
            same = melted.get(i).material == expected.get(i).material && melted.get(i).amount == expected.get(i).amount;
        }
        helper.assertTrue(same, what);
    }

    public static void molds(GameTestHelper helper) {
        MaterialShapes rod = MaterialShapes.byName("GT_ROD");
        NTMMaterial mat = Mats.matByName.get("gt_mithril");
        Item small = item("mold_gt_rod");
        helper.assertTrue(small instanceof ItemMold m && m.mold instanceof ItemMold.MoldShape shape
                && shape.shape == rod && shape.size == 0 && shape.getCost() == 36, "scripted small mold");
        ItemStack cast = ((ItemMold) small).mold.getOutput(mat);
        helper.assertTrue(cast.is(ModAutogenItems.get(rod, mat)) && cast.getCount() == 1, "scripted mold casts the autogen item");
        Item large = item("mold_gt_slabs");
        helper.assertTrue(large instanceof ItemMold m && m.mold.size == 1 && m.mold.getCost() == 216 * 3, "scripted basin mold");
        helper.assertTrue(AnvilRecipes.getSmithing().stream().anyMatch(r -> r.getSimpleOutput().is(small))
                && AnvilRecipes.getSmithing().stream().anyMatch(r -> r.getSimpleOutput().is(large)), "scripted mold anvil recipes");
        helper.succeed();
    }

    public static void hazards(GameTestHelper helper) {
        helper.assertTrue(HazardSystem.getHazardLevelFromStack(stack("ingot_gt_mithril"), HazardRegistry.RADIATION) == 5D,
                "ingot radiation");
        helper.assertTrue(HazardSystem.getHazardLevelFromStack(stack("nugget_gt_mithril"), HazardRegistry.RADIATION)
                == 5F * HazardRegistry.nugget, "nugget radiation");
        helper.assertTrue(HazardSystem.getHazardLevelFromStack(stack("block_gt_mithril"), HazardRegistry.RADIATION)
                == 5F * HazardRegistry.block, "block radiation");
        helper.assertTrue(HazardSystem.getHazardLevelFromStack(stack("ingot_gt_mithril"), HazardRegistry.HOT) == 3D, "extra hazard");
        helper.assertTrue(HazardSystem.getHazardLevelFromStack(stack("gem_gt_cinder"), HazardRegistry.RADIATION) == 0D,
                "non-radioactive material");
        helper.succeed();
    }

    public static void worldgen(GameTestHelper helper) {
        for (String feature : new String[] {"scripted_ore_gt_mithril", "scripted_ore_gt_cinder"}) {
            var key = ResourceKey.create(Registries.PLACED_FEATURE, Ids.of(Constants.MOD_ID, feature));
            helper.assertTrue(helper.getLevel().registryAccess().lookup(Registries.PLACED_FEATURE)
                    .flatMap(features -> features.get(key)).isPresent(), "placed feature " + feature);
        }
        helper.succeed();
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, id));
    }

    private static ItemStack stack(String id) {
        return new ItemStack(item(id));
    }
}
