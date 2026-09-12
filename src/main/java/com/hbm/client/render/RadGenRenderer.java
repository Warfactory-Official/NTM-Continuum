package com.hbm.client.render;

import com.hbm.util.GameTime;
import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineRadGen;
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

/** Legacy RadGen body, animated rotor, emissive indicator, and translucent glass shell. */
public final class RadGenRenderer implements CompatBER<TileEntityMachineRadGen, RadGenRenderState> {
    private static final int ON_LIGHT = 0xFF00FF00;
    private static final int OFF_LIGHT = 0xFF001A00;
    private static final int GLASS_TINT = 0x4D80BFFF;

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/radgen.obj"));
    private final RenderType bodyType = CompatRenderTypes.entityCutout(
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/rad_gen_body.png"));
    private final RenderType lightType = CompatRenderTypes.lightning();
    private final RenderType glassType = CompatRenderTypes.entityTranslucent(
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/rad_gen_body.png"));

    @Override
    public RadGenRenderState createRenderState() {
        return new RadGenRenderState();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public boolean shouldRender(TileEntityMachineRadGen blockEntity, Vec3 cameraPosition) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extract(TileEntityMachineRadGen blockEntity, RadGenRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState blockState = blockEntity.getBlockState();
        int meta = blockState.hasProperty(BlockDummyable.META)
                ? blockState.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 2 -> 90F;
            case 4 -> 180F;
            case 3 -> 270F;
            default -> 0F;
        };
        state.on = blockEntity.isOn;
        state.rotor = state.on ? -(GameTime.now() % 3600L) * 0.1F : 0F;
    }

    @Override
    public void submit(RadGenRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        sink.submitCustomGeometry(poseStack, bodyType,
                (pose, buffer) -> model.renderPart("Base", pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));

        poseStack.pushPose();
        poseStack.translate(0F, 1.5F, 0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.rotor));
        poseStack.translate(0F, -1.5F, 0F);
        sink.submitCustomGeometry(poseStack, bodyType,
                (pose, buffer) -> model.renderPart("Rotor", pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
        poseStack.popPose();

        int lightColor = state.on ? ON_LIGHT : OFF_LIGHT;
        sink.submitCustomGeometry(poseStack, lightType,
                (pose, buffer) -> model.renderPartColor("Light", pose, buffer, lightColor));

        SubmitSink glass = sink.order(1);
        glass.submitCustomGeometry(poseStack, glassType,
                (pose, buffer) -> model.renderPart("Glass", pose, buffer, state.lightCoords,
                        OverlayTexture.NO_OVERLAY, GLASS_TINT));
        glass.submitCustomGeometry(poseStack, bodyType,
                (pose, buffer) -> model.renderPart("Glass", pose, buffer, state.lightCoords,
                        OverlayTexture.NO_OVERLAY));
        poseStack.popPose();
    }
}
