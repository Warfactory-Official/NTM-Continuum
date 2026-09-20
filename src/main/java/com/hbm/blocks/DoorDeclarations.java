package com.hbm.blocks;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Dispatch;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Placement;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dejson declarations for the door family. Touches no Minecraft class: it runs in a plain JVM from
 * {@code dejson.gradle.kts}.
 *
 * <p>The renderer-drawn doors get the shared empty blockstate placeholder (their appearance is the
 * block-entity renderer) and an authored {@code hbm:machine_obj} item definition under
 * {@code assets/hbm/items/}. The vanilla-style mod doors and the steel trapdoor are the vanilla
 * door/trapdoor templates over the original's {@code _lower}/{@code _upper} sprites, with the vanilla
 * blockstate tables stated here because Dejson has no door vocabulary of its own.
 */
public final class DoorDeclarations {

    private static final String NS = "hbm";
    private static final String EMPTY = "hbm:block/machine_dummy_empty";
    private static boolean declared;

    private DoorDeclarations() {}

    static final String[] GENERIC_DOORS = {
            "vault_door", "sliding_blast_door", "fire_door", "transition_seal", "silo_hatch", "silo_hatch_large",
            "secure_access_door", "large_vehicle_door", "qe_containment", "qe_sliding_door", "round_airlock_door",
            "sliding_seal_door", "water_door", "cargo_door" };

    static final String[] MOD_DOORS = { "door_metal", "door_office", "door_bunker", "door_red" };

    public static void declare() {
        if (declared) return;
        declared = true;

        // Renderer-drawn: facing x cell x open (the 1.7 "extra" flag).
        for (String door : GENERIC_DOORS) {
            Dejson.block(NS, door).rendered(EMPTY)
                    .states(Variants.product(
                            Variants.uniform("facing", "north", "east", "south", "west"),
                            Variants.uniform("cell", "false", "true"),
                            Variants.uniform("open", "false", "true")))
                    .register();
            Drops.self(NS, door);
        }

        // 1.7 setBlockTextureName("hbm:transition_seal"): the seal's inventory icon is that flat block sprite.
        Dejson.item(NS, "transition_seal").models(Models.generated("hbm:blocks/transition_seal")).register();

        Dejson.block(NS, "blast_door").rendered(EMPTY).states(Variants.uniform("axis", "x", "z")).register();
        Drops.self(NS, "blast_door");
        Dejson.block(NS, "dummy_block_blast").rendered(EMPTY).states(Variants.simple()).register();
        Drops.nothing(NS, "dummy_block_blast");

        for (String door : MOD_DOORS) {
            String lower = "hbm:blocks/" + door + "_lower";
            String upper = "hbm:blocks/" + door + "_upper";
            Models.Group models = Models.group();
            for (String half : new String[] {"bottom", "top"}) {
                for (String hinge : new String[] {"left", "right"}) {
                    for (String open : new String[] {"", "_open"}) {
                        String suffix = "_" + half + "_" + hinge + open;
                        models = models.and(suffix, Models.renderType("minecraft:cutout",
                                Models.parented("minecraft:block/door" + suffix, Slots.of("bottom", lower, "top", upper))));
                    }
                }
            }
            Dejson.block(NS, door)
                    .models(models)
                    .states(doorStates())
                    .item(Models.generated("hbm:items/" + door))
                    .register();
            // The lower half alone drops, as vanilla doors do; stated as an authored table.
        }

        Dejson.block(NS, "trapdoor_steel")
                .models(Models.group()
                        .and("_bottom", Models.renderType("minecraft:cutout", Models.parented("minecraft:block/template_trapdoor_bottom", Slots.of("texture", "hbm:blocks/trapdoor_steel"))))
                        .and("_top", Models.renderType("minecraft:cutout", Models.parented("minecraft:block/template_trapdoor_top", Slots.of("texture", "hbm:blocks/trapdoor_steel"))))
                        .and("_open", Models.renderType("minecraft:cutout", Models.parented("minecraft:block/template_trapdoor_open", Slots.of("texture", "hbm:blocks/trapdoor_steel")))))
                .states(trapdoorStates())
                .item(Models.blockItem("hbm:block/trapdoor_steel_bottom"))
                .register();
        Drops.self(NS, "trapdoor_steel");
    }

    private static final String[] DOOR_FACINGS = {"east", "south", "west", "north"};

    /** Vanilla's door blockstate: closed doors turn with the facing, open leaves swing a quarter turn either way. */
    private static Dispatch doorStates() {
        Map<String, List<Placement>> placements = new LinkedHashMap<>();
        for (int i = 0; i < DOOR_FACINGS.length; i++) {
            String facing = DOOR_FACINGS[i];
            int closed = i * 90;
            for (String half : new String[] {"lower", "upper"}) {
                String model = half.equals("lower") ? "_bottom" : "_top";
                placements.put(key(facing, half, "left", "false"), List.of(new Placement(model + "_left", 0, closed, false)));
                placements.put(key(facing, half, "left", "true"), List.of(new Placement(model + "_left_open", 0, (closed + 90) % 360, false)));
                placements.put(key(facing, half, "right", "false"), List.of(new Placement(model + "_right", 0, closed, false)));
                placements.put(key(facing, half, "right", "true"), List.of(new Placement(model + "_right_open", 0, (closed + 270) % 360, false)));
            }
        }
        Set<String> properties = new LinkedHashSet<>(List.of("facing", "half", "hinge", "open"));
        return Variants.of(properties, placements);
    }

    private static String key(String facing, String half, String hinge, String open) {
        return "facing=" + facing + ",half=" + half + ",hinge=" + hinge + ",open=" + open;
    }

    private static final String[] TRAPDOOR_FACINGS = {"north", "east", "south", "west"};

    /** Vanilla's trapdoor blockstate. */
    private static Dispatch trapdoorStates() {
        Map<String, List<Placement>> placements = new LinkedHashMap<>();
        for (int i = 0; i < TRAPDOOR_FACINGS.length; i++) {
            String facing = TRAPDOOR_FACINGS[i];
            int y = i * 90;
            placements.put("facing=" + facing + ",half=bottom,open=false", List.of(new Placement("_bottom", 0, y, false)));
            placements.put("facing=" + facing + ",half=top,open=false", List.of(new Placement("_top", 0, y, false)));
            placements.put("facing=" + facing + ",half=bottom,open=true", List.of(new Placement("_open", 0, y, false)));
            placements.put("facing=" + facing + ",half=top,open=true", List.of(new Placement("_open", 180, (y + 180) % 360, false)));
        }
        Set<String> properties = new LinkedHashSet<>(List.of("facing", "half", "open"));
        return Variants.of(properties, placements);
    }
}
