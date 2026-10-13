package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.network.TileEntitySubstation;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/** 1.7 {@code RenderSubstation}: {@code substation.obj}, culled; old yaw map 4,5:0 and 2,3:90. */
public class RenderSubstation extends RenderPylonBase<TileEntitySubstation> {

    private final WavefrontObj model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/network/substation.obj"));
    private final Surface surface = Surface.cutoutCull(Constants.MOD_ID + ":textures/models/network/substation.png");

    @Override
    protected void setup(Gem.Model<PylonRenderState> declaration) {
        declaration.offScreen().viewDistance(256);
        declaration.part(model.all(), surface).translate(0.5F, 0F, 0.5F).angle(Gem.Axes.Y, s -> s.yaw);
    }

    @Override
    public void extract(TileEntitySubstation be, PylonRenderState state, float partialTicks, Vec3 cameraPosition) {
        state.yaw = switch (be.facingMeta()) {
            case 2, 3 -> 90F;
            default -> 0F;
        };
        extractWires(be, state);
    }
}
