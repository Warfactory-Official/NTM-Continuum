package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.compat.computercraft.OpenComputers;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.items.machine.ItemICFPellet;
import com.hbm.tileentity.machine.TileEntityICF;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code TileEntityICF}'s component {@code ntm_icf_reactor}. */
public final class ICFPeripheral extends SnapshotPeripheral<TileEntityICF> {

    /** 1.7 {@code slots[5]}, the pellet in the chamber. */
    private static final int CHAMBER = 5;

    private static final int HEAT = 0;
    private static final int HEATUP = 1;
    private static final int LASER = 2;
    /** Fill and max fill of tanks 0..2, interleaved. */
    private static final int TANKS = 3;
    private static final int PELLET = 9;
    private static final int DEPLETION = 10;
    private static final int MAX_DEPLETION = 11;
    private static final int DIFFICULTY = 12;

    private static final int NAME_0 = 0;
    private static final int NAME_1 = 1;
    private static final int FUEL_1 = 2;
    private static final int FUEL_2 = 3;

    public ICFPeripheral(TileEntityICF machine) {
        super(machine, "ntm_icf_reactor", 13, 4);
    }

    @Override
    protected void capture(TileEntityICF machine) {
        put(HEAT, machine.heat);
        put(HEATUP, machine.heatup);
        put(LASER, machine.laser);
        for (int i = 0; i < 3; i++) {
            put(TANKS + 2 * i, machine.tanks[i].getFill());
            put(TANKS + 2 * i + 1, machine.tanks[i].getMaxFill());
        }
        // 1.7 getUnlocalizedName: "hbmfluid." + the lower-case name
        putRef(NAME_0, machine.tanks[0].getTankType().getTranslationKey());
        putRef(NAME_1, machine.tanks[1].getTankType().getTranslationKey());
        // 1.7 read slots[5] whatever it held (the depleted pellet too) and threw only when it was empty
        ItemStack pellet = machine.getItem(CHAMBER);
        put(PELLET, !pellet.isEmpty());
        if (!pellet.isEmpty()) {
            put(DEPLETION, ItemICFPellet.getDepletion(pellet));
            put(MAX_DEPLETION, ItemICFPellet.getMaxDepletion(pellet));
            put(DIFFICULTY, ItemICFPellet.getFusingDifficulty(pellet));
            putRef(FUEL_1, ItemICFPellet.getType(pellet, true).name());
            putRef(FUEL_2, ItemICFPellet.getType(pellet, false).name());
        }
    }

    @LuaFunction
    public final Object[] getHeat() {
        return read(s -> new Object[]{s.longAt(HEAT)});
    }

    @LuaFunction
    public final Object[] getHeatingRate() {
        return read(s -> new Object[]{s.longAt(HEATUP)});
    }

    @LuaFunction
    public final Object[] getMaxHeat() {
        return new Object[]{TileEntityICF.maxHeat};
    }

    @LuaFunction
    public final Object[] getPower() {
        return read(s -> new Object[]{s.longAt(LASER)});
    }

    @LuaFunction
    public final Object[] getFluid() {
        return read(s -> new Object[]{s.intAt(TANKS), s.intAt(TANKS + 1), s.refAt(NAME_0), s.intAt(TANKS + 2),
                s.intAt(TANKS + 3), s.refAt(NAME_1), s.intAt(TANKS + 4), s.intAt(TANKS + 5)});
    }

    @LuaFunction
    public final Object[] getPelletStats() {
        return read(s -> s.booleanAt(PELLET)
                ? new Object[]{s.longAt(DEPLETION), s.longAt(MAX_DEPLETION), s.longAt(DIFFICULTY), s.refAt(FUEL_1),
                s.refAt(FUEL_2)}
                : OpenComputers.unknownError());
    }
}
//?}
