package com.hbm.client.render;

import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MachineObjRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, MachineRenderState> {

    public record ModelDef(Identifier obj, Identifier texture, float yOffset, Set<String> exclude) {
        public ModelDef(Identifier obj, Identifier texture) { this(obj, texture, 0F, Set.of()); }
        public ModelDef(Identifier obj, Identifier texture, float yOffset) { this(obj, texture, yOffset, Set.of()); }
        public ModelDef(Identifier obj, Identifier texture, Set<String> exclude) { this(obj, texture, 0F, exclude); }
    }

    private record Part(WavefrontObj model, RenderType renderType, float yOffset, Set<String> exclude) {}

    private final List<Part> parts;
    private final float baseYaw;
    private final float scale;
    private final Set<String> frameParts;
    private final int frameHeight;

    public MachineObjRenderer(float baseYaw, float scale, ModelDef... defs) {
        this(baseYaw, scale, Set.of(), 0, defs);
    }

    public MachineObjRenderer(float baseYaw, float scale, Set<String> frameParts, int frameHeight, ModelDef... defs) {
        this.baseYaw = baseYaw;
        this.scale = scale;
        this.frameParts = frameParts;
        this.frameHeight = frameHeight;
        List<Part> built = new ArrayList<>();
        for (ModelDef d : defs) {

            built.add(new Part(WavefrontObj.load(d.obj()), RenderTypes.entityCutout(d.texture()), d.yOffset(), d.exclude()));
        }
        this.parts = built;
    }

    @Override
    public MachineRenderState createRenderState() {
        return new MachineRenderState();
    }

    @Override
    public boolean shouldRenderOffScreen() {

        return true;
    }

    @Override
    public boolean shouldRender(T blockEntity, Vec3 cameraPosition) {

        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(T blockEntity, MachineRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        BlockState bs = blockEntity.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;

        state.yaw = switch (meta - BlockDummyable.offset) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };

        state.showFrame = frameParts.isEmpty() || (blockEntity.getLevel() != null
                && !blockEntity.getLevel().getBlockState(blockEntity.getBlockPos().above(frameHeight)).isAir());
    }

    @Override
    public void submit(MachineRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {

        Set<String> hidden = state.showFrame ? Set.of() : frameParts;
        for (Part part : parts) {
            Set<String> exclude = hidden.isEmpty() ? part.exclude()
                    : union(part.exclude(), hidden);
            poseStack.pushPose();
            poseStack.translate(0.5F, part.yOffset(), 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(baseYaw + state.yaw));
            if (scale != 1.0F) poseStack.scale(scale, scale, scale);
            collector.submitCustomGeometry(poseStack, part.renderType(),
                    (pose, buffer) -> part.model().renderExcept(pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY, exclude));
            poseStack.popPose();
        }
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        if (a.isEmpty()) return b;
        java.util.HashSet<String> s = new java.util.HashSet<>(a);
        s.addAll(b);
        return s;
    }
}
