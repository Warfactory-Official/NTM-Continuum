package com.hbm.recipes;

import com.hbm.recipes.tables.*;
import dev.golem.core.world.DataSource;
import dev.golem.core.world.RecipeSource;
import dev.golem.core.world.DataSources;
import java.util.ArrayList;
import java.util.List;

/**
 * HBM's machine recipe tables: one {@link RecipeSource} per table, each a transliteration of the 1.7 table's
 * {@code registerDefaults()}, generated into {@code data/hbm/recipe/<table>/} at build time ({@code :26.1-neoforge:runGenerateData},
 * see {@code build.neoforge.gradle.kts}). A new table adds its line.
 */
public final class HbmDataSources implements DataSources {

    @Override
    public String namespace() {
        return "hbm";
    }

    @Override
    public List<DataSource<?>> sources() {
        List<DataSource<?>> sources = new ArrayList<>(List.of(
                // GenericRecipe tables
                new FusionTable(),
                new ExposureChamberTable(),
                new FluidBreederTable(),
                new OutgasserTable(),
                new BlastFurnaceTable(),
                new DiFurnaceTable(),
                new MixerTable(),
                new PseudoFluidTable(),
                new CrucibleTable(),
                new PlasmaForgeTable(),
                new PrecAssTable(),
                new PurexTable(),
                new ChemicalPlantTable(),
                new AssemblyMachineTable(),
                // item-processing tables
                new ToolConversionTable(),
                new RotaryFurnaceTable(),
                new CyclotronTable(),
                new ElectrolyserMetalTable(),
                new ArcFurnaceTable(),
                new MaterialDistributionTable(),
                new CentrifugeTable(),
                new CrystallizerTable(),
                // shredder, SILEX and the fluid tables
                new CompressorTable(),
                new GasCentrifugeTable(),
                new CrackingTable(),
                new ReformingTable(),
                new HydrotreatingTable(),
                new FractionTable(),
                new RadiolysisTable(),
                new RefineryTable(),
                new VacuumDistillTable(),
                new ElectrolyserFluidTable(),
                new CokerTable(),
                new LiquefactionTable(),
                new PyrolysisTable(),
                new SolidificationTable(),
                new StorageDrumTable(),
                new SILEXTable(),
                new ShredderTable(),
                // item tables
                new SawmillTable(),
                new MagicTable(),
                new ParticleAcceleratorTable(),
                new PedestalTable(),
                new AnnihilatorTable(),
                new SolderingTable(),
                new BreederTable(),
                new LemegetonTable(),
                new ArcWelderTable(),
                new PressTable(),
                new CombinationTable(),
                new AmmoPressTable(),
                new AnvilSmithingTable(),
                new AnvilConstructionTable()));
        // data folders (fluid properties, data maps, item pools, removal lists, ...)
        sources.addAll(com.hbm.recipes.data.D3Sources.sources());
        sources.addAll(com.hbm.recipes.data.D1Sources.sources());
        sources.addAll(com.hbm.recipes.data.D2Sources.sources());
        return sources;
    }
}
