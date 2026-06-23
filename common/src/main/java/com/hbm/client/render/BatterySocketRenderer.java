package com.hbm.client.render;

import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.machine.ItemBatteryPack;
import com.hbm.tileentity.machine.storage.TileEntityBatterySocket;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class BatterySocketRenderer implements BlockEntityRenderer<TileEntityBatterySocket, BatterySocketRenderState> {

    private static final Identifier OBJ = Identifier.fromNamespaceAndPath("hbm", "models/battery_socket.obj");
    private static final Identifier SOCKET_TEX = Identifier.fromNamespaceAndPath("hbm", "textures/models/machines/battery_socket.png");

    private final WavefrontObj model = WavefrontObj.load(OBJ);
    private final RenderType socketType = RenderTypes.entityCutout(SOCKET_TEX);
    private final Map<Identifier, RenderType> bodyTypes = new HashMap<>();

    @Override
    public BatterySocketRenderState createRenderState() {
        return new BatterySocketRenderState();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public boolean shouldRender(TileEntityBatterySocket be, Vec3 cameraPosition) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(TileEntityBatterySocket be, BatterySocketRenderState state, float partialTicks,
                                   Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        BlockState bs = be.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 5;
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 2 -> 90F;
            case 4 -> 180F;
            case 3 -> 270F;
            default -> 0F;
        };

        ItemStack stack = be.syncStack;
        if (stack != null && stack.getItem() instanceof ItemBatteryPack battery) {
            state.batteryPart = battery.pack.isCapacitor() ? "Capacitor" : "Battery";
            state.batteryTexture = battery.pack.texture;
        } else {
            state.batteryPart = null;
            state.batteryTexture = null;
        }
    }

    @Override
    public void submit(BatterySocketRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();

        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.translate(-0.5F, 0F, 0.5F);

        collector.submitCustomGeometry(poseStack, socketType,
                (pose, buffer) -> model.renderPart("Socket", pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));

        if (state.batteryPart != null && state.batteryTexture != null) {
            RenderType bodyType = bodyTypes.computeIfAbsent(state.batteryTexture, RenderTypes::entityCutout);
            String part = state.batteryPart;
            collector.submitCustomGeometry(poseStack, bodyType,
                    (pose, buffer) -> model.renderPart(part, pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
        }
        poseStack.popPose();
    }
}
