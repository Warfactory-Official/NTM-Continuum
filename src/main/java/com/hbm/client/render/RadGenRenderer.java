package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineRadGen;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** RadGen: the housing and its rotor, with the running light and the glass shell over them. */
public final class RadGenRenderer extends GemBER<TileEntityMachineRadGen, RadGenRenderState> {

    private static final int ON_LIGHT = 0xFF00FF00;
    private static final int OFF_LIGHT = 0xFF001A00;
    private static final int GLASS_TINT = 0x4D80BFFF;

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/radgen.obj"));
    private final Surface body = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/rad_gen_body.png");
    private final Surface glass = Surface.translucent(
            Constants.MOD_ID + ":textures/models/machines/rad_gen_body.png");
    private final RenderType lightType = CompatRenderTypes.lightning();

    @Override
    protected void setup(Gem.Model<RadGenRenderState> declaration) {
        Gem.Part<RadGenRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        machine.child(model.part("Base"), body);
        machine.child(model.part("Rotor"), body)
                .spin(Gem.Axes.X, 0F, 1.5F, 0F, state -> state.rotor);

        machine.child(model.part("Glass"), glass).order(1).tint(state -> GLASS_TINT);
        machine.child(model.part("Glass"), body).order(1);
    }

    @Override
    public RadGenRenderState createRenderState() {
        return new RadGenRenderState();
    }

    @Override
    public void extract(TileEntityMachineRadGen blockEntity, RadGenRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState blockState = blockEntity.getBlockState();
        int meta = MachineCore.facingOrdinal(blockState, 2);
        state.yaw = switch (meta) {
            case 2 -> 90F;
            case 4 -> 180F;
            case 3 -> 270F;
            default -> 0F;
        };
        state.on = blockEntity.isOn;
        state.rotor = state.on ? -(GameTime.now() % 3600L) * 0.1F : 0F;
    }

    /**
     * The indicator is position-and-colour geometry with no texture, and the shell is drawn twice into a
     * later pass so its translucent face sorts behind the opaque one. Neither is a bucket a backend
     * could allocate from a declaration, so both stay drawn.
     */
    @Override
    protected void submitUndeclared(RadGenRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));

        int lightColor = state.on ? ON_LIGHT : OFF_LIGHT;
        sink.submitCustomGeometry(poseStack, lightType,
                (pose, buffer) -> model.renderPartColor("Light", pose, buffer, lightColor));
        poseStack.popPose();
    }
}
