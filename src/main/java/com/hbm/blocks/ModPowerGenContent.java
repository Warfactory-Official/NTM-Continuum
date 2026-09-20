package com.hbm.blocks;

import dev.golem.core.net.GolemNetwork;
import com.hbm.inventory.ModPowerGenMenus;
import com.hbm.items.ModPowerGenItems;
import com.hbm.packet.SirenPayload;
import com.hbm.sound.ModPowerGenSounds;
import com.hbm.tileentity.ModPowerGenBlockEntities;

/**
 * One common bootstrap hook for the large power-generation family: the Leviathan steam turbine
 * ({@code machine_chungus}), the LPW-2 ({@code machine_lpw2}), the geothermal heat exchanger
 * ({@code machine_hephaestus}), the industrial generator memorial ({@code machine_industrial_generator}),
 * the siren ({@code machine_siren}) and its flattened cassette family ({@code siren_track_*}).
 */
public final class ModPowerGenContent {
    private ModPowerGenContent() {}

    public static void init() {
        ModPowerGenBlocks.init();
        ModPowerGenItems.init();
        ModPowerGenBlockEntities.init();
        ModPowerGenMenus.init();
        ModPowerGenSounds.init();
        // 1.7 TESirenPacket: the siren tells every client within 1500 blocks what to play.
        GolemNetwork.declarePlayToClient(SirenPayload.CODEC);
        PowerGenDeclarations.declare();
    }
}
