package com.hbm.compat.computercraft.storage;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.storage.TileEntityMassStorage;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code TileEntityMassStorage}'s component {@code ntm_mass_storage}; the type is the stored item's name. */
public final class MassStoragePeripheral extends SnapshotPeripheral<TileEntityMassStorage> {

    public MassStoragePeripheral(TileEntityMassStorage machine) {
        super(machine, "ntm_mass_storage", 0, 0);
    }

    @Override
    protected void capture(TileEntityMassStorage machine) {}

    @LuaFunction(mainThread = true)
    public final Object[] getFill() {
        return new Object[]{machine().getStockpile()};
    }

    @LuaFunction(mainThread = true)
    public final Object[] getCapacity() {
        return new Object[]{machine().getCapacity()};
    }

    // IPeripheral owns getType(), so the Lua name is given explicitly
    @LuaFunction(value = "getType", mainThread = true)
    public final Object[] getItemType() {
        ItemStack type = machine().getFilterType();
        return new Object[]{type.isEmpty() ? "None" : type.getHoverName().getString()};
    }

    @LuaFunction(mainThread = true)
    public final Object[] getOutputMode() {
        return new Object[]{machine().output};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setOutputMode(boolean mode) {
        machine().output = mode;
        machine().setChanged();
        return new Object[]{};
    }
}
//?}
