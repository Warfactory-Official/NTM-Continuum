package com.hbm.api.tab;

/**
 * The HBM creative tabs, mirroring the tabs from the 1.12.2 mod (see {@code MainRegistry}). The
 * {@link #id} is used for the registry name and the {@code itemGroup.hbm.<id>} translation key.
 */
public enum HbmTab {

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
}
