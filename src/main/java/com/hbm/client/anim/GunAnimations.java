package com.hbm.client.anim;

import com.hbm.client.gun.JadeShotgunClient;
import com.hbm.items.ModGunItems;
import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import dev.golem.core.render.anim.BusAnimation;
import dev.golem.core.render.anim.BusAnimationKeyframe.IType;
import dev.golem.core.render.anim.BusAnimationSequence;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import dev.golem.core.Ids;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Which animation each gun plays for each cue.
 *
 * <p>Upstream hangs this off the gun config ({@code GunConfig.anim(...)}), which forces a common class to name a
 * client-only animation type. Here the table is registered per item on the client, and the cue that arrives over
 * {@code GunAnimationPayload} is looked up in it.
 *
 * <p>The keyframes are ports of 1.12.2's {@code XFactory357.LAMBDA_ATLAS_ANIMS} and
 * {@code Lego.LAMBDA_DEBUG_ANIMS}, value for value and millisecond for millisecond.
 */
public final class GunAnimations {

    /**
     * Declared tables, keyed by a <em>supplier</em> of the item.
     *
     * <p>Lazy: on NeoForge the client entry point runs during mod construction, before the item registry is
     * flushed, so resolving an item here would throw "Missing key in ResourceKey[minecraft:item]". The map is
     * built on first lookup, at render time.
     */
    private static final Map<Supplier<Item>, AnimationTable> DECLARED = new LinkedHashMap<>();
    private static Map<Item, AnimationTable> resolved;

    @FunctionalInterface
    private interface AnimationTable {
        BusAnimation get(ItemStack stack, GunAnimation cue, int rail);
    }

    private GunAnimations() {}

    public static void register(Supplier<Item> item, AnimationTable table) {
        DECLARED.put(item, table);
        resolved = null;
    }

    /** The animation {@code item} plays for {@code cue}, or null when it has none; that is fine. */
    public static BusAnimation get(ItemStack stack, GunAnimation cue, int rail) {
        if (!(stack.getItem() instanceof ItemGunBaseNT gun) || !gun.isValidConfigIndex(rail)) return null;
        if (resolved == null) {
            Map<Item, AnimationTable> built = new HashMap<>();
            DECLARED.forEach((supplier, table) -> built.put(supplier.get(), table));
            resolved = built;
        }
        AnimationTable table = resolved.get(stack.getItem());
        return table == null ? null : table.get(stack, cue, rail);
    }

    public static void init() {
        register(ModGunItems.gun_light_revolver::get, (stack, cue, rail) -> atlas(cue));
        register(ModGunItems.gun_debug::get, (stack, cue, rail) -> debug(cue));
        register(ModGunItems.gun_star_f::get, GunAnimations::star);
        register(ModGunItems.gun_star_f_akimbo::get, GunAnimations::star);
        register(ModGunItems.gun_am180::get, GunAnimations::am180);
        register(ModGunItems.gun_greasegun::get, GunAnimations::greasegun);
        register(ModGunItems.gun_lag::get, (stack, cue, rail) -> lag(cue));
        register(ModGunItems.gun_uzi::get, GunAnimations::uzi);
        register(ModGunItems.gun_uzi_akimbo::get, GunAnimations::uzi);
        register(ModGunItems.gun_henry::get, GunAnimations::henry);
        register(ModGunItems.gun_henry_lincoln::get, GunAnimations::henry);
        register(ModGunItems.gun_heavy_revolver::get, GunAnimations::nopip);
        register(ModGunItems.gun_heavy_revolver_lilmac::get, GunAnimations::lilmac);
        register(ModGunItems.gun_heavy_revolver_protege::get, GunAnimations::lilmac);
        register(ModGunItems.gun_hangman::get, GunAnimations::hangman);
        register(ModGunItems.gun_light_revolver_atlas::get, (stack, cue, rail) -> atlas(cue));
        register(ModGunItems.gun_light_revolver_dani::get, SednaGen3Animations::dani);
        register(ModGunItems.gun_pepperbox::get, SednaGen3Animations::pepperbox);
        register(ModGunItems.gun_g3::get, SednaGen3Animations::g3);

        registerById("gun_g3_zebra", SednaGen3Animations::g3);
        registerById("gun_minigun", GunAnimations::LAMBDA_MINIGUN_ANIMS);
        registerById("gun_minigun_lacunae", GunAnimations::LAMBDA_MINIGUN_ANIMS);
        registerById("gun_minigun_dual", GunAnimations::LAMBDA_MINIGUN_ANIMS);
        registerById("gun_stg77", GunAnimations::stg77);
        registerById("gun_flaregun", GunAnimations::flaregun);
        registerById("gun_flamer", GunAnimations::flamer);
        registerById("gun_flamer_topaz", GunAnimations::flamer);
        registerById("gun_flamer_daybreaker", GunAnimations::flamer);
        registerById("gun_chemthrower", (stack, cue, rail) -> cue == GunAnimation.EQUIP
                ? new BusAnimation().addBus("EQUIP", seq().addPos(-45, 0, 0, 0)
                        .addPos(0, 0, 0, 500, IType.SIN_DOWN)) : null);
        registerById("gun_congolake", GunAnimations::congolake);
        registerById("gun_spas12", GunAnimations::spas12);
        registerById("gun_panzerschreck", GunAnimations::panzerschreck);
        registerById("gun_stinger", GunAnimations::panzerschreck);
        registerById("gun_quadro", GunAnimations::quadro);
        registerById("gun_missile_launcher", GunAnimations::missileLauncher);
        registerById("gun_fatman", GunAnimations::fatman);
        registerById("gun_maresleg", GunAnimations::maresleg);
        registerById("gun_maresleg_akimbo", GunAnimations::maresleg);
        registerById("gun_maresleg_broken", GunAnimations::mareslegShort);
        registerById("gun_liberator", GunAnimations::liberator);
        registerById("gun_autoshotgun", GunAnimations::shredder);
        registerById("gun_autoshotgun_shredder", GunAnimations::shredder);
        registerById("gun_carbine", GunAnimations::carbine);
        registerById("gun_mas36", GunAnimations::mas36);
        registerById("gun_mk108", GunAnimations::mk108);
        registerById("gun_bolter", GunAnimations::bolter);
        registerById("gun_autoshotgun_sexy", GunAnimations::sexy);
        registerById("gun_autoshotgun_heretic", GunAnimations::sexy);
        registerById("gun_folly", GunAnimations::folly);
        registerById("gun_double_barrel", GunAnimations::doubleBarrel);
        registerById("gun_double_barrel_sacred_dragon", GunAnimations::doubleBarrel);
        registerById("gun_amat", GunAnimations::amat);
        registerById("gun_amat_subtlety", GunAnimations::amat);
        registerById("gun_amat_penance", GunAnimations::amat);
        registerById("gun_m2", (stack, cue, rail) -> cue == GunAnimation.EQUIP
                ? new BusAnimation().addBus("EQUIP", seq().addPos(80, 0, 0, 0)
                        .addPos(0, 0, 0, 500, IType.SIN_FULL))
                : cue == GunAnimation.CYCLE ? new BusAnimation().addBus("RECOIL", seq()
                        .addPos(0, 0, -0.25, 25).addPos(0, 0, 0, 75)) : null);
        registerById("gun_tesla_cannon", GunAnimations::tesla);
        registerById("gun_tau", GunAnimations::tau);
        registerById("gun_laser_pistol", GunAnimations::laserPistol);
        registerById("gun_laser_pistol_pew_pew", GunAnimations::laserPistol);
        registerById("gun_laser_pistol_morning_glory", GunAnimations::laserPistol);
        registerById("gun_lasrifle", GunAnimations::lasrifle);
        registerById("gun_coilgun", GunAnimations::coilgun);
        registerById("gun_n_i_4_n_i", GunAnimations::ni4ni);
        registerById("gun_charge_thrower", GunAnimations::chargeThrower);
        registerById("gun_drill", GunAnimations::drill);
        registerById("gun_aberrator", GunAnimations::aberrator);
        registerById("gun_jshotgun", (stack, cue, rail) -> JadeShotgunClient.animation(stack, cue));
        registerById("gun_aberrator_eott", GunAnimations::aberrator);
    }

    private static BusAnimation am180(ItemStack stack, GunAnimation type, int rail) {
        if (type == GunAnimation.EQUIP) {
            return new BusAnimation().addBus("EQUIP", seq().addPos(45, 0, 0, 0)
                    .addPos(0, 0, 0, 500, IType.SIN_FULL));
        }
        return resource("am180", type, Map.of(GunAnimation.CYCLE, "Fire",
                GunAnimation.CYCLE_DRY, "FireDry", GunAnimation.RELOAD, "Reload",
                GunAnimation.JAMMED, "Jammed", GunAnimation.INSPECT, "Inspect"));
    }

