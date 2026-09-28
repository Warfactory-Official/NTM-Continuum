package com.hbm.client.render;

import com.hbm.blocks.generic.BlockRebar.TileEntityRebar;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Geometry;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code BlockRebar.renderRebar} (from {@code onRenderWorldLastEvent}): a rebar holding concrete shows the
 * liquid concrete as a box filling the cell from the floor up to {@code progress / 1000}, drawn with 1.7's
 * {@code renderStandardBlock} UVs - the sides show the bottom slice of the {@code concrete_liquid} texture rather
 * than a squashed whole. 1.7 capped how many it drew per frame ({@code RENDER_REBAR_LIMIT}); the port draws
 * each rebar's box from its own block entity renderer, so no cap applies.
 */
public class RenderRebarFill implements CompatBER<TileEntityRebar, RenderRebarFill.State> {

    public static final class State extends CompatRenderState {
        int progress;
    }

    private static final Identifier CONCRETE = Ids.of("hbm", "textures/blocks/concrete_liquid.png");
    private static final Geometry[] BOXES = new Geometry[1001];

    private static Geometry box(int progress) {
        Geometry geometry = BOXES[progress];
        if (geometry == null) {
            geometry = TubeBoxes.box("rebar_fill_" + progress, 0, 0, 0, 1, progress / 1000D, 1, TubeBoxes.ALL, 0);
            BOXES[progress] = geometry;
        }
        return geometry;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityRebar rebar, State state, float partialTicks, Vec3 cameraPosition) {
        state.progress = Math.max(0, Math.min(1000, rebar.progress));
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        if (state.progress <= 0) return;
        Geometry geometry = box(state.progress);
        int light = state.lightCoords;
        sink.submitCustomGeometry(pose, CompatRenderTypes.entitySolid(CONCRETE),
                (p, buffer) -> geometry.draw(p, buffer, light, OverlayTexture.NO_OVERLAY, -1));
    }
}
