package com.hbm.client.render;

import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.CameraInfo;
import com.hbm.blocks.bomb.NukeMan;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a HBM bomb's static OBJ model, replacing the 1.12.2 {@code StaticTesrBakedModels} entry. Like
 * {@link MachineObjRenderer} but keyed on the bomb block's {@link NukeMan#FACING} property (2–5) with
 * the Fat Man's own yaw map (the old {@code yawMap}: 2→270°, 3→90°, 4→0°, 5→180°).
 */
public class BombObjRenderer<T extends BlockEntity> implements CompatBER<T, MachineRenderState> {

    private final WavefrontObj model;
    private final RenderType renderType;
    private final float scale;

    public BombObjRenderer(Identifier obj, Identifier texture, float scale) {
        this.model = WavefrontObj.load(obj);
        this.renderType = CompatRenderTypes.entityCutout(texture);
        this.scale = scale;
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
    public void extract(T blockEntity, MachineRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = blockEntity.getBlockState();
        int facing = bs.hasProperty(NukeMan.FACING) ? bs.getValue(NukeMan.FACING) : 2;
        state.yaw = switch (facing) {
            case 3 -> 90F;
            case 5 -> 180F;
            case 4 -> 0F;
            default -> 270F; // facing 2
        };
    }

    @Override
    public void submit(MachineRenderState state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        if (scale != 1.0F) poseStack.scale(scale, scale, scale);
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderAll(pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
        poseStack.popPose();
    }
}
