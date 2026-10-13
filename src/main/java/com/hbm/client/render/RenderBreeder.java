package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.tileentity.machine.TileEntityMachineReactorBreeding;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * The breeding reactor body (old TESR: a 90 degree base turn, then the standard facing map, culling off)
 * plus the three green/white spark bolts that crackle above it while it breeds.
 */
public final class RenderBreeder extends MachineObjRenderer<TileEntityMachineReactorBreeding> {

    public RenderBreeder() {
        super(90F, 1F, new ModelDef(Ids.of(Constants.MOD_ID, "models/reactors/breeder.obj"),
                Ids.of(Constants.MOD_ID, "textures/models/machines/breeder.png"), true));
    }

    /** The machine state plus whether the breeder is mid-cycle. */
    public static final class BreederRenderState extends MachineRenderState {
        public boolean sparking;
        public long millis;
    }

    @Override
    public MachineRenderState createRenderState() {
        return new BreederRenderState();
    }

    @Override
    public void extract(TileEntityMachineReactorBreeding blockEntity, MachineRenderState state, float partialTicks, Vec3 cameraPosition) {
        super.extract(blockEntity, state, partialTicks, cameraPosition);
        if (state instanceof BreederRenderState breeder) {
            breeder.sparking = blockEntity.progress > 0.0F;
            breeder.millis = System.currentTimeMillis();
        }
    }

    @Override
    protected void submitUndeclared(MachineRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (!(state instanceof BreederRenderState breeder) || !breeder.sparking) return;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0D, 0.5D);
        for (int i = 0; i < 3; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees((float) (Math.PI * i)));
            renderSpark(poseStack, sink, (int) ((breeder.millis % 10000) / 100 + i), 0, 1.5625, 0, 0.15F, 3, 4, 0x00ff00, 0xffffff);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** 1.7 {@code RenderSparks.renderSpark}: GL line segments (5px / 2px) become thin crossed ribbons. */
    private static void renderSpark(PoseStack poseStack, SubmitSink sink, int seed, double x, double y, double z,
                                    float length, int min, int max, int color1, int color2) {
        Random rand = new Random(seed);
        Vec3 vec = new Vec3(rand.nextDouble() - 0.5, rand.nextDouble() - 0.5, rand.nextDouble() - 0.5).normalize();
        int segments = min + rand.nextInt(max);
        double[][] points = new double[segments + 1][3];
        points[0] = new double[]{x, y, z};
        for (int i = 0; i < segments; i++) {
            Vec3 dir = vec.normalize();
            double dx = dir.x * length * rand.nextFloat();
            double dy = dir.y * length * rand.nextFloat();
            double dz = dir.z * length * rand.nextFloat();
            points[i + 1] = new double[]{points[i][0] + dx, points[i][1] + dy, points[i][2] + dz};
        }

        int outer = 0xFF000000 | (color1 & 0xFFFFFF);
        int inner = 0xFF000000 | (color2 & 0xFFFFFF);
        sink.submitCustomGeometry(poseStack, CompatRenderTypes.additive(), (pose, buffer) -> {
            for (int i = 0; i < segments; i++) {
                ribbon(buffer, pose, points[i], points[i + 1], 0.03F, outer);
                ribbon(buffer, pose, points[i], points[i + 1], 0.012F, inner);
            }
        });
    }

    /** A segment as three axis-aligned quads so it reads from every direction, like a fat GL line. */
    private static void ribbon(VertexConsumer buffer, PoseStack.Pose pose, double[] a, double[] b, float w, int color) {
        float ax = (float) a[0], ay = (float) a[1], az = (float) a[2];
        float bx = (float) b[0], by = (float) b[1], bz = (float) b[2];
        quad(buffer, pose, ax, ay, az - w, bx, by, bz - w, bx, by, bz + w, ax, ay, az + w, color);
        quad(buffer, pose, ax - w, ay, az, bx - w, by, bz, bx + w, by, bz, ax + w, ay, az, color);
        quad(buffer, pose, ax, ay - w, az, bx, by - w, bz, bx, by + w, bz, ax, ay + w, az, color);
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose pose,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3, int color) {
        Vertexes.color(buffer, pose, x0, y0, z0, color);
        Vertexes.color(buffer, pose, x1, y1, z1, color);
        Vertexes.color(buffer, pose, x2, y2, z2, color);
        Vertexes.color(buffer, pose, x3, y3, z3, color);
    }
}
