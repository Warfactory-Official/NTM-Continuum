package com.hbm.client.render.projectile;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.impl.ItemGunChargeThrower;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.Renderers;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import java.util.UUID;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code LegoClient.RENDER_CT_HOOK}: the hook model along the hook's own angles, and while a player holds the
 * charge thrower that fired it, {@code renderWire}'s sagging grey wire from the hook to that player's launcher.
 */
final class HookWire implements TracerRegistry.BulletTracer {

    private static final Identifier WIRE = Ids.of("hbm", "textures/models/network/wire_greyscale.png");
    private static final int COUNT = 10;
    private static final int COLOR = 0xFF606060;

    private final TracerRegistry.BulletTracer hook;

    HookWire(TracerRegistry.BulletTracer hook) {
        this.hook = hook;
    }

    @Override
    public void extract(EntityBulletBaseMK4 bullet, BulletProjectileRenderer.State state, float partialTicks) {
        state.wire = false;
        Player holder = holder(bullet);
        if (holder == null) return;
        Vec3 at = bullet.getPosition(partialTicks);
        double yaw = Mth.lerp(partialTicks, holder.yRotO, holder.getYRot());
        double pitch = Mth.lerp(partialTicks, holder.xRotO, holder.getXRot());
        Vec3 offset = new Vec3(0.125D, 0.25D, -0.75D)
                .xRot((float) -pitch / 180F * (float) Math.PI)
                .yRot((float) -yaw / 180F * (float) Math.PI);
        Vec3 eye = holder.getPosition(partialTicks).add(0D, holder.getEyeHeight(), 0D).subtract(offset);
        state.wire = true;
        state.wireX = eye.x - at.x;
        state.wireY = eye.y - at.y;
        state.wireZ = eye.z - at.z;
        double hang = Math.min(Math.sqrt(state.wireX * state.wireX + state.wireY * state.wireY
                + state.wireZ * state.wireZ) / 15D, 0.5D);
        state.wireLight = new int[COUNT];
        for (int j = 0; j < COUNT; j++) {
            double sagMean = (Math.sin(j / (double) COUNT * Math.PI) + Math.sin((j + 1) / (double) COUNT * Math.PI))
                    / 2D * hang;
            double ja = j + 0.5D;
            BlockPos light = BlockPos.containing(at.x + state.wireX / COUNT * ja,
                    at.y + state.wireY / COUNT * ja - sagMean, at.z + state.wireZ / COUNT * ja);
            state.wireLight[j] = Renderers.lightAt(bullet.level(), light);
        }
    }

    /** The player whose held charge thrower last fired this hook, as 1.7 compares {@code getLastHook}. */
    private static Player holder(EntityBulletBaseMK4 bullet) {
        for (Player player : bullet.level().players()) {
            if (!(player.getMainHandItem().getItem() instanceof ItemGunChargeThrower)) continue;
            UUID hook = ItemGunChargeThrower.getLastHook(player.getMainHandItem());
            if (bullet.getUUID().equals(hook)) return player;
        }
        return null;
    }

    @Override
    public void draw(BulletProjectileRenderer.State state, PoseStack pose, SubmitSink sink) {
        // the hook config skips the renderer's rotations, so 1.7 turns the model itself
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw - 90F));
        pose.mulPose(Axis.ZP.rotationDegrees(state.pitch + 180F));
        hook.draw(state, pose, sink);
        pose.popPose();
        if (!state.wire) return;
        double dX = state.wireX;
        double dY = state.wireY;
        double dZ = state.wireZ;
        double hang = Math.min(Math.sqrt(dX * dX + dY * dY + dZ * dZ) / 15D, 0.5D);
        double girth = 0.03125D;
        double yaw = Math.atan2(dX, dZ);
        double pitch = Math.atan2(dY, Math.sqrt(dX * dX + dZ * dZ));
        double newPitch = pitch + Math.PI * 0.5D;
        double newYaw = yaw + Math.PI * 0.5D;
        double iZ = Math.cos(yaw) * Math.cos(newPitch) * girth;
        double iX = Math.sin(yaw) * Math.cos(newPitch) * girth;
        double iY = Math.sin(newPitch) * girth;
        double jZ = Math.cos(newYaw) * girth;
        double jX = Math.sin(newYaw) * girth;
        int[] light = state.wireLight;
        sink.submitCustomGeometry(pose, CompatRenderTypes.flatCutout(WIRE), (p, buffer) -> {
            for (int j = 0; j < COUNT; j++) {
                int k = j + 1;
                double sagJ = Math.sin(j / (double) COUNT * Math.PI) * hang;
                double sagK = Math.sin(k / (double) COUNT * Math.PI) * hang;
                segment(p, buffer, dX * j / COUNT, dY * j / COUNT - sagJ, dZ * j / COUNT,
                        dX * k / COUNT, dY * k / COUNT - sagK, dZ * k / COUNT, iX, iY, iZ, jX, jZ, light[j]);
            }
        });
    }

    /** 1.7 {@code drawLineSegment}: two crossed quads, the texture wrapping eight times a block. */
    private static void segment(PoseStack.Pose pose, VertexConsumer buffer, double x, double y, double z,
            double a, double b, double c, double iX, double iY, double iZ, double jX, double jZ, int light) {
        double deltaX = a - x;
        double deltaY = b - y;
        double deltaZ = c - z;
        int wrap = (int) Math.ceil(Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) * 8);
        if (deltaX + deltaZ < 0) {
            wrap *= -1;
            jZ *= -1;
            jX *= -1;
        }
        vertex(buffer, pose, x + iX, y + iY, z + iZ, 0, 0, light);
        vertex(buffer, pose, x - iX, y - iY, z - iZ, 0, 1, light);
        vertex(buffer, pose, a - iX, b - iY, c - iZ, wrap, 1, light);
        vertex(buffer, pose, a + iX, b + iY, c + iZ, wrap, 0, light);
        vertex(buffer, pose, x + jX, y, z + jZ, 0, 0, light);
        vertex(buffer, pose, x - jX, y, z - jZ, 0, 1, light);
        vertex(buffer, pose, a - jX, b, c - jZ, wrap, 1, light);
        vertex(buffer, pose, a + jX, b, c + jZ, wrap, 0, light);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, double x, double y, double z,
            float u, float v, int light) {
        // 1.7 draws the wire unlit; straight-up normals read as full brightness on every node
        Vertexes.vertex(buffer, pose, (float) x, (float) y, (float) z, COLOR, u, v, OverlayTexture.NO_OVERLAY, light,
                0F, 1F, 0F);
    }
}
