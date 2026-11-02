package com.hbm.compat.computer.storage;

import com.hbm.compat.computer.SnapshotPeripheral;
import com.hbm.tileentity.machine.storage.TileEntityMassStorage;
import dev.golem.core.computer.ComputerMethod;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code TileEntityMassStorage}'s component {@code ntm_mass_storage}; the type is the stored item's name. */
public final class MassStoragePeripheral extends SnapshotPeripheral<TileEntityMassStorage> {

    public MassStoragePeripheral(TileEntityMassStorage machine) {
        super(machine, "ntm_mass_storage", 0, 0);
    }

    @Override
    protected void capture(TileEntityMassStorage machine) {}

    @ComputerMethod(mainThread = true)
    public final Object[] getFill() {
        return new Object[]{machine().getStockpile()};
    }

    @ComputerMethod(mainThread = true)
    public final Object[] getCapacity() {
        return new Object[]{machine().getCapacity()};
    }

    // IPeripheral owns getType(), so the Lua name is given explicitly
    @ComputerMethod(value = "getType", mainThread = true)
    public final Object[] getItemType() {
        ItemStack type = machine().getFilterType();
        return new Object[]{type.isEmpty() ? "None" : type.getHoverName().getString()};
    }

    @ComputerMethod(mainThread = true)
    public final Object[] getOutputMode() {
        return new Object[]{machine().output};
    }

    @ComputerMethod(mainThread = true)
    public final Object[] setOutputMode(boolean mode) {
        machine().output = mode;
        machine().setChanged();
        return new Object[]{};
    }
}
