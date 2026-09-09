package com.hbm.client.render.turret;

import com.hbm.items.weapon.ItemAmmoHIMARS;
import com.hbm.items.weapon.ItemAmmoHIMARS.HIMARSRocketType;
import com.hbm.tileentity.turret.TileEntityTurretHIMARS;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * The rocket artillery. Its launcher pitches about X rather than Z — the pod is modelled lying along
 * the gun's own axis — and the crane slides the pod five blocks back over the reload, which is what
 * makes the animation read as loading rather than as the launcher jumping.
 *
 * <p>The caps are the rockets still in the tubes: one mesh per round left, drawn in the crane's frame.
 */
public class RenderTurretHIMARS extends RenderTurretBase<TileEntityTurretHIMARS> {

    private static final String[] CAPS_STANDARD = {"CapStandard6", "CapStandard5", "CapStandard4",
            "CapStandard3", "CapStandard2", "CapStandard1"};

    public static final TurretState.Emitter<TileEntityTurretHIMARS> EMITTER = (turret, state, frame) -> {
        frame.push().obj(TurretResources.ARTY, "Base").texture(TurretResources.ARTY_BODY);

        TurretFrame.Part carriage = frame.push().obj(TurretResources.HIMARS, "Carriage")
                                          .texture(TurretResources.HIMARS_BODY);
        carriage.pose.rotateY((state.yaw - 180F) * Mth.DEG_TO_RAD);

        TurretFrame.Part launcher = frame.push().obj(TurretResources.HIMARS, "Launcher")
                                         .texture(TurretResources.HIMARS_BODY);
        launcher.pose.set(carriage.pose)
                     .translate(0F, 2.25F, 2F)
                     .rotateX(state.pitch * Mth.DEG_TO_RAD)
                     .translate(0F, -2.25F, -2F);

        float travel = Mth.lerp(state.partialTicks, turret.lastCrane, turret.crane) * -5F;
        TurretFrame.Part crane = frame.push().obj(TurretResources.HIMARS, "Crane")
                                      .texture(TurretResources.HIMARS_BODY);
        crane.pose.set(launcher.pose).translate(0F, 0F, travel);

        if (turret.typeLoaded < 0) return;
        HIMARSRocketType type = ItemAmmoHIMARS.byIndex(turret.typeLoaded);
        Matrix4f pod = crane.pose;

        if (type.modelType == 0) {
            frame.push().obj(TurretResources.HIMARS, "TubeStandard").texture(type.texture).pose.set(pod);
            for (int i = 0; i < turret.ammo && i < CAPS_STANDARD.length; i++) {
                frame.push().obj(TurretResources.HIMARS, CAPS_STANDARD[i]).texture(type.texture).pose.set(pod);
            }
        } else {
            frame.push().obj(TurretResources.HIMARS, "TubeSingle").texture(type.texture).pose.set(pod);
            if (turret.hasAmmo()) {
                frame.push().obj(TurretResources.HIMARS, "CapSingle").texture(type.texture).pose.set(pod);
            }
        }
    };

    @Override
    protected TurretState.Emitter<TileEntityTurretHIMARS> emitter() {
        return EMITTER;
    }
}
