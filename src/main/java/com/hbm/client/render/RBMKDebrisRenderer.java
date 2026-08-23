package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.entity.projectile.EntityRBMKDebris;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/** Tumbling, type-specific RBMK column fragment. */
public final class RBMKDebrisRenderer extends CompatEntityRenderer<EntityRBMKDebris, RBMKDebrisRenderer.State> {
    private static final float DIAG = 0.57735026F;
    private static final WavefrontObj[] MODELS = {
            model("deb_blank"), model("deb_element"), model("deb_fuel"), model("deb_rod"), model("deb_graphite"), model("deb_lid")};
    private static final Identifier[] TEXTURES = {
            texture("blank"), texture("element"), texture("fuel"), texture("control"), texture("graphite"), texture("lid")};

    public RBMKDebrisRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) { super(context); shadowRadius = .5F; }
    @Override public State createRenderState() { return new State(); }
    @Override public void extract(EntityRBMKDebris debris, State state, float partial) {
        state.type = switch (debris.getDebrisType()) { case BLANK -> 0; case ELEMENT -> 1; case FUEL -> 2; case ROD -> 3; case GRAPHITE -> 4; case LID -> 5; };
        state.rot = Mth.lerp(partial, debris.lastRot, debris.rot);
        state.spin = Math.floorMod(debris.getId(), 360);
    }
    @Override public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        pose.translate(0D, .125D, 0D);
        pose.mulPose(Axis.YP.rotationDegrees(state.spin));
        pose.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(state.rot), DIAG, DIAG, DIAG));
        sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutout(TEXTURES[state.type]),
                (last, buffer) -> MODELS[state.type].renderAll(last, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
        pose.popPose();
    }
    private static WavefrontObj model(String name) { return WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/projectiles/" + name + ".obj")); }
    private static Identifier texture(String name) { return Ids.of(Constants.MOD_ID, "textures/entity/rbmk_debris/" + name + ".png"); }
    public static final class State extends CompatEntityRenderState { int type = 1, spin; float rot; }
}
