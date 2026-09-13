package com.hbm.client.render;

import com.hbm.client.model.WavefrontObj;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Quads;
import dev.golem.core.render.gem.Surface;

import java.util.Map;
import java.util.Set;

/**
 * The Redstone-over-Radio gauge's inventory icon: the panel slab plus the four dials, needles parked.
 *
 * <p>Neither half of the placed block can stand in for the other. The slab is a block model and belongs
 * to no OBJ; the dials are {@code gauge.obj} parts the block-entity renderer adds and belong to no block
 * model. The old icon drew both, which is why this states both.
 *
 * <p>The panel faces south here rather than east. That is the orientation the old inventory render
 * produced — its base turned the slab a quarter turn one way and the gauge pass turned the dials the
 * other — and it is a quarter turn from where the placed block at {@code facing=east} holds them.
 *
 * <p>The slab carries the {@code Panel} surface, which is not an OBJ part name: the item definition has
 * one texture field and this icon needs three, so the panel's sheet rides in with the needle's.
 */
final class RBMKGaugeIcon {

    /** The name the item definition's {@code partTextures} gives the slab's sheet. */
    static final String PANEL = "Panel";

    /** The old icon drew the needle with texturing off under {@code glColor3f(0.5F, 0F, 0F)}. */
    private static final int NEEDLE_TINT = 0xFF800000;
    /** Every needle rests at the bottom of its scale, which is the placed gauge's angle of zero. */
    private static final float NEEDLE_REST = 85F;
    /** A quarter turn, putting the dials on the face the old inventory render showed. */
    private static final float ICON_YAW = -90F;
    private static final float PANEL_DEPTH = .75F;

    private static final Geometry SLAB =
            box(Quads.named("hbm:rbmk_gauge_panel"), 0F, 0F, 0F, PANEL_DEPTH, 1F, 1F).build();

    private RBMKGaugeIcon() {}

    /** The placed renderer's own tree, with the needles fixed and the block model's slab added under it. */
    static void declare(ItemGem.Builder builder, WavefrontObj model, Surface dialSurface,
                        Map<String, Surface> partSurfaces) {
        Surface panel = partSurfaces.get(PANEL);
        Surface needle = partSurfaces.get("Needle");
        Gem.Part<ItemPose> facing = builder.joint()
                .translate(.5F, 0F, .5F)
                .rotate(Gem.Axes.Y, ICON_YAW);

        builder.child(facing, SLAB, panel, new float[][] {{0F, 0F, 0F}, {PANEL_DEPTH, 1F, 1F}})
                .translate(-.5F, 0F, -.5F);

        for (int i = 0; i < 4; i++) {
            Gem.Part<ItemPose> dial = facing.joint()
                    .translate(.25F, (i / 2) * -.5F + .25F, (i % 2) * -.5F + .25F);
            builder.child(dial, model.part("Gauge"), dialSurface, model.boundsIncluding(Set.of("Gauge")));
            builder.child(dial, model.part("Needle"), needle, null)
                    .tint(state -> NEEDLE_TINT)
                    .translate(0F, .4375F, -.125F)
                    .rotate(Gem.Axes.X, NEEDLE_REST)
                    .translate(0F, -.4375F, .125F);
        }
    }

    /**
     * A cuboid wearing one texture, with the UVs vanilla gives an element of these bounds: a face
     * narrower than a block shows that much of the sheet rather than the whole of it squeezed.
     */
    private static Quads.Builder box(Quads.Builder faces, float x0, float y0, float z0,
                                     float x1, float y1, float z1) {
        faces.normal(0F, 0F, -1F)
                .vertex(x1, y0, z0, 1F - x1, 1F - y0)
                .vertex(x0, y0, z0, 1F - x0, 1F - y0)
                .vertex(x0, y1, z0, 1F - x0, 1F - y1)
                .vertex(x1, y1, z0, 1F - x1, 1F - y1);
        faces.normal(0F, 0F, 1F)
                .vertex(x0, y0, z1, x0, 1F - y0)
                .vertex(x1, y0, z1, x1, 1F - y0)
                .vertex(x1, y1, z1, x1, 1F - y1)
                .vertex(x0, y1, z1, x0, 1F - y1);
        faces.normal(-1F, 0F, 0F)
                .vertex(x0, y0, z0, z0, 1F - y0)
                .vertex(x0, y0, z1, z1, 1F - y0)
                .vertex(x0, y1, z1, z1, 1F - y1)
                .vertex(x0, y1, z0, z0, 1F - y1);
        faces.normal(1F, 0F, 0F)
                .vertex(x1, y0, z1, 1F - z1, 1F - y0)
                .vertex(x1, y0, z0, 1F - z0, 1F - y0)
                .vertex(x1, y1, z0, 1F - z0, 1F - y1)
                .vertex(x1, y1, z1, 1F - z1, 1F - y1);
        faces.normal(0F, 1F, 0F)
                .vertex(x0, y1, z0, x0, z0)
                .vertex(x1, y1, z0, x1, z0)
                .vertex(x1, y1, z1, x1, z1)
                .vertex(x0, y1, z1, x0, z1);
        faces.normal(0F, -1F, 0F)
                .vertex(x1, y0, z1, x1, 1F - z1)
                .vertex(x1, y0, z0, x1, 1F - z0)
                .vertex(x0, y0, z0, x0, 1F - z0)
                .vertex(x0, y0, z1, x0, 1F - z1);
        return faces;
    }
}
