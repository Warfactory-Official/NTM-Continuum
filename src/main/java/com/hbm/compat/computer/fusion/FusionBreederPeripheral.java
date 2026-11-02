package com.hbm.compat.computer.fusion;

import com.hbm.compat.computer.SnapshotPeripheral;
import com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder;
import dev.golem.core.computer.ComputerMethod;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code TileEntityFusionBreeder}'s component {@code ntm_fusion_breeder}. */
public final class FusionBreederPeripheral extends SnapshotPeripheral<TileEntityFusionBreeder> {

    private static final int NEUTRON = 0;
    private static final int PROGRESS = 1;
    private static final int FILL_0 = 2;
    private static final int MAX_0 = 3;
    private static final int FILL_1 = 4;
    private static final int MAX_1 = 5;
    private static final int INPUT_SIZE = 6;
    private static final int OUTPUT_SIZE = 7;
    private static final int NAME_0 = 0;
    private static final int NAME_1 = 1;
    private static final int INPUT_NAME = 2;
    private static final int OUTPUT_NAME = 3;

    public FusionBreederPeripheral(TileEntityFusionBreeder machine) {
        super(machine, "ntm_fusion_breeder", 8, 4);
    }

    @Override
    protected void capture(TileEntityFusionBreeder machine) {
        put(NEUTRON, machine.neutronEnergySync());
        put(PROGRESS, machine.progress / TileEntityFusionBreeder.CAPACITY);
        put(FILL_0, machine.tanks[0].getFill());
        put(MAX_0, machine.tanks[0].getMaxFill());
        put(FILL_1, machine.tanks[1].getFill());
        put(MAX_1, machine.tanks[1].getMaxFill());
        putRef(NAME_0, machine.tanks[0].getTankType().getTranslationKey());
        putRef(NAME_1, machine.tanks[1].getTankType().getTranslationKey());
        // 1.7 slots[1] and slots[2]; the description id stands in for getUnlocalizedName
        ItemStack input = machine.getItem(1);
        ItemStack output = machine.getItem(2);
        putRef(INPUT_NAME, input.isEmpty() ? "" : input.getItem().getDescriptionId());
        put(INPUT_SIZE, input.isEmpty() ? 0 : input.getCount());
        putRef(OUTPUT_NAME, output.isEmpty() ? "" : output.getItem().getDescriptionId());
        put(OUTPUT_SIZE, output.isEmpty() ? 0 : output.getCount());
    }

    @ComputerMethod
    public final Object[] getNeutronEnergy() {
        return read(s -> new Object[]{s.doubleAt(NEUTRON)});
    }

    @ComputerMethod
    public final Object[] getProgress() {
        return read(s -> new Object[]{s.doubleAt(PROGRESS)});
    }

    @ComputerMethod
    public final Object[] getFluid() {
        return read(s -> new Object[]{s.intAt(FILL_0), s.intAt(MAX_0), s.refAt(NAME_0), s.intAt(FILL_1),
                s.intAt(MAX_1), s.refAt(NAME_1)});
    }

    @ComputerMethod
    public final Object[] getCrafting() {
        return read(s -> new Object[]{s.refAt(INPUT_NAME), s.intAt(INPUT_SIZE), s.refAt(OUTPUT_NAME),
                s.intAt(OUTPUT_SIZE)});
    }

    @ComputerMethod
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.doubleAt(NEUTRON), s.doubleAt(PROGRESS), s.intAt(FILL_0), s.intAt(MAX_0),
                s.refAt(NAME_0), s.intAt(FILL_1), s.intAt(MAX_1), s.refAt(NAME_1), s.refAt(INPUT_NAME),
                s.intAt(INPUT_SIZE), s.refAt(OUTPUT_NAME), s.intAt(OUTPUT_SIZE)});
    }
}
