package com.hbm.client.render;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class MachineItemModelRenderer implements NoDataSpecialModelRenderer {

    public static final Identifier ID = Identifier.fromNamespaceAndPath("hbm", "machine_obj");

    private final WavefrontObj model;
    private final RenderType renderType;
    private final float scale;
    private final float yaw;
    private final float ty;
    private final Set<String> exclude;

    private final @Nullable RenderType overlayType;
    private final List<String> overlayParts;

    private MachineItemModelRenderer(WavefrontObj model, RenderType renderType, float scale, float yaw, float ty,
                                     Set<String> exclude, @Nullable RenderType overlayType, List<String> overlayParts) {
        this.model = model;
        this.renderType = renderType;
        this.scale = scale;
        this.yaw = yaw;
        this.ty = ty;
        this.overlayType = overlayType;
        this.overlayParts = overlayParts;

        if (overlayType != null && !overlayParts.isEmpty()) {
            Set<String> merged = new java.util.HashSet<>(exclude);
            merged.addAll(overlayParts);
            this.exclude = merged;
        } else {
            this.exclude = exclude;
        }
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        poseStack.pushPose();
        applyTransform(poseStack);
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderExcept(pose, buffer, lightCoords, overlayCoords, exclude));
        if (overlayType != null && !overlayParts.isEmpty()) {
            collector.submitCustomGeometry(poseStack, overlayType, (pose, buffer) -> {
                for (String part : overlayParts) model.renderPart(part, pose, buffer, lightCoords, overlayCoords);
            });
        }
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack ps = new PoseStack();
        applyTransform(ps);
        Matrix4f m = ps.last().pose();
        float[] mn = model.boundsMin();
        float[] mx = model.boundsMax();
        for (int xi = 0; xi < 2; xi++) for (int yi = 0; yi < 2; yi++) for (int zi = 0; zi < 2; zi++) {
            Vector3f v = new Vector3f(xi == 0 ? mn[0] : mx[0], yi == 0 ? mn[1] : mx[1], zi == 0 ? mn[2] : mx[2]);
            m.transformPosition(v);
            output.accept(v);
        }
    }

    private void applyTransform(PoseStack poseStack) {

        float[] mn = model.boundsMin();
        float[] mx = model.boundsMax();
        float cx = (mn[0] + mx[0]) * 0.5F;
        float cy = (mn[1] + mx[1]) * 0.5F;
        float cz = (mn[2] + mx[2]) * 0.5F;
        poseStack.translate(0.5F, 0.5F + ty, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-cx, -cy, -cz);
    }

    public record Unbaked(Identifier obj, Identifier texture, float scale, float yaw, float ty, List<String> exclude,
                          Optional<Identifier> overlayTexture, List<String> overlayParts) implements NoDataSpecialModelRenderer.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("obj").forGetter(Unbaked::obj),
                Identifier.CODEC.fieldOf("texture").forGetter(Unbaked::texture),
                Codec.FLOAT.optionalFieldOf("scale", 0.28F).forGetter(Unbaked::scale),
                Codec.FLOAT.optionalFieldOf("yaw", 90.0F).forGetter(Unbaked::yaw),
                Codec.FLOAT.optionalFieldOf("ty", 0.0F).forGetter(Unbaked::ty),
                Codec.STRING.listOf().optionalFieldOf("exclude", List.of()).forGetter(Unbaked::exclude),
                Identifier.CODEC.optionalFieldOf("overlayTexture").forGetter(Unbaked::overlayTexture),
                Codec.STRING.listOf().optionalFieldOf("overlayParts", List.of()).forGetter(Unbaked::overlayParts)
        ).apply(i, Unbaked::new));

        @Override
        public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) {
            RenderType overlay = overlayTexture.map(RenderTypes::entityCutout).orElse(null);
            return new MachineItemModelRenderer(WavefrontObj.load(obj), RenderTypes.entityCutout(texture), scale, yaw, ty,
                    Set.copyOf(exclude), overlay, List.copyOf(overlayParts));
        }
    }
}
