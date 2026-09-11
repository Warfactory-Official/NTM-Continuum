package com.hbm.saveddata.satellites;

import com.hbm.items.machine.ItemSatellite;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/** Reads ore through the ground under its target. */
public class SatelliteScanner extends Satellite {

    public SatelliteScanner() {
        this.ifaceAcs.add(InterfaceActions.HAS_ORES);
        this.satIface = Interfaces.SAT_PANEL;
    }

    @Override public String getType() { return "DEPTH_SCANNER"; }

    @Override
    public Component[] getInfo(Level level) {
        return new Component[]{Component.translatable(ItemSatellite.nameKey(EnumSatType.SCANNER))};
    }

    @Override
    public float[] getColor() {
        return new float[]{0.544F, 0.680F, 1.0F};
    }
}
