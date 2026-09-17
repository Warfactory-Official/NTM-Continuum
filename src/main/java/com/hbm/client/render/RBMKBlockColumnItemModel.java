package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Quads;
import dev.golem.core.render.gem.Surface;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import org.joml.Vector3fc;

/**
 * The inventory preview of a non-fuel RBMK column, ported from the old baked models' inventory quads:
 * four stacked casing sections ({@code RBMKColumnBakedModel}), optionally the four pipe stubs at the
 * top ({@code RBMKControlBakedModel.addPipes}), and optionally the sliding rod's Lid OBJ part parked at
 * y=3. Framing is identity here — {@code hbm:item/rbmk_column_base} carries the old
 * {@code BakedModelTransforms.rbmkColumn()} display verbatim, exactly as a baked model received it.
 */
public final class RBMKBlockColumnItemModel {

    private static final WavefrontObj RODS = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/rbmk_rods.obj"));
    private static final int SECTIONS = 4;
    // The inventory boiler/control shell ends at y=4; overlap the pipe bases by one eighth
    // so the GEM preview has no visible floating seam between the cap and pipe tops.
    private static final float PIPE_Y = 3.875F, LID_Y = 3F;
    private static final float[] STUBS = {.0625F, .5625F};
    private static final float STUB = .375F;
    private static final float STUB_HEIGHT = .125F;

    private final float scale, yaw, lift;
    private final ItemGem body;

    public RBMKBlockColumnItemModel(Identifier side, Identifier top, Identifier bottom,
                                    Identifier pipeSide, Identifier pipeTop, Identifier lidTexture,
                                    float scale, float yaw, float lift) {
        this.scale = scale;
        this.yaw = yaw;
        this.lift = lift;
        this.body = declare(RBMKColumnItemModel.skin(side), RBMKColumnItemModel.skin(top),
                pipeSide == null || pipeTop == null ? null : RBMKColumnItemModel.skin(pipeSide),
                pipeSide == null || pipeTop == null ? null : RBMKColumnItemModel.skin(pipeTop),
                lidTexture == null ? null : RBMKColumnItemModel.skin(lidTexture));
    }

    /**
     * Four casing sections, then the pipe stubs, then the lid.
     *
     * <p>The casing is one extent rather than four: the sections tile exactly, so the column's box is
     * recorded against the first of them, and the four stubs likewise share the box that encloses them.
     * The Lid OBJ is authored centred on the origin, like every old HBM part, while the casing here is in
     * block space — hence the same +0.5/+0.5 shift the world renderers bake in.
     */
    private static ItemGem declare(Surface side, Surface top, Surface pipeSide, Surface pipeTop, Surface lid) {
        ItemGem.Builder builder = ItemGem.builder();
        float[][] column = RBMKColumnGeometry.bounds(0F, 0F, 0F, 1F, SECTIONS, 1F);
        for (int section = 0; section < SECTIONS; section++) {
            builder.part(shell(section, false), side, section == 0 ? column : null);
            builder.part(shell(section, true), top, null);
        }
        if (pipeSide != null) {
            builder.part(stubs(false), pipeSide, RBMKColumnGeometry.bounds(STUBS[0], PIPE_Y, STUBS[0],
                    STUBS[1] + STUB, PIPE_Y + STUB_HEIGHT, STUBS[1] + STUB));
            builder.part(stubs(true), pipeTop, null);
        }
        if (lid != null) {
            builder.part(RODS.part("Lid"), lid, RODS.boundsIncluding(Set.of("Lid")))
                    .translate(.5F, LID_Y, .5F);
        }
        return builder.build();
    }

    private static Geometry shell(int section, boolean caps) {
        Quads.Builder faces = Quads.named(
                Constants.MOD_ID + ":rbmk_column_" + (caps ? "cap_" : "side_") + section);
        return (caps
                ? RBMKColumnGeometry.caps(faces, 0F, section, 0F, 1F, section + 1F, 1F)
                : RBMKColumnGeometry.sides(faces, 0F, section, 0F, 1F, section + 1F, 1F)).build();
    }

    private static Geometry stubs(boolean caps) {
        Quads.Builder faces = Quads.named(Constants.MOD_ID + ":rbmk_column_pipe_" + (caps ? "cap" : "side"));
        for (float x : STUBS) for (float z : STUBS) {
            if (caps) RBMKColumnGeometry.caps(faces, x, PIPE_Y, z, x + STUB, PIPE_Y + STUB_HEIGHT, z + STUB);
            else RBMKColumnGeometry.sides(faces, x, PIPE_Y, z, x + STUB, PIPE_Y + STUB_HEIGHT, z + STUB);
        }
        return faces.build();
    }

    public void submit(PoseStack pose, SubmitSink sink, int light, int overlay) {
        pose.pushPose();
        RBMKItemModelBounds.applyColumnTransform(pose, scale, yaw, lift);
        body.submit(pose, sink, light, overlay);
        pose.popPose();
    }

    public void extents(Consumer<Vector3fc> output) {
        PoseStack pose = new PoseStack();
        RBMKItemModelBounds.applyColumnTransform(pose, scale, yaw, lift);
        body.extents(pose, output);
    }
}
