package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code BlockDummyable#drawPlacementHighlight}, called from {@code ModEventHandlerRenderer#onDrawHighlight}:
 * while the main hand holds a multiblock's item and the crosshair is on a block, the footprint it would occupy is
 * outlined from its occupied cells, including the held block-entity geometry, pulsing
 * green if it can be placed there and red if not, plus any {@link BlockMultiblockCore#getAABBExtras} boxes (blue unless
 * coloured) and a yellow square on each {@link BlockMultiblockCore#getAllPorts} face.
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

    private MultiblockPlacementHighlight() {
    }

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

    /**
     * The occupied cells and placement verdict submitted by the held-item highlight.
     */
    public record Preview(BlockPos core, Direction facing, boolean placeable, List<BlockPos> cells) {
        /**
         * The same occupied cells, expressed in the core's local frame.
         */
        public VoxelShape shape() {
            var shape = Shapes.empty();
            for (var pos : cells) {
                int x = pos.getX() - core.getX();
                int y = pos.getY() - core.getY();
                int z = pos.getZ() - core.getZ();
                shape = Shapes.joinUnoptimized(shape, Shapes.box(x, y, z, x + 1, y + 1, z + 1), BooleanOp.OR);
            }
            return shape.optimize();
        }
    }

    public static @Nullable Preview extract(Level level, BlockHitResult hit, @Nullable Player player) {
        if (player == null || hit.getType() != HitResult.Type.BLOCK) return null;
        var block = heldMultiblock(player);
        return block == null ? null : extract(level, hit, player, block);
    }

    private static Preview extract(Level level, BlockHitResult mop, Player player, BlockMultiblockCore block) {
        // 1.7 always quartered 360 degrees here, even for a block that places on a finer yaw step
        // (the large pylon's placementYawSpan); kept as it was.
        int i = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        int o = -block.getOffset();
        BlockPos hit = mop.getBlockPos();
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

        int originX = hit.getX() + fd.offsetX * o + sideHit.getStepX();
        int originY = pY + sideHit.getStepY();
        int originZ = hit.getZ() + fd.offsetZ * o + sideHit.getStepZ();

        boolean canPlace;
        if (BlockMultiblockCore.wallPlacement) {
            // hbm-space's highlight: the same wall/ceiling placement as BlockMultiblockCore.placement (V-175)
            BlockMultiblockCore.Placement layout = block.placement(hit.relative(sideHit), sideHit, player.getYRot());
            facing = layout.facing();
            fd = ForgeDirection.getOrientation(facing);
            originX = layout.core().getX();
            originY = layout.core().getY();
            originZ = layout.core().getZ();
            canPlace = block.canPlace(level, layout, player.getMainHandItem());
        } else {
            canPlace = block.canPlaceAt(level,
                    new BlockPos(hit.getX() + sideHit.getStepX(), pY + sideHit.getStepY(),
                            hit.getZ() + sideHit.getStepZ()),
                    facing, player.getMainHandItem());
        }

        List<BlockPos> blocks = new ArrayList<>();
        var core = new BlockPos(originX, originY, originZ);
        var seen = new LongOpenHashSet();
        seen.add(core.asLong());
        blocks.add(core);
        block.visitPlacementCells(core, facing, player.getMainHandItem(), (pos, ignored) -> {
            if (seen.add(pos.asLong())) blocks.add(pos.immutable());
        });
        return new Preview(core, facing, canPlace, blocks);
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

        var preview = extract(mc.level, mop, player, block);
        BlockPos hit = mop.getBlockPos();
        // Everything is drawn relative to the block under the crosshair, through a pose that puts it where it is seen:
        // camera-relative in the world, or with the sub-level it stands on (a Create Aeronautics ship keeps its blocks
        // far from where it is drawn). 1.7 subtracted the camera position from each vertex instead.
        PoseStack placement = new PoseStack();
        SubLevelPoses.around(placement, mc.level, hit, camera.pos, Frames.partialTick());
        double dX = hit.getX();
        double dY = hit.getY();
        double dZ = hit.getZ();
        int originX = preview.core().getX();
        int originY = preview.core().getY();
        int originZ = preview.core().getZ();
        int o = -block.getOffset();
        ForgeDirection fdFinal = ForgeDirection.getOrientation(preview.facing());
        ForgeDirection lateralFinal = fdFinal.getRotation(ForgeDirection.UP);

        double timer = (System.currentTimeMillis() % (1000D * Math.PI)) / 250D;
        double sine = Math.sin(timer);
        int color = (int) (255 * (sine * 0.25 + 0.75));
        int footprintColor = preview.placeable() ? 0xFF000000 | color << 8 : 0xFF000000 | color << 16;
        int extraColor = 0xFF000000 | color;
        int portColor = 0xFF000000 | color << 16 | color << 8;

        List<BlockPos> blocks = preview.cells();
        LongOpenHashSet set = new LongOpenHashSet();
        for (var pos : blocks) set.add(pos.asLong());
        double[][] extras = block.getAABBExtras();
        int[][] ports = block.getAllPorts(fdFinal);
        int portX = originX - fdFinal.offsetX * o;
        int portY = originY;
        int portZ = originZ - fdFinal.offsetZ * o;
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

            // "boo-yeah": the extra boxes, in blue, a heat port's orange (1) or their own RGB.
            for (double[] extra : extras) {
                double upr = extra[0];
                double lwr = extra[1];
                double fwd = extra[2];
                double bwd = extra[3];
                double lft = extra[4];
                double rgt = extra[5];

                float x0 = (float) (cX + fwd * fdFinal.offsetX + lft * lateralFinal.offsetX);
                float x1 = (float) (cX + bwd * fdFinal.offsetX + rgt * lateralFinal.offsetX);
                float y0 = (float) (cY + lwr);
                float y1 = (float) (cY + upr);
                float z0 = (float) (cZ + fwd * fdFinal.offsetZ + lft * lateralFinal.offsetZ);
                float z1 = (float) (cZ + bwd * fdFinal.offsetZ + rgt * lateralFinal.offsetZ);
                int c = extra.length <= 6 ? extraColor
                        : extra[6] == 1 ? 0xFFFFA500 : 0xFF000000 | (int) Math.round(extra[6]);

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

            // 1.7's port faces: a 0.6 square on the face each port opens to, pulsing yellow.
            for (int[] port : ports) {
                float jX = (float) (portX + port[0] - dX);
                float jY = (float) (portY + port[1] - dY);
                float jZ = (float) (portZ + port[2] - dZ);
                ForgeDirection dir = ForgeDirection.getOrientation(port[3]);
                if (dir == ForgeDirection.UP || dir == ForgeDirection.DOWN) {
                    float y = jY + (dir.offsetY > 0 ? 1 : 0);
                    square(pose, buffer, jX + 0.5F, y, jZ + 0.5F, Direction.Axis.Y, portColor);
                } else if (dir == ForgeDirection.EAST || dir == ForgeDirection.WEST) {
                    float x = jX + (dir.offsetX > 0 ? 1 : 0);
                    square(pose, buffer, x, jY + 0.5F, jZ + 0.5F, Direction.Axis.X, portColor);
                } else {
                    float z = jZ + (dir.offsetZ > 0 ? 1 : 0);
                    square(pose, buffer, jX + 0.5F, jY + 0.5F, z, Direction.Axis.Z, portColor);
                }
            }
        });
    }

    /**
     * The outline of a 0.6-wide square centred on {@code (x, y, z)}, flat across {@code axis}.
     */
    private static void square(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z,
                               Direction.Axis axis, int color) {
        float s = 0.3F;
        float[][] corners = switch (axis) {
            case Y -> new float[][]{{x - s, y, z - s}, {x + s, y, z - s}, {x + s, y, z + s}, {x - s, y, z + s}};
            case X -> new float[][]{{x, y - s, z - s}, {x, y + s, z - s}, {x, y + s, z + s}, {x, y - s, z + s}};
            case Z -> new float[][]{{x - s, y - s, z}, {x + s, y - s, z}, {x + s, y + s, z}, {x - s, y + s, z}};
        };
        for (int i = 0; i < 4; i++) {
            float[] a = corners[i];
            float[] b = corners[(i + 1) % 4];
            line(pose, buffer, a[0], a[1], a[2], b[0], b[1], b[2], color);
        }
    }

    /**
     * One GL_LINES segment. A zero-length one (a flat extra box) drew nothing in 1.7 and has no direction here.
     */
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
