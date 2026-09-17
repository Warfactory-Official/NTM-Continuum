package com.hbm.client.render.turret;

import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/**
 * Draws a turret from its core. Everything visible — the pad, its edge plugs, the carriage and the gun
 * — comes from here, which is why the block itself has no model.
 *
 * <p>A turret's shape is not fixed: how many plugs it shows depends on its pad, and a HIMARS shows one
 * rocket cap per round still loaded. So the declaration is a fixed run of slots and the emitted
 * {@link TurretFrame} says, each frame, which mesh sits in each one and where. Slots past the end of
 * the frame draw nothing.
 */
public abstract class RenderTurretBase<T extends TileEntityTurretBaseNT>
        extends GemBER<T, TurretRenderState<T>> {

    /** More slots than any turret emits: eight plugs, a pad, a carriage, a gun and a full rocket pod. */
    private static final int MAX_PARTS = 24;

    private static final Matrix4fc REST = new Matrix4f();

    /** 1.12.2 {@code RenderTurretBase}: (rotation, z offset) per plug cell, in the mask's bit order. */
    private static final float[] PLUG_ROT = {0F, 0F, 90F, 90F, 180F, 180F, 270F, 270F};
    private static final float[] PLUG_OZ = {0F, -1F, -1F, 0F, -1F, 0F, -1F, 0F};

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

    /** Non-rigid extras drawn in the turret's own space — a beam, say — after its parts. */
    protected void submitEffects(TurretRenderState<T> state, PoseStack poseStack, SubmitSink sink) {}

    @Override
    protected void setup(Gem.Model<TurretRenderState<T>> declaration) {
        Geometry placeholder = TurretResources.CHEKHOV.part("Base");
        Surface unpainted = Surface.cutout(TurretResources.BASE.toString());

        Gem.Part<TurretRenderState<T>> pad = declaration.offScreen().viewDistance(256)
                .joint()
                .slide(Gem.Axes.X, state -> (float) state.turret.offsetX)
                .slide(Gem.Axes.Z, state -> (float) state.turret.offsetZ);

        for (int i = 0; i < MAX_PARTS; i++) {
            final int slot = i;
            pad.child(placeholder, unpainted)
               .visibleIf(state -> emitted(state, slot) != null)
               .mesh(state -> {
                   TurretFrame.Part part = emitted(state, slot);
                   return part == null ? placeholder : part.geometry();
               })
               .skin(state -> {
                   TurretFrame.Part part = emitted(state, slot);
                   return part == null ? unpainted : part.surface();
               })
               .pose(state -> {
                   TurretFrame.Part part = emitted(state, slot);
                   return part == null ? REST : part.pose;
               });
        }
    }

    private static TurretFrame.@Nullable Part emitted(TurretRenderState<?> state, int slot) {
        TurretFrame frame = state.turret.frame;
        if (slot >= frame.size()) return null;
        TurretFrame.Part part = frame.get(slot);
        return part.geometry() == null || part.surface() == null ? null : part;
    }

    @Override
    public TurretRenderState<T> createRenderState() {
        return new TurretRenderState<>(emitter());
    }

    /** Emitted here, not at draw time: the instanced backend reads it without a submit step at all. */
    @Override
    public void extract(T turret, TurretRenderState<T> state, float partialTicks, Vec3 cameraPosition) {
        state.turret.sample(turret, partialTicks);
        state.turret.emit();
    }

    @Override
    protected void submitUndeclared(TurretRenderState<T> state, PoseStack poseStack, SubmitSink sink,
                                    CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(state.turret.offsetX, 0D, state.turret.offsetZ);
        submitEffects(state, poseStack, sink);
        poseStack.popPose();
    }
}
