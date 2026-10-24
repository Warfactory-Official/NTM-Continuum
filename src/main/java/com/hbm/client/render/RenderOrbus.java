package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.storage.TileEntityMachineOrbus;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderOrbus}: the tank is centred on the corner the core shares with the other three middle columns,
 * shifted there per facing. While it holds anything a sphere of the fluid's colour, scaled by the fill, bobs at
 * y=2.5 inside it, and three beams run up the middle - a dark spiral and two jittering blue arcs.
 *
 * <p>The sphere was drawn untextured and unlit with the depth buffer on; it is submitted as flat colour on the
 * translucent textureless pipeline at full alpha, which has no depth write - it sits inside the opaque tank.
 */
public final class RenderOrbus extends GemBER<TileEntityMachineOrbus, RenderOrbus.State> {

    public static final class State extends CompatRenderState {
        public float offX;
        public float offZ;
        public int fill;
        public double scale;
        public int color;
        public long time;
        public float interp;
    }

    private final WavefrontObj model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/machines/orbus.obj"));
    private final WavefrontObj sphere = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/sphere_uv.obj"));
    private final Surface skin = Surface.cutoutCull(Constants.MOD_ID + ":textures/models/machines/orbus.png");

    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.offScreen().viewDistance(256).joint()
                .slide(Gem.Axes.X, state -> state.offX)
                .slide(Gem.Axes.Z, state -> state.offZ)
                .child(model.all(), skin);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityMachineOrbus orbus, State state, float partialTicks, Vec3 cameraPosition) {
        switch (MachineCore.facingOrdinal(orbus.getBlockState(), 3)) {
            case 2 -> { state.offX = 1F; state.offZ = 1F; }
            case 4 -> { state.offX = 1F; state.offZ = 0F; }
            case 5 -> { state.offX = 0F; state.offZ = 1F; }
            default -> { state.offX = 0F; state.offZ = 0F; }
        }
        state.fill = orbus.tank.getFill();
        state.scale = (double) orbus.tank.getFill() / (double) Math.max(1, orbus.tank.getMaxFill());
        state.color = orbus.tank.getTankType().clientMetadata().getColor();
        state.time = orbus.getLevel() == null ? 0L : orbus.getLevel().getGameTime();
        state.interp = partialTicks;
    }

    @Override
    protected void submitUndeclared(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (state.fill <= 0) return;
        double scale = state.scale;

        poseStack.pushPose();
        poseStack.translate(state.offX, 0F, state.offZ);

        poseStack.pushPose();
        poseStack.translate(0D, 2.5D + Math.sin(((state.time + state.interp) * 0.1D) % (Math.PI * 2D)) * 0.125 * scale, 0D);
        poseStack.scale((float) scale, (float) scale, (float) scale);
        int color = 0xFF000000 | (state.color & 0xFFFFFF);
        sink.submitCustomGeometry(poseStack, CompatRenderTypes.weaponSmoke(), (pose, buffer) -> sphere.renderAllColor(pose, buffer, color));
        poseStack.popPose();

        poseStack.translate(0F, 1F, 0F);
        Vec3 up = new Vec3(0, 3, 0);
        BeamPronter.prontBeam(poseStack, sink, up, BeamPronter.EnumWaveType.SPIRAL, BeamPronter.EnumBeamType.SOLID,
                0, 1, 0F, 0x101020, 0x101020, 6, (float) scale * 0.5F);
        BeamPronter.prontBeam(poseStack, sink, up, BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID,
                (int) (state.time / 2) % 1000, 6, (float) scale, 0x202060, 0x202060, 2, 0.0625F * (float) scale);
        BeamPronter.prontBeam(poseStack, sink, up, BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID,
                (int) (state.time / 4) % 1000, 6, (float) scale, 0x202060, 0x202060, 2, 0.0625F * (float) scale);

        poseStack.popPose();
    }
}
