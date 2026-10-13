package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.deco.TileEntityLanternBehemoth;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderLanternBehemoth}: the lantern post's OBJ in the rusted skin, unculled, with an untextured
 * fullbright lamp. Working, the lamp breathes green ({@code 0.5..1} of full green on
 * {@code sin(millis / 200)}); broken, the post leans (5 degrees about X, then 10 about Z) and the lamp pulses
 * red from black to full. The pulse is wall-clock, as upstream's is.
 */
public class RenderLanternBehemoth extends GemBER<TileEntityLanternBehemoth, RenderLanternBehemoth.State> {

    public static class State extends CompatRenderState {
        public boolean broken;
        public int glowTint = 0xFF00FF00;
    }

    private final WavefrontObj model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/deco/lantern.obj"));
    private final Surface skin = Surface.cutoutNoCull(Constants.MOD_ID + ":textures/models/deco/lantern_rusty.png");
    private final Surface lamp = Surface.emissive(Constants.MOD_ID + ":textures/models/misc/white.png");

    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.offScreen().viewDistance(256);
        declaration.part(model.part("Lantern"), skin)
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.X, s -> s.broken ? 5F : 0F)
                .angle(Gem.Axes.Z, s -> s.broken ? 10F : 0F);
        declaration.part(model.part("Light"), lamp)
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.X, s -> s.broken ? 5F : 0F)
                .angle(Gem.Axes.Z, s -> s.broken ? 10F : 0F)
                .tint(s -> s.glowTint)
                .light(s -> 0xF000F0);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRender(TileEntityLanternBehemoth be, Vec3 cameraPosition) {
        return true;
    }

    @Override
    public void extract(TileEntityLanternBehemoth lantern, State state, float partialTicks, Vec3 cameraPosition) {
        state.broken = lantern.isBroken;
        float wave = (float) (Math.sin(System.currentTimeMillis() / 200D) / 2 + 0.5);
        if (state.broken) {
            state.glowTint = 0xFF000000 | ((int) (255F * wave) << 16);
        } else {
            float mult = wave * 0.5F + 0.5F;
            state.glowTint = 0xFF000000 | ((int) (255F * mult) << 8);
        }
    }
}
