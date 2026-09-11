package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.dfc.CoreComponent;
import com.hbm.client.model.WavefrontObj;
import com.hbm.client.render.BeamPronter.EnumBeamType;
import com.hbm.client.render.BeamPronter.EnumWaveType;
import com.hbm.tileentity.machine.dfc.TileEntityCoreEmitter;
import com.hbm.tileentity.machine.dfc.TileEntityCoreInjector;
import com.hbm.tileentity.machine.dfc.TileEntityCoreReceiver;
import com.hbm.tileentity.machine.dfc.TileEntityCoreStabilizer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The four aimed parts of a dark fusion core. Each draws its own body and, where it has one, the beam it
 * is currently running — all four are {@code RenderShape.INVISIBLE} blocks, so this is the whole
 * appearance.
 *
 * <p>The stabiliser shares the injector's mesh, as upstream does: there is no stabiliser model in the
 * assets, only a stabiliser texture over the injector's geometry.
 */
public final class CoreComponentRenderers {

    private CoreComponentRenderers() {}

    /** Shared body-placement and long-range visibility; subclasses add their own beam. */
    public abstract static class Base<T extends BlockEntity> implements CompatBER<T, CoreComponentRenderState> {

        private final WavefrontObj model;
        private final RenderType renderType;

        protected Base(String model, String texture) {
            this.model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/" + model + ".obj"));
            this.renderType = CompatRenderTypes.entityCutout(
                    dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/" + texture + ".png"));
        }

        @Override public CoreComponentRenderState createRenderState() { return new CoreComponentRenderState(); }
        @Override public boolean shouldRenderOffScreen() { return true; }
        @Override public boolean shouldRender(T be, Vec3 cameraPosition) { return true; }
        /** The emitter's beam reaches fifty blocks, so the machine has to stay visible from well outside it. */
        @Override public int getViewDistance() { return 256; }

        @Override
        public void extract(T be, CoreComponentRenderState state, float partialTicks, Vec3 cameraPosition) {
            state.facing = be.getBlockState().hasProperty(CoreComponent.FACING)
                    ? be.getBlockState().getValue(CoreComponent.FACING) : Direction.NORTH;
            state.gameTime = be.getLevel() != null ? be.getLevel().getGameTime() : 0L;
            extractBeam(be, state);
        }

        protected abstract void extractBeam(T be, CoreComponentRenderState state);

        @Override
        public void submit(CoreComponentRenderState s, PoseStack ps, SubmitSink col, CameraInfo camera) {
            int light = s.lightCoords;
            ps.pushPose();
            aim(ps, s.facing);

            col.submitCustomGeometry(ps, renderType,
                    (pose, buffer) -> model.renderAll(pose, buffer, light, OverlayTexture.NO_OVERLAY));

            if (s.beam > 0) {
                ps.translate(0F, 0.5F, 0F);
                submitBeam(s, ps, col);
            }

            ps.popPose();
        }

        protected void submitBeam(CoreComponentRenderState s, PoseStack ps, SubmitSink col) {}

        /**
         * Upstream's placement, transliterated: a quarter turn about Y, then the facing's own turn, with
         * the two vertical cases pushed clear of the cell before being tipped over.
         */
        private static void aim(PoseStack ps, Direction facing) {
            ps.translate(0.5F, 0F, 0.5F);
            ps.mulPose(Axis.YP.rotationDegrees(90F));
            switch (facing) {
                case DOWN -> {
                    ps.translate(0F, 0.5F, -0.5F);
                    ps.mulPose(Axis.XP.rotationDegrees(90F));
                }
                case UP -> {
                    ps.translate(0F, 0.5F, 0.5F);
                    ps.mulPose(Axis.XP.rotationDegrees(-90F));
                }
                case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(90F));
                case WEST -> ps.mulPose(Axis.YP.rotationDegrees(180F));
                case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(270F));
                default -> { }
            }
        }
    }

    /** The emitter, whose beam thickens with the energy it last pushed. */
    public static final class Emitter extends Base<TileEntityCoreEmitter> {

        public Emitter() {
            super("core_emitter", "core_emitter");
        }

        @Override
        protected void extractBeam(TileEntityCoreEmitter be, CoreComponentRenderState state) {
            state.beam = be.beam;
            state.output = be.prev;
        }

        @Override
        protected void submitBeam(CoreComponentRenderState s, PoseStack ps, SubmitSink col) {
            float width = (float) Math.max(1, Math.log10(Math.max(1L, s.output)) - 6) / 8F;
            Vec3 skeleton = new Vec3(0, 0, s.beam);
            BeamPronter.prontBeam(ps, col, skeleton, EnumWaveType.STRAIGHT, EnumBeamType.SOLID,
                    0, 1, 0F, 0x401500, 0x7F7F7F, 2, width);
            int segments = (int) (0.3F * s.beam / width);
            BeamPronter.prontBeam(ps, col, skeleton, EnumWaveType.RANDOM, EnumBeamType.SOLID,
                    (int) (s.gameTime % 1000), segments, width * 0.75F, 0x401500, 0x7F7F7F, 2, width * 0.5F);
            BeamPronter.prontBeam(ps, col, skeleton, EnumWaveType.RANDOM, EnumBeamType.SOLID,
                    (int) (s.gameTime % 1000) + 1, segments, width * 0.75F, 0x5B1D00, 0x7F7F7F, 2, width * 0.5F);
        }
    }

    /** The stabiliser, whose field beam is a straight core wrapped in two counter-rotating spirals. */
    public static final class Stabilizer extends Base<TileEntityCoreStabilizer> {

        public Stabilizer() {
            super("core_injector", "core_stabilizer");
        }

        @Override
        protected void extractBeam(TileEntityCoreStabilizer be, CoreComponentRenderState state) {
            state.beam = be.beam;
        }

        @Override
        protected void submitBeam(CoreComponentRenderState s, PoseStack ps, SubmitSink col) {
            Vec3 skeleton = new Vec3(0, 0, s.beam);
            BeamPronter.prontBeam(ps, col, skeleton, EnumWaveType.STRAIGHT, EnumBeamType.SOLID,
                    0, 1, 0F, 0x002333, 0x7F7F7F, 2, 0.125F);
            int spin = (int) (s.gameTime * -8 % 360);
            BeamPronter.prontBeam(ps, col, skeleton, EnumWaveType.SPIRAL, EnumBeamType.SOLID,
                    spin, s.beam * 3, 0.125F, 0x002333, 0x7F7F7F, 2, 0.04F);
            BeamPronter.prontBeam(ps, col, skeleton, EnumWaveType.SPIRAL, EnumBeamType.SOLID,
                    spin + 180, s.beam * 3, 0.125F, 0x003C56, 0x7F7F7F, 2, 0.04F);
        }
    }

    /** The injector, which draws one spiral per fuel it is actually carrying, in that fuel's colour. */
    public static final class Injector extends Base<TileEntityCoreInjector> {

        public Injector() {
            super("core_injector", "core_injector");
        }

        @Override
        protected void extractBeam(TileEntityCoreInjector be, CoreComponentRenderState state) {
            state.beam = be.beam;
            state.fluidColorA = be.tanks[0].getFill() > 0 ? be.tanks[0].getTankType().getColor() : -1;
            state.fluidColorB = be.tanks[1].getFill() > 0 ? be.tanks[1].getTankType().getColor() : -1;
        }

        @Override
        protected void submitBeam(CoreComponentRenderState s, PoseStack ps, SubmitSink col) {
            Vec3 skeleton = new Vec3(0, 0, s.beam);
            int spin = (int) (s.gameTime * -2 % 360);
            if (s.fluidColorA != -1) {
                BeamPronter.prontBeam(ps, col, skeleton, EnumWaveType.SPIRAL, EnumBeamType.SOLID,
                        spin, s.beam, 0.09F, s.fluidColorA, 0x7F7F7F, 3, 0.0625F);
            }
            if (s.fluidColorB != -1) {
                BeamPronter.prontBeam(ps, col, skeleton, EnumWaveType.SPIRAL, EnumBeamType.SOLID,
                        spin + 180, s.beam, 0.09F, s.fluidColorB, 0x7F7F7F, 3, 0.0625F);
            }
        }
    }

    /** The receiver, which only ever draws its body. */
    public static final class Receiver extends Base<TileEntityCoreReceiver> {

        public Receiver() {
            super("core_receiver", "core_receiver");
        }

        @Override
        protected void extractBeam(TileEntityCoreReceiver be, CoreComponentRenderState state) {
            state.beam = 0;
        }
    }
}
