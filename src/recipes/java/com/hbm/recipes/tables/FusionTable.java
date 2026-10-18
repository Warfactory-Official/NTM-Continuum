package com.hbm.recipes.tables;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.FusionRecipe;
import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.items.ModFluidNetItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidContainerNT;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code FusionRecipes.registerDefaults}. */
public final class FusionTable extends GenericTableSource<FusionRecipe> {

    public FusionTable() {
        super(FusionRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {

        long solenoid = 25_000;
        double breederCapacity = FusionRecipes.BREEDER_CAPACITY; // 1.7 TileEntityFusionBreeder.capacity

        // mostly for breeding helium and tritium, energy gains are enough to ignite TH4
        // 15MHE/s to 20MHE/s
        this.register((FusionRecipe) new FusionRecipe("fus.dd").setInputEnergy(750_000).setOutputEnergy(1_000_000).setOutputFlux(breederCapacity / 200)
                .setRGB(1F, 0.2F, 0.2F) // extra red
                .setNamed().setIcon(gasFull(Fluids.DEUTERIUM))
                .setPower(solenoid).setDuration(100)
                .inputFluids(new FluidStack(Fluids.DEUTERIUM, 20))
                .outputFluids(new FluidStack(Fluids.HELIUM4, 1_000))); // akshuyally it should be helium-3 muh realisme

        // early fuel
        // 5MHE/s to 20MHE/s
        this.register((FusionRecipe) new FusionRecipe("fus.do").setInputEnergy(250_000).setOutputEnergy(1_250_000).setOutputFlux(breederCapacity / 200)
                .setNamed().setIcon(gasFull(Fluids.OXYGEN))
                .setPower(solenoid).setDuration(100)
                .inputFluids(new FluidStack(Fluids.DEUTERIUM, 10), new FluidStack(Fluids.OXYGEN, 10))
                .outputItems(new ItemStack(ModItems.pellet_charged.get())));

        // medium fuel
        // 15MHE/s to 75MHE/s
        this.register((FusionRecipe) new FusionRecipe("fus.dt").setInputEnergy(750_000).setOutputEnergy(3_750_000).setOutputFlux(breederCapacity / 100)
                .setNamed().setIcon(gasFull(Fluids.HELIUM4))
                .setPower(solenoid).setDuration(100)
                .inputFluids(new FluidStack(Fluids.DEUTERIUM, 10), new FluidStack(Fluids.TRITIUM, 10))
                .outputFluids(new FluidStack(Fluids.HELIUM4, 1_000)));

        // medium fuel, three klystrons or in tandem
        // 50MHE/s to 125MHE/s
        this.register((FusionRecipe) new FusionRecipe("fus.tcl").setInputEnergy(2_500_000).setOutputEnergy(6_250_000).setOutputFlux(breederCapacity / 20)
                .setRGB(0.8F, 0.6F, 0.4F) // everything chlorine is piss yellow
                .setNamed().setIcon(new ItemStack(ModItems.powder_chlorophyte.get()))
                .setPower(solenoid).setDuration(100)
                .inputFluids(new FluidStack(Fluids.TRITIUM, 10), new FluidStack(Fluids.CHLORINE, 10))
                .outputItems(new ItemStack(ModItems.powder_chlorophyte.get())));

        // medium fuel, aneutronic
        // 10MHE/s to 75MHE/s
        this.register((FusionRecipe) new FusionRecipe("fus.h3").setInputEnergy(500_000).setOutputEnergy(3_750_000).setOutputFlux(0)
                .setRGB(0.2F, 0.2F, 1F) // helium blue
                .setNamed().setIcon(gasFull(Fluids.HELIUM3))
                .setPower(solenoid).setDuration(100)
                .inputFluids(new FluidStack(Fluids.HELIUM3, 20))
                .outputFluids(new FluidStack(Fluids.HELIUM4, 1_000)));

        // medium fuel, in tandem with DD
        // 17.5MHE/s to 80MHE/s
        this.register((FusionRecipe) new FusionRecipe("fus.th4").setInputEnergy(875_000).setOutputEnergy(4_000_000).setOutputFlux(breederCapacity / 20)
                .setRGB(0.2F, 0.2F, 1F)
                .setNamed().setIcon(gasFull(Fluids.TRITIUM))
                .setPower(solenoid).setDuration(100)
                .inputFluids(new FluidStack(Fluids.TRITIUM, 10), new FluidStack(Fluids.HELIUM4, 10))
                .outputItems(new ItemStack(ModItems.pellet_charged.get())));

        // high fuel, ignition exceeds klystron power, requires TH4 or H3
        // 75MHE/s to 200MHE/s
        this.register((FusionRecipe) new FusionRecipe("fus.cl").setInputEnergy(3_750_000).setOutputEnergy(10_000_000).setOutputFlux(breederCapacity / 10)
                .setRGB(1F, 0.6F, 0.2F) // even more yellow
                .setNamed().setIcon(new ItemStack(ModItems.powder_chlorophyte.get()))
                .setPower(solenoid).setDuration(100)
                .inputFluids(new FluidStack(Fluids.CHLORINE, 20))
                .outputItems(new ItemStack(ModItems.powder_chlorophyte.get())));

        // high fuel, requires chlorine phase to ignite
        // 200MHE/s to 500MHE/s
        this.register((FusionRecipe) new FusionRecipe("fus.dhc").setInputEnergy(10_000_000).setOutputEnergy(25_000_000).setOutputFlux(breederCapacity / 5)
                .setRGB(0.2F, 0.8F, 0.8F) // cyan
                .setNamed().setIcon(ItemFluidIcon.make(Fluids.DHC))
                .setPower(solenoid).setDuration(100)
                .inputFluids(new FluidStack(Fluids.DHC, 20))
                .outputItems(new ItemStack(ModItems.powder_chlorophyte.get())));

        // high fuel, low ignition point
        // 20MHE/s to 250MHE/s
        this.register((FusionRecipe) new FusionRecipe("fus.bf").setInputEnergy(1_000_000).setOutputEnergy(12_500_000).setOutputFlux(breederCapacity / 5)
                .setRGB(0.2F, 1F, 0.2F) // what do you think?
                .setNamed().setIcon(ItemFluidIcon.make(Fluids.BALEFIRE))
                .setPower(solenoid).setDuration(100)
                .inputFluids(new FluidStack(Fluids.BALEFIRE, 15), new FluidStack(Fluids.AMAT, 5)) // do we kick the antimatter requirement or maybe change it?
                .outputItems(new ItemStack(ModItems.powder_balefire.get())));

        // high fuel, high ignition point
        // 200MHE/s/s to 1GHE/s
        this.register((FusionRecipe) new FusionRecipe("fus.stellar").setInputEnergy(10_000_000).setOutputEnergy(50_000_000).setOutputFlux(breederCapacity / 1)
                .setRGB(1F, 0.4F, 0.1F) // brilliant orange, like looking into a furnace
                .setNamed().setIcon(ItemFluidIcon.make(Fluids.STELLAR_FLUX))
                .setPower(solenoid).setDuration(100)
                .inputFluids(new FluidStack(Fluids.STELLAR_FLUX, 5))
                .outputItems(new ItemStack(ModItems.powder_gold.get()))); // eough
    }

    /** 1.7 {@code new ItemStack(ModItems.gas_full, 1, fluid.getID())}: a full gas tank of the fluid. */
    private static ItemStack gasFull(FluidType fluid) {
        ItemFluidContainerNT tank = ModFluidNetItems.gas_full.get();
        return tank.make(fluid, tank.capacity);
    }
}
