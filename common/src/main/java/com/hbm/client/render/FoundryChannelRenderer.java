package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.FoundryChannel;
import com.hbm.tileentity.machine.TileEntityFoundryChannel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Renderer for the foundry channel (1.12.2 {@code RenderFoundryChannel}): draws the molten metal level
 * inside the gutter — a brightened tint of the material's {@code moltenColor} on {@code lava_gray},
 * covering the centre plus each connected arm at {@code y = 0.125 + amount/capacity × 0.25}.
 */
public class FoundryChannelRenderer implements BlockEntityRenderer<TileEntityFoundryChannel, FoundryChannelRenderer.State> {

    public static class State extends BlockEntityRenderState {
        public boolean hasMelt;
        public int color = 0xFFFFFF;
        public float y;
        public int connections;
    }

    private final RenderType meltType;

    public FoundryChannelRenderer() {
        // Emissive = uniform fullbright, no per-face shading (the old lighting-off molten quads).
        this.meltType = RenderTypes.entityTranslucentEmissive(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/models/machines/lava_gray.png"));
    }

    @Override public State createRenderState() { return new State(); }
    @Override public boolean shouldRender(TileEntityFoundryChannel be, Vec3 cameraPosition) { return true; }

    @Override
    public void extractRenderState(TileEntityFoundryChannel be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.hasMelt = be.type != null && be.amount > 0;
        if (!state.hasMelt) return;

        // the old renderer brightened the molten colour by 70% toward white
        int hex = be.type.moltenColor;
        int r = (hex >> 16) & 0xFF, g = (hex >> 8) & 0xFF, b = hex & 0xFF;
        r = (int) (255 - (255 - r) * 0.7D);
        g = (int) (255 - (255 - g) * 0.7D);
        b = (int) (255 - (255 - b) * 0.7D);
        state.color = (r << 16) | (g << 8) | b;

        state.y = (float) (0.125D + be.amount * 0.25D / be.getCapacity());
        BlockState bs = be.getBlockState();
        state.connections = bs.hasProperty(FoundryChannel.META) ? bs.getValue(FoundryChannel.META) : 0;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (!s.hasMelt) return;
        int color = 0xFF000000 | s.color;
        float y = s.y;
        int meta = s.connections;

        col.submitCustomGeometry(ps, meltType, (pose, buffer) -> {
            // centre
            FoundryRenderHelper.flatQuad(pose, buffer, 0.375F, 0.375F, 0.625F, 0.625F, y, color);
            // arms (old renderLiquid rectangles, top faces only)
            if ((meta & 1) != 0) FoundryRenderHelper.flatQuad(pose, buffer, 0.625F, 0.3125F, 1F, 0.6875F, y, color);      // +X (E)
            if ((meta & 2) != 0) FoundryRenderHelper.flatQuad(pose, buffer, 0F, 0.3125F, 0.375F, 0.6875F, y, color);      // -X (W)
            if ((meta & 4) != 0) FoundryRenderHelper.flatQuad(pose, buffer, 0.3125F, 0.625F, 0.6875F, 1F, y, color);      // +Z (S)
            if ((meta & 8) != 0) FoundryRenderHelper.flatQuad(pose, buffer, 0.3125F, 0F, 0.6875F, 0.375F, y, color);      // -Z (N)
        });
    }
}
