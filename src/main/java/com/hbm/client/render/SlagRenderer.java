package com.hbm.client.render;

import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CameraInfo;
import com.hbm.Constants;
import com.hbm.blocks.generic.BlockDynamicSlag.TileEntitySlag;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Renderer for the dynamic slag pile (1.12.2 {@code RenderTileEntitySlag}): a 5-face box at
 * {@code amount / maxAmount} height, the slag texture tinted by the material's solid colour.
 */
public class SlagRenderer implements CompatBER<TileEntitySlag, SlagRenderer.State> {

    public static class State extends CompatRenderState {
        public float height;
        public int color = 0xFFFFFF;
    }

    private final RenderType renderType;

    public SlagRenderer() {
        // Emissive: uniform brightness on all faces (the single-winding quads read as back faces from
        // two sides under PER_FACE_LIGHTING otherwise).
        this.renderType = RenderTypes.entityTranslucentEmissive(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/blocks/slag.png"));
    }

    @Override public State createRenderState() { return new State(); }
    @Override public boolean shouldRender(TileEntitySlag be, Vec3 cameraPosition) { return true; }

    @Override
    public void extract(TileEntitySlag be, State state, float partialTicks, Vec3 cameraPosition) {
        state.height = Math.max(0.0625F, (float) be.amount / TileEntitySlag.maxAmount);
        state.color = be.mat != null ? be.mat.solidColorLight : 0xFFFFFF;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        int light = s.lightCoords;
        int color = 0xFF000000 | (s.color & 0xFFFFFF);
        float h = Math.min(s.height, 1F);

        col.submitCustomGeometry(ps, renderType, (pose, buffer) -> {
            // top
            FoundryRenderHelper.flatQuad(pose, buffer, 0F, 0F, 1F, 1F, h, color);
            // four sides
            FoundryRenderHelper.verticalQuad(pose, buffer, 0F, 0F, 1F, 0F, h, 0F, color); // north
            FoundryRenderHelper.verticalQuad(pose, buffer, 0F, 1F, 1F, 1F, h, 0F, color); // south
            FoundryRenderHelper.verticalQuad(pose, buffer, 0F, 0F, 0F, 1F, h, 0F, color); // west
            FoundryRenderHelper.verticalQuad(pose, buffer, 1F, 0F, 1F, 1F, h, 0F, color); // east
        });
    }
}
