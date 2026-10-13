package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineCrystallizer;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Ore acidizer: static body, spinning head, and the acid overlay tinted by the tank while processing. */
public class CrystallizerRenderer extends GemBER<TileEntityMachineCrystallizer, CrystallizerRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/crystallizer.obj"));
    private final Surface body = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/crystallizer.png");
    private final Surface acid = Surface.translucent(
            Constants.MOD_ID + ":textures/models/machines/chemical_plant_fluid.png");

    @Override
    protected void setup(Gem.Model<CrystallizerRenderState> declaration) {
        Gem.Part<CrystallizerRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        machine.child(model.part("Body"), body);
        machine.child(model.part("Spinner"), body)
                .angle(Gem.Axes.Y, state -> state.angle);
        machine.child(model.part("Fluid"), acid)
                .visibleIf(state -> state.processing)
                .tint(state -> 0xB0000000 | (state.fluidColor & 0xFFFFFF));
    }

    @Override
    public CrystallizerRenderState createRenderState() {
        return new CrystallizerRenderState();
    }

    /** The 1.12.2 RenderCrystallizer facing map, which differs from the chem plant's, with no base yaw. */
    @Override
    public void extract(TileEntityMachineCrystallizer be, CrystallizerRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = MachineCore.facingOrdinal(bs, 2);
        state.yaw = switch (meta) {
            case 2 -> 90F;
            case 4 -> 180F;
            case 3 -> 270F;
            default -> 0F;
        };
        state.angle = be.prevAngle + (be.angle - be.prevAngle) * partialTicks;
        state.processing = be.isOn;
        state.fluidColor = be.tank.getTankType().getColor();
    }
}
