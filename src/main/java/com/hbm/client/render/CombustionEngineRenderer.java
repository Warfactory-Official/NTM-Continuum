package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineCombustionEngine;
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

/** Animated legacy combustion-engine renderer with the exact model offset, hatch pivot and canister tint. */
public final class CombustionEngineRenderer
        implements CompatBER<TileEntityMachineCombustionEngine, CombustionEngineRenderState> {

    private final WavefrontObj model;
    private final RenderType renderType;

    public CombustionEngineRenderer() {
        model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID,
                "models/machines/combustion_engine.obj"));
        renderType = CompatRenderTypes.entityCutout(dev.golem.core.Ids.of(Constants.MOD_ID,
                "textures/models/machines/combustion_engine.png"));
    }

    @Override public CombustionEngineRenderState createRenderState() { return new CombustionEngineRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineCombustionEngine be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineCombustionEngine be, CombustionEngineRenderState state,
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
        state.doorAngle = be.prevDoorAngle + (be.doorAngle - be.prevDoorAngle) * partialTicks;

        Integer canister = canisterColor(be.tank.getTankType().getName());
        state.canisterTint = canister != null;
        state.canisterColor = canister == null ? 0xFFFFFFFF : legacyCanisterTint(canister);
    }

    @Override
    public void submit(CombustionEngineRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.translate(-0.5F, 0F, 3F);

        part(sink, poseStack, "Engine", light);
        if (state.canisterTint) {
            sink.submitCustomGeometry(poseStack, renderType,
                    (pose, buffer) -> model.renderPart("Canister", pose, buffer, light,
                            OverlayTexture.NO_OVERLAY, state.canisterColor));
        } else {
            part(sink, poseStack, "Canister", light);
        }

        poseStack.pushPose();
        poseStack.translate(1F, 0F, -2.6875F);
        poseStack.mulPose(Axis.YN.rotationDegrees(state.doorAngle));
        poseStack.translate(-1F, 0F, 2.6875F);
        part(sink, poseStack, "Hatch", light);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void part(SubmitSink sink, PoseStack poseStack, String name, int light) {
        sink.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }

    /** The legacy renderer tints with CD_Canister.color, not FluidType.color. */
    private static Integer canisterColor(String fluid) {
        return switch (fluid) {
            case "OIL", "CRACKOIL", "COALOIL", "OIL_DS", "CRACKOIL_DS" -> 0x424242;
            case "HEAVYOIL", "HEAVYOIL_VACUUM" -> 0x513F39;
            case "BITUMEN" -> 0x5A5877;
            case "SMEAR" -> 0x624F3B;
            case "HEATINGOIL", "HEATINGOIL_VACUUM" -> 0x694235;
            case "RECLAIMED" -> 0xF65723;
            case "PETROIL" -> 0x2369F6;
            case "LUBRICANT" -> 0xF1CC05;
            case "NAPHTHA", "NAPHTHA_CRACK", "NAPHTHA_DS" -> 0x5F6D44;
            case "DIESEL", "DIESEL_CRACK" -> 0xFF2C2C;
            case "LIGHTOIL", "LIGHTOIL_CRACK", "LIGHTOIL_DS", "LIGHTOIL_VACUUM" -> 0xB46B52;
            case "KEROSENE", "KEROSENE_REFORM" -> 0xFF377D;
            case "BIOFUEL" -> 0x9EB623;
            case "NITAN" -> 0x6B238C;
            case "GASOLINE" -> 0x2F7747;
            case "COALGAS" -> 0x2E155F;
            case "FRACKSOL" -> 0x4F887F;
            case "ETHANOL" -> 0xEAFFF3;
            case "PETROIL_LEADED" -> 0x2331F6;
            case "GASOLINE_LEADED" -> 0x2F775A;
            case "COALGAS_LEADED" -> 0x1E155F;
            case "WOODOIL" -> 0xBF7E4F;
            case "COALCREOSOTE" -> 0x285A3F;
            case "SEEDSLURRY" -> 0x7CC35E;
            case "SOLVENT" -> 0xE4E3EF;
            case "HYDRAZINE" -> 0x31517D;
            case "REFORMATE" -> 0xD180D6;
            case "XYLENE" -> 0xA380D6;
            case "DIESEL_REFORM", "DIESEL_CRACK_REFORM" -> 0xFFC500;
            default -> null;
        };
    }

    /** Convert the old {@code channel / 256F} tint to the closest 8-bit vertex colour. */
    private static int legacyCanisterTint(int rgb) {
        int r = Math.round(((rgb >> 16) & 0xFF) / 256F * 255F);
        int g = Math.round(((rgb >> 8) & 0xFF) / 256F * 255F);
        int b = Math.round((rgb & 0xFF) / 256F * 255F);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
