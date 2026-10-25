package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import java.util.List;
import net.minecraft.resources.Identifier;

/** S2C: the Astolfized players in the receiver's level, 1.7's {@code PermaSyncHandler.boykissers}, sent on change. */
public record ManlySyncPayload(List<Integer> ids) implements GolemPayload {

    public static final GolemCodec<ManlySyncPayload> CODEC = GolemCodec.of(ManlySyncPayload.class,
            Ids.of(Constants.MOD_ID, "manly_sync"),
            (payload, buffer) -> {
                buffer.writeShort(payload.ids.size());
                for (int id : payload.ids) buffer.writeInt(id);
            },
            buffer -> {
                Integer[] ids = new Integer[buffer.readShort()];
                for (int i = 0; i < ids.length; i++) ids[i] = buffer.readInt();
                return new ManlySyncPayload(List.of(ids));
            });

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
