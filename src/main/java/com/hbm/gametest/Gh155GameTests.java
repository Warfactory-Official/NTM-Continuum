package com.hbm.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.Constants;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.sound.ModGunSounds;
import com.hbm.sound.ModSounds;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * GitHub issue 155: every Sedna gun's fire sound against 1.7's {@code .sound(...)} call, and the machine sounds found
 * pointing at other files. The sound is compared by the
 * MD5 of the .ogg files its event resolves to in the shipped {@code sounds.json}, so a key that points at the wrong file
 * fails as well as a wrong key.
 */
public final class Gh155GameTests {

    private Gh155GameTests() {}

    /** Gun id, then per receiver in config order: the 1.7 files' MD5 prefixes joined by '|', volume, pitch. */
    private static final String[] FIRE_17 = {
            "gun_double_barrel 60e6be933d45/1/0.9",
            "gun_double_barrel_sacred_dragon 60e6be933d45/1/0.9",
            "gun_autoshotgun_heretic 559da381ff06/1/1",
            "gun_maresleg 60e6be933d45/1/1",
            "gun_maresleg_akimbo 60e6be933d45/1/1 60e6be933d45/1/1",
            "gun_maresleg_broken 60e6be933d45/1/1",
            "gun_liberator f542b10fa701/1/1",
            "gun_spas12 926c07550bc0/1/1",
            "gun_autoshotgun 559da381ff06/1/1",
            "gun_autoshotgun_shredder 559da381ff06/1/1",
            "gun_autoshotgun_sexy 559da381ff06/1/1",
            "gun_am180 566fe5c9e0d7/1/1",
            "gun_star_f ef9709483625/1/1",
            "gun_star_f_akimbo ef9709483625/1/1 ef9709483625/1/1",
            "gun_light_revolver c8a6bc9d9748/1/1",
            "gun_light_revolver_atlas c8a6bc9d9748/1/1",
            "gun_light_revolver_dani c8a6bc9d9748/1/1.1 c8a6bc9d9748/1/0.9",
            "gun_aberrator bc19c7845725/1/1",
            "gun_aberrator_eott bc19c7845725/1/1 bc19c7845725/1/1",
            "gun_flaregun ba71089bd91a/1/1",
            "gun_congolake b70f6ad86e05/1/1",
            "gun_mk108 ebca5cf9a06f/1/1",
            "gun_henry fca2063c91fe/1/1",
            "gun_henry_lincoln fca2063c91fe/1/1.25",
            "gun_heavy_revolver cf74e6437c73/1/1",
            "gun_heavy_revolver_lilmac cf74e6437c73/1/1",
            "gun_heavy_revolver_protege cf74e6437c73/1/0.8",
            "gun_hangman cf74e6437c73/1/1",
            "gun_amat 154c1ac5e32d/1/1",
            "gun_amat_subtlety 154c1ac5e32d/1/1",
            "gun_amat_penance e1aee4bcee6a/1/1",
            "gun_m2 861a80a5da31/1/1",
            "gun_g3 f3455ebd0db3/1/1",
            "gun_g3_zebra 8fb35a92b07a/1/1",
            "gun_stg77 f3455ebd0db3/1/1",
            "gun_bolter cc2f54f67252/1/1",
            "gun_carbine cc2f54f67252/1/1",
            "gun_minigun 6933c75eff3f|8b1065d49662|c9aa969772ab/1/1",
            "gun_minigun_lacunae afc1ffbc5791/1/1",
            "gun_minigun_dual 6933c75eff3f|8b1065d49662|c9aa969772ab/1/1 6933c75eff3f|8b1065d49662|c9aa969772ab/1/1",
            "gun_mas36 d03cc7991ac6/1/1",
            "gun_greasegun 566fe5c9e0d7/1/1",
            "gun_lag c8a6bc9d9748/1/1",
            "gun_uzi b94adc008a08/1/1",
            "gun_uzi_akimbo b94adc008a08/1/1 b94adc008a08/1/1",
            "gun_coilgun 679efed3ce68/1/1",
            "gun_n_i_4_n_i 679efed3ce68/1/1",
            "gun_pepperbox cc2f54f67252/1/1",
            "gun_fatman 20d259c9ffa1/1/1",
            "gun_tesla_cannon ae03937dc337/1/1",
            "gun_laser_pistol b316988bd5a1/1/1",
            "gun_laser_pistol_pew_pew b316988bd5a1/1/0.8",
            "gun_laser_pistol_morning_glory b316988bd5a1/1/1.1",
            "gun_lasrifle 0fa67e7b8ecd/1/1",
            "gun_flamer_daybreaker cc2f54f67252/1/1",
            "gun_folly 77b529982f7f/100/1",
            "gun_panzerschreck 881b5d3cd329/1/1",
            "gun_stinger 881b5d3cd329/1/1",
            "gun_quadro 881b5d3cd329/1/1",
            "gun_fliegerfaust 455e10d1708f/1/1",
            "gun_missile_launcher 881b5d3cd329/1/1",
            "gun_fireext 7cb724eee6e2/1/1",
            "gun_charge_thrower ba71089bd91a/1/1"
    };

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function FIRE_SOUNDS =
            TESTS.function("gh155_gun_fire_sounds_are_17", 20, () -> Gh155GameTests::fireSounds);
    public static final GameTests.Function GUN_WHACK =
            TESTS.function("gh155_gun_whack_is_17_foley", 20, () -> Gh155GameTests::gunWhack);
    public static final GameTests.Function MACHINE_SOUNDS =
            TESTS.function("gh155_machine_sounds_are_17", 20, () -> Gh155GameTests::machineSounds);

