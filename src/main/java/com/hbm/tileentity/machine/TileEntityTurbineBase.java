package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CoolingRecipes;
import com.hbm.lib.DirPos;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared body of the steam turbines: a two-tank machine that cools the fluid in its input tank by one
 * step per tick, banks the released heat as HE, and pushes the spent fluid back out. Subclasses supply
 * how much of the tank they draw per tick, how efficient they are, and where their ports sit; the
 * lever that steps the machine through the steam grades lives here because every turbine has one.
 */
public abstract class TileEntityTurbineBase extends TileEntityLoadedBase
        implements IEnergyProviderMK2, IFluidStandardTransceiverMK2, IRORValueProvider {

    public FluidTankNTM[] tanks;
    public long powerBuffer;
    public boolean operational;

    protected TileEntityTurbineBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract double getEfficiency();

    public abstract double consumptionPercent();

    public abstract DirPos[] getConPos();

    public abstract DirPos[] getPowerPos();

    public void generatePower(long power, int steamConsumed) {
        this.powerBuffer += power;
    }

    protected void tickServer(Level level) {
        this.powerBuffer = 0;
        this.operational = false;

        FluidType input = tanks[0].getTankType();
        CoolingRecipes.CoolingRecipe recipe = CoolingRecipes.get(input);
        boolean valid = recipe != null && getEfficiency() > 0D;
        if (valid) {
            tanks[1].setTankType(recipe.output());
            int inputOps = (int) (Math.min(Math.ceil(tanks[0].getFill() * consumptionPercent()),
                    tanks[0].getFill()) / recipe.inputAmount());
            int outputOps = (tanks[1].getMaxFill() - tanks[1].getFill()) / recipe.outputAmount();
            int ops = Math.min(inputOps, outputOps);
            if (ops > 0) {
                tanks[0].setFill(tanks[0].getFill() - ops * recipe.inputAmount());
                tanks[1].setFill(tanks[1].getFill() + ops * recipe.outputAmount());
                generatePower((long) (ops * recipe.heatEnergy() * getEfficiency()), ops * recipe.inputAmount());
                setChanged();
            }
            this.operational = ops > 0;
        }

        onServerTick();

        if (!valid) tanks[1].setTankType(Fluids.NONE);
        for (DirPos connection : getPowerPos()) {
            tryProvide(level, connection.getPos(), connection.getDir());
        }
        for (DirPos connection : getConPos()) {
            tryProvide(tanks[1], level, connection);
            trySubscribe(tanks[0].getTankType(), level, connection);
        }
        networkPackNT();
    }

    public void onServerTick() { }

    public void onClientTick() { }

    public void onLeverPull() {
        FluidType type = tanks[0].getTankType();
        boolean resize = doesResizeCompressor();

        if (type == Fluids.STEAM) {
            setStage(Fluids.HOTSTEAM, Fluids.STEAM);
            if (resize) shrinkTanks();
        } else if (type == Fluids.HOTSTEAM) {
            setStage(Fluids.SUPERHOTSTEAM, Fluids.HOTSTEAM);
            if (resize) shrinkTanks();
        } else if (type == Fluids.SUPERHOTSTEAM) {
            setStage(Fluids.ULTRAHOTSTEAM, Fluids.SUPERHOTSTEAM);
            if (resize) shrinkTanks();
        } else if (type == Fluids.ULTRAHOTSTEAM) {
            setStage(Fluids.STEAM, Fluids.SPENTSTEAM);
            if (resize) {
                tanks[0].changeTankSize(tanks[0].getMaxFill() * 1000);
                tanks[1].changeTankSize(tanks[1].getMaxFill() * 1000);
            }
        } else {
            setStage(Fluids.STEAM, Fluids.SPENTSTEAM);
        }

        setChanged();
    }

    private void setStage(FluidType input, FluidType output) {
        tanks[0].setTankType(input);
        tanks[1].setTankType(output);
    }

    private void shrinkTanks() {
        tanks[0].changeTankSize(tanks[0].getMaxFill() / 10);
        tanks[1].changeTankSize(tanks[1].getMaxFill() / 10);
    }

    public boolean doesResizeCompressor() {
        return false;
    }

    @Override public long getPower() { return powerBuffer; }
    @Override public void setPower(long power) { powerBuffer = power; }
    @Override public long getMaxPower() { return powerBuffer; }

    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
        buf.writeLong(powerBuffer);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
        powerBuffer = buf.readLong();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tanks[0].writeToNBT(output, "input");
        tanks[1].writeToNBT(output, "output");
        output.putLong("power", powerBuffer);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tanks[0].readFromNBT(input, "input");
        tanks[1].readFromNBT(input, "output");
        powerBuffer = input.getLongOr("power", 0L);
    }
}
