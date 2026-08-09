package com.hbm.client.render;

import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Generic {@link BlockEntityRenderer} that draws one or more OBJ models for a HBM machine, replacing
 * the old per-machine {@code TileEntitySpecialRenderer}s. It renders a <b>static</b> pose (the whole
 * model via {@link WavefrontObj#renderAll}); the animated arms/heads/etc. return once the machine's
 * processing logic is ported (see IMPLEMENTATION.md §18).
 *
 * <p>It is attached only to the multiblock <b>core</b> BlockEntity, so it draws once per machine and
 * the model spans the dummy footprint. Facing comes from the core's {@link BlockDummyable#META} state.
 */
public class MachineObjRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, MachineRenderState> {

    /**
     * A model file + the standalone texture it samples + its own vertical offset (block units) + the
     * set of OBJ parts to skip (e.g. dynamic fluid overlays, or a frame that only shows conditionally).
     */
    public record ModelDef(Identifier obj, Identifier texture, float yOffset, Set<String> exclude) {
        public ModelDef(Identifier obj, Identifier texture) { this(obj, texture, 0F, Set.of()); }
        public ModelDef(Identifier obj, Identifier texture, float yOffset) { this(obj, texture, yOffset, Set.of()); }
        public ModelDef(Identifier obj, Identifier texture, Set<String> exclude) { this(obj, texture, 0F, exclude); }
    }

    private record Part(WavefrontObj model, RenderType renderType, float yOffset, Set<String> exclude) {}

    private final List<Part> parts;
    private final float baseYaw;
    private final float scale;
    private final float offsetX;
    private final float offsetZ;
    private final Set<String> frameParts;
    private final int frameHeight;
    /** When true the facing rotation is skipped (old all-zero yaw maps, e.g. the oilburner). */
    private boolean ignoreFacing = false;

    public MachineObjRenderer(float baseYaw, float scale, ModelDef... defs) {
        this(baseYaw, scale, 0F, 0F, Set.of(), 0, defs);
    }

    public MachineObjRenderer(float baseYaw, float scale, Set<String> frameParts, int frameHeight, ModelDef... defs) {
        this(baseYaw, scale, 0F, 0F, frameParts, frameHeight, defs);
    }

    /**
     * @param offsetX,offsetZ a translation applied <b>after</b> the facing rotation (block units, in the
     *                        rotated model frame) — the 1.12.2 footprint offset for asymmetric machines
     *                        (whose model isn't centred on the core cell). {@code 0,0} for centred machines.
     * @param frameParts OBJ parts drawn only when a block sits {@code frameHeight} above the core (the
     *                   1.12.2 "frame" condition); skipped otherwise. Empty = no conditional parts.
     */
    public MachineObjRenderer(float baseYaw, float scale, float offsetX, float offsetZ, Set<String> frameParts, int frameHeight, ModelDef... defs) {
        this.baseYaw = baseYaw;
        this.scale = scale;
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;
        this.frameParts = frameParts;
        this.frameHeight = frameHeight;
        List<Part> built = new ArrayList<>();
        for (ModelDef d : defs) {
            // entityCutout (alpha test) not entitySolid: the old TESRs ran with GL alpha-test on, so
            // fully-transparent texels (e.g. the chemical plant's glass) are discarded instead of being
            // drawn as opaque black.
            built.add(new Part(WavefrontObj.load(d.obj()), RenderTypes.entityCutout(d.texture()), d.yOffset(), d.exclude()));
        }
        this.parts = built;
    }

    /** Old machines whose StaticTesrBakedModels yaw map is all zeros render facing-independent. */
    public MachineObjRenderer<T> ignoreFacing() {
        this.ignoreFacing = true;
        return this;
    }

    @Override
    public MachineRenderState createRenderState() {
        return new MachineRenderState();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        // The OBJ model spans several blocks past the 1×1×1 core; without this the BlockEntity is only
        // drawn while its core block's chunk section is on-screen, so it vanishes the moment that block
        // scrolls off. Returning true adds it to the level's "globally rendered" set (like the beacon),
        // which is drawn every frame regardless of frustum/section visibility.
        return true;
    }

    @Override
    public boolean shouldRender(T blockEntity, Vec3 cameraPosition) {
        // Never distance/visibility-cull the machine itself; the off-screen path above handles drawing.
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(T blockEntity, MachineRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        BlockState bs = blockEntity.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        // meta - offset gives the ForgeDirection ordinal of the facing: N=2, S=3, W=4, E=5.
        state.yaw = ignoreFacing ? 0F : switch (meta - BlockDummyable.offset) {
            case 4 -> 90F;   // WEST
            case 3 -> 180F;  // SOUTH
            case 5 -> 270F;  // EAST
            default -> 0F;   // NORTH (2)
        };
        // The frame (if this machine has one) shows only when a block sits `frameHeight` above the core,
        // matching the 1.12.2 `frame = blockState(pos.up(3)) != AIR` check.
        state.showFrame = frameParts.isEmpty() || (blockEntity.getLevel() != null
                && !blockEntity.getLevel().getBlockState(blockEntity.getBlockPos().above(frameHeight)).isAir());
    }

    @Override
    public void submit(MachineRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        // Skip the always-excluded parts (e.g. a dynamic fluid overlay) plus, when no block is above,
        // the conditional frame.
        Set<String> hidden = state.showFrame ? Set.of() : frameParts;
        for (Part part : parts) {
            Set<String> exclude = hidden.isEmpty() ? part.exclude()
                    : union(part.exclude(), hidden);
            poseStack.pushPose();
            poseStack.translate(0.5F, part.yOffset(), 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(baseYaw + state.yaw));
            if (offsetX != 0F || offsetZ != 0F) poseStack.translate(offsetX, 0F, offsetZ); // footprint offset (post-rotation)
            if (scale != 1.0F) poseStack.scale(scale, scale, scale);
            collector.submitCustomGeometry(poseStack, part.renderType(),
                    (pose, buffer) -> part.model().renderExcept(pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY, exclude));
            poseStack.popPose();
        }
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        if (a.isEmpty()) return b;
        java.util.HashSet<String> s = new java.util.HashSet<>(a);
        s.addAll(b);
        return s;
    }
}
