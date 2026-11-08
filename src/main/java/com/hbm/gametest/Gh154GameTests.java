package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.StandardInfo;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.inventory.recipes.FusionRecipe;
import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.inventory.recipes.PlasmaForgeRecipe;
import com.hbm.inventory.recipes.PlasmaForgeRecipes;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;

/** GitHub issue 154: the plasma forge's striker works its pistons, and recipe tooltips carry 1.7's lines. */
public final class Gh154GameTests {

    private Gh154GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function STRIKER =
            TESTS.function("gh154_plasma_forge_striker_strikes", 20, 8, () -> Gh154GameTests::striker);
    public static final GameTests.Function TOOLTIPS =
            TESTS.function("gh154_fusion_recipe_tooltips", 20, () -> Gh154GameTests::tooltips);
    public static final GameTests.Function INTERNAL =
            TESTS.function("gh154_recipe_tooltip_internal_name", 20, () -> Gh154GameTests::internalName);

    public static void init() {}

    /**
     * 1.7 {@code STRIKER_STATE_MACHINE}: from rest the arm swings to a striking position (joint 3 to 30 degrees) within
     * 10 + a few ticks, then fires its pistons (joints 4 and 5 to 0.5) in turn. Drives the client tick by hand.
     */
    public static void striker(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos core = FoldedTestSupport.assemble(helper, ModMachineBlocks.fusion_plasma_forge.get(), new BlockPos(6, 1, 6));
        TileEntityFusionPlasmaForge forge = (TileEntityFusionPlasmaForge) level.getBlockEntity(core);
        forge.didProcess = true;
        boolean mounted = false, right = false, left = false, jet = false;
        for (int tick = 0; tick < 200; tick++) {
            TileEntityFusionPlasmaForge.clientTick(level, core, forge.getBlockState(), forge);
            double[] striker = forge.armStriker.angles;
            mounted |= striker[3] == 30D;
            right |= striker[4] == 0.5D;
            left |= striker[5] == 0.5D;
            jet |= forge.armJet.angles[2] != 0D;
        }
        helper.assertTrue(mounted, "the striker mount never swung to 30 degrees");
        helper.assertTrue(right && left, "the striker pistons never fired: right " + right + ", left " + left);
        helper.assertTrue(jet, "the jet arm never moved");
        helper.succeed();
    }

    /** The torus and forge tooltips against 1.7 {@code FusionRecipe.print} and {@code PlasmaForgeRecipe.print}. */
    public static void tooltips(GameTestHelper helper) {
        FusionRecipes.INSTANCE.registerRecipes();
        PlasmaForgeRecipes.INSTANCE.registerRecipes();
        FusionRecipe dd = (FusionRecipe) FusionRecipes.INSTANCE.recipeNameMap.get("fus.dd");
        List<String> lines = flatten(dd.print());
        List<String> expected = List.of("gui.recipe.duration: 5.0s", "gui.recipe.consumption: 25.0kHE/t",
                "gui.recipe.fusionIn: 750.0kKyU/t", "gui.recipe.fusionOut: 1.0MTU/t", "gui.recipe.fusionFlux: 50.0 flux/t",
                "gui.recipe.input:");
        helper.assertTrue(lines.subList(1, 7).equals(expected), "fus.dd tooltip " + lines);
        helper.assertTrue(lines.get(7).startsWith("  20mB ") && lines.get(9).startsWith("  1000mB "),
                "fus.dd fluid lines " + lines);

        PlasmaForgeRecipe forge = (PlasmaForgeRecipe) PlasmaForgeRecipes.INSTANCE.recipeOrderedList.get(0);
        List<String> forgeLines = flatten(forge.print());
        int consumption = indexOf(forgeLines, "gui.recipe.consumption: ");
        int plasma = indexOf(forgeLines, "gui.recipe.plasmaIn: ");
        helper.assertTrue(consumption > 0 && plasma == consumption + 1 && forgeLines.get(plasma).endsWith("TU/t"),
                "the plasma line follows the power line: " + forgeLines);

        // 1.7 ChanceOutputMulti.getLabel: "One of:", then each choice with its share cut to one decimal
        PrecAssRecipes.INSTANCE.registerRecipes();
        List<String> blueprints = flatten(PrecAssRecipes.INSTANCE.recipeNameMap.get("precass.blueprints").print());
        int oneOf = blueprints.indexOf("  One of:");
        helper.assertTrue(oneOf > 0 && blueprints.get(oneOf + 1).endsWith(" (10.0%)")
                && blueprints.get(oneOf + 2).startsWith("    16x ") && blueprints.get(oneOf + 2).endsWith(" (90.0%)"),
                "weighted outputs " + blueprints);
        helper.succeed();
    }

