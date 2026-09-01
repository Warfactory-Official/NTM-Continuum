package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticReformer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The legacy catalytic reformer renderer. The normal model has no animated groups; its only conditional
 * client branch is the polaroid HorsePronter easter egg, which has no ported renderer seam.
 */
public final class CatalyticReformerRenderer
        implements CompatBER<TileEntityMachineCatalyticReformer, CatalyticReformerRenderState> {

    private final WavefrontObj model;
    private final RenderType renderType;

    public CatalyticReformerRenderer() {
        model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID,
                "models/machines/catalytic_reformer.obj"));
        renderType = CompatRenderTypes.entityCutoutNoCull(dev.golem.core.Ids.of(Constants.MOD_ID,
                "textures/models/machines/catalytic_reformer.png"));
    }

    @Override public CatalyticReformerRenderState createRenderState() { return new CatalyticReformerRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineCatalyticReformer be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineCatalyticReformer be, CatalyticReformerRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META)
                ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = 90F + switch (meta - BlockDummyable.offset) {
            case 4 -> 90F;   // WEST
            case 3 -> 180F;  // SOUTH
            case 5 -> 270F;  // EAST
            default -> 0F;   // NORTH
        };
    }

    @Override
    public void submit(CatalyticReformerRenderState state, PoseStack poseStack, SubmitSink sink,
                       CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        sink.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderAll(pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
        poseStack.popPose();
    }
}
