package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import org.jspecify.annotations.Nullable;
import com.hbm.lib.ForgeDirection;

/** A tile that can be connected to by a fluid network of the given type/side. */
public interface IFluidConnectorMK2 {
    /** Whether the given side can be connected to. */
    default boolean canConnect(FluidType type, ForgeDirection dir) {
        return dir != ForgeDirection.UNKNOWN;
    }

    /**
     * An immediate role contact, independent of fill, pressure and network subscription. A null type asks the
     * configured contact types rather than searching the entire fluid registry.
     */
    default boolean exposesFluidContact(@Nullable FluidType type, ForgeDirection dir, boolean receiving) {
        if (dir == ForgeDirection.UNKNOWN) return false;
        if (this instanceof IFluidDirectionalMK2 directional
                && !(receiving ? directional.receivesOn(dir) : directional.providesOn(dir))) return false;
        if (type == null) return exposesAnyFluidContact(dir);
        if (!canConnect(type, dir) && !canConnect(Fluids.kindOf(type), dir)) return false;
        if (this instanceof IFluidUserMK2 user) {
            var tanks = receiving && this instanceof IFluidStandardReceiverMK2 receiver ? receiver.getReceivingTanks()
                    : !receiving && this instanceof IFluidStandardSenderMK2 sender ? sender.getSendingTanks() : user.getAllTanks();
            if (tanks.length > 0) {
                for (var tank : tanks) if (tank.joins(type)) return true;
                return false;
            }
        }
        return true;
    }

    /**
     * The untyped contact query: side-only gates answer through NONE; typed gates use the machine's current tank
     * types. A virtual sink whose inputs are not owned tanks overrides this from its own declared input types.
     */
    default boolean exposesAnyFluidContact(ForgeDirection dir) {
        if (canConnect(Fluids.NONE, dir)) return true;
        if (this instanceof IFluidUserMK2 user) {
            for (var tank : user.getAllTanks()) if (canConnect(tank.getTankType(), dir)) return true;
        }
        return false;
    }
}
