package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntitySolarBoiler;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/** The solar boiler tower stands still; only its facing is read. */
public final class SolarBoilerRenderer extends GemBER<TileEntitySolarBoiler, SolarBoilerRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/solar_boiler.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/solar_boiler.png");

    @Override
    protected void setup(Gem.Model<SolarBoilerRenderState> declaration) {
        declaration.offScreen().viewDistance(256)
                .part(model.parts(Set.of("Base"), true), surface)
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> 90F + state.yaw);
    }

    @Override
    public SolarBoilerRenderState createRenderState() {
        return new SolarBoilerRenderState();
    }

    @Override
    public void extract(TileEntitySolarBoiler blockEntity, SolarBoilerRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState blockState = blockEntity.getBlockState();
        int meta = MachineCore.facingOrdinal(blockState, 2);
        state.yaw = switch (meta) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
    }
}