    public static void init() {}

    public static void fireSounds(GameTestHelper helper) {
        JsonObject sounds = soundsJson(helper);
        List<String> wrong = new ArrayList<>();
        for (String row : FIRE_17) {
            String[] parts = row.split(" ");
            Item item = BuiltInRegistries.ITEM.getOptional(Ids.of(Constants.MOD_ID, parts[0])).orElse(null);
            if (!(item instanceof ItemGunBaseNT gun)) {
                wrong.add(parts[0] + " (not a gun)");
                continue;
            }
            ItemStack stack = new ItemStack(gun);
            List<String> actual = new ArrayList<>();
            for (int i = 0; i < gun.getConfigCount(); i++) {
                GunConfig config = gun.getConfig(stack, i);
                for (Receiver receiver : config.getReceivers(stack)) {
                    SoundEvent sound = receiver.getFireSound(stack);
                    actual.add((sound == null ? "none" : files(sounds, sound)) + "/"
                            + number(receiver.getFireVolume(stack)) + "/" + number(receiver.getFirePitch(stack)));
                }
            }
            List<String> expected = List.of(parts).subList(1, parts.length);
            if (!actual.equals(expected)) wrong.add(parts[0] + " " + actual + " != " + expected);
        }
        helper.assertTrue(wrong.isEmpty(), "fire sounds unlike 1.7: " + wrong);
        helper.succeed();
    }

    /** The SPAS-12's jam plays 1.7's {@code weapon.foley.gunWhack}, two files, not the gavel's {@code weapon.whack}. */
    public static void gunWhack(GameTestHelper helper) {
        String files = files(soundsJson(helper), ModGunSounds.GUN_WHACK.get());
        helper.assertTrue(files.equals("b5698f9479ca|dc632d503581"), "gun whack files " + files);
        helper.succeed();
    }

    /**
     * The events the chemical plant, Leviathan, HIMARS turret, artillery and HIMARS rounds and request drone dock play
     * resolve to 1.7's files: {@code block.chemicalPlant}, {@code block.chungusTurbineRunning},
     * {@code weapon.rocketFlame}, {@code weapon.explosionMedium}, {@code block.storageClose}.
     */
    public static void machineSounds(GameTestHelper helper) {
        JsonObject sounds = soundsJson(helper);
        List<String> wrong = new ArrayList<>();
        check(sounds, ModSounds.CHEMICAL_PLANT.get(), "e57838bf0f08", wrong);
        check(sounds, ModSounds.CHUNGUS_TURBINE.get(), "b22329a3b6ed", wrong);
        check(sounds, ModSounds.WEAPON_ROCKET_FLAME.get(), "455e10d1708f", wrong);
        check(sounds, ModSounds.EXPLOSION_MEDIUM.get(), "de4f42ead998", wrong);
        check(sounds, ModSounds.STORAGE_CLOSE.get(), "bfa1382d4ebb", wrong);
        helper.assertTrue(wrong.isEmpty(), "machine sounds unlike 1.7: " + wrong);
        helper.succeed();
    }

    private static void check(JsonObject sounds, SoundEvent sound, String expected, List<String> wrong) {
        String files = files(sounds, sound);
        if (!files.equals(expected)) wrong.add(BuiltInRegistries.SOUND_EVENT.getKey(sound) + " " + files + " != " + expected);
    }

    private static String number(float value) {
        return value == (int) value ? Integer.toString((int) value) : Float.toString(value);
    }

    private static JsonObject soundsJson(GameTestHelper helper) {
        try (InputStream in = Gh155GameTests.class.getResourceAsStream("/assets/hbm/sounds.json")) {
            helper.assertTrue(in != null, "sounds.json is in the jar");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("sounds.json", e);
        }
    }

    /** The sorted MD5 prefixes of the files {@code sound} resolves to. */
    private static String files(JsonObject sounds, SoundEvent sound) {
        String[] id = String.valueOf(BuiltInRegistries.SOUND_EVENT.getKey(sound)).split(":", 2);
        String path = "/assets/" + id[0] + "/sounds.json";
        JsonObject table = id[0].equals(Constants.MOD_ID) ? sounds : null;
        if (table == null || !table.has(id[1])) return "unresolved:" + id[0] + ":" + id[1] + " in " + path;
        TreeSet<String> hashes = new TreeSet<>();
        for (JsonElement entry : table.getAsJsonObject(id[1]).getAsJsonArray("sounds")) {
            String name = entry.isJsonObject() ? entry.getAsJsonObject().get("name").getAsString() : entry.getAsString();
            String[] file = name.split(":", 2);
            hashes.add(md5("/assets/" + file[0] + "/sounds/" + file[1] + ".ogg"));
        }
        return String.join("|", hashes);
    }

    private static String md5(String resource) {
        try (InputStream in = Gh155GameTests.class.getResourceAsStream(resource)) {
            if (in == null) return "missing:" + resource;
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(in.readAllBytes())).substring(0, 12);
        } catch (IOException | NoSuchAlgorithmException e) {
            return "unreadable:" + resource;
        }
    }
}
