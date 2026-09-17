package com.hbm.client.render.weapon;

import com.hbm.items.weapon.sedna.factory.NtmToolGunCatalog;
import com.hbm.items.weapon.sedna.factory.XFactoryDrill;
import com.hbm.util.EntityDamageUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.client.Colors;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.WorldEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Port of the original {@code drawBlockHighlight}: the powered drill outlines the block it will break
 * (dark red) and, with an area-of-effect drillbit, the whole area (black inner, dark red outer).
 *
 * <p>Client only. Submitted at world-frame start through {@link WorldEffects}, in world orientation.
 * The original also cancelled vanilla's own outline; there is no loader-neutral seam for that yet, so
 * vanilla's outline still draws underneath.
 */
public final class DrillHighlightClient {

    private static boolean initialized;

    private DrillHighlightClient() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        WorldEffects.init();
        WorldEffects.onBegin(DrillHighlightClient::submit);
    }

    private static void submit(CameraInfo camera) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack drill = player.getMainHandItem();
        if (!drill.is(NtmToolGunCatalog.gun_drill.get())) return;

        HitResult mop = EntityDamageUtil.getMouseOver(player, XFactoryDrill.getModdableReach(drill, 5.0D));
        if (!(mop instanceof BlockHitResult blockHit)) return;
        BlockPos pos = blockHit.getBlockPos();
        int aoe = player.isShiftKeyDown() ? 0 : XFactoryDrill.getModdableAoE(drill, 1);
        float exp = 0.002F;

        PoseStack pose = new PoseStack();
        pose.translate(pos.getX() - camera.pos.x, pos.getY() - camera.pos.y, pos.getZ() - camera.pos.z);
        // WorldEffects replays the camera rotation around this pose; cancel it so the box stays world-aligned.
        pose.mulPose(new org.joml.Quaternionf(camera.orientation).conjugate());
        submitOutline(pose, -exp, -exp, -exp, 1F + exp, 1F + exp, 1F + exp,
                aoe > 0 ? Colors.black(102) : 0xFF800000);
        if (aoe > 0) submitOutline(pose, -aoe - exp, -aoe - exp, -aoe - exp,
                1F + aoe + exp, 1F + aoe + exp, 1F + aoe + exp, 0xFF800000);
    }

    private static void submitOutline(PoseStack poseStack, float minX, float minY, float minZ,
                                      float maxX, float maxY, float maxZ, int color) {
        WorldEffects.submit(poseStack, CompatRenderTypes.lines(), 2, (pose, buffer) -> {
            edge(pose, buffer, minX, minY, minZ, maxX, minY, minZ, color);
            edge(pose, buffer, minX, minY, maxZ, maxX, minY, maxZ, color);
            edge(pose, buffer, minX, maxY, minZ, maxX, maxY, minZ, color);
            edge(pose, buffer, minX, maxY, maxZ, maxX, maxY, maxZ, color);
            edge(pose, buffer, minX, minY, minZ, minX, maxY, minZ, color);
            edge(pose, buffer, maxX, minY, minZ, maxX, maxY, minZ, color);
            edge(pose, buffer, minX, minY, maxZ, minX, maxY, maxZ, color);
            edge(pose, buffer, maxX, minY, maxZ, maxX, maxY, maxZ, color);
            edge(pose, buffer, minX, minY, minZ, minX, minY, maxZ, color);
            edge(pose, buffer, maxX, minY, minZ, maxX, minY, maxZ, color);
            edge(pose, buffer, minX, maxY, minZ, minX, maxY, maxZ, color);
            edge(pose, buffer, maxX, maxY, minZ, maxX, maxY, maxZ, color);
        });
    }

    private static void edge(PoseStack.Pose pose, VertexConsumer buffer,
                             float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        Vertexes.color(buffer, pose, x1, y1, z1, color);
        Vertexes.color(buffer, pose, x2, y2, z2, color);
    }
}
