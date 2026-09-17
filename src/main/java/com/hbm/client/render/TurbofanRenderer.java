package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineTurbofan;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Turbofan declaration: spinning blades, and an exhaust cone that lights up on the afterburner. */
public final class TurbofanRenderer extends GemBER<TileEntityMachineTurbofan, TurbofanRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/turbofan.obj"));
    private final Surface body = Surface.cutoutNoCull(
            Constants.MOD_ID + ":textures/models/machines/turbofan.png");
    private final Surface back = Surface.cutoutNoCull(
            Constants.MOD_ID + ":textures/models/machines/turbofan_back.png");
    private final Surface afterburner = Surface.cutoutNoCull(
            Constants.MOD_ID + ":textures/models/machines/turbofan_afterburner.png");

    @Override
    protected void setup(Gem.Model<TurbofanRenderState> declaration) {
        Gem.Part<TurbofanRenderState> engine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        engine.child(model.part("Blades"), body)
                .spin(Gem.Axes.Z, 0F, 1.5F, 0F, state -> -state.spin);
        engine.child(model.part("Body"), body);
        engine.child(model.part("Afterburner"), back)
                .skin(state -> state.afterburner ? afterburner : back);
    }

    @Override
    public TurbofanRenderState createRenderState() {
        return new TurbofanRenderState();
    }

    @Override
    public void extract(TileEntityMachineTurbofan be, TurbofanRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = MachineCore.facingOrdinal(blockState, 2);
        state.yaw = 90F + switch (ForgeDirection.getOrientation(meta)) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
        state.spin = be.lastSpin + (be.spin - be.lastSpin) * partialTicks;
        state.afterburner = be.afterburner > 0;
    }
}
