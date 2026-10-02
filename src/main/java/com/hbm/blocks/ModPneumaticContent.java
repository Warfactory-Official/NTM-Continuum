package com.hbm.blocks;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import com.hbm.inventory.ModPneumaticMenus;
import com.hbm.packet.PneumoAccessInputPayload;
import com.hbm.packet.PneumoAccessSyncPayload;
import com.hbm.tileentity.ModPneumaticBlockEntities;
import com.hbm.uninos.networkproviders.PneumaticNetwork;

/** One common bootstrap hook for the pneumatic tube storage system's feature-local registries. */
public final class ModPneumaticContent {
    private ModPneumaticContent() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Constants.MOD_ID);
    private static final Reg REG = Reg.of(SOUNDS);

    /** 1.7 {@code hbm:weapon.reload.tubeFwoomp}, the compressor's send sound; the event is already in sounds.json. */
    public static final RegistryObject<SoundEvent> TUBE_FWOOMP = REG.sound("weapon.reload.tube_fwoomp");

    public static void init() {
        ModPneumaticBlocks.init();
        ModPneumaticBlockEntities.init();
        ModPneumaticMenus.init();
        PneumaticNetwork.init();
        GolemNetwork.declarePlayToClient(PneumoAccessSyncPayload.CODEC);
        GolemNetwork.registerPlayToServer(PneumoAccessInputPayload.CODEC, PneumoAccessInputPayload::handleServer);
    }
}
