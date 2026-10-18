package com.hbm.inventory.fluid.trait;

import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.inventory.fluid.FluidType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Map;

/**
 * 1.7 {@code FluidTrait.onRelease}: {@code mB} of a fluid leaving a machine, handed to every trait that reacts to a
 * release. Of 1.7's traits only {@link FT_Polluting} and {@link FT_VentRadiation} do; the port's traits have no common
 * base, so the two are asked here directly.
 */
public final class FluidTrait {

    private FluidTrait() {}

    public static void onRelease(Level world, BlockPos pos, FluidType type, FluidReleaseType release, int mB) {
        FT_Polluting polluting = type.getTrait(FT_Polluting.class);
        if (polluting != null && release != FluidReleaseType.VOID) {
            Map<PollutionType, Float> map = release == FluidReleaseType.BURN ? polluting.burnMap : polluting.releaseMap;
            for (Map.Entry<PollutionType, Float> entry : map.entrySet()) {
                PollutionHandler.incrementPollution(world, pos, entry.getKey(), entry.getValue() * mB);
            }
        }
        FT_VentRadiation radiation = type.getTrait(FT_VentRadiation.class);
        if (radiation != null) ChunkRadiationManager.proxy.incrementRad(world, pos, mB * radiation.getRadPerMB());
    }
}
