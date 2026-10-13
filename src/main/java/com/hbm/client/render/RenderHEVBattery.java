package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.network.TileEntityHEVBattery;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;

/** 1.7 {@code RenderBattery}: {@code battery.obj} skinned with the block's own {@code hev_battery} tile, centred on the cell. */
public class RenderHEVBattery extends GemBER<TileEntityHEVBattery, CompatRenderState> {

    private final WavefrontObj model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/blocks/battery.obj"));
    private final Surface surface = Surface.cutoutCull(Constants.MOD_ID + ":textures/blocks/hev_battery.png");

    @Override
    protected void setup(Gem.Model<CompatRenderState> declaration) {
        declaration.part(model.all(), surface).translate(0.5F, 0F, 0.5F);
    }

    @Override
    public CompatRenderState createRenderState() {
        return new CompatRenderState();
    }
}
