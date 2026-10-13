package com.hbm.items.weapon;

import com.hbm.packet.CrucibleAnimPayload;
import com.hbm.packet.CrucibleClickPayload;
import com.hbm.packet.MobSlicerPayload;
import com.hbm.packet.SpecialDeathPayload;
import dev.golem.core.net.GolemNetwork;

/**
 * The sword cutters' four channels (1.12.2 {@code PacketMobSlicer}, {@code PacketSpecialDeath}, the {@code Anim}
 * effect of {@code AuxParticlePacketNT} and {@code AuxButtonPacket} id 1000). The client handlers are attached by
 * {@code com.hbm.client.crucible.CrucibleClient}.
 */
public final class CrucibleNetwork {

    private CrucibleNetwork() {}

    public static void init() {
        GolemNetwork.declarePlayToClient(CrucibleAnimPayload.CODEC);
        GolemNetwork.declarePlayToClient(SpecialDeathPayload.CODEC);
        GolemNetwork.registerPlayToServer(MobSlicerPayload.CODEC, MobSlicerPayload::handleServer);
        GolemNetwork.registerPlayToServer(CrucibleClickPayload.CODEC, CrucibleClickPayload::handleServer);
    }
}
