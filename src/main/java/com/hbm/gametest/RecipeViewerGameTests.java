package com.hbm.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.Constants;
import com.hbm.handler.NTMToolHandler;
import com.hbm.handler.jei.BookRecipeHandler;
import com.hbm.handler.jei.ConstructionRecipeHandler;
import com.hbm.handler.jei.CrucibleCastingHandler;
import com.hbm.handler.jei.CrucibleSmeltingHandler;
import com.hbm.handler.jei.ElectrolyserFluidRecipeHandler;
import com.hbm.handler.jei.ElectrolyserMetalRecipeHandler;
import com.hbm.handler.jei.FluidContainerRecipeHandler;
import com.hbm.handler.jei.HbmJeiRecipeTypes;
import com.hbm.handler.jei.HbmRecipeViewer;
import com.hbm.handler.jei.SILEXRecipeHandler;
import com.hbm.handler.jei.ToolingRecipeHandler;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.viewer.ViewerCategory;
import dev.golem.core.viewer.ViewerType;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Items;

/**
 * The recipe-viewer categories as both viewers receive them from {@link HbmRecipeViewer}: every translated title has an
 * English line, and the tabs 1.7's NEI had are declared with rows from the real recipe sources.
 */
public final class RecipeViewerGameTests {

    private RecipeViewerGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function TITLES =
            TESTS.function("viewer_category_titles_translated", 20, () -> RecipeViewerGameTests::titlesTranslated);
    public static final GameTests.Function NEI_TABS =
            TESTS.function("viewer_nei_tabs_have_recipes", 20, () -> RecipeViewerGameTests::neiTabsHaveRecipes);

    public static void init() {}

    private static List<ViewerCategory<?>> categories() {
        List<ViewerCategory<?>> categories = new ArrayList<>();
        HbmRecipeViewer.INSTANCE.categories(added -> categories.addAll(List.of(added)));
        return categories;
    }

    private static void titlesTranslated(GameTestHelper helper) {
        JsonObject lang;
        try (InputStream in = RecipeViewerGameTests.class.getResourceAsStream("/assets/hbm/lang/en_us.json")) {
            helper.assertTrue(in != null, "en_us.json is on the classpath");
            lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        List<ViewerCategory<?>> categories = categories();
        List<String> missing = new ArrayList<>();
        for (ViewerCategory<?> category : categories) {
            if (category.title().getContents() instanceof TranslatableContents translatable
                    && !lang.has(translatable.getKey())) {
                missing.add(category.type().id() + " -> " + translatable.getKey());
            }
        }
        helper.assertTrue(categories.size() >= 50, "the viewer declares its categories: " + categories.size());
        helper.assertTrue(missing.isEmpty(), "every category title key is in en_us.json; missing " + missing);
        helper.succeed();
    }

    private static void neiTabsHaveRecipes(GameTestHelper helper) {
        List<ViewerCategory<?>> categories = categories();
        Map<ViewerType<?>, Integer> counts = new LinkedHashMap<>();
        counts.put(HbmJeiRecipeTypes.SILEX, SILEXRecipeHandler.buildRecipes().size());
        counts.put(HbmJeiRecipeTypes.ELECTROLYSIS_FLUID, ElectrolyserFluidRecipeHandler.buildRecipes().size());
        counts.put(HbmJeiRecipeTypes.ELECTROLYSIS_METAL, ElectrolyserMetalRecipeHandler.buildRecipes().size());
        counts.put(HbmJeiRecipeTypes.FLUID_CONTAINERS, FluidContainerRecipeHandler.buildRecipes().size());
        counts.put(HbmJeiRecipeTypes.CONSTRUCTION, ConstructionRecipeHandler.buildRecipes().size());
        counts.put(HbmJeiRecipeTypes.TOOLING, ToolingRecipeHandler.buildRecipes().size());
        counts.put(HbmJeiRecipeTypes.CRUCIBLE_SMELTING, CrucibleSmeltingHandler.buildRecipes().size());
        counts.put(HbmJeiRecipeTypes.CRUCIBLE_CASTING, CrucibleCastingHandler.buildRecipes().size());
        counts.put(HbmJeiRecipeTypes.BOOK, BookRecipeHandler.buildRecipes().size());
        for (Map.Entry<ViewerType<?>, Integer> tab : counts.entrySet()) {
            helper.assertTrue(categories.stream().anyMatch(c -> c.type().equals(tab.getKey())), tab.getKey() + " is declared");
            helper.assertTrue(tab.getValue() > 0, tab.getKey() + " has recipes: " + tab.getValue());
        }
        Constants.LOG.info("[viewer] 1.7 NEI tab rows: {}", counts);

        // 1.7 ConstructionHandler lists six builds, each with its assembled block
        List<ConstructionRecipeHandler.Row> builds = ConstructionRecipeHandler.buildRecipes();
        helper.assertTrue(builds.size() == 6, "six construction rows: " + builds.size());
        for (ConstructionRecipeHandler.Row build : builds) {
            helper.assertTrue(!build.output().isEmpty(), "every construction row shows its block: " + build.machine());
        }
        helper.assertTrue(counts.get(HbmJeiRecipeTypes.TOOLING) == NTMToolHandler.all().size(),
                "one tooling row per tool conversion");
        helper.assertTrue(FluidContainerRecipeHandler.buildRecipes().stream().anyMatch(e -> e.full().is(Items.WATER_BUCKET)
                && e.empty().is(Items.BUCKET) && e.amount() == 1000), "the water bucket is a fluid container row");
        helper.succeed();
    }
}
