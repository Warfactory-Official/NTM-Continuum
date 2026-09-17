package com.hbm.client.render.entity;

import com.hbm.entity.mob.EntityParasiteMaggot;
import com.hbm.main.ResourceManager;
import dev.golem.core.render.living.CompatLivingRenderState;
import dev.golem.core.render.living.CompatMobRenderer;
import dev.golem.core.render.living.LivingModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/** Vanilla silverfish geometry under the maggot's own sheet; it dies flat on its back like a silverfish. */
public final class RenderParasiteMaggot extends CompatMobRenderer<EntityParasiteMaggot, CompatLivingRenderState> {

    public RenderParasiteMaggot(EntityRendererProvider.Context context) {
        super(context, LivingModel.silverfish(context), 0.3F);
    }

    @Override
    public CompatLivingRenderState createRenderState() {
        return new CompatLivingRenderState();
    }

    @Override
    protected float flipDegrees() {
        return 180.0F;
    }

    @Override
    protected Identifier texture(CompatLivingRenderState state) {
        return ResourceManager.parasite_maggot_tex;
    }
}
