package com.hbm.client.render.entity;

import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.entity.item.EntityMovingPackage;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code render/entity/item/RenderMovingPackage}, whose dummy {@code EntityItem} carries
 * {@code hoverStart = 0} under {@code RenderItem.renderInFrame}: the framed block arm, bobbing and spinning for
 * neither. 1.7 drew {@code ModBlocks.crate}, the wooden loot crate, which this port does not carry; the wooden
 * supply crate stands in.
 */
public class RenderMovingPackage extends CompatEntityRenderer<EntityMovingPackage, RenderMovingPackage.State> {

    private static final float SCALE = 2F;
    private static final float DROP = 0.0125F;

    public RenderMovingPackage(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityMovingPackage entity, State state, float partialTicks) {
        FramedItem.resolve(state.item, new ItemStack(ModStorageBlocks.crate_supply.get()), entity.level());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (state.item.isEmpty()) return;

        poseStack.pushPose();
        poseStack.translate(0F, -DROP, 0F);
        poseStack.scale(SCALE, SCALE, SCALE);
        FramedItem.framedBlockPrefix(poseStack);
        FramedItem.draw(poseStack, state.item, sink, state.lightCoords);
        poseStack.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        public final CompatItemRenderState item = new CompatItemRenderState();
    }
}
