package com.hbm.client.render.entity;

import com.hbm.entity.mob.EntityCyberCrab;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.living.CompatLivingRenderState;
import dev.golem.core.render.living.CompatMobRenderer;
import dev.golem.core.render.living.LivingModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/**
 * 1.7 RenderCyberCrab. 1.7's ModelCrab lifted itself 1.5 blocks and yawed -90 inside {@code renderAll}; here that is
 * the renderer's frame, since the mesh is baked once and the model class only animates.
 */
public class RenderCyberCrab extends CompatMobRenderer<EntityCyberCrab, CompatLivingRenderState> {

    public RenderCyberCrab(EntityRendererProvider.Context context) {
        super(context, LivingModel.custom(new ModelCrab(ModelCrab.createBodyLayer().bakeRoot())), 1.0F);
        this.shadowStrength = 0F;
    }

    @Override
    public CompatLivingRenderState createRenderState() {
        return new CompatLivingRenderState();
    }

    @Override
    protected void scale(CompatLivingRenderState state, PoseStack poseStack) {
        poseStack.translate(0F, 1.5F, 0F);
        poseStack.mulPose(Axis.YN.rotationDegrees(90F));
    }

    @Override
    protected Identifier texture(CompatLivingRenderState state) {
        return ResourceManager.crab_tex;
    }
}
