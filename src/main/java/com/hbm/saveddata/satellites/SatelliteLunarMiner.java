package com.hbm.saveddata.satellites;

import com.hbm.items.machine.ItemSatellite;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/** The same miner pointed at the moon, which changes only what it brings back. */
public class SatelliteLunarMiner extends SatelliteMiner {

    @Override public String getType() { return "LUNAR_MINER"; }

    @Override
    public Component[] getInfo(Level level) {
        return new Component[]{Component.translatable(ItemSatellite.nameKey(EnumSatType.MINER_LUNAR))};
    }
}
