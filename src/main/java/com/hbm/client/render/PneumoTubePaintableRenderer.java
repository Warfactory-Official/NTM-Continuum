package com.hbm.client.render;

import com.hbm.blocks.network.PneumoTubePaintableBlock;
import com.hbm.tileentity.network.TileEntityPneumoTubePaintable;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBlockRenderState;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code PneumoTubePaintableBlock} through {@code RenderBlockMultipass}: pass 0 is a full cube in the
 * camouflage block's own faces or, unpainted, the plain {@code pneumatic_tube_paintable} sprite; pass 1, unless the
 * defuser turned it off, is the overlay - {@code _in} on the ejection face, {@code _out} on the insertion face
 * (1.7's own labelling, kept), the plain overlay everywhere else.
 */
public class PneumoTubePaintableRenderer extends GemBER<TileEntityPneumoTubePaintable, PneumoTubePaintableRenderer.State> {

    private static final String PLAIN = "hbm:textures/blocks/pneumatic_tube_paintable.png";
    private static final String OVERLAY = "hbm:textures/blocks/pneumatic_tube_paintable_overlay.png";
    private static final String OVERLAY_IN = "hbm:textures/blocks/pneumatic_tube_paintable_overlay_in.png";
    private static final String OVERLAY_OUT = "hbm:textures/blocks/pneumatic_tube_paintable_overlay_out.png";

    @Override
    protected void setup(Gem.Model<State> model) {
        // pass 0, unpainted
        model.part(TubeBoxes.box("pneumo_paintable_plain", 0, 0, 0, 1, 1, 1, TubeBoxes.ALL, 0), Surface.cutout(PLAIN))
                .visibleIf(s -> !s.painted);

        // pass 1, the overlay, one face at a time so the in/out faces can swap sprites
        for (Direction dir : Direction.values()) {
            int ordinal = dir.ordinal();
            var face = TubeBoxes.box("pneumo_paintable_overlay_" + dir.getName(), 0, 0, 0, 1, 1, 1, 1 << ordinal, 0);
            model.part(face, Surface.cutout(OVERLAY_IN)).visibleIf(s -> s.overlay && s.ejection == ordinal);
            model.part(face, Surface.cutout(OVERLAY_OUT))
                    .visibleIf(s -> s.overlay && s.insertion == ordinal && s.ejection != ordinal);
            model.part(face, Surface.cutout(OVERLAY))
                    .visibleIf(s -> s.overlay && s.insertion != ordinal && s.ejection != ordinal);
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityPneumoTubePaintable tube, State state, float partialTicks, Vec3 camera) {
        BlockState own = tube.getBlockState();
        BlockState camo = tube.getCamo();
        state.painted = camo != null;
        state.camo.extract(camo);
        state.overlay = !(own.getBlock() instanceof PneumoTubePaintableBlock) || own.getValue(PneumoTubePaintableBlock.OVERLAY);
        state.insertion = tube.insertionDir == null ? -1 : tube.insertionDir.ordinal();
        state.ejection = tube.ejectionDir == null ? -1 : tube.ejectionDir.ordinal();
    }

    /** The camouflage is another block's model, which has no fixed mesh of ours to allocate against. */
    @Override
    protected void submitUndeclared(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (!state.painted || state.camo.isEmpty()) return;
        sink.submitBlock(poseStack, state.camo, state.lightCoords, OverlayTexture.NO_OVERLAY);
    }

    public static final class State extends CompatRenderState {
        public final CompatBlockRenderState camo = new CompatBlockRenderState();
        boolean painted;
        boolean overlay;
        int insertion = -1;
        int ejection = -1;
    }
}
