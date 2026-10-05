package com.hbm.compat.computercraft.bomb;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.items.weapon.ItemMissileStandard;
import com.hbm.tileentity.bomb.TileEntityLaunchPadBase;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code TileEntityLaunchPadBase}'s component {@code ntm_launch_pad}: the launch pad and the large launch pad. */
public final class LaunchPadPeripheral extends SnapshotPeripheral<TileEntityLaunchPadBase> {

    private static final int POWER = 0;
    private static final int MAX_POWER = 1;
    private static final int FUEL = 2;
    private static final int FUEL_MAX = 3;
    private static final int OXIDIZER = 4;
    private static final int OXIDIZER_MAX = 5;
    private static final int CAN_LAUNCH = 6;
    private static final int TIER = 7;
    private static final int X = 8;
    private static final int Y = 9;
    private static final int Z = 10;
    private static final int FUEL_NAME = 0;
    private static final int OXIDIZER_NAME = 1;

    public LaunchPadPeripheral(TileEntityLaunchPadBase machine) {
        super(machine, "ntm_launch_pad", 11, 2);
    }

    @Override
    protected void capture(TileEntityLaunchPadBase machine) {
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
        put(FUEL, machine.tanks[0].getFill());
        put(FUEL_MAX, machine.tanks[0].getMaxFill());
        put(OXIDIZER, machine.tanks[1].getFill());
        put(OXIDIZER_MAX, machine.tanks[1].getMaxFill());
        putRef(FUEL_NAME, machine.tanks[0].getTankType().getTranslationKey());
        putRef(OXIDIZER_NAME, machine.tanks[1].getTankType().getTranslationKey());
        put(CAN_LAUNCH, machine.canLaunch());
        // 1.7 getTier: nothing unless the missile is valid, else its tier 0-4
        ItemStack missile = machine.getItem(0);
        put(TIER, machine.isMissileValid() && missile.getItem() instanceof ItemMissileStandard item
                ? item.tier.ordinal() : -1);
        put(X, machine.getBlockPos().getX());
        put(Y, machine.getBlockPos().getY());
        put(Z, machine.getBlockPos().getZ());
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[]{s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @LuaFunction
    public final Object[] getFluid() {
        return read(s -> new Object[]{s.intAt(FUEL), s.intAt(FUEL_MAX), s.refAt(FUEL_NAME), s.intAt(OXIDIZER),
                s.intAt(OXIDIZER_MAX), s.refAt(OXIDIZER_NAME)});
    }

    @LuaFunction
    public final Object[] canLaunch() {
        return read(s -> new Object[]{s.booleanAt(CAN_LAUNCH)});
    }

    @LuaFunction
    public final Object[] getTier() {
        return read(s -> s.intAt(TIER) < 0 ? new Object[]{} : new Object[]{s.intAt(TIER)});
    }

    // OpenComputers' checkInteger truncates any number; 1.7 passed -1 as the unused y
    @LuaFunction(mainThread = true)
    public final Object[] launch(double x, double z) {
        TileEntityLaunchPadBase machine = machine();
        if (!machine.canLaunch()) return new Object[]{false};
        boolean launched = machine.sendCommandPosition((int) x, -1, (int) z);
        machine.setChanged();
        return new Object[]{launched};
    }

    @LuaFunction
    public final Object[] getPos() {
        return read(s -> new Object[]{s.intAt(X), s.intAt(Y), s.intAt(Z)});
    }
}
//?}
