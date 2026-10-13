package com.hbm.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm.Constants;
import com.hbm.lib.ModDamageSources;
import com.hbm.lib.ModDamageTypes;
import dev.golem.core.Ids;
import dev.golem.core.dejson.Data;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Holds the damage-type table against the two things it has to agree with: the keys the game code
 * actually damages people through, and what the loaded registry says those types are.
 *
 * <p>The declaration and {@link ModDamageSources} are separate classes for a build reason: the
 * generator runs in a plain JVM and cannot load a class full of Minecraft, so the one thing that
 * could go wrong is that they drift. A key with no type throws at the moment of damage; a type with
 * no key is dead weight nobody notices. Both are cheap to check and neither is visible otherwise.
 */
public final class DamageTypeGameTests {

    private static final String TYPES = "data/hbm/damage_type/";
    private static final String TAGS = "data/minecraft/tags/damage_type/";

    private DamageTypeGameTests() {}

    /** Every damage type is declared, keyed, and carries exactly the tags it was declared with. */
    public static void everyDamageTypeIsKeyedAndTagged(GameTestHelper helper) {
        ModDamageTypes.declare();
        List<String> problems = new ArrayList<>();

        Set<String> declared = new TreeSet<>();
        for (String path : Data.files().keySet()) {
            if (path.startsWith(TYPES)) {
                declared.add(path.substring(TYPES.length(), path.length() - ".json".length()));
            }
        }

        Map<String, ResourceKey<DamageType>> keys = keysInUse();
        for (String name : declared) {
            if (!keys.containsKey(name)) {
                problems.add("hbm:" + name + " is declared but no ModDamageSources key names it");
            }
        }
        for (String name : keys.keySet()) {
            if (!declared.contains(name)) {
                problems.add("ModDamageSources names hbm:" + name + ", which is declared nowhere");
            }
        }

        Map<String, Set<String>> expected = new LinkedHashMap<>();
        Set<String> tagPaths = new LinkedHashSet<>();
        Data.files().forEach((path, content) -> {
            if (!path.startsWith(TAGS)) {
                return;
            }
            String tag = path.substring(TAGS.length(), path.length() - ".json".length());
            tagPaths.add(tag);
            for (JsonElement member : content.getAsJsonArray("values")) {
                expected.computeIfAbsent(member.getAsString(), id -> new LinkedHashSet<>()).add(tag);
            }
        });

        int checked = 0;
        for (Map.Entry<String, ResourceKey<DamageType>> entry : keys.entrySet()) {
            if (!declared.contains(entry.getKey())) {
                continue;
            }
            DamageSource source = ModDamageSources.source(helper.getLevel(), entry.getValue());
            Set<String> wanted = expected.getOrDefault("hbm:" + entry.getKey(), Set.of());
            checked++;
            for (String tag : tagPaths) {
                boolean holds = source.is(TagKey.create(Registries.DAMAGE_TYPE, Ids.of("minecraft", tag)));
                if (holds != wanted.contains(tag)) {
                    problems.add("hbm:" + entry.getKey() + (wanted.contains(tag)
                            ? " was declared into minecraft:" + tag + " but is not in it"
                            : " is in minecraft:" + tag + " without being declared into it"));
                }
            }
        }

        helper.assertTrue(problems.isEmpty(), join(problems));
        helper.assertTrue(checked > 0, "no damage type was resolved, so nothing was checked");
        Constants.LOG.info("[DAMAGE] {} declared types, all keyed, and every membership in {} vanilla "
                + "tags is exactly what was declared", checked, tagPaths.size());
        helper.succeed();
    }

    /**
     * The damage types the game actually uses, by path: every {@code ResourceKey} constant on
     * {@link ModDamageSources} plus the one each bullet class carries.
     */
    private static Map<String, ResourceKey<DamageType>> keysInUse() {
        Map<String, ResourceKey<DamageType>> keys = new LinkedHashMap<>();
        for (Field field : ModDamageSources.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != ResourceKey.class) {
                continue;
            }
            try {
                field.setAccessible(true);
                put(keys, (ResourceKey<?>) field.get(null));
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("could not read " + field.getName(), failure);
            }
        }
        for (ModDamageSources.DamageClass damageClass : ModDamageSources.DamageClass.values()) {
            put(keys, damageClass.type());
        }
        return keys;
    }

    @SuppressWarnings("unchecked")
    private static void put(Map<String, ResourceKey<DamageType>> keys, ResourceKey<?> key) {
        if (Constants.MOD_ID.equals(Ids.keyId(key).getNamespace())) {
            keys.put(Ids.keyId(key).getPath(), (ResourceKey<DamageType>) key);
        }
    }

    private static String join(List<String> problems) {
        if (problems.size() <= 8) {
            return String.join("; ", problems);
        }
        return String.join("; ", problems.subList(0, 8))
                + "; and " + (problems.size() - 8) + " more";
    }
}
