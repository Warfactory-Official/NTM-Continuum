package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidIcon;
import net.minecraft.world.item.ItemStack;

/**
 * The fusion torus's fuel cycles, ported 1:1 from the 1.12.2 {@code FusionRecipes}.
 *
 * <p>They form a progression rather than a menu: each cycle's {@link FusionRecipe#ignitionTemp} is the
 * klystron energy needed to light it, and its {@link FusionRecipe#outputTemp} is what it then gives
 * back — so an early cycle's output is what lets you afford the next one's ignition. D-D and D-O light
 * off a single klystron; chlorine needs the tritium or helium-3 phases running first; DHC needs
 * chlorine; and stellar flux sits at the top at 10 MHE ignition for 50 MHE output.
 *
 * <p>{@code maxInput} is the largest ignition temperature of any cycle, which the creative klystron
 * uses as its output so it can light anything.
 */
public final class FusionRecipes extends GenericRecipes {

    public static final FusionRecipes INSTANCE = new FusionRecipes();

    /** The highest ignition temperature of any registered cycle (the creative klystron's output). */
    public long maxInput;

    /**
     * The fusion breeder's flux buffer. Cycle flux values are written as fractions of it, so a cycle's
     * breeding rate reads as "fills a breeder in N seconds" rather than as a bare number. Lives here
     * rather than on the breeder so the recipe table has no forward dependency on that machine.
     */
    public static final double BREEDER_CAPACITY = 10_000D;

    private FusionRecipes() {}

