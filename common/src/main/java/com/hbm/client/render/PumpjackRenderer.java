package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.oil.TileEntityMachinePumpjack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Animated pumpjack renderer — port of the 1.12.2 {@code RenderPumpjack}. Draws the OBJ parts:
 * static {@code Base}; {@code Rotor} spinning about X at the pivot (0, 1.5, −5.5); {@code Head}
 * see-sawing {@code toDegrees(sin(rot))·0.25} about (0, 3.5, −3.5); {@code Carriage} bobbing
 * {@code −sin(rot)} vertically. The untextured POSITION_COLOR geometry (two gray side beams, the
 * dark crank ropes/bridle over the head "horse head" arc, and the twin polished-rod lines) is
 * emitted as fullbright vertex-colored quads on a white texture, double-wound to reproduce the old
 * {@code disableCull} (see IMPLEMENTATION.md §13q pass 4). Old yaw map = base 90° + the standard
 * facing map. Beam rotation is integrated client-side from the server-synced speed.
 */
public class PumpjackRenderer implements BlockEntityRenderer<TileEntityMachinePumpjack, PumpjackRenderState> {

    private static final float BASE_YAW = 90F;
    private static final int FULLBRIGHT = 0xF000F0; // packed skylight/blocklight 15/15 (see FoundryRenderHelper)

    private final WavefrontObj model;
    private final RenderType baseType;
    private final RenderType colorType;

    public PumpjackRenderer() {
        this.model = WavefrontObj.load(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "models/machines/pumpjack.obj"));
        this.baseType = RenderTypes.entityCutout(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/models/machines/pumpjack.png"));
        // The untextured crank/rope geometry is drawn on entitySolid (opaque, depth-writing, culls) —
        // its faces are double-wound below so the cull keeps exactly the outward copy, giving the flat
        // uniform shading the old disableCull+disableLighting POSITION_COLOR pass had. entityCutout would
        // draw both windings (no cull) and PER_FACE_LIGHTING would darken the back copy → the odd look.
        this.colorType = RenderTypes.entitySolid(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/models/misc/white.png"));
    }

    @Override public PumpjackRenderState createRenderState() { return new PumpjackRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachinePumpjack be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extractRenderState(TileEntityMachinePumpjack be, PumpjackRenderState state, float partialTicks,
                                   Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        BlockState bs = be.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 4 -> 90F;   // WEST
            case 3 -> 180F;  // SOUTH
            case 5 -> 270F;  // EAST
            default -> 0F;   // NORTH
        };
        state.rotation = be.prevRot + (be.rot - be.prevRot) * partialTicks;
    }

    @Override
    public void submit(PumpjackRenderState s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        float rotation = s.rotation;

        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(BASE_YAW + s.yaw));

        part(col, ps, "Base", light);

        // Rotor: continuous X spin about the crank axle (0, 1.5, -5.5).
        ps.pushPose();
        ps.translate(0F, 1.5F, -5.5F);
        ps.mulPose(Axis.XP.rotationDegrees(rotation - 90F));
        ps.translate(0F, -1.5F, 5.5F);
        part(col, ps, "Rotor", light);
        ps.popPose();

        // Head: see-saw about the samson-post bearing (0, 3.5, -3.5).
        ps.pushPose();
        ps.translate(0F, 3.5F, -3.5F);
        ps.mulPose(Axis.XP.rotationDegrees((float) (Math.toDegrees(Math.sin(Math.toRadians(rotation))) * 0.25)));
        ps.translate(0F, -3.5F, 3.5F);
        part(col, ps, "Head", light);
        ps.popPose();

        // Carriage: vertical bob.
        ps.pushPose();
        ps.translate(0F, (float) -Math.sin(Math.toRadians(rotation)), 0F);
        part(col, ps, "Carriage", light);
        ps.popPose();

        // Untextured crank/beam/rope geometry (old POSITION_COLOR pass, lighting off = fullbright).
        col.submitCustomGeometry(ps, colorType, (pose, buffer) -> drawColorGeometry(pose, buffer, rotation));

        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, String name, int light) {
        col.submitCustomGeometry(ps, baseType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }

    /** The old immediate-mode POSITION_COLOR pass, verbatim math (RenderPumpjack L68-162). */
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

    /** Emits a quad twice (both windings) — the modern stand-in for the old {@code disableCull}. */
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
        buf.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(r, g, b, a)
                .setUv(0.5F, 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULLBRIGHT)
                .setNormal(pose, 0F, 1F, 0F);
    }

    /**
     * Old {@code Vec3.rotateAroundX} (radians), VERBATIM: {@code y' = y·cos + z·sin}, {@code z' = z·cos
     * − y·sin}. This is the <b>clockwise</b> convention — the opposite of a standard CCW X‑rotation (and
     * of {@code Axis.XP.rotationDegrees} used for the Rotor/Head OBJ parts). The old call sites feed it
     * NEGATIVE angles, which nets back to the OBJ parts' CCW motion; using a standard CCW formula here
     * (with those same negative angles) would spin the crank/pitman geometry backwards relative to the
     * rotor — the "desynced / playing backwards" look. Must match {@code Vec3.rotateAroundX} exactly.
     */
    private static double[] rotateX(double x, double y, double z, float angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new double[]{x, y * cos + z * sin, z * cos - y * sin};
    }
}
