package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockDecoContainer;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.storage.TileEntityFileCabinet;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderFileCabinet}: the cabinet turned to its facing (north 180, south 0, west 270, east 90), in the
 * dusty green or steel skin, with each drawer slid out {@code 0.6875 * extent} along +Z, interpolated.
 */
public final class RenderFileCabinet extends GemBER<TileEntityFileCabinet, RenderFileCabinet.State> {

    public static final class State extends CompatRenderState {
        public float yaw;
        public boolean steel;
        public float lower;
        public float upper;
    }

    private final WavefrontObj model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/file_cabinet.obj"));
    private final Surface green = Surface.cutoutCull(Constants.MOD_ID + ":textures/models/file_cabinet.png");
    private final Surface steel = Surface.cutoutCull(Constants.MOD_ID + ":textures/models/file_cabinet_steel.png");

    @Override
    protected void setup(Gem.Model<State> declaration) {
        Gem.Part<State> root = declaration.joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);
        root.child(model.part("Cabinet"), green).skin(state -> state.steel ? steel : green);
        root.child(model.part("LowerDrawer"), green).skin(state -> state.steel ? steel : green)
                .slide(Gem.Axes.Z, state -> 0.6875F * state.lower);
        root.child(model.part("UpperDrawer"), green).skin(state -> state.steel ? steel : green)
                .slide(Gem.Axes.Z, state -> 0.6875F * state.upper);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityFileCabinet cabinet, State state, float interp, Vec3 cameraPosition) {
        BlockState bs = cabinet.getBlockState();
        state.yaw = switch (bs.getValue(BlockDecoContainer.FACING)) {
            case NORTH -> 180F;
            case WEST -> 270F;
            case EAST -> 90F;
            default -> 0F;
        };
        state.steel = bs.getValue(BlockDecoContainer.TYPE) == BlockDecoContainer.CabinetType.STEEL;
        state.lower = cabinet.prevLowerExtent + (cabinet.lowerExtent - cabinet.prevLowerExtent) * interp;
        state.upper = cabinet.prevUpperExtent + (cabinet.upperExtent - cabinet.prevUpperExtent) * interp;
    }
}
