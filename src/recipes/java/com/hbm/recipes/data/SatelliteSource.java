package com.hbm.recipes.data;

import com.hbm.api.data.SatelliteDataMaps;
import dev.golem.core.data.DataMapSource;

/**
 * 1.7 {@code XSatelliteRegistry.registerSatellite}: the {@code satellite} chips (one item per {@code EnumSatType}, in the
 * enum's order) and {@code sat_gerald}. The legacy chips are left out: 1.7 refused them (their class was already mapped).
 */
public final class SatelliteSource extends DataMapSource<String> {

    public SatelliteSource() {
        super(SatelliteDataMaps.SATELLITE, false);
    }

    @Override
    protected void values() {
        put("hbm:satellite_spy", "mapper");
        put("hbm:satellite_scanner", "scanner");
        put("hbm:satellite_radar", "radar");
        put("hbm:satellite_miner_astro", "miner");
        put("hbm:satellite_miner_lunar", "lunar_miner");
        put("hbm:satellite_precision_laser", "precision_laser");
        put("hbm:satellite_death_ray", "laser");
        put("hbm:satellite_xenium_resonator", "resonator");
        put("hbm:satellite_relay", "relay");
        put("hbm:satellite_detector", "detector");
        put("hbm:satellite_ray_scan", "ray_scan");
        put("hbm:sat_gerald", "horizons");
    }
}
