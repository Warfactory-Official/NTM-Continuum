package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.machine.TileEntityMachineMixer;
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

/** Animated OBJ renderer for the Mixer, including its translucent fluid volume. */
public final class MixerRenderer implements CompatBER<TileEntityMachineMixer, MixerRenderState> {

    private final WavefrontObj model;
    private final RenderType baseType;
    private final RenderType fluidType;

    public MixerRenderer() {
        Identifier obj = dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/mixer.obj");
        Identifier texture = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/mixer.png");
        model = WavefrontObj.load(obj);
        baseType = CompatRenderTypes.entityCutout(texture);
        // The legacy TESR disabled texturing for this part and supplied only the fluid colour.
        fluidType = CompatRenderTypes.entityTranslucent(
                dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/misc/white.png"), false);
    }

    @Override public MixerRenderState createRenderState() { return new MixerRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineMixer be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineMixer be, MixerRenderState state, float partialTicks, Vec3 cameraPosition) {
        state.rotation = be.prevRotation + (be.rotation - be.prevRotation) * partialTicks;
        int fill = 0;
        int capacity = 0;
        for (var tank : be.tanks) {
            if (tank.getTankType() != Fluids.NONE) {
                fill += tank.getFill();
                capacity += tank.getMaxFill();
            }
        }
        state.fillScale = capacity <= 0 ? 0F : (float) fill / capacity * 0.99F;
        state.fluidColor = be.tanks[2].getTankType().getColor();
    }

    @Override
    public void submit(MixerRenderState state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        part(collector, poseStack, "Main", light, baseType);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.rotation));
        part(collector, poseStack, "Mixer", light, baseType);
        poseStack.popPose();

        if (state.fillScale > 0F) {
            poseStack.pushPose();
            poseStack.translate(0F, 1F, 0F);
            poseStack.scale(1F, state.fillScale, 1F);
            poseStack.translate(0F, -1F, 0F);
            int color = 0xBF000000 | (state.fluidColor & 0xFFFFFF);
            collector.submitCustomGeometry(poseStack, fluidType,
                    (pose, buffer) -> model.renderPart("Fluid", pose, buffer, light, OverlayTexture.NO_OVERLAY, color));
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private void part(SubmitSink collector, PoseStack poseStack, String name, int light, RenderType type) {
        collector.submitCustomGeometry(poseStack, type,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
