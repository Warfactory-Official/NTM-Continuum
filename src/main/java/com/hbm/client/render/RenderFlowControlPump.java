package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.network.TileEntityFlowControlPump;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/** 1.7 {@code RenderFlowControlPump}: the pump body, a rail above and below where a block sits, and the turning rotor. */
public class RenderFlowControlPump extends GemBER<TileEntityFlowControlPump, RenderFlowControlPump.State> {

    public static class State extends CompatRenderState {
        public float yaw;
        public float rotor;
        public boolean topRail;
        public boolean bottomRail;
    }

    private static final String TEX = Ids.of(Constants.MOD_ID, "textures/models/network/flow_control_pump.png").toString();
    private final WavefrontObj model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/network/flow_control_pump.obj"));

    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.offScreen().viewDistance(256);
        face(declaration.part(model.part("Pump"), Surface.cutout(TEX)));
        face(declaration.part(model.part("RailTop"), Surface.cutout(TEX))).visibleIf(s -> s.topRail);
        face(declaration.part(model.part("RailBottom"), Surface.cutout(TEX))).visibleIf(s -> s.bottomRail);
        face(declaration.part(model.part("Rotor"), Surface.cutout(TEX)))
                .translate(0F, 0.5F, 0F)
                .angle(Gem.Axes.Z, s -> -s.rotor)
                .translate(0F, -0.5F, 0F);
    }

    private static Gem.Part<State> face(Gem.Part<State> part) {
        return part.translate(0.5F, 0F, 0.5F).angle(Gem.Axes.Y, s -> s.yaw);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** 1.7 turns N=90, W=180, S=270, E=0. */
    @Override
    public void extract(TileEntityFlowControlPump pump, State state, float partialTicks, Vec3 cameraPosition) {
        state.yaw = switch (MachineCore.facingOrdinal(pump.getBlockState(), 2)) {
            case 4 -> 180F;
            case 3 -> 270F;
            case 5 -> 0F;
            default -> 90F;
        };
        state.rotor = pump.prevRotor + (pump.rotor - pump.prevRotor) * partialTicks;
        state.topRail = pump.topRail;
        state.bottomRail = pump.bottomRail;
    }
}
