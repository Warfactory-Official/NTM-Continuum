package com.hbm.client.anim;

import com.hbm.client.anim.BusAnimationKeyframe.IType;
import com.hbm.items.ModGunItems;
import com.hbm.items.weapon.sedna.GunAnimation;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;

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
    private static final Map<Supplier<Item>, Function<GunAnimation, BusAnimation>> DECLARED = new LinkedHashMap<>();
    private static Map<Item, Function<GunAnimation, BusAnimation>> resolved;

    private GunAnimations() {}

    public static void register(Supplier<Item> item, Function<GunAnimation, BusAnimation> table) {
        DECLARED.put(item, table);
        resolved = null;
    }

    /** The animation {@code item} plays for {@code cue}, or null when it has none — which is fine. */
    public static BusAnimation get(Item item, GunAnimation cue) {
        if (resolved == null) {
            Map<Item, Function<GunAnimation, BusAnimation>> built = new HashMap<>();
            DECLARED.forEach((supplier, table) -> built.put(supplier.get(), table));
            resolved = built;
        }
        Function<GunAnimation, BusAnimation> table = resolved.get(item);
        return table == null ? null : table.apply(cue);
    }

    public static void init() {
        register(ModGunItems.gun_light_revolver::get, GunAnimations::atlas);
        register(ModGunItems.gun_debug::get, GunAnimations::debug);
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
}
