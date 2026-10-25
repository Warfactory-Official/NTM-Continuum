package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.tileentity.machine.TileEntitySoyuzStruct;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The Soyuz launch-complex ghost, 1.7 {@code RenderSoyuzMultiblock}: a small translucent cube in every
 * cell of the complex the {@link TileEntitySoyuzStruct} is waiting for: launcher plate for the apron,
 * outriggers and the two tower decks, concrete for the six legs, scaffold for the service tower and the
 * mast.
 *
 * <p>Unlike {@link StructPreviewRenderer}, every cell is drawn whether or not it is already built, as
 * upstream does: the complex is some five thousand cells, so testing each one per frame would cost more than
 * drawing it. The boxes are upstream's, box for box, in the same order and with the same three textures
 * (each block's top icon, {@code concrete} for {@code concrete_smooth}); the alpha is upstream's
 * {@code glColor4f(1, 1, 1, 0.75)}.
 *
 * <p>Upstream drew with the block's own light; the ghost is drawn full-bright like every other struct
 * preview in this port, so a complex being built at night stays readable.
 */
public final class SoyuzStructPreviewRenderer implements CompatBER<TileEntitySoyuzStruct, CompatRenderState> {

    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float ALPHA = 0.75F;

    private static final Identifier PLATE = tex("struct_launcher");
    private static final Identifier CONCRETE = tex("concrete");
    private static final Identifier SCAFFOLD = tex("struct_scaffold");

    private static final int[][] PLATE_BOXES = {
            {-6, 3, -6, 6, 4, 6}, {-1, 3, -8, 1, 4, -7}, {-2, 3, 7, 2, 4, 9},
            {-2, 51, 5, 2, 51, 9}, {-1, 38, -8, 1, 38, -6}};
    private static final int[][] CONCRETE_BOXES = {
            {3, 0, 3, 6, 2, 6}, {-6, 0, 3, -3, 2, 6}, {-6, 0, -6, -3, 2, -3}, {3, 0, -6, 6, 2, -3},
            {-1, 0, -8, 1, 2, -6}, {-2, 0, 5, 2, 2, 9}};
    private static final int[][] SCAFFOLD_BOXES = {{-1, 5, 6, 1, 50, 8}, {0, 5, -7, 0, 37, -7}};

    private static final float[] PLATE_CELLS = cells(PLATE_BOXES);
    private static final float[] CONCRETE_CELLS = cells(CONCRETE_BOXES);
    private static final float[] SCAFFOLD_CELLS = cells(SCAFFOLD_BOXES);

    @Override
    public CompatRenderState createRenderState() {
        return new CompatRenderState();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        // The complex reaches 51 blocks up and nine out from the seed.
        return true;
    }

    // No shouldRender override: viewDistance() is the only place the range is applied, and 1.7's
    // getMaxRenderDistanceSquared is 65536 (256 blocks).
    @Override
    public int viewDistance() {
        return 256;
    }

    @Override
    public void submit(CompatRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        draw(poseStack, sink, PLATE, PLATE_CELLS);
        draw(poseStack, sink, CONCRETE, CONCRETE_CELLS);
        draw(poseStack, sink, SCAFFOLD, SCAFFOLD_CELLS);
    }

    private static void draw(PoseStack poseStack, SubmitSink sink, Identifier texture, float[] cells) {
        // 1.7 enabled GL_CULL_FACE: back faces of the 0.75-alpha cubes are not drawn.
        sink.submitCustomGeometry(poseStack, CompatRenderTypes.entityTranslucentCull(texture), (pose, buffer) -> {
            for (int i = 0; i < cells.length; i += 3) {
                SmallBlockPronter.cube(pose, buffer, cells[i], cells[i + 1], cells[i + 2],
                        1F, 1F, 1F, ALPHA, FULL_BRIGHT);
            }
        });
    }

    private static float[] cells(int[][] boxes) {
        List<float[]> out = new ArrayList<>();
        for (int[] box : boxes) {
            for (int x = box[0]; x <= box[3]; x++) {
                for (int y = box[1]; y <= box[4]; y++) {
                    for (int z = box[2]; z <= box[5]; z++) out.add(new float[] {x, y, z});
                }
            }
        }
        float[] flat = new float[out.size() * 3];
        for (int i = 0; i < out.size(); i++) System.arraycopy(out.get(i), 0, flat, i * 3, 3);
        return flat;
    }

    private static Identifier tex(String name) {
        return Ids.of(Constants.MOD_ID, "textures/blocks/" + name + ".png");
    }
}
