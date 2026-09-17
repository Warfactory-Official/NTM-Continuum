package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineArcFurnaceLarge;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/** Large Arc Furnace BER: dynamic contents, lid, electrode states, cables, and molten pour stream. */
public class ArcFurnaceRenderer extends GemBER<TileEntityMachineArcFurnaceLarge, ArcFurnaceRenderer.State> {

    public static class State extends CompatRenderState {
        public ForgeDirection direction = ForgeDirection.NORTH;
        public float lid;
        public boolean progressing;
        public boolean hasMaterial;
        public long gameTime;
        public int meltAmount;
        public int pourColor = -1;
        public float pourLength;
        public final byte[] electrodes = new byte[3];
        /** Idle shake of the lid and the cable sway, zero while the furnace is not working. */
        public float lidJitter;
        public float cableSwing;
    }

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/arc_furnace.obj"));
    private final Surface body = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/arc_furnace.png");
    private final Surface hot = Surface.emissive(
            Constants.MOD_ID + ":textures/models/machines/arc_furnace.png");
    private final RenderType streamType = CompatRenderTypes.entitySolid(
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/lava_gray.png"));

    /**
     * The lid and everything mounted on it ride together, and each electrode slot shows one of four
     * things: nothing, a fresh rod, a used one glowing, or a spent stub.
     */
    @Override
    protected void setup(Gem.Model<State> declaration) {
        Gem.Part<State> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> 90F + yaw(state.direction));

        machine.child(model.part("Furnace"), body);
        machine.child(model.part("ContentsHot"), hot)
                .visibleIf(state -> state.meltAmount > 0)
                .slide(Gem.Axes.Y, state -> -1.75F
                        + state.meltAmount * 1.75F / TileEntityMachineArcFurnaceLarge.MAX_LIQUID);
        machine.child(model.part("ContentsCold"), body)
                .visibleIf(state -> state.meltAmount <= 0 && state.hasMaterial);

        Gem.Part<State> lid = machine.joint()
                .slide(Gem.Axes.Y, state -> 2F * state.lid)
                .slide(Gem.Axes.Z, state -> state.lidJitter);
        lid.child(model.part("Lid"), body);
        for (int i = 0; i < 3; i++) {
            final int slot = i;
            String index = String.valueOf(i + 1);
            lid.child(model.part("Ring" + index), body)
                    .visibleIf(state -> state.electrodes[slot] != TileEntityMachineArcFurnaceLarge.ELECTRODE_NONE);
            lid.child(model.part("Electrode" + index), body)
                    .visibleIf(state -> state.electrodes[slot] == TileEntityMachineArcFurnaceLarge.ELECTRODE_FRESH);
        }
        for (int i = 0; i < 3; i++) {
            final int slot = i;
            String index = String.valueOf(i + 1);
            lid.child(model.part("Electrode" + index + "Hot"), hot)
                    .visibleIf(state -> state.electrodes[slot] == TileEntityMachineArcFurnaceLarge.ELECTRODE_USED);
            lid.child(model.part("Electrode" + index + "Short"), hot)
                    .visibleIf(state -> state.electrodes[slot] != TileEntityMachineArcFurnaceLarge.ELECTRODE_NONE
                            && state.electrodes[slot] != TileEntityMachineArcFurnaceLarge.ELECTRODE_FRESH
                            && state.electrodes[slot] != TileEntityMachineArcFurnaceLarge.ELECTRODE_USED);
        }
        for (int i = 0; i < 3; i++) {
            final int slot = i;
            float z = i == 0 ? 0.5F : i == 2 ? -0.5F : 0F;
            lid.child(model.part("Cable" + (i + 1)), body)
                    .visibleIf(state -> state.electrodes[slot] != TileEntityMachineArcFurnaceLarge.ELECTRODE_NONE)
                    .translate(0F, 5.5F, z)
                    .angle(Gem.Axes.X, state -> state.cableSwing)
                    .translate(0F, -5.5F, -z);
        }
    }

    @Override public State createRenderState() { return new State(); }

    @Override
    public void extract(TileEntityMachineArcFurnaceLarge be, State state, float partialTicks, Vec3 cameraPosition) {
        int meta = MachineCore.facingOrdinal(be.getBlockState(), 2);
        state.direction = ForgeDirection.getOrientation(meta);
        state.lid = be.prevLid + (be.lid - be.prevLid) * partialTicks;
        state.progressing = be.isProgressing;
        state.hasMaterial = be.hasMaterial;
        state.gameTime = be.getLevel() == null ? 0L : be.getLevel().getGameTime();
        state.meltAmount = TileEntityMachineArcFurnaceLarge.getStackAmount(be.liquids);
        state.pourColor = be.pourColor;
        state.pourLength = be.pourLen;
        System.arraycopy(be.electrodes, 0, state.electrodes, 0, state.electrodes.length);
        state.lidJitter = state.progressing ? (float) Math.sin((state.gameTime + 0.5F) * 1D) * 0.005F : 0F;
        state.cableSwing = state.progressing ? (float) Math.sin((state.gameTime + 0.5F) / 2D) * 30F : 0F;
    }

    /** The pour stream's length is the gap it has to cross, so it is built per frame, not placed. */
    @Override
    protected void submitUndeclared(State state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90F + yaw(state.direction)));
        if (state.pourColor >= 0 && state.pourLength > 0F) {
            poseStack.pushPose();
            poseStack.translate(state.direction.offsetX * 2.875F, 1F,
                    state.direction.offsetZ * 2.875F);
            int color = state.pourColor;
            collector.submitCustomGeometry(poseStack, streamType,
                    (pose, buffer) -> FoundryRenderHelper.pourStream(pose, buffer, state.direction,
                            state.pourLength, 0.625F, 0.625F, color));
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static float yaw(ForgeDirection direction) {
        return switch (direction) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
    }
}
