package com.hbm.client.render.entity;

import com.hbm.entity.item.EntityFallingBlockNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatMovingBlock;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;

/**
 * Vanilla's falling-block renderer restated for the NTM variant: the carried block drawn as a moving block with
 * the world lighting of the column it is falling through. Vanilla also hides the block while it still overlaps
 * the identical block it started as, so the first tick does not draw double.
 */
public class RenderFallingBlockNT extends CompatEntityRenderer<EntityFallingBlockNT, RenderFallingBlockNT.State> {

    public RenderFallingBlockNT(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityFallingBlockNT entity, State state, float partialTicks) {
        BlockPos pos = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
        state.block.extract(entity.level(), entity.getBlockState(), pos, entity.getStartPos());
        state.hidden = entity.getBlockState() == entity.level().getBlockState(entity.blockPosition());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (state.hidden || state.block.isEmpty()) return;
        poseStack.pushPose();
        poseStack.translate(-0.5, 0.0, -0.5);
        sink.submitMovingBlock(poseStack, state.block);
        poseStack.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        final CompatMovingBlock block = new CompatMovingBlock();
        boolean hidden;
    }
}
