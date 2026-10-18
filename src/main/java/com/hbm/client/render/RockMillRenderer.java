package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineRockMill;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/** 1.7 {@code RenderRockMill}: the base, the stacking frame when something sits on top, and the wheel. */
public class RockMillRenderer extends GemBER<TileEntityMachineRockMill, RockMillRenderState> {

    private final WavefrontObj model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/machines/rockmill.obj"));
    private final Surface surface = Surface.cutout(Constants.MOD_ID + ":textures/models/machines/rockmill.png");

    @Override
    protected void setup(Gem.Model<RockMillRenderState> declaration) {
        Gem.Part<RockMillRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> 90F + state.yaw);

        machine.child(model.part("Base"), surface);
        machine.child(model.part("Frame"), surface).visibleIf(state -> state.frame);
        machine.child(model.part("Wheel"), surface).angle(Gem.Axes.Y, state -> -state.wheel);
    }

    @Override
    public RockMillRenderState createRenderState() {
        return new RockMillRenderState();
    }

    @Override
    public void extract(TileEntityMachineRockMill be, RockMillRenderState state, float partialTicks, Vec3 cameraPosition) {
        state.yaw = switch (MachineCore.facingOrdinal(be.getBlockState(), 2)) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
        state.frame = be.frame;
        state.wheel = be.prevRotation + (be.rotation - be.prevRotation) * partialTicks;
    }
}
