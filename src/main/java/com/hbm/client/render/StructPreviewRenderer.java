package com.hbm.client.render;

import com.hbm.blocks.machine.struct.StructPattern;
import com.hbm.tileentity.machine.struct.TileEntityStructBase;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Draws the still-unbuilt part of a {@link StructPattern} as translucent ghost cubes, replacing
 * 1.12.2's {@code RenderWatzMultiblock} / {@code RenderFusionTorusMultiblock}. Those two hard-coded
 * their machine's shape; this one reads it from the pattern, so every struct-built multiblock gets a
 * preview for free.
 *
 * <p>One deliberate improvement over upstream, which redrew every cell of the shape unconditionally:
 * cells that already hold the right block are skipped. Ghosting over a real block would z-fight, and
 * the preview reads much better when it shrinks as you build.
 */
public class StructPreviewRenderer<T extends TileEntityStructBase> implements CompatBER<T, StructPreviewRenderState> {

    /** Ghosts ignore world light — a half-built reactor pit is dark and the preview has to be readable. */
    private static final int FULL_BRIGHT = 0xF000F0;
    /** Upstream's ghost alpha (its {@code GlStateManager.color(1, 1, 1, 0.75F)}). */
    private static final float ALPHA = 0.75F;

    @Override
    public StructPreviewRenderState createRenderState() {
        return new StructPreviewRenderState();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        // The preview spans up to 15×15 blocks around the seed; without this it disappears as soon as
        // the seed's own chunk section leaves the frustum.
        return true;
    }

    @Override
    public boolean shouldRender(T blockEntity, Vec3 cameraPosition) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extract(T blockEntity, StructPreviewRenderState state, float partialTicks, Vec3 cameraPosition) {
        state.missing.clear();
        Level level = blockEntity.getLevel();
        if (level == null) return;
        BlockPos origin = blockEntity.getBlockPos();
        StructPattern pattern = blockEntity.pattern();
        for (StructPattern.Cell cell : pattern.cells()) {
            if (pattern.isSatisfied(level, origin, cell)) continue;
            state.missing.add(new StructPreviewRenderState.Ghost(cell.offset(), cell.top(), cell.side()));
        }
    }

    @Override
    public void submit(StructPreviewRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (state.missing.isEmpty()) return;

        // Batch by texture: one submission per distinct texture rather than per cube, since each
        // texture is its own RenderType.
        Map<Identifier, java.util.List<BlockPos>> sides = new LinkedHashMap<>();
        Map<Identifier, java.util.List<BlockPos>> ends = new LinkedHashMap<>();
        for (StructPreviewRenderState.Ghost ghost : state.missing) {
            sides.computeIfAbsent(ghost.side(), k -> new java.util.ArrayList<>()).add(ghost.offset());
            ends.computeIfAbsent(ghost.top(), k -> new java.util.ArrayList<>()).add(ghost.offset());
        }

        sides.forEach((texture, offsets) -> sink.submitCustomGeometry(poseStack,
                CompatRenderTypes.entityTranslucent(texture),
                (pose, buffer) -> {
                    for (BlockPos o : offsets) {
                        SmallBlockPronter.sideFaces(pose, buffer, o.getX(), o.getY(), o.getZ(),
                                1F, 1F, 1F, ALPHA, FULL_BRIGHT);
                    }
                }));
        ends.forEach((texture, offsets) -> sink.submitCustomGeometry(poseStack,
                CompatRenderTypes.entityTranslucent(texture),
                (pose, buffer) -> {
                    for (BlockPos o : offsets) {
                        SmallBlockPronter.endFaces(pose, buffer, o.getX(), o.getY(), o.getZ(),
                                1F, 1F, 1F, ALPHA, FULL_BRIGHT);
                    }
                }));
    }
}
