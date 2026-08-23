package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControl;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlAuto;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlManual;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.Renderers;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Sliding Lid mesh for manual, automatic, and dyeable control columns. */
public final class RBMKControlRodRenderer<T extends BlockEntityRBMKControl> implements CompatBER<T, RBMKControlRodRenderer.State> {
    private static final WavefrontObj RODS = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/rbmk_rods.obj"));
    private final String sheet;
    public RBMKControlRodRenderer(String sheet) { this.sheet = sheet; }
    @Override public State createRenderState() { return new State(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public int viewDistance() { return 256; }
    @Override public void extract(T be, State state, float partial, Vec3 camera) {
        state.level = Mth.lerp(partial, be.lastLevel, be.level);
        state.height = RBMKDials.getColumnHeight(be.getLevel());
        state.texture = be instanceof BlockEntityRBMKControlAuto ? "rbmk_control_auto" : sheet;
        if (be instanceof BlockEntityRBMKControlManual manual && manual.color != null)
            state.texture = "rbmk_control_" + manual.color.texture();
        // The old TESR sampled at pos.up(offset + 1): one above the shaft mouth, where the sky actually
        // reaches. Sampling lower lands in the occluded lattice and renders the lid near-black.
        state.lidLight = be.getLevel() == null ? state.lightCoords
                : Renderers.lightAt(be.getLevel(), be.getBlockPos().above(state.height + 1));
    }
    @Override public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose(); pose.translate(.5D, state.height + state.level, .5D);
        sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutout(texture(state.texture)),
                (last, buffer) -> RODS.renderPart("Lid", last, buffer, state.lidLight, OverlayTexture.NO_OVERLAY));
        pose.popPose();
    }
    private static Identifier texture(String name) { return Ids.of(Constants.MOD_ID, "textures/blocks/rbmk/" + name + ".png"); }
    public static final class State extends CompatRenderState { double level; int height; int lidLight; String texture; }
}
