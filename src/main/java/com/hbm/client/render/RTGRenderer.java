package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.energymk2.IEnergyConnectorMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Legacy RTG body plus the directional connector geometry from RenderRTG. */
public final class RTGRenderer<T extends TileEntityLoadedBase> implements CompatBER<T, RTGRenderState> {
    private final WavefrontObj body = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/rtg.obj"));
    private final WavefrontObj connector = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/rtg_connector.obj"));
    private final RenderType cellType = CompatRenderTypes.entityCutoutNoCull(
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/rtg_cell.png"));
    private final RenderType poloniumType = CompatRenderTypes.entityCutoutNoCull(
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/rtg_polonium.png"));
    private final RenderType greyType = CompatRenderTypes.entityCutoutNoCull(
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/rtg.png"));

    @Override
    public RTGRenderState createRenderState() {
        return new RTGRenderState();
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
    public void extract(T blockEntity, RTGRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        state.polonium = blockEntity.getBlockState().is(ModMachineBlocks.rtg_polonium.get());
        state.grey = blockEntity.getBlockState().is(ModMachineBlocks.machine_rtg_grey.get());
        Level level = blockEntity.getLevel();
        if (level == null) return;

        BlockPos pos = blockEntity.getBlockPos();
        state.positiveX = canConnect(level, pos, Direction.EAST);
        state.negativeX = canConnect(level, pos, Direction.WEST);
        state.negativeZ = canConnect(level, pos, Direction.NORTH);
        state.positiveZ = canConnect(level, pos, Direction.SOUTH);
    }

    @Override
    public void submit(RTGRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        RenderType type = state.grey ? greyType : state.polonium ? poloniumType : cellType;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180F));
        sink.submitCustomGeometry(poseStack, type,
                (pose, buffer) -> body.renderPart("Gen", pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));

        connector(state.positiveX, 0F, poseStack, sink, type, state);
        connector(state.negativeX, 180F, poseStack, sink, type, state);
        connector(state.negativeZ, 90F, poseStack, sink, type, state);
        connector(state.positiveZ, -90F, poseStack, sink, type, state);
        poseStack.popPose();
    }

    private void connector(boolean present, float yaw, PoseStack poseStack, SubmitSink sink, RenderType type,
                           RTGRenderState state) {
        if (!present) return;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        sink.submitCustomGeometry(poseStack, type,
                (pose, buffer) -> connector.renderAll(pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
        poseStack.popPose();
    }

    /** Mirrors Library.canConnect: block-level connector first, then tile-level connector. */
    private static boolean canConnect(Level level, BlockPos pos, Direction direction) {
        BlockPos neighbour = pos.relative(direction);
        ForgeDirection neighbourSide = ForgeDirection.getOrientation(direction.getOpposite());
        BlockState neighbourState = level.getBlockState(neighbour);
        if (neighbourState.getBlock() instanceof IEnergyConnectorBlock connectorBlock
                && connectorBlock.canConnect(level, neighbour, neighbourSide)) {
            return true;
        }
        return level.getBlockEntity(neighbour) instanceof IEnergyConnectorMK2 connector
                && connector.canConnect(neighbourSide);
    }
}
