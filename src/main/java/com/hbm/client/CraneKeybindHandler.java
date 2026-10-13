package com.hbm.client;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.EnumCraneKey;
import com.hbm.packet.CraneKeyPayload;
import com.mojang.blaze3d.platform.InputConstants;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.KeyMappings;
import dev.golem.core.net.GolemNetwork;
import java.util.Arrays;
import net.minecraft.client.Minecraft;

/** Client-side RBMK crane key registration and edge polling. */
public final class CraneKeybindHandler {
    private static final KeyMappings.Mapping[] KEYS = new KeyMappings.Mapping[EnumCraneKey.VALUES.length];
    private static final boolean[] LAST_STATE = new boolean[EnumCraneKey.VALUES.length];

    private CraneKeybindHandler() {}

    public static void register() {
        KEYS[EnumCraneKey.UP.ordinal()] = KeyMappings.register("hbm", "key.hbm.craneUp", InputConstants.KEY_UP, "hbm");
        KEYS[EnumCraneKey.DOWN.ordinal()] = KeyMappings.register("hbm", "key.hbm.craneDown", InputConstants.KEY_DOWN, "hbm");
        KEYS[EnumCraneKey.LEFT.ordinal()] = KeyMappings.register("hbm", "key.hbm.craneLeft", InputConstants.KEY_LEFT, "hbm");
        KEYS[EnumCraneKey.RIGHT.ordinal()] = KeyMappings.register("hbm", "key.hbm.craneRight", InputConstants.KEY_RIGHT, "hbm");
        KEYS[EnumCraneKey.LOAD.ordinal()] = KeyMappings.register("hbm", "key.hbm.craneLoad", InputConstants.KEY_RETURN, "hbm");
        ClientTicks.onEndTick(CraneKeybindHandler::poll);
    }

    private static void poll() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            Arrays.fill(LAST_STATE, false);
            return;
        }
        var props = HbmPlayerProps.getData(player);
        for (int i = 0; i < KEYS.length; i++) {
            boolean pressed = KEYS[i].isHeld();
            if (pressed == LAST_STATE[i]) continue;
            LAST_STATE[i] = pressed;
            props.setCraneKeyPressed(EnumCraneKey.VALUES[i], pressed);
            GolemNetwork.sendToServer(CraneKeyPayload.CODEC, new CraneKeyPayload(i, pressed));
        }
    }
}
