package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityICF;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderICF}: the whole {@code icf.obj} in {@code icf.png}, back-face culled, smooth shaded, turned
 * by the core's facing (N 90, W 180, S 270, E 0 degrees about Y).
 */
public final class RenderICF extends GemBER<TileEntityICF, RenderICF.State> {

    public static final class State extends CompatRenderState {
        public float yaw;
    }

    private final WavefrontObj model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/reactors/icf.obj"));
    private final Surface skin = Surface.cutoutCull(Constants.MOD_ID + ":textures/models/machines/icf.png");

    /** The vessel is 17 blocks long; it must stay drawn while its core cell is off screen. */
    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.offScreen().viewDistance(256)
                .part(model.all(), skin)
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityICF icf, State state, float partialTicks, Vec3 cameraPosition) {
        state.yaw = switch (ForgeDirection.getOrientation(MachineCore.facingOrdinal(icf.getBlockState(), 2))) {
            case NORTH -> 90F;
            case WEST -> 180F;
            case SOUTH -> 270F;
            default -> 0F; // EAST
        };
    }
}
