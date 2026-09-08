package com.hbm.client.render.turret;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.Poses;
import dev.golem.core.render.SubmitSink;
import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws a turret from its core. Everything visible — the pad, its edge plugs, the carriage and the gun
 * — comes from here, which is why the block itself has no model.
 */
public abstract class RenderTurretBase<T extends TileEntityTurretBaseNT>
        implements CompatBER<T, TurretRenderState<T>> {

    private static final Map<Identifier, RenderType> TYPES = new HashMap<>();

    /** 1.12.2 {@code RenderTurretBase}: (rotation, z offset) per plug cell, in the mask's bit order. */
    private static final float[] PLUG_ROT = {0F, 0F, 90F, 90F, 180F, 180F, 270F, 270F};
    private static final float[] PLUG_OZ = {0F, -1F, -1F, 0F, -1F, 0F, -1F, 0F};

    protected static RenderType typeFor(Identifier texture) {
        return TYPES.computeIfAbsent(texture, CompatRenderTypes::entityCutout);
    }

    protected static void connectors(TurretFrame frame, int mask) {
        for (int i = 0; i < 8; i++) {
            if ((mask & (1 << i)) == 0) continue;
            TurretFrame.Part plug = frame.push()
                                         .obj(TurretResources.CHEKHOV, "Connectors")
                                         .texture(TurretResources.CONNECTOR);
            plug.pose.rotateY(PLUG_ROT[i] * Mth.DEG_TO_RAD).translate(0F, 0F, PLUG_OZ[i]);
        }
    }

    /**
     * The shared base plus the yawing carriage and the pitching gun mounted on it. Returns the gun's
     * pose so a turret with moving parts of its own can hang them off it.
     */
    protected static Matrix4f mount(TurretFrame frame, TurretState<?> state, Identifier baseTexture,
                                    WavefrontObj carriage, String carriageGroup, Identifier carriageTexture,
                                    float pivot, WavefrontObj gun, String gunGroup, Identifier gunTexture) {
        frame.push().obj(TurretResources.CHEKHOV, "Base").texture(baseTexture);
        TurretFrame.Part mount = frame.push().obj(carriage, carriageGroup).texture(carriageTexture);
        mount.pose.rotateY((state.yaw - 90F) * Mth.DEG_TO_RAD);
        TurretFrame.Part body = frame.push().obj(gun, gunGroup).texture(gunTexture);
        body.pose.set(mount.pose)
                 .translate(0F, pivot, 0F)
                 .rotateZ(state.pitch * Mth.DEG_TO_RAD)
                 .translate(0F, -pivot, 0F);
        return body.pose;
    }

    protected abstract TurretState.Emitter<T> emitter();

    @Override
    public TurretRenderState<T> createRenderState() {
        return new TurretRenderState<>(emitter());
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public boolean shouldRender(T blockEntity, Vec3 cameraPosition) {
        return true;
    }

    @Override
    public int viewDistance() {
        return 256;
    }

    @Override
    public void extract(T turret, TurretRenderState<T> state, float partialTicks, Vec3 cameraPosition) {
        state.turret.sample(turret, partialTicks);
    }

    @Override
    public void submit(TurretRenderState<T> state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        TurretState<T> turret = state.turret;
        TurretFrame frame = turret.emit();
        int light = state.lightCoords;

        poseStack.pushPose();
        poseStack.translate(turret.offsetX, 0D, turret.offsetZ);

        for (int i = 0; i < frame.size(); i++) {
            TurretFrame.Part part = frame.get(i);
            WavefrontObj model = part.model;
            String group = part.group;
            Identifier texture = part.texture;
            if (model == null || group == null || texture == null) continue;

            poseStack.pushPose();
            Poses.mulPose(poseStack, part.pose);
            sink.submitCustomGeometry(poseStack, typeFor(texture),
                    (pose, buffer) -> model.renderPart(group, pose, buffer, light, OverlayTexture.NO_OVERLAY));
            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
