package com.hbm.tileentity.machine.albion;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Base for the cryogenically cooled machines — the particle accelerator's magnets and the fusion
 * reactor's superconducting solenoids. Each one passively warms every tick and is dragged back down by
 * running perfluoromethyl through it; the machine only operates while {@link #isCool()}.
 *
 * <p>Ported from 1.12.2's {@code albion/TileEntityCooledBase}. The class stays in the {@code albion}
 * package (upstream's name for the accelerator) because the accelerator parts will share it.
 */
public abstract class TileEntityCooledBase extends TileEntityMachineBase
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2 {

    public static final float KELVIN = 273F;
    /** The machine has to reach this to run: −150 °C. */
    public static final float temperature_target = KELVIN - 150F;
    public static final float temp_change_per_mb = 0.5F;
    public static final float temp_passive_heating = 2.5F;
    public static final float temp_change_max = 5F + temp_passive_heating;

    public final FluidTankNTM[] coolantTanks;
    public long power;
    public float temperature = KELVIN + 20;

    public TileEntityCooledBase(BlockEntityType<?> type, BlockPos pos, BlockState state, int slotCount) {
        super(type, pos, state, slotCount);
        this.coolantTanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.PERFLUOROMETHYL_COLD, 4_000),
                new FluidTankNTM(Fluids.PERFLUOROMETHYL, 4_000),
        };
    }

    /** Where this machine touches the world for power and coolant. */
    public abstract List<DirPos> getConPos();

    /**
     * Runs the passive heating and the coolant exchange. Subclasses call this from their own tick;
     * it deliberately does not subscribe to networks, because each machine has its own cadence for that.
     */
    protected void updateCooling() {
        this.temperature += temp_passive_heating;
        if (this.temperature > KELVIN + 20) this.temperature = KELVIN + 20;

        if (this.temperature <= temperature_target) return;

        int cyclesTemp = (int) Math.ceil(Math.min(this.temperature - temperature_target, temp_change_max) / temp_change_per_mb);
        int cyclesCool = coolantTanks[0].getFill();
        int cyclesHot = coolantTanks[1].getMaxFill() - coolantTanks[1].getFill();
        int cycles = Math.min(cyclesTemp, Math.min(cyclesCool, cyclesHot));

        coolantTanks[0].setFill(coolantTanks[0].getFill() - cycles);
        coolantTanks[1].setFill(coolantTanks[1].getFill() + cycles);
        this.temperature -= temp_change_per_mb * cycles;
    }

    /** Subscribes to power and cold coolant, and offers the warmed coolant back out. */
    protected void exchangeCoolant(Level level) {
        for (DirPos pos : getConPos()) {
            if (coolantTanks[1].getFill() > 0) this.tryProvide(coolantTanks[1], level, pos.getPos(), pos.getDir().toDirection());
        }
    }

    public boolean isCool() {
        return this.temperature <= temperature_target;
    }

    @Override public long getPower() { return this.power; }
    @Override public void setPower(long power) { this.power = power; }

    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{coolantTanks[1]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{coolantTanks[0]}; }
    @Override public FluidTankNTM[] getAllTanks() { return coolantTanks; }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        coolantTanks[0].serialize(buf);
        coolantTanks[1].serialize(buf);
        buf.writeFloat(temperature);
        buf.writeLong(power);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        coolantTanks[0].deserialize(buf);
        coolantTanks[1].deserialize(buf);
        this.temperature = buf.readFloat();
        this.power = buf.readLong();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        coolantTanks[0].writeToNBT(output, "t0");
        coolantTanks[1].writeToNBT(output, "t1");
        output.putFloat("temperature", temperature);
        output.putLong("power", power);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        coolantTanks[0].readFromNBT(input, "t0");
        coolantTanks[1].readFromNBT(input, "t1");
        this.temperature = input.getFloatOr("temperature", KELVIN + 20);
        this.power = input.getLongOr("power", 0L);
    }
}
