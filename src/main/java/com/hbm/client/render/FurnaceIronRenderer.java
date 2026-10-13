package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityFurnaceIron;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/** Iron furnace: one body whose front swaps for the lit variant while it burns. */
public final class FurnaceIronRenderer extends GemBER<TileEntityFurnaceIron, FurnaceRenderState> {

    private static final int FULLBRIGHT = 0x00F000F0;

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/furnace_iron.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/furnace_iron.png");

    /** The legacy renderer's model origin is half a block beyond the core cell. */
    @Override
    protected void setup(Gem.Model<FurnaceRenderState> declaration) {
        Gem.Part<FurnaceRenderState> furnace = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .translate(-0.5F, 0F, -0.5F);

        furnace.child(model.part("Main"), surface);
        furnace.child(model.part("On"), surface)
                .visibleIf(state -> state.burning)
                .light(state -> FULLBRIGHT);
        furnace.child(model.part("Off"), surface)
                .visibleIf(state -> !state.burning);
    }

    @Override
    public FurnaceRenderState createRenderState() {
        return new FurnaceRenderState();
    }

    @Override
    public void extract(TileEntityFurnaceIron furnace, FurnaceRenderState state, float partialTicks, Vec3 cameraPosition) {
        int facing = MachineCore.facingOrdinal(furnace.getBlockState(), 2);
        state.yaw = switch (facing) {
            case 5 -> 90F;
            case 2 -> 180F;
            case 4 -> 270F;
            default -> 0F;
        };
        state.burning = furnace.wasOn;
    }
}
