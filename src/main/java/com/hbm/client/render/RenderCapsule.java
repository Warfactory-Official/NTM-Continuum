package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.SoyuzCapsule;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.storage.TileEntitySoyuzCapsule;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderCapsule}: the lander's capsule, landed askew - {@code T(0.5, 0, 0.5) T(0, -0.25, 0)
 * Ry(-25) Rz(15)} - in the rusted skin when the block is rusted (1.7 metadata 3), smooth-shaded and culled.
 */
public final class RenderCapsule extends GemBER<TileEntitySoyuzCapsule, RenderCapsule.State> {

    public static final class State extends CompatRenderState {
        public boolean rusted;
    }

    private final WavefrontObj model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/soyuz_lander.obj"));
    private final Surface clean = Surface.cutoutCull(Constants.MOD_ID + ":textures/models/soyuz_capsule/soyuz_lander.png");
    private final Surface rust = Surface.cutoutCull(Constants.MOD_ID + ":textures/models/soyuz_capsule/soyuz_lander_rust.png");

    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.offScreen().viewDistance(256).joint()
                .translate(0.5F, 0F, 0.5F)
                .translate(0F, -0.25F, 0F)
                .rotate(Gem.Axes.Y, -25F)
                .rotate(Gem.Axes.Z, 15F)
                .child(model.part("Capsule"), clean)
                .skin(state -> state.rusted ? rust : clean);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntitySoyuzCapsule capsule, State state, float partialTicks, Vec3 cameraPosition) {
        state.rusted = capsule.getBlockState().hasProperty(SoyuzCapsule.RUSTED)
                && capsule.getBlockState().getValue(SoyuzCapsule.RUSTED);
    }
}
