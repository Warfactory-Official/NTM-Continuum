package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityFoundryOutlet;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Renderer for the foundry outlet / slagtap: the facing-rotated OBJ body plus the falling molten
 * stream while it's passing material (the old {@code HbmEffectNT.Foundry} particle, as a tinted
 * column). Yaw map from the old forge_marker blockstate: N->90, S->270, W->0, E->180.
 */
public class FoundryOutletRenderer<T extends TileEntityFoundryOutlet> extends GemBER<T, FoundryOutletRenderer.State> {

    public static class State extends CompatRenderState {
        public float yaw;
        /** ForgeDirection ordinal of the block's FACING (for the stream geometry). */
        public int facingOrdinal = 2;
        public int pourColor = -1;
        public float pourLen;
    }

    private final WavefrontObj body;
    /** material name -> the surface it wears (multi-texture OBJ, resolved from the .mtl). */
    private final Map<String, Surface> materials = new LinkedHashMap<>();
    private final RenderType streamType;

    public FoundryOutletRenderer(String objPath) {
        this.body = WavefrontObj.load(Ids.of(Constants.MOD_ID, objPath));
        var mtl = WavefrontObj.loadMtl(Ids.of(Constants.MOD_ID, objPath.replace(".obj", ".mtl")));
        for (String mat : body.materialNames()) {
            Identifier tex = mtl.get(mat);
            if (tex != null) materials.put(mat, Surface.cutout(tex.toString()));
        }
        // The stream is a self-overlapping tube and must write depth, which translucent-emissive does
        // not; entitySolid shades by the vertex normal and culls, so the geometry is double-wound.
        this.streamType = CompatRenderTypes.entitySolid(Ids.of(Constants.MOD_ID, "textures/models/machines/lava_gray.png"));
    }

    /**
     * OBJ vertices are in 0..1 corner space; vanilla blockstate "y" rotations are clockwise about the
     * block centre, so the body turns by -yaw around (0.5, 0.5), the anvil-renderer convention.
     */
    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.offScreen().viewDistance(128);
        for (var material : materials.entrySet()) {
            declaration.part(body.material(material.getKey()), material.getValue())
                    .translate(0.5F, 0F, 0.5F)
                    .angle(Gem.Axes.Y, state -> -state.yaw)
                    .translate(-0.5F, 0F, -0.5F);
        }
    }

    @Override public State createRenderState() { return new State(); }

    @Override
    public void extract(T be, State state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        var facing = bs.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? bs.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
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

    /** The stream pours as far as it has to reach, so its geometry is a length, not a shape. */
    @Override
    protected void submitUndeclared(State s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        if (s.pourColor >= 0 && s.pourLen > 0) {
            // Old particle: spawn at (0.5 − facing·0.125, y+0.125), dir = facing, base = 0, off = 0.375.
            ForgeDirection facing = ForgeDirection.getOrientation(s.facingOrdinal);
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
