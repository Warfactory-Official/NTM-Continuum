package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityFurnaceIron;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Iron furnace renderer: the old static body plus separate lit/unlit model parts. */
public final class FurnaceIronRenderer implements CompatBER<TileEntityFurnaceIron, FurnaceRenderState> {
    private static final Identifier OBJ = dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/furnace_iron.obj");
    private static final Identifier TEXTURE = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/furnace_iron.png");
    private final WavefrontObj model = WavefrontObj.load(OBJ);
    private final RenderType type = CompatRenderTypes.entityCutout(TEXTURE);

    @Override public FurnaceRenderState createRenderState() { return new FurnaceRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityFurnaceIron be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override public void extract(TileEntityFurnaceIron furnace, FurnaceRenderState state, float partialTicks, Vec3 cameraPosition) {
        int facing = furnace.getBlockState().getValue(BlockDummyable.META) - BlockDummyable.offset;
        state.yaw = switch (facing) {
            case 3 -> 0F;
            case 5 -> 90F;
            case 2 -> 180F;
            case 4 -> 270F;
            default -> 0F;
        };
        state.burning = furnace.wasOn;
    }

    @Override public void submit(FurnaceRenderState state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        // The legacy renderer's model origin is half a block beyond the core cell.
        poseStack.translate(-0.5F, 0F, -0.5F);
        collector.submitCustomGeometry(poseStack, type,
                (pose, buffer) -> model.renderPart("Main", pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
        collector.submitCustomGeometry(poseStack, type,
                (pose, buffer) -> model.renderPart(state.burning ? "On" : "Off", pose, buffer,
                        state.burning ? 0x00F000F0 : state.lightCoords, OverlayTexture.NO_OVERLAY));
        poseStack.popPose();
    }
}
