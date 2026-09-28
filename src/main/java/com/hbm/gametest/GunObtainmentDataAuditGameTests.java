package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.inventory.recipes.AmmoPressRecipes;
import com.hbm.inventory.recipes.AmmoPressRecipes.AmmoPressRecipe;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilOutput;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.GunCatalogue;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Audit contracts for the original-hbm gun and ammunition obtainment boundary. */
public final class GunObtainmentDataAuditGameTests {

    private static final Set<String> ORIGINAL_ANVIL_AMMO_OUTPUTS = Set.of(
            "mold_c9", "mold_c50", "mold_barrel_light", "mold_barrel_heavy", "mold_receiver_light",
            "mold_receiver_heavy", "mold_mechanism", "mold_stock", "mold_grip", "stamp_9", "stamp_50",
            "stamp_desh_9", "stamp_desh_50");

    private static final List<String> ORIGINAL_WEAPON_GUN_OUTPUTS = List.of(
            "gun_pepperbox", "gun_light_revolver", "gun_light_revolver_atlas", "gun_henry",
            "gun_henry_lincoln", "gun_greasegun", "gun_maresleg", "gun_maresleg_akimbo", "gun_flaregun",
            "gun_am180", "gun_liberator", "gun_congolake", "gun_flamer", "gun_flamer_topaz",
            "gun_heavy_revolver", "gun_carbine", "gun_uzi", "gun_uzi_akimbo", "gun_spas12",
            "gun_panzerschreck", "gun_star_f", "gun_star_f_akimbo", "gun_g3", "gun_g3_zebra",
            "gun_stinger", "gun_mk108", "gun_chemthrower", "gun_amat", "gun_m2", "gun_autoshotgun",
            "gun_autoshotgun_shredder", "gun_quadro", "gun_lag", "gun_minigun", "gun_missile_launcher",
            "gun_tesla_cannon", "gun_laser_pistol", "gun_laser_pistol_pew_pew", "gun_stg77", "gun_fatman",
            "gun_tau", "gun_lasrifle", "gun_double_barrel_sacred_dragon", "gun_charge_thrower",
            "gun_drill", "gun_pa_melee", "gun_pa_ranged", "gun_b92", "gun_b92_ammo", "gun_fireext");

    /**
     * Original-hbm items whose registry name starts with {@code gun_} but which are not guns: the two
     * weapon repair kits (1.7 {@code ModItems.gun_kit_1/2}, {@code ItemRepairKit}). Named explicitly so
     * the gun check above stays strict for everything else.
     */
    private static final List<String> ORIGINAL_NON_GUN_ITEMS = List.of("gun_kit_1", "gun_kit_2");

    /**
     * The guns original-hbm fires without a report of their own: the two flamethrowers and the
     * chemthrower run a looping client sound instead, the Tau charges, and the tools and the B92 have
     * no {@code .sound(...)} on any receiver upstream either.
     */
    private static final Set<String> ORIGINAL_SILENT_GUNS = Set.of(
            "gun_flamer", "gun_flamer_topaz", "gun_chemthrower", "gun_tau", "gun_drill",
            "gun_pa_melee", "gun_pa_ranged", "gun_b92", "gun_b92_ammo");

    private GunObtainmentDataAuditGameTests() {}

    /** The source table is 89 rows; the live list may be smaller only while dependency items are absent. */
    public static void ammoPressCatalogue(GameTestHelper helper) {
        List<AmmoPressRecipe> recipes = AmmoPressRecipes.getRecipes();
        helper.assertTrue(recipes.size() == AmmoPressRecipes.SOURCE_RECIPE_COUNT,
                "Ammo Press must expose all 89 dependency-complete original-hbm rows: " + recipes.size());
        Set<String> outputs = new HashSet<>();
        for (AmmoPressRecipe recipe : recipes) {
            String path = BuiltInRegistries.ITEM.getKey(recipe.output.getItem()).getPath();
            outputs.add(path);
            helper.assertTrue(path.startsWith("ammo_standard_"),
                    "Ammo Press output must use the flattened ammo_standard_<enum lower> convention: " + path);
            helper.assertTrue(recipe.input.length == 9, "Ammo Press row lost its nine positional slots: " + path);
            boolean hasIngredient = false;
            for (int slot = 0; slot < recipe.input.length; slot++) {
                // Null is the source recipe's intentional empty positional slot, not a missing dependency.
                if (recipe.input[slot] != null) hasIngredient = true;
            }
            helper.assertTrue(hasIngredient, "Active Ammo Press row has no source ingredients: " + path);
        }
        helper.assertTrue(outputs.contains("ammo_standard_m357_bp")
                        && outputs.contains("ammo_standard_m44_bp")
                        && outputs.contains("ammo_standard_p22_sp")
                        && outputs.contains("ammo_standard_p9_sp"),
                "the four first original-hbm Ammo Press families must remain in source order");
        helper.succeed();
    }

