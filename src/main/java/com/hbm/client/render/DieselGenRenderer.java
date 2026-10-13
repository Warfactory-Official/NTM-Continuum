package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.MachineDiesel;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineDiesel;
import com.hbm.util.GameTime;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Diesel generator: a static frame and the engine block jittering on it while it runs. */
public class DieselGenRenderer extends GemBER<TileEntityMachineDiesel, DieselGenRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/dieselgen.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/dieselgen.png");

    @Override
    protected void setup(Gem.Model<DieselGenRenderState> declaration) {
        Gem.Part<DieselGenRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        machine.child(model.part("Generator"), surface);
        machine.child(model.part("Engine"), surface)
                .slide(Gem.Axes.X, state -> state.swingFront)
                .slide(Gem.Axes.Z, state -> state.swingSide);
    }

    @Override
    public DieselGenRenderState createRenderState() {
        return new DieselGenRenderState();
    }

    /** Old meta yaw map (meta = EnumFacing ordinal): S(3)=0, E(5)=90, N(2)=180, W(4)=270. */
    @Override
    public void extract(TileEntityMachineDiesel be, DieselGenRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        Direction facing = bs.hasProperty(MachineDiesel.FACING) ? bs.getValue(MachineDiesel.FACING) : Direction.NORTH;
        state.yaw = switch (facing) {
            case EAST -> 90F;
            case NORTH -> 180F;
            case WEST -> 270F;
            default -> 0F;
        };
        state.running = be.wasOn;
        state.swingFront = state.running ? (float) (Math.sin(GameTime.now() / 25D) * 0.005D) : 0F;
        state.swingSide = state.running ? (float) (Math.sin(GameTime.now() / 50D) * 0.005D) : 0F;
    }
}
