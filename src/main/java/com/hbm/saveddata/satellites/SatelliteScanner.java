package com.hbm.saveddata.satellites;

import com.hbm.items.machine.ItemSatellite.EnumSatType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import com.mojang.serialization.MapCodec;

/**
 * 1.7 {@code SatelliteScanner}, the depth scanner. It answers only the four targeting commands; its use is the
 * neutrino lens ({@code ItemModLens}), which scans around its wearer while a scanner answers on the lens's
 * frequency.
 */
public class SatelliteScanner extends Satellite {

    public static final MapCodec<SatelliteScanner> CODEC = MapCodec.unit(SatelliteScanner::new);

    public SatelliteScanner() {}

    @Override
    public SatelliteType type() {
        return SatelliteType.SCANNER;
    }

    @Override public String getType() { return "DEPTH_SCANNER"; }

    @Override
    public Component[] getInfo(Level level) {
        return new Component[] { nameOf(EnumSatType.SCANNER) };
    }
}