    /**
     * 1.7 {@code GenericRecipe.header}: with left Shift held a dark-gray "Internal: name" line follows the name. Fusion
     * cycles skip {@code header} in 1.7 and never show it; the crucible's amounts switch to mB under Shift.
     */
    public static void internalName(GameTestHelper helper) {
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        FusionRecipes.INSTANCE.registerRecipes();
        PlasmaForgeRecipes.INSTANCE.registerRecipes();
        CrucibleRecipes.INSTANCE.registerRecipes();
        List<GenericRecipe> headed = List.of(AssemblyMachineRecipes.INSTANCE.recipeOrderedList.get(0),
                PlasmaForgeRecipes.INSTANCE.recipeOrderedList.get(0), CrucibleRecipes.INSTANCE.recipeOrderedList.get(0));
        GenericRecipe fusion = FusionRecipes.INSTANCE.recipeNameMap.get("fus.dd");
        CrucibleRecipes.CrucibleRecipe crucible = (CrucibleRecipes.CrucibleRecipe) headed.get(2);
        try {
            StandardInfo.setShiftHeldPoll(() -> false);
            for (GenericRecipe recipe : headed) {
                helper.assertTrue(indexOf(flatten(recipe.print()), "Internal: ") < 0, "internal line without Shift: " + recipe.name);
            }
            String plain = flatten(crucible.print()).get(2);
            StandardInfo.setShiftHeldPoll(() -> true);
            for (GenericRecipe recipe : headed) {
                List<Component> lines = recipe.print();
                Component internal = lines.get(1);
                helper.assertTrue(internal.getString().equals("Internal: " + recipe.getInternalName())
                                && internal.getStyle().getColor() == TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY),
                        "Shift must add a dark-gray internal line under the name: " + flatten(lines));
            }
            helper.assertTrue(indexOf(flatten(fusion.print()), "Internal: ") < 0, "1.7 fusion cycles show no internal line");
            String mb = flatten(crucible.print()).get(3);
            helper.assertTrue(mb.endsWith("mB") && !plain.endsWith("mB"),
                    "crucible amounts: " + plain + " without Shift, " + mb + " with it");
        } finally {
            StandardInfo.setShiftHeldPoll(() -> false);
        }
        helper.succeed();
    }

    private static int indexOf(List<String> lines, String prefix) {
        for (int i = 0; i < lines.size(); i++) if (lines.get(i).startsWith(prefix)) return i;
        return -1;
    }

    /** Each line with translation keys in place of their text, so the test needs no client language. */
    private static List<String> flatten(List<Component> lines) {
        List<String> out = new ArrayList<>();
        for (Component line : lines) {
            StringBuilder text = new StringBuilder();
            append(line, text);
            out.add(text.toString());
        }
        return out;
    }

    private static void append(Component component, StringBuilder text) {
        if (component.getContents() instanceof TranslatableContents translatable) text.append(translatable.getKey());
        else text.append(component.plainCopy().getString());
        for (Component sibling : component.getSiblings()) append(sibling, text);
    }
}
