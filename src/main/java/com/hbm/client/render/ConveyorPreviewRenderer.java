package com.hbm.client.render;

import com.hbm.blocks.network.BlockConveyorBendable;
import com.hbm.items.tool.ItemConveyorWand;
import com.hbm.util.ConveyorPlacer;
import com.hbm.util.ConveyorRoute;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.client.Frames;
import dev.golem.core.render.GhostBlocks;
import dev.golem.core.render.SubLevelPoses;
import dev.golem.core.render.WorldEffects;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code RenderOverhead.renderActionPreview}: once the conveyor wand has its first click, the run it would
 * lay to the block under the crosshair is drawn as a ghost — cyan where the route lands, red where it is refused.
 *
 * <p>1.7 drew that ghost under {@code Tessellator.disableColor()} and one {@code glColor3f}, which discards
 * ambient occlusion, face shade and tints alike, with the lightmap unit off for the whole of
 * {@code RenderWorldLastEvent}. {@link GhostBlocks} is that state; {@link WorldEffects} is that timing.
 *
 * <p>The route is walked by the same {@link ConveyorRoute#construct} the server commits with, into a scratch
 * {@link Volume} (1.7's {@code WorldInAJar}), so the preview and the placement agree by construction. Client only.
 */
public final class ConveyorPreviewRenderer {

    // 1.7: glColor3f(0, 1, 1) where the route lands, glColor3f(1, 0, 0) where it does not.
    private static final int LAID = 0xFF00FFFF;
    private static final int REFUSED = 0xFFFF0000;

    /** 1.7 {@code onUpdate}: the route is re-walked only once the aim has moved this far. */
    private static final float YAW_STEP = 15F;

    /** Replaced wholesale on every bake, so the deferred draw can hold the reference without copying it. */
    private static List<Cell> cells = List.of();

    private static int color = LAID;
    private static ItemConveyorWand.@Nullable Run lastRun;
    private static @Nullable BlockPos lastHit;
    private static @Nullable Direction lastSide;
    private static float lastYaw;
    private static boolean initialized;

    private ConveyorPreviewRenderer() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        WorldEffects.init();
        WorldEffects.onBegin(ConveyorPreviewRenderer::submit);
        dev.golem.core.client.ClientResourceReloads.register(com.hbm.Constants.MOD_ID, "conveyor_preview",
                ConveyorPreviewRenderer::clear);
    }

    private static void submit(CameraInfo camera) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) {
            clear();
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof ItemConveyorWand)) {
            clear();
            return;
        }

        // The first click is stored server-side in the stack; it reaches us with the ordinary stack sync.
        ItemConveyorWand.Run run = ItemConveyorWand.pendingRun(held);
        if (run == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            clear();
            return;
        }

        route(level, player, held, run, hit);
        draw(level, camera);
    }

    private static void clear() {
        cells = List.of();
        lastRun = null;
        lastHit = null;
        lastSide = null;
    }

    private static void route(ClientLevel level, LocalPlayer player, ItemStack held, ItemConveyorWand.Run run,
                              BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        Direction side = hit.getDirection();

        // 1.7 / ItemConveyorWand.useOn: with a run pending, clicking a bendable belt's body snaps to its INPUT end
        if (level.getBlockState(pos).getBlock() instanceof BlockConveyorBendable bendable) {
            Direction moveDir = bendable.getInputDirection(level, pos);
            if (level.getBlockState(pos.relative(moveDir)).canBeReplaced()) side = moveDir;
        }

        if (run.equals(lastRun) && pos.equals(lastHit) && side == lastSide
                && Math.abs(lastYaw - player.getYRot()) < YAW_STEP) {
            return;
        }
        lastRun = run;
        lastHit = pos;
        lastSide = side;
        lastYaw = player.getYRot();

        Volume volume = new Volume();
        int laid = ConveyorRoute.construct(level, volume, ItemConveyorWand.getType(held), player, run.anchor(),
                run.side(), pos, side, run.budget());
        // 1.7 writes as it walks, so a run that is refused still shows every cell it reached first.
        color = laid > 0 ? LAID : REFUSED;
        bake(volume);
    }

    private static void bake(Volume volume) {
        List<Cell> baked = new ArrayList<>(volume.cells.size());
        for (Long2ObjectMap.Entry<BlockState> entry : volume.cells.long2ObjectEntrySet()) {
            BlockPos pos = BlockPos.of(entry.getLongKey());
            // 1.7's ISBRH read the jar's neighbours at draw time, so a lift laid before the belt above it still
            // showed as a middle piece; settle every cell against the finished run for the same effect.
            BlockState state = ConveyorRoute.settle(volume, pos, entry.getValue());
            baked.add(new Cell(pos, GhostBlocks.bake(state, pos, color)));
        }
        cells = List.copyOf(baked);
    }

    private static void draw(ClientLevel level, CameraInfo camera) {
        List<Cell> run = cells;
        if (run.isEmpty()) return;

        Vec3 eye = camera.pos;
        float partialTick = Frames.partialTick();
        // Frame-start (onBegin) submissions are drawn under Core's hook model-view, which already is the frame's view
        // rotation on every node: the pose stays world-axis and camera-relative (applying viewRotation() here as
        // well rotated the ghost run twice, off into the sky).
        PoseStack pose = new PoseStack();

        // ONE submission for the whole run, as the 1.7 preview was one tessellator pass.
        WorldEffects.submit(pose, GhostBlocks.renderType(), 0, (drawPose, buffer) -> {
            PoseStack local = new PoseStack();
            for (Cell cell : run) {
                local.last().pose().set(drawPose.pose());
                local.last().normal().set(drawPose.normal());
                // where the cell is seen: camera-relative, or with the sub-level it is laid on (an Aeronautics ship)
                SubLevelPoses.around(local, level, cell.pos(), eye, partialTick);
                cell.geometry().emit(local.last(), buffer);
            }
        });
    }

    private record Cell(BlockPos pos, GhostBlocks.Geometry geometry) {}

    /**
     * 1.7 {@code WorldInAJar} as the wand asks for it. 1.7 sized a dense box with a one-block margin for the
     * run's overshoot; the cells are the box here, so a route leaving that margin is drawn rather than clipped.
     * Reads answer air everywhere but the laid cells, so lift parts and chute connections see only the run.
     */
    private static final class Volume implements ConveyorPlacer {

        private final Long2ObjectLinkedOpenHashMap<BlockState> cells = new Long2ObjectLinkedOpenHashMap<>();

        @Override
        public void place(BlockPos pos, BlockState state) {
            cells.put(pos.asLong(), state);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            BlockState state = cells.get(pos.asLong());
            return state == null ? Blocks.AIR.defaultBlockState() : state;
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public int getHeight() {
            return 0;
        }

        @Override
        public int getMinY() {
            return 0;
        }
    }
}
