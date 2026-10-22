package com.hbm.capability;

import com.hbm.Constants;
import com.hbm.api.fluidmk2.IFluidReceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import dev.golem.core.fluid.FluidCompat;
import dev.golem.core.fluid.FluidView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Another mod's tank as a receiver on an HBM duct net. port: 1.7 ducts reach only HBM machines; this is the fluid twin
 * of cables feeding FE/NeoForge/Team Reborn machines. Receive only, at pressure 0: a duct never pulls from a foreign tank.
 */
public final class ForeignFluidReceiver implements IFluidReceiverMK2 {

    /** Asked of a foreign handler as "how much would you take", the same cap HBM receivers default to. */
    private static final long PROBE = 1_000_000_000L;
    private static final FluidTankNTM[] NO_TANKS = new FluidTankNTM[0];

    private final FluidView view;

    private ForeignFluidReceiver(FluidView view) {
        this.view = view;
    }

    /** The foreign handler at {@code pos} on its {@code side}; null on HBM blocks, which the net reaches natively. */
    public static @Nullable FluidView viewAt(Level level, BlockPos pos, Direction side) {
        if (Constants.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getNamespace())) {
            return null;
        }
        return FluidCompat.find(level, pos, side);
    }

    /** Whether a duct should draw an arm to {@code pos}: outside a full level (worldgen, a chunk render cache) no. */
    public static boolean exposes(BlockGetter level, BlockPos pos, Direction side) {
        return level instanceof Level full && viewAt(full, pos, side) != null;
    }

    public static ForeignFluidReceiver of(FluidView view) {
        return new ForeignFluidReceiver(view);
    }

    @Override
    public long transferFluid(FluidType type, int pressure, long amount) {
        if (pressure != 0 || amount <= 0L) return amount;
        return amount - view.insert(Fluids.toVanilla(type), amount, false);
    }

    @Override
    public long getDemand(FluidType type, int pressure) {
        return pressure == 0 ? view.insert(Fluids.toVanilla(type), PROBE, true) : 0L;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return NO_TANKS;
    }

    @Override
    public boolean isLoaded() {
        return true;
    }
}
