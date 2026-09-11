package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.dfc.TileEntityCore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * The orb inside the reaction chamber. Idle it is a small grey sphere that throws the odd arc; running it
 * is a pulsing shell of nested spheres tinted by the two catalysts and sized by how much fuel is in it;
 * melting down it is the ray burst.
 *
 * <p>All three passes are untextured additive geometry upstream, drawn with texturing switched off. Here
 * they go through {@code dragonRays()}, the modern pipeline with the same blend, no depth write and no
 * cull — the same substitution {@link BeamPronter} makes.
 */
public class CoreRenderer implements CompatBER<TileEntityCore, CoreRenderState> {

    private static final int SHELLS = 16;
    private static final int FLARE_RAYS = 150;

    private final WavefrontObj sphere;
    private final WavefrontObj shell;

    public CoreRenderer() {
        this.sphere = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/sphere_uv.obj"));
        this.shell = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/sphere_ruv.obj"));
    }

    @Override public CoreRenderState createRenderState() { return new CoreRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityCore be, Vec3 cameraPosition) { return true; }
    /** A running core is a sixteen-block sphere, so it must stay visible from far outside its own cell. */
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityCore be, CoreRenderState state, float partialTicks, Vec3 cameraPosition) {
        state.heat = be.heat;
        state.color = be.color;
        state.meltdownTick = be.meltdownTick;
        int total = be.tanks[0].getMaxFill() + be.tanks[1].getMaxFill();
        state.fill = total <= 0 ? 0F : (float) (be.tanks[0].getFill() + be.tanks[1].getFill()) / total;
        state.gameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0L;
        state.spark = be.getLevel() != null && be.getLevel().getRandom().nextInt(50) == 0;
    }

    @Override
    public void submit(CoreRenderState s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        ps.pushPose();
        ps.translate(0.5F, 0.5F, 0.5F);

        if (s.heat == 0) standby(s, ps, col);
        else if (s.meltdownTick) flare(s, ps, col);
        else orb(s, ps, col);

        ps.popPose();
    }

    /** The cold core: a dull ball in a faint blue halo, arcing now and then. */
    private void standby(CoreRenderState s, PoseStack ps, SubmitSink col) {
        RenderType type = CompatRenderTypes.dragonRays();

        ps.pushPose();
        ps.scale(0.25F, 0.25F, 0.25F);
        submitSphere(ps, col, type, sphere, 0x808080);
        ps.scale(1.25F, 1.25F, 1.25F);
        submitSphere(ps, col, type, sphere, 0x1A3366);
        ps.popPose();

        if (!s.spark) return;

        int seed = (int) (s.gameTime / 2L);
        col.submitCustomGeometry(ps, type, (pose, buffer) -> {
            for (int i = 0; i < 3; i++) {
                arc(pose, buffer, seed + i * 10_000, 1.5F, 0x00FFFF);
                arc(pose, buffer, seed * 2 + i * 10_000, 3F, 0x00FFFF);
            }
        });
    }

    /** The running core: nested shells breathing on the old arctangent pulse. */
    private void orb(CoreRenderState s, PoseStack ps, SubmitSink col) {
        RenderType type = CompatRenderTypes.dragonRays();
        int tint = dim(s.color, 0.4F);

        ps.pushPose();
        float scale = 4.5F * s.fill + 0.5F;
        ps.scale(scale, scale, scale);
        ps.scale(0.25F, 0.25F, 0.25F);

        submitSphere(ps, col, type, shell, tint);

        float pulse = pulse(s.gameTime, 0.1D);
        for (int i = 0; i <= SHELLS; i++) {
            ps.pushPose();
            float step = 1F + 0.25F * i + (pulse * (20 - i)) * 0.125F;
            ps.scale(step, step, step);
            submitSphere(ps, col, type, shell, tint);
            ps.popPose();
        }

        ps.popPose();
    }

    /** The meltdown: a hundred and fifty rays in the core's colour, pulsing and shrinking. */
    private void flare(CoreRenderState s, PoseStack ps, SubmitSink col) {
        int color = s.color;
        float spin = s.gameTime / 200.0F;
        float pulse = pulse(s.gameTime, 0.2D);

        ps.pushPose();
        float scale = 0.875F + pulse * 0.125F;
        ps.scale(scale, scale, scale);

        Random random = new Random(432L);
        for (int i = 0; i < FLARE_RAYS; i++) {
            ps.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360F));
            ps.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360F));
            ps.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360F));
            ps.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360F));
            ps.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360F));
            ps.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360F + spin * 90F));

            float length = random.nextFloat() * 2F + 5F;
            float width = random.nextFloat() + 1F;

            col.submitCustomGeometry(ps, CompatRenderTypes.dragonRays(),
                    (pose, buffer) -> ray(pose, buffer, color, length, width));

            ps.scale(0.999F, 0.999F, 0.999F);
        }
        ps.popPose();
    }

    /** One ray of the burst: a fan from the centre out to a triangle, fading to nothing at the tip. */
    private static void ray(PoseStack.Pose pose, VertexConsumer buffer, int color, float length, float width) {
        int centre = 0xFF000000 | (color & 0xFFFFFF);
        int edge = color & 0xFFFFFF;

        float ax = -0.866F * width, az = -0.5F * width;
        float bx = 0.866F * width, bz = -0.5F * width;
        float cz = width;

        triangle(pose, buffer, centre, edge, 0F, 0F, 0F, ax, length, az, bx, length, bz);
        triangle(pose, buffer, centre, edge, 0F, 0F, 0F, bx, length, bz, 0F, length, cz);
        triangle(pose, buffer, centre, edge, 0F, 0F, 0F, 0F, length, cz, ax, length, az);
    }

    private static void triangle(PoseStack.Pose pose, VertexConsumer buffer, int centre, int edge,
                                 float ax, float ay, float az, float bx, float by, float bz,
                                 float cx, float cy, float cz) {
        vertex(pose, buffer, centre, ax, ay, az);
        vertex(pose, buffer, edge, bx, by, bz);
        vertex(pose, buffer, edge, cx, cy, cz);
    }

    /** A branching arc, the old {@code RenderSparks.renderSpark} drawn as thin prisms instead of lines. */
    private static void arc(PoseStack.Pose pose, VertexConsumer buffer, int seed, float length, int color) {
        Random random = new Random(seed);
        Vec3 direction = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5,
                random.nextDouble() - 0.5).normalize();

        double x = 0, y = 0, z = 0;
        int steps = 5 + random.nextInt(10);
        for (int i = 0; i < steps; i++) {
            Vec3 step = direction.scale(length * random.nextFloat());
            double nx = x + step.x, ny = y + step.y, nz = z + step.z;
            prism(pose, buffer, color, (float) x, (float) y, (float) z, (float) nx, (float) ny, (float) nz, 0.02F);
            x = nx;
            y = ny;
            z = nz;
        }
    }

    private static void prism(PoseStack.Pose pose, VertexConsumer buffer, int color,
                             float x0, float y0, float z0, float x1, float y1, float z1, float radius) {
        int argb = 0xFF000000 | (color & 0xFFFFFF);
        quad(pose, buffer, argb, x0 + radius, y0, z0, x0 - radius, y0, z0, x1 - radius, y1, z1, x1 + radius, y1, z1);
        quad(pose, buffer, argb, x0, y0, z0 + radius, x0, y0, z0 - radius, x1, y1, z1 - radius, x1, y1, z1 + radius);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int argb,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        vertex(pose, buffer, argb, ax, ay, az);
        vertex(pose, buffer, argb, bx, by, bz);
        vertex(pose, buffer, argb, cx, cy, cz);
        vertex(pose, buffer, argb, ax, ay, az);
        vertex(pose, buffer, argb, cx, cy, cz);
        vertex(pose, buffer, argb, dx, dy, dz);
        vertex(pose, buffer, argb, ax, ay, az);
        vertex(pose, buffer, argb, dx, dy, dz);
        vertex(pose, buffer, argb, cx, cy, cz);
        vertex(pose, buffer, argb, ax, ay, az);
        vertex(pose, buffer, argb, cx, cy, cz);
        vertex(pose, buffer, argb, bx, by, bz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, int argb, float x, float y, float z) {
        buffer.addVertex(pose, x, y, z).setColor(argb);
    }

    private static void submitSphere(PoseStack ps, SubmitSink col, RenderType type, WavefrontObj model, int color) {
        col.submitCustomGeometry(ps, type, (pose, buffer) -> model.renderAllColor(pose, buffer, color));
    }

    /** Upstream's breathing curve: an arctangent sweep that lingers at the extremes. */
    private static float pulse(long gameTime, double rate) {
        double angle = (gameTime * rate) % (Math.PI * 2D);
        double t = 0.8D;
        float value = (float) ((1D / t) * Math.atan((t * Math.sin(angle)) / (1D - t * Math.cos(angle))));
        return (value + 1F) / 2F;
    }

    private static int dim(int color, float factor) {
        int r = (int) ((color >> 16 & 0xFF) * factor);
        int g = (int) ((color >> 8 & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return r << 16 | g << 8 | b;
    }
}