    @Override
    public void registerRecipes() {
        if (!recipeOrderedList.isEmpty()) return;

        long solenoid = 25_000;
        double breederCapacity = BREEDER_CAPACITY;

        // Mostly for breeding helium and tritium; the energy gain alone is enough to ignite T-He4.
        // 15 MHE/s in, 20 MHE/s out.
        register(new FusionRecipe("fus.dd").setInputEnergy(750_000).setOutputEnergy(1_000_000)
                .setOutputFlux(breederCapacity / 200)
                .setRGB(1F, 0.2F, 0.2F) // extra red
                .setNamed().setIcon(gas(Fluids.DEUTERIUM))
                .setup(100, solenoid)
                .inputFluids(new FluidStack(Fluids.DEUTERIUM, 20))
                .outputFluids(new FluidStack(Fluids.HELIUM4, 1_000))); // should be helium-3, muh realisme

        // Early fuel. 5 MHE/s in, 20 MHE/s out.
        register(new FusionRecipe("fus.do").setInputEnergy(250_000).setOutputEnergy(1_250_000)
                .setOutputFlux(breederCapacity / 200)
                .setNamed().setIcon(gas(Fluids.OXYGEN))
                .setup(100, solenoid)
                .inputFluids(new FluidStack(Fluids.DEUTERIUM, 10), new FluidStack(Fluids.OXYGEN, 10))
                .outputItems(new ItemStack(ModItems.pellet_charged.get())));

        // Medium fuel. 15 MHE/s in, 75 MHE/s out.
        register(new FusionRecipe("fus.dt").setInputEnergy(750_000).setOutputEnergy(3_750_000)
                .setOutputFlux(breederCapacity / 100)
                .setNamed().setIcon(gas(Fluids.HELIUM4))
                .setup(100, solenoid)
                .inputFluids(new FluidStack(Fluids.DEUTERIUM, 10), new FluidStack(Fluids.TRITIUM, 10))
                .outputFluids(new FluidStack(Fluids.HELIUM4, 1_000)));

        // Medium fuel; needs three klystrons, or another cycle running in tandem. 50 MHE/s to 125 MHE/s.
        register(new FusionRecipe("fus.tcl").setInputEnergy(2_500_000).setOutputEnergy(6_250_000)
                .setOutputFlux(breederCapacity / 20)
                .setRGB(0.8F, 0.6F, 0.4F) // everything chlorine is piss yellow
                .setNamed().setIcon(new ItemStack(ModItems.powder_chlorophyte.get()))
                .setup(100, solenoid)
                .inputFluids(new FluidStack(Fluids.TRITIUM, 10), new FluidStack(Fluids.CHLORINE, 10))
                .outputItems(new ItemStack(ModItems.powder_chlorophyte.get())));

        // Medium fuel, aneutronic. 10 MHE/s to 75 MHE/s.
        register(new FusionRecipe("fus.h3").setInputEnergy(500_000).setOutputEnergy(3_750_000)
                .setOutputFlux(0)
                .setRGB(0.2F, 0.2F, 1F) // helium blue
                .setNamed().setIcon(gas(Fluids.HELIUM3))
                .setup(100, solenoid)
                .inputFluids(new FluidStack(Fluids.HELIUM3, 20))
                .outputFluids(new FluidStack(Fluids.HELIUM4, 1_000)));

        // Medium fuel, runs in tandem with D-D. 17.5 MHE/s to 80 MHE/s.
        register(new FusionRecipe("fus.th4").setInputEnergy(875_000).setOutputEnergy(4_000_000)
                .setOutputFlux(breederCapacity / 20)
                .setRGB(0.2F, 0.2F, 1F)
                .setNamed().setIcon(gas(Fluids.TRITIUM))
                .setup(100, solenoid)
                .inputFluids(new FluidStack(Fluids.TRITIUM, 10), new FluidStack(Fluids.HELIUM4, 10))
                .outputItems(new ItemStack(ModItems.pellet_charged.get())));

        // High fuel; ignition exceeds a klystron's output, so it needs T-He4 or He-3 first.
        // 75 MHE/s to 200 MHE/s.
        register(new FusionRecipe("fus.cl").setInputEnergy(3_750_000).setOutputEnergy(10_000_000)
                .setOutputFlux(breederCapacity / 10)
                .setRGB(1F, 0.6F, 0.2F) // even more yellow
                .setNamed().setIcon(new ItemStack(ModItems.powder_chlorophyte.get()))
                .setup(100, solenoid)
                .inputFluids(new FluidStack(Fluids.CHLORINE, 20))
                .outputItems(new ItemStack(ModItems.powder_chlorophyte.get())));

        // High fuel; needs the chlorine phase to ignite. 200 MHE/s to 500 MHE/s.
        register(new FusionRecipe("fus.dhc").setInputEnergy(10_000_000).setOutputEnergy(25_000_000)
                .setOutputFlux(breederCapacity / 5)
                .setRGB(0.2F, 0.8F, 0.8F) // cyan
                .setNamed().setIcon(icon(Fluids.DHC))
                .setup(100, solenoid)
                .inputFluids(new FluidStack(Fluids.DHC, 20))
                .outputItems(new ItemStack(ModItems.powder_chlorophyte.get())));

        // High fuel, low ignition point. 20 MHE/s to 250 MHE/s.
        register(new FusionRecipe("fus.bf").setInputEnergy(1_000_000).setOutputEnergy(12_500_000)
                .setOutputFlux(breederCapacity / 5)
                .setRGB(0.2F, 1F, 0.2F) // what do you think?
                .setNamed().setIcon(icon(Fluids.BALEFIRE))
                .setup(100, solenoid)
                .inputFluids(new FluidStack(Fluids.BALEFIRE, 15), new FluidStack(Fluids.AMAT, 5))
                .outputItems(new ItemStack(ModItems.powder_balefire.get())));

        // High fuel, high ignition point. 200 MHE/s to 1 GHE/s.
        register(new FusionRecipe("fus.stellar").setInputEnergy(10_000_000).setOutputEnergy(50_000_000)
                .setOutputFlux(breederCapacity)
                .setRGB(1F, 0.4F, 0.1F) // brilliant orange, like looking into a furnace
                .setNamed().setIcon(icon(Fluids.STELLAR_FLUX))
                .setup(100, solenoid)
                .inputFluids(new FluidStack(Fluids.STELLAR_FLUX, 10))
                .outputItems(new ItemStack(ModItems.powder_gold.get()))); // eough

        maxInput = 0;
        for (var recipe : recipeOrderedList) {
            if (recipe instanceof FusionRecipe fusion && fusion.ignitionTemp > maxInput) maxInput = fusion.ignitionTemp;
        }
    }

    /** The recipe's display icon: 1.7 used a filled gas tank ({@code gas_full}) for the bottled gases. */
    private static ItemStack gas(com.hbm.inventory.fluid.FluidType fluid) {
        com.hbm.items.machine.ItemFluidContainerNT tank = com.hbm.items.ModFluidNetItems.gas_full.get();
        return tank.make(fluid, tank.capacity);
    }

    private static ItemStack icon(com.hbm.inventory.fluid.FluidType fluid) {
        return ItemFluidIcon.make(fluid);
    }
}
