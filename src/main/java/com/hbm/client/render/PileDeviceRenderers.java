package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.pile.BlockPileDevice;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.pile.TileEntityPileControl;
import com.hbm.tileentity.machine.pile.TileEntityPileLoader;
import com.hbm.tileentity.machine.pile.TileEntityPileVent;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** The three Chicago Pile device renderers, which differ only in which part of the model moves. */
public final class PileDeviceRenderers {

    private PileDeviceRenderers() {}

    /**
     * Shared chrome: load the model, face the device the way its meta says, and declare it. The devices
     * sit flush against a pile face, so the only orientation that matters is which of the four
     * horizontals the low two bits of the meta name.
     */
    private abstract static class Base<T extends BlockEntity> extends GemBER<T, PileDeviceRenderState> {

        protected final WavefrontObj model;
        protected final Surface surface;

        Base(String name) {
            this.model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/pile/" + name + ".obj"));
            this.surface = Surface.cutout(Constants.MOD_ID + ":textures/models/pile/" + name + ".png");
        }

        @Override public PileDeviceRenderState createRenderState() { return new PileDeviceRenderState(); }

        protected Gem.Part<PileDeviceRenderState> device(Gem.Model<PileDeviceRenderState> declaration) {
            return declaration.joint()
                    .translate(0.5F, 0F, 0.5F)
                    .angle(Gem.Axes.Y, state -> state.yaw);
        }

        protected void extractFacing(BlockEntity be, PileDeviceRenderState state) {
            BlockState blockState = be.getBlockState();
            int meta = blockState.hasProperty(BlockPileDevice.META) ? blockState.getValue(BlockPileDevice.META) : 0;
            state.yaw = switch (ForgeDirection.getOrientation(meta % 4 + 2)) {
                case NORTH -> 90F;
                case SOUTH -> 270F;
                case WEST -> 180F;
                default -> 0F;
            };
        }
    }

    /** The control rod: the whole rod slides up out of its channel as it is withdrawn. */
    public static final class Control extends Base<TileEntityPileControl> {

        public Control() { super("pile_control"); }

        @Override
        protected void setup(Gem.Model<PileDeviceRenderState> declaration) {
            Gem.Part<PileDeviceRenderState> device = device(declaration);
            device.child(model.part("Base"), surface);
            device.child(model.part("Rod"), surface)
                    .slide(Gem.Axes.Y, state -> state.motion * 0.75F);
        }

        @Override
        public void extract(TileEntityPileControl be, PileDeviceRenderState state, float partialTicks, Vec3 camera) {
            extractFacing(be, state);
            state.motion = (float) (be.lastExtraction + (be.extraction - be.lastExtraction) * partialTicks);
        }
    }

    /** The fuel loader: a lever swings over as the ram slides in, carrying the rod with it. */
    public static final class Loader extends Base<TileEntityPileLoader> {

        public Loader() { super("pile_loader"); }

        @Override
        protected void setup(Gem.Model<PileDeviceRenderState> declaration) {
            Gem.Part<PileDeviceRenderState> device = device(declaration);
            device.child(model.part("Loader"), surface);
            device.child(model.part("Lever"), surface)
                    .spin(Gem.Axes.Z, -0.1875F, 0.5F, 0F, state -> state.motion * 90F);

            Gem.Part<PileDeviceRenderState> slider = device.child(model.part("Slider"), surface)
                    .slide(Gem.Axes.X, state -> state.motion * -0.5F);
            slider.child(model.part("Rod"), surface).visibleIf(state -> state.loaded);
        }

        @Override
        public void extract(TileEntityPileLoader be, PileDeviceRenderState state, float partialTicks, Vec3 camera) {
            extractFacing(be, state);
            state.motion = (float) (be.lastTravel + (be.travel - be.lastTravel) * partialTicks);
            state.loaded = !be.syncStack.isEmpty();
        }
    }

    /** The vent: the fan spins whenever air is actually moving into the channel. */
    public static final class Vent extends Base<TileEntityPileVent> {

        public Vent() { super("pile_vent"); }

        @Override
        protected void setup(Gem.Model<PileDeviceRenderState> declaration) {
            Gem.Part<PileDeviceRenderState> device = device(declaration);
            device.child(model.part("Pipe"), surface);
            device.child(model.part("Fan"), surface)
                    .angle(Gem.Axes.Y, state -> state.motion);
        }

        @Override
        public void extract(TileEntityPileVent be, PileDeviceRenderState state, float partialTicks, Vec3 camera) {
            extractFacing(be, state);
            state.motion = be.lastFan + (be.fan - be.lastFan) * partialTicks;
        }
    }
}
