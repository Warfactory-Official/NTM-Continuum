package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.storage.TileEntityMachineFENSU;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;

/** OBJ renderer for the colored FEnSU body, disc, and fullbright indicator lights. */
public final class FensuRenderer implements CompatBER<TileEntityMachineFENSU, FensuRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/fensu.obj"));
    private final Map<DyeColor, RenderType> types = new EnumMap<>(DyeColor.class);

    @Override public FensuRenderState createRenderState() { return new FensuRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineFENSU be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineFENSU be, FensuRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = blockState.hasProperty(BlockDummyable.META)
                ? blockState.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 2 -> 90F;   // NORTH
            case 4 -> 180F;  // WEST
            case 3 -> 270F;  // SOUTH
            default -> 0F;   // EAST
        };
        state.rotation = be.prevRotation + (be.rotation - be.prevRotation) * partialTicks;
        state.color = be.color;
    }

    @Override
    public void submit(FensuRenderState state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        RenderType type = types.computeIfAbsent(state.color, FensuRenderer::typeFor);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        part(collector, poseStack, type, "Base", state.lightCoords);

        poseStack.pushPose();
        poseStack.translate(0F, 2.5F, 0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.rotation));
        poseStack.translate(0F, -2.5F, 0F);
        part(collector, poseStack, type, "Disc", state.lightCoords);
        poseStack.popPose();

        // The old TESR disabled lighting and set both lightmap coordinates to 240 for this part.
        part(collector, poseStack, type, "Lights", FoundryRenderHelper.FULLBRIGHT);
        poseStack.popPose();
    }

    private void part(SubmitSink collector, PoseStack poseStack, RenderType type, String name, int light) {
        collector.submitCustomGeometry(poseStack, type,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }

    private static RenderType typeFor(DyeColor color) {
        Identifier texture = dev.golem.core.Ids.of(Constants.MOD_ID,
                "textures/models/machines/fensus/fensu_" + color.getName() + ".png");
        return CompatRenderTypes.entityCutout(texture);
    }
}
