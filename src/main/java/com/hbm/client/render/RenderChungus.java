package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.machine.TileEntityChungus;
import com.hbm.tileentity.MachineCore;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderChungus}: translate to the block centre, {@code glRotatef(90, Y)} plus the facing turn,
 * {@code glTranslated(0, 0, -3)}; the body, the lever tilted about X at z=4.5 by the steam grade, and the
 * blades spun about −Z at y=2.5.
 */
public final class RenderChungus extends GemBER<TileEntityChungus, ChungusRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/chungus.obj"));
    private final Surface surface = Surface.cutoutCull(
            Constants.MOD_ID + ":textures/models/machines/chungus.png");

    @Override
    protected void setup(Gem.Model<ChungusRenderState> declaration) {
        Gem.Part<ChungusRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .rotate(Gem.Axes.Y, 90F)
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
        int meta = MachineCore.facingOrdinal(be.getBlockState(), 2);
        state.yaw = switch (meta) {
            case 2 -> 90F;
            case 4 -> 180F;
            case 3 -> 270F;
            default -> 0F;
        };
        // 1.7: FluidType.ordinal() is its id, and STEAM is id 2.
        state.lever = 15F - (Fluids.legacyId(be.tanks[0].getTankType()) - Fluids.legacyId(Fluids.STEAM)) * 10F;
        state.rotor = be.getRotor(partialTicks);
    }
}
