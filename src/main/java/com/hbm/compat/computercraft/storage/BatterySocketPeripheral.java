package com.hbm.compat.computercraft.storage;

//? if computercraft {
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.tileentity.machine.storage.TileEntityBatterySocket;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code TileEntityBatterySocket}'s {@code ntm_energy_storage}. */
public final class BatterySocketPeripheral extends EnergyStoragePeripheral<TileEntityBatterySocket> {

    private static final int POWER = SLOTS;
    private static final int MAX_POWER = SLOTS + 1;
    private static final int DELTA = SLOTS + 2;
    private static final int HAS_PACK = SLOTS + 3;
    private static final int CHARGE_RATE = SLOTS + 4;
    private static final int DISCHARGE_RATE = SLOTS + 5;
    private static final int PACK_NAME = 0;

    public BatterySocketPeripheral(TileEntityBatterySocket machine) {
        super(machine, SLOTS + 6, 1);
    }

    @Override
    protected void capture(TileEntityBatterySocket machine) {
        super.capture(machine);
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
        put(DELTA, machine.delta);
        ItemStack stack = machine.inventory.getStackInSlot(0);
        boolean pack = !stack.isEmpty() && stack.getItem() instanceof IBatteryItem;
        put(HAS_PACK, pack);
        if (pack) {
            IBatteryItem battery = (IBatteryItem) stack.getItem();
            // the item's translation key stands in for 1.7's ItemStack.getUnlocalizedName
            putRef(PACK_NAME, stack.getItem().getDescriptionId());
            put(CHARGE_RATE, battery.getChargeRate(stack));
            put(DISCHARGE_RATE, battery.getDischargeRate(stack));
        }
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[]{s.longAt(POWER), s.longAt(MAX_POWER), s.longAt(DELTA)});
    }

    @LuaFunction
    public final Object[] getPackInfo() {
        return read(s -> s.booleanAt(HAS_PACK)
                ? new Object[]{s.refAt(PACK_NAME), s.longAt(CHARGE_RATE), s.longAt(DISCHARGE_RATE)}
                : new Object[]{"", 0, 0});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> {
            boolean pack = s.booleanAt(HAS_PACK);
            return new Object[]{s.longAt(POWER), s.longAt(MAX_POWER), s.longAt(DELTA), s.intAt(RED_LOW),
                    s.intAt(RED_HIGH), s.intAt(PRIORITY), pack ? s.refAt(PACK_NAME) : "",
                    pack ? (Object) s.longAt(CHARGE_RATE) : (Object) 0,
                    pack ? (Object) s.longAt(DISCHARGE_RATE) : (Object) 0};
        });
    }
}
//?}
