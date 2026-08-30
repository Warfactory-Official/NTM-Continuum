package com.hbm.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.client.render.PlasmaForgeRenderMath;
import com.hbm.inventory.gui.PlasmaForgeGuiLayout;
import net.minecraft.gametest.framework.GameTestHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Pure predicate and timing contracts for the plasma-forge renderer's extracted state. */
public final class PlasmaForgeRenderGameTests {

    private PlasmaForgeRenderGameTests() {}

    public static void predicates(GameTestHelper helper) {
        helper.assertTrue(PlasmaForgeRenderMath.shouldRenderJetPlume(true, 20D, 20D),
                "stationary processing jet arm must emit a plume");
        helper.assertFalse(PlasmaForgeRenderMath.shouldRenderJetPlume(false, 20D, 20D),
                "idle jet arm must not emit a plume");
        helper.assertFalse(PlasmaForgeRenderMath.shouldRenderJetPlume(true, 20D, 19D),
                "moving jet arm must not emit a plume");
        helper.assertFalse(PlasmaForgeRenderMath.shouldRenderJetPlume(true, 0D, 0D),
                "home-position jet arm must not emit a plume");

        helper.assertTrue(Float.compare(PlasmaForgeRenderMath.OUTER_PLUME_NARROW, 0.09375F) == 0,
                "outer plume width changed");
        helper.assertTrue(Float.compare(PlasmaForgeRenderMath.BEAM_BOTTOM, 1F) == 0,
                "beam bottom changed");
        helper.assertTrue(Float.compare(PlasmaForgeRenderMath.BEAM_LENGTH, 1.5F) == 0,
                "beam length changed");
        helper.assertTrue(Float.compare(PlasmaForgeRenderMath.BEAM_TOP, 2.5F) == 0,
                "beam top must be bottom plus t");
        helper.assertTrue(Float.compare(PlasmaForgeRenderMath.beamBottomUv(0.25F), 1.75F) == 0,
                "beam bottom UV must use t");
        helper.assertTrue(Float.compare(PlasmaForgeRenderMath.beamTopUv(0.25F), 0.25F) == 0,
                "beam top UV changed");

        helper.assertTrue(PlasmaForgeRenderMath.within(35D * 35D, 35D),
                "the item distance boundary must remain visible");
        helper.assertTrue(PlasmaForgeRenderMath.within(50D * 50D, 50D),
                "the beam distance boundary must remain visible");
        helper.assertFalse(PlasmaForgeRenderMath.within(50D * 50D + 0.001D, 50D),
                "values beyond the beam distance must be culled");

        long time = 1500L;
        helper.assertTrue(Float.compare(PlasmaForgeRenderMath.plasmaAlpha(time),
                        0.5F + (float) (Math.sin(time / 500D) * 0.25D)) == 0,
                "plasma alpha timing changed");
        helper.assertTrue(Float.compare(PlasmaForgeRenderMath.plasmaColorScale(false, time),
                        PlasmaForgeRenderMath.plasmaAlpha(time)) == 0,
                "main plasma pass must use the pulse");
        helper.assertTrue(Float.compare(PlasmaForgeRenderMath.GLOW_COLOR_SCALE, 2F) == 0
                        && Float.compare(PlasmaForgeRenderMath.plasmaColorScale(true, time), 2F) == 0
                        && Float.compare(PlasmaForgeRenderMath.plasmaColorScale(true, time + 100_000L), 2F) == 0,
                "both glow passes must use constant 2x colour");
        helper.assertTrue(Float.compare(PlasmaForgeRenderMath.mainScroll(time),
                        (float) (Math.sin(Math.PI / 2D * Math.cos(time / 750D)) % 1D)) == 0,
                "base plasma scroll timing changed");
        helper.assertTrue(Float.compare(PlasmaForgeRenderMath.firstGlowScroll(time),
                        (float) (Math.sin(time / 1000D) % 1D + time / 10_000D % 1D)) == 0,
                "first glow scroll timing changed");
        helper.assertTrue(Float.compare(PlasmaForgeRenderMath.secondGlowScroll(time),
                        (float) (Math.sin(time / 600D + 2D) % 1D + time / 5_000D % 1D)) == 0,
                "second glow scroll timing changed");
        helper.assertTrue(Float.compare(PlasmaForgeRenderMath.itemBob(40F, 0.5F),
                        (float) (Math.sin(40.5D * 0.1D) * 0.0625D)) == 0,
                "item bob timing changed");
        helper.assertTrue(Float.compare(PlasmaForgeRenderMath.beamScroll(40F, 0.5F), 40.5F / 15F % 1F) == 0,
                "stellar-flux beam timing changed");
        helper.assertTrue(itemHasBlackPlasmaPass(),
                "plasma forge item must retain the excluded Plasma part in a black pass");
        helper.assertTrue(PlasmaForgeGuiLayout.RECIPE_X == 7 && PlasmaForgeGuiLayout.RECIPE_Y == 80
                        && PlasmaForgeGuiLayout.RECIPE_SIZE == 18
                        && PlasmaForgeGuiLayout.RECIPE_ICON_X == 8 && PlasmaForgeGuiLayout.RECIPE_ICON_Y == 81,
                "plasma forge recipe hitbox/icon coordinates changed");
        helper.assertTrue(hasPlasmaInputLocalization(),
                "plasma forge input-energy localization changed or is missing");
        helper.succeed();
    }

    private static boolean hasPlasmaInputLocalization() {
        try (InputStream stream = PlasmaForgeRenderGameTests.class.getClassLoader()
                .getResourceAsStream("assets/hbm/lang/en_us.json")) {
            if (stream == null) return false;
            JsonObject language = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            return language.has(PlasmaForgeGuiLayout.PLASMA_INPUT_KEY)
                    && "Plasma Input Energy".equals(language.get(PlasmaForgeGuiLayout.PLASMA_INPUT_KEY).getAsString());
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    private static boolean itemHasBlackPlasmaPass() {
        try (InputStream stream = PlasmaForgeRenderGameTests.class.getClassLoader()
                .getResourceAsStream("assets/hbm/items/fusion_plasma_forge.json")) {
            if (stream == null) return false;
            JsonObject special = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("model");
            if (special == null || !"minecraft:special".equals(special.get("type").getAsString())
                    || !"hbm:item/machine_obj_base".equals(special.get("base").getAsString())) return false;
            JsonObject model = special.getAsJsonObject("model");
            if (model == null || !"hbm:machine_obj".equals(model.get("type").getAsString())) return false;
            return contains(model.getAsJsonArray("exclude"), PlasmaForgeRenderMath.ITEM_BLACK_PART)
                    && contains(model.getAsJsonArray("blackParts"), PlasmaForgeRenderMath.ITEM_BLACK_PART);
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    private static boolean contains(JsonArray values, String value) {
        if (values == null) return false;
        for (var element : values) if (value.equals(element.getAsString())) return true;
        return false;
    }
}
