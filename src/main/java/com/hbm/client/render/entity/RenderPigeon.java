package com.hbm.client.render.entity;

import com.hbm.entity.mob.EntityPigeon;
import com.hbm.main.ResourceManager;
import dev.golem.core.render.living.CompatLivingRenderState;
import dev.golem.core.render.living.CompatMobRenderer;
import dev.golem.core.render.living.LivingModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * 1.7 RenderPigeon.
 */
public class RenderPigeon extends CompatMobRenderer<EntityPigeon, RenderPigeon.State> {

    public RenderPigeon(EntityRendererProvider.Context context) {
        super(context, LivingModel.custom(new ModelPigeon(ModelPigeon.createBodyLayer().bakeRoot())), 0.3F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    protected void extract(EntityPigeon entity, State state, float partialTicks) {
        float fall = Mth.lerp(partialTicks, entity.prevFallTime, entity.fallTime);
        float dest = Mth.lerp(partialTicks, entity.prevDest, entity.dest);
        state.flap = (Mth.sin(fall) + 1.0F) * dest;
        state.fat = entity.isFat();
    }

    @Override
    protected Identifier texture(State state) {
        return ResourceManager.pigeon_tex;
    }

    public static final class State extends CompatLivingRenderState {
        /** 1.7 fed the wing flap through handleRotationFloat, which the model read as ageInTicks. */
        public float flap;
        public boolean fat;
    }
}
