package com.hbm.client.anim;

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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Which animation each gun plays for each cue.
 *
 * <p>Upstream hangs this off the gun config itself ({@code GunConfig.anim(...)}), which forces a common
 * class to name a client-only animation type. Here the table is registered per item on the client
 * instead: the server-side config stays free of render types, and the cue that arrives over
 * {@code GunAnimationPayload} is looked up here.
 *
 * <p>The keyframes are ports of 1.12.2's {@code XFactory357.LAMBDA_ATLAS_ANIMS} and
 * {@code Lego.LAMBDA_DEBUG_ANIMS}, value for value and millisecond for millisecond.
 */
public final class GunAnimations {

    /**
     * Declared tables, keyed by a <em>supplier</em> of the item.
     *
     * <p>Lazy on purpose: on NeoForge the client entry point runs during mod construction, before the
     * item registry is flushed, so resolving an item here would throw the same "Missing key in
     * ResourceKey[minecraft:item]" this port has already hit twice. The map is built on first lookup,
     * which happens at render time — long after registration.
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

    /** The animation {@code item} plays for {@code cue}, or null when it has none — which is fine. */
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
        register(ModGunItems.gun_am180::get, (stack, cue, rail) -> am180Legacy(cue));
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

    /**
     * The break-action revolver. The reload is the interesting one: the latch flips up, the barrel
     * assembly tips forward, the whole gun rotates out of view to be reloaded and comes back — five
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

    /** The debug gun — upstream's {@code LAMBDA_DEBUG_ANIMS}, a li'lpip variant. */
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
                net.minecraft.client.Minecraft.getInstance().player.getInventory());

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

    /** The legacy AM180 table; the JSON/Bezier importer is intentionally still outside this slice. */
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
