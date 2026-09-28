package com.hbm.client.render.entity;

import com.hbm.entity.item.EntityMovingItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.SubmitSink;
import java.util.Random;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * 1.7 {@code render/entity/item/RenderMovingItem}: the stack riding a belt, lifted a fraction of a pixel by
 * entity id so a dense belt does not z-fight, and laid flat unless it draws as a block. See {@link FramedItem}
 * for the two arms and their frame factors.
 */
public class RenderMovingItem extends CompatEntityRenderer<EntityMovingItem, RenderMovingItem.State> {

    public RenderMovingItem(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityMovingItem entity, State state, float partialTicks) {
        state.lift = (float) (new Random(entity.getId()).nextDouble() * 0.0625D);
        state.arm = FramedItem.resolveArm(state.item, entity.getItemStack(), entity.level());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (state.item.isEmpty()) return;

        poseStack.pushPose();
        poseStack.translate(0F, state.lift, 0F);

        if (state.arm.block()) {
            FramedItem.framedBlockPrefix(poseStack);
        } else {
            // 1.7 RenderMovingItem:34-35 - the caller's gate fails for the flat arm
            poseStack.mulPose(Axis.XP.rotationDegrees(90F));
            poseStack.translate(0F, -0.1875F, 0F);
            FramedItem.framedFlatArm(poseStack, state.arm);
        }
        FramedItem.draw(poseStack, state.item, sink, state.lightCoords);

        poseStack.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        public final CompatItemRenderState item = new CompatItemRenderState();
        float lift;
        FramedItem.Arm arm = FramedItem.Arm.SPRITE;
    }
}
