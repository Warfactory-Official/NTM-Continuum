package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.entity.projectile.EntityArtilleryShell;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemEntityRenderer;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** The shell in flight: the grenade mesh stretched fivefold along its axis and laid on the velocity. */
public class RenderArtilleryShell
        extends GemEntityRenderer<EntityArtilleryShell, RenderArtilleryShell.State> {

    private static final WavefrontObj PROJECTILES =
            WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/projectiles/projectiles.obj"));
    private static final Surface SURFACE =
            Surface.cutout(Constants.MOD_ID + ":textures/models/projectiles/grenade.png");

    public RenderArtilleryShell(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.part(PROJECTILES.part("Grenade"), SURFACE)
                .angle(Gem.Axes.Y, state -> state.yaw - 90F)
                .angle(Gem.Axes.Z, state -> state.pitch - 90F)
                .scale(2.5F, 5F, 2.5F);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityArtilleryShell shell, State state, float partialTicks) {
        state.yaw = net.minecraft.util.Mth.rotLerp(partialTicks, shell.yRotO, shell.getYRot());
        state.pitch = net.minecraft.util.Mth.lerp(partialTicks, shell.xRotO, shell.getXRot());
    }

    public static final class State extends CompatEntityRenderState {
        public float yaw;
        public float pitch;
    }
}
