package com.hbm.client.render;

import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.CameraInfo;
import com.hbm.Constants;
import com.hbm.blocks.ModAnvilBlocks;
import com.hbm.blocks.machine.NTMAnvil;
import com.hbm.client.model.WavefrontObj;
import dev.golem.core.registry.RegistryObject;
import com.hbm.tileentity.TileEntityAnvil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
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
public class AnvilObjRenderer implements CompatBER<TileEntityAnvil, AnvilRenderState> {

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
                    CompatRenderTypes.entityCutout(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/models/anvils/" + id + ".png"))));
        }
    }

    @Override
    public AnvilRenderState createRenderState() {
        return new AnvilRenderState();
    }

    @Override
    public void extract(TileEntityAnvil blockEntity, AnvilRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = blockEntity.getBlockState();
        state.block = bs.getBlock();
        state.yaw = bs.hasProperty(NTMAnvil.FACING) ? YAW.getOrDefault(bs.getValue(NTMAnvil.FACING), 0F) : 0F;
    }

    @Override
    public void submit(AnvilRenderState state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
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
