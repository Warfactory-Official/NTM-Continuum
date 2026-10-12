package com.hbm.recipes.data;

import dev.golem.core.world.DataSource;
import java.util.List;

/** Data agent D2's sources: HBM's data maps ({@code value_maps/}) and the fallout transforms. */
public final class D2Sources {

    private D2Sources() {
    }

    public static List<DataSource<?>> sources() {
        return List.of(
                // item maps
                new BrickFurnaceSpeedSource(),
                new DiFurnaceFuelSource(),
                new MachineUpgradeSource(),
                new PACoilSource(),
                new DrillBitSource(),
                new PistonSource(),
                new BatteryPackSource(),
                new BetavoltaicSource(),
                new PlateFuelSource(),
                new ResearchReactorFuelSource(),
                new PlasmaForgeBoosterSource(),
                new RadGenFuelSource(),
                new SatelliteSource(),
                new FuelCategorySource(),
                new ZirnoxFuelSource(),
                new PWRFuelSource(),
                new WatzPelletSource(),
                new PileRodSource(),
                new RTGPelletSource(),
                new ItemDamageResistanceSource(),
                new LockpickJacketSource(),
                new FluidContainerSource(),
                new CustomNukeSource.Items(),
                new ProtectionClassesSource(),
                new RadiationResistanceSource(),
                new ArmorSetsSource(),
                new RBMKFuelSource(),
                new HazardRegistrySource.Items(),
                new HazardRegistrySource.Cells(),
                // fluid maps
                new AutosawFuelSource(),
                new GasTurbineFuelSource(),
                new ICFFuelSource.Fluid(),
                new CustomNukeSource.Cells(),
                new FluidContainersSource(),
                // material, block and entity maps
                new ICFFuelSource.Material(),
                new RadiationEmissionSource(),
                new ErosionSource(),
                new FluidDepositSource(),
                new EntityDamageResistanceSource(),
                // fallout_transform
                new FalloutTransformSource());
    }
}
