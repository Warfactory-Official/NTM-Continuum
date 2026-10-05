package com.hbm.compat.computercraft.fusion;

//? if computercraft {
import com.hbm.compat.computercraft.albion.CooledPeripheral;
import com.hbm.tileentity.machine.fusion.TileEntityFusionTorus;
import dan200.computercraft.api.lua.LuaFunction;

/**
 * 1.7 {@code TileEntityFusionTorus}'s component {@code ntm_fusion_torus}. {@code getEnergyInfo} and
 * {@code getCoolant} come from the cooled base, as in 1.7.
 */
public final class FusionTorusPeripheral extends CooledPeripheral<TileEntityFusionTorus> {

    private static final int FILL = SLOTS;
    private static final int KLYSTRON = SLOTS + 8;
    private static final int PLASMA = SLOTS + 9;
    private static final int CONSUMPTION = SLOTS + 10;
    private static final int PROGRESS = SLOTS + 11;
    private static final int BONUS = SLOTS + 12;

    public FusionTorusPeripheral(TileEntityFusionTorus machine) {
        super(machine, "ntm_fusion_torus", SLOTS + 13, 4);
    }

    @Override
    protected void capture(TileEntityFusionTorus machine) {
        super.capture(machine);
        for (int i = 0; i < 4; i++) {
            put(FILL + 2 * i, machine.tanks[i].getFill());
            put(FILL + 2 * i + 1, machine.tanks[i].getMaxFill());
            putRef(i, machine.tanks[i].getTankType().getTranslationKey());
        }
        put(KLYSTRON, machine.klystronEnergy);
        put(PLASMA, machine.plasmaEnergy);
        put(CONSUMPTION, machine.fuelConsumption);
        put(PROGRESS, machine.fusionModule.progress);
        put(BONUS, machine.fusionModule.bonus);
    }

    @LuaFunction
    public final Object[] getFluid() {
        return read(s -> new Object[]{s.intAt(FILL), s.intAt(FILL + 1), s.refAt(0), s.intAt(FILL + 2),
                s.intAt(FILL + 3), s.refAt(1), s.intAt(FILL + 4), s.intAt(FILL + 5), s.refAt(2), s.intAt(FILL + 6),
                s.intAt(FILL + 7), s.refAt(3)});
    }

    @LuaFunction
    public final Object[] getKlystronEnergy() {
        return read(s -> new Object[]{s.longAt(KLYSTRON)});
    }

    @LuaFunction
    public final Object[] getPlasmaEnergy() {
        return read(s -> new Object[]{s.longAt(PLASMA)});
    }

    @LuaFunction
    public final Object[] getFuelConsumption() {
        return read(s -> new Object[]{s.doubleAt(CONSUMPTION)});
    }

    @LuaFunction
    public final Object[] getRecipeProgress() {
        return read(s -> new Object[]{s.doubleAt(PROGRESS), s.doubleAt(BONUS)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.longAt(POWER), s.longAt(MAX_POWER), s.intAt(FILL), s.intAt(FILL + 1),
                s.refAt(0), s.intAt(FILL + 2), s.intAt(FILL + 3), s.refAt(1), s.intAt(FILL + 4), s.intAt(FILL + 5),
                s.refAt(2), s.intAt(FILL + 6), s.intAt(FILL + 7), s.refAt(3), s.intAt(COOLANT), s.intAt(COOLANT_MAX),
                s.intAt(HOT), s.intAt(HOT_MAX), s.longAt(KLYSTRON), s.longAt(PLASMA), s.doubleAt(CONSUMPTION),
                s.doubleAt(PROGRESS), s.doubleAt(BONUS)});
    }
}
//?}
