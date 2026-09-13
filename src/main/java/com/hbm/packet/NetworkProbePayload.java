package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.resources.Identifier;

/**
 * A liveness probe, and the mod's one off-thread handler.
 *
 * <p>Its handler is the shape that earns the network thread: it writes a nonce and the sender's id
 * into a concurrent map and touches nothing else. No level, no entity, no item, no renderer — so
 * there is nothing for it to race against, and it costs no scheduled task and no tick of latency.
 *
 * <p>Everything else this mod sends is the opposite shape. A block-entity sync resolves a block
 * entity, a particle payload reaches the particle engine, a gun cue reads the held stack: all of them
 * must run where those are safe to touch, and all of them stay on the game thread. The reference
 * implementation this was taken from reaches the same conclusion — of its forty-six channels, exactly
 * one runs off-thread, and that one only logs.
 */
public record NetworkProbePayload(long nonce, long sentAtMillis) implements GolemPayload {

    public static final GolemCodec<NetworkProbePayload> CODEC = GolemCodec.of(NetworkProbePayload.class,
            Ids.of(Constants.MOD_ID, "network_probe"),
            (payload, buffer) -> {
                buffer.writeLong(payload.nonce);
                buffer.writeLong(payload.sentAtMillis);
            },
            buffer -> new NetworkProbePayload(buffer.readLong(), buffer.readLong()));

    private static final ConcurrentHashMap<Long, UUID> SEEN = new ConcurrentHashMap<>();
    private static final AtomicLong RECEIVED = new AtomicLong();

    @Override
    public Identifier id() {
        return CODEC.id();
    }

    /** Runs on the network thread. Must stay free of anything the game thread owns. */
    public static void received(NetworkProbePayload payload, UUID sender) {
        SEEN.put(payload.nonce(), sender);
        RECEIVED.incrementAndGet();
    }

    public static UUID senderOf(long nonce) {
        return SEEN.get(nonce);
    }

    public static long receivedCount() {
        return RECEIVED.get();
    }

    public static void forget(long nonce) {
        SEEN.remove(nonce);
    }
}
