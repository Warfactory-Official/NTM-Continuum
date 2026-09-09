package com.hbm.client.render.turret;

import com.hbm.tileentity.turret.TileEntityTurretSentry;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class RenderTurretSentry<T extends TileEntityTurretSentry> extends RenderTurretBase<T> {

    private final Identifier texture;

    public RenderTurretSentry() {
        this(TurretResources.SENTRY);
    }

    protected RenderTurretSentry(Identifier texture) {
        this.texture = texture;
    }

    @Override
    protected TurretState.Emitter<T> emitter() {
        Identifier skin = texture;
        return (turret, state, frame) -> {
            frame.push().obj(TurretResources.SENTRY_MODEL, "Base").texture(skin);

            TurretFrame.Part pivot = frame.push().obj(TurretResources.SENTRY_MODEL, "Pivot").texture(skin);
            pivot.pose.rotateY(state.yaw * Mth.DEG_TO_RAD);

            TurretFrame.Part body = frame.push().obj(TurretResources.SENTRY_MODEL, "Body").texture(skin);
            body.pose.set(pivot.pose)
                     .translate(0F, 1.25F, 0F)
                     .rotateX(-state.pitch * Mth.DEG_TO_RAD)
                     .translate(0F, -1.25F, 0F);

            frame.push().obj(TurretResources.SENTRY_MODEL, "Drum").texture(skin).pose.set(body.pose);

            double left = Mth.lerp(state.partialTicks, turret.lastBarrelLeftPos, turret.barrelLeftPos) * -0.5D;
            frame.push().obj(TurretResources.SENTRY_MODEL, "BarrelL").texture(skin)
                 .pose.set(body.pose).translate(0F, 0F, (float) left);

            TurretFrame.Part right = frame.push().obj(TurretResources.SENTRY_MODEL, "BarrelR").texture(skin);
            poseRightBarrel(turret, state, right, body.pose);
        };
    }

    protected void poseRightBarrel(T turret, TurretState<T> state, TurretFrame.Part barrel, Matrix4f body) {
        double right = Mth.lerp(state.partialTicks, turret.lastBarrelRightPos, turret.barrelRightPos) * -0.5D;
        barrel.pose.set(body).translate(0F, 0F, (float) right);
    }
}
