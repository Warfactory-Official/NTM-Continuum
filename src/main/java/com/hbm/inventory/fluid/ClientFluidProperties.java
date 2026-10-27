package com.hbm.inventory.fluid;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.golem.core.Ids;
import dev.golem.core.net.ClientSessions;
import dev.golem.core.net.Configurations;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;

/** Client metadata belongs to an actual connection; canonical FluidType fields remain server-owned. */
public final class ClientFluidProperties {
    private record Resolved(Map<FluidType, FluidMetadata> metadata, Map<FluidType, FluidType> kinds,
                            Map<Fluid, FluidProperties.Properties> owned) {}
    private record State(ClientSessions.Session session, Resolved resolved,
                         Map<String, String> baseFiles, Map<String, String> patchFiles) {}

    private static volatile Resolved local;
    private static volatile State state;

    private ClientFluidProperties() {}

    /** Called after common fluid registration and before the first client resource load or connection. */
    public static synchronized void init() {
        if (local != null) throw new IllegalStateException("Client fluid metadata initialized twice");
        local = resolve(FluidProperties.declarationSnapshot());
        ClientSessions.START.register(ClientFluidProperties::start);
        ClientSessions.END.register(ClientFluidProperties::reset);
        Configurations.registerClient(FluidPropertySnapshot.CODEC, ClientFluidProperties::accept);
    }

    private static synchronized void start(ClientSessions.Session session) {
        state = new State(session, null, Map.of(), Map.of());
    }

    /** Decodes beside the live state, then replaces it atomically before configuration acknowledgement. */
    public static void accept(FluidPropertySnapshot snapshot, ClientSessions.Session session) {
        if (!session.active()) return;
        var resolved = resolve(snapshot);
        synchronized (ClientFluidProperties.class) {
            var current = state;
            if (!session.active() || current == null || current.session() != session) return;
            state = new State(session, resolved, Map.of(), Map.of());
        }
    }

    /** A stale connection's disconnect cannot clear a newer connection's metadata. */
    public static synchronized void reset(ClientSessions.Session session) {
        var current = state;
        if (current != null && current.session() == session) state = null;
    }

    /** Raw play folder sync is staged until the following full resolved payload commits both folders together. */
    public static synchronized void stageBase(Map<String, JsonElement> files, ClientSessions.Session session) { stage(files, session, false); }
    public static synchronized void stagePatches(Map<String, JsonElement> files, ClientSessions.Session session) { stage(files, session, true); }

    private static void stage(Map<String, JsonElement> files, ClientSessions.Session session, boolean patches) {
        var current = state;
        if (current == null || current.session() != session || !session.active()) return;
        var copy = new HashMap<String, String>();
        files.forEach((name, value) -> copy.put(name, value.toString()));
        state = patches ? new State(current.session(), current.resolved(), current.baseFiles(), Map.copyOf(copy))
                : new State(current.session(), current.resolved(), Map.copyOf(copy), current.patchFiles());
    }

    public static Map<Fluid, FluidProperties.Properties> snapshot() {
        var current = state;
        return current == null || !current.session().active() || current.resolved() == null
                ? Map.of() : current.resolved().owned();
    }

    /** Explicit client access; an active connection must install its required metadata before world rendering. */
    public static FluidMetadata get(FluidType type) {
        var current = state;
        var resolved = current != null && current.session().active() ? current.resolved() : local;
        if (resolved == null) throw new IllegalStateException("Client fluid metadata is not installed");
        var metadata = resolved.metadata().get(type);
        if (metadata == null) throw new IllegalStateException("Client lacks fluid metadata for " + type.getRegistryName());
        return metadata;
    }

    public static FluidType kindOf(FluidType type) {
        var current = state;
        var resolved = current != null && current.session().active() ? current.resolved() : local;
        if (resolved == null) throw new IllegalStateException("Client fluid metadata is not installed");
        var kind = resolved.kinds().get(type);
        if (kind == null) throw new IllegalStateException("Client lacks fluid kind for " + type.getRegistryName());
        return kind;
    }

    private static Resolved resolve(FluidPropertySnapshot snapshot) {
        var metadata = new HashMap<FluidType, FluidMetadata>();
        var owned = new HashMap<Fluid, FluidProperties.Properties>();
        var kinds = new HashMap<FluidType, FluidType>();
        for (var row : snapshot.entries().entrySet()) {
            var id = Ids.parse(row.getKey());
            if (!BuiltInRegistries.FLUID.containsKey(id)) throw new IllegalArgumentException("Unknown fluid " + id);
            var fluid = BuiltInRegistries.FLUID.getOptional(id).orElseThrow();
            var type = Fluids.fromVanillaExact(fluid);
            var kindId = Ids.parse(snapshot.kinds().get(row.getKey()));
            if (!BuiltInRegistries.FLUID.containsKey(kindId)) throw new IllegalArgumentException("Unknown fluid kind " + kindId);
            var kind = Fluids.fromVanillaExact(BuiltInRegistries.FLUID.getOptional(kindId).orElseThrow());
            var entry = row.getValue();
            var properties = FluidProperties.read(JsonParser.parseString(entry.properties()).getAsJsonObject());
            var own = entry.own();
            var naming = own ? type : kind;
            var defined = own || snapshot.entries().get(snapshot.kinds().get(row.getKey())).own();
            if (metadata.put(type, new FluidMetadata(properties, entry.containers(), naming, defined)) != null) {
                throw new IllegalArgumentException("Duplicate exact fluid " + row.getKey());
            }
            kinds.put(type, kind);
            if (own) owned.put(fluid, properties);
        }
        return new Resolved(Map.copyOf(metadata), Map.copyOf(kinds), Map.copyOf(owned));
    }
}
