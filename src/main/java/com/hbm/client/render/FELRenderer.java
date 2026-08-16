package com.hbm.client.render;

import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CameraInfo;
import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityFEL;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * FEL renderer — port of the 1.12.2 {@code RenderFEL}. The emitter body is static; what needs a custom
 * renderer is the <b>beam</b>, drawn with {@link BeamPronter} (the same helper the mining laser uses)
 * from the aperture out to whatever the beam is currently hitting.
 *
 * <p>Old yaw map N0/S180/W90/E270 — the standard facing map, so no base offset.
 *
 * <p>The beam gate here uses {@code pow(4)}, matching the server. (The GUI and the audio ramp use
 * {@code pow(2)} — an upstream inconsistency, preserved on all three sides.)
 */
public class FELRenderer implements CompatBER<TileEntityFEL, FELRenderState> {

    private final WavefrontObj model;
    private final RenderType renderType;

    public FELRenderer() {
        this.model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/fel.obj"));
        this.renderType = CompatRenderTypes.entityCutout(
                dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/fel.png"));
    }

    @Override public FELRenderState createRenderState() { return new FELRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityFEL be, Vec3 cameraPosition) { return true; }
    /** The beam can be 24 blocks long, so the machine must stay visible from well outside its own cell. */
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityFEL be, FELRenderState state, float partialTicks,
                                   Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (ForgeDirection.getOrientation(meta - BlockDummyable.offset)) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F; // NORTH
        };

        // The visible beam runs from the aperture (3 blocks in) to whatever it hit.
        state.length = be.distance - 3;
        state.beam = be.isOn && be.mode != EnumWavelengths.NULL && state.length > 0
                && be.power > TileEntityFEL.powerReq * Math.pow(4, be.mode.ordinal());
        state.color = be.mode.renderedBeamColor == 0
                // VISIBLE has no fixed colour: the original cycles the hue very slowly (÷2000) at low
                // brightness (0.1), giving a dim shifting glow rather than a rainbow strobe.
                ? java.awt.Color.HSBtoRGB(gameTime(be) / 2000.0F, 0.5F, 0.1F) & 0xFFFFFF
                : be.mode.renderedBeamColor;
    }

    private static long gameTime(TileEntityFEL be) {
        return be.getLevel() != null ? be.getLevel().getGameTime() : 0L;
    }

    @Override
    public void submit(FELRenderState s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        col.submitCustomGeometry(ps, renderType,
                (pose, buffer) -> model.renderAll(pose, buffer, light, OverlayTexture.NO_OVERLAY));

        if (s.beam) {
            ps.pushPose();
            // Old: translate(0, 1.5, -1.5) then a straight beam of -(length + 1) along Z.
            ps.translate(0F, 1.5F, -1.5F);
            BeamPronter.prontBeam(ps, col, new Vec3(0, 0, -s.length - 1), s.color, 0xFFFFFF, 3, 0.0625F);
            ps.popPose();
        }

        ps.popPose();
    }
}
