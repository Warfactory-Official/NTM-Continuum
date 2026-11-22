package net.neoforged.neoforge.fluids.capability;
import net.neoforged.neoforge.fluids.FluidStack;
public interface IFluidHandler { enum FluidAction { SIMULATE, EXECUTE; public boolean execute(){return this==EXECUTE;} }
 int getTanks(); FluidStack getFluidInTank(int t); int getTankCapacity(int t); boolean isFluidValid(int t,FluidStack s);
 int fill(FluidStack r,FluidAction a); FluidStack drain(FluidStack r,FluidAction a); FluidStack drain(int m,FluidAction a); }
