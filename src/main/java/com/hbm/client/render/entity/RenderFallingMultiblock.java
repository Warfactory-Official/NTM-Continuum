package com.hbm.client.render.entity;

import com.hbm.entity.item.EntityFallingMultiblock;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBlockEntityRenderState;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatMovingBlock;
import dev.golem.core.render.SubmitSink;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;

/**
 * Draws a falling multiblock as its member blocks, each submitted as a moving block at its rigid offset from the
 * entity anchor - the same mechanism as {@link RenderFallingBlockNT}, fanned out over the member list - plus the
 * carried core block entity through its own renderer.
 */
public class RenderFallingMultiblock extends CompatEntityRenderer<EntityFallingMultiblock, RenderFallingMultiblock.State> {

    public RenderFallingMultiblock(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** The structure reaches well past the entity's single-block collision box - same reason as RenderMeteor. */
    @Override
    public boolean culled(EntityFallingMultiblock entity) {
        return false;
    }

    @Override
    public void extract(EntityFallingMultiblock entity, State state, float partialTicks) {
        state.members.clear();
        state.offsets.clear();
        state.carried.clear();
        BlockPos origin = entity.originCore();
        BlockPos anchor = entity.blockPosition();
        for (long member : entity.membersPacked()) {
            BlockPos offset = new BlockPos(EntityFallingMultiblock.offsetXOf(member),
                    EntityFallingMultiblock.offsetYOf(member), EntityFallingMultiblock.offsetZOf(member));
            CompatMovingBlock moving = new CompatMovingBlock();
            moving.extract(entity.level(), EntityFallingMultiblock.stateOf(member), anchor.offset(offset),
                    origin.offset(offset));
            state.offsets.add(offset);
            state.members.add(moving);
        }
        state.carried.extract(entity.carriedBlockEntity(anchor), partialTicks);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        for (int i = 0; i < state.members.size(); i++) {
            CompatMovingBlock moving = state.members.get(i);
            if (moving.isEmpty()) continue;
            BlockPos offset = state.offsets.get(i);
            poseStack.pushPose();
            poseStack.translate(offset.getX() - 0.5, offset.getY(), offset.getZ() - 0.5);
            sink.submitMovingBlock(poseStack, moving);
            poseStack.popPose();
        }
        if (!state.carried.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(-0.5, 0.0, -0.5);
            sink.submitBlockEntity(poseStack, state.carried);
            poseStack.popPose();
        }
    }

    public static final class State extends CompatEntityRenderState {
        private final List<CompatMovingBlock> members = new ArrayList<>();
        private final List<BlockPos> offsets = new ArrayList<>();
        private final CompatBlockEntityRenderState carried = new CompatBlockEntityRenderState();
    }
}
