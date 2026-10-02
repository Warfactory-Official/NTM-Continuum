package com.hbm.client.render.entity;

import com.hbm.entity.projectile.EntityTom;
import com.hbm.main.ResourceManager;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import java.util.Random;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * 1.7 RenderTom / TomPronter: the hundred-block moonstone body, full-bright, with twenty additive flame shells
 * spun about it on the wall clock, each shell offset by a fixed seed and scrolling its sheet.
 */
public class RenderTom extends CompatEntityRenderer<EntityTom, RenderTom.State> {

    // 1.7 TomPronter:20 disables GL_LIGHTING before the body as well
    private static final RenderType TYPE = CompatRenderTypes.entityCutoutNoCull(ResourceManager.tom_main_tex);
    private static final RenderType FLAME = CompatRenderTypes.plasmaGlow(ResourceManager.tom_flame_tex);

    /** 1.7 TomPronter:38 seeds a fresh {@code Random(0)} every frame, so the twenty offsets are one fixed table. */
    private static final int[] SHELL_OFFSETS = shellOffsets();

    /** 1.7 TomPronter:28 - this plume's own, against HmfController's 100000/5000 default. */
    private static final double SCROLL_PERIOD = 50000D;
    private static final double SCROLL_DIVISOR = 2500D;

    public RenderTom(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    private static int[] shellOffsets() {
        Random random = new Random(0);
        int[] offsets = new int[20];
        for (int i = 0; i < offsets.length; i++) offsets[i] = random.nextInt(90);
        return offsets;
    }

    // 1.7: ignoreFrustumCheck - the distance gate stays with the entity
    @Override
    public boolean culled(EntityTom entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityTom entity, State state, float partialTicks) {
        long now = GameTime.now();
        // 1.7 TomPronter:34 - the shells' common spin
        state.spin = -(now / 10L) % 360L;
        // 1.7 HbmFace.addFaceForRender:47 - the sheet's own scroll, added to V
        state.scroll = (float) ((now % SCROLL_PERIOD) / SCROLL_DIVISOR);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0D, -50D, 0D);
        poseStack.scale(100F, 100F, 100F);
        sink.submitCustomGeometry(poseStack, TYPE, (pose, buffer) -> NtmEntityModels.TOM_MAIN.renderAll(
                pose, buffer, CloudRenderTypes.FULL_BRIGHT, OverlayTexture.NO_OVERLAY));

        float spin = state.spin;
        float scroll = state.scroll;
        poseStack.scale(0.8F, 5F, 0.8F);
        for (int shell = 0; shell < SHELL_OFFSETS.length; shell++) {
            poseStack.mulPose(Axis.YP.rotationDegrees(spin + SHELL_OFFSETS[shell]));
            sink.submitCustomGeometry(poseStack, FLAME, (pose, buffer) -> NtmEntityModels.TOM_FLAME.renderAll(
                    pose, buffer, CloudRenderTypes.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1, 0F, scroll));
            poseStack.mulPose(Axis.YN.rotationDegrees(spin));
            poseStack.scale(-1.015F, 0.9F, 1.015F);
        }
        poseStack.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        public float spin;
        public float scroll;
    }
}