    private static BusAnimation stg77(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(45, 0, 0, 0)
                    .addPos(0, 0, 0, 500, IType.SIN_FULL));
            case CYCLE -> new BusAnimation().addBus("RECOIL", seq()
                    .addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.125 : -0.375, 25, IType.SIN_DOWN)
                    .addPos(0, 0, 0, 75, IType.SIN_FULL))
                    .addBus("SAFETY", seq().addPos(0.25, 0, 0, 0).addPos(0.25, 0, 0, 2000)
                            .addPos(0, 0, 0, 50));
            default -> resource("stg77", type, Map.of(GunAnimation.CYCLE_DRY, "FireDry",
                    GunAnimation.RELOAD, "Reload", GunAnimation.INSPECT, "Inspect"));
        };
    }

    private static BusAnimation congolake(ItemStack stack, GunAnimation type, int rail) {
        int ammo = loaded(stack, rail);
        String animation = switch (type) {
            case EQUIP -> "Equip";
            case CYCLE -> ammo <= 1 ? "FireEmpty" : "Fire";
            case RELOAD -> ammo == 0 ? "ReloadEmpty" : "ReloadStart";
            case RELOAD_CYCLE -> "Reload";
            case RELOAD_END -> "ReloadEnd";
            case JAMMED -> "Jammed";
            case INSPECT -> "Inspect";
            default -> null;
        };
        return animation == null ? null : LagAnimationLoader.load("congolake").get(animation);
    }

    private static BusAnimation spas12(ItemStack stack, GunAnimation type, int rail) {
        if (type == GunAnimation.EQUIP) {
            return new BusAnimation().addBus("EQUIP", seq().addPos(-60, 0, 0, 0)
                    .addPos(0, 0, -3, 500, IType.SIN_DOWN));
        }
        String animation = switch (type) {
            case CYCLE -> "Fire";
            case CYCLE_DRY -> "FireDry";
            case ALT_CYCLE -> "FireAlt";
            case RELOAD -> loaded(stack, rail) <= 0 ? "ReloadEmptyStart" : "ReloadStart";
            case RELOAD_CYCLE -> "Reload";
            case RELOAD_END -> "ReloadEnd";
            case JAMMED -> "Jammed";
            case INSPECT -> "Inspect";
            default -> null;
        };
        return animation == null ? null : LagAnimationLoader.load("spas12").get(animation);
    }

    private static BusAnimation greasegun(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case EQUIP -> new BusAnimation()
                    .addBus("EQUIP", seq().addPos(80, 0, 0, 0).addPos(80, 0, 0, 500)
                            .addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("STOCK", seq().addPos(0, 0, -4, 0).addPos(0, 0, -4, 200)
                            .addPos(0, 0, 0, 300, IType.SIN_FULL));
            case CYCLE -> new BusAnimation()
                    .addBus("RECOIL", seq().addPos(0, 0,
                            ItemGunBaseNT.getIsAiming(stack) ? -0.25 : -0.5, 50, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("FLAP", seq().addPos(0, 0, 15, 100, IType.SIN_DOWN)
                            .addPos(0, 0, -5, 100, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_FULL));
            case CYCLE_DRY -> new BusAnimation()
                    .addBus("LIFT", seq().addPos(0, 0, 0, 500).addPos(-25, 0, 0, 250, IType.SIN_FULL)
                            .addPos(-25, 0, 0, 750).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("TURN", seq().addPos(0, 0, 0, 500).addPos(0, 0, -45, 250, IType.SIN_FULL)
                            .addPos(0, 0, -45, 750).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("HANDLE", seq().addPos(0, 0, 0, 500).addPos(0, 0, 0, 250)
                            .addPos(-90, 0, 0, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL));
            case RELOAD -> new BusAnimation()
                    .addBus("MAG", seq().addPos(0, -8, 0, 250, IType.SIN_UP).addPos(0, -8, 0, 750)
                            .addPos(0, 0, 0, 500, IType.SIN_DOWN))
                    .addBus("LIFT", seq().addPos(0, 0, 0, 500).addPos(-25, 0, 0, 250, IType.SIN_FULL)
                            .addPos(-25, 0, 0, 1750).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("TURN", seq().addPos(0, 0, 0, 1750).addPos(0, 0, -45, 250, IType.SIN_FULL)
                            .addPos(0, 0, -45, 500).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("HANDLE", seq().addPos(0, 0, 0, 2000).addPos(-90, 0, 0, 250, IType.SIN_FULL)
                            .addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("BULLET", seq().addPos(loaded(stack, 0) <= 0 ? 1 : 0, 0, 0, 0)
                            .addPos(0, 0, 0, 1000));
            case JAMMED -> new BusAnimation()
                    .addBus("LIFT", seq().addPos(0, 0, 0, 500).addPos(-25, 0, 0, 250, IType.SIN_FULL)
                            .addPos(-25, 0, 0, 1500).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("TURN", seq().addPos(0, 0, 0, 500).addPos(0, 0, -45, 250, IType.SIN_FULL)
                            .addPos(0, 0, -45, 1500).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("HANDLE", seq().addPos(0, 0, 0, 500).addPos(0, 0, 0, 250)
                            .addPos(-90, 0, 0, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL)
                            .addPos(0, 0, 0, 250).addPos(-90, 0, 0, 250, IType.SIN_FULL)
                            .addPos(0, 0, 0, 250, IType.SIN_FULL));
            case INSPECT -> new BusAnimation()
                    .addBus("TURN", seq().addPos(0, 0, -45, 150).addPos(0, 0, 45, 150)
                            .addPos(0, 0, 45, 50).addPos(0, 0, 0, 250).addPos(0, 0, 0, 500)
                            .addPos(0, 0, 45, 150).addPos(0, 0, -45, 150).addPos(0, 0, 0, 150))
                    .addBus("FLAP", seq().addPos(0, 0, 0, 300).addPos(0, 0, 180, 150)
                            .addPos(0, 0, 180, 850).addPos(0, 0, 0, 150));
            default -> null;
        };
    }

    private static BusAnimation lag(GunAnimation type) {
        if (type == GunAnimation.EQUIP) {
            return new BusAnimation().addBus("EQUIP", seq().addPos(-90, 0, 0, 0)
                    .addPos(0, 0, 0, 350, IType.SIN_DOWN));
        }
        Map<String, BusAnimation> lagAnimations = LagAnimationLoader.load();
        BusAnimation animation = lagAnimations.get(switch (type) {
            case CYCLE -> "Firing";
            case CYCLE_DRY -> "Dryfire";
            case RELOAD -> "Reload";
            case JAMMED -> "Jam";
            case INSPECT -> "Inspect";
            default -> "";
        });
        if (animation == null) return null;
        if (type == GunAnimation.INSPECT) {
            animation.addBus("ADD_TRANS", seq().addPos(-4, 0, -3, 500).addPos(-4, 0, -3, 2000)
                    .addPos(0, 0, 0, 500));
            animation.addBus("ADD_ROT", seq().addPos(0, -2, 5, 500).addPos(0, -2, 5, 2000)
                    .addPos(0, 0, 0, 500));
        }
        return animation;
    }

    private static BusAnimationSequence seq() {
        return new BusAnimationSequence();
    }

    private static void registerById(String itemId, AnimationTable table) {
        register(() -> BuiltInRegistries.ITEM.getValue(Ids.of("hbm", itemId)), table);
    }

    private static BusAnimation amat(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(45, 0, 0, 0)
                    .addPos(0, 0, 0, 500, IType.SIN_FULL)).addBus("BIPOD", seq().hold(500)
                            .addPos(80, 0, 0, 350).addPos(80, 25, 0, 150));
            case CYCLE -> new BusAnimation().addBus("RECOIL", seq().addPos(0, 0, -0.5, 50, IType.SIN_DOWN)
                    .addPos(0, 0, 0, 100, IType.SIN_FULL)).addBus("BOLT_TURN", seq().hold(250)
                            .addPos(0, 0, -60, 150).hold(700).addPos(0, 0, 0, 150))
                    .addBus("BOLT_PULL", seq().hold(350).addPos(0, 0, -2.5, 250, IType.SIN_UP)
                            .hold(250).addPos(0, 0, 0, 200)).addBus("LIFT", seq().hold(600)
                            .addPos(-3, 0, 0, 150, IType.SIN_DOWN).hold(300).addPos(0, 0, 0, 250, IType.SIN_FULL));
            case CYCLE_DRY -> amatCycleDry();
            case RELOAD -> new BusAnimation().addBus("MAG", seq().addPos(0, -10, 0, 350, IType.SIN_UP)
                    .addPos(0, 0, 0, 650, IType.SIN_UP)).addBus("LIFT", seq().hold(1000)
                            .addPos(-2, 0, 0, 150, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL)
                            .hold(450).addPos(-3, 0, 0, 150, IType.SIN_DOWN).hold(300)
                            .addPos(0, 0, 0, 250, IType.SIN_FULL)).addBus("BOLT_TURN", seq().hold(1500)
                            .addPos(0, 0, -60, 150).hold(700).addPos(0, 0, 0, 150))
                    .addBus("BOLT_PULL", seq().hold(1600).addPos(0, 0, -2.5, 250, IType.SIN_UP)
                            .hold(250).addPos(0, 0, 0, 200));
            case JAMMED -> new BusAnimation().addBus("LIFT", seq().hold(250).addPos(-15, 0, 0, 500, IType.SIN_FULL)
                    .holdUntil(1650).addPos(0, 0, 0, 500, IType.SIN_FULL)).addBus("BOLT_TURN", seq().hold(250)
                            .addPos(0, 0, -60, 150).holdUntil(1250).addPos(0, 0, 0, 150))
                    .addBus("BOLT_PULL", seq().hold(350).addPos(0, 0, -2.5, 250, IType.SIN_UP)
                            .addPos(0, 0, 0, 200).addPos(0, 0, -2.5, 250, IType.SIN_UP).addPos(0, 0, 0, 200));
            case INSPECT -> new BusAnimation().addBus("SCOPE_THROW", seq().addPos(0, 0.5, 0, 100, IType.SIN_FULL)
                    .addPos(4, -2, 0, 500, IType.SIN_FULL).addPos(4, -2.5, 0, 100)
                    .addPos(4, 7, 0, 350, IType.SIN_FULL).addPos(4, -2.5, 0, 350, IType.SIN_DOWN)
                    .addPos(4, -2, 0, 100).hold(250).addPos(0, 0.5, 0, 500, IType.SIN_FULL))
                    .addBus("SCOPE_SPIN", seq().hold(700).addPos(-360, 0, 0, 700));
            default -> null;
        };
    }

    private static BusAnimation amatCycleDry() {
        return new BusAnimation().addBus("BOLT_TURN", seq().hold(250).addPos(0, 0, -60, 150)
                .hold(700).addPos(0, 0, 0, 150)).addBus("BOLT_PULL", seq().hold(350)
                        .addPos(0, 0, -2.5, 250, IType.SIN_UP).hold(250).addPos(0, 0, 0, 200))
                .addBus("LIFT", seq().hold(600).addPos(-3, 0, 0, 150, IType.SIN_DOWN)
                        .hold(300).addPos(0, 0, 0, 250, IType.SIN_FULL));
    }

    private static BusAnimation tesla(ItemStack stack, GunAnimation type, int rail) {
        int amount = loaded(stack, rail);
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(60, 0, 0, 0)
                    .addPos(0, 0, 0, 1000, IType.SIN_DOWN));
            case CYCLE -> new BusAnimation().addBus("RECOIL", seq().addPos(0, 0,
                    ItemGunBaseNT.getIsAiming(stack) ? -0.5 : -1, 100, IType.SIN_DOWN)
                    .addPos(0, 0, 0, 250, IType.SIN_FULL)).addBus("CYCLE", seq().addPos(0, 0, 0, 150)
                            .addPos(0, 0, 22.5, 350)).addBus("COUNT", seq().addPos(amount, 0, 0, 0));
            case CYCLE_DRY -> new BusAnimation().addBus("CYCLE", seq().addPos(0, 0, 0, 150)
                    .addPos(0, 0, 22.5, 350));
            case INSPECT -> new BusAnimation().addBus("YOMI", seq().addPos(8, -4, 0, 0)
                    .addPos(4, -1, 0, 500, IType.SIN_DOWN).addPos(4, -1, 0, 1000)
                    .addPos(6, -6, 0, 500, IType.SIN_UP)).addBus("SQUEEZE", seq().setPos(1, 1, 1)
                            .addPos(1, 1, 1, 750).addPos(1, 1, 0.5, 125).addPos(1, 1, 1, 125));
            default -> null;
        };
    }

    private static BusAnimation laserPistol(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(60, 0, 0, 0)
                    .addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case CYCLE -> new BusAnimation().addBus("RECOIL", seq().addPos(0, 0, -0.5, 50, IType.SIN_DOWN)
                    .addPos(0, 0, 0, 150, IType.SIN_FULL));
            case RELOAD -> new BusAnimation()
                    .addBus("LATCH", seq().addPos(0, -20, 0, 100).hold(1900).addPos(0, 0, 0, 100))
                    .addBus("LIFT", seq().hold(100).addPos(-45, 0, 0, 250, IType.SIN_FULL)
                            .hold(500).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("JOLT", seq().hold(350).addPos(0, 0, 0.5, 100, IType.SIN_FULL)
                            .addPos(0, 0, -1.5, 100, IType.SIN_UP).addPos(0, 0, 0, 150, IType.SIN_FULL)
                            .holdUntil(2100).addPos(-0.0625, 0, 0, 50, IType.SIN_UP)
                            .addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("BATTERY", seq().hold(550).addPos(0, 0, 5, 250).hold(550)
                            .setPos(0, -2, -2).addPos(0, 0, -2, 250, IType.SIN_FULL)
                            .addPos(0, 0, 0, 250, IType.SIN_UP));
            case JAMMED -> new BusAnimation()
                    .addBus("LATCH", seq().hold(500).addPos(0, -20, 0, 100).hold(250)
                            .addPos(0, 0, 0, 100))
                    .addBus("JOLT", seq().hold(950).addPos(-0.0625, 0, 0, 50, IType.SIN_UP)
                            .addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("EQUIP", seq().hold(1500).addPos(7.5, 0, 0, 100, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 250, IType.SIN_FULL));
            case INSPECT -> new BusAnimation().addBus("SWIRL", seq().addPos(-720, 0, 0, 750, IType.SIN_FULL)
                    .hold(500).addPos(0, 0, 0, 750, IType.SIN_FULL));
            default -> null;
        };
    }

    private static BusAnimation lasrifle(ItemStack stack, GunAnimation type, int rail) {
        int amount = loaded(stack, rail);
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(60, 0, 0, 0)
                    .addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case CYCLE -> new BusAnimation()
                    .addBus("RECOIL", seq().addPos(0, 0, -0.5, 50, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 150, IType.SIN_FULL))
                    .addBus("CYCLE", seq().addPos(0, 0, 0, 150).addPos(0, 0, 22.5, 350))
                    .addBus("COUNT", seq().addPos(amount, 0, 0, 0));
            case RELOAD -> new BusAnimation()
                    .addBus("LEVER", seq().addPos(-90, 0, 0, 350, IType.SIN_UP)
                            .addPos(-90, 0, 0, 1500).addPos(0, 0, 0, 350, IType.SIN_UP))
                    .addBus("MAG", seq().addPos(0, 0, 0, 350).addPos(0, -5, 0, 350, IType.SIN_UP)
                            .addPos(0, -5, 0, 500).addPos(0, -0.25, 0, 500, IType.SIN_FULL)
                            .addPos(0, -0.25, 0, 150).addPos(0, 0, 0, 350))
                    .addBus("EQUIP", seq().addPos(0, 0, 0, 1700).addPos(-2, 0, 0, 100, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 100, IType.SIN_FULL));
            case JAMMED -> new BusAnimation()
                    .addBus("LEVER", seq().addPos(0, 0, 0, 500).addPos(-90, 0, 0, 350, IType.SIN_UP)
                            .addPos(-90, 0, 0, 600).addPos(0, 0, 0, 350, IType.SIN_UP))
                    .addBus("MAG", seq().addPos(0, 0, 0, 500).addPos(0, 0, 0, 350)
                            .addPos(0, -2, 0, 200, IType.SIN_UP).addPos(0, -0.25, 0, 250, IType.SIN_FULL)
                            .addPos(0, -0.25, 0, 150).addPos(0, 0, 0, 350))
                    .addBus("EQUIP", seq().addPos(0, 0, 0, 500).addPos(0, 0, 0, 800)
                            .addPos(-2, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL));
            case INSPECT -> new BusAnimation()
                    .addBus("LEVER", seq().addPos(-90, 0, 0, 350, IType.SIN_UP)
                            .addPos(-90, 0, 0, 600).addPos(0, 0, 0, 350, IType.SIN_UP))
                    .addBus("MAG", seq().addPos(0, 0, 0, 350).addPos(0, -2, 0, 200, IType.SIN_UP)
                            .addPos(0, -0.25, 0, 250, IType.SIN_FULL).addPos(0, -0.25, 0, 150)
                            .addPos(0, 0, 0, 350))
                    .addBus("EQUIP", seq().addPos(0, 0, 0, 800).addPos(-2, 0, 0, 100, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 100, IType.SIN_FULL));
            default -> null;
        };
    }

    private static BusAnimation tau(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(45, 0, 0, 0)
                    .addPos(0, 0, 0, 500, IType.SIN_FULL));
            case CYCLE -> tauCycle(-0.5);
            case ALT_CYCLE -> tauCycle(-3);
            case INSPECT -> new BusAnimation().addBus("EQUIP", seq().addPos(2, 0, 0, 150, IType.SIN_DOWN)
                    .addPos(0, 0, 0, 100, IType.SIN_FULL)).addBus("ROTATE", seq()
                            .addPos(0, 0, -1080, 1500, IType.SIN_DOWN));
            case SPINUP -> new BusAnimation().addBus("ROTATE", seq().addPos(0, 0, 2160, 3000, IType.SIN_UP)
                    .addPos(0, 0, 0, 0).addPos(0, 0, 14400, 10000));
            default -> null;
        };
    }

    private static BusAnimation tauCycle(double recoil) {
        return new BusAnimation().addBus("RECOIL", seq().addPos(0, 0, recoil, recoil == -3 ? 100 : 50, IType.SIN_DOWN)
                .addPos(0, 0, 0, 150, IType.SIN_FULL)).addBus("ROTATE", seq().addPos(0, 0, -5, 50, IType.SIN_DOWN)
                        .addPos(0, 0, 5, 100, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP));
    }

    private static BusAnimation coilgun(ItemStack stack, GunAnimation type, int rail) {
        if (type == GunAnimation.EQUIP) return new BusAnimation().addBus("RELOAD", seq().addPos(1, 0, 0, 0)
                .addPos(0, 0, 0, 250));
        if (type == GunAnimation.CYCLE) return new BusAnimation().addBus("RECOIL", seq()
                .addPos(ItemGunBaseNT.getIsAiming(stack) ? 0.5 : 1, 0, 0, 100).addPos(0, 0, 0, 200));
        if (type == GunAnimation.RELOAD) return new BusAnimation().addBus("RELOAD", seq().addPos(1, 0, 0, 250)
                .addPos(1, 0, 0, 500).addPos(0, 0, 0, 250));
        return null;
    }

    private static BusAnimation ni4ni(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(-720, 0, 0, 500));
            case CYCLE -> new BusAnimation().addBus("RECOIL", seq().addPos(ItemGunBaseNT.getIsAiming(stack) ? -5 : -30,
                    0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 150)).addBus("DRUM", seq().hold(50)
                            .addPos(0, 0, 120, 300, IType.SIN_FULL));
            case INSPECT -> new BusAnimation().addBus("EQUIP", seq().addPos(-1080, 0, 0, 750)
                    .hold(100).addPos(0, 0, 0, 750));
            default -> null;
        };
    }

    private static BusAnimation chargeThrower(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(-45, 0, 0, 0)
                    .addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case CYCLE -> new BusAnimation().addBus("RECOIL", seq().addPos(0, 0, -1, 100, IType.SIN_DOWN)
                    .addPos(0, 0, 0, 250, IType.SIN_FULL));
            case RELOAD -> new BusAnimation().addBus("RAISE", seq().addPos(-45, 0, 0, 500, IType.SIN_FULL)
                    .hold(2000).addPos(0, 0, 0, 500, IType.SIN_FULL)).addBus("AMMO", seq().setPos(0, -10, -5)
                            .hold(500).addPos(0, 0, 5, 750, IType.SIN_FULL).addPos(0, 0, 0, 500, IType.SIN_UP)
                            .hold(4000)).addBus("TWIST", seq().setPos(0, 0, 25).hold(2000)
                                    .addPos(0, 0, 0, 150));
            case INSPECT -> new BusAnimation().addBus("TURN", seq().addPos(0, 60, 0, 500, IType.SIN_FULL)
                    .hold(1750).addPos(0, 0, 0, 500, IType.SIN_FULL)).addBus("ROLL", seq().hold(750)
                            .addPos(0, 0, -90, 500, IType.SIN_FULL).hold(1000)
                            .addPos(0, 0, 0, 500, IType.SIN_FULL));
            default -> null;
        };
    }

    private static BusAnimation drill(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().setPos(-1, 0, 0)
                    .addPos(0, 0, 0, 750, IType.SIN_DOWN));
            case CYCLE -> drillCycle(rail);
            case CYCLE_DRY -> new BusAnimation().addBus("DEPLOY", seq().addPos(0.25, 0, 0, 250, IType.SIN_FULL)
                    .addPos(0, 0, 0, 250, IType.SIN_FULL)).addBus("SPIN", seq().addPos(360, 0, 0, 1500, IType.SIN_DOWN))
                    .addBus("SPEED", seq().addPos(0.75, 0, 0, 250).addPos(0, 0, 0, 1000, IType.SIN_DOWN));
            case INSPECT -> new BusAnimation().addBus("LIFT", seq().addPos(-45, 0, 0, 500, IType.SIN_FULL)
                    .hold(1000).addPos(0, 0, 0, 500, IType.SIN_DOWN));
            default -> null;
        };
    }

    private static BusAnimation drillCycle(int rail) {
        double deploy = HbmAnimations.getRelevantTransformationForBuild("DEPLOY", rail)[0];
        double speed = HbmAnimations.getRelevantTransformationForBuild("SPEED", rail)[0];
        double spin = HbmAnimations.getRelevantTransformationForBuild("SPIN", rail)[0] % 360;
        int tail = 750 + (int) (1000D * (1D - spin / 360D));
        return new BusAnimation().addBus("DEPLOY", seq().setPos(deploy, 0, 0)
                .addPos(1, 0, 0, (int) (500 * (1 - deploy)), IType.SIN_FULL).hold(1000)
                .addPos(0, 0, 0, 500, IType.SIN_FULL))
                .addBus("SPIN", seq().setPos(spin, 0, 0).addPos(spin + 540, 0, 0, 1500)
                        .addPos(1080, 0, 0, tail, IType.SIN_DOWN))
                .addBus("SPEED", seq().setPos(speed, 0, 0).addPos(1, 0, 0, 500).hold(1000)
                        .addPos(0, 0, 0, tail, IType.SIN_DOWN));
    }

    private static BusAnimation aberrator(ItemStack stack, GunAnimation type, int rail) {
        boolean aim = ItemGunBaseNT.getIsAiming(stack);
        int ammo = loaded(stack, rail);
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(360, 0, 0, 0)
                    .addPos(0, 0, 0, 500, IType.SIN_FULL)).addBus("RISE", seq().addPos(0, -3, 0, 0)
                            .addPos(0, 0, 0, 500, IType.SIN_FULL));
            case CYCLE -> new BusAnimation().addBus("RECOIL", seq().addPos(0, 0, 0, 50)
                    .addPos(aim ? -15 : -25, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("SIGHT", seq().addPos(0, 0, 0, 50).addPos(aim ? 5 : 15, 0, 0, 100, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 250, IType.SIN_FULL)).addBus("SLIDE", seq().addPos(0, 0, 0, 50)
                                    .addPos(0, 0, -1.125, 50, IType.SIN_DOWN).addPos(0, 0, -1.125, 50)
                                    .addPos(0, 0, 0, 150, IType.SIN_UP)).addBus(ammo <= 1 ? "NULL" : "BULLET",
                                            seq().addPos(0, 0, 0, 150).addPos(0, 0.375, 1.125, 150, IType.SIN_UP))
                    .addBus("HAMMER", seq().addPos(45, 0, 0, 50).addPos(-45, 0, -1.125, 50)
                            .addPos(-20, 0, -1.125, 50).addPos(0, 0, 0, 150, IType.SIN_UP));
            case CYCLE_DRY -> new BusAnimation().addBus("RECOIL", seq().addPos(0, 0, 0, 700)
                    .addPos(-5, 0, 0, 100, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("SLIDE", seq().addPos(0, 0, 0, 550).addPos(0, 0, -1.125, 150, IType.SIN_FULL)
                            .addPos(0, 0, -1.125, 50).addPos(0, 0, 0, 150, IType.SIN_UP))
                    .addBus("HAMMER", seq().addPos(45, 0, 0, 50).addPos(45, 0, 0, 500)
                            .addPos(-45, 0, -1.125, 150, IType.SIN_FULL).addPos(-20, 0, -1.125, 50)
                            .addPos(0, 0, 0, 150, IType.SIN_UP));
            case RELOAD -> new BusAnimation().addBus("ROLL", seq().addPos(0, 0, 20, 150, IType.SIN_FULL)
                    .addPos(0, 0, 20, 50).addPos(0, 0, -45, 150, IType.SIN_UP)
                    .addPos(0, 0, 0, 150, IType.SIN_FULL)).addBus("MAG", seq().addPos(0, 0, 0, 350)
                    .addPos(0, -2, 0, 0).addPos(-15, -5, 0, 350).addPos(-15, 0, 0, 0)
                    .addPos(-15, 0, 0, 700).addPos(3, 3, 0, 0).addPos(0, -2, 0, 250, IType.SIN_DOWN)
                    .addPos(0, -2, 0, 50).addPos(0, 0, 0, 150, IType.SIN_DOWN))
                    .addBus("MAGROLL", seq().addPos(0, 0, 0, 350).addPos(0, 0, -180, 250)
                            .setPos(0, 0, 0)).addBus("EQUIP", seq().addPos(0, 0, 0, 750)
                            .addPos(5, 0, 0, 150, IType.SIN_FULL).addPos(-190, 0, 0, 500, IType.SIN_FULL)
                            .addPos(-190, 0, 0, 450).addPos(-360, 0, 0, 350, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 0)).addBus("RECOIL", seq().addPos(0, 0, 0, 2350)
                            .addPos(-5, 0, 0, 100, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("SLIDE", seq().addPos(0, 0, 0, 2200).addPos(0, 0, -1.125, 150, IType.SIN_FULL)
                            .addPos(0, 0, -1.125, 50).addPos(0, 0, 0, 150, IType.SIN_UP))
                    .addBus("HAMMER", seq().addPos(0, 0, 0, 2250).addPos(-45, 0, -1.125, 100, IType.SIN_FULL)
                            .addPos(-20, 0, -1.125, 50).addPos(0, 0, 0, 150, IType.SIN_UP))
                    .addBus("BULLET", seq().addPos(ammo > 0 ? 0 : -100, 0, 0, 0)
                            .addPos(ammo > 0 ? 0 : -100, 0, 0, 2400).addPos(0, 0, 0, 0)
                            .addPos(0, 0.375, 1.125, 150, IType.SIN_UP));
            case INSPECT -> new BusAnimation().addBus("EQUIP", seq().addPos(0, 0, 0, 0)
                    .addPos(-720, 0, 0, 1000, IType.SIN_FULL).addPos(-720, 0, 0, 250)
                    .addPos(0, 0, 0, 1000, IType.SIN_FULL));
            default -> null;
        };
    }

    private static BusAnimation resource(String name, GunAnimation cue, Map<GunAnimation, String> names) {
        String animation = names.get(cue);
        return animation == null ? null : LagAnimationLoader.load(name).get(animation);
    }

    /** Source XFactory762mm.LAMBDA_MINIGUN_ANIMS, shared by both dual rails. */
    private static BusAnimation LAMBDA_MINIGUN_ANIMS(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case EQUIP -> new BusAnimation()
                    .addBus("EQUIP", seq().addPos(45, 0, 0, 0)
                            .addPos(0, 0, 0, 1000, IType.SIN_FULL));
            case CYCLE -> new BusAnimation()
                    .addBus("RECOIL", seq().addPos(0, 0,
                            ItemGunBaseNT.getIsAiming(stack) ? -0.25 : -0.5, 0)
                            .addPos(0, 0,
                                    ItemGunBaseNT.getIsAiming(stack) ? -0.25 : -0.5,
                                    100)
                            .addPos(0, 0, 0, 150, IType.SIN_FULL))
                    .addBus("ROTATE", seq().addPos(0, 0, 60, 50)
                            .addPos(0, 0, 720, 1000, IType.SIN_DOWN));
            case CYCLE_DRY -> new BusAnimation()
                    .addBus("ROTATE", seq().addPos(0, 0, 60, 50)
                            .addPos(0, 0, 720, 1000, IType.SIN_DOWN));
            case RELOAD -> new BusAnimation()
                    .addBus("EQUIP", seq().addPos(-15, 0, 0, 250, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("ROTATE", seq().addPos(0, 0, 60, 50)
                            .addPos(0, 0, 720, 1000, IType.SIN_DOWN));
            case INSPECT -> new BusAnimation()
                    .addBus("EQUIP", seq().addPos(3, 0, 0, 150, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("ROTATE", seq().addPos(0, 0, -720, 1000, IType.SIN_DOWN));
            default -> null;
        };
    }

    private static BusAnimation panzerschreck(ItemStack stack, GunAnimation type, int rail) {
        boolean empty = loaded(stack, rail) <= 0;
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(60, 0, 0, 0)
                    .addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case RELOAD -> new BusAnimation().addBus("RELOAD", seq().addPos(90, 0, 0, 750, IType.SIN_FULL)
                    .addPos(90, 0, 0, 1000).addPos(0, 0, 0, 750, IType.SIN_FULL))
                    .addBus("ROCKET", seq().addPos(0, -3, -6, 0).addPos(0, -3, -6, 750)
                            .addPos(0, 0, -6.5, 500, IType.SIN_DOWN).addPos(0, 0, 0, 350, IType.SIN_UP));
            case JAMMED, INSPECT -> new BusAnimation().addBus("RELOAD", seq().addPos(90, 0, 0, 750, IType.SIN_FULL)
                    .addPos(90, 0, 0, 500).addPos(0, 0, 0, 750, IType.SIN_FULL))
                    .addBus("ROCKET", seq().addPos(0, empty ? -3 : 0, 0, 0));
            default -> null;
        };
    }

    private static BusAnimation fatman(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(60, 0, 0, 0)
                    .addPos(0, 0, 0, 1000, IType.SIN_DOWN));
            case CYCLE -> new BusAnimation().addBus("GAUGE", seq().addPos(0, 0,
                    135 + Minecraft.getInstance().player.getRandom().nextInt(136),
                    100, IType.SIN_DOWN).addPos(0, 0, 0, 500, IType.SIN_DOWN))
                    .addBus("PISTON", seq().addPos(0, 0, 3, 100, IType.SIN_UP))
                    .addBus("NUKE", seq().addPos(0, 0, 3, 100, IType.SIN_UP).addPos(0, 0, 0, 0));
            case RELOAD -> new BusAnimation().addBus("LID", seq().addPos(0, 0, 0, 250)
                    .addPos(0, 0, -45, 250, IType.SIN_UP).addPos(0, 0, -45, 1200)
                    .addPos(0, 0, 0, 250, IType.SIN_UP)).addBus("HANDLE", seq().addPos(0, 0, -2, 500, IType.SIN_FULL)
                    .addPos(0, 0, -2, 1700).addPos(0, 0, 0, 750, IType.SIN_FULL))
                    .addBus("NUKE", seq().addPos(5, -4, 3, 0).addPos(5, -4, 3, 750)
                            .addPos(2, 0.5, 3, 500, IType.SIN_UP).addPos(1, 0.5, 3, 100)
                            .addPos(0, 0, 3, 100).addPos(0, 0, 3, 750).addPos(0, 0, 0, 750, IType.SIN_FULL))
                    .addBus("PISTON", seq().addPos(0, 0, 3, 0).addPos(0, 0, 3, 2200)
                            .addPos(0, 0, 0, 750, IType.SIN_FULL)).addBus("EQUIP", seq()
                            .addPos(5, 0, 0, 500, IType.SIN_FULL).addPos(0, 0, 0, 500, IType.SIN_FULL)
                            .addPos(0, 0, 0, 450).addPos(3, 0, 0, 100, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 100, IType.SIN_FULL).addPos(0, 0, 0, 500)
                            .addPos(-10, 0, 0, 375, IType.SIN_DOWN).addPos(0, 0, 0, 375, IType.SIN_UP));
            case JAMMED -> new BusAnimation().addBus("HANDLE", seq().addPos(0, 0, 0, 750)
                    .addPos(0, 0, -2, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL)
                    .addPos(0, 0, -2, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("EQUIP", seq().addPos(0, 0, 0, 500).addPos(-15, 0, 0, 250, IType.SIN_FULL)
                            .addPos(-15, 0, 0, 1000).addPos(0, 0, 0, 250, IType.SIN_FULL));
            case INSPECT -> new BusAnimation().addBus("HANDLE", seq().addPos(0, 0, 0, 250)
                    .addPos(0, 0, -2, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL)
                    .addPos(0, 0, -2, 250, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("EQUIP", seq().addPos(-15, 0, 0, 250, IType.SIN_FULL)
                            .addPos(-15, 0, 0, 1000).addPos(0, 0, 0, 250, IType.SIN_FULL));
            default -> null;
        };
    }

    private static BusAnimation maresleg(ItemStack stack, GunAnimation type, int rail) {
        boolean empty = loaded(stack, rail) <= 0;
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(-60, 0, 0, 0)
                    .addPos(0, 0, -3, 500, IType.SIN_DOWN));
            case CYCLE -> new BusAnimation().addBus("RECOIL", seq().addPos(0, 0, 0, 50)
                    .addPos(0, 0, -1, 50).addPos(0, 0, 0, 250))
                    .addBus("SIGHT", seq().addPos(35, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("LEVER", seq().addPos(0, 0, 0, 600).addPos(-85, 0, 0, 200).addPos(0, 0, 0, 200))
                    .addBus("TURN", seq().addPos(0, 0, 0, 600).addPos(0, 0, 45, 200, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 200, IType.SIN_UP))
                    .addBus("HAMMER", seq().addPos(30, 0, 0, 50).addPos(30, 0, 0, 550).addPos(0, 0, 0, 200));
            case CYCLE_DRY -> new BusAnimation().addBus("LEVER", seq().addPos(0, 0, 0, 600)
                    .addPos(-90, 0, 0, 200).addPos(0, 0, 0, 200))
                    .addBus("TURN", seq().addPos(0, 0, 0, 600).addPos(0, 0, 45, 200, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 200, IType.SIN_UP))
                    .addBus("HAMMER", seq().addPos(30, 0, 0, 50).addPos(30, 0, 0, 550).addPos(0, 0, 0, 200));
            case RELOAD -> new BusAnimation().addBus("LIFT", seq().addPos(30, 0, 0, 400, IType.SIN_FULL))
                    .addBus("LEVER", seq().addPos(0, 0, 0, 400).addPos(-85, 0, 0, 200))
                    .addBus("SHELL", seq().addPos(0, 0, 0, 600).addPos(0, 0.25, -3, 0)
                            .addPos(0, empty ? 0.25 : 0.125, -1.5, 150, IType.SIN_UP)
                            .addPos(0, empty ? 0.25 : -0.25, 0, 150, IType.SIN_DOWN))
                    .addBus("FLAG", seq().addPos(0, 0, 0, empty ? 900 : 0).addPos(1, 1, 1, 0));
            case RELOAD_CYCLE -> new BusAnimation().addBus("LIFT", seq().addPos(30, 0, 0, 0))
                    .addBus("LEVER", seq().addPos(-85, 0, 0, 0))
                    .addBus("SHELL", seq().addPos(0, 0.25, -3, 0).addPos(0, 0.125, -1.5, 150, IType.SIN_UP)
                            .addPos(0, -0.125, 0, 150, IType.SIN_DOWN))
                    .addBus("FLAG", seq().addPos(1, 1, 1, 0));
            case RELOAD_END -> new BusAnimation().addBus("LIFT", seq().addPos(30, 0, 0, 0)
                    .addPos(30, 0, 0, 250).addPos(0, 0, 0, 400, IType.SIN_FULL))
                    .addBus("LEVER", seq().addPos(-85, 0, 0, 0).addPos(0, 0, 0, 200))
                    .addBus("FLAG", seq().addPos(1, 1, 1, 0));
            case JAMMED -> new BusAnimation().addBus("LIFT", seq().addPos(30, 0, 0, 0)
                    .addPos(30, 0, 0, 250).addPos(0, 0, 0, 400, IType.SIN_FULL))
                    .addBus("LEVER", seq().addPos(-85, 0, 0, 0).addPos(-15, 0, 0, 200)
                            .addPos(-15, 0, 0, 650).addPos(-85, 0, 0, 200).addPos(-15, 0, 0, 200)
                            .addPos(-15, 0, 0, 200).addPos(-85, 0, 0, 200).addPos(0, 0, 0, 200))
                    .addBus("TURN", seq().addPos(0, 0, 0, 850).addPos(0, 0, 45, 200, IType.SIN_DOWN)
                            .addPos(0, 0, 45, 800).addPos(0, 0, 0, 200, IType.SIN_UP))
                    .addBus("FLAG", seq().addPos(1, 1, 1, 0));
            case INSPECT -> new BusAnimation().addBus("LIFT", seq().addPos(-35, 0, 0, 300, IType.SIN_FULL)
                    .addPos(-35, 0, 0, 1150).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("TURN", seq().addPos(0, 0, 0, 450).addPos(0, 0, -90, 500, IType.SIN_FULL)
                            .addPos(0, 0, -90, 500).addPos(0, 0, 0, 500, IType.SIN_FULL));
            default -> null;
        };
    }

    private static BusAnimation mareslegShort(ItemStack stack, GunAnimation type, int rail) {
        if (type == GunAnimation.EQUIP) {
            return new BusAnimation().addBus("EQUIP", seq().addPos(-60, 0, 0, 0)
                    .addPos(0, 0, -3, 250, IType.SIN_DOWN));
        }
        if (type == GunAnimation.CYCLE || type == GunAnimation.CYCLE_DRY) {
            return maresleg(stack, type, rail).addBus("FLIP", seq().addPos(0, 0, 0, 600).addPos(360, 0, 0, 400))
                    .addBus("SHELL", seq().addPos(-20, 0, 0, 0));
        }
        return maresleg(stack, type, rail);
    }

    private static BusAnimation liberator(ItemStack stack, GunAnimation type, int rail) {
        int ammo = heldAmmo(stack, rail);
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(60, 0, 0, 0)
                    .addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case CYCLE -> new BusAnimation().addBus("RECOIL", seq().addPos(0, 0, -2.5, 50, IType.SIN_DOWN)
                    .addPos(0, 0, 0, 350, IType.SIN_FULL));
            case CYCLE_DRY -> new BusAnimation();
            // 1.7's RELOAD case falls through to RELOAD_CYCLE, which has no row for a full chamber either
            case RELOAD -> ammo > 3 ? null : liberatorLoad(ammo, true);
            case RELOAD_CYCLE -> ammo > 2 ? null : liberatorLoad(ammo + 1, false);
            case RELOAD_END -> liberatorShells(new BusAnimation()
                    .addBus("LATCH", seq().addPos(15, 0, 0, 0).addPos(15, 0, 0, 250).addPos(0, 0, 0, 50))
                    .addBus("BREAK", seq().addPos(60, 0, 0, 0).addPos(0, 0, 0, 250, IType.SIN_UP)), ammo + 1);
            case JAMMED -> liberatorShells(new BusAnimation()
                    .addBus("LATCH", seq().addPos(15, 0, 0, 0).addPos(15, 0, 0, 250).addPos(0, 0, 0, 50)
                            .addPos(0, 0, 0, 550).addPos(15, 0, 0, 100).addPos(15, 0, 0, 600).addPos(0, 0, 0, 50))
                    .addBus("BREAK", seq().addPos(60, 0, 0, 0).addPos(0, 0, 0, 250, IType.SIN_UP).addPos(0, 0, 0, 600)
                            .addPos(45, 0, 0, 250, IType.SIN_DOWN).addPos(45, 0, 0, 300).addPos(0, 0, 0, 150, IType.SIN_UP)),
                    ammo + 1);
            case INSPECT -> liberatorShells(new BusAnimation()
                    .addBus("LATCH", seq().addPos(15, 0, 0, 100).addPos(15, 0, 0, 1100).addPos(0, 0, 0, 50))
                    .addBus("BREAK", seq().addPos(0, 0, 0, 100).addPos(60, 0, 0, 350, IType.SIN_DOWN)
                            .addPos(60, 0, 0, 500).addPos(0, 0, 0, 250, IType.SIN_UP)), ammo);
            default -> null;
        };
    }

    /**
     * 1.7's reload rows: shells below {@code loading} sit in the barrel, shell {@code loading} slides in from below
     * and the rest wait below. The first row also breaks the action open; the cycle rows start open.
     */
    private static BusAnimation liberatorLoad(int loading, boolean opening) {
        BusAnimation animation = new BusAnimation()
                .addBus("LATCH", opening ? seq().addPos(15, 0, 0, 100) : seq().addPos(15, 0, 0, 0))
                .addBus("BREAK", opening ? seq().addPos(0, 0, 0, 100).addPos(60, 0, 0, 350, IType.SIN_DOWN)
                        : seq().addPos(60, 0, 0, 0));
        for (int shell = 0; shell < 4; shell++) {
            BusAnimationSequence seq;
            if (shell < loading) seq = seq().addPos(0, 0, 0, 0);
            else if (shell == loading && opening) seq = seq().addPos(2, -4, -2, 0).addPos(2, -4, -2, 400)
                    .addPos(0, 0, -2, 450, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP);
            else if (shell == loading) seq = seq().addPos(2, -4, -2, 0)
                    .addPos(0, 0, -2, 450, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP);
            else seq = seq().addPos(2, -4, -2, 0);
            animation.addBus("SHELL" + (shell + 1), seq);
        }
        return animation;
    }

    /** The first {@code chambered} shells sit in the barrel, the others hang out of sight below it. */
    private static BusAnimation liberatorShells(BusAnimation animation, int chambered) {
        for (int shell = 0; shell < 4; shell++) {
            animation.addBus("SHELL" + (shell + 1), shell < chambered ? seq().addPos(0, 0, 0, 0) : seq().addPos(2, -8, -2, 0));
        }
        return animation;
    }

    private static BusAnimation flamer(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(-45, 0, 0, 0)
                    .addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case RELOAD -> resource("flamethrower", type, Map.of(GunAnimation.RELOAD, "Reload"));
            case INSPECT, JAMMED -> new BusAnimation().addBus("ROTATE", seq().addPos(0, 0, 45, 250, IType.SIN_FULL)
                    .addPos(0, 0, 45, 350).addPos(0, 0, -15, 150, IType.SIN_FULL)
                    .addPos(0, 0, 0, 100, IType.SIN_FULL));
            default -> null;
        };
    }

    private static BusAnimation flaregun(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(-90, 0, 0, 0)
                    .addPos(0, 0, 0, 350, IType.SIN_DOWN));
            case CYCLE -> new BusAnimation().addBus("RECOIL", seq().addPos(0, 0, 0, 50)
                    .addPos(0, 0, -3, 50).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("HAMMER", seq().addPos(15, 0, 0, 50).addPos(15, 0, 0, 550)
                            .addPos(0, 0, 0, 100));
            case CYCLE_DRY -> new BusAnimation().addBus("HAMMER", seq().addPos(15, 0, 0, 50)
                    .addPos(15, 0, 0, 550).addPos(0, 0, 0, 100));
            case RELOAD -> new BusAnimation().addBus("OPEN", seq().addPos(45, 0, 0, 200, IType.SIN_FULL)
                    .addPos(45, 0, 0, 750).addPos(0, 0, 0, 200, IType.SIN_UP))
                    .addBus("SHELL", seq().addPos(4, -8, -4, 0).addPos(4, -8, -4, 200)
                            .addPos(0, 0, -5, 500, IType.SIN_DOWN).addPos(0, 0, 0, 200, IType.SIN_UP))
                    .addBus("FLIP", seq().addPos(0, 0, 0, 200).addPos(25, 0, 0, 200, IType.SIN_DOWN)
                            .addPos(25, 0, 0, 800).addPos(0, 0, 0, 200, IType.SIN_DOWN));
            case JAMMED -> new BusAnimation().addBus("OPEN", seq().addPos(0, 0, 0, 500)
                    .addPos(45, 0, 0, 200, IType.SIN_FULL).addPos(45, 0, 0, 500)
                    .addPos(0, 0, 0, 200, IType.SIN_UP))
                    .addBus("FLIP", seq().addPos(0, 0, 0, 500).addPos(0, 0, 0, 200)
                            .addPos(25, 0, 0, 200, IType.SIN_DOWN).addPos(25, 0, 0, 550)
                            .addPos(0, 0, 0, 200, IType.SIN_DOWN));
            case INSPECT -> new BusAnimation().addBus("FLIP", seq().addPos(-1080, 0, 0, 1500, IType.SIN_FULL));
            default -> null;
        };
    }

    private static BusAnimation quadro(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(60, 0, 0, 0)
                    .addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case CYCLE -> new BusAnimation().addBus("RECOIL", seq().addPos(0, 0, -0.5, 50).addPos(0, 0, 0, 50));
            case RELOAD -> new BusAnimation().addBus("RELOAD_ROTATE", seq().addPos(0, 0, 60, 500, IType.SIN_FULL)
                    .addPos(0, 0, 60, 1500).addPos(0, 0, 0, 750, IType.SIN_FULL))
                    .addBus("RELOAD_PUSH", seq().addPos(-1, -1, 0, 0).addPos(-1, -1, 0, 500)
                            .addPos(-1, 0, 0, 350).addPos(0, 0, 0, 1000));
            case JAMMED, INSPECT -> new BusAnimation().addBus("RELOAD_ROTATE", seq().addPos(0, 0, 60, 750, IType.SIN_FULL)
                    .addPos(0, 0, 60, 500).addPos(0, 0, 0, 750, IType.SIN_FULL));
            default -> null;
        };
    }

    private static BusAnimation missileLauncher(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(60, 0, 0, 0)
                    .addPos(0, 0, 0, 1000, IType.SIN_DOWN));
            case RELOAD -> new BusAnimation().addBus("BARREL", seq().addPos(0, 0, 1.5, 150)
                    .addPos(0, 0, 1.5, 2100).addPos(0, 0, 0, 150))
                    .addBus("OPEN", seq().addPos(0, 0, 0, 250).addPos(90, 0, 0, 500, IType.SIN_FULL)
                            .addPos(90, 0, 0, 1000).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("EQUIP", seq().addPos(0, 0, 0, 2250).addPos(-1, 0, 0, 150, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 150, IType.SIN_UP))
                    .addBus("MISSILE", seq().addPos(-10, 0, 0, 0).addPos(-10, 0, 0, 750)
                            .addPos(3, 0, 2, 0).addPos(0, 0, -6, 350, IType.SIN_FULL)
                            .addPos(0, 0, 0, 350, IType.SIN_UP));
            case JAMMED, INSPECT -> new BusAnimation().addBus("BARREL", seq().addPos(0, 0, 1.5, 150)
                    .addPos(0, 0, 1.5, 1350).addPos(0, 0, 0, 150))
                    .addBus("OPEN", seq().addPos(0, 0, 0, 250).addPos(90, 0, 0, 500, IType.SIN_FULL)
                            .addPos(90, 0, 0, 250).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("EQUIP", seq().addPos(0, 0, 0, 1500).addPos(-1, 0, 0, 150, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 150, IType.SIN_UP));
            default -> null;
        };
    }

    private static BusAnimation folly(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(-60, 0, 0, 0)
                    .addPos(5, 0, 0, 1500, IType.SIN_DOWN).addPos(0, 0, 0, 500, IType.SIN_FULL));
            case CYCLE -> new BusAnimation().addBus("RECOIL", seq().addPos(0, 0, -4.5, 50)
                    .addPos(0, 0, -4.5, 500).addPos(0, 0, 0, 500, IType.SIN_UP))
                    .addBus("LOAD", seq().addPos(0, 0, 0, 50).addPos(-25, 0, 0, 250, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 1000, IType.SIN_FULL));
            case RELOAD -> new BusAnimation().addBus("LOAD", seq().addPos(60, 0, 0, 1000, IType.SIN_FULL)
                    .addPos(60, 0, 0, 6000).addPos(0, 0, 0, 1000, IType.SIN_FULL))
                    .addBus("SCREW", seq().addPos(0, 0, 0, 1000).addPos(0, 0, -135, 1000, IType.SIN_FULL)
                            .addPos(0, 0, -135, 4000).addPos(0, 0, 0, 1000, IType.SIN_FULL))
                    .addBus("BREECH", seq().addPos(0, 0, 0, 1000).addPos(0, 0, -0.5, 1000, IType.SIN_FULL)
                            .addPos(0, -4, -0.5, 1000, IType.SIN_FULL).addPos(0, -4, -0.5, 2000)
                            .addPos(0, 0, -0.5, 1000, IType.SIN_FULL).addPos(0, 0, 0, 1000, IType.SIN_FULL))
                    .addBus("SHELL", seq().addPos(0, -4, -4.5, 0).addPos(0, -4, -4.5, 3000)
                            .addPos(0, 0, -4.5, 1000, IType.SIN_FULL).addPos(0, 0, 0, 500, IType.SIN_UP));
            default -> null;
        };
    }

    private static BusAnimation doubleBarrel(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(-60, 0, 0, 0)
                    .addPos(0, 0, -3, 500, IType.SIN_DOWN));
            case CYCLE -> new BusAnimation().addBus("RECOIL", seq().addPos(0, 0, -1, 50).addPos(0, 0, 0, 250))
                    .addBus("BUCKLE", seq().addPos(0, -60, 0, 50).addPos(0, 0, 0, 250));
            case RELOAD -> new BusAnimation().addBus("TURN", seq().addPos(0, 30, 0, 350, IType.SIN_FULL)
                    .addPos(0, 30, 0, 1150).addPos(0, 0, 0, 350, IType.SIN_FULL))
                    .addBus("LEVER", seq().addPos(0, 0, 0, 250).addPos(0, 0, -90, 100, IType.SIN_FULL)
                            .addPos(0, 0, -90, 1300).addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("BARREL", seq().addPos(0, 0, 0, 300).addPos(60, 0, 0, 150, IType.SIN_UP)
                            .addPos(60, 0, 0, 1150).addPos(0, 0, 0, 150, IType.SIN_UP))
                    .addBus("LIFT", seq().addPos(0, 0, 0, 350).addPos(-5, 0, 0, 150, IType.SIN_FULL)
                            .addPos(0, 0, 0, 100, IType.SIN_FULL).addPos(0, 0, 0, 700)
                            .addPos(-5, 0, 0, 100, IType.SIN_FULL).addPos(0, 0, 0, 100, IType.SIN_UP)
                            .addPos(45, 0, 0, 150).addPos(45, 0, 0, 150).addPos(-5, 0, 0, 150, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("SHELLS", seq().addPos(0, 0, 0, 450).addPos(0, 0, -2.5, 100)
                            .addPos(0, -5, -5, 350, IType.SIN_DOWN).addPos(0, -3, -2, 0)
                            .addPos(0, 0, -2, 250).addPos(0, 0, 0, 150, IType.SIN_UP))
                    .addBus("SHELL_FLIP", seq().addPos(0, 0, 0, 450).addPos(-360, 0, 0, 450));
            case INSPECT -> new BusAnimation().addBus("LEVER", seq().addPos(0, 0, 0, 250)
                    .addPos(0, 0, -90, 100, IType.SIN_FULL).addPos(0, 0, -90, 800)
                    .addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("BARREL", seq().addPos(0, 0, 0, 300).addPos(60, 0, 0, 150, IType.SIN_UP)
                            .addPos(60, 0, 0, 650).addPos(0, 0, 0, 150, IType.SIN_UP));
            default -> null;
        };
    }

    /** Held-ammo count for the primary magazine, as the source lambdas read it on the client. */
    private static int heldAmmo(ItemStack stack, int rail) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        LocalPlayer player = Minecraft.getInstance().player;
        return gun.getPrimaryMagazine(stack, rail).getAmount(stack, player == null ? null : player.getInventory());
    }

    /** Source XFactory762mm.LAMBDA_CARBINE_ANIMS. */
    private static BusAnimation carbine(ItemStack stack, GunAnimation type, int rail) {
        int ammo = heldAmmo(stack, 0);
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
            case CYCLE -> new BusAnimation()
                    .addBus("RECOIL", seq().addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.25 : -0.5, 50, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("SLIDE", seq().addPos(0, 0, -1, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_UP))
                    .addBus(ammo <= 1 ? "NULL" : "REL", seq().addPos(0, 0, 0.25, 50).addPos(0, 0.125, 1.25, 100, IType.SIN_UP));
            case CYCLE_DRY -> new BusAnimation()
                    .addBus("SLIDE", seq().addPos(0, 0, 0, 500).addPos(0, 0, -1, 100, IType.SIN_DOWN)
                            .addPos(0, 0, -1, 50).addPos(0, 0, 0, 100, IType.SIN_UP));
            case RELOAD -> new BusAnimation()
                    .addBus("MAG", seq().addPos(0, -4, 0, 250, IType.SIN_UP).addPos(0, -4, 0, 750).addPos(0, 0, 0, 500, IType.SIN_DOWN))
                    .addBus("LIFT", seq().addPos(0, 0, 0, 500).addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 1000))
                    .addBus("BULLET", seq().addPos(ammo == 0 ? 1 : 0, 0, 0, 0).addPos(0, 0, 0, 1000));
            case RELOAD_END -> new BusAnimation()
                    .addBus("LIFT", seq().addPos(-25, 0, 0, 0).addPos(-25, 0, 0, 750).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("SLIDE", seq().addPos(0, 0, 0, 250).addPos(0, 0, -1, 100, IType.SIN_DOWN)
                            .addPos(0, 0, -1, 50).addPos(0, 0, 0, 100, IType.SIN_UP))
                    .addBus("REL", seq().addPos(0, 0, 0, 250).addPos(0, 0, 0.25, 150).addPos(0, 0.125, 1.25, 100, IType.SIN_UP));
            case JAMMED -> new BusAnimation()
                    .addBus("LIFT", seq().addPos(-25, 0, 0, 0).addPos(-25, 0, 0, 750).addPos(0, 0, 0, 500, IType.SIN_FULL)
                            .addPos(0, 0, 0, 250).addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 750)
                            .addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("SLIDE", seq().addPos(0, 0, 0, 250).addPos(0, 0, -1, 100, IType.SIN_DOWN).addPos(0, 0, -1, 50)
                            .addPos(0, 0, -0.25, 100, IType.SIN_UP).addPos(0, 0, -0.25, 1250)
                            .addPos(0, 0, -1, 100, IType.SIN_DOWN).addPos(0, 0, -1, 50).addPos(0, 0, 0, 100, IType.SIN_UP))
                    .addBus("REL", seq().addPos(0, 0, 0, 250).addPos(0, 0, 0.25, 150).addPos(0, 0.125, 1, 100, IType.SIN_UP)
                            .addPos(0, 0.125, 1, 1250).addPos(0, 0.125, 0.25, 100, IType.SIN_DOWN)
                            .addPos(0, 0.125, 1, 100, IType.SIN_UP));
            case INSPECT -> new BusAnimation()
                    .addBus("LIFT", seq().addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 1500).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("SLIDE", seq().addPos(0, 0, 0, 500).addPos(0, 0, -0.75, 150, IType.SIN_DOWN)
                            .addPos(0, 0, -0.75, 1000).addPos(0, 0, 0, 100, IType.SIN_UP))
                    .addBus(ammo == 0 ? "NULL" : "REL", seq().addPos(0, 0.125, 1.25, 0).addPos(0, 0.125, 1.25, 500)
                            .addPos(0, 0.125, 0.5, 150, IType.SIN_DOWN).addPos(0, 0.125, 0.5, 1000)
                            .addPos(0, 0.125, 1.25, 100, IType.SIN_UP));
            default -> null;
        };
    }

    /** Source XFactory762mm.LAMBDA_MAS36_ANIMS. */
    private static BusAnimation mas36(ItemStack stack, GunAnimation type, int rail) {
        int mag = heldAmmo(stack, 0);
        double turn = -90;
        double pullAmount = ItemGunBaseNT.getIsAiming(stack) ? -1F : -1.5D;
        return switch (type) {
            case EQUIP -> new BusAnimation()
                    .addBus("STOCK", seq().setPos(-158, 0, 0).hold(500).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("EQUIP", seq().setPos(45, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL).hold(500)
                            .addPos(1, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL));
            case CYCLE -> new BusAnimation()
                    .addBus("RECOIL", seq().addPos(0, 0, -0.5, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("BOLT_TURN", seq().hold(250).addPos(0, 0, turn, 150).hold(700).addPos(0, 0, 0, 150))
                    .addBus("BOLT_PULL", seq().hold(350).addPos(0, 0, pullAmount, 250, IType.SIN_UP).hold(250).addPos(0, 0, 0, 200, IType.LINEAR))
                    .addBus("LIFT", seq().hold(600).addPos(-3, 0, 0, 150, IType.SIN_DOWN).hold(300).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("BULLET", mag <= 1 ? seq().setPos(-100, 0, 0) : seq().hold(850).addPos(0, 0.1875, 1.5, 200, IType.LINEAR));
            case CYCLE_DRY -> new BusAnimation()
                    .addBus("BOLT_TURN", seq().hold(250).addPos(0, 0, turn, 150).hold(700).addPos(0, 0, 0, 150))
                    .addBus("BOLT_PULL", seq().hold(350).addPos(0, 0, pullAmount, 250, IType.SIN_UP).hold(250).addPos(0, 0, 0, 200, IType.LINEAR))
                    .addBus("LIFT", seq().hold(600).addPos(-3, 0, 0, 150, IType.SIN_DOWN).hold(300).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("BULLET", seq().setPos(-100, 0, 0));
            case RELOAD -> new BusAnimation()
                    .addBus("BOLT_TURN", seq().addPos(0, 0, turn, 150).holdUntil(2000).addPos(0, 0, 0, 150))
                    .addBus("BOLT_PULL", seq().hold(100).addPos(0, 0, -1.5D, 250, IType.SIN_UP).holdUntil(1800).addPos(0, 0, 0, 200, IType.LINEAR))
                    .addBus("BULLET", seq().setPos(-100, 0, 0).holdUntil(1200).setPos(0, 0, 0).hold(600).addPos(0, 0.1875, 1.5, 200, IType.LINEAR))
                    .addBus("LIFT", seq().hold(200).addPos(30, 0, 0, 500, IType.SIN_FULL).holdUntil(1200).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("SHOW_CLIP", seq().setPos(1, 1, 1))
                    .addBus("CLIP", seq().setPos(2, -3, 0).hold(250).addPos(0.5, 1, 0, 500, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 250, IType.SIN_FULL).hold(400).addPos(-0.5, 0.5, 0, 150).addPos(-3, -3, 0, 250, IType.SIN_UP))
                    .addBus("BULLETS", seq().setPos(2, -3, 0).hold(250).addPos(0.5, 1, 0, 500, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 250, IType.SIN_FULL).hold(150).addPos(0, -1.5, 0, 250, IType.SIN_DOWN));
            case JAMMED -> new BusAnimation()
                    .addBus("LIFT", seq().hold(250).addPos(-15, 0, 0, 500, IType.SIN_FULL).holdUntil(1650).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("BOLT_TURN", seq().hold(250).addPos(0, 0, turn, 150).holdUntil(1250).addPos(0, 0, 0, 150))
                    .addBus("BOLT_PULL", seq().hold(350).addPos(0, 0, pullAmount, 250, IType.SIN_UP).addPos(0, 0, 0, 200, IType.LINEAR)
                            .addPos(0, 0, pullAmount, 250, IType.SIN_UP).addPos(0, 0, 0, 200, IType.LINEAR));
            case INSPECT -> new BusAnimation()
                    .addBus("LIFT", seq().hold(350).addPos(-3, 0, 0, 150, IType.SIN_DOWN).holdUntil(1050).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("BOLT_TURN", seq().addPos(0, 0, turn, 150).holdUntil(1050).addPos(0, 0, 0, 150))
                    .addBus("BOLT_PULL", seq().hold(100).addPos(0, 0, -1D, 250, IType.SIN_UP).hold(500).addPos(0, 0, 0, 200, IType.LINEAR))
                    .addBus("BULLET", mag == 0 ? seq().setPos(-100, 0, 0) : seq().setPos(0, 0.1875, 1.5).hold(100)
                            .addPos(0, 0.125, 0.5, 250, IType.SIN_UP).hold(500).addPos(0, 0.1875, 1.5, 200, IType.LINEAR));
            default -> null;
        };
    }

    /** Source XFactory40mm.LAMBDA_MK108_ANIMS. */
    private static BusAnimation mk108(ItemStack stack, GunAnimation type, int rail) {
        switch (type) {
            case EQUIP:
                return new BusAnimation().addBus("EQUIP", seq().setPos(45, 0, 0).addPos(0, 0, 0, 1000, IType.SIN_DOWN));
            case CYCLE:
                int amount = ((ItemGunBaseNT) stack.getItem()).getPrimaryMagazine(stack, 0).getAmount(stack, null);
                return new BusAnimation()
                        .addBus("RECOIL", seq().hold(50).addPos(0, 0, -0.25, 100, IType.SIN_DOWN).addPos(0, 0, 0, 150, IType.SIN_FULL))
                        .addBus("BARREL", seq().addPos(0, 0, -1, 100, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL))
                        .addBus("CYCLE", seq().addPos(0, 0, 0, 100).addPos(1, 0, 0, 150))
                        .addBus("SHELLS", seq().setPos(amount - 1, 0, 0));
            case CYCLE_DRY:
                return new BusAnimation().addBus("HAMMER", seq().addPos(15, 0, 0, 50).addPos(15, 0, 0, 550).addPos(0, 0, 0, 100));
            case RELOAD:
                return new BusAnimation()
                        .addBus("LIFT", seq().addPos(10, 0, 0, 500, IType.SIN_FULL).holdUntil(1250).addPos(-50, 0, 0, 750, IType.SIN_FULL)
                                .holdUntil(5500).addPos(0, 0, 0, 500, IType.SIN_FULL).hold(500).addPos(1, 0, 0, 100, IType.SIN_UP)
                                .addPos(0, 0, 0, 150, IType.SIN_FULL))
                        .addBus("LID", seq().addPos(60, 0, 0, 500, IType.SIN_FULL).holdUntil(6000).addPos(0, 0, 0, 500, IType.SIN_UP))
                        .addBus("BELT", seq().setPos(1, 0, 0).hold(500).addPos(0, 0, 0, 750, IType.SIN_UP).holdUntil(4500).addPos(1, 0, 0, 750, IType.SIN_UP))
                        .addBus("DRUM", seq().hold(2000).addPos(2.5, 0, 0, 500, IType.SIN_DOWN).addPos(2.5, -2, -8, 500, IType.SIN_UP)
                                .setPos(4, -3, -8).addPos(2.5, 0, 0, 1000, IType.SIN_FULL).addPos(0, 0, 0, 500, IType.SIN_UP));
            case JAMMED:
                return new BusAnimation()
                        .addBus("LID", seq().hold(250).addPos(45, 0, 0, 500, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_UP))
                        .addBus("LIFT", seq().hold(1000).addPos(1, 0, 0, 100, IType.SIN_UP).addPos(0, 0, 0, 150, IType.SIN_FULL));
            case INSPECT:
                int yeetHorizontal = 750;
                int untilImpact = yeetHorizontal * 9 / 15;
                int delay = 250;
                int height = 6;
                int arcUp = untilImpact * 5 / 8;
                int arcDown = untilImpact * 3 / 8;
                BusAnimation inspect = new BusAnimation()
                        .addBus("LIFT", seq().hold(untilImpact).addPos(1, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_FULL)
                                .hold(delay - 150).addPos(1, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_FULL)
                                .hold(delay - 150).addPos(1, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_FULL));
                // The source spells GRENH1..3/GRENV1..3/GRENS1..3 out; each grenade is the same arc delayed by i*delay.
                for (int i = 0; i < 3; i++) {
                    int wait = delay * i;
                    inspect.addBus("GRENH" + (i + 1), seq().setPos(9, 0, 0).hold(wait).addPos(-6, 0, 0, yeetHorizontal))
                            .addBus("GRENV" + (i + 1), seq().setPos(0, -2, 0).hold(wait).addPos(0, height, 0, arcUp, IType.SIN_DOWN)
                                    .addPos(0, 2, 0, arcDown, IType.SIN_UP).addPos(0, 3, 0, yeetHorizontal - untilImpact, IType.SIN_DOWN))
                            .addBus("GRENS" + (i + 1), seq().hold(wait).addPos(360 * 2, 0, 0, untilImpact).setPos(0, 0, 0)
                                    .addPos(360, 0, 0, yeetHorizontal - untilImpact));
                }
                return inspect;
            default:
                return null;
        }
    }

    /** Source XFactory75Bolt.LAMBDA_BOLTER_ANIMS. */
    private static BusAnimation bolter(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case CYCLE -> new BusAnimation().addBus("RECOIL", seq().addPos(1, 0, 0, 25).addPos(0, 0, 0, 75));
            case RELOAD -> new BusAnimation()
                    .addBus("TILT", seq().addPos(1, 0, 0, 250).addPos(1, 0, 0, 1500).addPos(0, 0, 0, 250))
                    .addBus("MAG", seq().addPos(0, 0, 1, 500).addPos(1, 0, 1, 500).addPos(0, 0, 0, 500));
            case JAMMED -> new BusAnimation()
                    .addBus("TILT", seq().addPos(0, 0, 0, 500).addPos(1, 0, 0, 250).addPos(1, 0, 0, 700).addPos(0, 0, 0, 250))
                    .addBus("MAG", seq().addPos(0, 0, 0, 750).addPos(0.6, 0, 0, 250).addPos(0, 0, 0, 250));
            default -> null;
        };
    }

    private static BusAnimation shredder(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(60, 0, 0, 0)
                    .addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case CYCLE -> new BusAnimation().addBus("RECOIL", seq().addPos(0, 0, -1, 50, IType.SIN_DOWN)
                    .addPos(0, 0, 0, 150, IType.SIN_FULL))
                    .addBus("CYCLE", seq().addPos(0, 0, 0, 150).addPos(0, 0, 18, 100));
            case CYCLE_DRY -> new BusAnimation().addBus("CYCLE", seq().addPos(0, 0, 0, 150)
                    .addPos(0, 0, 18, 100));
            case RELOAD -> new BusAnimation().addBus("MAG", seq().addPos(0, -8, 0, 250, IType.SIN_UP)
                    .addPos(0, -8, 0, 1000).addPos(0, 0, 0, 300))
                    .addBus("LIFT", seq().addPos(0, 0, 0, 750).addPos(-25, 0, 0, 300, IType.SIN_FULL)
                            .addPos(-25, 0, 0, 500).addPos(-27, 0, 0, 100, IType.SIN_DOWN)
                            .addPos(-25, 0, 0, 100, IType.SIN_FULL).addPos(-25, 0, 0, 150)
                            .addPos(0, 0, 0, 300, IType.SIN_FULL));
            case JAMMED -> new BusAnimation().addBus("MAG", seq().addPos(0, 0, 0, 500)
                    .addPos(0, -2, 0, 150, IType.SIN_UP).addPos(0, 0, 0, 100))
                    .addBus("LIFT", seq().addPos(0, 0, 0, 750).addPos(-2, 0, 0, 100, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 100, IType.SIN_FULL));
            case INSPECT -> new BusAnimation().addBus("MAG", seq().addPos(0, -1, 0, 150)
                    .addPos(6, -1, 0, 150).addPos(6, 12, 0, 350, IType.SIN_DOWN)
                    .addPos(6, -2, 0, 350, IType.SIN_UP).addPos(6, -1, 0, 50)
                    .addPos(0, -1, 0, 150, IType.SIN_FULL).addPos(0, 0, 0, 150, IType.SIN_UP))
                    .addBus("SPEEN", seq().addPos(0, 0, 0, 300).addPos(360, 0, 0, 700))
                    .addBus("LIFT", seq().addPos(0, 0, 0, 1450).addPos(-2, 0, 0, 100, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 100, IType.SIN_FULL));
            default -> null;
        };
    }

    private static BusAnimation sexy(ItemStack stack, GunAnimation type, int rail) {
        int amount = loaded(stack, rail);
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(45, 0, 0, 0)
                    .addPos(0, 0, 0, 1000, IType.SIN_DOWN));
            case CYCLE -> new BusAnimation().addBus("RECOIL", seq().hold(50)
                    .addPos(0, 0, -0.25, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("BARREL", seq().addPos(0, 0, -1, 50, IType.SIN_DOWN).addPos(0, 0, 0, 150))
                    .addBus("CYCLE", seq().addPos(1, 0, 0, 150))
                    .addBus("HOOD", seq().hold(50).addPos(3, 0, 0, 50, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 50, IType.SIN_UP))
                    .addBus("SHELLS", seq().setPos(amount - 1, 0, 0));
            case CYCLE_DRY -> new BusAnimation().addBus("CYCLE", seq().addPos(0, 0, 18, 50));
            case RELOAD -> new BusAnimation().addBus("LOWER", seq().addPos(15, 0, 0, 500, IType.SIN_FULL)
                    .hold(2750).addPos(12, 0, 0, 100, IType.SIN_DOWN).addPos(15, 0, 0, 100, IType.SIN_FULL)
                    .hold(1050).addPos(18, 0, 0, 100, IType.SIN_DOWN).addPos(15, 0, 0, 100, IType.SIN_FULL)
                    .hold(300).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("LEVER", seq().addPos(0, 0, 1, 150).hold(4700).addPos(0, 0, 0, 150))
                    .addBus("HOOD", seq().hold(250).addPos(60, 0, 0, 500, IType.SIN_FULL)
                            .hold(3250).addPos(0, 0, 0, 500, IType.SIN_UP))
                    .addBus("BELT", seq().setPos(1, 0, 0).hold(750).addPos(0, 0, 0, 500, IType.SIN_UP)
                            .hold(2000).addPos(1, 0, 0, 500, IType.SIN_UP))
                    .addBus("MAG", seq().hold(1500).addPos(0, -1, 0, 250, IType.SIN_UP)
                            .addPos(2, -1, 0, 500, IType.SIN_UP).addPos(7, 1, 0, 250, IType.SIN_UP)
                            .addPos(15, 2, 0, 250).setPos(0, -2, 0).addPos(0, 0, 0, 500, IType.SIN_UP))
                    .addBus("MAGROT", seq().hold(2250).addPos(0, 0, -180, 500, IType.SIN_FULL)
                            .setPos(0, 0, 0));
            case INSPECT -> new BusAnimation().addBus("BOTTLE", seq().setPos(8, -8, -2)
                    .addPos(6, -4, -2, 500, IType.SIN_DOWN).addPos(3, -3, -5, 500, IType.SIN_FULL)
                    .addPos(3, -2, -5, 1000).addPos(4, -6, -2, 750, IType.SIN_FULL)
                    .addPos(6, -8, -2, 500, IType.SIN_UP))
                    .addBus("SIP", seq().setPos(25, 0, 0).hold(500)
                            .addPos(-90, 0, 0, 500, IType.SIN_FULL).addPos(-110, 0, 0, 1000)
                            .addPos(25, 0, 0, 750, IType.SIN_FULL));
            default -> null;
        };
    }

    /**
     * The break-action revolver. The reload is the interesting one: the latch flips up, the barrel
     * assembly tips forward, the whole gun rotates out of view to be reloaded and comes back: five
     * buses staggered against each other.
     */
    private static BusAnimation atlas(GunAnimation type) {
        return switch (type) {
            case EQUIP -> new BusAnimation()
                    .addBus("EQUIP", seq().addPos(-90, 0, 0, 0).addPos(0, 0, 0, 350, IType.SIN_DOWN));
            case CYCLE -> new BusAnimation()
                    .addBus("RECOIL", seq().addPos(0, 0, 0, 50).addPos(0, 0, -3, 50).addPos(0, 0, 0, 250))
                    .addBus("HAMMER", seq().addPos(0, 0, 1, 50).addPos(0, 0, 1, 550).addPos(0, 0, 0, 200))
                    .addBus("DRUM", seq().addPos(0, 0, 0, 600).addPos(0, 0, 1, 200));
            case CYCLE_DRY -> new BusAnimation()
                    .addBus("HAMMER", seq().addPos(0, 0, 1, 50).addPos(0, 0, 1, 550).addPos(0, 0, 0, 200))
                    .addBus("DRUM", seq().addPos(0, 0, 0, 600).addPos(0, 0, 1, 200));
            case RELOAD -> new BusAnimation()
                    .addBus("LATCH", seq().addPos(0, 0, 90, 300).addPos(0, 0, 90, 2000).addPos(0, 0, 0, 150))
                    .addBus("FRONT", seq().addPos(0, 0, 0, 200).addPos(0, 0, 45, 150).addPos(0, 0, 45, 2000).addPos(0, 0, 0, 75))
                    .addBus("RELOAD_ROT", seq().addPos(0, 0, 0, 300).addPos(60, 0, 0, 500).addPos(60, 0, 0, 500)
                            .addPos(0, -90, -90, 0).addPos(0, -90, -90, 600).addPos(0, 0, 0, 300)
                            .addPos(0, 0, 0, 100).addPos(-45, 0, 0, 50).addPos(-45, 0, 0, 100).addPos(0, 0, 0, 300))
                    .addBus("RELOAD_MOVE", seq().addPos(0, 0, 0, 300).addPos(0, -15, 0, 1000).addPos(0, 0, 0, 450))
                    .addBus("DRUM_PUSH", seq().addPos(0, 0, 0, 1600).addPos(0, 0, -5, 0).addPos(0, 0, 0, 300));
            case INSPECT -> new BusAnimation()
                    .addBus("LATCH", seq().addPos(0, 0, 90, 300).addPos(0, 0, 90, 1000).addPos(0, 0, 0, 150))
                    .addBus("FRONT", seq().addPos(0, 0, 0, 200).addPos(0, 0, 45, 150).addPos(0, 0, 45, 1000).addPos(0, 0, 0, 75))
                    .addBus("RELOAD_ROT", seq().addPos(0, 0, 0, 300).addPos(45, 0, 0, 500, IType.SIN_FULL)
                            .addPos(45, 0, 0, 500).addPos(-45, 0, 0, 50).addPos(-45, 0, 0, 100).addPos(0, 0, 0, 300))
                    .addBus("RELOAD_MOVE", seq().addPos(0, 0, 0, 300).addPos(0, -2.5, 0, 500, IType.SIN_FULL)
                            .addPos(0, -2.5, 0, 500).addPos(0, 0, 0, 350));
            case JAMMED -> new BusAnimation()
                    .addBus("LATCH", seq().addPos(0, 0, 0, 500).addPos(0, 0, 90, 300).addPos(0, 0, 90, 1000).addPos(0, 0, 0, 150))
                    .addBus("FRONT", seq().addPos(0, 0, 0, 500).addPos(0, 0, 0, 200).addPos(0, 0, 45, 150)
                            .addPos(0, 0, 45, 1000).addPos(0, 0, 0, 75))
                    .addBus("RELOAD_ROT", seq().addPos(0, 0, 0, 500).addPos(0, 0, 0, 300)
                            .addPos(45, 0, 0, 500, IType.SIN_FULL).addPos(45, 0, 0, 500)
                            .addPos(-45, 0, 0, 50).addPos(-45, 0, 0, 100).addPos(0, 0, 0, 300))
                    .addBus("RELOAD_MOVE", seq().addPos(0, 0, 0, 500).addPos(0, 0, 0, 300)
                            .addPos(0, -2.5, 0, 500, IType.SIN_FULL).addPos(0, -2.5, 0, 500).addPos(0, 0, 0, 350));
            default -> null;
        };
    }

    /** The debug gun: upstream's {@code LAMBDA_DEBUG_ANIMS}, a li'lpip variant. */
    private static BusAnimation debug(GunAnimation type) {
        return switch (type) {
            case CYCLE -> new BusAnimation()
                    .addBus("RECOIL", seq().addPos(0, 0, 0, 50).addPos(0, 0, -3, 50).addPos(0, 0, 0, 250))
                    .addBus("HAMMER", seq().addPos(0, 0, 1, 50).addPos(0, 0, 1, 400).addPos(0, 0, 0, 200))
                    .addBus("DRUM", seq().addPos(0, 0, 0, 450).addPos(0, 0, 1, 200));
            case CYCLE_DRY -> new BusAnimation()
                    .addBus("HAMMER", seq().addPos(0, 0, 1, 50).addPos(0, 0, 1, 400).addPos(0, 0, 0, 200))
                    .addBus("DRUM", seq().addPos(0, 0, 0, 450).addPos(0, 0, 1, 200));
            case EQUIP -> new BusAnimation()
                    .addBus("ROTATE", seq().addPos(-360, 0, 0, 350));
            case RELOAD -> new BusAnimation()
                    .addBus("RELAOD_TILT", seq().addPos(-15, 0, 0, 100).addPos(65, 0, 0, 100).addPos(45, 0, 0, 50)
                            .addPos(0, 0, 0, 200).addPos(0, 0, 0, 1450).addPos(-80, 0, 0, 100)
                            .addPos(-80, 0, 0, 100).addPos(0, 0, 0, 200))
                    .addBus("RELOAD_CYLINDER", seq().addPos(0, 0, 0, 200).addPos(90, 0, 0, 100)
                            .addPos(90, 0, 0, 1700).addPos(0, 0, 0, 70))
                    .addBus("RELOAD_LIFT", seq().addPos(0, 0, 0, 350).addPos(-45, 0, 0, 250).addPos(-45, 0, 0, 350)
                            .addPos(-15, 0, 0, 200).addPos(-15, 0, 0, 1050).addPos(0, 0, 0, 100))
                    .addBus("RELOAD_JOLT", seq().addPos(0, 0, 0, 600).addPos(2, 0, 0, 50).addPos(0, 0, 0, 100))
                    .addBus("RELOAD_BULLETS", seq().addPos(0, 0, 0, 650).addPos(10, 0, 0, 300)
                            .addPos(10, 0, 0, 200).addPos(0, 0, 0, 700))
                    .addBus("RELOAD_BULLETS_CON", seq().addPos(1, 0, 0, 0).addPos(1, 0, 0, 950).addPos(0, 0, 0, 1));
            case INSPECT, JAMMED -> new BusAnimation()
                    .addBus("RELAOD_TILT", seq().addPos(-15, 0, 0, 100).addPos(65, 0, 0, 100).addPos(45, 0, 0, 50)
                            .addPos(0, 0, 0, 200).addPos(0, 0, 0, 200).addPos(-80, 0, 0, 100)
                            .addPos(-80, 0, 0, 100).addPos(0, 0, 0, 200))
                    .addBus("RELOAD_CYLINDER", seq().addPos(0, 0, 0, 200).addPos(90, 0, 0, 100)
                            .addPos(90, 0, 0, 450).addPos(0, 0, 0, 70));
            default -> null;
        };
    }

    /** The Star-F family, including the magazine-dependent last-round animation. */
    private static BusAnimation star(ItemStack stack, GunAnimation type, int rail) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        int ammo = gun.getPrimaryMagazine(stack, rail).getAmount(stack,
                Minecraft.getInstance().player.getInventory());

        return switch (type) {
            case EQUIP -> new BusAnimation()
                    .addBus("EQUIP", seq().addPos(45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case CYCLE -> new BusAnimation()
                    .addBus("RECOIL", seq().addPos(0, 0, 0, 50)
                            .addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.125 : -0.5, 15, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 35, IType.SIN_FULL))
                    .addBus("SLIDE", seq().addPos(0, 0, 0, 50)
                            .addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.5 : -1, 25, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 75, IType.SIN_UP))
                    .addBus("HAMMER", seq().addPos(1, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 50, IType.SIN_DOWN))
                    .addBus("BULLET", ammo <= 1 ? seq().setPos(100, 0, 0)
                            : seq().addPos(0, 0, 0, 90).addPos(0, 0.5, 2.25, 50));
            case CYCLE_DRY -> new BusAnimation()
                    .addBus("HAMMER", seq().addPos(1, 0, 0, 50, IType.SIN_UP).hold(450)
                            .addPos(0, 0, 0, 50, IType.SIN_DOWN))
                    .addBus("SLIDE", seq().addPos(0, 0, 0, 500)
                            .addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.5 : -1, 100, IType.SIN_FULL)
                            .hold(100).addPos(0, 0, 0, 75, IType.SIN_UP))
                    .addBus("EQUIP", seq().addPos(0, 0, 0, 600).addPos(-3, 0, 0, 175, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("BULLET", seq().setPos(100, 0, 0));
            case RELOAD -> new BusAnimation()
                    .addBus("TILT", seq().addPos(-30, 0, 0, 250, IType.SIN_FULL).hold(1500)
                            .addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("SLIDE", seq().addPos(0, 0, 0, 250).addPos(0, 0, -1, 100, IType.SIN_FULL)
                            .hold(1125).addPos(0, 0, 0, 100, IType.SIN_UP))
                    .addBus("MAG", seq().addPos(0, 0, 0, 250).addPos(0, -7, -1.5, 300, IType.SIN_UP)
                            .hold(400).addPos(0, 0, 0, 300, IType.SIN_UP))
                    .addBus("EQUIP", seq().addPos(0, 0, 0, 500).addPos(3, 0, 0, 750, IType.SIN_FULL)
                            .addPos(-3, 0, 0, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("TURN", seq().addPos(0, 0, 0, 200).addPos(0, 0, 15, 300, IType.SIN_FULL)
                            .hold(900).addPos(0, 0, 0, 150, IType.SIN_FULL))
                    .addBus("BULLET", seq().setPos(ammo <= 1 ? 100 : 0, 0, 0).hold(750)
                            .setPos(0, 0, 0).hold(750).addPos(0, 0.5, 2.25, 50));
            case JAMMED -> new BusAnimation()
                    .addBus("TILT", seq().addPos(0, 0, 0, 500).addPos(-30, 0, 0, 150, IType.SIN_FULL)
                            .hold(800).addPos(0, 0, 0, 150, IType.SIN_FULL))
                    .addBus("TURN", seq().addPos(0, 0, 0, 500).addPos(0, 0, 25, 150, IType.SIN_FULL)
                            .hold(800).addPos(0, 0, 0, 150, IType.SIN_FULL))
                    .addBus("SLIDE", seq().addPos(0, 0, 0, 750).addPos(0, 0, -0.5, 100, IType.SIN_FULL)
                            .hold(100).addPos(0, 0, 0, 100, IType.SIN_UP).hold(100)
                            .addPos(0, 0, -0.5, 100, IType.SIN_FULL).hold(100)
                            .addPos(0, 0, 0, 100, IType.SIN_UP))
                    .addBus("BULLET", seq().setPos(0, 0.5, 2.25).hold(750)
                            .addPos(0, 0.5, 1.25, 100, IType.SIN_FULL).hold(100)
                            .addPos(0, 0.5, 2.25, 100, IType.SIN_UP).hold(100)
                            .addPos(0, 0.5, 1.25, 100, IType.SIN_FULL).hold(100)
                            .addPos(0, 0.5, 2.25, 100, IType.SIN_UP));
            case INSPECT -> new BusAnimation()
                    .addBus("TILT", seq().addPos(-30, 0, 0, 250, IType.SIN_FULL).hold(1500)
                            .addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("TURN", seq().addPos(0, 0, 25, 250, IType.SIN_FULL).hold(1500)
                            .addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("SLIDE", seq().addPos(0, 0, 0, 350).addPos(0, 0, -0.5, 100, IType.SIN_FULL)
                            .hold(1125).addPos(0, 0, 0, 100, IType.SIN_UP))
                    .addBus("BULLET", ammo <= 1 ? seq().setPos(100, 0, 0)
                            : seq().setPos(0, 0.5, 2.25).hold(350)
                            .addPos(0, 0.5, 1.25, 100, IType.SIN_FULL).hold(1125)
                            .addPos(0, 0.5, 2.25, 100, IType.SIN_UP));
            default -> null;
        };
    }

    /** The legacy AM180 table; the JSON/Bezier importer is not ported here. */
    private static BusAnimation am180Legacy(GunAnimation type) {
        return switch (type) {
            case EQUIP -> new BusAnimation()
                    .addBus("EQUIP", seq().addPos(45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
            case CYCLE -> new BusAnimation()
                    .addBus("RECOIL", seq().addPos(0, 0, 0, 50).addPos(0, 0, -0.25, 15, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 35, IType.SIN_FULL));
            case CYCLE_DRY -> new BusAnimation()
                    .addBus("BOLT", seq().addPos(0, 0, 0, 550).addPos(0, 0, -1.5, 100, IType.SIN_UP)
                            .addPos(0, 0, 0, 100, IType.SIN_UP))
                    .addBus("TURN", seq().addPos(0, 0, 0, 300).addPos(0, 0, 15, 250, IType.SIN_FULL)
                            .addPos(0, 0, 15, 400).addPos(0, 0, 0, 250, IType.SIN_FULL));
            case RELOAD -> new BusAnimation()
                    .addBus("MAGTURN", seq().addPos(15, 0, 0, 250, IType.SIN_FULL).addPos(15, 0, 0, 250)
                            .addPos(15, 0, 70, 300, IType.SIN_FULL).addPos(15, 0, 0, 0)
                            .addPos(15, 0, 0, 750).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("MAG", seq().addPos(0, 0, 0, 250).addPos(2, 0, -4, 250, IType.SIN_FULL)
                            .addPos(-10, 2, -4, 300, IType.SIN_UP).addPos(3, -6, -4, 0)
                            .addPos(2, 0, -4, 500, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("BOLT", seq().addPos(0, 0, 0, 2250).addPos(0, 0, -1.5, 100, IType.SIN_UP)
                            .addPos(0, 0, 0, 100, IType.SIN_UP))
                    .addBus("TURN", seq().addPos(0, 0, 0, 2000).addPos(0, 0, 15, 250, IType.SIN_FULL)
                            .addPos(0, 0, 15, 400).addPos(0, 0, 0, 250, IType.SIN_FULL));
            case JAMMED -> new BusAnimation()
                    .addBus("BOLT", seq().addPos(0, 0, 0, 750).addPos(0, 0, -1.5, 100, IType.SIN_UP)
                            .addPos(0, 0, 0, 100, IType.SIN_UP))
                    .addBus("TURN", seq().addPos(0, 0, 0, 500).addPos(0, 0, 45, 250, IType.SIN_FULL)
                            .addPos(0, 0, 45, 400).addPos(0, 0, 0, 250, IType.SIN_FULL));
            case INSPECT -> new BusAnimation()
                    .addBus("MAGTURN", seq().addPos(15, 0, 0, 250, IType.SIN_FULL).addPos(15, 0, 0, 1400)
                            .addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("MAG", seq().addPos(0, 0, 0, 200).addPos(4, -1, -4, 200, IType.SIN_FULL)
                            .addPos(4, -1.5, -4, 50).addPos(4, 0, -4, 100).addPos(4, 6, -4, 250, IType.SIN_DOWN)
                            .addPos(4, 0, -4, 150, IType.SIN_UP).addPos(4, -1, -4, 100, IType.SIN_DOWN)
                            .addPos(4, -1, -4, 250).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("MAGSPIN", seq().addPos(0, 0, 0, 600).addPos(-400, 0, 0, 500, IType.SIN_FULL)
                            .addPos(-400, 0, 0, 250).addPos(-360, 0, 0, 250));
            default -> null;
        };
    }

    /** The Henry lever-action table, ported from XFactory44 value for value. */
    private static BusAnimation henry(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case EQUIP -> new BusAnimation()
                    .addBus("EQUIP", seq().addPos(-90, 0, 0, 0).addPos(0, 0, -3, 350, IType.SIN_DOWN))
                    .addBus("SIGHT", seq().addPos(80, 0, 0, 0).addPos(80, 0, 0, 500)
                            .addPos(0, 0, -3, 250, IType.SIN_DOWN));
            case CYCLE -> new BusAnimation()
                    .addBus("RECOIL", seq().addPos(0, 0, 0, 50).addPos(0, 0, -1, 50).addPos(0, 0, 0, 250))
                    .addBus("SIGHT", seq().addPos(35, 0, 0, 100, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("LEVER", seq().addPos(0, 0, 0, 600).addPos(-90, 0, 0, 200)
                            .addPos(0, 0, 0, 200))
                    .addBus("TURN", seq().addPos(0, 0, 0, 600).addPos(0, 0, 45, 200, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 200, IType.SIN_UP))
                    .addBus("HAMMER", seq().addPos(30, 0, 0, 50).addPos(30, 0, 0, 550)
                            .addPos(0, 0, 0, 200));
            case CYCLE_DRY -> new BusAnimation()
                    .addBus("LEVER", seq().addPos(0, 0, 0, 600).addPos(-90, 0, 0, 200)
                            .addPos(0, 0, 0, 200))
                    .addBus("TURN", seq().addPos(0, 0, 0, 600).addPos(0, 0, 45, 200, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 200, IType.SIN_UP))
                    .addBus("HAMMER", seq().addPos(30, 0, 0, 50).addPos(30, 0, 0, 550)
                            .addPos(0, 0, 0, 200));
            case RELOAD -> new BusAnimation()
                    .addBus("LIFT", seq().addPos(-60, 0, 0, 400, IType.SIN_FULL))
                    .addBus("TWIST", seq().addPos(0, 0, 0, 500).addPos(0, 0, -90, 200, IType.SIN_FULL))
                    .addBus("BULLET", seq().addPos(0, 0, 0, 700).addPos(3, 0, -6, 0)
                            .addPos(0, 0, 1, 300, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_FULL));
            case RELOAD_CYCLE -> new BusAnimation()
                    .addBus("LIFT", seq().addPos(-60, 0, 0, 0))
                    .addBus("TWIST", seq().addPos(0, 0, -90, 0))
                    .addBus("BULLET", seq().addPos(3, 0, -6, 0).addPos(0, 0, 1, 300, IType.SIN_FULL)
                            .addPos(0, 0, 0, 250, IType.SIN_FULL));
            case RELOAD_END -> {
                boolean empty = ((ItemGunBaseNT) stack.getItem()).getPrimaryMagazine(stack, 0)
                        .getAmountBeforeReload(stack) <= 0;
                yield new BusAnimation()
                        .addBus("LIFT", seq().addPos(-60, 0, 0, 0).addPos(-60, 0, 0, 300)
                                .addPos(0, 0, 0, 400, IType.SIN_FULL))
                        .addBus("TWIST", seq().addPos(0, 0, -90, 0).addPos(0, 0, 0, 200, IType.SIN_FULL))
                        .addBus("LEVER", seq().addPos(0, 0, 0, 700).addPos(empty ? -90 : 0, 0, 0, 200)
                                .addPos(0, 0, 0, 200))
                        .addBus("TURN", seq().addPos(0, 0, 0, 700).addPos(0, 0, empty ? 45 : 0, 200, IType.SIN_DOWN)
                                .addPos(0, 0, 0, 200, IType.SIN_UP));
            }
            case JAMMED -> new BusAnimation()
                    .addBus("LIFT", seq().addPos(-60, 0, 0, 0).addPos(-60, 0, 0, 300)
                            .addPos(0, 0, 0, 400, IType.SIN_FULL))
                    .addBus("TWIST", seq().addPos(0, 0, -90, 0).addPos(0, 0, 0, 200, IType.SIN_FULL))
                    .addBus("LEVER", seq().addPos(0, 0, 0, 700).addPos(-90, 0, 0, 200)
                            .addPos(0, 0, 0, 200).addPos(0, 0, 0, 500).addPos(-90, 0, 0, 200)
                            .addPos(0, 0, 0, 200).addPos(0, 0, 0, 200).addPos(-90, 0, 0, 200)
                            .addPos(0, 0, 0, 200))
                    .addBus("TURN", seq().addPos(0, 0, 0, 700).addPos(0, 0, 45, 200, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 200, IType.SIN_UP).addPos(0, 0, 0, 500)
                            .addPos(0, 0, 45, 200, IType.SIN_FULL).addPos(0, 0, 45, 600)
                            .addPos(0, 0, 0, 200, IType.SIN_FULL));
            case INSPECT -> new BusAnimation()
                    .addBus("YEET", seq().addPos(0, 2, 0, 200, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 200, IType.SIN_UP))
                    .addBus("ROLL", seq().addPos(0, 0, 360, 400));
            default -> null;
        };
    }

    /** The heavy revolver table, ported from XFactory44's NOPIP animation. */
    private static BusAnimation nopip(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case CYCLE -> new BusAnimation()
                    .addBus("RECOIL", seq().addPos(0, 0, 0, 50).addPos(0, 0, -3, 50).addPos(0, 0, 0, 250))
                    .addBus("HAMMER", seq().addPos(0, 0, 1, 50).addPos(0, 0, 1, 400).addPos(0, 0, 0, 200))
                    .addBus("DRUM", seq().addPos(0, 0, 0, 450).addPos(0, 0, 1, 200));
            case CYCLE_DRY -> new BusAnimation()
                    .addBus("HAMMER", seq().addPos(0, 0, 1, 50).addPos(0, 0, 1, 400).addPos(0, 0, 0, 200))
                    .addBus("DRUM", seq().addPos(0, 0, 0, 450).addPos(0, 0, 1, 200));
            case EQUIP -> new BusAnimation().addBus("ROTATE", seq().addPos(90, 0, 0, 0)
                    .addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case RELOAD -> new BusAnimation()
                    .addBus("RELAOD_TILT", seq().addPos(-15, 0, 0, 100).addPos(65, 0, 0, 100)
                            .addPos(45, 0, 0, 50).addPos(0, 0, 0, 200).addPos(0, 0, 0, 1450)
                            .addPos(-80, 0, 0, 100).addPos(-80, 0, 0, 100).addPos(0, 0, 0, 200))
                    .addBus("RELOAD_CYLINDER", seq().addPos(0, 0, 0, 200).addPos(90, 0, 0, 100)
                            .addPos(90, 0, 0, 1700).addPos(0, 0, 0, 70))
                    .addBus("RELOAD_LIFT", seq().addPos(0, 0, 0, 350).addPos(-45, 0, 0, 250)
                            .addPos(-45, 0, 0, 350).addPos(-15, 0, 0, 200).addPos(-15, 0, 0, 1050)
                            .addPos(0, 0, 0, 100))
                    .addBus("RELOAD_JOLT", seq().addPos(0, 0, 0, 600).addPos(2, 0, 0, 50)
                            .addPos(0, 0, 0, 100))
                    .addBus("RELOAD_BULLETS", seq().addPos(0, 0, 0, 650).addPos(10, 0, 0, 300)
                            .addPos(10, 0, 0, 200).addPos(0, 0, 0, 700))
                    .addBus("RELOAD_BULLETS_CON", seq().addPos(1, 0, 0, 0).addPos(1, 0, 0, 950)
                            .addPos(0, 0, 0, 1));
            case INSPECT, JAMMED -> new BusAnimation()
                    .addBus("RELAOD_TILT", seq().addPos(-15, 0, 0, 100).addPos(65, 0, 0, 100)
                            .addPos(45, 0, 0, 50).addPos(0, 0, 0, 200).addPos(0, 0, 0, 200)
                            .addPos(-80, 0, 0, 100).addPos(-80, 0, 0, 100).addPos(0, 0, 0, 200))
                    .addBus("RELOAD_CYLINDER", seq().addPos(0, 0, 0, 200).addPos(90, 0, 0, 100)
                            .addPos(90, 0, 0, 450).addPos(0, 0, 0, 70));
            default -> null;
        };
    }

    private static BusAnimation lilmac(ItemStack stack, GunAnimation type, int rail) {
        if (type == GunAnimation.EQUIP) return new BusAnimation().addBus("SPIN", seq().addPos(-360, 0, 0, 350));
        return nopip(stack, type, rail);
    }

    /** The Hangman table, ported from XFactory44 value for value. */
    private static BusAnimation hangman(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(60, 0, 0, 0)
                    .addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case CYCLE -> new BusAnimation().addBus("RECOIL", seq().addPos(0, 0, 0, 50)
                    .addPos(0, 0, -3, 50).addPos(0, 0, 0, 250));
            case RELOAD -> new BusAnimation()
                    .addBus("LID", seq().addPos(0, 0, -90, 250).addPos(0, 0, -90, 1500)
                            .addPos(0, 0, 0, 250))
                    .addBus("MAG", seq().addPos(0, 0, 0, 250).addPos(0, -10, 0, 250, IType.SIN_UP)
                            .addPos(0, -10, 0, 500).addPos(0, 0, 0, 350, IType.SIN_FULL))
                    .addBus("BULLETS", seq().addPos(1, 1, 1, 0).addPos(0, 0, 0, 500))
                    .addBus("EQUIP", seq().addPos(-15, 0, 0, 500, IType.SIN_FULL).addPos(-15, 0, 0, 850)
                            .addPos(-25, 0, 0, 100, IType.SIN_DOWN).addPos(0, 0, 0, 350, IType.SIN_FULL))
                    .addBus("ROLL", seq().addPos(0, 0, 0, 500).addPos(0, 0, 25, 250, IType.SIN_FULL)
                            .addPos(0, 0, 25, 1000).addPos(0, 0, 0, 250, IType.SIN_FULL));
            case INSPECT -> new BusAnimation()
                    .addBus("TURN", seq().addPos(0, 170, 0, 500, IType.SIN_UP).addPos(0, 170, 0, 550)
                            .addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("ROLL", seq().addPos(0, 0, 110, 500, IType.SIN_FULL).addPos(0, 0, 110, 550)
                            .addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("SMACK", seq().addPos(0, 0, 0, 500).addPos(0, 0, 1, 150, IType.SIN_DOWN)
                            .addPos(0, 0, -3, 150, IType.SIN_UP).addPos(0, 0, 0, 350, IType.SIN_FULL));
            case JAMMED -> new BusAnimation()
                    .addBus("LID", seq().addPos(0, 0, 0, 500).addPos(0, 0, -90, 250)
                            .addPos(0, 0, -90, 300).addPos(0, 0, 0, 250))
                    .addBus("MAG", seq().addPos(0, 0, 0, 500).addPos(0, 0, 0, 250)
                            .addPos(0, -3, 0, 150, IType.SIN_UP).addPos(0, 0, 0, 150, IType.SIN_FULL))
                    .addBus("EQUIP", seq().addPos(0, 0, 0, 1000).addPos(-10, 0, 0, 100, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 350, IType.SIN_FULL))
                    .addBus("ROLL", seq().addPos(0, 0, 0, 500).addPos(0, 0, 25, 250, IType.SIN_FULL)
                            .addPos(0, 0, 25, 300).addPos(0, 0, 0, 250, IType.SIN_FULL));
            default -> null;
        };
    }

    /** The Uzi family, including the animated folding stocks and last-round magazine cue. */
    private static BusAnimation uzi(ItemStack stack, GunAnimation type, int rail) {
        boolean empty = loaded(stack, rail) <= 0;
        return switch (type) {
            case EQUIP -> new BusAnimation()
                    .addBus("EQUIP", seq().addPos(80, 0, 0, 0).addPos(80, 0, 0, 500)
                            .addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("STOCKBACK", seq().addPos(-200, 0, 0, 0)
                            .addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("STOCKFRONT", seq().addPos(180, 0, 0, 0)
                            .addPos(0, 0, 0, 500, IType.SIN_FULL));
            case CYCLE -> new BusAnimation()
                    .addBus("RECOIL", seq().addPos(0, 0,
                            ItemGunBaseNT.getIsAiming(stack) ? -0.5 : -0.75, 25, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 75, IType.SIN_FULL));
            case CYCLE_DRY -> new BusAnimation()
                    .addBus("LIFT", seq().addPos(0, 0, 0, 250).addPos(-25, 0, 0, 250, IType.SIN_FULL)
                            .addPos(-25, 0, 0, 500).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("SLIDE", seq().addPos(0, 0, 0, 500).addPos(0, 0, -2, 150, IType.SIN_FULL)
                            .addPos(0, 0, 0, 50, IType.SIN_UP));
            case RELOAD -> new BusAnimation()
                    .addBus("MAG", seq().addPos(0, 0, 0, 250).addPos(0, -10, 0, 250, IType.SIN_UP)
                            .addPos(0, -10, 0, 750).addPos(0, 0, 0, 500, IType.SIN_DOWN))
                    .addBus("LIFT", seq().addPos(-25, 0, 0, 250, IType.SIN_FULL)
                            .addPos(-25, 0, 0, 2000).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("SLIDE", seq().addPos(0, 0, 0, 2000).addPos(0, 0, -2, 150, IType.SIN_FULL)
                            .addPos(0, 0, 0, 50, IType.SIN_UP))
                    .addBus("BULLET", seq().addPos(empty ? 0 : 1, 0, 0, 0)
                            .addPos(empty ? 0 : 1, 0, 0, 500).addPos(1, 0, 0, 0));
            case JAMMED -> new BusAnimation()
                    .addBus("LIFT", seq().addPos(0, 0, 0, 500).addPos(-25, 0, 0, 250, IType.SIN_FULL)
                            .addPos(-25, 0, 0, 1250).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("SLIDE", seq().addPos(0, 0, 0, 1000).addPos(0, 0, -2, 150, IType.SIN_FULL)
                            .addPos(0, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 500)
                            .addPos(0, 0, -2, 150, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP));
            case INSPECT -> new BusAnimation()
                    .addBus("YEET", seq().addPos(0, -1, 0, 100).addPos(0, 0, 0, 100, IType.SIN_UP)
                            .addPos(0, 12, 0, 350, IType.SIN_DOWN).addPos(0, 0, 0, 350, IType.SIN_UP)
                            .addPos(0, -1, 0, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("SPEEN", seq().addPos(0, 0, 0, 250).addPos(-360, 0, 0, 600));
            default -> null;
        };
    }

    private static int loaded(ItemStack stack, int rail) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        return gun.getPrimaryMagazine(stack, rail).getAmount(stack, null);
    }
}
