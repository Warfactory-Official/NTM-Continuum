package com.hbm.client.render;

import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CameraInfo;
import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineMiningLaser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Renderer for the mining laser — a port of the 1.12.2 {@code RenderLaserMiner} TESR. The static
 * {@code Base} hangs from the ceiling; the {@code Pivot} yaws and the {@code Laser} barrel yaws+pitches
 * to track the interpolated mining target; while {@code beam} is set, the layered additive beam
 * ({@link BeamPronter}, white core → dark-red shell) is drawn from the emitter to the target block.
 * All transforms are the old renderer's, verbatim (origin {@code (+0.5, −1, +0.5)}, emitter offset 1.5
 * along the aim vector, {@code v = target − pos + (0,3,0)}).
 */
public class MiningLaserRenderer implements CompatBER<TileEntityMachineMiningLaser, MiningLaserRenderState> {

    private static final int BEAM_OUTER = 0xa00000;
    private static final int BEAM_INNER = 0xFFFFFF;

    private final WavefrontObj model;
    private final RenderType baseType;
    private final RenderType pivotType;
    private final RenderType laserType;

    public MiningLaserRenderer() {
        this.model = WavefrontObj.load(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "models/machines/mining_laser.obj"));
        this.baseType = RenderTypes.entityCutout(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/models/machines/mining_laser_base.png"));
        this.pivotType = RenderTypes.entityCutout(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/models/machines/mining_laser_pivot.png"));
        this.laserType = RenderTypes.entityCutout(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/models/machines/mining_laser_laser.png"));
    }

    @Override public MiningLaserRenderState createRenderState() { return new MiningLaserRenderState(); }
    // The beam can span hundreds of blocks below the machine — never frustum-cull it, and keep it
    // rendering at long range (the old getRenderBoundingBox/65536-dist² overrides).
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineMiningLaser be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineMiningLaser be, MiningLaserRenderState state, float partialTicks,
                                   Vec3 cameraPosition) {
        state.beam = be.beam;

        // Interpolate the target while beaming (old RenderLaserMiner lines 36-38); parked aim otherwise
        // (the TE holds targetY = y−2 when off — replaces the old's garbage-input math when idle).
        double tx = be.targetX, ty = be.targetY, tz = be.targetZ;
        if (be.beam) {
            tx = (be.targetX - be.lastTargetX) * partialTicks + be.lastTargetX;
            ty = (be.targetY - be.lastTargetY) * partialTicks + be.lastTargetY;
            tz = (be.targetZ - be.lastTargetZ) * partialTicks + be.lastTargetZ;
        }
        state.vx = tx - be.getBlockPos().getX();
        state.vy = ty - be.getBlockPos().getY() + 3;
        state.vz = tz - be.getBlockPos().getZ();
    }

    @Override
    public void submit(MiningLaserRenderState s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        int light = s.lightCoords;

        ps.pushPose();
        ps.translate(0.5F, -1F, 0.5F); // the old translate(x+0.5, y-1, z+0.5)

        // Emitter offset: 1.5 along the normalized aim vector; the barrel aims along the remainder.
        Vec3 v = new Vec3(s.vx, s.vy, s.vz);
        Vec3 nVec = v.normalize().scale(1.5);
        Vec3 vec = v.subtract(nVec);

        double yaw = Math.toDegrees(Math.atan2(vec.x, vec.z));
        double sqrt = Math.sqrt(vec.x * vec.x + vec.z * vec.z);
        double pitch = Math.toDegrees(Math.atan2(vec.y, sqrt));

        part(col, ps, "Base", baseType, light);

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees((float) yaw));
        part(col, ps, "Pivot", pivotType, light);
        ps.popPose();

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees((float) yaw));
        ps.translate(0F, -1F, 0F);
        ps.mulPose(Axis.XN.rotationDegrees((float) pitch + 90F)); // old glRotated(pitch+90, -1, 0, 0)
        ps.translate(0F, 1F, 0F);
        part(col, ps, "Laser", laserType, light);
        ps.popPose();

        if (s.beam) {
            ps.pushPose();
            ps.translate(nVec.x, nVec.y - 1, nVec.z); // old: beam starts 1.5 out from the pivot, 1 down
            BeamPronter.prontBeam(ps, col, vec, BEAM_OUTER, BEAM_INNER, 3, 0.09F);
            ps.popPose();
        }

        ps.popPose();
    }

    private void part(SubmitSink col, PoseStack ps, String name, RenderType type, int light) {
        col.submitCustomGeometry(ps, type,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
