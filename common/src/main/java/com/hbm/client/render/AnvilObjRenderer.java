package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.ModAnvilBlocks;
import com.hbm.blocks.machine.NTMAnvil;
import com.hbm.client.model.WavefrontObj;
import com.hbm.registry.RegistryObject;
import com.hbm.tileentity.TileEntityAnvil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws the 12 OBJ-modelled anvils, replacing their 1.12.2 {@code forge_marker} OBJ blockstates. One
 * renderer serves all of them (the per-block model + texture are looked up from the BE's block, like
 * {@link DecoObjRenderer}).
 *
 * <p>The anvil OBJs are authored in 0..1 block-local space, so the per-facing yaw (taken verbatim from
 * the old blockstate variants: west=0 / north=90 / east=180 / south=270, applied as the vanilla
 * clockwise blockstate "y" rotation) spins around the block centre.
 */
public class AnvilObjRenderer implements BlockEntityRenderer<TileEntityAnvil, AnvilRenderState> {

    private record Cfg(WavefrontObj model, RenderType renderType) {}

    private final Map<Block, Cfg> configs = new HashMap<>();

    // The old blockstate "y" per facing.
    private static final Map<Direction, Float> YAW = Map.of(
            Direction.WEST, 0F, Direction.NORTH, 90F, Direction.EAST, 180F, Direction.SOUTH, 270F);

    public AnvilObjRenderer() {
        for (RegistryObject<NTMAnvil> anvil : ModAnvilBlocks.ANVILS) {
            String id = anvil.getId().getPath();
            configs.put(anvil.get(), new Cfg(
                    WavefrontObj.load(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "models/anvils/" + id + ".obj")),
                    RenderTypes.entityCutout(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/models/anvils/" + id + ".png"))));
        }
    }

    @Override
    public AnvilRenderState createRenderState() {
        return new AnvilRenderState();
    }

    @Override
    public void extractRenderState(TileEntityAnvil blockEntity, AnvilRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        BlockState bs = blockEntity.getBlockState();
        state.block = bs.getBlock();
        state.yaw = bs.hasProperty(NTMAnvil.FACING) ? YAW.getOrDefault(bs.getValue(NTMAnvil.FACING), 0F) : 0F;
    }

    @Override
    public void submit(AnvilRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Cfg cfg = configs.get(state.block);
        if (cfg == null) return;
        poseStack.pushPose();
        // Vanilla blockstate "y" rotations are clockwise seen from above -> negative around +Y, about the
        // block centre (the OBJ vertices are in 0..1 corner space).
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        collector.submitCustomGeometry(poseStack, cfg.renderType,
                (pose, buffer) -> cfg.model.renderAll(pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
        poseStack.popPose();
    }
}
