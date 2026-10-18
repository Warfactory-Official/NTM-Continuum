package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.*;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.SuperComputerRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipe.OutputChoice;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.machine.ItemBlueprintFolder;
import com.hbm.items.machine.ItemDrive;
import com.hbm.items.machine.ItemDrive.EnumDriveType;
import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 1.7 {@code SuperComputerRecipes.registerDefaults}. */
public final class SuperComputerTable extends GenericTableSource<GenericRecipe> {

    private static final TagKey<Item> KEY_BLUE = TagKey.create(Registries.ITEM, Ids.of("c", "dyes/blue"));

    public SuperComputerTable() {
        super(SuperComputerRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {

        int min = 60 * 20;

        // simulate
        registerSimulation(EnumDriveType.FLASH_FLIGHTSIM, "com.flightcalc");
        registerSimulation(EnumDriveType.FLASH_PARTICLESIM, "com.particlecalc");

        // process
        registerTriplet("com.processflight", 30 * min, 15 * min, 2 * min, EnumDriveType.DISK_FLIGHTDATA, EnumDriveType.DISK_FLIGHTDATA_PROCESSED, EnumDriveType.DISK_BROKEN, 99, 95, 90);
        registerTriplet("com.processorbit", 60 * min, 30 * min, 5 * min, EnumDriveType.DISK_ORBITDATA, EnumDriveType.DISK_ORBITDATA_PROCESSED, EnumDriveType.DISK_BROKEN, 75, 65, 50);

        // copy
        registerCopy("com.copyflightcalc", 15 * min, EnumDriveType.FLASH_FLIGHTSIM, EnumDriveType.FLASH_EMPTY, EnumDriveType.FLASH_BROKEN, 95);
        registerCopy("com.copyparticlecalc", 15 * min, EnumDriveType.FLASH_PARTICLESIM, EnumDriveType.FLASH_EMPTY, EnumDriveType.FLASH_BROKEN, 95);
        registerCopy("com.copyfligthdata", 15 * min, EnumDriveType.DISK_FLIGHTDATA_PROCESSED, EnumDriveType.DISK_EMPTY, EnumDriveType.DISK_BROKEN, 75);

        this.register(new GenericRecipe("com.blueprints").setup(15 * min, 50_000L)
                .inputItems(new ComparableStack(Items.PAPER, 16),
                        new TagStack(KEY_BLUE, 16))
                .outputChoices(
                    new OutputChoice(ItemBlueprintFolder.make(0), 20),
                    new OutputChoice(new ItemStack(Items.PAPER, 16), 80)
                ));
        this.register(new GenericRecipe("com.beigeprints").setup(15 * min, 50_000L)
                .inputItems(new ComparableStack(Items.PAPER, 24),
                        CINNABAR.gem().count(24))
                .outputChoices(
                    new OutputChoice(ItemBlueprintFolder.make(1), 10),
                    new OutputChoice(new ItemStack(Items.PAPER, 24), 90)
                ));
        this.register(new GenericRecipe("com.klaus").setup(60 * min, 5_000_000L)
                .inputItems(drive(EnumDriveType.DISK_EMPTY, 64),
                        drive(EnumDriveType.DISK_EMPTY, 64),
                        drive(EnumDriveType.DISK_EMPTY, 64))
                .inputFluids(new FluidStack(Fluids.WATER, 1_000_000))
                .outputItems(ItemDrive.stack(EnumDriveType.KLAUS, 1))
                .outputFluids(new FluidStack(Fluids.SLOP, 1_000)
                ));
    }

    private void registerSimulation(EnumDriveType type, String name) {

        int min = 60 * 20;
        registerTriplet(name, 15 * min, 5 * min / 2, 1 * min, EnumDriveType.FLASH_EMPTY, type, EnumDriveType.FLASH_BROKEN, 95, 50, 25);
    }

    private void registerTriplet(String name, int time0, int time1, int time2, EnumDriveType input, EnumDriveType output, EnumDriveType broken, int chance0, int chance1, int chance2) {

        this.register(new GenericRecipe(name + "_water").setup(time0, 10_000)
                .inputItems(drive(input, 1))
                .outputChoices(
                        new OutputChoice(ItemDrive.stack(output, 1), chance0),
                        new OutputChoice(ItemDrive.stack(broken, 1), 100 - chance0))
                .inputFluids(new FluidStack(Fluids.WATER, 16_000)).outputFluids(new FluidStack(Fluids.SPENTSTEAM, 16_000)));

        this.register(new GenericRecipe(name + "_pfm").setup(time1, 10_000)
                .inputItems(drive(input, 1))
                .outputChoices(
                        new OutputChoice(ItemDrive.stack(output, 1), chance1),
                        new OutputChoice(ItemDrive.stack(broken, 1), 100 - chance1))
                .inputFluids(new FluidStack(Fluids.PERFLUOROMETHYL_COLD, 16_000)).outputFluids(new FluidStack(Fluids.PERFLUOROMETHYL, 16_000)));

        this.register(new GenericRecipe(name + "_helium").setup(time2, 10_000)
                .inputItems(drive(input, 1))
                .outputChoices(
                        new OutputChoice(ItemDrive.stack(output, 1), chance2),
                        new OutputChoice(ItemDrive.stack(broken, 1), 100 - chance2))
                .inputFluids(new FluidStack(Fluids.HELIUM4, 16_000)));
    }

    private void registerCopy(String name, int time, EnumDriveType full, EnumDriveType empty, EnumDriveType broken, int chance) {

        this.register(new GenericRecipe(name).setup(time, 10_000).setNameWrapper("com.copy")
                .inputItems(drive(full, 1), drive(empty, 1))
                .outputChoices(
                        new OutputChoice(ItemDrive.stack(full, 2), chance),
                        new OutputChoice(ItemDrive.stack(broken, 2), 100 - chance)));
    }

    private static ComparableStack drive(EnumDriveType type, int count) {
        return new ComparableStack(ModIntermediateItems.drive(type), count);
    }
}
