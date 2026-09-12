package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityHeatBoiler;
import com.hbm.util.GameTime;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Heat boiler: the intact shell and the burst husk, and the wobble it takes on when it is nearly full.
 *
 * <p>The intact shell and the burst husk are two meshes over the same texture, so the model states one
 * shell-shaped slot and the state says which mesh sits in it.
 */
public class HeatBoilerRenderer extends GemBER<TileEntityHeatBoiler, HeatBoilerRenderState> {

    private static final float BASE_YAW = 180F;

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/boiler.obj"));
    private final WavefrontObj burstModel = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/boiler_burst.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/boiler.png");

    @Override
    protected void setup(Gem.Model<HeatBoilerRenderState> declaration) {
        Gem.Part<HeatBoilerRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> BASE_YAW + state.yaw)
                .stretch(Gem.Axes.X, state -> 1F - state.sine)
                .stretch(Gem.Axes.Y, state -> 1F + state.sine)
                .stretch(Gem.Axes.Z, state -> 1F - state.sine);

        Geometry intact = model.all();
        Geometry burst = burstModel.all();
        machine.child(intact, surface)
                .mesh(state -> state.hasExploded ? burst : intact);
    }

    @Override
    public HeatBoilerRenderState createRenderState() {
        return new HeatBoilerRenderState();
    }

    @Override
    public void extract(TileEntityHeatBoiler be, HeatBoilerRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
        state.hasExploded = be.hasExploded;
        state.sine = !be.hasExploded && be.tanks[1].getFill() > be.tanks[1].getMaxFill() * 0.9
                ? (float) (Math.sin(GameTime.now() / 50D % (Math.PI * 2)) * 0.01D) : 0F;
    }
}
