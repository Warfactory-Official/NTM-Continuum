package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityChungus;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Animated renderer for the Leviathan turbine, matching the old three-part TESR. */
public final class ChungusRenderer extends GemBER<TileEntityChungus, ChungusRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/chungus.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/chungus.png");

    @Override
    protected void setup(Gem.Model<ChungusRenderState> declaration) {
        Gem.Part<ChungusRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .translate(0F, 0F, -3F);

        machine.child(model.part("Body"), surface);
        machine.child(model.part("Lever"), surface)
                .spin(Gem.Axes.X, 0F, 0F, 4.5F, state -> state.lever);
        machine.child(model.part("Blades"), surface)
                .spin(Gem.Axes.Z, 0F, 2.5F, 0F, state -> -state.rotor);
    }

    @Override
    public ChungusRenderState createRenderState() {
        return new ChungusRenderState();
    }

    @Override
    public void extract(TileEntityChungus be, ChungusRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        state.yaw = 180F + switch (MachineCore.facing(blockState)) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
        state.lever = 15F - grade(be.tanks[0].getTankType()) * 10F;
        state.rotor = be.lastRotor + (be.rotor - be.lastRotor) * partialTicks;
    }

    /** Which notch of the compressor lever this input steam corresponds to, 0 (steam) to 3 (ultra dense). */
    private static int grade(FluidType type) {
        if (type == Fluids.HOTSTEAM) return 1;
        if (type == Fluids.SUPERHOTSTEAM) return 2;
        if (type == Fluids.ULTRAHOTSTEAM) return 3;
        return 0;
    }
}
