package com.hbm.client.render;

import com.hbm.blocks.network.BlockCable;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Draws HBM's red cable as the real {@code cable_neo.obj} model, replacing the 1.12.2 baked model
 * ({@code BlockCableBakedModel}). Reads the six connection booleans from the {@link BlockCable} state
 * and draws the matching OBJ parts: a {@code Core} plus an arm per connected side, with the
 * straight-through optimisations {@code CX}/{@code CY}/{@code CZ} for a cable that only connects along
 * one axis. (The block renders {@code INVISIBLE}; this BER draws it.)
 *
 * <p>Mirrors the original part selection exactly, including the Z-axis quirk (a NORTH connection draws
 * the {@code posZ} part, SOUTH draws {@code negZ}).
 */
public class CableObjRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, CableRenderState> {

    private static final Identifier OBJ = Identifier.fromNamespaceAndPath("hbm", "models/blocks/cable_neo.obj");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("hbm", "textures/models/blocks/cable_neo.png");

    private final WavefrontObj model = WavefrontObj.load(OBJ);
    private final RenderType renderType = RenderTypes.entityCutout(TEXTURE);

    @Override
    public CableRenderState createRenderState() {
        return new CableRenderState();
    }

    @Override
    public void extractRenderState(T be, CableRenderState state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        BlockState bs = be.getBlockState();
        boolean pX = bs.getValue(BlockCable.EAST);
        boolean nX = bs.getValue(BlockCable.WEST);
        boolean pY = bs.getValue(BlockCable.UP);
        boolean nY = bs.getValue(BlockCable.DOWN);
        boolean pZ = bs.getValue(BlockCable.SOUTH);
        boolean nZ = bs.getValue(BlockCable.NORTH);
        state.parts = selectParts(pX, nX, pY, nY, pZ, nZ);
    }

    /** The 1.12.2 {@code BlockCableBakedModel.buildWorldQuads} part selection, verbatim. */
    private static Set<String> selectParts(boolean pX, boolean nX, boolean pY, boolean nY, boolean pZ, boolean nZ) {
        if (pX && nX && !pY && !nY && !pZ && !nZ) return Set.of("CX");
        if (!pX && !nX && pY && nY && !pZ && !nZ) return Set.of("CY");
        if (!pX && !nX && !pY && !nY && pZ && nZ) return Set.of("CZ");
        Set<String> parts = new HashSet<>();
        parts.add("Core");
        if (pX) parts.add("posX");
        if (nX) parts.add("negX");
        if (pY) parts.add("posY");
        if (nY) parts.add("negY");
        if (nZ) parts.add("posZ"); // mirrors the original (NORTH -> posZ)
        if (pZ) parts.add("negZ"); // mirrors the original (SOUTH -> negZ)
        return parts;
    }

    @Override
    public void submit(CableRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.parts.isEmpty()) return;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F); // OBJ is centred on the origin (-0.5..0.5)
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            for (String part : state.parts) {
                model.renderPart(part, pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY);
            }
        });
        poseStack.popPose();
    }
}
