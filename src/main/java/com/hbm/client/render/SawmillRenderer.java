package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntitySawmill;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/** Sawmill: the frame, the blade it only has when one is fitted, and the two counter-turning gears. */
public class SawmillRenderer extends GemBER<TileEntitySawmill, SawmillRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/sawmill.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/sawmill.png");

    @Override
    protected void setup(Gem.Model<SawmillRenderState> declaration) {
        Gem.Part<SawmillRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        machine.child(model.part("Main"), surface);
        machine.child(model.part("Blade"), surface)
                .visibleIf(state -> state.hasBlade)
                .spin(Gem.Axes.Z, 0F, 1.375F, 0F, state -> -state.rotation * 2F);
        machine.child(model.part("GearLeft"), surface)
                .spin(Gem.Axes.Z, 0.5625F, 1.375F, 0F, state -> state.rotation);
        machine.child(model.part("GearRight"), surface)
                .spin(Gem.Axes.Z, -0.5625F, 1.375F, 0F, state -> -state.rotation);
    }

    @Override
    public SawmillRenderState createRenderState() {
        return new SawmillRenderState();
    }

    @Override
    public void extract(TileEntitySawmill blockEntity, SawmillRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        int meta = MachineCore.facingOrdinal(blockEntity.getBlockState(), 2);
        state.yaw = switch (meta) {
            case 5 -> 90F;
            case 2 -> 180F;
            case 4 -> 270F;
            default -> 0F;
        };
        state.rotation = blockEntity.lastSpin + (blockEntity.spin - blockEntity.lastSpin) * partialTicks;
        state.hasBlade = blockEntity.hasBlade;
    }
}
