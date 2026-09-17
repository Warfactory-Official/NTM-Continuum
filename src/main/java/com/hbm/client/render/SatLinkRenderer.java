package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineSatLink;
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

/**
 * The sat link dish: a fixed base, a rotor that swings round, and a dish that tips up when the machine has
 * something to talk to. The dish pivots about the point 7.375 up the mast, which is where the model's own
 * hinge is.
 */
public class SatLinkRenderer implements CompatBER<TileEntityMachineSatLink, SatLinkRenderState> {

    private static final float PIVOT = 7.375F;

    private final WavefrontObj model;
    private final RenderType renderType;

    public SatLinkRenderer() {
        this.model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/satlink.obj"));
        this.renderType = CompatRenderTypes.entityCutout(
                dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/satlink.png"));
    }

    @Override public SatLinkRenderState createRenderState() { return new SatLinkRenderState(); }
    /** The mast stands six blocks above the core cell, so the machine must not be culled by its own cell. */
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineSatLink be, SatLinkRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        state.rot = be.prevRot + (be.rot - be.prevRot) * partialTicks;
        state.lift = be.prevLift + (be.lift - be.prevLift) * partialTicks;

        BlockState bs = be.getBlockState();
                ForgeDirection dir = MachineCore.facing(bs);
        ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
        state.offsetX = (dir.offsetX + rot.offsetX) * 0.5F;
        state.offsetZ = (dir.offsetZ + rot.offsetZ) * 0.5F;
    }

    @Override
    public void submit(SatLinkRenderState s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(180F));
        ps.translate(s.offsetX, 0F, s.offsetZ);

        part(ps, col, "Base", light);
        ps.mulPose(Axis.YP.rotationDegrees(s.rot));
        part(ps, col, "Rotor", light);
        ps.translate(0F, PIVOT, 0F);
        ps.mulPose(Axis.ZP.rotationDegrees(s.lift));
        ps.translate(0F, -PIVOT, 0F);
        part(ps, col, "Dish", light);

        ps.popPose();
    }

    private void part(PoseStack ps, SubmitSink col, String name, int light) {
        col.submitCustomGeometry(ps, renderType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
