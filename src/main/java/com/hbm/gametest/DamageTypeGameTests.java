package com.hbm.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.golem.core.world.ItemStacks;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Mob;
import com.hbm.Constants;
import com.hbm.capability.HbmLivingProps;
import com.hbm.handler.EntityEffectHandler;
import com.hbm.lib.ModDamageSources;
import com.hbm.lib.ModDamageTypes;
import dev.golem.core.Ids;
import dev.golem.core.dejson.Data;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

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
     * 1.7 radiation is only setDamageBypassesArmor, so Resistance X (the AWESOME syringe) cancels the 1000 damage at
     * 1000 RAD before the RAD is cleared; a black hole is setDamageIsAbsolute, which skips potions and enchantments.
     * Neither sets setDamageAllowedInCreativeMode, so a totem of undying fires for both.
     */
    public static void absoluteDamageFollows17(GameTestHelper helper) {
        Player bare = survivalPlayer(helper);
        HbmLivingProps.setRadiation(bare, 1000);
        EntityEffectHandler.onUpdate(bare);
        helper.assertTrue(bare.getHealth() <= 0, "a player at 1000 RAD dies, health " + bare.getHealth());
        helper.assertTrue(HbmLivingProps.getRadiation(bare) == 0, "the RAD is cleared after the hit");

        Player awesome = survivalPlayer(helper);
        Effects.add(awesome, Effects.resistance(), 50 * 20, 9);
        HbmLivingProps.setRadiation(awesome, 1000);
        EntityEffectHandler.onUpdate(awesome);
        helper.assertTrue(awesome.getHealth() == awesome.getMaxHealth(),
                "Resistance X cancels radiation as in 1.7, health " + awesome.getHealth());
        helper.assertTrue(HbmLivingProps.getRadiation(awesome) == 0, "the RAD is cleared even when the hit did nothing");

        Player totem = survivalPlayer(helper);
        totem.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        HbmLivingProps.setRadiation(totem, 1000);
        EntityEffectHandler.onUpdate(totem);
        helper.assertTrue(totem.getHealth() > 0 && totem.getMainHandItem().isEmpty(),
                "radiation does not bypass invulnerability, so the totem is used");

        LivingEntity pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 2, 1));
        Effects.add(pig, Effects.resistance(), 50 * 20, 9);
        HbmLivingProps.setRadiation(pig, 1000);
        EntityEffectHandler.onUpdate(pig);
        helper.assertTrue(pig.getHealth() <= 0, "1.7 kills a mob at 1000 RAD whatever the hit did");

        Player hole = survivalPlayer(helper);
        Effects.add(hole, Effects.resistance(), 50 * 20, 9);
        Compat.hurt(hole, helper.getLevel(), ModDamageSources.blackhole(helper.getLevel()), 1000F);
        helper.assertTrue(hole.getHealth() <= 0, "black hole damage is absolute and ignores Resistance X");

        Player lunar = survivalPlayer(helper);
        Effects.add(lunar, Effects.resistance(), 50 * 20, 9);
        Compat.hurt(lunar, helper.getLevel(), ModDamageSources.lunar(helper.getLevel()), 1000F);
        helper.assertTrue(lunar.getHealth() <= 0, "lunar damage is absolute and ignores Resistance X");
        helper.succeed();
    }

    /** 1.7 knocked back only when an entity dealt the damage, so radiation and asbestos leave the victim standing. */
    public static void sourcelessDamageDoesNotKnockBack(GameTestHelper helper) {
        LivingEntity pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 2, 1));
        for (DamageSource source : List.of(ModDamageSources.radiation(helper.getLevel()),
                ModDamageSources.asbestos(helper.getLevel()), ModDamageSources.digamma(helper.getLevel()))) {
            pig.setHealth(pig.getMaxHealth());
            pig.invulnerableTime = 0;
            pig.setDeltaMovement(Vec3.ZERO);
            Compat.hurt(pig, helper.getLevel(), source, 1F);
            helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), source.getMsgId() + " did no damage");
            helper.assertTrue(pig.getDeltaMovement().horizontalDistanceSqr() == 0,
                    source.getMsgId() + " knocked the pig back: " + pig.getDeltaMovement());
        }
        helper.succeed();
    }

    /** HBM types whose 1.7 message names the attacker: the sedna classes and 1.7's indirect sources. */
    private static final Set<String> NAMES_ATTACKER = Set.of("bullet_physical", "bullet_fire", "bullet_explosive",
            "bullet_electric", "bullet_plasma", "bullet_laser", "bullet_microwave", "bullet_subatomic", "bullet_other",
            "bullet_piercing", "revolver_bullet", "emplacer", "tau", "combine", "subatomic_1", "subatomic_2",
            "subatomic_3", "subatomic_4", "subatomic_5", "euthanized", "electrified", "flamethrower", "plasma", "ice",
            "laser", "boil", "acid_player");
    /** Types 1.7 gave a {@code .item} message naming the attacker's weapon. */
    private static final Set<String> NAMES_ITEM = Set.of("flamethrower", "laser", "revolver_bullet");

    /** Every HBM death message names a translated key, and the attacker and weapon wherever 1.7's did. */
    public static void deathMessagesAreTranslated(GameTestHelper helper) {
        JsonObject lang;
        try (InputStream in = DamageTypeGameTests.class.getResourceAsStream("/assets/hbm/lang/en_us.json")) {
            helper.assertTrue(in != null, "en_us.json is in the jar");
            lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException failure) {
            throw new IllegalStateException("en_us.json", failure);
        }
        LivingEntity victim = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 2, 1));
        Mob armed = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 2, 1));
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        ItemStacks.setCustomName(sword, Component.literal("Named"));
        armed.setItemInHand(InteractionHand.MAIN_HAND, sword);
        armed.setCustomName(Component.literal("Armed"));
        Mob bare = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 2, 3));
        bare.setCustomName(Component.literal("Bare"));
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, ResourceKey<DamageType>> entry : keysInUse().entrySet()) {
            String name = entry.getKey();
            DamageSource sourceless = ModDamageSources.source(helper.getLevel(), entry.getValue());
            victim.setLastHurtByMob(null);
            check(lang, name + " alone", sourceless.getLocalizedDeathMessage(victim), problems);
            for (Mob attacker : List.of(armed, bare)) {
                String label = name + (attacker == armed ? " by a named sword" : " by a bare attacker");
                Component message = ModDamageSources.source(helper.getLevel(), entry.getValue(), attacker, attacker)
                        .getLocalizedDeathMessage(victim);
                String template = check(lang, label, message, problems);
                if (template == null || !NAMES_ATTACKER.contains(name)) continue;
                Object[] args = ((TranslatableContents) message.getContents()).getArgs();
                boolean named = args.length > 1 && args[1] instanceof Component arg
                        && arg.getString().equals(attacker.getCustomName().getString());
                if (!template.contains("%2$s") || !named) {
                    problems.add(label + " does not name the attacker: " + template);
                }
                if (attacker == armed && NAMES_ITEM.contains(name) && !template.contains("%3$s")) {
                    problems.add(label + " does not name the weapon: " + template);
                }
            }
            // vanilla's player message: 1.7's boltgun source always carries its player
            if ("player".equals(sourceless.getMsgId())) continue;
            victim.setLastHurtByMob(armed);
            check(lang, name + " with a kill credit", sourceless.getLocalizedDeathMessage(victim), problems);
        }
        helper.assertTrue(problems.isEmpty(), "death messages: " + join(problems));
        helper.succeed();
    }

    private static String check(JsonObject lang, String label, Component message, List<String> problems) {
        if (!(message.getContents() instanceof TranslatableContents contents)) return null;
        String key = contents.getKey();
        if (lang.has(key)) return lang.get(key).getAsString();
        if (Language.getInstance().has(key)) return Language.getInstance().getOrDefault(key);
        problems.add(label + " gives " + key);
        return null;
    }

    private static Player survivalPlayer(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        return player;
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
