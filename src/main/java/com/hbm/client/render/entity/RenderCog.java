package com.hbm.client.render.entity;

import com.hbm.entity.projectile.EntityCog;
import com.hbm.main.ResourceManager;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * 1.7 render/entity/projectile/RenderCog: the thrown stirling gear, spinning while airborne and frozen once its
 * orientation has gained the landed 6. The sawblade half of ntm-next's shared renderer is
 * {@code SawbladeRenderer}, which predates this port.
 */
public class RenderCog extends CompatEntityRenderer<EntityCog, RenderCog.State> {

    /** 1.7: {@code millis % (360 * 3) / 3} - the gear turns once in 1080 ms. */
    public static final float SPIN_DIVISOR = 3F;
    public static final long COG_SPIN_PERIOD = 360L * 3L;
    public static final int LANDED = 6;

    private final RenderType[] types = {
            CompatRenderTypes.entityCutoutCull(ResourceManager.stirling_tex),
            CompatRenderTypes.entityCutoutCull(ResourceManager.stirling_steel_tex)};

    public RenderCog(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    /** 1.7 keys the skin off {@code meta == 0}, and the creative stirling spawns its gear at meta 2. */
    private static int steel(EntityCog cog) {
        return cog.getMeta() == 0 ? 0 : 1;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityCog entity, State state, float partialTicks) {
        state.orientation = entity.getOrientation();
        state.skin = steel(entity);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        pose.pushPose();

        switch (state.orientation % 6) {
            case 5 -> pose.mulPose(Axis.YP.rotationDegrees(90F));
            case 2 -> pose.mulPose(Axis.YP.rotationDegrees(180F));
            case 4 -> pose.mulPose(Axis.YP.rotationDegrees(270F));
            default -> {
            }
        }

        pose.translate(0D, 0D, -1D);

        if (state.orientation < LANDED) {
            pose.mulPose(Axis.ZN.rotationDegrees((GameTime.now() % COG_SPIN_PERIOD) / SPIN_DIVISOR));
        }

        pose.translate(0D, -1.375D, 0D);

        sink.submitCustomGeometry(pose, types[state.skin], (p, buffer) ->
                NtmEntityModels.STIRLING.renderPart("Cog", p, buffer, light, OverlayTexture.NO_OVERLAY));
        pose.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        public int orientation;
        public int skin;
    }
}
