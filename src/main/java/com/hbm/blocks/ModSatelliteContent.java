package com.hbm.blocks;

import com.hbm.commands.CommandSatellites;
import com.hbm.entity.ModSatelliteEntities;
import com.hbm.inventory.ModSatelliteMenus;
import com.hbm.saveddata.satellites.SatelliteSystems;
import com.hbm.tileentity.ModSatelliteBlockEntities;
import dev.golem.core.command.CompatCommands;
import dev.golem.core.platform.Lifecycle;

/**
 * One bootstrap hook for the satellite family's blocks, block entities, menus, entities and Dejson declarations
 * (the {@code satellite} item and the chips register through {@code ModSatelliteItems}). Also starts the
 * emission-log ageing that 1.7 ran from {@code ModEventHandler}'s world tick.
 */
public final class ModSatelliteContent {

    private ModSatelliteContent() {}

    public static void init() {
        ModSatelliteBlocks.init();
        ModSatelliteBlockEntities.init();
        ModSatelliteMenus.init();
        ModSatelliteEntities.init();
        SatelliteDeclarations.declare();

        Lifecycle.onServerTick(SatelliteSystems::tickServer);
        CompatCommands.server(CommandSatellites::register);
    }
}
