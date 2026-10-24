package com.hbm.inventory.fluid;

import com.hbm.Constants;
import com.hbm.api.data.FluidDataMaps;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}

/** Resolved server metadata, independent of mutable fluid objects and registry numeric ids. */
public record FluidPropertySnapshot(Map<String, Entry> entries, Map<String, String> kinds) implements GolemPayload {

    public record Entry(String properties, FluidDataMaps.@org.jspecify.annotations.Nullable Containers containers,
                        boolean own) {}

    public FluidPropertySnapshot {
        entries = Map.copyOf(entries);
        kinds = Map.copyOf(kinds);
        if (!entries.keySet().equals(kinds.keySet()) || !entries.keySet().containsAll(kinds.values())) {
            throw new IllegalArgumentException("Incomplete resolved fluid snapshot");
        }
    }

    public static final GolemCodec<FluidPropertySnapshot> CODEC = GolemCodec.of(FluidPropertySnapshot.class,
            Ids.of(Constants.MOD_ID, "fluid_properties"), FluidPropertySnapshot::encode, FluidPropertySnapshot::decode);

    @Override
    //? if >=26.1 {
    public Identifier id() {
    //?} else {
    /*public ResourceLocation id() {
    *///?}
        return CODEC.id();
    }

    private void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entries.size());
        for (var row : entries.entrySet()) {
            buffer.writeUtf(row.getKey());
            buffer.writeUtf(row.getValue().properties(), 262144);
            buffer.writeBoolean(row.getValue().own());
            var containers = row.getValue().containers();
            buffer.writeBoolean(containers != null);
            if (containers != null) {
                writeColor(buffer, containers.canister());
                writeColor(buffer, containers.gasBottle());
                writeColor(buffer, containers.gasLabel());
            }
            buffer.writeUtf(kinds.get(row.getKey()));
        }
    }

    private static FluidPropertySnapshot decode(FriendlyByteBuf buffer) {
        var entries = new LinkedHashMap<String, Entry>();
        var kinds = new LinkedHashMap<String, String>();
        var size = buffer.readVarInt();
        if (size < 0 || size > 65536) throw new IllegalArgumentException("Invalid fluid snapshot size " + size);
        for (var i = 0; i < size; i++) {
            var id = buffer.readUtf();
            var properties = buffer.readUtf(262144);
            var own = buffer.readBoolean();
            var containers = buffer.readBoolean()
                    ? new FluidDataMaps.Containers(readColor(buffer), readColor(buffer), readColor(buffer)) : null;
            if (entries.put(id, new Entry(properties, containers, own)) != null) {
                throw new IllegalArgumentException("Duplicate fluid metadata " + id);
            }
            kinds.put(id, buffer.readUtf());
        }
        return new FluidPropertySnapshot(entries, kinds);
    }

    private static void writeColor(FriendlyByteBuf buffer, Optional<Integer> color) {
        buffer.writeBoolean(color.isPresent());
        if (color.isPresent()) buffer.writeInt(color.get());
    }

    private static Optional<Integer> readColor(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? Optional.of(buffer.readInt()) : Optional.empty();
    }
}
