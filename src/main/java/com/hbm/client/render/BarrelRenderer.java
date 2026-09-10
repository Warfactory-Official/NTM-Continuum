package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.BlockFluidBarrel;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityBarrel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a drum and the short connector that bridges it to a neighbouring drum. Ported from 1.12.2's
 * Forge-blockstate submodel setup, which composed the same two OBJ pieces per connected side.
 *
 * <p>Each variant is its own model and texture, so both are looked up from the block once and cached;
 * the connector borrows the drum's flipped texture, which is what makes a row read as one vessel.
 */
public final class BarrelRenderer implements CompatBER<TileEntityBarrel, BarrelRenderer.State> {

    private static final WavefrontObj CONNECTOR =
            WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/machines/barrel_connector.obj"));
    private static final Map<Block, WavefrontObj> MODELS = new HashMap<>();
    private static final Map<Block, Identifier> TEXTURES = new HashMap<>();
    private static final Map<Block, Identifier> FLIPPED = new HashMap<>();

    public BarrelRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityBarrel barrel, State state, float partial, Vec3 camera) {
        BlockState block = barrel.getBlockState();
        if (!(block.getBlock() instanceof BlockFluidBarrel)) return;

        String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block.getBlock()).getPath();
        state.model = MODELS.computeIfAbsent(block.getBlock(),
                b -> WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/machines/" + name + ".obj")));
        state.texture = TEXTURES.computeIfAbsent(block.getBlock(),
                b -> Ids.of(Constants.MOD_ID, "textures/blocks/" + name + ".png"));
        state.flipped = FLIPPED.computeIfAbsent(block.getBlock(),
                b -> Ids.of(Constants.MOD_ID, "textures/blocks/" + name + "_flipped.png"));

        state.posX = block.getValue(BlockFluidBarrel.CONN_POS_X);
        state.negX = block.getValue(BlockFluidBarrel.CONN_NEG_X);
        state.posZ = block.getValue(BlockFluidBarrel.CONN_POS_Z);
        state.negZ = block.getValue(BlockFluidBarrel.CONN_NEG_Z);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        if (state.model == null) return;

        pose.pushPose();
        pose.translate(.5D, 0D, .5D);
        sink.submitCustomGeometry(pose, CompatRenderTypes.entitySolid(state.texture),
                (last, buffer) -> state.model.renderAll(last, buffer, state.lightCoords,
                        OverlayTexture.NO_OVERLAY));

        connector(state, pose, sink, state.posX, 0F);
        connector(state, pose, sink, state.posZ, 90F);
        connector(state, pose, sink, state.negX, 180F);
        connector(state, pose, sink, state.negZ, 270F);
        pose.popPose();
    }

    private static void connector(State state, PoseStack pose, SubmitSink sink, boolean present, float yaw) {
        if (!present) return;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        sink.submitCustomGeometry(pose, CompatRenderTypes.entitySolid(state.flipped),
                (last, buffer) -> CONNECTOR.renderAll(last, buffer, state.lightCoords,
                        OverlayTexture.NO_OVERLAY));
        pose.popPose();
    }

    public static final class State extends CompatRenderState {
        WavefrontObj model;
        Identifier texture;
        Identifier flipped;
        boolean posX, negX, posZ, negZ;
    }
}
