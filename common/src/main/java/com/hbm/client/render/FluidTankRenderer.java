package com.hbm.client.render;

import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FluidTankRenderer implements BlockEntityRenderer<TileEntityMachineFluidTank, FluidTankRenderState> {

    private static final Identifier OBJ = Identifier.fromNamespaceAndPath("hbm", "models/fluidtank.obj");
    private static final Identifier FRAME_TEX = Identifier.fromNamespaceAndPath("hbm", "textures/models/machines/tank.png");
    private static final Identifier TANK_NONE = Identifier.fromNamespaceAndPath("hbm", "textures/models/tank/tank_none.png");
    private static final float BASE_YAW = 180F;

    private final WavefrontObj model = WavefrontObj.load(OBJ);
    private final RenderType frameType = RenderTypes.entityCutout(FRAME_TEX);
    private final Map<Identifier, RenderType> bodyTypes = new HashMap<>();

    private static final Map<String, Identifier> BODY_TEX = new HashMap<>();

    private static Identifier bodyTextureFor(String fluidName) {
        return BODY_TEX.computeIfAbsent(fluidName.toLowerCase(Locale.US), name -> {
            Identifier tex = Identifier.fromNamespaceAndPath("hbm", "textures/models/tank/tank_" + name + ".png");
            return net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(tex).isPresent() ? tex : TANK_NONE;
        });
    }

    @Override
    public FluidTankRenderState createRenderState() {
        return new FluidTankRenderState();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public boolean shouldRender(TileEntityMachineFluidTank be, Vec3 cameraPosition) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(TileEntityMachineFluidTank be, FluidTankRenderState state, float partialTicks,
                                   Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        BlockState bs = be.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
        state.bodyTexture = bodyTextureFor(be.tank.getTankType().getName());
    }

    @Override
    public void submit(FluidTankRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        RenderType bodyType = bodyTypes.computeIfAbsent(state.bodyTexture, RenderTypes::entityCutout);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(BASE_YAW + state.yaw));
        collector.submitCustomGeometry(poseStack, frameType,
                (pose, buffer) -> model.renderPart("Frame", pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
        collector.submitCustomGeometry(poseStack, bodyType,
                (pose, buffer) -> model.renderPart("Tank", pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
        poseStack.popPose();
    }
}
