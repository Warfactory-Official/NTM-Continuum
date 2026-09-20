package com.hbm.client.render;

import com.hbm.blocks.network.BlockCablePaintable;
import com.hbm.tileentity.network.TileEntityCablePaintable;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBlockRenderState;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Quads;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * The painted cable: the paint block's own model drawn in the cable's cell (1.7's second render pass
 * borrowed the paint's icons) with the {@code red_cable_overlay} stripes over it unless the defuser hid them.
 * An unpainted cable is drawn by its block model and this renderer draws nothing.
 */
public class RenderCablePaintable extends GemBER<TileEntityCablePaintable, RenderCablePaintable.State> {

    public static class State extends CompatRenderState {
        public final CompatBlockRenderState paint = new CompatBlockRenderState();
        public boolean overlay;
    }

    private static final float E = 0.002F;
    private final Geometry cube = cube("energynet_paint_overlay", -E, 1F + E);
    private final Surface overlay = Surface.cutoutNoCull("hbm:textures/blocks/red_cable_overlay.png");

    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.part(cube, overlay).visibleIf(s -> s.overlay && !s.paint.isEmpty());
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityCablePaintable be, State state, float partialTicks, Vec3 cameraPosition) {
        Block paint = be.getPaint();
        state.paint.extract(paint == null ? null : paint.defaultBlockState());
        state.overlay = be.getBlockState().hasProperty(BlockCablePaintable.OVERLAY)
                && be.getBlockState().getValue(BlockCablePaintable.OVERLAY);
    }

    @Override
    protected void submitUndeclared(State state, PoseStack ps, SubmitSink sink, CameraInfo camera) {
        if (state.paint.isEmpty()) return;
        sink.submitBlock(ps, state.paint, state.lightCoords, OverlayTexture.NO_OVERLAY);
    }

    /** A textured box from {@code lo} to {@code hi} on every axis, one full texture tile per face. */
    private static Geometry cube(String name, float lo, float hi) {
        Quads.Builder b = Quads.named(name);
        // down
        b.normal(0, -1, 0).vertex(lo, lo, lo, 0, 0).vertex(hi, lo, lo, 1, 0).vertex(hi, lo, hi, 1, 1).vertex(lo, lo, hi, 0, 1);
        // up
        b.normal(0, 1, 0).vertex(lo, hi, hi, 0, 1).vertex(hi, hi, hi, 1, 1).vertex(hi, hi, lo, 1, 0).vertex(lo, hi, lo, 0, 0);
        // north
        b.normal(0, 0, -1).vertex(hi, hi, lo, 0, 0).vertex(hi, lo, lo, 0, 1).vertex(lo, lo, lo, 1, 1).vertex(lo, hi, lo, 1, 0);
        // south
        b.normal(0, 0, 1).vertex(lo, hi, hi, 0, 0).vertex(lo, lo, hi, 0, 1).vertex(hi, lo, hi, 1, 1).vertex(hi, hi, hi, 1, 0);
        // west
        b.normal(-1, 0, 0).vertex(lo, hi, lo, 0, 0).vertex(lo, lo, lo, 0, 1).vertex(lo, lo, hi, 1, 1).vertex(lo, hi, hi, 1, 0);
        // east
        b.normal(1, 0, 0).vertex(hi, hi, hi, 0, 0).vertex(hi, lo, hi, 0, 1).vertex(hi, lo, lo, 1, 1).vertex(hi, hi, lo, 1, 0);
        return b.build();
    }
}
