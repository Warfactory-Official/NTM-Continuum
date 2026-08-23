package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKDebris;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/** Draws the four wreck blocks with their authored pribris meshes rather than cube placeholders. */
public final class RBMKDebrisBlockRenderer implements CompatBER<BlockEntityRBMKDebris, RBMKDebrisBlockRenderer.State> {
    private static final WavefrontObj[] MODELS = {
            obj("pribris.obj"), obj("pribris_burning.obj"), obj("pribris_radiating.obj"), obj("pribris_digamma.obj")};
    private static final Identifier[] TEXTURES = {
            texture("rbmk_debris"), texture("rbmk_debris_burning"),
            texture("rbmk_debris_radiating"), texture("rbmk_debris_digamma")};

    @Override public State createRenderState() { return new State(); }
    @Override public void extract(BlockEntityRBMKDebris be, State state, float partial, Vec3 camera) {
        Block block = be.getBlockState().getBlock();
        state.variant = block == ModRBMKBlocks.rbmk_debris_burning.get() ? 1
                : block == ModRBMKBlocks.rbmk_debris_radiating.get() ? 2
                : block == ModRBMKBlocks.rbmk_debris_digamma.get() ? 3 : 0;
    }
    @Override public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutout(TEXTURES[state.variant]),
                (last, buffer) -> MODELS[state.variant].renderAll(last, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
    }
    private static WavefrontObj obj(String name) { return WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/" + name)); }
    private static Identifier texture(String name) { return Ids.of(Constants.MOD_ID, "textures/blocks/rbmk/" + name + ".png"); }
    public static final class State extends CompatRenderState { int variant; }
}
