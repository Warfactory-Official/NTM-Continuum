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
 * <p>Its handler writes a nonce and the sender's id into a concurrent map and touches nothing else (no level,
 * entity, item or renderer), so nothing can race against it and it needs no scheduled task or tick of latency.
 *
 * <p>Every other payload resolves a block entity, reaches the particle engine or reads the held stack, so it
 * runs on the game thread. In the reference implementation, exactly one of forty-six channels runs off-thread,
 * and that one only logs.
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
