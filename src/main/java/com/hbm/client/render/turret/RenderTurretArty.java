package com.hbm.client.render.turret;

import com.hbm.tileentity.turret.TileEntityTurretArty;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * The 16-inch gun. The barrel slides out of the cannon on recoil and creeps back over the next twenty
 * ticks, so it reads as the recoil rather than as the whole gun jumping.
 */
public class RenderTurretArty extends RenderTurretBase<TileEntityTurretArty> {

    public static final TurretState.Emitter<TileEntityTurretArty> EMITTER = (turret, state, frame) -> {
        frame.push().obj(TurretResources.ARTY, "Base").texture(TurretResources.ARTY_BODY);

        TurretFrame.Part carriage = frame.push().obj(TurretResources.ARTY, "Carriage")
                                          .texture(TurretResources.ARTY_BODY);
        carriage.pose.rotateY((state.yaw - 180F) * Mth.DEG_TO_RAD);

        TurretFrame.Part cannon = frame.push().obj(TurretResources.ARTY, "Cannon")
                                       .texture(TurretResources.ARTY_BODY);
        cannon.pose.set(carriage.pose)
                   .translate(0F, 3F, 0F)
                   .rotateX(state.pitch * Mth.DEG_TO_RAD)
                   .translate(0F, -3F, 0F);

        float recoil = (float) (Mth.lerp(state.partialTicks, turret.lastBarrelPos, turret.barrelPos) * 2.5D);
        Matrix4f mount = cannon.pose;
        frame.push().obj(TurretResources.ARTY, "Barrel").texture(TurretResources.ARTY_BODY)
             .pose.set(mount).translate(0F, 0F, recoil);
    };

    @Override
    protected TurretState.Emitter<TileEntityTurretArty> emitter() {
        return EMITTER;
    }
}
