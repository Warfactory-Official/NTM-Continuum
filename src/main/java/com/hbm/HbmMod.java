package com.hbm;

import com.hbm.creativetabs.ModTabs;
import com.hbm.gametest.HbmGameTests;
import com.hbm.hazard.Hazards;
import com.hbm.items.ModFuelItems;
import com.hbm.packet.HbmPackets;
import com.hbm.api.content.NtmPhases;
import dev.golem.core.platform.GolemMod;
import dev.golem.core.registry.Fuels;

/** HBM on Golem Core's bootstrap; {@code platform/*} keeps loader-only event subscriptions. */
public final class HbmMod implements GolemMod {

    @Override
    public void init() {
        NtmPhases.init();
        HbmGameTests.init();
        CommonClass.init();
        HbmPackets.declare();
        ModTabs.install();
    }

    @Override
    public void setup() {
        Fuels.register(ModFuelItems.BURN_TIMES);
        Hazards.init();
    }

    @Override
    public void initClient() {
        com.hbm.client.HbmClient.init();
    }
}
