package com.hbm.creativetabs;

public enum HbmTab {

    MATERIALS("materials"),
    CONTROL("control"),
    TEMPLATES("templates"),
    RESOURCES("resources"),
    BLOCKS("blocks"),
    MACHINES("machines"),
    BOMBS("bombs"),
    MISSILES("missiles"),
    WEAPONS("weapons"),
    CONSUMABLES("consumables");

    public final String id;

    HbmTab(String id) {
        this.id = id;
    }
}
