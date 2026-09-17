package com.hbm.client.render;

import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * A declared item body, together with the boxes its icon is framed on. Drawing and framing are the
 * same walk over the same declaration, which is what they had drifted apart from — the jetpack
 * chestplate was framed without the offset it was drawn with.
 *
 * <p>Not every part carries a box. Where a model makes several draws its framing has always counted as
 * one extent — every socket of a part-textured machine, every material of a multi-texture one — the
 * box is recorded against the first and the rest hold none.
 */
final class ItemGem {

    private final Gem.Model<ItemPose> declaration;
    private final Map<Gem.Part<ItemPose>, float[][]> boxes;
    private final ItemPose pose = new ItemPose();

    private ItemGem(Gem.Model<ItemPose> declaration, Map<Gem.Part<ItemPose>, float[][]> boxes) {
        this.declaration = declaration;
        this.boxes = boxes;
    }

    Gem.Model<ItemPose> model() {
        return declaration;
    }

    void submit(PoseStack poseStack, SubmitSink sink, int light, int overlay) {
        pose.animation = clock();
        pose.overlay = overlay;
        Gem.replay(declaration, pose, poseStack, sink, light);
    }

    /** The transformed corners of every framed part, at one animation phase, in the caller's frame. */
    void extents(PoseStack framed, float animation, Consumer<Vector3fc> output) {
        pose.animation = animation;
        Matrix4f start = new Matrix4f(framed.last().pose());
        for (Gem.Part<ItemPose> root : declaration.roots()) corners(root, start, output);
    }

    private void corners(Gem.Part<ItemPose> part, Matrix4f parent, Consumer<Vector3fc> output) {
        if (!part.visible(pose)) return;
        Matrix4f here = new Matrix4f(parent);
        part.applyTo(here, pose);
        float[][] box = boxes.get(part);
        if (box != null) {
            float[] min = box[0];
            float[] max = box[1];
            for (int xi = 0; xi < 2; xi++) for (int yi = 0; yi < 2; yi++) for (int zi = 0; zi < 2; zi++) {
                output.accept(here.transformPosition(new Vector3f(
                        xi == 0 ? min[0] : max[0], yi == 0 ? min[1] : max[1], zi == 0 ? min[2] : max[2])));
            }
        }
        for (Gem.Part<ItemPose> child : part.children()) corners(child, here, output);
    }

    static float clock() {
        return GameTime.now() % 3600L * 0.1F;
    }

    static Builder builder() {
        return new Builder();
    }

    static final class Builder {

        private final Gem.Model<ItemPose> declaration = Gem.model();
        private final Map<Gem.Part<ItemPose>, float[][]> boxes = new IdentityHashMap<>();

        private Builder() {
            declaration.overlay(state -> state.overlay);
        }

        Gem.Part<ItemPose> part(Geometry geometry, Surface surface, float @Nullable [][] box) {
            Gem.Part<ItemPose> declared = declaration.part(geometry, surface);
            if (box != null) boxes.put(declared, box);
            return declared;
        }

        /** A transform-only root, for an icon whose parts hang off a chain rather than sitting flat. */
        Gem.Part<ItemPose> joint() {
            return declaration.joint();
        }

        Gem.Part<ItemPose> child(Gem.Part<ItemPose> parent, Geometry geometry, Surface surface,
                                 float @Nullable [][] box) {
            Gem.Part<ItemPose> declared = parent.child(geometry, surface);
            if (box != null) boxes.put(declared, box);
            return declared;
        }

        /** Records a box against a part declared earlier, for a group of draws framed as one. */
        void frame(Gem.Part<ItemPose> part, float @Nullable [][] box) {
            if (box != null) boxes.put(part, box);
        }

        ItemGem build() {
            declaration.seal();
            return new ItemGem(declaration, boxes);
        }
    }
}
