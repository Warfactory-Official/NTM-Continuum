package com.hbm.client.render.entity;

import com.hbm.entity.grenade.EntityDisperserCanister;
import com.hbm.entity.grenade.IGenericGrenade;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 RenderGenericGrenade: the grenade drawn as its own item icon on a single camera-facing quad at half size;
 * the disperser canister draws its body and fluid-tinted overlay. Both are what the item's own model draws, so
 * the port bills the stack itself to the item renderer facing the camera rather than sampling the icon by hand -
 * the icon, its tint layer and its half-size frame all come from the item model.
 */
public class RenderGenericGrenade<T extends Entity> extends CompatEntityRenderer<T, RenderGenericGrenade.State> {

    public RenderGenericGrenade(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F; // 1.7 getShadowSize
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(T entity, State state, float partialTicks) {
        ItemStack stack;
        if (entity instanceof EntityDisperserCanister canister) {
            com.hbm.items.weapon.ItemDisperser item = canister.getDisperserItem();
            stack = item.make(canister.getFluid(), item.capacity);
        } else {
            stack = new ItemStack(((IGenericGrenade) entity).getGrenade());
        }
        state.item.extract(entity.level(), stack, ItemDisplayContext.NONE);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        if (state.item.isEmpty()) return;
        pose.pushPose();
        // 1.7: translate dropped - dispatcher does it
        pose.scale(0.5F, 0.5F, 0.5F);
        // 1.7: camera orientation quaternion IS the rotate pair
        pose.mulPose(camera.orientation);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        sink.submitItem(pose, state.item, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        final CompatItemRenderState item = new CompatItemRenderState();
    }
}
