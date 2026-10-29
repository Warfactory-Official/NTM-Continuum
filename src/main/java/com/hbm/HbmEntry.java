package com.hbm;

import com.hbm.api.data.HbmDataMaps;
import com.hbm.api.script.HbmScripts;
import com.hbm.config.MachineDynConfig;
import com.hbm.handler.ExplosivesEntityHooks;
import com.hbm.hazard.Hazards;
import com.hbm.inventory.fluid.FluidProperties;
import com.hbm.inventory.recipes.HbmRecipeTables;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.PoolFill;
import com.hbm.packet.HbmPackets;
import com.hbm.tileentity.bomb.TileEntityLaunchPadBase;
import dev.golem.core.platform.Lifecycle;
import dev.golem.core.platform.ModEntry;
import dev.golem.core.world.EntityLoot;
import dev.golem.core.world.EntityTicks;

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
        HbmRecipeTables.declare();
        HbmDataMaps.declare();
        ItemPool.declare();
        PoolFill.register();
        EntityLoot.register(); // golemcore:looting, read by the entity loot tables
        FluidProperties.declare(); // fluid_property + fluid_property_patch
        HbmScripts.declare(); // the HBM script global and HBMEvents (inert without KubeJS)
    }

    @Override
    public void registered() {
        EntityTicks.AFTER.register(ExplosivesEntityHooks::tick);
        // after every mod's declare(), so an addon's MachineDynConfig.register is in the file too
        MachineDynConfig.initialize();
        Lifecycle.onServerStarting(server -> MachineDynConfig.restoreLocal());
        // Item and material hazards name registered items.
        Hazards.init();
        // the missile item -> entity table, its own start-up step (separate from Hazards.init)
        TileEntityLaunchPadBase.registerLaunchables();
        // materials requested by startup scripts (HBMEvents.registry), once every register is filled
        HbmScripts.registered();
    }
}
