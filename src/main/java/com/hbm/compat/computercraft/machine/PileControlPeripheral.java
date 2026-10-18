package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.pile.TileEntityPileControl;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.util.Mth;

/** 1.7 {@code TileEntityPileControl}'s component {@code ntm_pile_control}: the rod's extraction, set in percent. */
public final class PileControlPeripheral extends SnapshotPeripheral<TileEntityPileControl> {

    private static final int LEVEL = 0;

    public PileControlPeripheral(TileEntityPileControl machine) {
        super(machine, "ntm_pile_control", 1, 0);
    }

    @Override
    protected void capture(TileEntityPileControl machine) {
        put(LEVEL, machine.extraction);
    }

    @LuaFunction
    public final Object[] getLevel() {
        return read(s -> new Object[]{s.doubleAt(LEVEL)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setLevel(double target) {
        TileEntityPileControl machine = machine();
        machine.setTarget(Mth.clamp(target / 100D, 0D, 1D));
        machine.setChanged();
        return new Object[]{};
    }
}
//?}
