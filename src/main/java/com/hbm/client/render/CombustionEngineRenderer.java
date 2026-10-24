package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.FluidContainers;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineCombustionEngine;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Animated legacy combustion-engine renderer with the exact model offset, hatch pivot and canister tint. */
public final class CombustionEngineRenderer
        extends GemBER<TileEntityMachineCombustionEngine, CombustionEngineRenderState> {

    private final WavefrontObj model = WavefrontObj.load(Ids.of(Constants.MOD_ID,
            "models/machines/combustion_engine.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/combustion_engine.png");

    /**
     * The canister is tinted by the fuel it holds, and white where the fuel has no legacy tint, which
     * is the same {@code 0xFFFFFFFF} the untinted path means, so it is one part either way.
     */
    @Override
    protected void setup(Gem.Model<CombustionEngineRenderState> declaration) {
        Gem.Part<CombustionEngineRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .translate(-0.5F, 0F, 3F);

        machine.child(model.part("Engine"), surface);
        machine.child(model.part("Canister"), surface)
                .tint(state -> state.canisterColor);
        machine.child(model.part("Hatch"), surface)
                .spin(Gem.Axes.Y, 1F, 0F, -2.6875F, state -> -state.doorAngle);
    }

    @Override
    public CombustionEngineRenderState createRenderState() {
        return new CombustionEngineRenderState();
    }

    @Override
    public void extract(TileEntityMachineCombustionEngine be, CombustionEngineRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = MachineCore.facingOrdinal(bs, 2);
        state.yaw = 90F + switch (meta) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
        state.doorAngle = be.prevDoorAngle + (be.doorAngle - be.prevDoorAngle) * partialTicks;

        FluidType tankType = be.tank.getTankType();
        state.canisterColor = FluidContainers.clientHasCanister(tankType)
                ? legacyCanisterTint(FluidContainers.clientCanisterColor(tankType)) : 0xFFFFFFFF;
    }

    // The legacy renderer tints with CD_Canister.color (the hbm:fluid_containers data map), not FluidType.color.

    /** Convert the old {@code channel / 256F} tint to the closest 8-bit vertex colour. */
    private static int legacyCanisterTint(int rgb) {
        int r = Math.round(((rgb >> 16) & 0xFF) / 256F * 255F);
        int g = Math.round(((rgb >> 8) & 0xFF) / 256F * 255F);
        int b = Math.round((rgb & 0xFF) / 256F * 255F);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
