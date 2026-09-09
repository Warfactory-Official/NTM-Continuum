package com.hbm.client.render.turret;

import com.hbm.tileentity.turret.TileEntityTurretRichard;
import org.joml.Matrix4f;

/**
 * The pad mount carrying a missile rack: one mesh per loaded round, walked down the tube bank. Four
 * indices wrap, and those are where the next row starts.
 */
public class RenderTurretRichard extends RenderTurretBase<TileEntityTurretRichard> {

    public static final TurretState.Emitter<TileEntityTurretRichard> EMITTER = (turret, state, frame) -> {
        connectors(frame, state.connectorMask);
        Matrix4f launcher = mount(frame, state, TurretResources.BASE,
                TurretResources.CHEKHOV, "Carriage", TurretResources.CARRIAGE, 1.5F,
                TurretResources.RICHARD, "Launcher", TurretResources.RICHARD_BODY);

        TurretFrame.Part previous = null;
        for (int i = 0; i < turret.loaded; i++) {
            TurretFrame.Part missile = frame.push().obj(TurretResources.RICHARD, "MissileLoaded")
                                            .texture(TurretResources.RICHARD_BODY);
            if (previous == null) {
                missile.pose.set(launcher).translate(0F, .375F, .1875F);
            } else {
                int last = i - 1;
                missile.pose.set(previous.pose);
                if (last == 2 || last == 6 || last == 9 || last == 13) {
                    missile.pose.translate(0F, -.1875F, .1875F * 2F + .09375F);
                } else {
                    missile.pose.translate(0F, 0F, -.1875F);
                }
            }
            previous = missile;
        }
    };

    @Override
    protected TurretState.Emitter<TileEntityTurretRichard> emitter() {
        return EMITTER;
    }
}
