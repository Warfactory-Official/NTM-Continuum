package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.entity.missile.EntitySoyuz;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.HashMap;
import java.util.Map;

/**
 * The launch vehicle. Upstream draws it part by part, binding a different texture for each: the engine
 * block, the two stages, the payload and its shroud, the escape tower and its thrusters, both engine
 * clusters and the four strap-on boosters. That shape is kept, with one render type per texture, and the
 * skin chooses which set of textures those are.
 */
public class SoyuzRenderer extends CompatEntityRenderer<EntitySoyuz, SoyuzRenderState> {

    /** The three paint jobs, in the order the item's variants are numbered. */
    private static final String[] SKINS = {"soyuz", "soyuz_luna", "soyuz_authentic"};

    /** Which texture each part of the model wears. */
    private static final String[][] PARTS = {
            {"EngineBlock", "engineblock"},
            {"BottomStage", "bottomstage"},
            {"TopStage", "topstage"},
            {"Payload", "payload"},
            {"PayloadBlocks", "payloadblocks"},
            {"LES", "les"},
            {"LESThrusters", "lesthrusters"},
            {"MainEngines", "mainengines"},
            {"SideEngines", "sideengines"},
    };

    private static final String[] BOOSTER_PARTS = {"Booster", "BoosterEngines", "BoosterSide"};
    private static final String[] BOOSTER_TEXTURES = {"booster", "mainengines", "boosterside"};

    private final WavefrontObj model;
    private final Map<String, RenderType> types = new HashMap<>();

    public SoyuzRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/soyuz.obj"));
    }

    private RenderType type(String skin, String texture) {
        return types.computeIfAbsent(skin + "/" + texture, key -> CompatRenderTypes.entityCutout(
                dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/" + key + ".png")));
    }

    @Override
    public SoyuzRenderState createRenderState() {
        return new SoyuzRenderState();
    }

    @Override
    public void extract(EntitySoyuz entity, SoyuzRenderState state, float partialTicks) {
        state.skin = Math.floorMod(entity.getSkin(), SKINS.length);
    }

    @Override
    public void submit(SoyuzRenderState state, PoseStack ps, SubmitSink collector, CameraInfo camera) {
        String skin = SKINS[state.skin];
        int light = state.lightCoords;

        for (String[] part : PARTS) {
            submitPart(ps, collector, type(skin, part[1]), part[0], light);
        }

        for (int i = 0; i < BOOSTER_PARTS.length; i++) {
            RenderType type = type(skin, BOOSTER_TEXTURES[i]);
            for (int booster = 0; booster < 4; booster++) {
                submitPart(ps, collector, type, BOOSTER_PARTS[i] + String.format(".%03d", booster), light);
            }
        }
    }

    private void submitPart(PoseStack ps, SubmitSink collector, RenderType type, String name, int light) {
        collector.submitCustomGeometry(ps, type,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
