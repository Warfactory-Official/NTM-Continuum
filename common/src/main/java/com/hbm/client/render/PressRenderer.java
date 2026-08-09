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

import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * Renderer for the burner / electric press: a static <b>body</b> plus a <b>head</b> that descends with
 * the machine's {@code progress}, ported from the 1.12.2 {@code RenderPress}/{@code RenderEPress}. The
 * head travels {@code 0.875} blocks down over a full stroke (the original {@code progress*(1-0.125)/max}).
 *
 * <p>The two presses differ slightly: the burner press always faces one way (base yaw 180, no facing
 * rotation) with its head seated at +0.875 and scaled 0.95 in X/Z; the electric press rotates with its
 * placement (base yaw 90 + the standard facing map) with its head at +1.875. Both are expressed through
 * the constructor params. The floating pressed-item display above the head is deferred (see §18).
 */
public class PressRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, PressRenderState> {

    private final WavefrontObj body;
    private final WavefrontObj head;
    private final RenderType bodyType;
    private final RenderType headType;
    private final float baseYaw;
    private final boolean applyFacing;
    private final float headBaseOffset;
    private final float headScaleXZ;
    private final int maxProgress;
    private final ToIntFunction<T> progress;

    public PressRenderer(Identifier bodyObj, Identifier bodyTex, Identifier headObj, Identifier headTex,
                         float baseYaw, boolean applyFacing, float headBaseOffset, float headScaleXZ,
                         int maxProgress, ToIntFunction<T> progress) {
        this.body = WavefrontObj.load(bodyObj);
        this.head = WavefrontObj.load(headObj);
        this.bodyType = RenderTypes.entityCutout(bodyTex);
        this.headType = RenderTypes.entityCutout(headTex);
        this.baseYaw = baseYaw;
        this.applyFacing = applyFacing;
        this.headBaseOffset = headBaseOffset;
        this.headScaleXZ = headScaleXZ;
        this.maxProgress = maxProgress;
        this.progress = progress;
    }

    @Override public PressRenderState createRenderState() { return new PressRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(T be, Vec3 cam) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extractRenderState(T be, PressRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        if (applyFacing) {
            BlockState bs = be.getBlockState();
            int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
            state.yaw = switch (meta - BlockDummyable.offset) {
                case 4 -> 90F;   // WEST
                case 3 -> 180F;  // SOUTH
                case 5 -> 270F;  // EAST
                default -> 0F;   // NORTH
            };
        } else {
            state.yaw = 0F;
        }
        state.progress = progress.applyAsInt(be);
    }

    @Override
    public void submit(PressRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float yaw = baseYaw + state.yaw;

        // Body (static).
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        collector.submitCustomGeometry(poseStack, bodyType,
                (pose, buffer) -> body.renderExcept(pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY, Set.of()));
        poseStack.popPose();

        // Head (descends with progress).
        float travel = state.progress * 0.875F / maxProgress;
        poseStack.pushPose();
        poseStack.translate(0.5F, headBaseOffset - travel, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        if (headScaleXZ != 1.0F) poseStack.scale(headScaleXZ, 1.0F, headScaleXZ);
        collector.submitCustomGeometry(poseStack, headType,
                (pose, buffer) -> head.renderExcept(pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY, Set.of()));
        poseStack.popPose();
    }
}
