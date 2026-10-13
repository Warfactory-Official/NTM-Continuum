package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.network.TileEntityConnector;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderConnector} / {@code RenderConnectorSuper}: the connector mesh at the block centre, turned
 * by the mount side (X then Z, the old {@code glRotated} pairs), then dropped half a block to sit on the wall.
 */
public class RenderConnector<T extends TileEntityConnector> extends RenderPylonBase<T> {

    private final WavefrontObj model;
    private final Surface surface;

    public RenderConnector(boolean superConnector) {
        String name = superConnector ? "connector_super" : "connector";
        model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/network/" + name + ".obj"));
        surface = Surface.cutoutCull(Constants.MOD_ID + ":textures/models/network/" + name + ".png");
    }

    @Override
    protected void setup(Gem.Model<PylonRenderState> declaration) {
        declaration.offScreen().viewDistance(256);
        declaration.part(model.all(), surface)
                .translate(0.5F, 0.5F, 0.5F)
                .angle(Gem.Axes.X, s -> s.rotX)
                .angle(Gem.Axes.Z, s -> s.rotZ)
                .translate(0F, -0.5F, 0F);
    }

    @Override
    public void extract(T be, PylonRenderState state, float partialTicks, Vec3 cameraPosition) {
        switch (be.placedSide().ordinal()) {
            case 0 -> { state.rotX = 180F; state.rotZ = 0F; }
            case 2 -> { state.rotX = 90F; state.rotZ = 180F; }
            case 3 -> { state.rotX = 90F; state.rotZ = 0F; }
            case 4 -> { state.rotX = 90F; state.rotZ = 90F; }
            case 5 -> { state.rotX = 90F; state.rotZ = 270F; }
            default -> { state.rotX = 0F; state.rotZ = 0F; }
        }
        extractWires(be, state);
    }
}
