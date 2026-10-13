package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.network.TileEntityPylonLarge;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/** 1.7 {@code RenderPylonLarge}: {@code pylon_large.obj} both-sided; old yaw map 2:90 4:135 3:0 5:45. */
public class RenderPylonLarge extends RenderPylonBase<TileEntityPylonLarge> {

    private final WavefrontObj model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/network/pylon_large.obj"));
    private final Surface surface = Surface.cutoutNoCull(Constants.MOD_ID + ":textures/models/network/pylon_large.png");

    @Override
    protected void setup(Gem.Model<PylonRenderState> declaration) {
        declaration.offScreen().viewDistance(256);
        declaration.part(model.all(), surface).translate(0.5F, 0F, 0.5F).angle(Gem.Axes.Y, s -> s.yaw);
    }

    @Override
    public void extract(TileEntityPylonLarge be, PylonRenderState state, float partialTicks, Vec3 cameraPosition) {
        state.yaw = switch (be.facingMeta()) {
            case 2 -> 90F;
            case 4 -> 135F;
            case 5 -> 45F;
            default -> 0F;
        };
        extractWires(be, state);
    }
}
