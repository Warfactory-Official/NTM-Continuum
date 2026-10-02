package com.hbm.client.render.entity;

import com.hbm.client.render.item.ItemRenderGrenade;
import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.items.weapon.grenade.GrenadeData;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code RenderGrenadeUniversal}: the thrown grenade at 1/16 scale, turned to its heading, tumbling about X by its
 * spin and tipped over by -80 degrees about Z once it has bounced, drawn by {@code ItemRenderGrenade.renderGrenade}
 * with a {@code null} render type (the shell's own offsets, no spoon, ring or cap).
 */
public final class RenderGrenadeUniversal extends CompatEntityRenderer<EntityGrenadeUniversal, RenderGrenadeUniversal.State> {

    public RenderGrenadeUniversal(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityGrenadeUniversal entity, State state, float partialTicks) {
        ItemStack stack = entity.getGrenadeItem();
        state.data = stack.isEmpty() ? null : ItemGrenadeUniversal.getData(stack);
        state.yaw = (float) (entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTicks);
        state.spin = (float) Mth.lerp(partialTicks, entity.prevSpin, entity.spin);
        state.bounces = entity.getBounces();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        if (state.data == null) return;
        pose.pushPose();
        float scale = 0.0625F;
        pose.scale(scale, scale, scale);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        pose.mulPose(Axis.XP.rotationDegrees(state.spin));
        if (state.bounces > 0) pose.mulPose(Axis.ZP.rotationDegrees(-80F));
        ItemRenderGrenade.renderGrenade(state.data, null, pose, sink, state.lightCoords);
        pose.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        @Nullable GrenadeData data;
        float yaw;
        float spin;
        int bounces;
    }
}
