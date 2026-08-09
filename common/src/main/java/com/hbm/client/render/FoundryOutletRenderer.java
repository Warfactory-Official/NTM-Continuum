package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityFoundryOutlet;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Renderer for the foundry outlet / slagtap: the facing-rotated OBJ body plus the falling molten
 * stream while it's passing material (the old {@code HbmEffectNT.Foundry} particle, as a tinted
 * column). Yaw map from the old forge_marker blockstate: N→90, S→270, W→0, E→180.
 */
public class FoundryOutletRenderer<T extends TileEntityFoundryOutlet> implements BlockEntityRenderer<T, FoundryOutletRenderer.State> {

    public static class State extends BlockEntityRenderState {
        public float yaw;
        /** ForgeDirection ordinal of the block's FACING (for the stream geometry). */
        public int facingOrdinal = 2;
        public int pourColor = -1;
        public float pourLen;
    }

    private final WavefrontObj body;
    /** material name → its render type (multi-texture OBJ, resolved from the .mtl). */
    private final java.util.Map<String, RenderType> materialTypes = new java.util.LinkedHashMap<>();
    private final RenderType streamType;

    public FoundryOutletRenderer(String objPath) {
        this.body = WavefrontObj.load(Identifier.fromNamespaceAndPath(Constants.MOD_ID, objPath));
        var mtl = WavefrontObj.loadMtl(Identifier.fromNamespaceAndPath(Constants.MOD_ID, objPath.replace(".obj", ".mtl")));
        for (String mat : body.materialNames()) {
            Identifier tex = mtl.get(mat);
            if (tex != null) materialTypes.put(mat, RenderTypes.entityCutout(tex));
        }
        // Stream: opaque + depth-writing (entitySolid) so the self-overlapping tube's near faces occlude
        // the far ones — translucent-emissive doesn't write depth, so sides vanish with view angle. Solid
        // shades by the vertex normal (all up → uniform, unlike entityCutout's winding-based per-face
        // darkening); it culls, so the stream is double-wound (FoundryRenderHelper.quad). lava_gray is opaque.
        this.streamType = RenderTypes.entitySolid(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/models/machines/lava_gray.png"));
    }

    @Override public State createRenderState() { return new State(); }
    @Override public boolean shouldRender(T be, Vec3 cameraPosition) { return true; }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public int getViewDistance() { return 128; }

    @Override
    public void extractRenderState(T be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        BlockState bs = be.getBlockState();
        var facing = bs.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? bs.getValue(BlockStateProperties.HORIZONTAL_FACING) : net.minecraft.core.Direction.NORTH;
        state.facingOrdinal = facing.get3DDataValue();
        state.yaw = switch (facing) {
            case SOUTH -> 270F;
            case WEST -> 0F;
            case EAST -> 180F;
            default -> 90F; // NORTH
        };
        state.pourColor = be.pourColor;
        state.pourLen = be.pourLen;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;

        // OBJ vertices are in 0..1 corner space; vanilla blockstate "y" rotations are clockwise about
        // the block centre → rotate by -yaw around (0.5, 0.5) (the anvil-renderer convention).
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(-s.yaw));
        ps.translate(-0.5F, 0F, -0.5F);
        for (var e : materialTypes.entrySet()) {
            String mat = e.getKey();
            col.submitCustomGeometry(ps, e.getValue(),
                    (pose, buffer) -> body.renderMaterial(mat, pose, buffer, light, OverlayTexture.NO_OVERLAY));
        }
        ps.popPose();

        if (s.pourColor >= 0 && s.pourLen > 0) {
            // Old particle: spawn at (0.5 − facing·0.125, y+0.125), dir = facing, base = 0, off = 0.375.
            com.hbm.lib.ForgeDirection facing = com.hbm.lib.ForgeDirection.getOrientation(s.facingOrdinal);
            float len = s.pourLen;
            int rgb = s.pourColor;
            ps.pushPose();
            ps.translate(0.5F - facing.offsetX * 0.125F, 0.125F, 0.5F - facing.offsetZ * 0.125F);
            col.submitCustomGeometry(ps, streamType,
                    (pose, buffer) -> FoundryRenderHelper.pourStream(pose, buffer, facing, len, 0F, 0.375F, rgb));
            ps.popPose();
        }
    }
}
