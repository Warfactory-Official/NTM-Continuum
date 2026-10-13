package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineAnnihilator;
import com.hbm.tileentity.MachineCore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderAnnihilator}: the {@code Annihilator} body, the {@code Roller} spinning on the wall
 * clock, and the {@code Belt} whose texture scrolls one tile every three seconds.
 */
public final class RenderAnnihilator extends GemBER<TileEntityMachineAnnihilator, AnnihilatorRenderState> {

    private final WavefrontObj model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/machines/annihilator.obj"));
    private final Surface surface = Surface.cutout(Constants.MOD_ID + ":textures/models/machines/annihilator.png");
    private final RenderType beltType = CompatRenderTypes.entityCutout(Ids.of(Constants.MOD_ID, "textures/models/machines/annihilator_belt.png"));

    @Override
    protected void setup(Gem.Model<AnnihilatorRenderState> declaration) {
        Gem.Part<AnnihilatorRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .rotate(Gem.Axes.Y, 90F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        machine.child(model.part("Annihilator"), surface);
        // glRotated(t, 0, 0, -1) is a rotation of -t about +Z.
        machine.child(model.part("Roller"), surface)
                .spin(Gem.Axes.Z, 0F, 1.75F, 0F, state -> -state.roller);
    }

    @Override
    public AnnihilatorRenderState createRenderState() {
        return new AnnihilatorRenderState();
    }

    /** Old meta-offset yaw map (after the fixed 90): N(2)=0, W(4)=90, S(3)=180, E(5)=270. */
    @Override
    public void extract(TileEntityMachineAnnihilator be, AnnihilatorRenderState state, float partialTicks, Vec3 cameraPosition) {
        int meta = MachineCore.facingOrdinal(be.getBlockState(), 2);
        state.yaw = switch (meta) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
        long now = System.currentTimeMillis();
        state.roller = (float) (now * 0.15 % 360);
        state.belt = (float) (now / 3000D % 1D);
    }

    /** The belt's UV scroll is baked per frame, so it is not a declared part. */
    @Override
    protected void submitUndeclared(AnnihilatorRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90F));
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        int light = state.lightCoords;
        float scroll = -state.belt;
        sink.submitCustomGeometry(poseStack, beltType,
                (pose, buffer) -> model.renderPart("Belt", pose, buffer, light, OverlayTexture.NO_OVERLAY, -1, scroll, 0F, false));
        poseStack.popPose();
    }
}
