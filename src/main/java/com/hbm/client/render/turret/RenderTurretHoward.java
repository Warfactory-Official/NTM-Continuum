package com.hbm.client.render.turret;

import com.hbm.tileentity.turret.TileEntityTurretHoward;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** The CIWS mount, whose two barrel banks counter-rotate about separate pivots. */
public class RenderTurretHoward extends RenderTurretBase<TileEntityTurretHoward> {

    public static final TurretState.Emitter<TileEntityTurretHoward> EMITTER = (turret, state, frame) -> {
        connectors(frame, state.connectorMask);
        Matrix4f body = mount(frame, state, TurretResources.BASE,
                TurretResources.HOWARD, "Carriage", TurretResources.CARRIAGE_CIWS, 2.25F,
                TurretResources.HOWARD, "Body", TurretResources.HOWARD_BODY);

        float spin = Mth.lerp(state.partialTicks, turret.lastSpin, turret.spin);

        TurretFrame.Part top = frame.push().obj(TurretResources.HOWARD, "BarrelsTop")
                                    .texture(TurretResources.HOWARD_BARRELS);
        top.pose.set(body)
                .translate(0F, 2.5F, 0F)
                .rotateX(-spin * Mth.DEG_TO_RAD)
                .translate(0F, -2.5F, 0F);

        TurretFrame.Part bottom = frame.push().obj(TurretResources.HOWARD, "BarrelsBottom")
                                       .texture(TurretResources.HOWARD_BARRELS);
        bottom.pose.set(body)
                   .translate(0F, 2F, 0F)
                   .rotateX(spin * Mth.DEG_TO_RAD)
                   .translate(0F, -2F, 0F);
    };

    @Override
    protected TurretState.Emitter<TileEntityTurretHoward> emitter() {
        return EMITTER;
    }
}
