package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.entity.projectile.EntitySawblade;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemEntityRenderer;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Quads;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Renders the ejected blade as two crossed, item-textured planes. */
public class SawbladeRenderer extends GemEntityRenderer<EntitySawblade, SawbladeRenderState> {

    private static final Surface SURFACE = Surface.cutout(Constants.MOD_ID + ":textures/items/sawblade.png");

    /** Half the blade's width, in blocks. */
    private static final float REACH = 0.65F;

    /** A flung blade is lit as an item is, not by the block it happens to be passing. */
    private static final int FULL_BRIGHT = 0xF000F0;

    private static final Geometry BLADE = Quads.named(Constants.MOD_ID + ":sawblade")
            .normal(0F, 0F, 1F)
            .vertex(-REACH, -REACH, 0F, 0F, 1F)
            .vertex(REACH, -REACH, 0F, 1F, 1F)
            .vertex(REACH, REACH, 0F, 1F, 0F)
            .vertex(-REACH, REACH, 0F, 0F, 0F)
            .normal(1F, 0F, 0F)
            .vertex(0F, -REACH, -REACH, 0F, 1F)
            .vertex(0F, -REACH, REACH, 1F, 1F)
            .vertex(0F, REACH, REACH, 1F, 0F)
            .vertex(0F, REACH, -REACH, 0F, 0F)
            .build();

    public SawbladeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void setup(Gem.Model<SawbladeRenderState> declaration) {
        declaration.light(state -> FULL_BRIGHT)
                .part(BLADE, SURFACE)
                .scale(0.8F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .angle(Gem.Axes.Z, state -> state.rotation);
    }

    @Override
    public SawbladeRenderState createRenderState() {
        return new SawbladeRenderState();
    }

    @Override
    public void extract(EntitySawblade entity, SawbladeRenderState state, float partialTicks) {
        state.rotation = entity.lastRotation + (entity.rotation - entity.lastRotation) * partialTicks;
        state.yaw = entity.getOrientation() % 6 * 60F;
    }
}
