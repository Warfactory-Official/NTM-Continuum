package com.hbm;

import com.hbm.hazard.Hazards;
import com.hbm.packet.HbmPackets;
import dev.golem.core.platform.ModEntry;

/**
 * HBM's common start-up, in Golem Core's three phases (see {@link dev.golem.core.platform.ModEntries}): the same on
 * every loader, and the same path an addon's own entry takes. NeoForge and Forge start it from their {@code @Mod}
 * constructor; Fabric's {@code FabricInit} is the {@code golemcore:entry} and delegates here.
 *
 * <p><b>Order an addon can rely on</b> (ADDON-API-INVENTORY S-01, S-02): an addon's own {@code ModEntry} runs each
 * phase after HBM's, because Golem's {@code ModEntries} runs entries in dependency order (Fabric: the addon lists
 * {@code hbm} under {@code depends}; NeoForge/Forge: its {@code mods.toml} dependency on {@code hbm} has
 * {@code ordering = "AFTER"}, so its constructor, which starts the entry, follows HBM's). So in the addon's
 * {@code declare()} HBM's fluids ({@code Fluids.register(ns, ...)}), materials ({@code Mats.register}), bullet configs
 * ({@code BulletConfigs.register(Identifier, ...)}) and registers already exist, and in its {@code registered()} HBM's
 * hazards, armor protection and launch table are filled: an addon entry added there extends or overrides them.
 */
public class HbmEntry implements ModEntry {

    @Override
    public void declare() {
        CommonClass.init();
        HbmPackets.declare();
        com.hbm.inventory.recipes.HbmRecipeTables.declare();
        com.hbm.api.data.HbmDataMaps.declare();
        com.hbm.itempool.ItemPool.declare();
        com.hbm.inventory.fluid.FluidProperties.declare(); // fluid_property + fluid_property_patch
        com.hbm.api.script.HbmScripts.declare(); // the HBM script global and HBMEvents (inert without KubeJS)
    }

    @Override
    public void registered() {
        // Item and material hazards name registered items.
        Hazards.init();
        // the missile item -> entity table, its own start-up step (it used to hide inside Hazards.init)
        com.hbm.tileentity.bomb.TileEntityLaunchPadBase.registerLaunchables();
        // materials requested by startup scripts (HBMEvents.registry), once every register is filled
        com.hbm.api.script.HbmScripts.registered();
    }
}
