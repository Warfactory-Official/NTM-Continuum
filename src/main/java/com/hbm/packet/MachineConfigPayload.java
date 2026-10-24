package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/** S2C, on join: the server's {@code hbmMachines.json} values ({@code MachineDynConfig.values()}), for GUIs and renderers. */
public record MachineConfigPayload(String json) implements GolemPayload {

    private static final int MAX_LENGTH = 1 << 18;

    public static final GolemCodec<MachineConfigPayload> CODEC = GolemCodec.of(MachineConfigPayload.class,
            Ids.of(Constants.MOD_ID, "machine_config"),
            (payload, buffer) -> buffer.writeUtf(payload.json, MAX_LENGTH),
            buffer -> new MachineConfigPayload(buffer.readUtf(MAX_LENGTH)));

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
