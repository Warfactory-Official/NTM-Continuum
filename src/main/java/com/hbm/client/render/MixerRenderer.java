package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.machine.TileEntityMachineMixer;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/**
 * Mixer: a static vessel, the paddle turning in it, and the contents scaled to the combined fill.
 *
 * <p>The legacy TESR disabled texturing for the fluid and supplied only its colour, which is a white
 * texture carrying a vertex tint here.
 */
public final class MixerRenderer extends GemBER<TileEntityMachineMixer, MixerRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/mixer.obj"));
    private final Surface body = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/mixer.png");
    private final Surface fluid = Surface.translucent(
            Constants.MOD_ID + ":textures/models/misc/white.png");

    @Override
    protected void setup(Gem.Model<MixerRenderState> declaration) {
        Gem.Part<MixerRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F);

        machine.child(model.part("Main"), body);
        machine.child(model.part("Mixer"), body)
                .angle(Gem.Axes.Y, state -> -state.rotation);
        machine.child(model.part("Fluid"), fluid)
                .visibleIf(state -> state.fillScale > 0F)
                .translate(0F, 1F, 0F)
                .stretch(Gem.Axes.Y, state -> state.fillScale)
                .translate(0F, -1F, 0F)
                .tint(state -> 0xBF000000 | (state.fluidColor & 0xFFFFFF));
    }

    @Override
    public MixerRenderState createRenderState() {
        return new MixerRenderState();
    }

    @Override
    public void extract(TileEntityMachineMixer be, MixerRenderState state, float partialTicks, Vec3 cameraPosition) {
        state.rotation = be.prevRotation + (be.rotation - be.prevRotation) * partialTicks;
        int fill = 0;
        int capacity = 0;
        for (var tank : be.tanks) {
            if (tank.getTankType() != Fluids.NONE) {
                fill += tank.getFill();
                capacity += tank.getMaxFill();
            }
        }
        state.fillScale = capacity <= 0 ? 0F : (float) fill / capacity * 0.99F;
        state.fluidColor = be.tanks[2].getTankType().getColor();
    }
}
