package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineFunnel;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/** Static three-texture OBJ renderer matching old MachineFunnel's Top/Bottom/Side draws. */
public final class RenderFunnel extends GemBER<TileEntityMachineFunnel, MachineRenderState> {
    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/blocks/funnel.obj"));
    private final Surface top = Surface.cutout(Constants.MOD_ID + ":textures/blocks/machine_funnel_top.png");
    private final Surface bottom = Surface.cutout(Constants.MOD_ID + ":textures/blocks/machine_funnel_bottom.png");
    private final Surface side = Surface.cutout(Constants.MOD_ID + ":textures/blocks/machine_funnel_side.png");

    @Override protected void setup(Gem.Model<MachineRenderState> declaration) {
        Gem.Part<MachineRenderState> body = declaration.offScreen().viewDistance(256)
                .joint().translate(0.5F, 0F, 0.5F);
        body.child(model.part("Top"), top);
        body.child(model.part("Bottom"), bottom);
        body.child(model.part("Side"), side);
    }

    @Override public MachineRenderState createRenderState() { return new MachineRenderState(); }
    @Override public void extract(TileEntityMachineFunnel be, MachineRenderState state, float partialTicks, Vec3 cameraPosition) {
        state.yaw = 0F;
    }
}
