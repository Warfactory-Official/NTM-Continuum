package com.hbm.client.render;

import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CameraInfo;
import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineCompressor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Compressor renderer — port of the 1.12.2 {@code RenderCompressor}. Needs a custom renderer (not the
 * generic {@code MachineObjRenderer}) because two parts are animated: the {@code Pump} translates by
 * {@code lift·3 − 3} (so it is <b>−3 blocks at rest</b> — a static render would drop it through the
 * floor) and the {@code Fan} spins about the X axis at y=1.5.
 *
 * <p>Old yaw map N90/S270/W180/E0 = the standard facing map (N0/S180/W90/E270) + 90.
 */
public class CompressorRenderer implements CompatBER<TileEntityMachineCompressor, CompressorRenderState> {

    private final WavefrontObj model;
    private final RenderType renderType;

    public CompressorRenderer() {
        this.model = WavefrontObj.load(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "models/machines/compressor.obj"));
        this.renderType = CompatRenderTypes.entityCutout(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/models/machines/compressor.png"));
    }

    @Override public CompressorRenderState createRenderState() { return new CompressorRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineCompressor be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineCompressor be, CompressorRenderState state, float partialTicks,
                                   Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = 90F + switch (ForgeDirection.getOrientation(meta - BlockDummyable.offset)) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F; // NORTH
        };
        state.lift = be.prevPiston + (be.piston - be.prevPiston) * partialTicks;
        state.fan = be.prevFanSpin + (be.fanSpin - be.prevFanSpin) * partialTicks;
    }

    @Override
    public void submit(CompressorRenderState s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        col.submitCustomGeometry(ps, renderType,
                (pose, buffer) -> model.renderPart("Compressor", pose, buffer, light, OverlayTexture.NO_OVERLAY));

        ps.pushPose();
        ps.translate(0F, s.lift * 3F - 3F, 0F);
        col.submitCustomGeometry(ps, renderType,
                (pose, buffer) -> model.renderPart("Pump", pose, buffer, light, OverlayTexture.NO_OVERLAY));
        ps.popPose();

        ps.pushPose();
        ps.translate(0F, 1.5F, 0F);
        ps.mulPose(Axis.XP.rotationDegrees(s.fan));
        ps.translate(0F, -1.5F, 0F);
        col.submitCustomGeometry(ps, renderType,
                (pose, buffer) -> model.renderPart("Fan", pose, buffer, light, OverlayTexture.NO_OVERLAY));
        ps.popPose();

        ps.popPose();
    }
}
