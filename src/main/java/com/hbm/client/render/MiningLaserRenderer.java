package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineMiningLaser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Renderer for the mining laser, a port of the 1.12.2 {@code RenderLaserMiner} TESR. The static
 * {@code Base} hangs from the ceiling; the {@code Pivot} yaws and the {@code Laser} barrel yaws+pitches
 * to track the interpolated mining target; while {@code beam} is set, the layered additive beam
 * ({@link BeamPronter}, white core to dark-red shell) is drawn from the emitter to the target block.
 * All transforms match the old renderer (origin {@code (+0.5, −1, +0.5)}, emitter offset 1.5
 * along the aim vector, {@code v = target − pos + (0,3,0)}).
 */
public class MiningLaserRenderer extends GemBER<TileEntityMachineMiningLaser, MiningLaserRenderState> {

    private static final int BEAM_OUTER = 0xa00000;
    private static final int BEAM_INNER = 0xFFFFFF;

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/mining_laser.obj"));
    private final Surface base = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/mining_laser_base.png");
    private final Surface pivot = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/mining_laser_pivot.png");
    private final Surface barrel = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/mining_laser_laser.png");

    /**
     * The beam can span hundreds of blocks below the machine, so the model never frustum-culls and keeps
     * rendering at long range: the old getRenderBoundingBox and 65536-dist-squared overrides.
     */
    @Override
    protected void setup(Gem.Model<MiningLaserRenderState> declaration) {
        Gem.Part<MiningLaserRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, -1F, 0.5F);

        machine.child(model.part("Base"), base);
        machine.child(model.part("Pivot"), pivot)
                .angle(Gem.Axes.Y, state -> state.yaw);
        machine.child(model.part("Laser"), barrel)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .translate(0F, -1F, 0F)
                .angle(Gem.Axes.X, state -> -(state.pitch + 90F))
                .translate(0F, 1F, 0F);
    }

    @Override public MiningLaserRenderState createRenderState() { return new MiningLaserRenderState(); }

    @Override
    public void extract(TileEntityMachineMiningLaser be, MiningLaserRenderState state, float partialTicks,
                                   Vec3 cameraPosition) {
        state.beam = be.beam;

        // Interpolate the target while beaming (old RenderLaserMiner lines 36-38); parked aim otherwise
        // (the TE holds targetY = y−2 when off, which replaces the old's garbage-input math when idle).
        double tx = be.targetX, ty = be.targetY, tz = be.targetZ;
        if (be.beam) {
            tx = (be.targetX - be.lastTargetX) * partialTicks + be.lastTargetX;
            ty = (be.targetY - be.lastTargetY) * partialTicks + be.lastTargetY;
            tz = (be.targetZ - be.lastTargetZ) * partialTicks + be.lastTargetZ;
        }
        state.vx = tx - be.getBlockPos().getX();
        state.vy = ty - be.getBlockPos().getY() + 3;
        state.vz = tz - be.getBlockPos().getZ();

        // Emitter offset: 1.5 along the normalized aim vector; the barrel aims along the remainder.
        Vec3 aim = new Vec3(state.vx, state.vy, state.vz);
        Vec3 emitter = aim.normalize().scale(1.5);
        Vec3 rest = aim.subtract(emitter);
        state.emitter = emitter;
        state.reach = rest;
        state.yaw = (float) Math.toDegrees(Math.atan2(rest.x, rest.z));
        state.pitch = (float) Math.toDegrees(
                Math.atan2(rest.y, Math.sqrt(rest.x * rest.x + rest.z * rest.z)));
    }

    /** The beam reaches wherever the laser is pointing, so its length is its geometry. */
    @Override
    protected void submitUndeclared(MiningLaserRenderState s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        if (!s.beam) return;
        ps.pushPose();
        ps.translate(0.5F, -1F, 0.5F);
        ps.translate(s.emitter.x, s.emitter.y - 1, s.emitter.z);
        BeamPronter.prontMachineBeam(ps, col, s.reach, BEAM_OUTER, BEAM_INNER, 3, 0.09F);
        ps.popPose();
    }
}
