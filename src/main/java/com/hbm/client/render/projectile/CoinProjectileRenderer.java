package com.hbm.client.render.projectile;

import com.hbm.client.model.WavefrontObj;
import com.hbm.entity.projectile.EntityCoin;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemEntityRenderer;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** GEM port of 1.7's RenderCoin, retaining the authored chip mesh. */
public final class CoinProjectileRenderer extends GemEntityRenderer<EntityCoin, CoinProjectileRenderer.State> {
    private static final String MODEL_PATH = "models/trinkets/chip.obj";
    private static final String TEXTURE_PATH = "textures/models/trinkets/chip_gold.png";
    private static final Surface COIN = Surface.cutout(Ids.of("hbm", TEXTURE_PATH).toString());

    private final WavefrontObj model = WavefrontObj.load(Ids.of("hbm", MODEL_PATH));

    public CoinProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.part(model.all(), COIN)
                // GL11's (0, -1, 0) axis is the negative-Y equivalent of this GEM rotation.
                .angle(Gem.Axes.Y, state -> 90F - state.yaw)
                .angle(Gem.Axes.Z, state -> state.roll)
                .scale(0.125F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityCoin entity, State state, float partialTicks) {
        // Preserve RenderCoin's tracker interpolation instead of using the current frame's yaw only.
        state.yaw = entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTicks;
        state.roll = (entity.tickCount + partialTicks) * 45F;
    }

    public static final class State extends CompatEntityRenderState {
        public float yaw;
        public float roll;
    }
}
