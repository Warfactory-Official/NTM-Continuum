package com.hbm.creativetabs;

/**
 * The HBM creative tabs, mirroring the tabs from the 1.12.2 mod (see {@code MainRegistry}). The
 * {@link #id} is used for the registry name and the {@code itemGroup.hbm.<id>} translation key.
 */
public enum HbmTab implements dev.golem.core.registry.TabTarget {

    MATERIALS("materials"),     // partsTab  — ingots, nuggets, wires, machine parts
    CONTROL("control"),         // controlTab — items that belong in machines, fuels, etc.
    TEMPLATES("templates"),     // templateTab — templates, siren tracks
    RESOURCES("resources"),     // resourceTab — ore and mineral blocks
    BLOCKS("blocks"),           // blockTab — construction blocks
    MACHINES("machines"),       // machineTab — machines, structure parts
    BOMBS("bombs"),             // nukeTab — bombs
    MISSILES("missiles"),       // missileTab — missiles, satellites
    WEAPONS("weapons"),         // weaponTab — turrets, weapons, ammo
    CONSUMABLES("consumables"); // consumableTab — drinks, kits, tools

    public final String id;

    HbmTab(String id) {
        this.id = id;
    }

    /** A registration chain's {@code .tab(HbmTab.X)}; an addon lists its items in HBM's tabs the same way. */
    @Override
    public void list(dev.golem.core.registry.RegistryObject<? extends net.minecraft.world.item.Item> item) {
        ModTabs.addTo(this, item);
    }
}
