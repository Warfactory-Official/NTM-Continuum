package com.hbm.client.render.turret;

import com.hbm.tileentity.turret.TileEntityTurretHowardDamaged;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** The same mount as the Goalkeeper, in rust, and with no pad plugs left to draw. */
public class RenderTurretHowardDamaged extends RenderTurretBase<TileEntityTurretHowardDamaged> {

    public static final TurretState.Emitter<TileEntityTurretHowardDamaged> EMITTER = (turret, state, frame) -> {
        frame.push().obj(TurretResources.CHEKHOV, "Base").texture(TurretResources.BASE_RUSTED);

        TurretFrame.Part mount = frame.push().obj(TurretResources.HOWARD_DAMAGED, "Carriage")
                                      .texture(TurretResources.CARRIAGE_CIWS_RUSTED);
        mount.pose.rotateY((state.yaw - 90F) * Mth.DEG_TO_RAD);

        TurretFrame.Part body = frame.push().obj(TurretResources.HOWARD_DAMAGED, "Body")
                                     .texture(TurretResources.HOWARD_RUSTED);
        body.pose.set(mount.pose)
                 .translate(0F, 2.25F, 0F)
                 .rotateZ(state.pitch * Mth.DEG_TO_RAD)
                 .translate(0F, -2.25F, 0F);

        float spin = Mth.lerp(state.partialTicks, turret.lastSpin, turret.spin);
        Matrix4f pose = body.pose;

        TurretFrame.Part top = frame.push().obj(TurretResources.HOWARD_DAMAGED, "BarrelsTop")
                                    .texture(TurretResources.HOWARD_BARRELS_RUSTED);
        top.pose.set(pose)
                .translate(0F, 2.5F, 0F)
                .rotateX(-spin * Mth.DEG_TO_RAD)
                .translate(0F, -2.5F, 0F);

        TurretFrame.Part bottom = frame.push().obj(TurretResources.HOWARD_DAMAGED, "BarrelsBottom")
                                       .texture(TurretResources.HOWARD_BARRELS_RUSTED);
        bottom.pose.set(pose)
                   .translate(0F, 2F, 0F)
                   .rotateX(spin * Mth.DEG_TO_RAD)
                   .translate(0F, -2F, 0F);
    };

    @Override
    protected TurretState.Emitter<TileEntityTurretHowardDamaged> emitter() {
        return EMITTER;
    }
}
