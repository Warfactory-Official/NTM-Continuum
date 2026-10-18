package com.hbm.items.weapon.sedna.factory;

/** 1.7 {@code GunValues}: puts every gun, attachment and turret damage into the server config. */
public final class GunValues {

    private GunValues() {}

    public static void initDefaults() {
        XFactory10ga.initConfig();
        XFactory12ga.initConfig();
        XFactory22lr.initConfig();
        XFactory357.initConfig();
        XFactory35800.initConfig();
        XFactory40mm.initConfig();
        XFactory44.initConfig();
        XFactory50.initConfig();
        XFactory556mm.initConfig();
        XFactory75Bolt.initConfig();
        XFactory762mm.initConfig();
        XFactory9mm.initConfig();
        XFactoryAccelerator.initConfig();
        XFactoryBlackPowder.initConfig();
        XFactoryCatapult.initConfig();
        XFactoryDrill.initConfig();
        XFactoryEnergy.initConfig();
        XFactoryFlamer.initConfig();
        XFactoryPA.initConfig();
        XFactoryRocket.initConfig();
        XFactoryTool.initConfig();
        XFactoryTurret.initConfig();
    }
}