    /** The source's 13 casing/gun-part stamp outputs must remain the anvil gateway, not table substitutes. */
    public static void anvilAmmoGateway(GameTestHelper helper) {
        Set<String> found = new HashSet<>();
        for (var recipe : AnvilRecipes.getConstruction()) {
            for (AnvilOutput output : recipe.output) {
                String path = BuiltInRegistries.ITEM.getKey(output.stack.getItem()).getPath();
                if (ORIGINAL_ANVIL_AMMO_OUTPUTS.contains(path)) found.add(path);
            }
        }
        helper.assertTrue(found.equals(ORIGINAL_ANVIL_AMMO_OUTPUTS),
                "anvil ammo/gun-part gateway is incomplete: missing " + difference(ORIGINAL_ANVIL_AMMO_OUTPUTS, found));
        helper.succeed();
    }

    private static Set<String> difference(Set<String> expected, Set<String> actual) {
        Set<String> missing = new HashSet<>(expected);
        missing.removeAll(actual);
        return missing;
    }

    /** Registered gun items must be source-named; unrelated registry entries are not silently treated as guns. */
    public static void gunRegistryNames(GameTestHelper helper) {
        int guns = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (!Constants.MOD_ID.equals(id.getNamespace()) || !id.getPath().startsWith("gun_")) continue;
            if (ORIGINAL_NON_GUN_ITEMS.contains(id.getPath())) {
                helper.assertTrue(!(item instanceof ItemGunBaseNT),
                        "source non-gun item is registered as a gun: " + id);
                continue;
            }
            guns++;
            helper.assertTrue(item instanceof ItemGunBaseNT || ORIGINAL_WEAPON_GUN_OUTPUTS.contains(id.getPath()),
                    "gun-shaped registry entry has no Sedna/source identity: " + id);
        }
        helper.assertTrue(guns >= 15, "the completed Gen-3 gun slice must retain its existing gun registrations");
        helper.succeed();
    }

    /** Positive post-flush signal that common bootstrap declared every current gun/ammo catalogue. */
    public static void catalogueRegistration(GameTestHelper helper) {
        int guns = 0;
        int ammo = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (!Constants.MOD_ID.equals(id.getNamespace())) continue;
            if (id.getPath().startsWith("gun_") && item instanceof ItemGunBaseNT) guns++;
            if (id.getPath().startsWith("ammo_")) ammo++;
        }
        helper.assertTrue(guns == GunCatalogue.EXPECTED_GUN_ITEMS,
                "weapon catalogue registration count changed: " + guns);
        helper.assertTrue(ammo == GunCatalogue.EXPECTED_AMMO_ITEMS,
                "ammo catalogue registration count changed: " + ammo);
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(com.hbm.items.ModAmmoItems.designator_arty_range.get())
                        .getPath().equals("designator_arty_range"),
                "non-ammo obtainment item must remain registered beside the ammo catalogue");

        int configCount = com.hbm.items.weapon.sedna.BulletConfigs.all().size();
        helper.assertTrue(configCount == GunCatalogue.EXPECTED_BULLET_CONFIGS,
                "factory catalogue registration count changed: " + configCount);
        GunCatalogue.init();
        helper.assertTrue(com.hbm.items.weapon.sedna.BulletConfigs.all().size() == configCount,
                "repeated catalogue initialization must not duplicate bullet configs");
        helper.succeed();
    }

    /**
     * Every receiver that can fire names a sound. A receiver built without one is silent with nothing
     * logged, which is invisible to every other test here and to the compile.
     */
    public static void gunFireSounds(GameTestHelper helper) {
        int checked = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof ItemGunBaseNT gun)) continue;
            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            if (ORIGINAL_SILENT_GUNS.contains(path)) continue;
            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item);
            for (int index = 0; gun.isValidConfigIndex(index); index++) {
                com.hbm.items.weapon.sedna.Receiver[] receivers = gun.getConfig(stack, index).getReceivers(stack);
                if (receivers == null) continue;
                for (com.hbm.items.weapon.sedna.Receiver receiver : receivers) {
                    helper.assertTrue(receiver.getFireSound(stack) != null,
                            "Gun receiver fires with no sound: " + path + " config " + index);
                    checked++;
                }
            }
        }
        helper.assertTrue(checked >= 60, "Expected every registered gun receiver to be audited: " + checked);
        Constants.LOG.info("[GUNS] {} gun receivers audited, every one names a fire sound", checked);
        helper.succeed();
    }
}
