package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineRotaryFurnace;
import com.hbm.tileentity.MachineCore;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;

/**
 * Rotary furnace: the static {@code Furnace} body and the {@code Piston} riding the eased stroke
 * {@code sps(anim * 0.75 * 0.125) * 0.5 - 0.5} of the original {@code RenderRotaryFurnace}. The old
 * {@code "foundry"} particle stream is drawn as the same pour geometry the crucible uses.
 */
public final class RenderRotaryFurnace extends GemBER<TileEntityMachineRotaryFurnace, RotaryFurnaceRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/rotary_furnace.obj"));
    private final Surface surface = Surface.cutout(Constants.MOD_ID + ":textures/models/machines/rotary_furnace.png");
    private final RenderType streamType = CompatRenderTypes.entitySolid(
            Ids.of(Constants.MOD_ID, "textures/models/machines/lava_gray.png"));

    @Override
    protected void setup(Gem.Model<RotaryFurnaceRenderState> declaration) {
        Gem.Part<RotaryFurnaceRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);
        machine.child(model.part("Furnace"), surface);
        machine.child(model.part("Piston"), surface)
                .slide(Gem.Axes.Y, state -> state.piston);
    }

    @Override
    public RotaryFurnaceRenderState createRenderState() {
        return new RotaryFurnaceRenderState();
    }

    /** Old meta-offset yaw map: N(2)=90, W(4)=180, S(3)=270, E(5)=0. */
    @Override
    public void extract(TileEntityMachineRotaryFurnace be, RotaryFurnaceRenderState state, float partialTicks, Vec3 cameraPosition) {
        int meta = MachineCore.facingOrdinal(be.getBlockState(), 2);
        state.dirOrdinal = meta;
        state.yaw = switch (meta) {
            case 2 -> 90F;
            case 4 -> 180F;
            case 3 -> 270F;
            default -> 0F;
        };
        float anim = be.lastAnim + (be.anim - be.lastAnim) * partialTicks;
        state.piston = (float) (sps((anim * 0.75) * 0.125) * 0.5 - 0.5);
        state.pourColor = be.pourColor;
        state.pourLen = be.pourLen;
    }

    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    /** The pour stream at the side spout (2.875 blocks out along the clockwise rotation), where the old particle stood. */
    @Override
    protected void submitUndeclared(RotaryFurnaceRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (state.pourColor < 0 || state.pourLen <= 0) return;
        ForgeDirection dir = ForgeDirection.getOrientation(state.dirOrdinal);
        ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
        poseStack.pushPose();
        poseStack.translate(0.5F + rot.offsetX * 2.875F, 0.75F, 0.5F + rot.offsetZ * 2.875F);
        float len = state.pourLen;
        int rgb = state.pourColor;
        sink.submitCustomGeometry(poseStack, streamType,
                (pose, buffer) -> FoundryRenderHelper.pourStream(pose, buffer, rot, len, 0.625F, 0.625F, rgb));
        poseStack.popPose();
    }
}
