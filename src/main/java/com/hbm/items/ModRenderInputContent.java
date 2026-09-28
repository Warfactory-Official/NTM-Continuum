package com.hbm.items;

import com.hbm.handler.EquipReceiverHandler;
import com.hbm.packet.ToolAnimationPayload;
import dev.golem.core.net.GolemNetwork;

/**
 * G-C (rendering and input gaps): the common halves - the tool animation cue ({@code HbmAnimationPacket} for
 * {@link IAnimatedItem}s) and the held-item equip edge ({@link IEquipReceiver}).
 */
public final class ModRenderInputContent {

    private ModRenderInputContent() {}

    public static void init() {
        GolemNetwork.declarePlayToClient(ToolAnimationPayload.CODEC);
        EquipReceiverHandler.init();
    }
}
