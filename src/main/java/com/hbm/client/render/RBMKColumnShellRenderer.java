package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBase;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.Renderers;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Quads;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/** Runtime top geometry for normal/control/boiler columns; bodies stay in JSON to retain culling. */
public final class RBMKColumnShellRenderer<T extends BlockEntityRBMKBase> extends GemBER<T, RBMKColumnShellRenderer.State> {
    public enum Kind { PLAIN, BOILER }

    private static final float[] STUBS = {.0625F, .5625F};
    private static final float STUB = .375F, STUB_HEIGHT = .125F;

    private static final Geometry PIPE_SIDES = stubs(false);
    private static final Geometry PIPE_CAPS = stubs(true);

    private final String sheet;
    private final Kind kind;

    public RBMKColumnShellRenderer(String sheet, Kind kind) { this.sheet = sheet; this.kind = kind; }

    private static Geometry stubs(boolean caps) {
        Quads.Builder faces = Quads.named(Constants.MOD_ID + ":rbmk_pipe_" + (caps ? "cap" : "side"));
        for (float x : STUBS) for (float z : STUBS) {
            if (caps) RBMKColumnGeometry.blockCaps(faces, x, 0F, z, x + STUB, STUB_HEIGHT, z + STUB);
            else RBMKColumnGeometry.blockSides(faces, x, 0F, z, x + STUB, STUB_HEIGHT, z + STUB);
        }
        return faces.build();
    }

    /**
     * The lid and the boiler's pipe stubs, parked a whole column above the core and lit where they
     * render rather than where the block entity sits.
     */
    @Override
    protected void setup(Gem.Model<State> declaration) {
        Surface glassSide = skin("_glass_side"), glassTop = skin("_glass_top");
        Surface coverSide = skin("_cover_side"), coverTop = skin("_cover_top");

        Gem.Part<State> top = declaration.offScreen().viewDistance(256)
                .light(state -> state.topLight)
                .joint()
                .slide(Gem.Axes.Y, state -> state.height);

        top.child(RBMKColumnGeometry.LID_SIDES, coverSide)
                .visibleIf(state -> state.lid.present())
                .skin(state -> state.lid == RBMKBase.Lid.GLASS ? glassSide : coverSide);
        top.child(RBMKColumnGeometry.LID_CAPS, coverTop)
                .visibleIf(state -> state.lid.present())
                .skin(state -> state.lid == RBMKBase.Lid.GLASS ? glassTop : coverTop);

        if (kind == Kind.BOILER) {
            top.child(PIPE_SIDES, skin("_pipe_side")).visibleIf(state -> !state.lid.present());
            top.child(PIPE_CAPS, skin("_pipe_top")).visibleIf(state -> !state.lid.present());
        }
    }

    private Surface skin(String suffix) {
        return Surface.cutout(Ids.of(Constants.MOD_ID, "textures/blocks/rbmk/" + sheet + suffix + ".png").toString());
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extract(T be, State state, float partial, Vec3 camera) {
        // 1.7 draws these from the top dummy at y + 1, the block above the column
        state.height = RBMKDials.getColumnHeightRuleValue(be.getLevel());
        state.lid = RBMKBase.lidOf(be.getBlockState());
        state.topLight = be.getLevel() == null ? 0 : Renderers.lightAt(be.getLevel(), be.getBlockPos().above(state.height));
    }

    public static final class State extends CompatRenderState { int height; int topLight; RBMKBase.Lid lid = RBMKBase.Lid.NONE; }
}
