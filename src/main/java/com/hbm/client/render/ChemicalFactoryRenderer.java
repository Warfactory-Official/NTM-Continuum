package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineChemicalFactory;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** OBJ renderer for the legacy two-fan Chemical Factory. */
public final class ChemicalFactoryRenderer
        implements CompatBER<TileEntityMachineChemicalFactory, ChemicalFactoryRenderState> {

    private static final float BASE_YAW = 90F;
    private final WavefrontObj model;
    private final RenderType renderType;

    public ChemicalFactoryRenderer() {
        Identifier obj = dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/chemical_factory.obj");
        Identifier texture = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/chemical_factory.png");
        model = WavefrontObj.load(obj);
        renderType = CompatRenderTypes.entityCutout(texture);
    }

    @Override public ChemicalFactoryRenderState createRenderState() { return new ChemicalFactoryRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineChemicalFactory be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineChemicalFactory be, ChemicalFactoryRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = blockState.hasProperty(BlockDummyable.META)
                ? blockState.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
        state.showFrame = be.frame;
        state.anim = be.prevAnim + (be.anim - be.prevAnim) * partialTicks;
    }

    @Override
    public void submit(ChemicalFactoryRenderState state, PoseStack poseStack, SubmitSink collector,
                       CameraInfo camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(BASE_YAW + state.yaw));

        part(collector, poseStack, "Base", light);
        if (state.showFrame) part(collector, poseStack, "Frame", light);

        poseStack.pushPose();
        poseStack.translate(1F, 0F, 0F);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) ((-state.anim * 45D) % 360D)));
        poseStack.translate(-1F, 0F, 0F);
        part(collector, poseStack, "Fan1", light);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(-1F, 0F, 0F);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) ((-state.anim * 45D) % 360D)));
        poseStack.translate(1F, 0F, 0F);
        part(collector, poseStack, "Fan2", light);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void part(SubmitSink collector, PoseStack poseStack, String name, int light) {
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
