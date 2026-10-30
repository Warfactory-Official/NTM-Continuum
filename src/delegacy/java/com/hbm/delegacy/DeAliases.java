package com.hbm.delegacy;

import com.hbm.Constants;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Registry aliases for NTM: Decentralized Edition's one-to-one renames, so a DE stack the importer could not reach
 * (an AE2 drive, a backpack, a quest reward) loads as its Continuum item. Splits and same-id collisions need the
 * converter and are not aliased. A registered DE addon's {@code aliases} table is applied too.
 */
@EventBusSubscriber(modid = Constants.MOD_ID)
public final class DeAliases {
    private record Alias(String registry, ResourceLocation from, ResourceLocation to) {}

    private static final List<Alias> ALIASES = load(DeAliases.class.getResourceAsStream("/hbm_delegacy/aliases.tsv"));
    private static @Nullable List<Alias> withAddons;

    private DeAliases() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        String registry = event.getRegistryKey().location().getPath();
        Registry<?> target = event.getRegistry();
        if (target == null) return;
        if (withAddons == null) {
            withAddons = new ArrayList<>(ALIASES);
            for (DeAddons.Addon addon : DeAddons.all()) {
                InputStream in = addon.table("aliases");
                if (in != null) withAddons.addAll(load(in));
            }
        }
        for (Alias alias : withAddons) if (alias.registry().equals(registry)) target.addAlias(alias.from(), alias.to());
    }

    private static List<Alias> load(@Nullable InputStream stream) {
        List<Alias> out = new ArrayList<>();
        try (InputStream in = stream) {
            if (in == null) throw new IllegalStateException("missing aliases table");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = reader.readLine()) != null; ) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] cols = line.split("\t");
                out.add(new Alias(cols[0], ResourceLocation.parse(cols[1]), ResourceLocation.parse(cols[2])));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out;
    }
}
