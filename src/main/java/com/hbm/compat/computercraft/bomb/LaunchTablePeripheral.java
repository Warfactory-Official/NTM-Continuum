package com.hbm.compat.computercraft.bomb;

//? if computercraft {
import com.hbm.api.item.IDesignatorItem;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.items.ModDataComponents;
import com.hbm.tileentity.bomb.TileEntityLaunchTable;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code TileEntityLaunchTable}'s component {@code ntm_custom_launch_pad}. 1.7's designator {@code xCoord}/
 * {@code zCoord} tag is the designator's two coordinate components here.
 */
public final class LaunchTablePeripheral extends SnapshotPeripheral<TileEntityLaunchTable> {

    private static final int POWER = 0;
    private static final int MAX_POWER = 1;
    private static final int FUEL = 2;
    private static final int FUEL_MAX = 3;
    private static final int OXIDIZER = 4;
    private static final int OXIDIZER_MAX = 5;
    private static final int SOLID = 6;
    private static final int CAN_LAUNCH = 7;
    private static final int MISSILE_VALID = 8;
    private static final int HAS_DESIGNATOR = 9;
    private static final int HAS_FUEL = 10;
    private static final int FUEL_NAME = 0;
    private static final int OXIDIZER_NAME = 1;

    public LaunchTablePeripheral(TileEntityLaunchTable machine) {
        super(machine, "ntm_custom_launch_pad", 11, 2);
    }

    @Override
    protected void capture(TileEntityLaunchTable machine) {
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
        put(FUEL, machine.tanks[0].getFill());
        put(FUEL_MAX, machine.tanks[0].getMaxFill());
        put(OXIDIZER, machine.tanks[1].getFill());
        put(OXIDIZER_MAX, machine.tanks[1].getMaxFill());
        putRef(FUEL_NAME, machine.tanks[0].getTankType().getTranslationKey());
        putRef(OXIDIZER_NAME, machine.tanks[1].getTankType().getTranslationKey());
        put(SOLID, machine.solid);
        put(CAN_LAUNCH, machine.canLaunch());
        put(MISSILE_VALID, machine.isMissileValid());
        put(HAS_DESIGNATOR, machine.hasDesignator());
        put(HAS_FUEL, machine.hasFuel());
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[]{s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @LuaFunction
    public final Object[] getContents() {
        return read(s -> new Object[]{s.intAt(FUEL), s.intAt(FUEL_MAX), s.refAt(FUEL_NAME), s.intAt(OXIDIZER),
                s.intAt(OXIDIZER_MAX), s.refAt(OXIDIZER_NAME), s.intAt(SOLID), TileEntityLaunchTable.maxSolid});
    }

    @LuaFunction
    public final Object[] getLaunchInfo() {
        return read(s -> new Object[]{s.booleanAt(CAN_LAUNCH), s.booleanAt(MISSILE_VALID),
                s.booleanAt(HAS_DESIGNATOR), s.booleanAt(HAS_FUEL)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] getCoords() {
        ItemStack designator = machine().getItem(1);
        if (designator.isEmpty() || !(designator.getItem() instanceof IDesignatorItem)) {
            return new Object[]{false, "Designator not found"};
        }
        // 1.7: an untagged designator answers false alone
        if (!ModDataComponents.DESIGNATOR_X.has(designator)) return new Object[]{false};
        return new Object[]{ModDataComponents.DESIGNATOR_X.getOr(designator, 0),
                ModDataComponents.DESIGNATOR_Z.getOr(designator, 0)};
    }

    // OpenComputers' checkInteger truncates any number
    @LuaFunction(mainThread = true)
    public final Object[] setCoords(double x, double z) {
        TileEntityLaunchTable machine = machine();
        ItemStack designator = machine.getItem(1);
        if (designator.isEmpty() || !(designator.getItem() instanceof IDesignatorItem)) {
            return new Object[]{false, "Designator not found"};
        }
        ModDataComponents.DESIGNATOR_X.set(designator, (int) x);
        ModDataComponents.DESIGNATOR_Z.set(designator, (int) z);
        machine.setChanged();
        return new Object[]{true};
    }

    @LuaFunction(mainThread = true)
    public final Object[] launch() {
        TileEntityLaunchTable machine = machine();
        if (!machine.canLaunch()) return new Object[]{false};
        machine.launchFromDesignator();
        machine.setChanged();
        return new Object[]{true};
    }
}
//?}
