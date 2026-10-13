package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.oil.TileEntityMachinePumpjack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Animated pumpjack renderer, a port of the 1.12.2 {@code RenderPumpjack}. Draws the OBJ parts:
 * static {@code Base}; {@code Rotor} spinning about X at the pivot (0, 1.5, −5.5); {@code Head}
 * see-sawing {@code toDegrees(sin(rot))·0.25} about (0, 3.5, −3.5); {@code Carriage} bobbing
 * {@code −sin(rot)} vertically. The untextured POSITION_COLOR geometry (two gray side beams, the
 * dark crank ropes/bridle over the head "horse head" arc, and the twin polished-rod lines) is
 * emitted as fullbright vertex-colored quads on a white texture, double-wound to reproduce the old
 * {@code disableCull}. Old yaw map = base 90° + the standard
 * facing map. Beam rotation is integrated client-side from the server-synced speed.
 */
public class PumpjackRenderer extends GemBER<TileEntityMachinePumpjack, PumpjackRenderState> {

    private static final float BASE_YAW = 90F;
    private static final int FULLBRIGHT = 0xF000F0; // packed skylight/blocklight 15/15 (see FoundryRenderHelper)

    private final WavefrontObj model;
    private final Surface surface;
    private final RenderType colorType;

    public PumpjackRenderer() {
        this.model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/machines/pumpjack.obj"));
        this.surface = Surface.cutout(Constants.MOD_ID + ":textures/models/machines/pumpjack.png");
        // The untextured crank/rope geometry is drawn on entitySolid (opaque, depth-writing, culls).
        // Its faces are double-wound below so the cull keeps exactly the outward copy, giving the flat
        // uniform shading the old disableCull+disableLighting POSITION_COLOR pass had. entityCutout would
        // draw both windings (no cull) and PER_FACE_LIGHTING would darken the back copy.
        this.colorType = CompatRenderTypes.entitySolid(Ids.of(Constants.MOD_ID, "textures/models/misc/white.png"));
    }

    @Override
    protected void setup(Gem.Model<PumpjackRenderState> declaration) {
        Gem.Part<PumpjackRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> BASE_YAW + state.yaw);

