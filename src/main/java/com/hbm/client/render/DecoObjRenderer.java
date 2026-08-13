package com.hbm.client.render;

import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.CameraInfo;
import com.hbm.Constants;
import com.hbm.blocks.ModDecoBlocks;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.deco.TileEntityDeco;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws the OBJ-modelled decorative blocks (boxcar / boat / the 6 satellites), replacing the 1.12.2
 * {@code StaticDecoBakedModels} custom baked models. One renderer serves all of them: it looks up the
 * per-block {@link Config} (model parts + transform style + per-facing yaw) from the block-entity's block.
 *
 * <p>The {@code poseStack} sequences below mirror the old {@code compose(...)} matrices verbatim — since
 * {@code compose(a,b,c) = a·b·c} and {@code poseStack} composes calls in the same order, calling the ops
 * in argument order reproduces the exact old placement (the OBJ vertices are the same files, and both the
 * old bake and this BER output into block-local space).
 */
public class DecoObjRenderer implements CompatBER<TileEntityDeco, DecoRenderState> {

    private enum Style { BOXCAR, BOAT, SAT, STEEL }

    private record Part(WavefrontObj model, RenderType renderType) {}

    private record Config(List<Part> parts, Style style, Map<Direction, Float> yaw) {}

    private final Map<Block, Config> configs = new HashMap<>();

    public DecoObjRenderer() {
        // yaw maps copied from StaticDecoBakedModels (boxcarYaw / satelliteYaw / resonatorYaw).
        Map<Direction, Float> boxcarYaw = yawMap(270F, 90F, 0F, 180F);
        Map<Direction, Float> satYaw = yawMap(180F, 0F, 90F, 270F);
        Map<Direction, Float> resonatorYaw = yawMap(180F, 0F, 270F, 90F);
        Map<Direction, Float> legacyYaw = yawMap(180F, 0F, 90F, 270F);
        Map<Direction, Float> noYaw = yawMap(0F, 0F, 0F, 0F);

        // steel structural pieces: generated OBJs (from the old ModelBase boxes, in pixel units).
        put(ModDecoBlocks.steel_wall.get(), Style.STEEL, legacyYaw, part("models/deco/steel_wall.obj", "deco/steelwall"));
        put(ModDecoBlocks.steel_corner.get(), Style.STEEL, legacyYaw, part("models/deco/steel_corner.obj", "deco/steelcorner"));
        // steel_roof has no per-facing rotation in the old bake (only rotateZ 180); noYaw keeps it fixed.
        put(ModDecoBlocks.steel_roof.get(), Style.STEEL, noYaw, part("models/deco/steel_roof.obj", "deco/steelroof"));

        put(ModDecoBlocks.boxcar.get(), Style.BOXCAR, boxcarYaw,
                part("models/boxcar.obj", "misc/boxcar"));
        put(ModDecoBlocks.boat.get(), Style.BOAT, noYaw,
                part("models/duchessgambit.obj", "misc/duchessgambit"));

        // Satellites = a shared sat_base.obj + the per-type dish OBJ (except foeq, one combined mesh).
        Part base = part("models/sat_base.obj", "sat/sat_base");
        put(ModDecoBlocks.deco_sat_mapper.get(), Style.SAT, satYaw, base, part("models/sat_mapper.obj", "sat/sat_mapper"));
        put(ModDecoBlocks.deco_sat_radar.get(), Style.SAT, satYaw, base, part("models/sat_radar.obj", "sat/sat_radar"));
        put(ModDecoBlocks.deco_sat_scanner.get(), Style.SAT, satYaw, base, part("models/sat_scanner.obj", "sat/sat_scanner"));
        put(ModDecoBlocks.deco_sat_laser.get(), Style.SAT, satYaw, base, part("models/sat_laser.obj", "sat/sat_laser"));
        put(ModDecoBlocks.deco_sat_resonator.get(), Style.SAT, resonatorYaw, base, part("models/sat_resonator.obj", "sat/sat_resonator"));
        put(ModDecoBlocks.deco_sat_foeq.get(), Style.SAT, satYaw, part("models/sat_foeq.obj", "sat/sat_foeq"));
    }

    private static Map<Direction, Float> yawMap(float north, float south, float west, float east) {
        Map<Direction, Float> m = new HashMap<>();
        m.put(Direction.NORTH, north);
        m.put(Direction.SOUTH, south);
        m.put(Direction.WEST, west);
        m.put(Direction.EAST, east);
        return m;
    }

    private static Part part(String obj, String texture) {
        return new Part(WavefrontObj.load(Identifier.fromNamespaceAndPath(Constants.MOD_ID, obj)),
                CompatRenderTypes.entityCutout(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/models/" + texture + ".png")));
    }

    private void put(Block block, Style style, Map<Direction, Float> yaw, Part... parts) {
        configs.put(block, new Config(List.of(parts), style, yaw));
    }

    @Override
    public DecoRenderState createRenderState() {
        return new DecoRenderState();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extract(TileEntityDeco blockEntity, DecoRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = blockEntity.getBlockState();
        state.block = bs.getBlock();
        state.yaw = 0F;
        Config cfg = configs.get(state.block);
        if (cfg != null && bs.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state.yaw = cfg.yaw.getOrDefault(bs.getValue(BlockStateProperties.HORIZONTAL_FACING), 0F);
        }
    }

    @Override
    public void submit(DecoRenderState state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        Config cfg = configs.get(state.block);
        if (cfg == null) return;
        poseStack.pushPose();
        switch (cfg.style) {
            case BOXCAR -> {
                poseStack.translate(0.5F, 0.0F, 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
            }
            case BOAT -> poseStack.translate(0.5F, 0.0F, 0.5F);
            case STEEL -> {
                // compose(translate(0.5,1.5,0.5), rotateZ(180), rotateY(legacyFacingYaw)) + the ModelBase 0.0625 scale.
                poseStack.translate(0.5F, 1.5F, 0.5F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(180F));
                poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
                poseStack.scale(0.0625F, 0.0625F, 0.0625F);
            }
            case SAT -> {
                // The port's BER basis is the old bake's post-flip basis (cf. BombObjRenderer), so drop the
                // old rotateZ(180)+1.5 Y-offset compensation; keep the dish-orienting rotateY(90)+yaw.
                poseStack.translate(0.5F, 0.0F, 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90F));
                poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
            }
        }
        for (Part p : cfg.parts) {
            collector.submitCustomGeometry(poseStack, p.renderType,
                    (pose, buffer) -> p.model.renderAll(pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
        }
        poseStack.popPose();
    }
}
