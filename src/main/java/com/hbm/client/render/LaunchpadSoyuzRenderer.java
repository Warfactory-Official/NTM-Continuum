package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.client.render.entity.SoyuzMesh;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityLaunchpadSoyuz;
import com.hbm.tileentity.machine.TileEntityLaunchpadSoyuz.SoyuzStatus;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderLaunchpadSoyuz}: the pad, five struts sliding in, and the carriage that rolls the rocket in on
 * sixteen wheels, tips at the buffer stops and stands it up on its rotor and mount. The rocket rides the mount until
 * the countdown, then stands free on the pad.
 */
public final class LaunchpadSoyuzRenderer extends GemBER<TileEntityLaunchpadSoyuz, LaunchpadSoyuzRenderer.State> {

    private static final WavefrontObj MODEL =
            WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/machines/launchpad_soyuz.obj"));
    private static final Surface SKIN = Surface.cutoutCull(Constants.MOD_ID + ":textures/models/machines/launchpad_soyuz.png");

    private static final double[] WHEELS_FORWARD = {17D, 19D, 29D, 31D};
    private static final double[] WHEELS_SIDE = {6.75D, 5.25D, -5.25D, -6.75D};

    @Override
    protected void setup(Gem.Model<State> declaration) {
        Gem.Part<State> pad = declaration.offScreen().viewDistance(256).joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.rotation)
                .translate(-4F, 0F, -4F);

        SoyuzMesh.declare(pad.joint().translate(0F, 4F, 0F).angle(Gem.Axes.Y, state -> -state.rotation)
                .visibleIf(state -> state.renderSoyuz && state.lockSoyuz), state -> state.skin);

        pad.child(MODEL.part("Launchpad"), SKIN);
        for (int i = 1; i <= 5; i++) {
            int strut = i - 1;
            pad.child(MODEL.part("Strut" + i), SKIN).slide(Gem.Axes.Z, state -> state.struts[strut]);
        }

        Gem.Part<State> carriage = pad.joint()
                .slide(Gem.Axes.Z, state -> -state.carriage)
                .spin(Gem.Axes.X, 0F, 1.5F, -32F, state -> -state.tilt);
        carriage.child(MODEL.part("Carriage"), SKIN);

        for (int i = 1; i <= 4; i++) for (int j = 1; j <= 4; j++) {
            float forward = (float) WHEELS_FORWARD[i - 1];
            float side = (float) WHEELS_SIDE[j - 1];
            float sign = j % 2 == 0 ? -1F : 1F;
            carriage.child(MODEL.part("Wheel_" + i + "_" + j), SKIN)
                    .spin(Gem.Axes.Y, side, 0F, -forward, state -> state.wheels * sign);
        }

        Gem.Part<State> rotor = carriage.joint().spin(Gem.Axes.X, 0F, 24.5F, -18F, state -> -state.rotor);
        rotor.child(MODEL.part("Rotor"), SKIN);

        Gem.Part<State> mount = rotor.joint().spin(Gem.Axes.X, 0F, 24.5F, -6F, state -> state.rotor);
        mount.child(MODEL.part("Mount"), SKIN);

        SoyuzMesh.declare(mount.joint().translate(0F, 4F, 0F).angle(Gem.Axes.Y, state -> -state.rotation)
                .visibleIf(state -> state.renderSoyuz && !state.lockSoyuz), state -> state.skin);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityLaunchpadSoyuz pad, State state, float interp, Vec3 camera) {
        state.rotation = switch (MachineCore.facingOrdinal(pad.getBlockState(), 5)) {
            case 2 -> 90F;
            case 4 -> 180F;
            case 3 -> 270F;
            default -> 0F;
        };

        state.rotor = Mth.clamp(pad.getInterpPos(TileEntityLaunchpadSoyuz.INDEX_ROTOR, interp) * -180F + 180F, 0F, 180F);
        state.carriage = Mth.clamp(pad.getInterpPos(TileEntityLaunchpadSoyuz.INDEX_CARRIAGE, interp) * -19.5F + 19.5F, 0F, 19.5F);
        state.wheels = (float) (state.carriage * 360D / Math.PI);
        state.tilt = pad.getInterpPos(TileEntityLaunchpadSoyuz.INDEX_TILT, interp);

        for (int i = 0; i < 5; i++) {
            float ext = i == 4 ? 3F : 4.5F;
            state.struts[i] = Mth.clamp(pad.getInterpPos(i, interp) * -ext + ext, 0F, ext);
        }

        state.renderSoyuz = pad.loadedType >= 0 && pad.soyuzStatus != SoyuzStatus.ABSENT && SoyuzMesh.hasSkin(pad.loadedType);
        state.lockSoyuz = pad.soyuzStatus == SoyuzStatus.LAUNCHING;
        state.skin = Math.max(pad.loadedType, 0);
    }

    public static final class State extends CompatRenderState {
        float rotation;
        float rotor;
        float carriage;
        float wheels;
        float tilt;
        final float[] struts = new float[5];
        boolean renderSoyuz;
        boolean lockSoyuz;
        int skin;
    }
}
