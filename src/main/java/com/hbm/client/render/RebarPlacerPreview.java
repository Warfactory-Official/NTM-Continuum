package com.hbm.client.render;

import com.hbm.items.ModDungeonItems;
import com.hbm.items.tool.ItemRebarPlacer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.client.Frames;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubLevelPoses;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.WorldEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 1.7 {@code BlockRebar.renderRebar}'s second half: while the held rebar placer has its first corner, the box it
 * would fill up to the cell in front of the looked-at face is outlined in white lines (inset by 1/8 on every side),
 * and the "rebar in inventory / rebar needed" count is shown as the cable-slot info tooltip, green when there is
 * enough and red when there is not. Client only.
 */
public final class RebarPlacerPreview {

    /** 1.7 {@code ServerProxy.ID_CABLE}. */
    private static final int ID_CABLE = 3;
    private static boolean initialized;

    private RebarPlacerPreview() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        WorldEffects.init();
        WorldEffects.onBegin(RebarPlacerPreview::submit);
    }

    private static void submit(CameraInfo camera) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof ItemRebarPlacer) || !ModDungeonItems.REBAR_POS.has(held)) return;
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = ModDungeonItems.REBAR_POS.getOr(held, BlockPos.ZERO);
        BlockPos at = hit.getBlockPos().relative(hit.getDirection());

        double minX = Math.min(pos.getX(), at.getX()) + 0.125, maxX = Math.max(pos.getX(), at.getX()) + 0.875;
        double minY = Math.min(pos.getY(), at.getY()) + 0.125, maxY = Math.max(pos.getY(), at.getY()) + 0.875;
        double minZ = Math.min(pos.getZ(), at.getZ()) + 0.125, maxZ = Math.max(pos.getZ(), at.getZ()) + 0.875;

        // Drawn relative to the looked-at cell through a pose that puts it where it is seen: camera-relative in the
        // world, or with the sub-level it is on (a Create Aeronautics ship keeps its blocks far from where it is drawn).
        // Core's hook model-view already carries the view rotation (see WorldEffects.onBegin).
        PoseStack pose = new PoseStack();
        SubLevelPoses.around(pose, mc.level, at, camera.pos, Frames.partialTick());
        float x0 = (float) (minX - at.getX()), x1 = (float) (maxX - at.getX());
        float y0 = (float) (minY - at.getY()), y1 = (float) (maxY - at.getY());
        float z0 = (float) (minZ - at.getZ()), z1 = (float) (maxZ - at.getZ());

        WorldEffects.submit(pose, CompatRenderTypes.lines(), 0, (p, buffer) -> {
            // top
            line(buffer, p, x0, y1, z0, x0, y1, z1);
            line(buffer, p, x0, y1, z1, x1, y1, z1);
            line(buffer, p, x1, y1, z1, x1, y1, z0);
            line(buffer, p, x1, y1, z0, x0, y1, z0);
            // bottom
            line(buffer, p, x0, y0, z0, x0, y0, z1);
            line(buffer, p, x0, y0, z1, x1, y0, z1);
            line(buffer, p, x1, y0, z1, x1, y0, z0);
            line(buffer, p, x1, y0, z0, x0, y0, z0);
            // sides
            line(buffer, p, x0, y0, z0, x0, y1, z0);
            line(buffer, p, x1, y0, z0, x1, y1, z0);
            line(buffer, p, x1, y0, z1, x1, y1, z1);
            line(buffer, p, x0, y0, z1, x0, y1, z1);
        });

        int rebarLeft = ItemRebarPlacer.countRebar(player);
        int rebarRequired = (Math.max(pos.getX(), at.getX()) - Math.min(pos.getX(), at.getX()) + 1)
                * (Math.max(pos.getY(), at.getY()) - Math.min(pos.getY(), at.getY()) + 1)
                * (Math.max(pos.getZ(), at.getZ()) - Math.min(pos.getZ(), at.getZ()) + 1);
        RenderInfoSystem.displayTooltip(Component.literal(rebarLeft + " / " + rebarRequired)
                .withStyle(rebarRequired > rebarLeft ? ChatFormatting.RED : ChatFormatting.GREEN), 1_000, ID_CABLE);
    }

    private static void line(VertexConsumer buffer, PoseStack.Pose pose, float ax, float ay, float az,
                             float bx, float by, float bz) {
        float nx = bx - ax, ny = by - ay, nz = bz - az;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0) {
            nx /= len;
            ny /= len;
            nz /= len;
        }
        Vertexes.line(buffer, pose, ax, ay, az, 0xFFFFFFFF, nx, ny, nz);
        Vertexes.line(buffer, pose, bx, by, bz, 0xFFFFFFFF, nx, ny, nz);
    }
}
