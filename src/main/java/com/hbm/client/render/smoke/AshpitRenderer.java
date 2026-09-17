package com.hbm.client.render.smoke;

import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityAshpit;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/** The ashpit: the oven shell, its sliding door, and the interior that fills as ash collects. */
public final class AshpitRenderer extends GemBER<TileEntityAshpit, AshpitRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of("hbm", "models/machines/heating_oven.obj"));
    private final Surface surface = Surface.cutout("hbm:textures/models/machines/ashpit.png");

    @Override
    protected void setup(Gem.Model<AshpitRenderState> declaration) {
        Gem.Part<AshpitRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        machine.child(model.part("Main"), surface);
        machine.child(model.part("Door"), surface)
                .slide(Gem.Axes.Z, state -> state.doorAngle * 0.75F / 135F);
        machine.child(model.part("InnerBurning"), surface)
                .visibleIf(state -> state.full);
        machine.child(model.part("Inner"), surface)
                .visibleIf(state -> !state.full);
    }

    @Override
    public AshpitRenderState createRenderState() {
        return new AshpitRenderState();
    }

    /** The old renderer applied its facing switch and then a final -90 degree rotation. */
    @Override
    public void extract(TileEntityAshpit ashpit, AshpitRenderState state, float partialTicks, Vec3 cameraPosition) {
        int meta = MachineCore.facingOrdinal(ashpit.getBlockState(), 2);
        state.yaw = -90F + switch (meta) {
            case 5 -> 90F;
            case 2 -> 180F;
            case 4 -> 270F;
            default -> 0F;
        };
        state.doorAngle = ashpit.prevDoorAngle
                + (ashpit.doorAngle - ashpit.prevDoorAngle) * partialTicks;
        state.full = ashpit.isFull;
    }
}
