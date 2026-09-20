package com.hbm.client.render;

import com.hbm.blocks.network.FluidDuctGauge;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Quads;
import dev.golem.core.render.gem.Surface;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The second render pass of the 1.7 coated cube ducts ({@code IBlockMultiPass}): a full-cube overlay
 * drawn a hair outside the block model. For the paintable duct it is the {@code _color} port overlay
 * tinted by the carried fluid; for the gauge it is the untinted {@code _overlay} port on five faces and
 * the {@code pipe_gauge} dial on the face the block was placed from.
 */
public class CubeDuctOverlayRenderer<T extends TileEntityFluidDuctNT> extends GemBER<T, CubeDuctOverlayRenderer.State> {

    public static class State extends CompatRenderState {
        public int color = 0xFFFFFFFF;
        public int gaugeFace = -1;
    }

    private static final Surface COLOR = Surface.cutoutNoCull("hbm:textures/blocks/fluid_duct_paintable_color.png");
    private static final Surface OVERLAY = Surface.cutoutNoCull("hbm:textures/blocks/fluid_duct_paintable_overlay.png");
    private static final Surface GAUGE = Surface.cutoutNoCull("hbm:textures/blocks/pipe_gauge.png");
    private static final Geometry[] FACES = faces();

    private final boolean gauge;

    public CubeDuctOverlayRenderer(boolean gauge) {
        this.gauge = gauge;
    }

    private static Geometry[] faces() {
        float e = 0.002F, x0 = -e, x1 = 1 + e;
        Geometry[] f = new Geometry[6];
        f[0] = Quads.named("cube_overlay_d").normal(0, -1, 0).vertex(x0, x0, x0, 0, 0).vertex(x1, x0, x0, 1, 0).vertex(x1, x0, x1, 1, 1).vertex(x0, x0, x1, 0, 1).build();
        f[1] = Quads.named("cube_overlay_u").normal(0, 1, 0).vertex(x0, x1, x0, 0, 0).vertex(x0, x1, x1, 0, 1).vertex(x1, x1, x1, 1, 1).vertex(x1, x1, x0, 1, 0).build();
        f[2] = Quads.named("cube_overlay_n").normal(0, 0, -1).vertex(x1, x1, x0, 0, 0).vertex(x1, x0, x0, 0, 1).vertex(x0, x0, x0, 1, 1).vertex(x0, x1, x0, 1, 0).build();
        f[3] = Quads.named("cube_overlay_s").normal(0, 0, 1).vertex(x0, x1, x1, 0, 0).vertex(x0, x0, x1, 0, 1).vertex(x1, x0, x1, 1, 1).vertex(x1, x1, x1, 1, 0).build();
        f[4] = Quads.named("cube_overlay_w").normal(-1, 0, 0).vertex(x0, x1, x0, 0, 0).vertex(x0, x0, x0, 0, 1).vertex(x0, x0, x1, 1, 1).vertex(x0, x1, x1, 1, 0).build();
        f[5] = Quads.named("cube_overlay_e").normal(1, 0, 0).vertex(x1, x1, x1, 0, 0).vertex(x1, x0, x1, 0, 1).vertex(x1, x0, x0, 1, 1).vertex(x1, x1, x0, 1, 0).build();
        return f;
    }

    @Override
    protected void setup(Gem.Model<State> declaration) {
        for (int face = 0; face < 6; face++) {
            final int f = face;
            Gem.Part<State> part = declaration.part(FACES[f], gauge ? OVERLAY : COLOR);
            if (gauge) part.skin(state -> state.gaugeFace == f ? GAUGE : OVERLAY);
            else part.tint(state -> state.color);
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(T be, State state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        // 1.7 colorMultiplier: the carried type's colour, NONE (0x888888) included.
        state.color = 0xFF000000 | (be.getFluidType().getColor() & 0xFFFFFF);
        state.gaugeFace = bs.hasProperty(FluidDuctGauge.GAUGE) ? bs.getValue(FluidDuctGauge.GAUGE).ordinal() : -1;
        if (state.gaugeFace >= 0 && state.gaugeFace >= Direction.values().length) state.gaugeFace = -1;
    }
}
