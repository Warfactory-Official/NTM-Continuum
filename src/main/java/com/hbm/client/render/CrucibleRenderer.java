package com.hbm.client.render;

import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CameraInfo;
import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.material.Mats;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityCrucible;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Renderer for the crucible — a port of the 1.12.2 {@code RenderCrucible} TESR. Draws the basin OBJ,
 * the molten pool surface (the untinted lava texture at a height proportional to the total stored
 * mass, fullbright) and the two pour streams (thin tinted columns at the front/back spouts, replacing
 * the old {@code HbmEffectNT.Foundry} particle).
 */
public class CrucibleRenderer implements CompatBER<TileEntityCrucible, CrucibleRenderState> {

    private final WavefrontObj model;
    private final RenderType baseType;
    private final RenderType lavaType;
    private final RenderType streamType;

    public CrucibleRenderer() {
        Identifier obj = dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/crucible.obj");
        Identifier tex = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/crucible_heat.png");
        Identifier lava = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/lava.png");
        Identifier lavaGray = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/lava_gray.png");
        this.model = WavefrontObj.load(obj);
        this.baseType = CompatRenderTypes.entityCutout(tex);
        // Pool surface: flat single quad, no self-overlap → emissive (uniform fullbright) reads well.
        this.lavaType = CompatRenderTypes.entityTranslucentEmissive(lava);
        // Stream: a self-overlapping tube. It MUST write depth or its far faces overwrite the near ones
        // and "sides disappear" with view angle (translucent-emissive has depth-write OFF). entitySolid is
        // opaque + depth-write and shades by the vertex NORMAL (all up here → every face uniform, unlike
        // entityCutout's winding-based per-face darkening). It culls, so the geometry is double-wound
        // (FoundryRenderHelper.quad) → cull keeps the outward face. lava_gray is fully opaque.
        this.streamType = CompatRenderTypes.entitySolid(lavaGray);
    }

    @Override public CrucibleRenderState createRenderState() { return new CrucibleRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityCrucible be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityCrucible be, CrucibleRenderState state, float partialTicks,
                                   Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 3;
        state.dirOrdinal = meta - BlockDummyable.offset;
        state.yaw = 90F + switch (state.dirOrdinal) {
            case 4 -> 90F;   // WEST
            case 3 -> 180F;  // SOUTH
            case 5 -> 270F;  // EAST
            default -> 0F;   // NORTH
        };

        int totalMass = 0;
        for (Mats.MaterialStack stack : be.recipeStack) totalMass += stack.amount;
        for (Mats.MaterialStack stack : be.wasteStack) totalMass += stack.amount;
        int totalCap = TileEntityCrucible.recipeZCapacity + TileEntityCrucible.wasteZCapacity;
        state.poolLevel = totalMass <= 0 ? 0F : (float) ((double) totalMass / (double) totalCap * 0.875D);

        state.wastePourColor = be.wastePourColor;
        state.wastePourLen = be.wastePourLen;
        state.recipePourColor = be.recipePourColor;
        state.recipePourLen = be.recipePourLen;
    }

    @Override
    public void submit(CrucibleRenderState s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        int light = s.lightCoords;

        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);

        // The basin model, rotated to the facing.
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));
        col.submitCustomGeometry(ps, baseType,
                (pose, buffer) -> model.renderAll(pose, buffer, light, OverlayTexture.NO_OVERLAY));
        ps.popPose();

        // Molten pool surface (2×2 centred, untinted lava, fullbright) — the old y = 0.5 + level quad.
        if (s.poolLevel > 0F) {
            float y = 0.5F + s.poolLevel;
            col.submitCustomGeometry(ps, lavaType,
                    (pose, buffer) -> FoundryRenderHelper.flatQuad(pose, buffer, -1F, -1F, 1F, 1F, y, 0xFFFFFFFF));
        }

        // Pour streams at the spouts (recipe = facing, waste = opposite), in world axes (no rotation).
        // Old particle params: spawn at (±dir·1.875, block base), off = base = 0.625.
        ForgeDirection dir = ForgeDirection.getOrientation(s.dirOrdinal);
        if (s.recipePourColor >= 0 && s.recipePourLen > 0) {
            submitStream(col, ps, dir, s.recipePourLen, s.recipePourColor);
        }
        if (s.wastePourColor >= 0 && s.wastePourLen > 0) {
            submitStream(col, ps, dir.getOpposite(), s.wastePourLen, s.wastePourColor);
        }

        ps.popPose();
    }

    private void submitStream(SubmitSink col, PoseStack ps, ForgeDirection dir, float len, int rgb) {
        ps.pushPose();
        // pose is at the block centre (translated +0.5); the old particle spawned at the spout xz, block-base y
        ps.translate(dir.offsetX * 1.875F, 0F, dir.offsetZ * 1.875F);
        col.submitCustomGeometry(ps, streamType,
                (pose, buffer) -> FoundryRenderHelper.pourStream(pose, buffer, dir, len, 0.625F, 0.625F, rgb));
        ps.popPose();
    }
}
