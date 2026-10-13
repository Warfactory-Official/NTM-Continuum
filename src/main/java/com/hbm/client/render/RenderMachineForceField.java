package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityForceField;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderMachineForceField}: {@code T(0.5, 0, 0.5) Ry(180)}, the radar body in the forcefield skin, then
 * at half a block up the field itself and, half a block above that, the torus - which spins with the wall clock
 * ({@code -millis/10} degrees) only while the field stands.
 *
 * <p>The field is a wire sphere of {@code 16 + radius/8} rings: 1.7's first pass draws the meridians by turning
 * the frame about Y and walking {@code (0, r, 0)} around X, the second the parallels by walking it down about Z
 * and around Y, both as one-pixel line strips in the field's colour with texture and lighting off. The
 * {@code Vec3.rotateAround*} conventions of 1.7 are reproduced exactly.
 */
public final class RenderMachineForceField extends GemBER<TileEntityForceField, RenderMachineForceField.State> {

    public static final class State extends CompatRenderState {
        public boolean field;
        public float radius;
        public int color;
        public float spin;
    }

    private final WavefrontObj body = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/radar_base.obj"));
    private final WavefrontObj top = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/forcefield_top.obj"));
    private final Surface bodySkin = Surface.cutoutNoCull(Constants.MOD_ID + ":textures/models/forcefield_base.png");
    private final Surface topSkin = Surface.cutoutNoCull(Constants.MOD_ID + ":textures/models/forcefield_top.png");

    @Override
    protected void setup(Gem.Model<State> declaration) {
        Gem.Part<State> root = declaration.offScreen().viewDistance(256).joint()
                .translate(0.5F, 0F, 0.5F)
                .rotate(Gem.Axes.Y, 180F);
        root.child(body.all(), bodySkin);
        root.joint()
                .translate(0F, 0.5F, 0F)
                .angle(Gem.Axes.Y, state -> state.field ? -state.spin : 0F)
                .translate(0F, 0.5F, 0F)
                .child(top.all(), topSkin);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityForceField ff, State state, float partialTicks, Vec3 cameraPosition) {
        state.field = ff.isOn && ff.health > 0 && ff.power > 0 && ff.cooldown == 0;
        state.radius = ff.radius;
        state.color = ff.color;
        state.spin = (float) ((GameTime.now() / 10D) % 360D);
    }

    @Override
    protected void submitUndeclared(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (!state.field) return;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180F));
        poseStack.translate(0F, 0.5F, 0F);

        int segments = (int) (16 + state.radius * 0.125);
        int color = 0xFF000000 | state.color;
        meridians(poseStack, sink, segments, segments * 2, state.radius, color);
        parallels(poseStack, sink, segments, segments * 2, state.radius, color);

        poseStack.popPose();
    }

    /** 1.7 {@code generateSphere}: per step the frame turns another {@code 360/s} degrees about Y. */
    private static void meridians(PoseStack poseStack, SubmitSink sink, int l, int s, float rad, int color) {
        poseStack.pushPose();
        float sRot = 360F / s;
        float lRot = (float) Math.PI / l;
        for (int k = 0; k < s; k++) {
            poseStack.mulPose(Axis.YP.rotationDegrees(sRot));
            sink.submitCustomGeometry(poseStack, CompatRenderTypes.lines(), (pose, buffer) -> {
                double x = 0, y = rad, z = 0;
                for (int i = 0; i < l; i++) {
                    double ny = y * Math.cos(lRot) + z * Math.sin(lRot);
                    double nz = z * Math.cos(lRot) - y * Math.sin(lRot);
                    line(buffer, pose, x, y, z, x, ny, nz, color);
                    y = ny;
                    z = nz;
                }
            });
        }
        poseStack.popPose();
    }

    /** 1.7 {@code generateSphere2}: the point steps down about Z, then walks a full circle about Y. */
    private static void parallels(PoseStack poseStack, SubmitSink sink, int l, int s, float rad, int color) {
        float sRot = (float) Math.PI * 2F / s;
        float lRot = (float) Math.PI / l;
        sink.submitCustomGeometry(poseStack, CompatRenderTypes.lines(), (pose, buffer) -> {
            double x = 0, y = rad, z = 0;
            for (int k = 0; k < l; k++) {
                double rx = x * Math.cos(lRot) + y * Math.sin(lRot);
                double ry = y * Math.cos(lRot) - x * Math.sin(lRot);
                x = rx;
                y = ry;
                for (int i = 0; i < s; i++) {
                    double nx = x * Math.cos(sRot) + z * Math.sin(sRot);
                    double nz = z * Math.cos(sRot) - x * Math.sin(sRot);
                    line(buffer, pose, x, y, z, nx, y, nz, color);
                    x = nx;
                    z = nz;
                }
            }
        });
    }

    private static void line(VertexConsumer buffer, PoseStack.Pose pose, double x0, double y0, double z0,
                             double x1, double y1, double z1, int color) {
        float dx = (float) (x1 - x0), dy = (float) (y1 - y0), dz = (float) (z1 - z0);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-6F) return;
        dx /= len; dy /= len; dz /= len;
        Vertexes.line(buffer, pose, (float) x0, (float) y0, (float) z0, color, dx, dy, dz);
        Vertexes.line(buffer, pose, (float) x1, (float) y1, (float) z1, color, dx, dy, dz);
    }
}
