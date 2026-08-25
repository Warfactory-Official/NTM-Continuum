package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.bomb.TileEntityLaunchPad;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The launch pad: the silo, plus <b>the missile standing on it</b> — 1.12.2's {@code RenderLaunchPad}.
 *
 * <p>The missile half is the point. A pad with a missile loaded shows it sitting on the pad, which is
 * how a player reads at a glance what is armed and whether the pad is loaded at all; the generic OBJ
 * renderer this replaces drew the silo alone, so a loaded pad looked identical to an empty one.
 *
 * <p>Upstream's geometry exactly: the silo at the pad's facing, then {@code translate(0, 1, 0)} and the
 * missile body drawn <em>inside the same matrix</em>, so it inherits the pad's rotation. Which body and
 * at what scale comes from {@link MissileModels}.
 *
 * <p>The missile comes from {@code TileEntityLaunchPad.toRender}, which the block entity syncs from
 * slot 0 in its network pack — the client never sees the pad's inventory otherwise.
 */
public class LaunchPadRenderer implements CompatBER<TileEntityLaunchPad, LaunchPadRenderState> {

    /** Upstream's TESR base yaw for the silo. */
    private static final float BASE_YAW = 90F;

    private final WavefrontObj silo = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/weapons/launch_pad_silo.obj"));
    private final RenderType siloType = CompatRenderTypes.entityCutout(
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/launchpad/silo.png"));

    @Override
    public LaunchPadRenderState createRenderState() {
        return new LaunchPadRenderState();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        // The silo and a tier-4 missile stand well outside the core's own block section.
        return true;
    }

    @Override
    public boolean shouldRender(TileEntityLaunchPad blockEntity, Vec3 cameraPosition) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extract(TileEntityLaunchPad pad, LaunchPadRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = pad.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 4 -> 90F;   // WEST
            case 3 -> 180F;  // SOUTH
            case 5 -> 270F;  // EAST
            default -> 0F;   // NORTH (2)
        };

        ItemStack loaded = pad.toRender;
        state.missile = loaded == null || loaded.isEmpty() ? null : MissileModels.get(loaded.getItem());
    }

    @Override
    public void submit(LaunchPadRenderState state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(BASE_YAW + state.yaw));

        collector.submitCustomGeometry(poseStack, siloType,
                (pose, buffer) -> silo.renderAll(pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY, true));

        if (state.missile != null) {
            poseStack.translate(0F, 1F, 0F);
            float scale = state.missile.scale();
            if (scale != 1.0F) poseStack.scale(scale, scale, scale);
            MissileModels.MissileModel missile = state.missile;
            collector.submitCustomGeometry(poseStack, missile.renderType(),
                    (pose, buffer) -> missile.model().renderAll(pose, buffer, state.lightCoords,
                            OverlayTexture.NO_OVERLAY, true));
        }

        poseStack.popPose();
    }
}
