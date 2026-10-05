package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.blocks.machine.dfc.ModDFCBlocks;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.items.machine.ItemLens;
import com.hbm.tileentity.machine.dfc.TileEntityCoreStabilizer;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code TileEntityCoreStabilizer}'s component {@code dfc_stabilizer}. */
public final class CoreStabilizerPeripheral extends SnapshotPeripheral<TileEntityCoreStabilizer> {

    private static final int POWER = 0;
    private static final int MAX_POWER = 1;
    private static final int WATTS = 2;
    private static final int HAS_LENS = 3;
    private static final int LENS_DAMAGE = 4;

    public CoreStabilizerPeripheral(TileEntityCoreStabilizer machine) {
        super(machine, "dfc_stabilizer", 5, 0);
    }

    @Override
    protected void capture(TileEntityCoreStabilizer machine) {
        put(POWER, machine.power);
        put(MAX_POWER, TileEntityCoreStabilizer.maxPower);
        put(WATTS, machine.watts);
        // 1.7: slot 0 holds ams_lens and its damage is below the lens's maxDamage, else "N/A"
        ItemStack lens = machine.inventory.getStackInSlot(0);
        boolean usable = !lens.isEmpty() && lens.getItem() == ModDFCBlocks.ams_lens.get()
                && ItemLens.getLensDamage(lens) < ModDFCBlocks.ams_lens.get().maxDamage;
        put(HAS_LENS, usable);
        put(LENS_DAMAGE, usable ? ItemLens.getLensDamage(lens) : 0L);
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[]{s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @LuaFunction
    public final Object[] getInput() {
        return read(s -> new Object[]{s.intAt(WATTS)});
    }

    @LuaFunction
    public final Object[] getDurability() {
        return read(s -> new Object[]{durability(s)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.longAt(POWER), s.longAt(MAX_POWER), s.intAt(WATTS), durability(s)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setInput(double input) {
        TileEntityCoreStabilizer machine = machine();
        machine.watts = Mth.clamp((int) input, 0, 100);
        machine.setChanged();
        return new Object[]{};
    }

    private static Object durability(SnapshotPeripheral<?> s) {
        return s.booleanAt(HAS_LENS) ? (Object) s.longAt(LENS_DAMAGE) : "N/A";
    }
}
//?}
