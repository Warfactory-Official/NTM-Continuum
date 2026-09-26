package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.content.NtmPhases;
import com.hbm.api.content.Spec;
import dev.golem.core.content.Phase;
import dev.golem.core.content.Pipeline;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** NTM's content phases post once, in order; a recipe edit replays across table resets and reloads. */
public final class PipelineGameTests {
    private PipelineGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function ORDER = TESTS.function("pipeline_order", 20, () -> PipelineGameTests::order);
    public static final GameTests.Function RELOAD = TESTS.function("pipeline_recipe_reload", 20, () -> PipelineGameTests::reload);

    private static final List<String> POSTED = new ArrayList<>();

    /** Before any stage posts. */
    public static void init() {
        for (Phase<?> phase : List.of(NtmPhases.SHAPES, NtmPhases.MATERIALS, NtmPhases.FLUIDS, NtmPhases.FUELS,
                NtmPhases.RECIPES, NtmPhases.HAZARDS)) {
            phase.onPosted(event -> POSTED.add(phase.id()));
        }
    }

    private static void order(GameTestHelper helper) {
        List<String> first = POSTED.subList(0, Math.min(6, POSTED.size()));
        helper.assertTrue(first.equals(List.of("hbm:shapes", "hbm:materials", "hbm:fluids", "hbm:fuels", "hbm:recipes", "hbm:hazards")),
                "phase order " + POSTED);
        helper.succeed();
    }

    private static void reload(GameTestHelper helper) {
        ItemStack input = new ItemStack(Items.POINTED_DRIPSTONE);
        var subscription = Pipeline.on(NtmPhases.RECIPES,
                event -> event.shredder.add(new Spec.Ingredient("minecraft:pointed_dripstone"), new Spec.Item("minecraft:flint")));
        try {
            helper.assertTrue(com.hbm.inventory.recipes.ShredderRecipes.getShredderResult(input).is(Items.FLINT), "late handler applied at once");
            Pipeline.reload(NtmPhases.RECIPES);
            helper.assertTrue(com.hbm.inventory.recipes.ShredderRecipes.getShredderResult(input).is(Items.FLINT), "edit replays after reload");
        } finally {
            subscription.remove();
            Pipeline.reload(NtmPhases.RECIPES);
        }
        helper.assertTrue(!com.hbm.inventory.recipes.ShredderRecipes.getShredderResult(input).is(Items.FLINT), "removed handler's edit is gone");
        helper.succeed();
    }
}
