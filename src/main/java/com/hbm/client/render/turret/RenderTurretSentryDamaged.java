package com.hbm.client.render.turret;

import com.hbm.tileentity.turret.TileEntityTurretSentryDamaged;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class RenderTurretSentryDamaged extends RenderTurretSentry<TileEntityTurretSentryDamaged> {

    public RenderTurretSentryDamaged() {
        super(TurretResources.SENTRY_DAMAGED);
    }

    @Override
    protected void poseRightBarrel(TileEntityTurretSentryDamaged turret, TurretState<TileEntityTurretSentryDamaged> state,
                                   TurretFrame.Part barrel, Matrix4f body) {
        barrel.pose.set(body)
                .translate(0F, 1.5F, 0.5F)
                .rotateX(25F * Mth.DEG_TO_RAD)
                .translate(0F, -1.5F, -0.5F);
    }
}
