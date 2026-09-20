package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineIGenerator;
import com.hbm.tileentity.MachineCore;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderIGenerator}: the model at one sixth scale, shifted half a (scaled) block back, drawn
 * without culling; the rotor turns about −Z at y=1.5 by an angle the memorial never advances.
 */
public final class RenderIGenerator extends GemBER<TileEntityMachineIGenerator, IGeneratorRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/igen.obj"));
    private final Surface surface = Surface.cutoutNoCull(
            Constants.MOD_ID + ":textures/models/machines/igen.png");

    @Override
    protected void setup(Gem.Model<IGeneratorRenderState> declaration) {
        Gem.Part<IGeneratorRenderState> machine = declaration.viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .scale(1F / 6F)
                .translate(0F, 0F, -0.5F);

        machine.child(model.part("Body"), surface);
        machine.child(model.part("Rotor"), surface)
                .spin(Gem.Axes.Z, 0F, 1.5F, 0F, state -> -state.rotor);
    }

    @Override
    public IGeneratorRenderState createRenderState() {
        return new IGeneratorRenderState();
    }

    @Override
    public void extract(TileEntityMachineIGenerator be, IGeneratorRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        int meta = MachineCore.facingOrdinal(be.getBlockState(), 2);
        state.yaw = switch (meta) {
            case 2 -> 180F;
            case 4 -> 270F;
            case 5 -> 90F;
            default -> 0F;
        };
        state.rotor = be.prevRotation + (be.rotation - be.prevRotation) * partialTicks;
    }
}
