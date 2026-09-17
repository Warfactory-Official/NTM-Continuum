package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockDynamicSlag.TileEntitySlag;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Quads;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/**
 * Renderer for the dynamic slag pile (1.12.2 {@code RenderTileEntitySlag}): a 5-face box at
 * {@code amount / maxAmount} height, the slag texture tinted by the material's solid colour.
 */
public class SlagRenderer extends GemBER<TileEntitySlag, SlagRenderer.State> {

    public static class State extends CompatRenderState {
        public float height;
        public int color = 0xFFFFFF;
    }

    /** Emissive: the single-winding quads read as back faces under PER_FACE_LIGHTING otherwise. */
    private static final Surface SLAG = Surface.emissive(Constants.MOD_ID + ":textures/blocks/slag.png");

    private static final Geometry BOX = box();

    private static Geometry box() {
        Quads.Builder faces = Quads.named(Constants.MOD_ID + ":slag").normal(0F, 1F, 0F);
        faces.vertex(0F, 1F, 0F, 0F, 0F).vertex(0F, 1F, 1F, 0F, 1F)
                .vertex(1F, 1F, 1F, 1F, 1F).vertex(1F, 1F, 0F, 1F, 0F);
        side(faces, 0F, 0F, 1F, 0F);
        side(faces, 0F, 1F, 1F, 1F);
        side(faces, 0F, 0F, 0F, 1F);
        side(faces, 1F, 0F, 1F, 1F);
        return faces.build();
    }

    private static void side(Quads.Builder faces, float x0, float z0, float x1, float z1) {
        faces.vertex(x0, 0F, z0, 0F, 1F).vertex(x1, 0F, z1, 1F, 1F)
                .vertex(x1, 1F, z1, 1F, 0F).vertex(x0, 1F, z0, 0F, 0F);
    }

    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.light(state -> FoundryRenderHelper.FULLBRIGHT)
                .part(BOX, SLAG)
                .stretch(Gem.Axes.Y, state -> state.height)
                .tint(state -> 0xFF000000 | (state.color & 0xFFFFFF));
    }

    @Override public State createRenderState() { return new State(); }
    @Override public boolean shouldRender(TileEntitySlag be, Vec3 cameraPosition) { return true; }

    @Override
    public void extract(TileEntitySlag be, State state, float partialTicks, Vec3 cameraPosition) {
        state.height = Math.min(1F, Math.max(0.0625F, (float) be.amount / TileEntitySlag.maxAmount));
        state.color = be.mat != null ? be.mat.solidColorLight : 0xFFFFFF;
    }
}