        machine.child(model.part("Base"), surface);
        machine.child(model.part("Rotor"), surface)
                .spin(Gem.Axes.X, 0F, 1.5F, -5.5F, state -> state.rotation - 90F);
        machine.child(model.part("Head"), surface)
                .spin(Gem.Axes.X, 0F, 3.5F, -3.5F, state ->
                        (float) (Math.toDegrees(Math.sin(Math.toRadians(state.rotation))) * 0.25));
        machine.child(model.part("Carriage"), surface)
                .slide(Gem.Axes.Y, state -> (float) -Math.sin(Math.toRadians(state.rotation)));
    }

    @Override public PumpjackRenderState createRenderState() { return new PumpjackRenderState(); }

    @Override
    public void extract(TileEntityMachinePumpjack be, PumpjackRenderState state, float partialTicks,
                                   Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = MachineCore.facingOrdinal(bs, 2);
        state.yaw = switch (meta) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
        state.rotation = be.prevRot + (be.rot - be.prevRot) * partialTicks;
    }

    /**
     * The crank, pitman beams and bridle ropes are built from the rotor angle every frame. The old
     * immediate-mode POSITION_COLOR pass, with lighting off, so they are fullbright and untextured.
     */
    @Override
    protected void submitUndeclared(PumpjackRenderState s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        float rotation = s.rotation;
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(BASE_YAW + s.yaw));
        col.submitCustomGeometry(ps, colorType, (pose, buffer) -> drawColorGeometry(pose, buffer, rotation));
        ps.popPose();
    }

    /** The old immediate-mode POSITION_COLOR pass, using the same math (RenderPumpjack L68-162). */
    private static void drawColorGeometry(PoseStack.Pose pose, VertexConsumer buf, float rotation) {
        // rotateAroundX helper (old Vec3.rotateAroundX, angle in radians)
        // back position of the pitman arms on the rotor crank
        double[] backPos = rotateX(0, 0, -2, -(float) Math.sin(Math.toRadians(rotation)) * 0.25F);
        double[] rot = rotateX(0, 0.5, 0, -(float) Math.toRadians(rotation - 90));

        final float la = 1.0F;
        final float lr = 0.5F, lg = 0.5F, lb = 0.5F;

        // Two side pitman beams (gray)
        for (int i = -1; i <= 1; i += 2) {
            double xi = 0.53125 * i;
            double y1 = 1.5D + rot[1];
            double z1 = -5.5D + rot[2];
            double y2 = 3.5 + backPos[1];
            double z2 = -3.5 + backPos[2];

            quad(pose, buf, lr, lg, lb, la,
                    xi, y1, z1 - 0.0625,
                    xi, y1, z1 + 0.0625,
                    xi, y2, z2 + 0.0625,
                    xi, y2, z2 - 0.0625);
        }

        final float dr = 0.2F, dg = 0.2F, db = 0.2F;

        double pd = 0.03125D;
        double width = 0.25D;
        double height = -Math.sin(Math.toRadians(rotation));

        // The bridle ropes over the horse-head arc
        for (int i = -1; i <= 1; i += 2) {
            float pRot = -(float) (Math.sin(Math.toRadians(rotation)) * 0.25);
            double[] frontPos = rotateX(0, 0, 1, pRot);

            double dist = 0.03125D;
            double cutlet = 360D / 32D;
            double[] frontRad = rotateX(0, 0, 2.5 + dist, pRot);
            frontRad = rotateX(frontRad[0], frontRad[1], frontRad[2], -(float) Math.toRadians(cutlet * -3));

            for (int j = 0; j < 4; j++) {
                double sumY1 = frontPos[1] + frontRad[1];
                double sumZ1 = frontPos[2] + frontRad[2];
                if (frontRad[1] < 0) sumZ1 = 3.5 + dist * 0.5;

                frontRad = rotateX(frontRad[0], frontRad[1], frontRad[2], -(float) Math.toRadians(cutlet));

                double sumY2 = frontPos[1] + frontRad[1];
                double sumZ2 = frontPos[2] + frontRad[2];
                if (frontRad[1] < 0) sumZ2 = 3.5 + dist * 0.5;

                double xL = (width - pd) * i;
                double xR = (width + pd) * i;

                quad(pose, buf, dr, dg, db, la,
                        xL, 3.5 + sumY1, -3.5 + sumZ1,
                        xR, 3.5 + sumY1, -3.5 + sumZ1,
                        xR, 3.5 + sumY2, -3.5 + sumZ2,
                        xL, 3.5 + sumY2, -3.5 + sumZ2);
            }

            double sumY = frontPos[1] + frontRad[1];
            double sumZ = frontPos[2] + frontRad[2];
            if (frontRad[1] < 0) sumZ = 3.5 + dist * 0.5;

            double xR = (width + pd) * i;
            double xL = (width - pd) * i;

            quad(pose, buf, dr, dg, db, la,
                    xR, 3.5 + sumY, -3.5 + sumZ,
                    xL, 3.5 + sumY, -3.5 + sumZ,
                    xL, 2 + height, 0,
                    xR, 2 + height, 0);
        }

        // The polished rod (two thin crossed strips down to the wellhead)
        double p = 0.03125;
        quad(pose, buf, dr, dg, db, la,
                p, height + 1.5, p,
                -p, height + 1.5, -p,
                -p, 0.75, -p,
                p, 0.75, p);
        quad(pose, buf, dr, dg, db, la,
                -p, height + 1.5, p,
                p, height + 1.5, -p,
                p, 0.75, -p,
                -p, 0.75, p);
    }

    /** Emits a quad twice (both windings), the modern stand-in for the old {@code disableCull}. */
    private static void quad(PoseStack.Pose pose, VertexConsumer buf, float r, float g, float b, float a,
                             double x1, double y1, double z1, double x2, double y2, double z2,
                             double x3, double y3, double z3, double x4, double y4, double z4) {
        vertex(pose, buf, r, g, b, a, x1, y1, z1);
        vertex(pose, buf, r, g, b, a, x2, y2, z2);
        vertex(pose, buf, r, g, b, a, x3, y3, z3);
        vertex(pose, buf, r, g, b, a, x4, y4, z4);
        // reverse winding
        vertex(pose, buf, r, g, b, a, x4, y4, z4);
        vertex(pose, buf, r, g, b, a, x3, y3, z3);
        vertex(pose, buf, r, g, b, a, x2, y2, z2);
        vertex(pose, buf, r, g, b, a, x1, y1, z1);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buf, float r, float g, float b, float a,
                               double x, double y, double z) {
        Vertexes.vertex(buf, pose, (float) x, (float) y, (float) z, r, g, b, a, 0.5F, 0.5F,
                OverlayTexture.NO_OVERLAY, FULLBRIGHT, 0F, 1F, 0F);
    }

    /**
     * Old {@code Vec3.rotateAroundX} (radians): {@code y' = y·cos + z·sin}, {@code z' = z·cos
     * − y·sin}. This is the <b>clockwise</b> convention, the opposite of a standard CCW X‑rotation (and
     * of {@code Axis.XP.rotationDegrees} used for the Rotor/Head OBJ parts). The old call sites feed it
     * negative angles, which nets back to the OBJ parts' CCW motion; a standard CCW formula with those
     * same angles would spin the crank/pitman geometry backwards relative to the rotor.
     * Must match {@code Vec3.rotateAroundX} exactly.
     */
    private static double[] rotateX(double x, double y, double z, float angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new double[]{x, y * cos + z * sin, z * cos - y * sin};
    }
}
