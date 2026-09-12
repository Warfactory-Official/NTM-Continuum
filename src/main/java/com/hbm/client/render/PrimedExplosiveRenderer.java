package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.bomb.BlockTNTBase;
import com.hbm.entity.explosive.EntityPrimedExplosive;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemEntityRenderer;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Quads;
import dev.golem.core.render.gem.Surface;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/** Block-textured primed renderer with the legacy final-ten-tick swelling and white flash. */
public class PrimedExplosiveRenderer
        extends GemEntityRenderer<EntityPrimedExplosive, PrimedExplosiveRenderState> {

    private static final Surface FLASH =
            Surface.translucent(Constants.MOD_ID + ":textures/models/misc/white.png");

    /** The flash is a lamp over the charge, not a lit surface. */
    private static final int FULL_BRIGHT = 0xF000F0;

    private static final Geometry SIDES = sides(Quads.named(Constants.MOD_ID + ":primed_sides")).build();
    private static final Geometry TOP = top(Quads.named(Constants.MOD_ID + ":primed_top")).build();
    private static final Geometry BOTTOM = bottom(Quads.named(Constants.MOD_ID + ":primed_bottom")).build();
    private static final Geometry CUBE =
            bottom(top(sides(Quads.named(Constants.MOD_ID + ":primed_cube")))).build();

    private static final Map<String, Surface[]> SKINS = new ConcurrentHashMap<>();

    public PrimedExplosiveRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.5F;
    }

    @Override
    protected void setup(Gem.Model<PrimedExplosiveRenderState> declaration) {
        Gem.Part<PrimedExplosiveRenderState> charge = declaration.joint()
                .translate(0F, 0.5F, 0F)
                .stretch(state -> swell(state.fuse))
                .rotate(Gem.Axes.Y, -90F)
                .translate(-0.5F, -0.5F, 0.5F);

        charge.child(SIDES, skins("tnt")[0]).skin(state -> skins(state.textureName)[0]);
        charge.child(TOP, skins("tnt")[1]).skin(state -> skins(state.textureName)[1]);
        charge.child(BOTTOM, skins("tnt")[2]).skin(state -> skins(state.textureName)[2]);

        charge.child(CUBE, FLASH)
                .order(1)
                .visibleIf(state -> ((int) state.fuse) / 5 % 2 == 0)
                .tint(state -> flash(state.fuse))
                .light(state -> FULL_BRIGHT)
                .translate(0.5F, 0.5F, 0.5F)
                .scale(1.006F)
                .translate(-0.5F, -0.5F, -0.5F);
    }

    @Override
    public PrimedExplosiveRenderState createRenderState() {
        return new PrimedExplosiveRenderState();
    }

    @Override
    public void extract(EntityPrimedExplosive entity, PrimedExplosiveRenderState state, float partialTicks) {
        state.fuse = entity.getFuse() - partialTicks + 1.0F;
        if (entity.getExplosiveState().getBlock() instanceof BlockTNTBase explosive) {
            state.textureName = explosive.textureName();
        }
    }

    /** Upstream's quartic ramp over the last ten ticks, flat at one until then. */
    private static float swell(float fuse) {
        if (fuse >= 10F) return 1F;
        float swell = Mth.clamp(1F - fuse / 10F, 0F, 1F);
        swell *= swell;
        swell *= swell;
        return 1F + swell * 0.3F;
    }

    private static int flash(float fuse) {
        float alpha = (1F - fuse / 100F) * 0.8F;
        return ((int) (Mth.clamp(alpha, 0F, 1F) * 255F) << 24) | 0xFFFFFF;
    }

    private static Surface[] skins(String name) {
        return SKINS.computeIfAbsent(name, base -> new Surface[] {
                cutout(base + "_side"), cutout(base + "_top"), cutout(base + "_bottom")
        });
    }

    private static Surface cutout(String name) {
        return Surface.cutout(Constants.MOD_ID + ":textures/blocks/" + name + ".png");
    }

    private static Quads.Builder sides(Quads.Builder faces) {
        quad(faces, 0F, 0F, -1F, 0F, 0F, 0F, 0F, 1F, 0F, 1F, 1F, 0F, 1F, 0F, 0F);
        quad(faces, 0F, 0F, 1F, 1F, 0F, 1F, 1F, 1F, 1F, 0F, 1F, 1F, 0F, 0F, 1F);
        quad(faces, -1F, 0F, 0F, 0F, 0F, 1F, 0F, 1F, 1F, 0F, 1F, 0F, 0F, 0F, 0F);
        quad(faces, 1F, 0F, 0F, 1F, 0F, 0F, 1F, 1F, 0F, 1F, 1F, 1F, 1F, 0F, 1F);
        return faces;
    }

    private static Quads.Builder top(Quads.Builder faces) {
        return quad(faces, 0F, 1F, 0F, 0F, 1F, 0F, 0F, 1F, 1F, 1F, 1F, 1F, 1F, 1F, 0F);
    }

    private static Quads.Builder bottom(Quads.Builder faces) {
        return quad(faces, 0F, -1F, 0F, 0F, 0F, 1F, 0F, 0F, 0F, 1F, 0F, 0F, 1F, 0F, 1F);
    }

    private static Quads.Builder quad(Quads.Builder faces, float nx, float ny, float nz,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float x2, float y2, float z2, float x3, float y3, float z3) {
        return faces.normal(nx, ny, nz)
                .vertex(x0, y0, z0, 0F, 1F)
                .vertex(x1, y1, z1, 0F, 0F)
                .vertex(x2, y2, z2, 1F, 0F)
                .vertex(x3, y3, z3, 1F, 1F);
    }
}
