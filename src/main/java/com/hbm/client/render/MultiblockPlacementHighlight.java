package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.lib.ForgeDirection;
import com.hbm.util.EntityDamageUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.render.BlockOutlines;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.client.Frames;
import dev.golem.core.render.LineRenderTypes;
import dev.golem.core.render.SubLevelPoses;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.WorldEffects;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code BlockDummyable#drawPlacementHighlight}, called from {@code ModEventHandlerRenderer#onDrawHighlight}:
 * while the main hand holds a multiblock's item and the crosshair is on a block, the footprint it would occupy is
 * outlined (only the outer edges of the union of its {@link BlockMultiblockCore#getAllDimensions} boxes), pulsing
 * green if it can be placed there and red if not, plus any {@link BlockMultiblockCore#getAABBExtras} boxes in blue.
 * Vanilla's own block outline is suppressed for as long as the item is held, as 1.7 cancelled the event.
 *
 * <p>Lines are 2 px wide ({@code glLineWidth(2)}), full-bright and untextured. 1.7 also turned depth writes off;
 * Core's line type writes depth, which only matters where a later translucent draw passes behind a line.
 * Frame-start submissions already sit under the view rotation, so the pose is world-axis and camera-relative
 * (see {@code ConveyorPreviewRenderer.draw}).
 */
public final class MultiblockPlacementHighlight {

    private static final float LINE_WIDTH = 2F;
    private static boolean initialized;

    private MultiblockPlacementHighlight() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        WorldEffects.init();
        WorldEffects.onBegin(MultiblockPlacementHighlight::submit);
        // 1.7 cancelled the highlight event whenever the held ItemBlock's block was a BlockDummyable, whether or
        // not its own trace then found a block.
        BlockOutlines.suppressWhen(() -> {
            Player player = Minecraft.getInstance().player;
            return player != null && heldMultiblock(player) != null;
        });
    }

    private static @Nullable BlockMultiblockCore heldMultiblock(Player player) {
        return player.getMainHandItem().getItem() instanceof BlockItem item
                && item.getBlock() instanceof BlockMultiblockCore block ? block : null;
    }

    private static void submit(CameraInfo camera) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;
        // DrawBlockHighlightEvent's own gates: something under the crosshair, GUI shown, a player's view.
        if (mc.options.hideGui || !(mc.getCameraEntity() instanceof Player)) return;
        if (mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.MISS) return;
        BlockMultiblockCore block = heldMultiblock(player);
        if (block == null) return;

        HitResult trace = EntityDamageUtil.getMouseOver(player, 5.0D);
        if (!(trace instanceof BlockHitResult mop) || mop.getType() != HitResult.Type.BLOCK) return;

        // 1.7 always quartered 360 degrees here, even for a block that places on a finer yaw step
        // (the large pylon's placementYawSpan); kept as it was.
        int i = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        int o = -block.getOffset();
        BlockPos hit = mop.getBlockPos();
        // Everything is drawn relative to the block under the crosshair, through a pose that puts it where it is seen:
        // camera-relative in the world, or with the sub-level it stands on (a Create Aeronautics ship keeps its blocks
        // far from where it is drawn). 1.7 subtracted the camera position from each vertex instead.
        PoseStack placement = new PoseStack();
        SubLevelPoses.around(placement, mc.level, hit, camera.pos, Frames.partialTick());
        double dX = hit.getX();
        double dY = hit.getY();
        double dZ = hit.getZ();
        int pY = hit.getY() + block.getHeightOffset();

        Direction facing = switch (i) {
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            default -> Direction.NORTH;
        };
        Direction sideHit = mop.getDirection();
        facing = block.getDirModified(facing);
        ForgeDirection fd = ForgeDirection.getOrientation(facing);
        ForgeDirection lateral = fd.getRotation(ForgeDirection.UP);

        int originX = hit.getX() + fd.offsetX * o + sideHit.getStepX();
        int originY = pY + sideHit.getStepY();
        int originZ = hit.getZ() + fd.offsetZ * o + sideHit.getStepZ();

        boolean canPlace = block.canPlaceAt(mc.level,
                new BlockPos(hit.getX() + sideHit.getStepX(), pY + sideHit.getStepY(), hit.getZ() + sideHit.getStepZ()),
                facing);

        double timer = (System.currentTimeMillis() % (1000D * Math.PI)) / 250D;
        double sine = Math.sin(timer);
        int color = (int) (255 * (sine * 0.25 + 0.75));
        int footprintColor = canPlace ? 0xFF000000 | color << 8 : 0xFF000000 | color << 16;
        int extraColor = 0xFF000000 | color;

        // Every cell of every box; the set answers the neighbour tests that decide which edges are outer.
        List<BlockPos> blocks = new ArrayList<>();
        LongOpenHashSet set = new LongOpenHashSet();
        for (int[] dims : block.getAllDimensions()) {
            int offFwd = dims.length > 6 ? dims[6] : 0;
            int offUp = dims.length > 7 ? dims[7] : 0;
            int offLat = dims.length > 8 ? dims[8] : 0;
            int worldOffY = offUp;
            int worldOffX = fd.offsetX * offFwd + lateral.offsetX * offLat;
            int worldOffZ = fd.offsetZ * offFwd + lateral.offsetZ * offLat;

            int[] rot = MultiblockHandlerXR.rotate(dims, facing);
            for (int bx = -rot[4] + worldOffX; bx <= rot[5] + worldOffX; bx++)
                for (int by = -rot[1] + worldOffY; by <= rot[0] + worldOffY; by++)
                    for (int bz = -rot[2] + worldOffZ; bz <= rot[3] + worldOffZ; bz++) {
                        BlockPos bp = new BlockPos(originX + bx, originY + by, originZ + bz);
                        blocks.add(bp);
                        set.add(bp.asLong());
                    }
        }

        double[][] extras = block.getAABBExtras();
        double cX = originX - dX + 0.5;
        double cY = originY - dY;
        double cZ = originZ - dZ + 0.5;

        WorldEffects.submit(placement, LineRenderTypes.lines(LINE_WIDTH), 2, (pose, buffer) -> {
            for (BlockPos pos : blocks) {
                boolean px = set.contains(BlockPos.asLong(pos.getX() + 1, pos.getY(), pos.getZ()));
                boolean nx = set.contains(BlockPos.asLong(pos.getX() - 1, pos.getY(), pos.getZ()));
                boolean ppy = set.contains(BlockPos.asLong(pos.getX(), pos.getY() + 1, pos.getZ()));
                boolean ny = set.contains(BlockPos.asLong(pos.getX(), pos.getY() - 1, pos.getZ()));
                boolean ppz = set.contains(BlockPos.asLong(pos.getX(), pos.getY(), pos.getZ() + 1));
                boolean nz = set.contains(BlockPos.asLong(pos.getX(), pos.getY(), pos.getZ() - 1));

                float minX = (float) (pos.getX() - dX);
                float maxX = (float) (pos.getX() + 1 - dX);
                float minY = (float) (pos.getY() - dY);
                float maxY = (float) (pos.getY() + 1 - dY);
                float minZ = (float) (pos.getZ() - dZ);
                float maxZ = (float) (pos.getZ() + 1 - dZ);
                int c = footprintColor;

                // 1.7's six face groups verbatim (Mellow's edge walk); shared edges are drawn more than once.
                if (!ppy) {
                    if (!nx) line(pose, buffer, minX, maxY, minZ, minX, maxY, maxZ, c);
                    if (!ppz) line(pose, buffer, minX, maxY, maxZ, maxX, maxY, maxZ, c);
                    if (!px) line(pose, buffer, maxX, maxY, maxZ, maxX, maxY, minZ, c);
                    if (!nz) line(pose, buffer, maxX, maxY, minZ, minX, maxY, minZ, c);
                }
                if (!ny) {
                    if (!nx) line(pose, buffer, minX, minY, minZ, minX, minY, maxZ, c);
                    if (!ppz) line(pose, buffer, minX, minY, maxZ, maxX, minY, maxZ, c);
                    if (!px) line(pose, buffer, maxX, minY, maxZ, maxX, minY, minZ, c);
                    if (!nz) line(pose, buffer, maxX, minY, minZ, minX, minY, minZ, c);
                }
                if (!nz) {
                    if (!nx) line(pose, buffer, minX, minY, minZ, minX, maxY, minZ, c);
                    if (!ppy) line(pose, buffer, minX, maxY, minZ, maxX, maxY, minZ, c);
                    if (!px) line(pose, buffer, maxX, maxY, minZ, maxX, minY, minZ, c);
                    if (!ny) line(pose, buffer, maxX, minY, minZ, minX, minY, minZ, c);
                }
                if (!ppz) {
                    if (!nx) line(pose, buffer, minX, minY, maxZ, minX, maxY, maxZ, c);
                    if (!ppy) line(pose, buffer, minX, maxY, maxZ, maxX, maxY, maxZ, c);
                    if (!px) line(pose, buffer, maxX, maxY, maxZ, maxX, minY, maxZ, c);
                    if (!ny) line(pose, buffer, maxX, minY, maxZ, minX, minY, maxZ, c);
                }
                if (!nx) {
                    if (!nz) line(pose, buffer, minX, minY, minZ, minX, maxY, minZ, c);
                    if (!ppy) line(pose, buffer, minX, maxY, minZ, minX, maxY, maxZ, c);
                    if (!ppz) line(pose, buffer, minX, maxY, maxZ, minX, minY, maxZ, c);
                    if (!ny) line(pose, buffer, minX, minY, maxZ, minX, minY, minZ, c);
                }
                if (!px) {
                    if (!nz) line(pose, buffer, maxX, minY, minZ, maxX, maxY, minZ, c);
                    if (!ppy) line(pose, buffer, maxX, maxY, minZ, maxX, maxY, maxZ, c);
                    if (!ppz) line(pose, buffer, maxX, maxY, maxZ, maxX, minY, maxZ, c);
                    if (!ny) line(pose, buffer, maxX, minY, maxZ, maxX, minY, minZ, c);
                }
            }

            // "boo-yeah": the extra boxes, in blue.
            for (double[] extra : extras) {
                double upr = extra[0];
                double lwr = extra[1];
                double fwd = extra[2];
                double bwd = extra[3];
                double lft = extra[4];
                double rgt = extra[5];

                float x0 = (float) (cX + fwd * fd.offsetX + lft * lateral.offsetX);
                float x1 = (float) (cX + bwd * fd.offsetX + rgt * lateral.offsetX);
                float y0 = (float) (cY + lwr);
                float y1 = (float) (cY + upr);
                float z0 = (float) (cZ + fwd * fd.offsetZ + lft * lateral.offsetZ);
                float z1 = (float) (cZ + bwd * fd.offsetZ + rgt * lateral.offsetZ);
                int c = extraColor;

                line(pose, buffer, x0, y0, z0, x0, y0, z1, c);
                line(pose, buffer, x1, y0, z0, x1, y0, z1, c);
                line(pose, buffer, x0, y0, z0, x1, y0, z0, c);
                line(pose, buffer, x0, y0, z1, x1, y0, z1, c);

                line(pose, buffer, x0, y1, z0, x0, y1, z1, c);
                line(pose, buffer, x1, y1, z0, x1, y1, z1, c);
                line(pose, buffer, x0, y1, z0, x1, y1, z0, c);
                line(pose, buffer, x0, y1, z1, x1, y1, z1, c);

                line(pose, buffer, x0, y0, z0, x0, y1, z0, c);
                line(pose, buffer, x1, y0, z0, x1, y1, z0, c);
                line(pose, buffer, x0, y0, z1, x0, y1, z1, c);
                line(pose, buffer, x1, y0, z1, x1, y1, z1, c);
            }
        });
    }

    /** One GL_LINES segment. A zero-length one (a flat extra box) drew nothing in 1.7 and has no direction here. */
    private static void line(PoseStack.Pose pose, VertexConsumer buffer,
                             float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-6F) return;
        float nx = dx / length;
        float ny = dy / length;
        float nz = dz / length;
        Vertexes.line(buffer, pose, x1, y1, z1, color, nx, ny, nz, LINE_WIDTH);
        Vertexes.line(buffer, pose, x2, y2, z2, color, nx, ny, nz, LINE_WIDTH);
    }
}
