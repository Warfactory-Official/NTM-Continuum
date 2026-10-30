package com.hbm.delegacy.legacy;

import com.hbm.Constants;
import com.hbm.delegacy.DeAddons;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * DE ids Continuum lacks, registered last so a DE stack in another mod's storage still decodes: its split items as
 * {@link LegacyItem}s and its item components as raw NBT. Such a stack converts the first time the game touches it.
 */
@EventBusSubscriber(modid = Constants.MOD_ID)
public final class DeLegacy {
    private static final Codec<Tag> RAW = Codec.PASSTHROUGH.xmap(d -> d.convert(NbtOps.INSTANCE).getValue(),
            t -> new Dynamic<>(NbtOps.INSTANCE, t));
    private static final Set<DataComponentType<?>> RAW_TYPES = new HashSet<>();

    private DeLegacy() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void register(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.DATA_COMPONENT_TYPE)) {
            for (String[] row : rows("components")) {
                ResourceLocation id = ResourceLocation.parse(row[0]);
                if (BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(id)) continue;
                DataComponentType<Tag> type = DataComponentType.<Tag>builder().persistent(RAW)
                        .networkSynchronized(ByteBufCodecs.TAG).build();
                RAW_TYPES.add(type);
                event.register(Registries.DATA_COMPONENT_TYPE, id, () -> type);
            }
        } else if (event.getRegistryKey().equals(Registries.LOOT_NUMBER_PROVIDER_TYPE)) {
            event.register(Registries.LOOT_NUMBER_PROVIDER_TYPE, ResourceLocation.fromNamespaceAndPath("hbm_de", "config_scaled"),
                    () -> ScaledRolls.TYPE);
        } else if (event.getRegistryKey().equals(Registries.ITEM)) {
            for (String[] row : rows("legacy_items")) {
                ResourceLocation id = ResourceLocation.parse(row[0]);
                if (BuiltInRegistries.ITEM.containsKey(id)) continue;
                ResourceLocation target = ResourceLocation.parse(row[1]);
                event.register(Registries.ITEM, id, () -> new LegacyItem(target, new Item.Properties()));
            }
        }
    }

    /** True for a component type that only holds DE's raw value until its stack converts. */
    public static boolean isRaw(DataComponentType<?> type) {
        return RAW_TYPES.contains(type);
    }

    public static boolean isRaw(String id) {
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse(id));
        return type != null && RAW_TYPES.contains(type);
    }

    /** DE's rows and every registered addon's, whose tables name its own DE ids. */
    private static List<String[]> rows(String table) {
        List<String[]> out = new ArrayList<>();
        try (InputStream in = DeLegacy.class.getResourceAsStream("/hbm_delegacy/de7/" + table + ".tsv")) {
            if (in == null) throw new IllegalStateException("missing hbm_delegacy/de7/" + table + ".tsv");
            read(in, out);
            for (DeAddons.Addon addon : DeAddons.all()) {
                try (InputStream rows = addon.table(table)) {
                    if (rows != null) read(rows, out);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out;
    }

    private static void read(InputStream in, List<String[]> out) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        for (String line; (line = reader.readLine()) != null; ) {
            if (!line.isEmpty() && !line.startsWith("#")) out.add(line.split("\t"));
        }
    }
}
