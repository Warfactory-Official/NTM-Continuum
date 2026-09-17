package com.hbm.client.render.entity;

import com.hbm.entity.projectile.EntityChopperMine;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * 1.7 RenderChopperMine: the dropped mine cube, 1.5x and flipped.
 */
public class RenderChopperMine extends CompatEntityRenderer<EntityChopperMine, CompatEntityRenderState> {

    private static final RenderType TYPE = CompatRenderTypes.entityCutout(ResourceManager.chopper_bomb_tex);

    public RenderChopperMine(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public CompatEntityRenderState createRenderState() {
        return new CompatEntityRenderState();
    }

    @Override
    public void extract(EntityChopperMine entity, CompatEntityRenderState state, float partialTicks) {}

    @Override
    public void submit(CompatEntityRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180F));
        RenderBoxModel.submit(poseStack, sink, TYPE, RenderBoxModel.CHOPPER_MINE, state.lightCoords);
        poseStack.popPose();
    }
}
