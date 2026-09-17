package com.hbm.client.render.entity;

import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * 1.7 SoyuzPronter: the launch vehicle's body, one texture per structural group with the skin picked from the
 * variant. The four boosters, their engines and their fairings are separate OBJ groups sharing one skin each.
 */
public final class SoyuzMesh {

    private static final int MAINENGINES = 7;
    private static final int BOOSTER = 9;
    private static final int BOOSTERSIDE = 10;

    private static final String[] SINGLE = {
            "EngineBlock", "BottomStage", "TopStage", "Payload", "PayloadBlocks", "LES", "LESThrusters",
            "MainEngines", "SideEngines"};
    private static final String MEMENTO = "Memento";
    private static final String[] BOOSTERS = {"Booster.000", "Booster.001", "Booster.002", "Booster.003"};
    private static final String[] BOOSTER_ENGINES = {
            "BoosterEngines.000", "BoosterEngines.001", "BoosterEngines.002", "BoosterEngines.003"};
    private static final String[] BOOSTER_SIDES = {
            "BoosterSide.000", "BoosterSide.001", "BoosterSide.002", "BoosterSide.003"};

    private SoyuzMesh() {
    }

    /** 1.7 {@code prontSoyuz:96} refuses a skin outside the three rather than clamping. */
    public static boolean hasSkin(int skin) {
        return skin >= 0 && skin < ResourceManager.soyuz_skin_tex.length;
    }

    public static int wrapSkin(int skin) {
        return Mth.positiveModulo(skin, ResourceManager.soyuz_skin_tex.length);
    }

    public static void submit(PoseStack poseStack, SubmitSink sink, int light, int skinIndex) {
        Identifier[] skin = ResourceManager.soyuz_skin_tex[skinIndex];

        for (int i = 0; i < SINGLE.length; i++) part(poseStack, sink, light, skin[i], SINGLE[i]);
        part(poseStack, sink, light, ResourceManager.soyuz_memento_tex, MEMENTO);
        for (String booster : BOOSTERS) part(poseStack, sink, light, skin[BOOSTER], booster);
        for (String engine : BOOSTER_ENGINES) part(poseStack, sink, light, skin[MAINENGINES], engine);
        for (String side : BOOSTER_SIDES) part(poseStack, sink, light, skin[BOOSTERSIDE], side);
    }

    private static void part(PoseStack poseStack, SubmitSink sink, int light, Identifier texture, String part) {
        sink.submitCustomGeometry(poseStack, CompatRenderTypes.entityCutout(texture), (pose, buffer) ->
                NtmEntityModels.SOYUZ.renderPart(part, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
