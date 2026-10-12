package com.hbm.recipes.data;

import com.hbm.api.data.MachineDataMaps;
import com.hbm.items.machine.ItemICFPellet.EnumICFFuel;
import dev.golem.core.data.DataMapSource;
import dev.golem.core.data.DataMaps.DataMap;

/**
 * 1.7 {@code ItemICFPellet.init}: {@code fluidMap} and {@code materialMap}, two files. 1.7's maps were {@code HashMap}s;
 * the files list their keys by name.
 */
public final class ICFFuelSource {

    private ICFFuelSource() {}

    /** 1.7 {@code fluidMap}, keyed by vanilla fluid id. */
    public static final class Fluid extends DataMapSource<EnumICFFuel> {

        public Fluid() {
            super(MachineDataMaps.ICF_FUEL_FLUID, false);
        }

        @Override
        protected void values() {
            put("hbm:chlorine", EnumICFFuel.CHLORINE);
            put("hbm:deuterium", EnumICFFuel.DEUTERIUM);
            put("hbm:helium3", EnumICFFuel.HELIUM3);
            put("hbm:helium4", EnumICFFuel.HELIUM4);
            put("hbm:hydrogen", EnumICFFuel.HYDROGEN);
            put("hbm:oxygen", EnumICFFuel.OXYGEN);
            put("hbm:tritium", EnumICFFuel.TRITIUM);
        }
    }

    /** 1.7 {@code materialMap}, keyed by HBM material name. */
    public static final class Material extends DataMapSource<EnumICFFuel> {

        public Material() {
            super(materialMap(), false);
        }

        @Override
        protected void values() {
            put("beryllium", EnumICFFuel.BERYLLIUM);
            put("boron", EnumICFFuel.BORON);
            put("calcium", EnumICFFuel.CALCIUM);
            put("graphite", EnumICFFuel.CARBON);
            put("lithium", EnumICFFuel.LITHIUM);
            put("sodium", EnumICFFuel.SODIUM);
        }
    }

    private static DataMap<?, EnumICFFuel> materialMap() {
        return MachineDataMaps.icfMaterialMap();
    }
}
