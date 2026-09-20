package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.ModPowerGenBlockEntities;

/**
 * 1.7 {@code TileEntityMachineIGenerator} — the industrial generator memorial. The whole generation loop is
 * commented out upstream ("o7"), the GUI provider returns null and the block never opens it; what remains is
 * a 21-slot inventory, three tanks and their NBT, kept here so a world carrying one round-trips unchanged.
 * The rotor angle it renders with is never advanced, exactly as upstream.
 */
public class TileEntityMachineIGenerator extends TileEntityMachineBase {

    public long power;
    public int spin;
    public int[] burn = new int[4];
    public boolean hasRTG = false;
    public final int[] rtgSlots = {11, 12, 13, 14, 15, 16, 17, 18, 19, 20};

    public float rotation;
    public float prevRotation;

    public final FluidTankNTM[] tanks;

    public static final int coalConRate = 75;

    /* CONFIGURABLE */
    public static long maxPower = 1_000_000;
    public static int waterCap = 16000;
    public static int oilCap = 16000;
    public static int lubeCap = 4000;
    public static int coalGenRate = 100;
    public static double rtgHeatMult = 0.15D;
    public static int waterRate = 10;
    public static int lubeRate = 1;
    public static long fluidHeatDiv = 1_000L;

    protected long output;

    public TileEntityMachineIGenerator(BlockPos pos, BlockState state) {
        super(ModPowerGenBlockEntities.INDUSTRIAL_GENERATOR.get(), pos, state, 21);
        tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.WATER, waterCap),
                new FluidTankNTM(Fluids.HEATINGOIL, oilCap),
                new FluidTankNTM(Fluids.LUBRICANT, lubeCap)
        };
    }

    @Override
    public String getDefaultName() {
        return "container.iGenerator";
    }

    /** 1.7: only the four solid-fuel slots take anything, and only burnable items. */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot >= 3 && slot <= 6 && level != null && dev.golem.core.world.Compat.burnDuration(level, stack) > 0;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[]{3, 4, 5, 6};
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        for (int i = 0; i < 3; i++) tanks[i].writeToNBT(output, "tank_" + i);
        output.putLong("power", power);
        output.putIntArray("burn", burn);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        for (int i = 0; i < 3; i++) tanks[i].readFromNBT(input, "tank_" + i);
        power = input.getLongOr("power", 0L);
        int[] loaded = input.getIntArrayOr("burn", new int[4]);
        burn = loaded.length == 4 ? loaded : new int[4];
    }
}
