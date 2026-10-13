package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.deco.TileEntityLantern;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderLantern}: the post's textured body plus its lamp, which breathes.
 *
 * <p>Two parts of one OBJ. {@code Lantern} is the ordinary lit, unculled body. {@code Light} is drawn
 * untextured and fullbright upstream - {@code GL_TEXTURE_2D} off and a flat colour - which here is a
 * white texture on an emissive surface carrying the same tint, because a declared part wears a surface
 * rather than switching GL state.
 *
 * <p>The pulse is upstream's exactly: {@code sin(millis / 200) / 2 + 0.5} scaled into {@code 0.9..1.0}
 * over a {@code (1, 1, 0.7)} warm white. It is wall-clock rather than tick-based there, so it keeps
 * breathing while the game is paused, and reading the tick count instead would visibly change it.
 */
public class LanternRenderer extends GemBER<TileEntityLantern, LanternRenderer.State> {

    public static class State extends CompatRenderState {
        public int glowTint = 0xFFFFFFFF;
    }

    private static final String BODY = "Lantern";
    private static final String LAMP = "Light";

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/deco/lantern.obj"));
    private final Surface skin = Surface.cutoutNoCull(
            Constants.MOD_ID + ":textures/models/deco/lantern.png");
    private final Surface lamp = Surface.emissive(
            Constants.MOD_ID + ":textures/models/misc/white.png");

    @Override
    protected void setup(Gem.Model<State> declaration) {
        // 1.7 widened the tile's render box to x..x+1, y..y+6, z..z+1 because the post stands five
        // blocks above the core; the declaration states that as offScreen() instead of a tile override.
        declaration.offScreen().viewDistance(256);
        declaration.part(model.part(BODY), skin);
        declaration.part(model.part(LAMP), lamp)
                .tint(state -> state.glowTint)
                .light(state -> 0xF000F0);
    }

    @Override public State createRenderState() { return new State(); }

    @Override public boolean shouldRender(TileEntityLantern be, Vec3 cameraPosition) { return true; }

    @Override
    public void extract(TileEntityLantern be, State state, float partialTicks, Vec3 cameraPosition) {
        float mult = (float) (Math.sin(System.currentTimeMillis() / 200D) / 2D + 0.5D) * 0.1F + 0.9F;
        int r = (int) (255F * mult);
        int g = (int) (255F * mult);
        int b = (int) (255F * 0.7F * mult);
        state.glowTint = 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
