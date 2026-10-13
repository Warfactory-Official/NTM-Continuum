package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityConveyorPress;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Conveyor press: the {@code Press} frame, the {@code Piston} dropping {@code 0.75} blocks at full stroke
 * (drawn only while a stamp is installed), and the {@code Belt} whose texture the original scrolled
 * through the GL texture matrix by {@code (gameTime % 16 - 2) / 16} each tick. The scroll is baked into
 * the belt vertices per frame, so the belt is drawn undeclared.
 */
public final class RenderConveyorPress extends GemBER<TileEntityConveyorPress, ConveyorPressRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/conveyor_press.obj"));
    private final Surface surface = Surface.cutout(Constants.MOD_ID + ":textures/models/machines/conveyor_press.png");
    private final RenderType beltType = CompatRenderTypes.entityCutout(
            Ids.of(Constants.MOD_ID, "textures/models/machines/conveyor_press_belt.png"));

    @Override
    protected void setup(Gem.Model<ConveyorPressRenderState> declaration) {
        Gem.Part<ConveyorPressRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);
        machine.child(model.part("Press"), surface);
        machine.child(model.part("Piston"), surface)
                .slide(Gem.Axes.Y, state -> -state.piston * 0.75F)
                .visibleIf(state -> state.hasStamp);
    }

    @Override
    public ConveyorPressRenderState createRenderState() {
        return new ConveyorPressRenderState();
    }

    /** Old meta-offset yaw map: N(2)=90, W(4)=180, S(3)=270, E(5)=0. */
    @Override
    public void extract(TileEntityConveyorPress be, ConveyorPressRenderState state, float partialTicks, Vec3 cameraPosition) {
        int meta = MachineCore.facingOrdinal(be.getBlockState(), 2);
        state.yaw = switch (meta) {
            case 2 -> 90F;
            case 4 -> 180F;
            case 3 -> 270F;
            default -> 0F;
        };
        state.piston = (float) (be.lastPress + (be.renderPress - be.lastPress) * partialTicks);
        state.hasStamp = !be.syncStack.isEmpty();
        Level level = be.getLevel();
        int ticks = level == null ? 0 : (int) (level.getGameTime() % 16) - 2;
        state.beltScroll = ticks / 16F;
    }

    @Override
    protected void submitUndeclared(ConveyorPressRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        float v = state.beltScroll;
        int light = state.lightCoords;
        sink.submitCustomGeometry(poseStack, beltType, (pose, buffer) ->
                model.renderPart("Belt", pose, buffer, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, 0F, v, false));
        poseStack.popPose();
    }
}
