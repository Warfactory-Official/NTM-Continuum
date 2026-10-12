package com.hbm.recipes.data;

import com.hbm.api.data.FluidDataMaps;
import com.hbm.api.data.FluidDataMaps.Containers;
import dev.golem.core.data.DataMapSource;
import java.util.Optional;

/**
 * 1.7 {@code Fluids.init}: every {@code .addContainers(new CD_Canister(color))} and
 * {@code .addContainers(new CD_Gastank(bottleColor, labelColor))}, each in 1.7's declaration order; the file lists the
 * canisters first, then the gas tanks.
 */
public final class FluidContainersSource extends DataMapSource<Containers> {

    public FluidContainersSource() {
        super(FluidDataMaps.FLUID_CONTAINERS, false);
    }

    /** 1.7 {@code CD_Canister(color)}. */
    private void canister(String fluid, int color) {
        put("hbm:" + fluid, new Containers(Optional.of(color), Optional.empty(), Optional.empty()));
    }

    /** 1.7 {@code CD_Gastank(bottleColor, labelColor)}. */
    private void gastank(String fluid, int bottle, int label) {
        put("hbm:" + fluid, new Containers(Optional.empty(), Optional.of(bottle), Optional.of(label)));
    }

    @Override
    protected void values() {
        canister("oil", 0x424242);
        canister("heavyoil", 0x513F39);
        canister("bitumen", 0x5A5877);
        canister("smear", 0x624F3B);
        canister("heatingoil", 0x694235);
        canister("reclaimed", 0xF65723);
        canister("petroil", 0x2369F6);
        canister("lubricant", 0xF1CC05);
        canister("naphtha", 0x5F6D44);
        canister("diesel", 0xFF2C2C);
        canister("lightoil", 0xB46B52);
        canister("kerosene", 0xFF377D);
        canister("biofuel", 0x9EB623);
        canister("nitan", 0x6B238C);
        canister("gasoline", 0x2F7747);
        canister("coalgas", 0x2E155F);
        canister("fracksol", 0x4F887F);
        canister("ethanol", 0xEAFFF3);
        canister("crackoil", 0x424242);
        canister("coaloil", 0x424242);
        canister("naphtha_crack", 0x5F6D44);
        canister("lightoil_crack", 0xB46B52);
        canister("diesel_crack", 0xFF2C2C);
        canister("petroil_leaded", 0x2331F6);
        canister("gasoline_leaded", 0x2F775A);
        canister("coalgas_leaded", 0x1E155F);
        canister("woodoil", 0xBF7E4F);
        canister("coalcreosote", 0x285A3F);
        canister("seedslurry", 0x7CC35E);
        canister("solvent", 0xE4E3EF);
        canister("heavyoil_vacuum", 0x513F39);
        canister("reformate", 0xD180D6);
        canister("lightoil_vacuum", 0xB46B52);
        canister("xylene", 0xA380D6);
        canister("heatingoil_vacuum", 0x694235);
        canister("diesel_reform", 0xFFC500);
        canister("diesel_crack_reform", 0xFFC500);
        canister("kerosene_reform", 0xFF377D);
        canister("oil_ds", 0x424242);
        canister("crackoil_ds", 0x424242);
        canister("naphtha_ds", 0x5F6D44);
        canister("lightoil_ds", 0xB46B52);

        gastank("deuterium", 0x0000FF, 0xFFFFFF);
        gastank("tritium", 0x000099, 0xE9FFAA);
        gastank("gas", 0xFF4545, 0xFFE97F);
        gastank("petroleum", 0x5E7CFF, 0xFFE97F);
        gastank("biogas", 0xC8FF1F, 0x303030);
        gastank("hydrogen", 0x4286f4, 0xffffff);
        gastank("oxygen", 0x98bdf9, 0xffffff);
        gastank("xenon", 0x8C21FF, 0x303030);
        gastank("helium3", 0xFD631F, 0xffffff);
        gastank("aromatics", 0x68A09A, 0xEDCF27);
        gastank("unsaturateds", 0x628FAE, 0xEDCF27);
        gastank("syngas", 0xFFFFFF, 0x131313);
        gastank("chlorine", 0xBAB572, 0x887B34);
        gastank("sourgas", 0xC9BE0D, 0x303030);
        gastank("reformgas", 0x9392FF, 0xFFB992);
        gastank("phosgene", 0xCFC4A4, 0x361414);
        gastank("mustardgas", 0xBAB572, 0x361414);
        gastank("helium4", 0xFD631F, 0xffff00);
        gastank("flue", 0xFF4545, 0xFFE97F);
    }
}
