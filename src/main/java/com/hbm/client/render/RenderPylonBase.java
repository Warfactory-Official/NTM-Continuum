package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.tileentity.network.TileEntityPylonBase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.Renderers;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.GemBER;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7's {@code RenderPylonBase}: the pylon body is a declared model (per subclass); the wires are built
 * here. Each pylon draws its half of every wire -- from its own mount to the midpoint between the two
 * mounts -- as ten sagging segments of two crossed quads, lit by the block each segment hangs in. Neither
 * end owns a wire: the two halves meet at the midpoint, and a peer whose block entity is not loaded on
 * the client simply contributes no half.
 *
 * <p>1.7 drew the wire with {@code GL_LIGHTING} off, so it carries no shade of its own -- vertex colour
 * times the lightmap. That is {@link CompatRenderTypes#flatCutout}, not the stock entity cutout: on 26.1
 * the stock pipeline lights by {@code gl_FrontFacing}, and a two-sided crossed sheet is a back face from
 * half the viewing angles, which put half of every wire on the 0.4 ambient floor (dark banding).
 */
public abstract class RenderPylonBase<T extends TileEntityPylonBase> extends GemBER<T, PylonRenderState> {

    /** The old {@code ClientConfig.RENDER_CABLE_HANG}: straight wires when off. */
    public static boolean RENDER_CABLE_HANG = true;

    private static final Identifier WIRE = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/network/wire.png");
    private static final Identifier WIRE_GREYSCALE = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/network/wire_greyscale.png");
    private static final int COUNT = 10;
    public static final int LINE_COLOR = 0xBB3311;

    @Override
    public PylonRenderState createRenderState() {
        return new PylonRenderState();
    }

    /**
     * 1.7 {@code renderLinesGeneric}: for every connected pylon, pair up the mount points and record the
     * half-line from ours to the midpoint. Sampling the light here keeps the submit step off the world.
     */
    protected void extractWires(T pyl, PylonRenderState state) {
        state.wires.clear();
        state.color = pyl.color;
        Level level = pyl.getLevel();
        if (level == null) return;
        BlockPos at = pyl.getBlockPos();

        for (BlockPos wire : pyl.getConnected()) {
            BlockEntity tile = level.getBlockEntity(wire);
            if (!(tile instanceof TileEntityPylonBase pylon)) continue;

            Vec3[] m1 = pyl.getMountPos();
            Vec3[] m2 = pylon.getMountPos();
            int lineCount = Math.min(m1.length, m2.length);

            for (int line = 0; line < lineCount; line++) {
                Vec3 first = m1[line % m1.length];
                int secondIndex = line % m2.length;

                // hacky hacky hack: shift the mount order by 2 so the wires of a meta 12 / meta 15 pair do not cross.
                if (lineCount == 4 && ((pyl.facingMeta() == 5 && pylon.facingMeta() == 2)
                        || (pyl.facingMeta() == 2 && pylon.facingMeta() == 5))) {
                    secondIndex += 2;
                    secondIndex %= m2.length;
                }
                Vec3 second = m2[secondIndex];

                double sX = second.x + wire.getX() - at.getX();
                double sY = second.y + wire.getY() - at.getY();
                double sZ = second.z + wire.getZ() - at.getZ();

                double x0 = first.x, y0 = first.y, z0 = first.z;
                double x1 = first.x + (sX - first.x) * 0.5;
                double y1 = first.y + (sY - first.y) * 0.5;
                double z1 = first.z + (sZ - first.z) * 0.5;

                int[] light = new int[COUNT];
                double hang = RENDER_CABLE_HANG ? Math.min(Math.sqrt(sq(x0 - x1) + sq(y0 - y1) + sq(z0 - z1)) / 15D, 2.5D) : 0;
                for (int j = 0; j < COUNT; j++) {
                    double sagJ = Math.sin(j / (double) COUNT * Math.PI * 0.5) * hang;
                    double sagK = Math.sin((j + 1) / (double) COUNT * Math.PI * 0.5) * hang;
                    double ja = j + 0.5D;
                    double ix = at.getX() + x0 + (x1 - x0) / COUNT * ja;
                    double iy = at.getY() + y0 + (y1 - y0) / COUNT * ja - (sagJ + sagK) / 2D;
                    double iz = at.getZ() + z0 + (z1 - z0) / COUNT * ja;
                    light[j] = Renderers.lightAt(level, new BlockPos(Mth.floor(ix), Mth.floor(iy), Mth.floor(iz)));
                }
                state.wires.add(new PylonRenderState.WireHalf(x0, y0, z0, x1, y1, z1, light));
            }
        }
    }

    private static double sq(double d) {
        return d * d;
    }

    @Override
    protected void submitUndeclared(PylonRenderState state, PoseStack ps, SubmitSink sink, CameraInfo camera) {
        if (state.wires.isEmpty()) return;
        Identifier tex = state.color == 0 ? WIRE : WIRE_GREYSCALE;
        int color = 0xFF000000 | (state.color == 0 ? 0xFFFFFF : state.color);
        int ownLight = state.lightCoords;
        // one submit for the whole run: every segment shares this pose and render type
        sink.submitCustomGeometry(ps, CompatRenderTypes.flatCutout(tex), (pose, buffer) -> {
            for (PylonRenderState.WireHalf wire : state.wires) renderLine(pose, buffer, wire, color, ownLight);
        });
    }

    /**
     * 1.7 {@code renderLine}: half a wire, its "hang" computed here. A straight wire (hang off) is lit
     * by the pylon's own block, the way the 1.7 straight arm never sampled along its span.
     */
    private static void renderLine(PoseStack.Pose pose, VertexConsumer buffer, PylonRenderState.WireHalf w, int color, int ownLight) {
        double x0 = w.x0(), y0 = w.y0(), z0 = w.z0(), x1 = w.x1(), y1 = w.y1(), z1 = w.z1();
        double dX = x0 - x1, dY = y0 - y1, dZ = z0 - z1;

        double girth = 0.03125D;
        double hyp = Math.sqrt(dX * dX + dZ * dZ);
        double yaw = Math.atan2(dX, dZ);
        double pitch = Math.atan2(dY, hyp);
        double rotator = Math.PI * 0.5D;
        double newPitch = pitch + rotator;
        double newYaw = yaw + rotator;
        double iZ = Math.cos(yaw) * Math.cos(newPitch) * girth;
        double iX = Math.sin(yaw) * Math.cos(newPitch) * girth;
        double iY = Math.sin(newPitch) * girth;
        double jZ = Math.cos(newYaw) * girth;
        double jX = Math.sin(newYaw) * girth;

        if (!RENDER_CABLE_HANG) {
            drawLineSegment(pose, buffer, x0, y0, z0, x1, y1, z1, iX, iY, iZ, jX, jZ, ownLight, color);
            return;
        }

        double hang = Math.min(Math.sqrt(dX * dX + dY * dY + dZ * dZ) / 15D, 2.5D);
        for (int j = 0; j < COUNT; j++) {
            int k = j + 1;
            double sagJ = Math.sin(j / (double) COUNT * Math.PI * 0.5) * hang;
            double sagK = Math.sin(k / (double) COUNT * Math.PI * 0.5) * hang;
            double deltaX = x1 - x0, deltaY = y1 - y0, deltaZ = z1 - z0;
            drawLineSegment(pose, buffer,
                    x0 + (deltaX * j / COUNT), y0 + (deltaY * j / COUNT) - sagJ, z0 + (deltaZ * j / COUNT),
                    x0 + (deltaX * k / COUNT), y0 + (deltaY * k / COUNT) - sagK, z0 + (deltaZ * k / COUNT),
                    iX, iY, iZ, jX, jZ, w.light()[j], color);
        }
    }

    /** 1.7 {@code drawLineSegment}: two crossed quads from one point to the next, the texture wrapping along them. */
    private static void drawLineSegment(PoseStack.Pose pose, VertexConsumer buffer, double x, double y, double z,
                                        double a, double b, double c, double iX, double iY, double iZ, double jX, double jZ,
                                        int light, int color) {
        double deltaX = a - x, deltaY = b - y, deltaZ = c - z;
        double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        int wrap = (int) Math.ceil(length * 8);

        if (deltaX + deltaZ < 0) {
            wrap *= -1;
            jZ *= -1;
            jX *= -1;
        }

        vertex(buffer, pose, x + iX, y + iY, z + iZ, 0, 0, light, color);
        vertex(buffer, pose, x - iX, y - iY, z - iZ, 0, 1, light, color);
        vertex(buffer, pose, a - iX, b - iY, c - iZ, wrap, 1, light, color);
        vertex(buffer, pose, a + iX, b + iY, c + iZ, wrap, 0, light, color);
        vertex(buffer, pose, x + jX, y, z + jZ, 0, 0, light, color);
        vertex(buffer, pose, x - jX, y, z - jZ, 0, 1, light, color);
        vertex(buffer, pose, a - jX, b, c - jZ, wrap, 1, light, color);
        vertex(buffer, pose, a + jX, b, c + jZ, wrap, 0, light, color);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, double x, double y, double z,
                               float u, float v, int light, int color) {
        // Lighting was off for the wires in 1.7. The flat render type ignores the normal on 26.1; below it
        // the stock entity shader still reads one, and straight up evaluates to exactly 1.0 (see
        // FlatCutoutRenderType), so the same vertex is flat on every node.
        Vertexes.vertex(buffer, pose, (float) x, (float) y, (float) z, color, u, v, OverlayTexture.NO_OVERLAY, light, 0F, 1F, 0F);
    }
}
