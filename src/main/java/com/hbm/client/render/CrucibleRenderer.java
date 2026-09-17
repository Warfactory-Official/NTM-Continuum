package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.material.Mats;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityCrucible;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Quads;
import dev.golem.core.render.gem.Surface;
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
public class CrucibleRenderer extends GemBER<TileEntityCrucible, CrucibleRenderState> {

    private static final Geometry POOL = Quads.named(Constants.MOD_ID + ":crucible_pool")
            .normal(0F, 1F, 0F)
            .vertex(-1F, 0F, -1F, 0F, 0F).vertex(-1F, 0F, 1F, 0F, 1F)
            .vertex(1F, 0F, 1F, 1F, 1F).vertex(1F, 0F, -1F, 1F, 0F)
            .build();

    private final WavefrontObj model;
    private final Surface basin;
    private final Surface lava;
    private final RenderType streamType;

    public CrucibleRenderer() {
        Identifier obj = dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/crucible.obj");
        Identifier tex = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/crucible_heat.png");
        Identifier lavaPool = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/lava.png");
        Identifier lavaGray = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/lava_gray.png");
        this.model = WavefrontObj.load(obj);
        this.basin = Surface.cutout(tex.toString());
        this.lava = Surface.emissive(lavaPool.toString());
        // The stream is a self-overlapping tube and must write depth, which translucent-emissive does
        // not; entitySolid shades by the vertex normal and culls, so the geometry is double-wound.
        this.streamType = CompatRenderTypes.entitySolid(lavaGray);
    }

    @Override
    protected void setup(Gem.Model<CrucibleRenderState> declaration) {
        declaration.offScreen().viewDistance(256)
                .part(model.all(), basin)
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        declaration.part(POOL, lava)
                .visibleIf(state -> state.poolLevel > 0F)
                .translate(0.5F, 0F, 0.5F)
                .slide(Gem.Axes.Y, state -> 0.5F + state.poolLevel)
                .light(state -> FoundryRenderHelper.FULLBRIGHT);
    }

    @Override public CrucibleRenderState createRenderState() { return new CrucibleRenderState(); }

    @Override
    public void extract(TileEntityCrucible be, CrucibleRenderState state, float partialTicks,
                                   Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = MachineCore.facingOrdinal(bs, 3);
        state.dirOrdinal = meta;
        state.yaw = 90F + switch (state.dirOrdinal) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
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

    /**
     * The pool surface stands at the melt level and the two spouts pour as far as they have to reach;
     * both are built from those numbers rather than placed.
     */
    @Override
    protected void submitUndeclared(CrucibleRenderState s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);

        // Pour streams at the spouts (recipe = facing, waste = opposite), in world axes.
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
        ps.translate(dir.offsetX * 1.875F, 0F, dir.offsetZ * 1.875F);
        col.submitCustomGeometry(ps, streamType,
                (pose, buffer) -> FoundryRenderHelper.pourStream(pose, buffer, dir, len, 0.625F, 0.625F, rgb));
        ps.popPose();
    }
}
