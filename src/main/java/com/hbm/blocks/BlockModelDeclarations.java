package com.hbm.blocks;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Model;
import dev.golem.core.dejson.Model.Element;
import dev.golem.core.dejson.Model.Face;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;

/**
 * Block models no block declaration owns: multipart pieces, renderer-loaded parts and the
 * hand-built geometry of pipes, cables, conveyors, fences and RBMK columns, in {@link Model}
 * form.
 *
 * <p>Touches no Minecraft class: Dejson runs {@link #declare()} in a plain JVM at build time.
 */
public final class BlockModelDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private BlockModelDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;
        part0();
        part1();
        part2();
        part3();
        part4();
        part5();
        part6();
        part7();
    }

    private static void part0() {
        Dejson.model("hbm:block/cable_diode", Model.parent("minecraft:block/block")
                .textures(Slots.of("particle", "hbm:blocks/cable_diode", "pad", "hbm:blocks/cable_diode", "core", "hbm:blocks/hadron_coil_alloy"))
                .elements(Element.box(0, 0, 0, 16, 16, 2)
                        .face(Face.of("down", "#pad"))
                        .face(Face.of("up", "#pad"))
                        .face(Face.of("north", "#pad"))
                        .face(Face.of("south", "#pad"))
                        .face(Face.of("west", "#pad"))
                        .face(Face.of("east", "#pad")),
                        Element.box(2, 2, 2, 14, 14, 14)
                        .face(Face.of("down", "#core"))
                        .face(Face.of("up", "#core"))
                        .face(Face.of("north", "#core"))
                        .face(Face.of("south", "#core"))
                        .face(Face.of("west", "#core"))
                        .face(Face.of("east", "#core"))));
        Dejson.model("hbm:block/capacitor", Model.parent("block/block")
                .textures(Slots.of("top", "hbm:blocks/capacitor_copper_top", "side", "hbm:blocks/capacitor_copper_side", "bottom", "hbm:blocks/capacitor_copper_bottom").and("inner_top", "hbm:blocks/capacitor_copper_inner_top").and("inner_side", "hbm:blocks/capacitor_copper_inner_side").and("particle", "hbm:blocks/capacitor_copper_inner_side"))
                .elements(Element.box(0, 15, 0, 16, 16, 16)
                        .face(Face.of("up", "#top"))
                        .face(Face.of("down", "#top")),
                        Element.box(0, 0, 0, 16, 4, 16)
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#bottom"))
                        .face(Face.of("north", "#bottom"))
                        .face(Face.of("south", "#bottom"))
                        .face(Face.of("west", "#bottom"))
                        .face(Face.of("east", "#bottom")),
                        Element.box(0.0001, 0, 0.0001, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side")),
                        Element.box(1, 14, 1, 15, 15, 15)
                        .face(Face.of("up", "#inner_top"))
                        .face(Face.of("down", "#inner_top")),
                        Element.box(1, 4, 1, 15, 14, 15)
                        .face(Face.of("north", "#inner_side"))
                        .face(Face.of("south", "#inner_side"))
                        .face(Face.of("west", "#inner_side"))
                        .face(Face.of("east", "#inner_side"))));
        Dejson.model("hbm:block/capacitor_schrabidate", Model.parent("minecraft:block/block")
                .textures(Slots.of("top", "hbm:blocks/capacitor_schrabidate_top", "side", "hbm:blocks/capacitor_schrabidate_side", "bottom", "hbm:blocks/capacitor_schrabidate_bottom").and("inner_top", "hbm:blocks/capacitor_schrabidate_inner_top").and("inner_side", "hbm:blocks/capacitor_schrabidate_inner_side").and("particle", "hbm:blocks/capacitor_schrabidate_inner_side"))
                .elements(Element.box(0, 15, 0, 16, 16, 16)
                        .face(Face.of("up", "#top"))
                        .face(Face.of("down", "#top")),
                        Element.box(0, 0, 0, 16, 4, 16)
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#bottom"))
                        .face(Face.of("north", "#bottom"))
                        .face(Face.of("south", "#bottom"))
                        .face(Face.of("west", "#bottom"))
                        .face(Face.of("east", "#bottom")),
                        Element.box(0.0001, 0, 0.0001, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side")),
                        Element.box(1, 14, 1, 15, 15, 15)
                        .face(Face.of("up", "#inner_top"))
                        .face(Face.of("down", "#inner_top")),
                        Element.box(1, 4, 1, 15, 14, 15)
                        .face(Face.of("north", "#inner_side"))
                        .face(Face.of("south", "#inner_side"))
                        .face(Face.of("west", "#inner_side"))
                        .face(Face.of("east", "#inner_side"))));
        Dejson.model("hbm:block/capacitor_tantalium", Model.parent("minecraft:block/block")
                .textures(Slots.of("top", "hbm:blocks/capacitor_tantalium_top", "side", "hbm:blocks/capacitor_tantalium_side", "bottom", "hbm:blocks/capacitor_tantalium_bottom").and("inner_top", "hbm:blocks/capacitor_tantalium_inner_top").and("inner_side", "hbm:blocks/capacitor_tantalium_inner_side").and("particle", "hbm:blocks/capacitor_tantalium_inner_side"))
                .elements(Element.box(0, 15, 0, 16, 16, 16)
                        .face(Face.of("up", "#top"))
                        .face(Face.of("down", "#top")),
                        Element.box(0, 0, 0, 16, 4, 16)
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#bottom"))
                        .face(Face.of("north", "#bottom"))
                        .face(Face.of("south", "#bottom"))
                        .face(Face.of("west", "#bottom"))
                        .face(Face.of("east", "#bottom")),
                        Element.box(0.0001, 0, 0.0001, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side")),
                        Element.box(1, 14, 1, 15, 15, 15)
                        .face(Face.of("up", "#inner_top"))
                        .face(Face.of("down", "#inner_top")),
                        Element.box(1, 4, 1, 15, 14, 15)
                        .face(Face.of("north", "#inner_side"))
                        .face(Face.of("south", "#inner_side"))
                        .face(Face.of("west", "#inner_side"))
                        .face(Face.of("east", "#inner_side"))));
        Dejson.model("hbm:block/conveyor_belt", Model.parent("minecraft:block/block").ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "#top"))
                .elements(Element.box(0, 0, 0, 16, 4, 16)
                        .face(Face.of("down", "#top").cullface("down").rotation(180))
                        .face(Face.of("up", "#top"))
                        .face(Face.of("north", "#top").cullface("north"))
                        .face(Face.of("south", "#top").cullface("south"))
                        .face(Face.of("west", "#side").cullface("west"))
                        .face(Face.of("east", "#side").cullface("east").rotation(180))));
        Dejson.model("hbm:block/conveyor_belt_bend", Model.parent("minecraft:block/block").ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "#top"))
                .elements(Element.box(0, 0, 0, 16, 4, 16)
                        .face(Face.of("down", "#top").cullface("down").rotation(180))
                        .face(Face.of("up", "#top"))
                        .face(Face.of("north", "#side").cullface("north"))
                        .face(Face.of("south", "#side").cullface("south"))
                        .face(Face.of("west", "#side").cullface("west"))
                        .face(Face.of("east", "#side").cullface("east"))));
        Dejson.model("hbm:block/conveyor_chute_belt", Model.parent("minecraft:block/block").ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/conveyor", "belt", "hbm:blocks/conveyor", "side", "hbm:blocks/conveyor_side").and("concrete", "hbm:blocks/concrete").and("grate", "hbm:blocks/grate_top"))
                .elements(Element.box(4.0, 0, 0, 12.0, 4.0, 16)
                        .face(Face.of("down", "#belt").rotation(180))
                        .face(Face.of("up", "#belt"))
                        .face(Face.of("north", "#belt"))
                        .face(Face.of("south", "#belt"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side").rotation(180)),
                        Element.box(0, 0, 4.0, 4.0, 4.0, 12.0)
                        .face(Face.of("down", "#belt").rotation(180))
                        .face(Face.of("up", "#belt"))
                        .face(Face.of("north", "#belt"))
                        .face(Face.of("south", "#belt"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side").rotation(180)),
                        Element.box(12.0, 0, 4.0, 16, 4.0, 12.0)
                        .face(Face.of("down", "#belt").rotation(180))
                        .face(Face.of("up", "#belt"))
                        .face(Face.of("north", "#belt"))
                        .face(Face.of("south", "#belt"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side").rotation(180))));
        Dejson.model("hbm:block/conveyor_chute_frame", Model.parent("minecraft:block/block").ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/conveyor", "belt", "hbm:blocks/conveyor", "side", "hbm:blocks/conveyor_side").and("concrete", "hbm:blocks/concrete").and("grate", "hbm:blocks/grate_top"))
                .elements(Element.box(0, 0, 0, 4.0, 16, 4.0)
                        .face(Face.of("down", "#concrete"))
                        .face(Face.of("up", "#concrete"))
                        .face(Face.of("north", "#concrete"))
                        .face(Face.of("south", "#concrete"))
                        .face(Face.of("west", "#concrete"))
                        .face(Face.of("east", "#concrete")),
                        Element.box(12.0, 0, 0, 16, 16, 4.0)
                        .face(Face.of("down", "#concrete"))
                        .face(Face.of("up", "#concrete"))
                        .face(Face.of("north", "#concrete"))
                        .face(Face.of("south", "#concrete"))
                        .face(Face.of("west", "#concrete"))
                        .face(Face.of("east", "#concrete")),
                        Element.box(0, 0, 12.0, 4.0, 16, 16)
                        .face(Face.of("down", "#concrete"))
                        .face(Face.of("up", "#concrete"))
                        .face(Face.of("north", "#concrete"))
                        .face(Face.of("south", "#concrete"))
                        .face(Face.of("west", "#concrete"))
                        .face(Face.of("east", "#concrete")),
                        Element.box(12.0, 0, 12.0, 16, 16, 16)
                        .face(Face.of("down", "#concrete"))
                        .face(Face.of("up", "#concrete"))
                        .face(Face.of("north", "#concrete"))
                        .face(Face.of("south", "#concrete"))
                        .face(Face.of("west", "#concrete"))
                        .face(Face.of("east", "#concrete"))));
        Dejson.model("hbm:block/conveyor_chute_grate", Model.parent("minecraft:block/block").ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/conveyor", "belt", "hbm:blocks/conveyor", "side", "hbm:blocks/conveyor_side").and("concrete", "hbm:blocks/concrete").and("grate", "hbm:blocks/grate_top"))
                .elements(Element.box(4.0, 0, 2.0, 12.0, 16, 2.0)
                        .face(Face.of("north", "#grate"))
                        .face(Face.of("south", "#grate"))));
        Dejson.model("hbm:block/conveyor_chute_grate_short", Model.parent("minecraft:block/block").ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/conveyor", "belt", "hbm:blocks/conveyor", "side", "hbm:blocks/conveyor_side").and("concrete", "hbm:blocks/concrete").and("grate", "hbm:blocks/grate_top"))
                .elements(Element.box(4.0, 4.0, 2.0, 12.0, 16, 2.0)
                        .face(Face.of("north", "#grate"))
                        .face(Face.of("south", "#grate"))));
        Dejson.model("hbm:block/conveyor_chute_inventory", Model.parent("minecraft:block/block").ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/conveyor", "belt", "hbm:blocks/conveyor", "side", "hbm:blocks/conveyor_side").and("concrete", "hbm:blocks/concrete").and("grate", "hbm:blocks/grate_top"))
                .elements(Element.box(4.0, 0, 0, 12.0, 4.0, 16)
                        .face(Face.of("down", "#belt").rotation(180))
                        .face(Face.of("up", "#belt"))
                        .face(Face.of("north", "#belt"))
                        .face(Face.of("south", "#belt"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side").rotation(180)),
                        Element.box(0, 0, 4.0, 4.0, 4.0, 12.0)
                        .face(Face.of("down", "#belt").rotation(180))
                        .face(Face.of("up", "#belt"))
                        .face(Face.of("north", "#belt"))
                        .face(Face.of("south", "#belt"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side").rotation(180)),
                        Element.box(12.0, 0, 4.0, 16, 4.0, 12.0)
                        .face(Face.of("down", "#belt").rotation(180))
                        .face(Face.of("up", "#belt"))
                        .face(Face.of("north", "#belt"))
                        .face(Face.of("south", "#belt"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side").rotation(180)),
                        Element.box(0, 0, 0, 4.0, 16, 4.0)
                        .face(Face.of("down", "#concrete"))
                        .face(Face.of("up", "#concrete"))
                        .face(Face.of("north", "#concrete"))
                        .face(Face.of("south", "#concrete"))
                        .face(Face.of("west", "#concrete"))
                        .face(Face.of("east", "#concrete")),
                        Element.box(12.0, 0, 0, 16, 16, 4.0)
                        .face(Face.of("down", "#concrete"))
                        .face(Face.of("up", "#concrete"))
                        .face(Face.of("north", "#concrete"))
                        .face(Face.of("south", "#concrete"))
                        .face(Face.of("west", "#concrete"))
                        .face(Face.of("east", "#concrete")),
                        Element.box(0, 0, 12.0, 4.0, 16, 16)
                        .face(Face.of("down", "#concrete"))
                        .face(Face.of("up", "#concrete"))
                        .face(Face.of("north", "#concrete"))
                        .face(Face.of("south", "#concrete"))
                        .face(Face.of("west", "#concrete"))
                        .face(Face.of("east", "#concrete")),
                        Element.box(12.0, 0, 12.0, 16, 16, 16)
                        .face(Face.of("down", "#concrete"))
                        .face(Face.of("up", "#concrete"))
                        .face(Face.of("north", "#concrete"))
                        .face(Face.of("south", "#concrete"))
                        .face(Face.of("west", "#concrete"))
                        .face(Face.of("east", "#concrete")),
                        Element.box(4.0, 4.0, 2.0, 12.0, 16, 2.0)
                        .face(Face.of("north", "#grate"))
                        .face(Face.of("south", "#grate")),
                        Element.box(2.0, 4.0, 4.0, 2.0, 16, 12.0)
                        .face(Face.of("west", "#grate"))
                        .face(Face.of("east", "#grate")),
                        Element.box(14.0, 4.0, 4.0, 14.0, 16, 12.0)
                        .face(Face.of("west", "#grate"))
                        .face(Face.of("east", "#grate"))));
        Dejson.model("hbm:block/conveyor_chute_stub_east", Model.parent("minecraft:block/block").ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/conveyor", "belt", "hbm:blocks/conveyor", "side", "hbm:blocks/conveyor_side").and("concrete", "hbm:blocks/concrete").and("grate", "hbm:blocks/grate_top"))
                .elements(Element.box(14.0, 0, 4.0, 16, 4.0, 12.0)
                        .face(Face.of("down", "#belt"))
                        .face(Face.of("up", "#belt").rotation(270))
                        .face(Face.of("north", "#belt"))
                        .face(Face.of("south", "#belt"))
                        .face(Face.of("west", "#belt"))
                        .face(Face.of("east", "#belt"))));
        Dejson.model("hbm:block/conveyor_chute_stub_north", Model.parent("minecraft:block/block").ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/conveyor", "belt", "hbm:blocks/conveyor", "side", "hbm:blocks/conveyor_side").and("concrete", "hbm:blocks/concrete").and("grate", "hbm:blocks/grate_top"))
                .elements(Element.box(4.0, 0, 0, 12.0, 4.0, 2.0)
                        .face(Face.of("down", "#belt"))
                        .face(Face.of("up", "#belt").rotation(180))
                        .face(Face.of("north", "#belt"))
                        .face(Face.of("south", "#belt"))
                        .face(Face.of("west", "#belt"))
                        .face(Face.of("east", "#belt"))));
        Dejson.model("hbm:block/conveyor_chute_stub_south", Model.parent("minecraft:block/block").ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/conveyor", "belt", "hbm:blocks/conveyor", "side", "hbm:blocks/conveyor_side").and("concrete", "hbm:blocks/concrete").and("grate", "hbm:blocks/grate_top"))
                .elements(Element.box(4.0, 0, 14.0, 12.0, 4.0, 16)
                        .face(Face.of("down", "#belt"))
                        .face(Face.of("up", "#belt").rotation(0))
                        .face(Face.of("north", "#belt"))
                        .face(Face.of("south", "#belt"))
                        .face(Face.of("west", "#belt"))
                        .face(Face.of("east", "#belt"))));
        Dejson.model("hbm:block/conveyor_chute_stub_west", Model.parent("minecraft:block/block").ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/conveyor", "belt", "hbm:blocks/conveyor", "side", "hbm:blocks/conveyor_side").and("concrete", "hbm:blocks/concrete").and("grate", "hbm:blocks/grate_top"))
                .elements(Element.box(0, 0, 4.0, 2.0, 4.0, 12.0)
                        .face(Face.of("down", "#belt"))
                        .face(Face.of("up", "#belt").rotation(90))
                        .face(Face.of("north", "#belt"))
                        .face(Face.of("south", "#belt"))
                        .face(Face.of("west", "#belt"))
                        .face(Face.of("east", "#belt"))));
        Dejson.model("hbm:block/conveyor_lift_bottom", Model.parent("minecraft:block/block").ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/conveyor", "belt", "hbm:blocks/conveyor", "side", "hbm:blocks/conveyor_side").and("concrete", "hbm:blocks/concrete").and("iron", "minecraft:block/iron_block"))
                .elements(Element.box(0, 0, 4.0, 4.0, 4.0, 12.0)
                        .face(Face.of("down", "#belt").rotation(90))
                        .face(Face.of("up", "#belt").rotation(90))
                        .face(Face.of("north", "#belt"))
                        .face(Face.of("south", "#belt"))
                        .face(Face.of("west", "#belt"))
                        .face(Face.of("east", "#belt")),
                        Element.box(12.0, 0, 4.0, 16, 4.0, 12.0)
                        .face(Face.of("down", "#belt").rotation(270))
                        .face(Face.of("up", "#belt").rotation(270))
                        .face(Face.of("north", "#belt"))
                        .face(Face.of("south", "#belt"))
                        .face(Face.of("west", "#belt"))
                        .face(Face.of("east", "#belt")),
                        Element.box(4.0, 0, 12.0, 12.0, 4.0, 16)
                        .face(Face.of("down", "#belt").rotation(180))
                        .face(Face.of("up", "#belt"))
                        .face(Face.of("north", "#belt"))
                        .face(Face.of("south", "#belt"))
                        .face(Face.of("west", "#belt"))
                        .face(Face.of("east", "#belt")),
                        Element.box(0, 0, 0, 16, 16, 4.0)
                        .face(Face.of("down", "#concrete"))
                        .face(Face.of("up", "#concrete"))
                        .face(Face.of("north", "#concrete"))
                        .face(Face.of("south", "#concrete"))
                        .face(Face.of("west", "#concrete"))
                        .face(Face.of("east", "#concrete")),
                        Element.box(0, 0, 12.0, 4.0, 16, 16)
                        .face(Face.of("down", "#concrete"))
                        .face(Face.of("up", "#concrete"))
                        .face(Face.of("north", "#concrete"))
                        .face(Face.of("south", "#concrete"))
                        .face(Face.of("west", "#concrete"))
                        .face(Face.of("east", "#concrete")),
                        Element.box(12.0, 0, 12.0, 16, 16, 16)
                        .face(Face.of("down", "#concrete"))
                        .face(Face.of("up", "#concrete"))
                        .face(Face.of("north", "#concrete"))
                        .face(Face.of("south", "#concrete"))
                        .face(Face.of("west", "#concrete"))
                        .face(Face.of("east", "#concrete")),
                        Element.box(4.0, 0, 4.0, 12.0, 16, 6.0)
                        .face(Face.of("down", "#belt"))
                        .face(Face.of("up", "#belt"))
                        .face(Face.of("north", "#belt"))
                        .face(Face.of("south", "#belt"))
                        .face(Face.of("west", "#belt"))
                        .face(Face.of("east", "#belt")),
                        Element.box(4.0, 0, 6.0, 12.0, 4.0, 12.0)
                        .face(Face.of("down", "#iron"))
                        .face(Face.of("up", "#iron"))
                        .face(Face.of("north", "#iron"))
                        .face(Face.of("south", "#iron"))
                        .face(Face.of("west", "#iron"))
                        .face(Face.of("east", "#iron"))));
        Dejson.model("hbm:block/conveyor_lift_middle", Model.parent("minecraft:block/block").ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/conveyor", "belt", "hbm:blocks/conveyor", "side", "hbm:blocks/conveyor_side").and("concrete", "hbm:blocks/concrete").and("iron", "minecraft:block/iron_block"))
                .elements(Element.box(0, 0, 0, 16, 16, 4.0)
                        .face(Face.of("down", "#concrete"))
                        .face(Face.of("up", "#concrete"))
                        .face(Face.of("north", "#concrete"))
                        .face(Face.of("south", "#concrete"))
                        .face(Face.of("west", "#concrete"))
                        .face(Face.of("east", "#concrete")),
                        Element.box(0, 0, 12.0, 4.0, 16, 16)
                        .face(Face.of("down", "#concrete"))
                        .face(Face.of("up", "#concrete"))
                        .face(Face.of("north", "#concrete"))
                        .face(Face.of("south", "#concrete"))
                        .face(Face.of("west", "#concrete"))
                        .face(Face.of("east", "#concrete")),
                        Element.box(12.0, 0, 12.0, 16, 16, 16)
                        .face(Face.of("down", "#concrete"))
                        .face(Face.of("up", "#concrete"))
                        .face(Face.of("north", "#concrete"))
                        .face(Face.of("south", "#concrete"))
                        .face(Face.of("west", "#concrete"))
                        .face(Face.of("east", "#concrete")),
                        Element.box(4.0, 0, 4.0, 12.0, 16, 6.0)
                        .face(Face.of("down", "#belt"))
                        .face(Face.of("up", "#belt"))
                        .face(Face.of("north", "#belt"))
                        .face(Face.of("south", "#belt"))
                        .face(Face.of("west", "#belt"))
                        .face(Face.of("east", "#belt"))));
        Dejson.model("hbm:block/conveyor_lift_top", Model.parent("minecraft:block/block").ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/conveyor", "belt", "hbm:blocks/conveyor", "side", "hbm:blocks/conveyor_side").and("concrete", "hbm:blocks/concrete").and("iron", "minecraft:block/iron_block"))
                .elements(Element.box(0, 0, 0, 4.0, 8.0, 16)
                        .face(Face.of("down", "#concrete"))
                        .face(Face.of("up", "#concrete"))
                        .face(Face.of("north", "#concrete"))
                        .face(Face.of("south", "#concrete"))
                        .face(Face.of("west", "#concrete"))
                        .face(Face.of("east", "#concrete")),
                        Element.box(12.0, 0, 0, 16, 8.0, 16)
                        .face(Face.of("down", "#concrete"))
                        .face(Face.of("up", "#concrete"))
                        .face(Face.of("north", "#concrete"))
                        .face(Face.of("south", "#concrete"))
                        .face(Face.of("west", "#concrete"))
                        .face(Face.of("east", "#concrete")),
                        Element.box(4.0, 0, 0, 12.0, 4.0, 6.0)
                        .face(Face.of("down", "#belt"))
                        .face(Face.of("up", "#belt"))
                        .face(Face.of("north", "#belt"))
                        .face(Face.of("south", "#belt"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side").rotation(180))));
        Dejson.model("hbm:block/crane_cube_0", Model.parent("minecraft:block/block")
                .textures(Slots.of("particle", "#side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("down", "#down").cullface("down"))
                        .face(Face.of("up", "#up").cullface("up"))
                        .face(Face.of("north", "#north").cullface("north"))
                        .face(Face.of("south", "#south").cullface("south"))
                        .face(Face.of("west", "#west").cullface("west"))
                        .face(Face.of("east", "#east").cullface("east"))));
        Dejson.model("hbm:block/crane_cube_180", Model.parent("minecraft:block/block")
                .textures(Slots.of("particle", "#side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("down", "#down").cullface("down"))
                        .face(Face.of("up", "#up").cullface("up").rotation(180))
                        .face(Face.of("north", "#north").cullface("north"))
                        .face(Face.of("south", "#south").cullface("south"))
                        .face(Face.of("west", "#west").cullface("west"))
                        .face(Face.of("east", "#east").cullface("east"))));
        Dejson.model("hbm:block/crane_cube_270", Model.parent("minecraft:block/block")
                .textures(Slots.of("particle", "#side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("down", "#down").cullface("down"))
                        .face(Face.of("up", "#up").cullface("up").rotation(270))
                        .face(Face.of("north", "#north").cullface("north"))
                        .face(Face.of("south", "#south").cullface("south"))
                        .face(Face.of("west", "#west").cullface("west"))
                        .face(Face.of("east", "#east").cullface("east"))));
        Dejson.model("hbm:block/crane_cube_90", Model.parent("minecraft:block/block")
                .textures(Slots.of("particle", "#side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("down", "#down").cullface("down"))
                        .face(Face.of("up", "#up").cullface("up").rotation(90))
                        .face(Face.of("north", "#north").cullface("north"))
                        .face(Face.of("south", "#south").cullface("south"))
                        .face(Face.of("west", "#west").cullface("west"))
                        .face(Face.of("east", "#east").cullface("east"))));
        Dejson.model("hbm:block/crane_router", Model.parent("minecraft:block/block").renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/crane_in", "base", "hbm:blocks/crane_in", "overlay", "hbm:blocks/crane_router_overlay"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("down", "#base").cullface("down"))
                        .face(Face.of("up", "#base").cullface("up"))
                        .face(Face.of("north", "#base").cullface("north"))
                        .face(Face.of("south", "#base").cullface("south"))
                        .face(Face.of("west", "#base").cullface("west"))
                        .face(Face.of("east", "#base").cullface("east")),
                        Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("down", "#overlay").cullface("down").tintIndex(0))
                        .face(Face.of("up", "#overlay").cullface("up").tintIndex(1))
                        .face(Face.of("north", "#overlay").cullface("north").tintIndex(2))
                        .face(Face.of("south", "#overlay").cullface("south").tintIndex(3))
                        .face(Face.of("west", "#overlay").cullface("west").tintIndex(4))
                        .face(Face.of("east", "#overlay").cullface("east").tintIndex(5))));
        Dejson.model("hbm:block/fallout", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/ash", "texture", "hbm:blocks/ash"))
                .elements(Element.box(0, 0, 0, 16, 2, 16)
                        .face(Face.of("down", "#texture").uv(0, 0, 16, 16).cullface("down"))
                        .face(Face.of("up", "#texture").uv(0, 0, 16, 16))
                        .face(Face.of("north", "#texture").uv(0, 14, 16, 16).cullface("north"))
                        .face(Face.of("south", "#texture").uv(0, 14, 16, 16).cullface("south"))
                        .face(Face.of("west", "#texture").uv(0, 14, 16, 16).cullface("west"))
                        .face(Face.of("east", "#texture").uv(0, 14, 16, 16).cullface("east"))));
        Dejson.model("hbm:block/fence_metal_post", Model.parent("minecraft:block/fence_post").renderType("minecraft:cutout")
                .textures(Slots.of("texture", "hbm:blocks/fence_metal_post", "particle", "hbm:blocks/fence_metal_post")));
        Dejson.model("hbm:block/fence_metal_side", Model.of().ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/fence_metal", "texture", "hbm:blocks/fence_metal"))
                .elements(Element.box(8, 0, 0, 8, 16, 8)
                        .face(Face.of("west", "#texture").uv(0, 0, 8, 16))
                        .face(Face.of("east", "#texture").uv(0, 0, 8, 16))));
        Dejson.model("hbm:block/fire_digamma_floor", Model.parent("minecraft:block/template_fire_floor").renderType("minecraft:cutout")
                .textures(Slots.of("fire", "hbm:blocks/fire_digamma", "particle", "hbm:blocks/fire_digamma")));
        Dejson.model("hbm:block/fluid_duct_gauge", Models.cubeAll("hbm:blocks/deco_steel"));
        Dejson.model("hbm:block/fluid_duct_paintable", Models.cubeAll("hbm:blocks/fluid_duct_paintable"));
        Dejson.model("hbm:block/fluid_duct_paintable_block_exhaust", Models.cubeAll("hbm:blocks/fluid_duct_paintable_block_exhaust"));
    }

    private static void part1() {
        Dejson.model("hbm:block/foam_layer_height10", Models.parented("minecraft:block/snow_height10", Slots.of("particle", "hbm:blocks/block_foam", "texture", "hbm:blocks/block_foam")));
        Dejson.model("hbm:block/foam_layer_height12", Models.parented("minecraft:block/snow_height12", Slots.of("particle", "hbm:blocks/block_foam", "texture", "hbm:blocks/block_foam")));
        Dejson.model("hbm:block/foam_layer_height14", Models.parented("minecraft:block/snow_height14", Slots.of("particle", "hbm:blocks/block_foam", "texture", "hbm:blocks/block_foam")));
        Dejson.model("hbm:block/foam_layer_height2", Models.parented("minecraft:block/snow_height2", Slots.of("particle", "hbm:blocks/block_foam", "texture", "hbm:blocks/block_foam")));
        Dejson.model("hbm:block/foam_layer_height4", Models.parented("minecraft:block/snow_height4", Slots.of("particle", "hbm:blocks/block_foam", "texture", "hbm:blocks/block_foam")));
        Dejson.model("hbm:block/foam_layer_height6", Models.parented("minecraft:block/snow_height6", Slots.of("particle", "hbm:blocks/block_foam", "texture", "hbm:blocks/block_foam")));
        Dejson.model("hbm:block/foam_layer_height8", Models.parented("minecraft:block/snow_height8", Slots.of("particle", "hbm:blocks/block_foam", "texture", "hbm:blocks/block_foam")));
        Dejson.model("hbm:block/foundry_channel_center", Model.parent("block/block")
                .textures(Slots.of("0", "hbm:blocks/foundry_channel_bottom", "1", "hbm:blocks/foundry_channel_inner", "2", "hbm:blocks/foundry_channel_side").and("3", "hbm:blocks/foundry_channel_top").and("particle", "hbm:blocks/foundry_channel_bottom"))
                .elements(Element.box(6, 0, 6, 10, 2, 10).name("bot").rotation(7, 0, 7, "y", 0)
                        .face(Face.of("up", "#0").uv(6, 6, 10, 10))
                        .face(Face.of("down", "#0").uv(6, 6, 10, 10)),
                        Element.box(5, 0, 5, 6, 8, 11).name("wall2").rotation(6, 0, 9, "y", 0)
                        .face(Face.of("north", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("east", "#1").uv(5, 8, 11, 16))
                        .face(Face.of("south", "#2").uv(0, 8, 1, 16))
                        .face(Face.of("west", "#2").uv(5, 8, 11, 16))
                        .face(Face.of("up", "#3").uv(10, 5, 11, 11).rotation(180))
                        .face(Face.of("down", "#0").uv(0, 0, 1, 6).rotation(180)),
                        Element.box(6, 0, 5, 10, 8, 6).name("wall3").rotation(6, 0, 6, "y", 0)
                        .face(Face.of("north", "#2").uv(6, 8, 10, 16))
                        .face(Face.of("east", "#2").uv(14, 8, 16, 16))
                        .face(Face.of("south", "#1").uv(6, 8, 10, 16))
                        .face(Face.of("west", "#2").uv(0, 8, 2, 16))
                        .face(Face.of("up", "#3").uv(5, 6, 6, 10).rotation(270))
                        .face(Face.of("down", "#0").uv(0, 12, 1, 16).rotation(90)),
                        Element.box(6, 0, 10, 10, 8, 11).name("wall4").rotation(10, 0, 10, "y", 0)
                        .face(Face.of("north", "#1").uv(6, 8, 10, 16))
                        .face(Face.of("east", "#2").uv(0, 8, 2, 16))
                        .face(Face.of("south", "#2").uv(6, 8, 10, 16))
                        .face(Face.of("west", "#2").uv(14, 8, 16, 16))
                        .face(Face.of("up", "#3").uv(10, 6, 11, 10).rotation(90))
                        .face(Face.of("down", "#0").uv(0, 0, 1, 4).rotation(270)),
                        Element.box(10, 0, 5, 11, 8, 11).name("wall1").rotation(10, 0, 7, "y", 0)
                        .face(Face.of("north", "#2").uv(0, 8, 1, 16))
                        .face(Face.of("east", "#2").uv(5, 8, 11, 16))
                        .face(Face.of("south", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("west", "#1").uv(5, 8, 11, 16))
                        .face(Face.of("up", "#3").uv(5, 5, 6, 11))
                        .face(Face.of("down", "#0").uv(15, 0, 16, 6))));
        Dejson.model("hbm:block/foundry_channel_corner", Model.parent("block/block")
                .textures(Slots.of("0", "hbm:blocks/foundry_channel_bottom", "1", "hbm:blocks/foundry_channel_inner", "2", "hbm:blocks/foundry_channel_side").and("3", "hbm:blocks/foundry_channel_top").and("particle", "hbm:blocks/foundry_channel_bottom"))
                .elements(Element.box(6, 0, 6, 16, 2, 10).name("bot").rotation(8, 2.5, 8.75, "y", 0)
                        .face(Face.of("east", "#0").uv(0, 0, 4, 2))
                        .face(Face.of("west", "#0").uv(0, 0, 4, 2))
                        .face(Face.of("up", "#0").uv(6, 0, 10, 10).rotation(90))
                        .face(Face.of("down", "#0").uv(6, 0, 10, 10).rotation(270)),
                        Element.box(10, 0, 5, 16, 8, 6).name("wall2").rotation(8, 2.5, 7.75, "y", 0)
                        .face(Face.of("north", "#2").uv(0, 8, 6, 16))
                        .face(Face.of("east", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("south", "#1").uv(10, 8, 16, 16))
                        .face(Face.of("west", "#1").uv(0, 8, 1, 16))
                        .face(Face.of("up", "#3").uv(5, 10, 6, 16).rotation(90))
                        .face(Face.of("down", "#0").uv(15, 0, 16, 12).rotation(90)),
                        Element.box(10, 0, 0, 11, 8, 5).name("wall4").rotation(11, 4, 1.5, "y", 0)
                        .face(Face.of("north", "#2").uv(0, 8, 1, 16))
                        .face(Face.of("east", "#2").uv(6, 8, 11, 16))
                        .face(Face.of("south", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("west", "#1").uv(10, 8, 14, 16))
                        .face(Face.of("up", "#3").uv(10, 12, 11, 16).rotation(180))
                        .face(Face.of("down", "#0").uv(15, 0, 16, 12)),
                        Element.box(5, 0, 10, 16, 8, 11).name("wall1").rotation(8, 2.5, 7.75, "y", 0)
                        .face(Face.of("north", "#1").uv(0, 8, 11, 16))
                        .face(Face.of("east", "#2").uv(0, 8, 1, 16))
                        .face(Face.of("south", "#2").uv(0, 8, 11, 16))
                        .face(Face.of("west", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("up", "#3").uv(5, 0, 6, 11).rotation(90))
                        .face(Face.of("down", "#0").uv(0, 0, 1, 16).rotation(270)),
                        Element.box(5, 0, 0, 6, 8, 10).name("wall3").rotation(5.5, 4, 4.5, "y", 0)
                        .face(Face.of("north", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("east", "#1").uv(0, 8, 10, 16))
                        .face(Face.of("south", "#2").uv(0, 8, 1, 16))
                        .face(Face.of("west", "#2").uv(5, 8, 15, 16))
                        .face(Face.of("up", "#3").uv(5, 0, 6, 11).rotation(180))
                        .face(Face.of("down", "#0").uv(0, 0, 1, 16).rotation(180)),
                        Element.box(6, 0, 0, 10, 2, 6).name("bot1").rotation(6, 0, 0, "y", 0)
                        .face(Face.of("north", "#0").uv(0, 0, 4, 2))
                        .face(Face.of("up", "#0").uv(0, 0, 4, 6))
                        .face(Face.of("down", "#0").uv(0, 0, 4, 6))));
        Dejson.model("hbm:block/foundry_channel_cross", Model.parent("block/block")
                .textures(Slots.of("0", "hbm:blocks/foundry_channel_bottom", "1", "hbm:blocks/foundry_channel_inner", "2", "hbm:blocks/foundry_channel_side").and("3", "hbm:blocks/foundry_channel_top").and("particle", "hbm:blocks/foundry_channel_bottom"))
                .elements(Element.box(0, 0, 6, 16, 2, 10).name("bot").rotation(7.75, 2.5, 9.5, "y", 0)
                        .face(Face.of("east", "#0").uv(0, 0, 4, 2))
                        .face(Face.of("west", "#0").uv(0, 0, 4, 2))
                        .face(Face.of("up", "#0").uv(6, 0, 10, 16).rotation(270))
                        .face(Face.of("down", "#0").uv(6, 0, 10, 10).rotation(90)),
                        Element.box(6, 0, 10, 10, 2, 16).name("bot1").rotation(7.75, 2.5, 9.5, "y", 0)
                        .face(Face.of("south", "#0").uv(0, 0, 4, 2))
                        .face(Face.of("up", "#0").uv(0, 0, 4, 6).rotation(180))
                        .face(Face.of("down", "#0").uv(0, 0, 4, 6).rotation(180)),
                        Element.box(0, 0, 10, 6, 8, 11).name("wall2").rotation(7.75, 2.5, 9.5, "y", 0)
                        .face(Face.of("north", "#1").uv(10, 8, 16, 16))
                        .face(Face.of("east", "#1").uv(0, 8, 1, 16))
                        .face(Face.of("south", "#2").uv(0, 8, 6, 16))
                        .face(Face.of("west", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("up", "#3").uv(5, 10, 6, 16).rotation(270))
                        .face(Face.of("down", "#0").uv(15, 0, 16, 6).rotation(270)),
                        Element.box(11, 0, 10, 16, 8, 11).name("wall3").rotation(7.75, 2.5, 9.5, "y", 0)
                        .face(Face.of("north", "#1").uv(10, 8, 14, 16))
                        .face(Face.of("east", "#2").uv(0, 8, 1, 16))
                        .face(Face.of("south", "#2").uv(6, 8, 11, 16))
                        .face(Face.of("west", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("up", "#3").uv(10, 12, 11, 16).rotation(270))
                        .face(Face.of("down", "#0").uv(15, 0, 16, 5).rotation(270)),
                        Element.box(10, 0, 10, 11, 8, 16).name("wall5").rotation(7.75, 2.5, 9.5, "y", 0)
                        .face(Face.of("north", "#1").uv(0, 8, 1, 16))
                        .face(Face.of("east", "#2").uv(0, 8, 6, 16))
                        .face(Face.of("south", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("west", "#1").uv(10, 8, 16, 16))
                        .face(Face.of("up", "#3").uv(5, 10, 6, 16).rotation(180))
                        .face(Face.of("down", "#0").uv(15, 0, 16, 6)),
                        Element.box(5, 0, 11, 6, 8, 16).name("wall4").rotation(7.75, 2.5, 9.5, "y", 0)
                        .face(Face.of("north", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("east", "#1").uv(10, 8, 14, 16))
                        .face(Face.of("south", "#2").uv(0, 8, 1, 16))
                        .face(Face.of("west", "#2").uv(6, 8, 11, 16))
                        .face(Face.of("up", "#3").uv(10, 12, 11, 16))
                        .face(Face.of("down", "#0").uv(15, 0, 16, 5).rotation(180)),
                        Element.box(6, 0, 0, 10, 2, 6).name("bot1").rotation(8.25, 2.5, 5.75, "y", 0)
                        .face(Face.of("north", "#0").uv(0, 0, 4, 2))
                        .face(Face.of("up", "#0").uv(0, 0, 4, 6))
                        .face(Face.of("down", "#0").uv(0, 0, 4, 6)),
                        Element.box(10, 0, 5, 16, 8, 6).name("wall2").rotation(8.25, 2.5, 5.75, "y", 0)
                        .face(Face.of("north", "#2").uv(0, 8, 6, 16))
                        .face(Face.of("east", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("south", "#1").uv(10, 8, 16, 16))
                        .face(Face.of("west", "#1").uv(0, 8, 1, 16))
                        .face(Face.of("up", "#3").uv(5, 10, 6, 16).rotation(90))
                        .face(Face.of("down", "#0").uv(15, 0, 16, 6).rotation(90)),
                        Element.box(0, 0, 5, 5, 8, 6).name("wall3").rotation(8.25, 2.5, 5.75, "y", 0)
                        .face(Face.of("north", "#2").uv(6, 8, 11, 16))
                        .face(Face.of("east", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("south", "#1").uv(10, 8, 14, 16))
                        .face(Face.of("west", "#2").uv(0, 8, 1, 16))
                        .face(Face.of("up", "#3").uv(10, 12, 11, 16).rotation(90))
                        .face(Face.of("down", "#0").uv(15, 0, 16, 5).rotation(90)),
                        Element.box(5, 0, 0, 6, 8, 6).name("wall5").rotation(8.25, 2.5, 5.75, "y", 0)
                        .face(Face.of("north", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("east", "#1").uv(10, 8, 16, 16))
                        .face(Face.of("south", "#1").uv(0, 8, 1, 16))
                        .face(Face.of("west", "#2").uv(0, 8, 6, 16))
                        .face(Face.of("up", "#3").uv(5, 10, 6, 16))
                        .face(Face.of("down", "#0").uv(15, 0, 16, 6).rotation(180)),
                        Element.box(10, 0, 0, 11, 8, 5).name("wall4").rotation(8.25, 2.5, 5.75, "y", 0)
                        .face(Face.of("north", "#2").uv(0, 8, 1, 16))
                        .face(Face.of("east", "#2").uv(6, 8, 11, 16))
                        .face(Face.of("south", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("west", "#1").uv(10, 8, 14, 16))
                        .face(Face.of("up", "#3").uv(10, 12, 11, 16).rotation(180))
                        .face(Face.of("down", "#0").uv(15, 0, 16, 5))));
        Dejson.model("hbm:block/foundry_channel_side", Model.parent("block/block")
                .textures(Slots.of("0", "hbm:blocks/foundry_channel_bottom", "1", "hbm:blocks/foundry_channel_inner", "2", "hbm:blocks/foundry_channel_side").and("3", "hbm:blocks/foundry_channel_top").and("particle", "hbm:blocks/foundry_channel_bottom"))
                .elements(Element.box(6, 0, 0, 10, 2, 10).name("bot").rotation(7, 0, 7, "y", 0)
                        .face(Face.of("north", "#0").uv(0, 0, 4, 2))
                        .face(Face.of("up", "#0").uv(6, 6, 10, 16))
                        .face(Face.of("down", "#0").uv(6, 6, 10, 16)),
                        Element.box(5, 0, 0, 6, 8, 11).name("wall2").rotation(6, 0, 9, "y", 0)
                        .face(Face.of("north", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("east", "#1").uv(5, 8, 16, 16))
                        .face(Face.of("south", "#2").uv(0, 8, 1, 16))
                        .face(Face.of("west", "#2").uv(0, 8, 11, 16))
                        .face(Face.of("up", "#3").uv(10, 5, 11, 16).rotation(180))
                        .face(Face.of("down", "#0").uv(15, 0, 16, 12).rotation(180)),
                        Element.box(6, 0, 10, 10, 8, 11).name("wall4").rotation(10, 0, 10, "y", 0)
                        .face(Face.of("north", "#1").uv(6, 8, 10, 16))
                        .face(Face.of("east", "#2").uv(0, 8, 2, 16))
                        .face(Face.of("south", "#2").uv(6, 8, 10, 16))
                        .face(Face.of("west", "#2").uv(14, 8, 16, 16))
                        .face(Face.of("up", "#3").uv(10, 6, 11, 10).rotation(90))
                        .face(Face.of("down", "#0").uv(0, 0, 1, 4).rotation(270)),
                        Element.box(10, 0, 0, 11, 8, 11).name("wall1").rotation(10, 0, 7, "y", 0)
                        .face(Face.of("north", "#2").uv(0, 8, 1, 16))
                        .face(Face.of("east", "#2").uv(5, 8, 16, 16))
                        .face(Face.of("south", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("west", "#1").uv(0, 8, 11, 16))
                        .face(Face.of("up", "#3").uv(5, 5, 6, 16))
                        .face(Face.of("down", "#0").uv(0, 0, 1, 12))));
        Dejson.model("hbm:block/foundry_channel_straight", Model.parent("block/block")
                .textures(Slots.of("0", "hbm:blocks/foundry_channel_bottom", "1", "hbm:blocks/foundry_channel_inner", "2", "hbm:blocks/foundry_channel_side").and("3", "hbm:blocks/foundry_channel_top").and("particle", "hbm:blocks/foundry_channel_bottom"))
                .elements(Element.box(0, 0, 6, 16, 2, 10).name("bot").rotation(8, 2.5, 8.75, "y", 0)
                        .face(Face.of("east", "#0").uv(0, 0, 4, 2))
                        .face(Face.of("west", "#0").uv(0, 0, 4, 2))
                        .face(Face.of("up", "#0").uv(6, 0, 10, 16).rotation(90))
                        .face(Face.of("down", "#0").uv(6, 0, 10, 16).rotation(270)),
                        Element.box(0, 0, 5, 16, 8, 6).name("wall2").rotation(8, 2.5, 7.75, "y", 0)
                        .face(Face.of("north", "#2").uv(0, 8, 16, 16))
                        .face(Face.of("east", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("south", "#1").uv(0, 8, 16, 16))
                        .face(Face.of("west", "#2").uv(0, 8, 1, 16))
                        .face(Face.of("up", "#3").uv(10, 0, 11, 16).rotation(90))
                        .face(Face.of("down", "#0").uv(15, 0, 16, 12).rotation(90)),
                        Element.box(0, 0, 10, 16, 8, 11).name("wall1").rotation(8, 2.5, 7.75, "y", 0)
                        .face(Face.of("north", "#1").uv(0, 8, 16, 16))
                        .face(Face.of("east", "#2").uv(0, 8, 1, 16))
                        .face(Face.of("south", "#2").uv(0, 8, 16, 16))
                        .face(Face.of("west", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("up", "#3").uv(5, 0, 6, 16).rotation(90))
                        .face(Face.of("down", "#0").uv(0, 0, 1, 16).rotation(270))));
        Dejson.model("hbm:block/foundry_channel_t", Model.parent("block/block")
                .textures(Slots.of("0", "hbm:blocks/foundry_channel_bottom", "1", "hbm:blocks/foundry_channel_inner", "2", "hbm:blocks/foundry_channel_side").and("3", "hbm:blocks/foundry_channel_top").and("particle", "hbm:blocks/foundry_channel_bottom"))
                .elements(Element.box(0, 0, 6, 16, 2, 10).name("bot").rotation(7.75, 2.5, 9.5, "y", 0)
                        .face(Face.of("east", "#0").uv(0, 0, 4, 2))
                        .face(Face.of("west", "#0").uv(0, 0, 4, 2))
                        .face(Face.of("up", "#0").uv(6, 0, 10, 16).rotation(270))
                        .face(Face.of("down", "#0").uv(6, 0, 10, 10).rotation(90)),
                        Element.box(6, 0, 10, 10, 2, 16).name("bot1").rotation(7.75, 2.5, 9.5, "y", 0)
                        .face(Face.of("south", "#0").uv(0, 0, 4, 2))
                        .face(Face.of("up", "#0").uv(0, 0, 4, 6).rotation(180))
                        .face(Face.of("down", "#0").uv(0, 0, 4, 6).rotation(180)),
                        Element.box(0, 0, 10, 6, 8, 11).name("wall2").rotation(7.75, 2.5, 9.5, "y", 0)
                        .face(Face.of("north", "#1").uv(10, 8, 16, 16))
                        .face(Face.of("east", "#1").uv(0, 8, 1, 16))
                        .face(Face.of("south", "#2").uv(0, 8, 6, 16))
                        .face(Face.of("west", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("up", "#3").uv(5, 10, 6, 16).rotation(270))
                        .face(Face.of("down", "#0").uv(15, 0, 16, 6).rotation(270)),
                        Element.box(11, 0, 10, 16, 8, 11).name("wall3").rotation(7.75, 2.5, 9.5, "y", 0)
                        .face(Face.of("north", "#1").uv(10, 8, 14, 16))
                        .face(Face.of("east", "#2").uv(0, 8, 1, 16))
                        .face(Face.of("south", "#2").uv(6, 8, 11, 16))
                        .face(Face.of("west", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("up", "#3").uv(10, 12, 11, 16).rotation(270))
                        .face(Face.of("down", "#0").uv(15, 0, 16, 5).rotation(270)),
                        Element.box(10, 0, 10, 11, 8, 16).name("wall5").rotation(7.75, 2.5, 9.5, "y", 0)
                        .face(Face.of("north", "#1").uv(0, 8, 1, 16))
                        .face(Face.of("east", "#2").uv(0, 8, 6, 16))
                        .face(Face.of("south", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("west", "#1").uv(10, 8, 16, 16))
                        .face(Face.of("up", "#3").uv(5, 10, 6, 16).rotation(180))
                        .face(Face.of("down", "#0").uv(15, 0, 16, 6)),
                        Element.box(5, 0, 11, 6, 8, 16).name("wall4").rotation(7.75, 2.5, 9.5, "y", 0)
                        .face(Face.of("north", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("east", "#1").uv(10, 8, 14, 16))
                        .face(Face.of("south", "#2").uv(0, 8, 1, 16))
                        .face(Face.of("west", "#2").uv(6, 8, 11, 16))
                        .face(Face.of("up", "#3").uv(10, 12, 11, 16))
                        .face(Face.of("down", "#0").uv(15, 0, 16, 5).rotation(180)),
                        Element.box(0, 0, 5, 16, 8, 6).name("wall1").rotation(7.75, 2.5, 9.5, "y", 0)
                        .face(Face.of("north", "#2").uv(0, 8, 16, 16))
                        .face(Face.of("east", "#2").uv(15, 8, 16, 16))
                        .face(Face.of("south", "#1").uv(0, 8, 11, 16))
                        .face(Face.of("west", "#2").uv(0, 8, 1, 16))
                        .face(Face.of("up", "#3").uv(5, 0, 6, 16).rotation(270))
                        .face(Face.of("down", "#0").uv(0, 0, 1, 16).rotation(90))));
        Dejson.model("hbm:block/foundry_mold", Model.parent("block/block")
                .textures(Slots.of("0", "hbm:blocks/foundry_basin_bottom", "1", "hbm:blocks/foundry_mold_inner", "2", "hbm:blocks/foundry_mold_side").and("3", "hbm:blocks/foundry_mold_top").and("particle", "hbm:blocks/foundry_basin_bottom"))
                .elements(Element.box(0, 0, 0, 16, 2, 16).name("bot")
                        .face(Face.of("north", "#2").uv(0, 14, 16, 16))
                        .face(Face.of("east", "#2").uv(0, 14, 16, 16))
                        .face(Face.of("south", "#2").uv(0, 14, 16, 16))
                        .face(Face.of("west", "#2").uv(0, 14, 16, 16))
                        .face(Face.of("up", "#0").uv(0, 0, 16, 16))
                        .face(Face.of("down", "#0").uv(0, 0, 16, 16)),
                        Element.box(0, 2, 0, 16, 8, 2).name("wallleft").rotation(0, 2, 0, "y", 0)
                        .face(Face.of("north", "#2").uv(0, 8, 16, 14))
                        .face(Face.of("east", "#2").uv(14, 8, 16, 14))
                        .face(Face.of("south", "#1").uv(0, 8, 16, 14))
                        .face(Face.of("west", "#2").uv(0, 8, 2, 14))
                        .face(Face.of("up", "#3").uv(0, 14, 16, 16)),
                        Element.box(0, 2, 14, 16, 8, 16).name("wallright").rotation(16, 2, 16, "y", 0)
                        .face(Face.of("north", "#1").uv(0, 8, 16, 14))
                        .face(Face.of("east", "#2").uv(0, 8, 2, 14))
                        .face(Face.of("south", "#2").uv(0, 8, 16, 14))
                        .face(Face.of("west", "#2").uv(14, 8, 16, 14))
                        .face(Face.of("up", "#3").uv(0, 0, 16, 2).rotation(180)),
                        Element.box(0, 2, 2, 2, 8, 14).name("wallfront").rotation(0, 2, 2, "y", 0)
                        .face(Face.of("east", "#1").uv(2, 8, 14, 14))
                        .face(Face.of("west", "#2").uv(2, 8, 14, 14))
                        .face(Face.of("up", "#3").uv(0, 2, 2, 14)),
                        Element.box(14, 2, 2, 16, 8, 14).name("wallback").rotation(16, 2, 14, "y", 0)
                        .face(Face.of("east", "#2").uv(2, 8, 14, 14))
                        .face(Face.of("west", "#1").uv(2, 8, 14, 14))
                        .face(Face.of("up", "#3").uv(0, 2, 2, 14).rotation(180))));
        Dejson.model("hbm:block/foundry_tank_floor", Model.of()
                .textures(Slots.of("bottom", "hbm:blocks/foundry_tank_bottom", "particle", "hbm:blocks/foundry_tank_bottom"))
                .elements(Element.box(0, 0, 0, 16, 2, 16)
                        .face(Face.of("up", "#bottom"))
                        .face(Face.of("down", "#bottom").cullface("down"))));
        Dejson.model("hbm:block/foundry_tank_wall", Model.of()
                .textures(Slots.of("particle", "#outer"))
                .elements(Element.box(0, 0, 0, 16, 16, 2)
                        .face(Face.of("north", "#outer").cullface("north"))
                        .face(Face.of("south", "#inner"))
                        .face(Face.of("up", "#top"))
                        .face(Face.of("east", "#inner"))
                        .face(Face.of("west", "#inner"))));
        Dejson.model("hbm:block/foundry_tank_wall_side_bottom", Models.parented("hbm:block/foundry_tank_wall", Slots.of("outer", "hbm:blocks/foundry_tank_side", "inner", "hbm:blocks/foundry_tank_bottom", "top", "hbm:blocks/foundry_tank_top")));
        Dejson.model("hbm:block/foundry_tank_wall_side_inner", Models.parented("hbm:block/foundry_tank_wall", Slots.of("outer", "hbm:blocks/foundry_tank_side", "inner", "hbm:blocks/foundry_tank_inner", "top", "hbm:blocks/foundry_tank_top")));
        Dejson.model("hbm:block/foundry_tank_wall_side_outlet_bottom", Models.parented("hbm:block/foundry_tank_wall", Slots.of("outer", "hbm:blocks/foundry_tank_side_outlet", "inner", "hbm:blocks/foundry_tank_bottom", "top", "hbm:blocks/foundry_tank_top")));
        Dejson.model("hbm:block/foundry_tank_wall_side_outlet_inner", Models.parented("hbm:block/foundry_tank_wall", Slots.of("outer", "hbm:blocks/foundry_tank_side_outlet", "inner", "hbm:blocks/foundry_tank_inner", "top", "hbm:blocks/foundry_tank_top")));
        Dejson.model("hbm:block/foundry_tank_wall_upper_bottom", Models.parented("hbm:block/foundry_tank_wall", Slots.of("outer", "hbm:blocks/foundry_tank_upper", "inner", "hbm:blocks/foundry_tank_bottom", "top", "hbm:blocks/foundry_tank_top")));
        Dejson.model("hbm:block/foundry_tank_wall_upper_inner", Models.parented("hbm:block/foundry_tank_wall", Slots.of("outer", "hbm:blocks/foundry_tank_upper", "inner", "hbm:blocks/foundry_tank_inner", "top", "hbm:blocks/foundry_tank_top")));
        Dejson.model("hbm:block/foundry_tank_wall_upper_outlet_bottom", Models.parented("hbm:block/foundry_tank_wall", Slots.of("outer", "hbm:blocks/foundry_tank_upper_outlet", "inner", "hbm:blocks/foundry_tank_bottom", "top", "hbm:blocks/foundry_tank_top")));
        Dejson.model("hbm:block/foundry_tank_wall_upper_outlet_inner", Models.parented("hbm:block/foundry_tank_wall", Slots.of("outer", "hbm:blocks/foundry_tank_upper_outlet", "inner", "hbm:blocks/foundry_tank_inner", "top", "hbm:blocks/foundry_tank_top")));
        Dejson.model("hbm:block/gas_asbestos", Model.of().ambientOcclusion(false)
                .elements());
        Dejson.model("hbm:block/gas_meltdown", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/block_steel")));
        Dejson.model("hbm:block/gas_monoxide", Model.of().ambientOcclusion(false)
                .elements());
        Dejson.model("hbm:block/gas_radon", Model.of().ambientOcclusion(false)
                .elements());
        Dejson.model("hbm:block/gas_radon_dense", Model.of().ambientOcclusion(false)
                .elements());
        Dejson.model("hbm:block/gas_radon_tomb", Model.of().ambientOcclusion(false)
                .elements());
    }

    private static void part2() {
        Dejson.model("hbm:block/ladder_aluminium", Model.of().ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/ladder_aluminium", "texture", "hbm:blocks/ladder_aluminium"))
                .elements(Element.box(0, 0, 15.2, 16, 16, 15.2).shade(false)
                        .face(Face.of("north", "#texture").uv(0, 0, 16, 16))
                        .face(Face.of("south", "#texture").uv(0, 0, 16, 16))));
        Dejson.model("hbm:block/ladder_cobalt", Model.of().ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/ladder_cobalt", "texture", "hbm:blocks/ladder_cobalt"))
                .elements(Element.box(0, 0, 15.2, 16, 16, 15.2).shade(false)
                        .face(Face.of("north", "#texture").uv(0, 0, 16, 16))
                        .face(Face.of("south", "#texture").uv(0, 0, 16, 16))));
        Dejson.model("hbm:block/ladder_copper", Model.of().ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/ladder_copper", "texture", "hbm:blocks/ladder_copper"))
                .elements(Element.box(0, 0, 15.2, 16, 16, 15.2).shade(false)
                        .face(Face.of("north", "#texture").uv(0, 0, 16, 16))
                        .face(Face.of("south", "#texture").uv(0, 0, 16, 16))));
        Dejson.model("hbm:block/ladder_gold", Model.of().ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/ladder_gold", "texture", "hbm:blocks/ladder_gold"))
                .elements(Element.box(0, 0, 15.2, 16, 16, 15.2).shade(false)
                        .face(Face.of("north", "#texture").uv(0, 0, 16, 16))
                        .face(Face.of("south", "#texture").uv(0, 0, 16, 16))));
        Dejson.model("hbm:block/ladder_iron", Model.of().ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/ladder_iron", "texture", "hbm:blocks/ladder_iron"))
                .elements(Element.box(0, 0, 15.2, 16, 16, 15.2).shade(false)
                        .face(Face.of("north", "#texture").uv(0, 0, 16, 16))
                        .face(Face.of("south", "#texture").uv(0, 0, 16, 16))));
        Dejson.model("hbm:block/ladder_lead", Model.of().ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/ladder_lead", "texture", "hbm:blocks/ladder_lead"))
                .elements(Element.box(0, 0, 15.2, 16, 16, 15.2).shade(false)
                        .face(Face.of("north", "#texture").uv(0, 0, 16, 16))
                        .face(Face.of("south", "#texture").uv(0, 0, 16, 16))));
        Dejson.model("hbm:block/ladder_red", Model.of().ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/ladder_red", "texture", "hbm:blocks/ladder_red"))
                .elements(Element.box(0, 0, 15.2, 16, 16, 15.2).shade(false)
                        .face(Face.of("north", "#texture").uv(0, 0, 16, 16))
                        .face(Face.of("south", "#texture").uv(0, 0, 16, 16))));
        Dejson.model("hbm:block/ladder_red_top", Model.of().ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/ladder_red_top", "texture", "hbm:blocks/ladder_red_top"))
                .elements(Element.box(0, 0, 15.2, 16, 4, 15.2).shade(false)
                        .face(Face.of("north", "#texture").uv(0, 0, 16, 16))
                        .face(Face.of("south", "#texture").uv(0, 0, 16, 16))));
        Dejson.model("hbm:block/ladder_steel", Model.of().ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/ladder_steel", "texture", "hbm:blocks/ladder_steel"))
                .elements(Element.box(0, 0, 15.2, 16, 16, 15.2).shade(false)
                        .face(Face.of("north", "#texture").uv(0, 0, 16, 16))
                        .face(Face.of("south", "#texture").uv(0, 0, 16, 16))));
        Dejson.model("hbm:block/ladder_sturdy", Model.of().ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/ladder_sturdy", "texture", "hbm:blocks/ladder_sturdy"))
                .elements(Element.box(0, 0, 15.2, 16, 16, 15.2).shade(false)
                        .face(Face.of("north", "#texture").uv(0, 0, 16, 16))
                        .face(Face.of("south", "#texture").uv(0, 0, 16, 16))));
        Dejson.model("hbm:block/ladder_titanium", Model.of().ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/ladder_titanium", "texture", "hbm:blocks/ladder_titanium"))
                .elements(Element.box(0, 0, 15.2, 16, 16, 15.2).shade(false)
                        .face(Face.of("north", "#texture").uv(0, 0, 16, 16))
                        .face(Face.of("south", "#texture").uv(0, 0, 16, 16))));
        Dejson.model("hbm:block/ladder_tungsten", Model.of().ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/ladder_tungsten", "texture", "hbm:blocks/ladder_tungsten"))
                .elements(Element.box(0, 0, 15.2, 16, 16, 15.2).shade(false)
                        .face(Face.of("north", "#texture").uv(0, 0, 16, 16))
                        .face(Face.of("south", "#texture").uv(0, 0, 16, 16))));
        Dejson.model("hbm:block/machine_capacitor_copper", Models.parented("hbm:block/capacitor", Slots.of("top", "hbm:blocks/capacitor_copper_top", "side", "hbm:blocks/capacitor_copper_side", "bottom", "hbm:blocks/capacitor_copper_bottom").and("inner_top", "hbm:blocks/capacitor_copper_inner_top").and("inner_side", "hbm:blocks/capacitor_copper_inner_side").and("particle", "hbm:blocks/capacitor_copper_inner_side")));
        Dejson.model("hbm:block/machine_capacitor_gold", Models.parented("hbm:block/capacitor", Slots.of("top", "hbm:blocks/capacitor_gold_top", "side", "hbm:blocks/capacitor_gold_side", "bottom", "hbm:blocks/capacitor_gold_bottom").and("inner_top", "hbm:blocks/capacitor_gold_inner_top").and("inner_side", "hbm:blocks/capacitor_gold_inner_side").and("particle", "hbm:blocks/capacitor_gold_inner_side")));
        Dejson.model("hbm:block/machine_capacitor_niobium", Models.parented("hbm:block/capacitor", Slots.of("top", "hbm:blocks/capacitor_niobium_top", "side", "hbm:blocks/capacitor_niobium_side", "bottom", "hbm:blocks/capacitor_niobium_bottom").and("inner_top", "hbm:blocks/capacitor_niobium_inner_top").and("inner_side", "hbm:blocks/capacitor_niobium_inner_side").and("particle", "hbm:blocks/capacitor_niobium_inner_side")));
        Dejson.model("hbm:block/machine_difurnace_off", Models.parented("minecraft:block/orientable", Slots.of("top", "hbm:blocks/difurnace_top_off", "front", "hbm:blocks/difurnace_front_off", "side", "hbm:blocks/difurnace_side")));
        Dejson.model("hbm:block/machine_difurnace_off_tall", Models.parented("minecraft:block/orientable", Slots.of("top", "hbm:blocks/difurnace_top_off", "front", "hbm:blocks/difurnace_front_off_tall", "side", "hbm:blocks/difurnace_side_tall")));
        Dejson.model("hbm:block/machine_difurnace_on", Models.parented("minecraft:block/orientable", Slots.of("top", "hbm:blocks/difurnace_top_on", "front", "hbm:blocks/difurnace_front_on", "side", "hbm:blocks/difurnace_side")));
        Dejson.model("hbm:block/machine_difurnace_on_tall", Models.parented("minecraft:block/orientable", Slots.of("top", "hbm:blocks/difurnace_top_on", "front", "hbm:blocks/difurnace_front_on_tall", "side", "hbm:blocks/difurnace_side_tall")));
        Dejson.model("hbm:block/machine_dummy_empty", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/block_steel")));
        Dejson.model("hbm:block/machine_electric_furnace_off", Models.parented("minecraft:block/orientable", Slots.of("top", "hbm:blocks/machine_electric_furnace_top", "front", "hbm:blocks/machine_electric_furnace_front_off", "side", "hbm:blocks/machine_electric_furnace_side").and("bottom", "hbm:blocks/machine_electric_furnace_bottom")));
        Dejson.model("hbm:block/machine_electric_furnace_on", Models.parented("minecraft:block/orientable", Slots.of("top", "hbm:blocks/machine_electric_furnace_top", "front", "hbm:blocks/machine_electric_furnace_front_on", "side", "hbm:blocks/machine_electric_furnace_side").and("bottom", "hbm:blocks/machine_electric_furnace_bottom")));
        Dejson.model("hbm:block/machine_furnace_brick_off", Models.parented("minecraft:block/cube", Slots.of("particle", "hbm:blocks/machine_furnace_brick_side", "down", "hbm:blocks/machine_furnace_brick_bottom", "up", "hbm:blocks/machine_furnace_brick_top").and("north", "hbm:blocks/machine_furnace_brick_front_off").and("south", "hbm:blocks/machine_furnace_brick_side").and("east", "hbm:blocks/machine_furnace_brick_side").and("west", "hbm:blocks/machine_furnace_brick_side")));
        Dejson.model("hbm:block/machine_furnace_brick_on", Models.parented("minecraft:block/cube", Slots.of("particle", "hbm:blocks/machine_furnace_brick_side", "down", "hbm:blocks/machine_furnace_brick_bottom", "up", "hbm:blocks/machine_furnace_brick_top").and("north", "hbm:blocks/machine_furnace_brick_front_on").and("south", "hbm:blocks/machine_furnace_brick_side").and("east", "hbm:blocks/machine_furnace_brick_side").and("west", "hbm:blocks/machine_furnace_brick_side")));
        Dejson.model("hbm:block/machine_pump_electric", Models.parented("minecraft:block/air", Slots.EMPTY));
        Dejson.model("hbm:block/machine_pump_steam", Models.parented("minecraft:block/air", Slots.EMPTY));
        Dejson.model("hbm:block/machine_radgen", Models.parented("minecraft:block/air", Slots.EMPTY));
        Dejson.model("hbm:block/machine_weapon_table", Models.parented("minecraft:block/cube_bottom_top", Slots.of("bottom", "hbm:blocks/gun_table_bottom", "side", "hbm:blocks/gun_table_side", "top", "hbm:blocks/gun_table_top")));
        Dejson.model("hbm:block/ore_volcano", Model.parent("minecraft:block/block").renderType("minecraft:cutout")
                .textures(Slots.of("particle", "minecraft:block/bedrock", "bedrock", "minecraft:block/bedrock", "overlay", "hbm:blocks/molten_overlay"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("down", "#bedrock").cullface("down"))
                        .face(Face.of("up", "#bedrock").cullface("up"))
                        .face(Face.of("north", "#bedrock").cullface("north"))
                        .face(Face.of("south", "#bedrock").cullface("south"))
                        .face(Face.of("west", "#bedrock").cullface("west"))
                        .face(Face.of("east", "#bedrock").cullface("east")),
                        Element.box(-0.001, -0.001, -0.001, 16.001, 16.001, 16.001)
                        .face(Face.of("down", "#overlay").uv(0, 0, 16, 16))
                        .face(Face.of("up", "#overlay").uv(0, 0, 16, 16))
                        .face(Face.of("north", "#overlay").uv(0, 0, 16, 16))
                        .face(Face.of("south", "#overlay").uv(0, 0, 16, 16))
                        .face(Face.of("west", "#overlay").uv(0, 0, 16, 16))
                        .face(Face.of("east", "#overlay").uv(0, 0, 16, 16))));
        Dejson.model("hbm:block/pile_device", Models.parented("minecraft:block/air", Slots.EMPTY));
    }

    private static void part3() {
        Dejson.model("hbm:block/pipe_anchor_down", Model.parent("minecraft:block/block")
                .textures(Slots.of("all", "hbm:blocks/block_steel", "particle", "hbm:blocks/block_steel"))
                .elements(Element.box(4, 4, 4, 12, 16, 12)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all"))));
        Dejson.model("hbm:block/pipe_anchor_east", Model.parent("minecraft:block/block")
                .textures(Slots.of("all", "hbm:blocks/block_steel", "particle", "hbm:blocks/block_steel"))
                .elements(Element.box(0, 4, 4, 12, 12, 12)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all"))));
        Dejson.model("hbm:block/pipe_anchor_north", Model.parent("minecraft:block/block")
                .textures(Slots.of("all", "hbm:blocks/block_steel", "particle", "hbm:blocks/block_steel"))
                .elements(Element.box(4, 4, 4, 12, 12, 16)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all"))));
        Dejson.model("hbm:block/pipe_anchor_south", Model.parent("minecraft:block/block")
                .textures(Slots.of("all", "hbm:blocks/block_steel", "particle", "hbm:blocks/block_steel"))
                .elements(Element.box(4, 4, 0, 12, 12, 12)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all"))));
        Dejson.model("hbm:block/pipe_anchor_up", Model.parent("minecraft:block/block")
                .textures(Slots.of("all", "hbm:blocks/block_steel", "particle", "hbm:blocks/block_steel"))
                .elements(Element.box(4, 0, 4, 12, 12, 12)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all"))));
        Dejson.model("hbm:block/pipe_anchor_west", Model.parent("minecraft:block/block")
                .textures(Slots.of("all", "hbm:blocks/block_steel", "particle", "hbm:blocks/block_steel"))
                .elements(Element.box(4, 4, 4, 16, 12, 12)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all"))));
        // plant_reeds top segment (1.7 drawCrossedSquares, 0.75 grey via tint 0); RenderReeds draws the stem below.
        Dejson.model("hbm:block/plant_reeds", Model.of().ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/reeds_top", "tex0", "hbm:blocks/reeds_top"))
                .elements(Element.box(0.8, 0, 8, 15.2, 16, 8).rotation(8, 8, 8, "y", 45, true).shade(false)
                        .face(Face.of("north", "#tex0").uv(0, 0, 16, 16).tintIndex(0))
                        .face(Face.of("south", "#tex0").uv(0, 0, 16, 16).tintIndex(0)),
                        Element.box(0.8, 0, 8, 15.2, 16, 8).rotation(8, 8, 8, "y", -45, true).shade(false)
                        .face(Face.of("north", "#tex0").uv(0, 0, 16, 16).tintIndex(0))
                        .face(Face.of("south", "#tex0").uv(0, 0, 16, 16).tintIndex(0))));
        Dejson.model("hbm:block/radio_torch_template", Model.of().ambientOcclusion(false)
                .textures(Slots.of("particle", "#torch"))
                .elements(Element.box(7, 0, 7, 9, 10, 9).shade(false)
                        .face(Face.of("down", "#particle").uv(7, 13, 9, 15))
                        .face(Face.of("up", "#particle").uv(7, 6, 9, 8)),
                        Element.box(7, 0, 0, 9, 16, 16).shade(false)
                        .face(Face.of("west", "#particle").uv(0, 0, 16, 16))
                        .face(Face.of("east", "#particle").uv(0, 0, 16, 16)),
                        Element.box(0, 0, 7, 16, 16, 9).shade(false)
                        .face(Face.of("north", "#particle").uv(0, 0, 16, 16))
                        .face(Face.of("south", "#particle").uv(0, 0, 16, 16))));
        Dejson.model("hbm:block/rbmk/rbmk_absorber_column_none", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_absorber_top", "side", "hbm:blocks/rbmk/rbmk_absorber_side", "bottom", "hbm:blocks/rbmk/rbmk_absorber_top").and("particle", "hbm:blocks/rbmk/rbmk_absorber_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#top"))));
        Dejson.model("hbm:block/rbmk/rbmk_blank_column_none", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_blank_top", "side", "hbm:blocks/rbmk/rbmk_blank_side", "bottom", "hbm:blocks/rbmk/rbmk_blank_top").and("particle", "hbm:blocks/rbmk/rbmk_blank_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#top"))));
        Dejson.model("hbm:block/rbmk/rbmk_boiler_column_none", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_boiler_top", "side", "hbm:blocks/rbmk/rbmk_boiler_side", "bottom", "hbm:blocks/rbmk/rbmk_boiler_top").and("particle", "hbm:blocks/rbmk/rbmk_boiler_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#top"))));
        Dejson.model("hbm:block/rbmk/rbmk_control_auto_column_none", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_control_auto_top", "side", "hbm:blocks/rbmk/rbmk_control_auto_side", "bottom", "hbm:blocks/rbmk/rbmk_control_auto_top").and("particle", "hbm:blocks/rbmk/rbmk_control_auto_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#top"))));
        Dejson.model("hbm:block/rbmk/rbmk_control_auto_column_top", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_control_auto_top", "side", "hbm:blocks/rbmk/rbmk_control_auto_side", "bottom", "hbm:blocks/rbmk/rbmk_control_auto_top").and("particle", "hbm:blocks/rbmk/rbmk_control_auto_side").and("pipe_top", "hbm:blocks/rbmk/rbmk_control_auto_pipe_top").and("pipe_side", "hbm:blocks/rbmk/rbmk_control_auto_pipe_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#top")),
                        Element.box(1, 16, 1, 7, 18, 7)
                        .face(Face.of("up", "#pipe_top"))
                        .face(Face.of("down", "#pipe_top"))
                        .face(Face.of("north", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("south", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("west", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("east", "#pipe_side").uv(9, 14, 15, 16)),
                        Element.box(1, 16, 9, 7, 18, 15)
                        .face(Face.of("up", "#pipe_top"))
                        .face(Face.of("down", "#pipe_top"))
                        .face(Face.of("north", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("south", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("west", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("east", "#pipe_side").uv(1, 14, 7, 16)),
                        Element.box(9, 16, 1, 15, 18, 7)
                        .face(Face.of("up", "#pipe_top"))
                        .face(Face.of("down", "#pipe_top"))
                        .face(Face.of("north", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("south", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("west", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("east", "#pipe_side").uv(9, 14, 15, 16)),
                        Element.box(9, 16, 9, 15, 18, 15)
                        .face(Face.of("up", "#pipe_top"))
                        .face(Face.of("down", "#pipe_top"))
                        .face(Face.of("north", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("south", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("west", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("east", "#pipe_side").uv(1, 14, 7, 16))));
        Dejson.model("hbm:block/rbmk/rbmk_control_column_none", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_control_top", "side", "hbm:blocks/rbmk/rbmk_control_side", "bottom", "hbm:blocks/rbmk/rbmk_control_top").and("particle", "hbm:blocks/rbmk/rbmk_control_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#top"))));
        Dejson.model("hbm:block/rbmk/rbmk_control_column_top", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_control_top", "side", "hbm:blocks/rbmk/rbmk_control_side", "bottom", "hbm:blocks/rbmk/rbmk_control_top").and("particle", "hbm:blocks/rbmk/rbmk_control_side").and("pipe_top", "hbm:blocks/rbmk/rbmk_control_pipe_top").and("pipe_side", "hbm:blocks/rbmk/rbmk_control_pipe_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#top")),
                        Element.box(1, 16, 1, 7, 18, 7)
                        .face(Face.of("up", "#pipe_top"))
                        .face(Face.of("down", "#pipe_top"))
                        .face(Face.of("north", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("south", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("west", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("east", "#pipe_side").uv(9, 14, 15, 16)),
                        Element.box(1, 16, 9, 7, 18, 15)
                        .face(Face.of("up", "#pipe_top"))
                        .face(Face.of("down", "#pipe_top"))
                        .face(Face.of("north", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("south", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("west", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("east", "#pipe_side").uv(1, 14, 7, 16)),
                        Element.box(9, 16, 1, 15, 18, 7)
                        .face(Face.of("up", "#pipe_top"))
                        .face(Face.of("down", "#pipe_top"))
                        .face(Face.of("north", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("south", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("west", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("east", "#pipe_side").uv(9, 14, 15, 16)),
                        Element.box(9, 16, 9, 15, 18, 15)
                        .face(Face.of("up", "#pipe_top"))
                        .face(Face.of("down", "#pipe_top"))
                        .face(Face.of("north", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("south", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("west", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("east", "#pipe_side").uv(1, 14, 7, 16))));
        Dejson.model("hbm:block/rbmk/rbmk_control_mod_column_none", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_control_mod_top", "side", "hbm:blocks/rbmk/rbmk_control_mod_side", "bottom", "hbm:blocks/rbmk/rbmk_control_mod_top").and("particle", "hbm:blocks/rbmk/rbmk_control_mod_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#top"))));
        Dejson.model("hbm:block/rbmk/rbmk_control_mod_column_top", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_control_mod_top", "side", "hbm:blocks/rbmk/rbmk_control_mod_side", "bottom", "hbm:blocks/rbmk/rbmk_control_mod_top").and("particle", "hbm:blocks/rbmk/rbmk_control_mod_side").and("pipe_top", "hbm:blocks/rbmk/rbmk_control_mod_pipe_top").and("pipe_side", "hbm:blocks/rbmk/rbmk_control_mod_pipe_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#top")),
                        Element.box(1, 16, 1, 7, 18, 7)
                        .face(Face.of("up", "#pipe_top"))
                        .face(Face.of("down", "#pipe_top"))
                        .face(Face.of("north", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("south", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("west", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("east", "#pipe_side").uv(9, 14, 15, 16)),
                        Element.box(1, 16, 9, 7, 18, 15)
                        .face(Face.of("up", "#pipe_top"))
                        .face(Face.of("down", "#pipe_top"))
                        .face(Face.of("north", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("south", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("west", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("east", "#pipe_side").uv(1, 14, 7, 16)),
                        Element.box(9, 16, 1, 15, 18, 7)
                        .face(Face.of("up", "#pipe_top"))
                        .face(Face.of("down", "#pipe_top"))
                        .face(Face.of("north", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("south", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("west", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("east", "#pipe_side").uv(9, 14, 15, 16)),
                        Element.box(9, 16, 9, 15, 18, 15)
                        .face(Face.of("up", "#pipe_top"))
                        .face(Face.of("down", "#pipe_top"))
                        .face(Face.of("north", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("south", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("west", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("east", "#pipe_side").uv(1, 14, 7, 16))));
        Dejson.model("hbm:block/rbmk/rbmk_control_reasim_auto_column_none", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_control_reasim_auto_top", "side", "hbm:blocks/rbmk/rbmk_control_reasim_auto_side", "bottom", "hbm:blocks/rbmk/rbmk_control_reasim_auto_bottom").and("particle", "hbm:blocks/rbmk/rbmk_control_reasim_auto_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#top"))));
        Dejson.model("hbm:block/rbmk/rbmk_control_reasim_auto_column_top", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_control_reasim_auto_top", "side", "hbm:blocks/rbmk/rbmk_control_reasim_auto_side", "bottom", "hbm:blocks/rbmk/rbmk_control_reasim_auto_bottom").and("particle", "hbm:blocks/rbmk/rbmk_control_reasim_auto_side").and("pipe_top", "hbm:blocks/rbmk/rbmk_control_reasim_auto_pipe_top").and("pipe_side", "hbm:blocks/rbmk/rbmk_control_reasim_auto_pipe_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#top")),
                        Element.box(1, 16, 1, 7, 18, 7)
                        .face(Face.of("up", "#pipe_top"))
                        .face(Face.of("down", "#pipe_top"))
                        .face(Face.of("north", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("south", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("west", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("east", "#pipe_side").uv(9, 14, 15, 16)),
                        Element.box(1, 16, 9, 7, 18, 15)
                        .face(Face.of("up", "#pipe_top"))
                        .face(Face.of("down", "#pipe_top"))
                        .face(Face.of("north", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("south", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("west", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("east", "#pipe_side").uv(1, 14, 7, 16)),
                        Element.box(9, 16, 1, 15, 18, 7)
                        .face(Face.of("up", "#pipe_top"))
                        .face(Face.of("down", "#pipe_top"))
                        .face(Face.of("north", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("south", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("west", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("east", "#pipe_side").uv(9, 14, 15, 16)),
                        Element.box(9, 16, 9, 15, 18, 15)
                        .face(Face.of("up", "#pipe_top"))
                        .face(Face.of("down", "#pipe_top"))
                        .face(Face.of("north", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("south", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("west", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("east", "#pipe_side").uv(1, 14, 7, 16))));
        Dejson.model("hbm:block/rbmk/rbmk_control_reasim_column_none", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_control_reasim_top", "side", "hbm:blocks/rbmk/rbmk_control_reasim_side", "bottom", "hbm:blocks/rbmk/rbmk_control_reasim_bottom").and("particle", "hbm:blocks/rbmk/rbmk_control_reasim_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#top"))));
        Dejson.model("hbm:block/rbmk/rbmk_control_reasim_column_top", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_control_reasim_top", "side", "hbm:blocks/rbmk/rbmk_control_reasim_side", "bottom", "hbm:blocks/rbmk/rbmk_control_reasim_bottom").and("particle", "hbm:blocks/rbmk/rbmk_control_reasim_side").and("pipe_top", "hbm:blocks/rbmk/rbmk_control_reasim_pipe_top").and("pipe_side", "hbm:blocks/rbmk/rbmk_control_reasim_pipe_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#top")),
                        Element.box(1, 16, 1, 7, 18, 7)
                        .face(Face.of("up", "#pipe_top"))
                        .face(Face.of("down", "#pipe_top"))
                        .face(Face.of("north", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("south", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("west", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("east", "#pipe_side").uv(9, 14, 15, 16)),
                        Element.box(1, 16, 9, 7, 18, 15)
                        .face(Face.of("up", "#pipe_top"))
                        .face(Face.of("down", "#pipe_top"))
                        .face(Face.of("north", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("south", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("west", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("east", "#pipe_side").uv(1, 14, 7, 16)),
                        Element.box(9, 16, 1, 15, 18, 7)
                        .face(Face.of("up", "#pipe_top"))
                        .face(Face.of("down", "#pipe_top"))
                        .face(Face.of("north", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("south", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("west", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("east", "#pipe_side").uv(9, 14, 15, 16)),
                        Element.box(9, 16, 9, 15, 18, 15)
                        .face(Face.of("up", "#pipe_top"))
                        .face(Face.of("down", "#pipe_top"))
                        .face(Face.of("north", "#pipe_side").uv(1, 14, 7, 16))
                        .face(Face.of("south", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("west", "#pipe_side").uv(9, 14, 15, 16))
                        .face(Face.of("east", "#pipe_side").uv(1, 14, 7, 16))));
        Dejson.model("hbm:block/rbmk/rbmk_cooler_column_none", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_cooler_top", "side", "hbm:blocks/rbmk/rbmk_cooler_side", "bottom", "hbm:blocks/rbmk/rbmk_cooler_top").and("particle", "hbm:blocks/rbmk/rbmk_cooler_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#top"))));
        Dejson.model("hbm:block/rbmk/rbmk_element_column_none", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_element_top", "side", "hbm:blocks/rbmk/rbmk_element_side", "bottom", "hbm:blocks/rbmk/rbmk_element_top").and("particle", "hbm:blocks/rbmk/rbmk_element_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))));
        Dejson.model("hbm:block/rbmk/rbmk_element_mod_column_none", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_element_mod_top", "side", "hbm:blocks/rbmk/rbmk_element_mod_side", "bottom", "hbm:blocks/rbmk/rbmk_element_mod_top").and("particle", "hbm:blocks/rbmk/rbmk_element_mod_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))));
        Dejson.model("hbm:block/rbmk/rbmk_element_reasim_column_none", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_element_reasim_top", "side", "hbm:blocks/rbmk/rbmk_element_reasim_side", "bottom", "hbm:blocks/rbmk/rbmk_element_reasim_top").and("particle", "hbm:blocks/rbmk/rbmk_element_reasim_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))));
        Dejson.model("hbm:block/rbmk/rbmk_element_reasim_mod_column_none", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_element_reasim_mod_top", "side", "hbm:blocks/rbmk/rbmk_element_reasim_mod_side", "bottom", "hbm:blocks/rbmk/rbmk_element_reasim_mod_top").and("particle", "hbm:blocks/rbmk/rbmk_element_reasim_mod_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))));
        Dejson.model("hbm:block/rbmk/rbmk_heater_column_none", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_heater_top", "side", "hbm:blocks/rbmk/rbmk_heater_side", "bottom", "hbm:blocks/rbmk/rbmk_heater_top").and("particle", "hbm:blocks/rbmk/rbmk_heater_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#top"))));
        Dejson.model("hbm:block/rbmk/rbmk_moderator_column_none", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_moderator_top", "side", "hbm:blocks/rbmk/rbmk_moderator_side", "bottom", "hbm:blocks/rbmk/rbmk_moderator_top").and("particle", "hbm:blocks/rbmk/rbmk_moderator_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#top"))));
    }

    private static void part4() {
        Dejson.model("hbm:block/rbmk/rbmk_outgasser_column_none", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_outgasser_top", "side", "hbm:blocks/rbmk/rbmk_outgasser_side", "bottom", "hbm:blocks/rbmk/rbmk_outgasser_top").and("particle", "hbm:blocks/rbmk/rbmk_outgasser_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#top"))));
        Dejson.model("hbm:block/rbmk/rbmk_reflector_column_none", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_reflector_top", "side", "hbm:blocks/rbmk/rbmk_reflector_side", "bottom", "hbm:blocks/rbmk/rbmk_reflector_top").and("particle", "hbm:blocks/rbmk/rbmk_reflector_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#top"))));
        Dejson.model("hbm:block/rbmk/rbmk_storage_column_none", Model.of()
                .textures(Slots.of("top", "hbm:blocks/rbmk/rbmk_storage_top", "side", "hbm:blocks/rbmk/rbmk_storage_side", "bottom", "hbm:blocks/rbmk/rbmk_storage_top").and("particle", "hbm:blocks/rbmk/rbmk_storage_side"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))
                        .face(Face.of("down", "#bottom"))
                        .face(Face.of("up", "#top"))));
        Dejson.model("hbm:block/rbmk_absorber_base", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/rbmk/rbmk_absorber_side", "side", "hbm:blocks/rbmk/rbmk_absorber_side", "top", "hbm:blocks/rbmk/rbmk_absorber_top").and("cover_side", "hbm:blocks/rbmk/rbmk_absorber_cover_side").and("cover_top", "hbm:blocks/rbmk/rbmk_absorber_cover_top").and("glass_side", "hbm:blocks/rbmk/rbmk_absorber_glass_side").and("glass_top", "hbm:blocks/rbmk/rbmk_absorber_glass_top")));
        Dejson.model("hbm:block/rbmk_blank_base", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/rbmk/rbmk_blank_side", "side", "hbm:blocks/rbmk/rbmk_blank_side", "top", "hbm:blocks/rbmk/rbmk_blank_top").and("cover_side", "hbm:blocks/rbmk/rbmk_blank_cover_side").and("cover_top", "hbm:blocks/rbmk/rbmk_blank_cover_top").and("glass_side", "hbm:blocks/rbmk/rbmk_blank_glass_side").and("glass_top", "hbm:blocks/rbmk/rbmk_blank_glass_top")));
        Dejson.model("hbm:block/rbmk_boiler_base", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/rbmk/rbmk_boiler_side", "side", "hbm:blocks/rbmk/rbmk_boiler_side", "top", "hbm:blocks/rbmk/rbmk_boiler_top").and("cover_side", "hbm:blocks/rbmk/rbmk_boiler_cover_side").and("cover_top", "hbm:blocks/rbmk/rbmk_boiler_cover_top").and("glass_side", "hbm:blocks/rbmk/rbmk_boiler_glass_side").and("glass_top", "hbm:blocks/rbmk/rbmk_boiler_glass_top").and("pipe_side", "hbm:blocks/rbmk/rbmk_boiler_pipe_side").and("pipe_top", "hbm:blocks/rbmk/rbmk_boiler_pipe_top")));
        Dejson.model("hbm:block/rbmk_console", Models.parented("hbm:block/machine_dummy_empty", Slots.EMPTY));
        Dejson.model("hbm:block/rbmk_control_auto_base", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/rbmk/rbmk_control_auto_side", "side", "hbm:blocks/rbmk/rbmk_control_auto_side", "top", "hbm:blocks/rbmk/rbmk_control_auto_top").and("bottom", "hbm:blocks/rbmk/rbmk_control_auto_top").and("pipe_side", "hbm:blocks/rbmk/rbmk_control_auto_pipe_side").and("pipe_top", "hbm:blocks/rbmk/rbmk_control_auto_pipe_top")));
        Dejson.model("hbm:block/rbmk_control_base", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/rbmk/rbmk_control_side", "side", "hbm:blocks/rbmk/rbmk_control_side", "top", "hbm:blocks/rbmk/rbmk_control_top").and("bottom", "hbm:blocks/rbmk/rbmk_control_top").and("pipe_side", "hbm:blocks/rbmk/rbmk_control_pipe_side").and("pipe_top", "hbm:blocks/rbmk/rbmk_control_pipe_top")));
        Dejson.model("hbm:block/rbmk_control_mod_base", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/rbmk/rbmk_control_mod_side", "side", "hbm:blocks/rbmk/rbmk_control_mod_side", "top", "hbm:blocks/rbmk/rbmk_control_mod_top").and("bottom", "hbm:blocks/rbmk/rbmk_control_mod_top").and("pipe_side", "hbm:blocks/rbmk/rbmk_control_mod_pipe_side").and("pipe_top", "hbm:blocks/rbmk/rbmk_control_mod_pipe_top")));
        Dejson.model("hbm:block/rbmk_control_reasim_auto_base", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/rbmk/rbmk_control_reasim_auto_side", "side", "hbm:blocks/rbmk/rbmk_control_reasim_auto_side", "top", "hbm:blocks/rbmk/rbmk_control_reasim_auto_top").and("bottom", "hbm:blocks/rbmk/rbmk_control_reasim_auto_bottom").and("pipe_side", "hbm:blocks/rbmk/rbmk_control_reasim_auto_pipe_side").and("pipe_top", "hbm:blocks/rbmk/rbmk_control_reasim_auto_pipe_top")));
        Dejson.model("hbm:block/rbmk_control_reasim_base", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/rbmk/rbmk_control_reasim_side", "side", "hbm:blocks/rbmk/rbmk_control_reasim_side", "top", "hbm:blocks/rbmk/rbmk_control_reasim_top").and("bottom", "hbm:blocks/rbmk/rbmk_control_reasim_bottom").and("pipe_side", "hbm:blocks/rbmk/rbmk_control_reasim_pipe_side").and("pipe_top", "hbm:blocks/rbmk/rbmk_control_reasim_pipe_top")));
        Dejson.model("hbm:block/rbmk_cooler_base", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/rbmk/rbmk_cooler_side", "side", "hbm:blocks/rbmk/rbmk_cooler_side", "top", "hbm:blocks/rbmk/rbmk_cooler_top").and("cover_side", "hbm:blocks/rbmk/rbmk_cooler_cover_side").and("cover_top", "hbm:blocks/rbmk/rbmk_cooler_cover_top").and("glass_side", "hbm:blocks/rbmk/rbmk_cooler_glass_side").and("glass_top", "hbm:blocks/rbmk/rbmk_cooler_glass_top")));
        Dejson.model("hbm:block/rbmk_crane_console", Models.parented("hbm:block/machine_dummy_empty", Slots.EMPTY));
        Dejson.model("hbm:block/rbmk_fuel_rod_base", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/rbmk/rbmk_element_side", "side", "hbm:blocks/rbmk/rbmk_element_side", "top", "hbm:blocks/rbmk/rbmk_element_top").and("cover_side", "hbm:blocks/rbmk/rbmk_element_cover_side").and("cover_top", "hbm:blocks/rbmk/rbmk_element_cover_top").and("glass_side", "hbm:blocks/rbmk/rbmk_element_glass_side").and("glass_top", "hbm:blocks/rbmk/rbmk_element_glass_top").and("inner", "hbm:blocks/rbmk/rbmk_element_inner")));
        Dejson.model("hbm:block/rbmk_fuel_rod_moderated_base", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/rbmk/rbmk_element_mod_side", "side", "hbm:blocks/rbmk/rbmk_element_mod_side", "top", "hbm:blocks/rbmk/rbmk_element_mod_top").and("cover_side", "hbm:blocks/rbmk/rbmk_element_mod_cover_side").and("cover_top", "hbm:blocks/rbmk/rbmk_element_mod_cover_top").and("glass_side", "hbm:blocks/rbmk/rbmk_element_mod_glass_side").and("glass_top", "hbm:blocks/rbmk/rbmk_element_mod_glass_top").and("inner", "hbm:blocks/rbmk/rbmk_element_mod_inner")));
        Dejson.model("hbm:block/rbmk_fuel_rod_reasim_base", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/rbmk/rbmk_element_reasim_side", "side", "hbm:blocks/rbmk/rbmk_element_reasim_side", "top", "hbm:blocks/rbmk/rbmk_element_reasim_top").and("cover_side", "hbm:blocks/rbmk/rbmk_element_reasim_cover_side").and("cover_top", "hbm:blocks/rbmk/rbmk_element_reasim_cover_top").and("glass_side", "hbm:blocks/rbmk/rbmk_element_reasim_glass_side").and("glass_top", "hbm:blocks/rbmk/rbmk_element_reasim_glass_top").and("inner", "hbm:blocks/rbmk/rbmk_element_reasim_inner")));
        Dejson.model("hbm:block/rbmk_fuel_rod_reasim_moderated_base", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/rbmk/rbmk_element_reasim_mod_side", "side", "hbm:blocks/rbmk/rbmk_element_reasim_mod_side", "top", "hbm:blocks/rbmk/rbmk_element_reasim_mod_top").and("cover_side", "hbm:blocks/rbmk/rbmk_element_reasim_mod_cover_side").and("cover_top", "hbm:blocks/rbmk/rbmk_element_reasim_mod_cover_top").and("glass_side", "hbm:blocks/rbmk/rbmk_element_reasim_mod_glass_side").and("glass_top", "hbm:blocks/rbmk/rbmk_element_reasim_mod_glass_top").and("inner", "hbm:blocks/rbmk/rbmk_element_reasim_mod_inner")));
        Dejson.model("hbm:block/rbmk_gauge", Model.of()
                .textures(Slots.of("all", "hbm:blocks/rbmk/rbmk_display", "particle", "hbm:blocks/rbmk/rbmk_display"))
                .elements(Element.box(0, 0, 0, 12, 16, 16)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all"))));
        Dejson.model("hbm:block/rbmk_heater_base", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/rbmk/rbmk_heater_side", "side", "hbm:blocks/rbmk/rbmk_heater_side", "top", "hbm:blocks/rbmk/rbmk_heater_top").and("cover_side", "hbm:blocks/rbmk/rbmk_heater_cover_side").and("cover_top", "hbm:blocks/rbmk/rbmk_heater_cover_top").and("glass_side", "hbm:blocks/rbmk/rbmk_heater_glass_side").and("glass_top", "hbm:blocks/rbmk/rbmk_heater_glass_top").and("pipe_side", "hbm:blocks/rbmk/rbmk_heater_pipe_side").and("pipe_top", "hbm:blocks/rbmk/rbmk_heater_pipe_top")));
        Dejson.model("hbm:block/rbmk_moderator_base", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/rbmk/rbmk_moderator_side", "side", "hbm:blocks/rbmk/rbmk_moderator_side", "top", "hbm:blocks/rbmk/rbmk_moderator_top").and("cover_side", "hbm:blocks/rbmk/rbmk_moderator_cover_side").and("cover_top", "hbm:blocks/rbmk/rbmk_moderator_cover_top").and("glass_side", "hbm:blocks/rbmk/rbmk_moderator_glass_side").and("glass_top", "hbm:blocks/rbmk/rbmk_moderator_glass_top")));
        Dejson.model("hbm:block/rbmk_outgasser_base", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/rbmk/rbmk_outgasser_side", "side", "hbm:blocks/rbmk/rbmk_outgasser_side", "top", "hbm:blocks/rbmk/rbmk_outgasser_top").and("cover_side", "hbm:blocks/rbmk/rbmk_outgasser_cover_side").and("cover_top", "hbm:blocks/rbmk/rbmk_outgasser_cover_top").and("glass_side", "hbm:blocks/rbmk/rbmk_outgasser_glass_side").and("glass_top", "hbm:blocks/rbmk/rbmk_outgasser_glass_top")));
        Dejson.model("hbm:block/rbmk_reflector_base", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/rbmk/rbmk_reflector_side", "side", "hbm:blocks/rbmk/rbmk_reflector_side", "top", "hbm:blocks/rbmk/rbmk_reflector_top").and("cover_side", "hbm:blocks/rbmk/rbmk_reflector_cover_side").and("cover_top", "hbm:blocks/rbmk/rbmk_reflector_cover_top").and("glass_side", "hbm:blocks/rbmk/rbmk_reflector_glass_side").and("glass_top", "hbm:blocks/rbmk/rbmk_reflector_glass_top")));
        Dejson.model("hbm:block/rbmk_storage_base", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/rbmk/rbmk_storage_side", "side", "hbm:blocks/rbmk/rbmk_storage_side", "top", "hbm:blocks/rbmk/rbmk_storage_top").and("cover_side", "hbm:blocks/rbmk/rbmk_storage_cover_side").and("cover_top", "hbm:blocks/rbmk/rbmk_storage_cover_top").and("glass_side", "hbm:blocks/rbmk/rbmk_storage_glass_side").and("glass_top", "hbm:blocks/rbmk/rbmk_storage_glass_top")));
        Dejson.model("hbm:block/rbmk_terminal", Model.of()
                .textures(Slots.of("all", "hbm:blocks/rbmk/rbmk_display", "particle", "hbm:blocks/rbmk/rbmk_display"))
                .elements(Element.box(0, 0, 0, 12, 16, 16)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all"))));
        Dejson.model("hbm:block/red_cable_box_inventory_0", Model.parent("minecraft:block/block")
                .textures(Slots.of("particle", "hbm:blocks/boxduct_cable_straight", "straight", "hbm:blocks/boxduct_cable_straight", "end", "hbm:blocks/boxduct_cable_end_0"))
                .elements(Element.box(2, 2, 0, 14, 14, 16)
                        .face(Face.of("down", "#straight"))
                        .face(Face.of("up", "#straight"))
                        .face(Face.of("west", "#straight"))
                        .face(Face.of("east", "#straight"))
                        .face(Face.of("north", "#end").rotation(90))
                        .face(Face.of("south", "#end").rotation(270))));
        Dejson.model("hbm:block/red_cable_box_inventory_1", Model.parent("minecraft:block/block")
                .textures(Slots.of("particle", "hbm:blocks/boxduct_cable_straight", "straight", "hbm:blocks/boxduct_cable_straight", "end", "hbm:blocks/boxduct_cable_end_1"))
                .elements(Element.box(3, 3, 0, 13, 13, 16)
                        .face(Face.of("down", "#straight"))
                        .face(Face.of("up", "#straight"))
                        .face(Face.of("west", "#straight"))
                        .face(Face.of("east", "#straight"))
                        .face(Face.of("north", "#end").rotation(90))
                        .face(Face.of("south", "#end").rotation(270))));
        Dejson.model("hbm:block/red_cable_box_inventory_2", Model.parent("minecraft:block/block")
                .textures(Slots.of("particle", "hbm:blocks/boxduct_cable_straight", "straight", "hbm:blocks/boxduct_cable_straight", "end", "hbm:blocks/boxduct_cable_end_2"))
                .elements(Element.box(4, 4, 0, 12, 12, 16)
                        .face(Face.of("down", "#straight"))
                        .face(Face.of("up", "#straight"))
                        .face(Face.of("west", "#straight"))
                        .face(Face.of("east", "#straight"))
                        .face(Face.of("north", "#end").rotation(90))
                        .face(Face.of("south", "#end").rotation(270))));
        Dejson.model("hbm:block/red_cable_box_inventory_3", Model.parent("minecraft:block/block")
                .textures(Slots.of("particle", "hbm:blocks/boxduct_cable_straight", "straight", "hbm:blocks/boxduct_cable_straight", "end", "hbm:blocks/boxduct_cable_end_3"))
                .elements(Element.box(5, 5, 0, 11, 11, 16)
                        .face(Face.of("down", "#straight"))
                        .face(Face.of("up", "#straight"))
                        .face(Face.of("west", "#straight"))
                        .face(Face.of("east", "#straight"))
                        .face(Face.of("north", "#end").rotation(90))
                        .face(Face.of("south", "#end").rotation(270))));
        Dejson.model("hbm:block/red_cable_box_inventory_4", Model.parent("minecraft:block/block")
                .textures(Slots.of("particle", "hbm:blocks/boxduct_cable_straight", "straight", "hbm:blocks/boxduct_cable_straight", "end", "hbm:blocks/boxduct_cable_end_4"))
                .elements(Element.box(6, 6, 0, 10, 10, 16)
                        .face(Face.of("down", "#straight"))
                        .face(Face.of("up", "#straight"))
                        .face(Face.of("west", "#straight"))
                        .face(Face.of("east", "#straight"))
                        .face(Face.of("north", "#end").rotation(90))
                        .face(Face.of("south", "#end").rotation(270))));
    }

    private static void part5() {
        Dejson.model("hbm:block/red_cable_box_particle", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/boxduct_cable_straight")));
        Dejson.model("hbm:block/red_cable_classic_arm", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/red_cable_classic", "t", "hbm:blocks/red_cable_classic"))
                .elements(Element.box(5.5, 5.5, 0, 10.5, 10.5, 5.5)
                        .face(Face.of("down", "#t").uv(5, 0, 10, 5))
                        .face(Face.of("up", "#t").uv(5, 0, 10, 5))
                        .face(Face.of("west", "#t").uv(5, 0, 10, 5))
                        .face(Face.of("east", "#t").uv(5, 0, 10, 5))
                        .face(Face.of("north", "#t").uv(0, 0, 5, 5).cullface("north"))));
        Dejson.model("hbm:block/red_cable_classic_core", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/red_cable_classic", "t", "hbm:blocks/red_cable_classic"))
                .elements(Element.box(5.5, 5.5, 5.5, 10.5, 10.5, 10.5)
                        .face(Face.of("down", "#t").uv(0, 0, 5, 5))
                        .face(Face.of("up", "#t").uv(0, 0, 5, 5))
                        .face(Face.of("north", "#t").uv(0, 0, 5, 5))
                        .face(Face.of("south", "#t").uv(0, 0, 5, 5))
                        .face(Face.of("west", "#t").uv(0, 0, 5, 5))
                        .face(Face.of("east", "#t").uv(0, 0, 5, 5))));
        Dejson.model("hbm:block/red_cable_classic_item", Model.parent("minecraft:block/block")
                .textures(Slots.of("particle", "hbm:blocks/red_cable_classic", "t", "hbm:blocks/red_cable_classic"))
                .elements(Element.box(5.5, 5.5, 5.5, 10.5, 10.5, 10.5)
                        .face(Face.of("down", "#t").uv(0, 0, 5, 5))
                        .face(Face.of("up", "#t").uv(0, 0, 5, 5))
                        .face(Face.of("north", "#t").uv(0, 0, 5, 5))
                        .face(Face.of("south", "#t").uv(0, 0, 5, 5))
                        .face(Face.of("west", "#t").uv(0, 0, 5, 5))
                        .face(Face.of("east", "#t").uv(0, 0, 5, 5)),
                        Element.box(5.5, 5.5, 0, 10.5, 10.5, 5.5)
                        .face(Face.of("down", "#t").uv(5, 0, 10, 5))
                        .face(Face.of("up", "#t").uv(5, 0, 10, 5))
                        .face(Face.of("west", "#t").uv(5, 0, 10, 5))
                        .face(Face.of("east", "#t").uv(5, 0, 10, 5))
                        .face(Face.of("north", "#t").uv(0, 0, 5, 5))
                        .face(Face.of("south", "#t").uv(0, 0, 5, 5)),
                        Element.box(5.5, 5.5, 10.5, 10.5, 10.5, 16)
                        .face(Face.of("down", "#t").uv(5, 0, 10, 5))
                        .face(Face.of("up", "#t").uv(5, 0, 10, 5))
                        .face(Face.of("west", "#t").uv(5, 0, 10, 5))
                        .face(Face.of("east", "#t").uv(5, 0, 10, 5))
                        .face(Face.of("north", "#t").uv(0, 0, 5, 5))
                        .face(Face.of("south", "#t").uv(0, 0, 5, 5)),
                        Element.box(0, 5.5, 5.5, 5.5, 10.5, 10.5)
                        .face(Face.of("down", "#t").uv(5, 0, 10, 5).rotation(90))
                        .face(Face.of("up", "#t").uv(5, 0, 10, 5).rotation(90))
                        .face(Face.of("north", "#t").uv(5, 0, 10, 5))
                        .face(Face.of("south", "#t").uv(5, 0, 10, 5))
                        .face(Face.of("west", "#t").uv(0, 0, 5, 5))
                        .face(Face.of("east", "#t").uv(0, 0, 5, 5)),
                        Element.box(10.5, 5.5, 5.5, 16, 10.5, 10.5)
                        .face(Face.of("down", "#t").uv(5, 0, 10, 5).rotation(90))
                        .face(Face.of("up", "#t").uv(5, 0, 10, 5).rotation(90))
                        .face(Face.of("north", "#t").uv(5, 0, 10, 5))
                        .face(Face.of("south", "#t").uv(5, 0, 10, 5))
                        .face(Face.of("west", "#t").uv(0, 0, 5, 5))
                        .face(Face.of("east", "#t").uv(0, 0, 5, 5))));
        Dejson.model("hbm:block/red_cable_gauge", Model.parent("minecraft:block/block").renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/deco_red_copper", "base", "hbm:blocks/deco_red_copper", "gauge", "hbm:blocks/cable_gauge"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("down", "#base").cullface("down"))
                        .face(Face.of("up", "#base").cullface("up"))
                        .face(Face.of("north", "#base").cullface("north"))
                        .face(Face.of("south", "#base").cullface("south"))
                        .face(Face.of("west", "#base").cullface("west"))
                        .face(Face.of("east", "#base").cullface("east")),
                        Element.box(0, 0, -0.02, 16, 16, -0.02)
                        .face(Face.of("north", "#gauge").cullface("north"))));
        Dejson.model("hbm:block/red_cable_paintable", Model.parent("minecraft:block/block").renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/red_cable_base", "base", "hbm:blocks/red_cable_base", "overlay", "hbm:blocks/red_cable_overlay"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("down", "#base").cullface("down"))
                        .face(Face.of("up", "#base").cullface("up"))
                        .face(Face.of("north", "#base").cullface("north"))
                        .face(Face.of("south", "#base").cullface("south"))
                        .face(Face.of("west", "#base").cullface("west"))
                        .face(Face.of("east", "#base").cullface("east")),
                        Element.box(-0.02, -0.02, -0.02, 16.02, 16.02, 16.02)
                        .face(Face.of("down", "#overlay").uv(0, 0, 16, 16))
                        .face(Face.of("up", "#overlay").uv(0, 0, 16, 16))
                        .face(Face.of("north", "#overlay").uv(0, 0, 16, 16))
                        .face(Face.of("south", "#overlay").uv(0, 0, 16, 16))
                        .face(Face.of("west", "#overlay").uv(0, 0, 16, 16))
                        .face(Face.of("east", "#overlay").uv(0, 0, 16, 16))));
        Dejson.model("hbm:block/red_cable_paintable_plain", Model.parent("minecraft:block/block")
                .textures(Slots.of("particle", "hbm:blocks/red_cable_base", "base", "hbm:blocks/red_cable_base"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("down", "#base").cullface("down"))
                        .face(Face.of("up", "#base").cullface("up"))
                        .face(Face.of("north", "#base").cullface("north"))
                        .face(Face.of("south", "#base").cullface("south"))
                        .face(Face.of("west", "#base").cullface("west"))
                        .face(Face.of("east", "#base").cullface("east"))));
        Dejson.model("hbm:block/reinforced_glass_pane_noside", Model.parent("minecraft:block/template_glass_pane_noside").renderType("minecraft:cutout_mipped")
                .textures(Slots.of("pane", "hbm:blocks/reinforced_glass_pane")));
        Dejson.model("hbm:block/reinforced_glass_pane_noside_alt", Model.parent("minecraft:block/template_glass_pane_noside_alt").renderType("minecraft:cutout_mipped")
                .textures(Slots.of("pane", "hbm:blocks/reinforced_glass_pane")));
        Dejson.model("hbm:block/reinforced_glass_pane_post", Model.parent("minecraft:block/template_glass_pane_post").renderType("minecraft:cutout_mipped")
                .textures(Slots.of("pane", "hbm:blocks/reinforced_glass_pane", "edge", "hbm:blocks/reinforced_glass_pane_edge")));
        Dejson.model("hbm:block/reinforced_glass_pane_side", Model.parent("minecraft:block/template_glass_pane_side").renderType("minecraft:cutout_mipped")
                .textures(Slots.of("pane", "hbm:blocks/reinforced_glass_pane", "edge", "hbm:blocks/reinforced_glass_pane_edge")));
        Dejson.model("hbm:block/reinforced_glass_pane_side_alt", Model.parent("minecraft:block/template_glass_pane_side_alt").renderType("minecraft:cutout_mipped")
                .textures(Slots.of("pane", "hbm:blocks/reinforced_glass_pane", "edge", "hbm:blocks/reinforced_glass_pane_edge")));
        Dejson.model("hbm:block/reinforced_laminate_pane_noside", Model.parent("minecraft:block/template_glass_pane_noside").renderType("minecraft:translucent")
                .textures(Slots.of("pane", "hbm:blocks/reinforced_laminate_pane")));
        Dejson.model("hbm:block/reinforced_laminate_pane_noside_alt", Model.parent("minecraft:block/template_glass_pane_noside_alt").renderType("minecraft:translucent")
                .textures(Slots.of("pane", "hbm:blocks/reinforced_laminate_pane")));
        Dejson.model("hbm:block/reinforced_laminate_pane_post", Model.parent("minecraft:block/template_glass_pane_post").renderType("minecraft:translucent")
                .textures(Slots.of("pane", "hbm:blocks/reinforced_laminate_pane", "edge", "hbm:blocks/reinforced_laminate_pane_edge")));
        Dejson.model("hbm:block/reinforced_laminate_pane_side", Model.parent("minecraft:block/template_glass_pane_side").renderType("minecraft:translucent")
                .textures(Slots.of("pane", "hbm:blocks/reinforced_laminate_pane", "edge", "hbm:blocks/reinforced_laminate_pane_edge")));
        Dejson.model("hbm:block/reinforced_laminate_pane_side_alt", Model.parent("minecraft:block/template_glass_pane_side_alt").renderType("minecraft:translucent")
                .textures(Slots.of("pane", "hbm:blocks/reinforced_laminate_pane", "edge", "hbm:blocks/reinforced_laminate_pane_edge")));
        Dejson.model("hbm:block/sand_boron_layer_height10", Models.parented("minecraft:block/snow_height10", Slots.of("particle", "hbm:blocks/sand_boron", "texture", "hbm:blocks/sand_boron")));
        Dejson.model("hbm:block/sand_boron_layer_height12", Models.parented("minecraft:block/snow_height12", Slots.of("particle", "hbm:blocks/sand_boron", "texture", "hbm:blocks/sand_boron")));
        Dejson.model("hbm:block/sand_boron_layer_height14", Models.parented("minecraft:block/snow_height14", Slots.of("particle", "hbm:blocks/sand_boron", "texture", "hbm:blocks/sand_boron")));
        Dejson.model("hbm:block/sand_boron_layer_height2", Models.parented("minecraft:block/snow_height2", Slots.of("particle", "hbm:blocks/sand_boron", "texture", "hbm:blocks/sand_boron")));
        Dejson.model("hbm:block/sand_boron_layer_height4", Models.parented("minecraft:block/snow_height4", Slots.of("particle", "hbm:blocks/sand_boron", "texture", "hbm:blocks/sand_boron")));
        Dejson.model("hbm:block/sand_boron_layer_height6", Models.parented("minecraft:block/snow_height6", Slots.of("particle", "hbm:blocks/sand_boron", "texture", "hbm:blocks/sand_boron")));
        Dejson.model("hbm:block/sand_boron_layer_height8", Models.parented("minecraft:block/snow_height8", Slots.of("particle", "hbm:blocks/sand_boron", "texture", "hbm:blocks/sand_boron")));
        Dejson.model("hbm:block/steel_beam", Model.parent("minecraft:block/block")
                .textures(Slots.of("all", "hbm:blocks/block_steel", "particle", "hbm:blocks/block_steel"))
                .elements(Element.box(7, 0, 7, 9, 16, 9)
                        .face(Face.of("north", "#all").uv(0, 0, 2, 16))
                        .face(Face.of("south", "#all").uv(0, 0, 2, 16))
                        .face(Face.of("west", "#all").uv(0, 0, 2, 16))
                        .face(Face.of("east", "#all").uv(0, 0, 2, 16))
                        .face(Face.of("up", "#all").uv(7, 7, 9, 9))
                        .face(Face.of("down", "#all").uv(7, 7, 9, 9))));
        Dejson.model("hbm:block/steel_grate0", Model.parent("minecraft:block/block").ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/grate_top", "end", "hbm:blocks/grate_top", "side", "hbm:blocks/grate_side"))
                .elements(Element.box(0, 0, 0, 16, 2, 16)
                        .face(Face.of("down", "#end").cullface("down"))
                        .face(Face.of("up", "#end"))
                        .face(Face.of("north", "#side").cullface("north"))
                        .face(Face.of("south", "#side").cullface("south"))
                        .face(Face.of("west", "#side").cullface("west"))
                        .face(Face.of("east", "#side").cullface("east"))));
        Dejson.model("hbm:block/steel_grate1", Model.parent("minecraft:block/block").ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/grate_top", "end", "hbm:blocks/grate_top", "side", "hbm:blocks/grate_side"))
                .elements(Element.box(0, 2, 0, 16, 4, 16)
                        .face(Face.of("down", "#end"))
                        .face(Face.of("up", "#end"))
                        .face(Face.of("north", "#side").cullface("north"))
                        .face(Face.of("south", "#side").cullface("south"))
                        .face(Face.of("west", "#side").cullface("west"))
                        .face(Face.of("east", "#side").cullface("east"))));
        Dejson.model("hbm:block/steel_grate2", Model.parent("minecraft:block/block").ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/grate_top", "end", "hbm:blocks/grate_top", "side", "hbm:blocks/grate_side"))
                .elements(Element.box(0, 4, 0, 16, 6, 16)
                        .face(Face.of("down", "#end"))
                        .face(Face.of("up", "#end"))
                        .face(Face.of("north", "#side").cullface("north"))
                        .face(Face.of("south", "#side").cullface("south"))
                        .face(Face.of("west", "#side").cullface("west"))
                        .face(Face.of("east", "#side").cullface("east"))));
        Dejson.model("hbm:block/steel_grate3", Model.parent("minecraft:block/block").ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/grate_top", "end", "hbm:blocks/grate_top", "side", "hbm:blocks/grate_side"))
                .elements(Element.box(0, 6, 0, 16, 8, 16)
                        .face(Face.of("down", "#end"))
                        .face(Face.of("up", "#end"))
                        .face(Face.of("north", "#side").cullface("north"))
                        .face(Face.of("south", "#side").cullface("south"))
                        .face(Face.of("west", "#side").cullface("west"))
                        .face(Face.of("east", "#side").cullface("east"))));
        Dejson.model("hbm:block/steel_grate4", Model.parent("minecraft:block/block").ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/grate_top", "end", "hbm:blocks/grate_top", "side", "hbm:blocks/grate_side"))
                .elements(Element.box(0, 8, 0, 16, 10, 16)
                        .face(Face.of("down", "#end"))
                        .face(Face.of("up", "#end"))
                        .face(Face.of("north", "#side").cullface("north"))
                        .face(Face.of("south", "#side").cullface("south"))
                        .face(Face.of("west", "#side").cullface("west"))
                        .face(Face.of("east", "#side").cullface("east"))));
    }

    private static void part6() {
        Dejson.model("hbm:block/steel_grate5", Model.parent("minecraft:block/block").ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/grate_top", "end", "hbm:blocks/grate_top", "side", "hbm:blocks/grate_side"))
                .elements(Element.box(0, 10, 0, 16, 12, 16)
                        .face(Face.of("down", "#end"))
                        .face(Face.of("up", "#end"))
                        .face(Face.of("north", "#side").cullface("north"))
                        .face(Face.of("south", "#side").cullface("south"))
                        .face(Face.of("west", "#side").cullface("west"))
                        .face(Face.of("east", "#side").cullface("east"))));
        Dejson.model("hbm:block/steel_grate6", Model.parent("minecraft:block/block").ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/grate_top", "end", "hbm:blocks/grate_top", "side", "hbm:blocks/grate_side"))
                .elements(Element.box(0, 12, 0, 16, 14, 16)
                        .face(Face.of("down", "#end"))
                        .face(Face.of("up", "#end"))
                        .face(Face.of("north", "#side").cullface("north"))
                        .face(Face.of("south", "#side").cullface("south"))
                        .face(Face.of("west", "#side").cullface("west"))
                        .face(Face.of("east", "#side").cullface("east"))));
        Dejson.model("hbm:block/steel_grate7", Model.parent("minecraft:block/block").ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/grate_top", "end", "hbm:blocks/grate_top", "side", "hbm:blocks/grate_side"))
                .elements(Element.box(0, 14, 0, 16, 16, 16)
                        .face(Face.of("down", "#end"))
                        .face(Face.of("up", "#end").cullface("up"))
                        .face(Face.of("north", "#side").cullface("north"))
                        .face(Face.of("south", "#side").cullface("south"))
                        .face(Face.of("west", "#side").cullface("west"))
                        .face(Face.of("east", "#side").cullface("east"))));
        Dejson.model("hbm:block/steel_grate8", Model.parent("minecraft:block/block").ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/grate_top", "end", "hbm:blocks/grate_top", "side", "hbm:blocks/grate_side"))
                .elements(Element.box(0, 16, 0, 16, 18, 16)
                        .face(Face.of("down", "#end").cullface("up"))
                        .face(Face.of("up", "#end"))
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))));
        Dejson.model("hbm:block/steel_grate9", Model.parent("minecraft:block/block").ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/grate_top", "end", "hbm:blocks/grate_top", "side", "hbm:blocks/grate_side"))
                .elements(Element.box(0, -2, 0, 16, 0, 16)
                        .face(Face.of("down", "#end"))
                        .face(Face.of("up", "#end").cullface("down"))
                        .face(Face.of("north", "#side"))
                        .face(Face.of("south", "#side"))
                        .face(Face.of("west", "#side"))
                        .face(Face.of("east", "#side"))));
        Dejson.model("hbm:block/steel_scaffold", Model.of().ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/scaffold_steel")));
        Dejson.model("hbm:block/template_chain", Model.parent("minecraft:block/cross").renderType("minecraft:cutout")
                .textures(Slots.of("cross", "#chain", "particle", "#chain")));
        Dejson.model("hbm:block/template_chain_wall", Model.of().ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "#chain"))
                .elements(Element.box(0, 0, 15, 16, 16, 15).shade(false)
                        .face(Face.of("north", "#chain").uv(0, 0, 16, 16))));
        // Octagonal pipe bodies (1.7 RenderPipe + pipe/pipe_rim/pipe_quad/pipe_frame OBJs). Each octagon is four
        // boxes rotated +/-22.5 degrees; only each box's two SHORT ends lie on the octagon perimeter (the long
        // sides are interior chords), so those ends carry the side texture. Drawing the long sides instead
        // produced an eight-pointed star of chords ("spikey" pipes, 2026-09-29 playtest). The block/block parent
        // supplies the 3D inventory display (1.7 shouldRender3DInInventory); without it the icon was a flat front view.
        Dejson.model("hbm:block/template_pipe", Model.parent("minecraft:block/block").ambientOcclusion(false)
                .textures(Slots.of("particle", "#top", "top", "#top", "side", "#side"))
                .elements(pipeOctagon(8, 8, 6, 0, 16, 3, 8, 13, 0, 16, 1, true)));
        Dejson.model("hbm:block/template_pipe_rim", Model.parent("minecraft:block/block").ambientOcclusion(false)
                .textures(Slots.of("particle", "#top", "top", "#top", "side", "#side"))
                .elements(pipeRim()));
        Dejson.model("hbm:block/template_pipe_framed", Model.parent("minecraft:block/block").ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "#top", "top", "#top", "side", "#side").and("frame", "hbm:blocks/pipe_frame").and("mesh", "hbm:blocks/pipe_mesh"))
                .elements(concat(pipeRim(), new Element[]{
                        Element.box(0, 0, 0, 2, 16, 2)
                        .face(Face.of("north", "#frame").uv(0, 0, 2, 16))
                        .face(Face.of("south", "#frame").uv(0, 0, 2, 16))
                        .face(Face.of("west", "#frame").uv(0, 0, 2, 16))
                        .face(Face.of("east", "#frame").uv(0, 0, 2, 16))
                        .face(Face.of("up", "#frame").uv(0, 0, 2, 2))
                        .face(Face.of("down", "#frame").uv(0, 0, 2, 2)),
                        Element.box(0, 0, 14, 2, 16, 16)
                        .face(Face.of("north", "#frame").uv(0, 0, 2, 16))
                        .face(Face.of("south", "#frame").uv(0, 0, 2, 16))
                        .face(Face.of("west", "#frame").uv(14, 0, 16, 16))
                        .face(Face.of("east", "#frame").uv(14, 0, 16, 16))
                        .face(Face.of("up", "#frame").uv(0, 14, 2, 16))
                        .face(Face.of("down", "#frame").uv(0, 14, 2, 16)),
                        Element.box(14, 0, 0, 16, 16, 2)
                        .face(Face.of("north", "#frame").uv(14, 0, 16, 16))
                        .face(Face.of("south", "#frame").uv(14, 0, 16, 16))
                        .face(Face.of("west", "#frame").uv(0, 0, 2, 16))
                        .face(Face.of("east", "#frame").uv(0, 0, 2, 16))
                        .face(Face.of("up", "#frame").uv(14, 0, 16, 2))
                        .face(Face.of("down", "#frame").uv(14, 0, 16, 2)),
                        Element.box(14, 0, 14, 16, 16, 16)
                        .face(Face.of("north", "#frame").uv(14, 0, 16, 16))
                        .face(Face.of("south", "#frame").uv(14, 0, 16, 16))
                        .face(Face.of("west", "#frame").uv(14, 0, 16, 16))
                        .face(Face.of("east", "#frame").uv(14, 0, 16, 16))
                        .face(Face.of("up", "#frame").uv(14, 14, 16, 16))
                        .face(Face.of("down", "#frame").uv(14, 14, 16, 16)),
                        Element.box(2, 0, 1, 14, 16, 1)
                        .face(Face.of("north", "#mesh").uv(2, 0, 14, 16))
                        .face(Face.of("south", "#mesh").uv(2, 0, 14, 16)),
                        Element.box(2, 0, 15, 14, 16, 15)
                        .face(Face.of("north", "#mesh").uv(2, 0, 14, 16))
                        .face(Face.of("south", "#mesh").uv(2, 0, 14, 16)),
                        Element.box(1, 0, 2, 1, 16, 14)
                        .face(Face.of("west", "#mesh").uv(2, 0, 14, 16))
                        .face(Face.of("east", "#mesh").uv(2, 0, 14, 16)),
                        Element.box(15, 0, 2, 15, 16, 14)
                        .face(Face.of("west", "#mesh").uv(2, 0, 14, 16))
                        .face(Face.of("east", "#mesh").uv(2, 0, 14, 16))})));
        // pipe_quad: four radius-2 pipes at the quarter points; their sides use u 6..8/8..10 and every cap maps
        // the texture's central radius-3 disc (1.5 texels per model pixel), exactly as the OBJ's UVs do.
        Dejson.model("hbm:block/template_pipe_quad", Model.parent("minecraft:block/block").ambientOcclusion(false)
                .textures(Slots.of("particle", "#top", "top", "#top", "side", "#side"))
                .elements(concat(pipeOctagon(4, 4, 2, 0, 16, 6, 8, 10, 0, 16, 1.5, true),
                        pipeOctagon(4, 12, 2, 0, 16, 6, 8, 10, 0, 16, 1.5, true),
                        pipeOctagon(12, 4, 2, 0, 16, 6, 8, 10, 0, 16, 1.5, true),
                        pipeOctagon(12, 12, 2, 0, 16, 6, 8, 10, 0, 16, 1.5, true))));
        Dejson.model("hbm:block/tinted_cube_all", Model.parent("minecraft:block/block")
                .textures(Slots.of("particle", "#all"))
                .elements(Element.box(0, 0, 0, 16, 16, 16)
                        .face(Face.of("down", "#all").cullface("down").tintIndex(0))
                        .face(Face.of("up", "#all").cullface("up").tintIndex(0))
                        .face(Face.of("north", "#all").cullface("north").tintIndex(0))
                        .face(Face.of("south", "#all").cullface("south").tintIndex(0))
                        .face(Face.of("west", "#all").cullface("west").tintIndex(0))
                        .face(Face.of("east", "#all").cullface("east").tintIndex(0))));
        Dejson.model("hbm:block/vine_phosphor_ground", Model.of().ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/vine_phosphor_ground", "base", "hbm:blocks/vine_phosphor_ground", "spots", "hbm:blocks/vine_phosphor_spots"))
                .elements(Element.box(0, 0, 0, 16, 16, 16).shade(false)
                        .face(Face.of("north", "#base").uv(0, 0, 16, 16))
                        .face(Face.of("south", "#base").uv(0, 0, 16, 16))
                        .face(Face.of("east", "#base").uv(0, 0, 16, 16))
                        .face(Face.of("west", "#base").uv(0, 0, 16, 16)),
                        Element.box(-0.05, 0, -0.05, 16.05, 16, 16.05).shade(false)
                        .face(Face.of("north", "#spots").uv(0, 0, 16, 16))
                        .face(Face.of("south", "#spots").uv(0, 0, 16, 16))
                        .face(Face.of("east", "#spots").uv(0, 0, 16, 16))
                        .face(Face.of("west", "#spots").uv(0, 0, 16, 16))));
        Dejson.model("hbm:block/vine_phosphor_hang", Model.of().ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/vine_phosphor_hang", "base", "hbm:blocks/vine_phosphor_hang", "spots", "hbm:blocks/vine_phosphor_spots_hang"))
                .elements(Element.box(0, 0, 0, 16, 16, 16).shade(false)
                        .face(Face.of("north", "#base").uv(0, 0, 16, 16))
                        .face(Face.of("south", "#base").uv(0, 0, 16, 16))
                        .face(Face.of("east", "#base").uv(0, 0, 16, 16))
                        .face(Face.of("west", "#base").uv(0, 0, 16, 16)),
                        Element.box(-0.05, 0, -0.05, 16.05, 16, 16.05).shade(false)
                        .face(Face.of("north", "#spots").uv(0, 0, 16, 16))
                        .face(Face.of("south", "#spots").uv(0, 0, 16, 16))
                        .face(Face.of("east", "#spots").uv(0, 0, 16, 16))
                        .face(Face.of("west", "#spots").uv(0, 0, 16, 16))));
        Dejson.model("hbm:block/vine_phosphor_middle", Model.of().ambientOcclusion(false).renderType("minecraft:cutout")
                .textures(Slots.of("particle", "hbm:blocks/vine_phosphor", "base", "hbm:blocks/vine_phosphor", "spots", "hbm:blocks/vine_phosphor_spots"))
                .elements(Element.box(0, 0, 0, 16, 16, 16).shade(false)
                        .face(Face.of("north", "#base").uv(0, 0, 16, 16))
                        .face(Face.of("south", "#base").uv(0, 0, 16, 16))
                        .face(Face.of("east", "#base").uv(0, 0, 16, 16))
                        .face(Face.of("west", "#base").uv(0, 0, 16, 16)),
                        Element.box(-0.05, 0, -0.05, 16.05, 16, 16.05).shade(false)
                        .face(Face.of("north", "#spots").uv(0, 0, 16, 16))
                        .face(Face.of("south", "#spots").uv(0, 0, 16, 16))
                        .face(Face.of("east", "#spots").uv(0, 0, 16, 16))
                        .face(Face.of("west", "#spots").uv(0, 0, 16, 16))));
        Dejson.model("hbm:block/wood_barrier_east_cap_north", Model.of().ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/wood_barrier", "all", "hbm:blocks/wood_barrier"))
                .elements(Element.box(15, 1, 0, 16, 7, 2)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all")),
                        Element.box(15, 9, 0, 16, 15, 2)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all"))));
        Dejson.model("hbm:block/wood_barrier_east_cap_south", Model.of().ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/wood_barrier", "all", "hbm:blocks/wood_barrier"))
                .elements(Element.box(15, 1, 14, 16, 7, 16)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all")),
                        Element.box(15, 9, 14, 16, 15, 16)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all"))));
        Dejson.model("hbm:block/wood_barrier_east_post", Model.of().ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/wood_barrier", "all", "hbm:blocks/wood_barrier"))
                .elements(Element.box(14, 0, 7, 16, 16, 9)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all"))));
        Dejson.model("hbm:block/wood_barrier_east_rail", Model.of().ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/wood_barrier", "all", "hbm:blocks/wood_barrier"))
                .elements(Element.box(15, 1, 2, 16, 7, 14)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all")),
                        Element.box(15, 9, 2, 16, 15, 14)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all"))));
        Dejson.model("hbm:block/wood_barrier_inventory", Model.parent("minecraft:block/block").ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/wood_barrier", "all", "hbm:blocks/wood_barrier"))
                .elements(Element.box(7, 0, 7, 9, 16, 9)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all")),
                        Element.box(0, 1, 7, 16, 7.56, 8)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all")),
                        Element.box(0, 9, 7, 16, 15, 8)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all"))));
        Dejson.model("hbm:block/wood_barrier_north_cap_east", Model.of().ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/wood_barrier", "all", "hbm:blocks/wood_barrier"))
                .elements(Element.box(14, 1, 0, 16, 7, 1)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all")),
                        Element.box(14, 9, 0, 16, 15, 1)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all"))));
        Dejson.model("hbm:block/wood_barrier_north_cap_west", Model.of().ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/wood_barrier", "all", "hbm:blocks/wood_barrier"))
                .elements(Element.box(0, 1, 0, 2, 7, 1)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all")),
                        Element.box(0, 9, 0, 2, 15, 1)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all"))));
        Dejson.model("hbm:block/wood_barrier_north_post", Model.of().ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/wood_barrier", "all", "hbm:blocks/wood_barrier"))
                .elements(Element.box(7, 0, 0, 9, 16, 2)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all"))));
        Dejson.model("hbm:block/wood_barrier_north_rail", Model.of().ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/wood_barrier", "all", "hbm:blocks/wood_barrier"))
                .elements(Element.box(2, 1, 0, 14, 7, 1)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all")),
                        Element.box(2, 9, 0, 14, 15, 1)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all"))));
        Dejson.model("hbm:block/wood_barrier_south_cap_east", Model.of().ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/wood_barrier", "all", "hbm:blocks/wood_barrier"))
                .elements(Element.box(14, 1, 15, 16, 7, 16)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all")),
                        Element.box(14, 9, 15, 16, 15, 16)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all"))));
        Dejson.model("hbm:block/wood_barrier_south_cap_west", Model.of().ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/wood_barrier", "all", "hbm:blocks/wood_barrier"))
                .elements(Element.box(0, 1, 15, 2, 7, 16)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all")),
                        Element.box(0, 9, 15, 2, 15, 16)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all"))));
        Dejson.model("hbm:block/wood_barrier_south_post", Model.of().ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/wood_barrier", "all", "hbm:blocks/wood_barrier"))
                .elements(Element.box(7, 0, 14, 9, 16, 16)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all"))));
        Dejson.model("hbm:block/wood_barrier_south_rail", Model.of().ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/wood_barrier", "all", "hbm:blocks/wood_barrier"))
                .elements(Element.box(2, 1, 15, 14, 7, 16)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all")),
                        Element.box(2, 9, 15, 14, 15, 16)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all"))));
        Dejson.model("hbm:block/wood_barrier_top", Model.of().ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/wood_barrier", "all", "hbm:blocks/wood_barrier"))
                .elements(Element.box(0, 14, 0, 2, 15, 16)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all")),
                        Element.box(14, 14, 0, 16, 15, 16)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all")),
                        Element.box(0, 15, 1, 16, 16, 7)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all")),
                        Element.box(0, 15, 9, 16, 16, 15)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all"))));
    }

    private static void part7() {
        Dejson.model("hbm:block/wood_barrier_west_cap_north", Model.of().ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/wood_barrier", "all", "hbm:blocks/wood_barrier"))
                .elements(Element.box(0, 1, 0, 1, 7, 2)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all")),
                        Element.box(0, 9, 0, 1, 15, 2)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all"))));
        Dejson.model("hbm:block/wood_barrier_west_cap_south", Model.of().ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/wood_barrier", "all", "hbm:blocks/wood_barrier"))
                .elements(Element.box(0, 1, 14, 1, 7, 16)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all")),
                        Element.box(0, 9, 14, 1, 15, 16)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all"))));
        Dejson.model("hbm:block/wood_barrier_west_post", Model.of().ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/wood_barrier", "all", "hbm:blocks/wood_barrier"))
                .elements(Element.box(0, 0, 7, 2, 16, 9)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all"))));
        Dejson.model("hbm:block/wood_barrier_west_rail", Model.of().ambientOcclusion(false)
                .textures(Slots.of("particle", "hbm:blocks/wood_barrier", "all", "hbm:blocks/wood_barrier"))
                .elements(Element.box(0, 1, 2, 1, 7, 14)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all")),
                        Element.box(0, 9, 2, 1, 15, 14)
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("east", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("down", "#all"))));
        Dejson.model("hbm:block/wood_structure_ceiling", Model.parent("minecraft:block/block")
                .textures(Slots.of("all", "hbm:blocks/wood_barrier", "particle", "hbm:blocks/wood_barrier"))
                .elements(Element.box(0, 14, 0, 2, 15, 16)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(14, 14, 0, 16, 15, 16)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(0, 15, 1, 16, 16, 7)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(0, 15, 9, 16, 16, 15)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all"))));
        Dejson.model("hbm:block/wood_structure_roof", Model.parent("minecraft:block/block")
                .textures(Slots.of("all", "hbm:blocks/wood_barrier", "particle", "hbm:blocks/wood_barrier"))
                .elements(Element.box(0, 0, 0, 2, 2, 16)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(14, 0, 0, 16, 2, 16)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(1, 2, 1, 15, 3, 7)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(1, 2, 9, 15, 3, 15)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all"))));
        Dejson.model("hbm:block/wood_structure_roof_base", Model.parent("minecraft:block/block")
                .textures(Slots.of("all", "hbm:blocks/wood_barrier", "particle", "hbm:blocks/wood_barrier"))
                .elements(Element.box(0, 0, 0, 2, 2, 16)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(14, 0, 0, 16, 2, 16)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(1, 2, 1, 15, 3, 7)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(1, 2, 9, 15, 3, 15)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all"))));
        Dejson.model("hbm:block/wood_structure_roof_east", Model.parent("minecraft:block/block")
                .textures(Slots.of("all", "hbm:blocks/wood_barrier", "particle", "hbm:blocks/wood_barrier"))
                .elements(Element.box(15, 2, 1, 16, 3, 7)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(15, 2, 9, 16, 3, 15)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all"))));
        Dejson.model("hbm:block/wood_structure_roof_west", Model.parent("minecraft:block/block")
                .textures(Slots.of("all", "hbm:blocks/wood_barrier", "particle", "hbm:blocks/wood_barrier"))
                .elements(Element.box(0, 2, 1, 1, 3, 7)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(0, 2, 9, 1, 3, 15)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all"))));
        Dejson.model("hbm:block/wood_structure_scaffold", Model.parent("minecraft:block/block")
                .textures(Slots.of("all", "hbm:blocks/wood_barrier", "particle", "hbm:blocks/wood_barrier"))
                .elements(Element.box(1, 0, 1, 3, 14, 3)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(13, 0, 1, 15, 14, 3)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(1, 0, 13, 3, 14, 15)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(13, 0, 13, 15, 14, 15)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(0, 2, 0, 1, 6, 16)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(15, 2, 0, 16, 6, 16)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(0, 8, 0, 16, 12, 1)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(0, 8, 15, 16, 12, 16)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(0, 14, 0, 16, 16, 16)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all"))));
        Dejson.model("hbm:block/wood_structure_scaffold_east", Model.parent("minecraft:block/block")
                .textures(Slots.of("all", "hbm:blocks/wood_barrier", "particle", "hbm:blocks/wood_barrier"))
                .elements(Element.box(15, 2, 0, 16, 6, 16)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all"))));
        Dejson.model("hbm:block/wood_structure_scaffold_north", Model.parent("minecraft:block/block")
                .textures(Slots.of("all", "hbm:blocks/wood_barrier", "particle", "hbm:blocks/wood_barrier"))
                .elements(Element.box(0, 8, 0, 16, 12, 1)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all"))));
        Dejson.model("hbm:block/wood_structure_scaffold_post_tops", Model.parent("minecraft:block/block")
                .textures(Slots.of("all", "hbm:blocks/wood_barrier", "particle", "hbm:blocks/wood_barrier"))
                .elements(Element.box(1, 14, 1, 3, 16, 3)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(13, 14, 1, 15, 16, 3)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(1, 14, 13, 3, 16, 15)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(13, 14, 13, 15, 16, 15)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all"))));
        Dejson.model("hbm:block/wood_structure_scaffold_posts", Model.parent("minecraft:block/block")
                .textures(Slots.of("all", "hbm:blocks/wood_barrier", "particle", "hbm:blocks/wood_barrier"))
                .elements(Element.box(1, 0, 1, 3, 14, 3)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(13, 0, 1, 15, 14, 3)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(1, 0, 13, 3, 14, 15)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all")),
                        Element.box(13, 0, 13, 15, 14, 15)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all"))));
        Dejson.model("hbm:block/wood_structure_scaffold_south", Model.parent("minecraft:block/block")
                .textures(Slots.of("all", "hbm:blocks/wood_barrier", "particle", "hbm:blocks/wood_barrier"))
                .elements(Element.box(0, 8, 15, 16, 12, 16)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all"))));
        Dejson.model("hbm:block/wood_structure_scaffold_top", Model.parent("minecraft:block/block")
                .textures(Slots.of("all", "hbm:blocks/wood_barrier", "particle", "hbm:blocks/wood_barrier"))
                .elements(Element.box(0, 14, 0, 16, 16, 16)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all"))));
        Dejson.model("hbm:block/wood_structure_scaffold_west", Model.parent("minecraft:block/block")
                .textures(Slots.of("all", "hbm:blocks/wood_barrier", "particle", "hbm:blocks/wood_barrier"))
                .elements(Element.box(0, 2, 0, 1, 6, 16)
                        .face(Face.of("down", "#all"))
                        .face(Face.of("up", "#all"))
                        .face(Face.of("north", "#all"))
                        .face(Face.of("south", "#all"))
                        .face(Face.of("west", "#all"))
                        .face(Face.of("east", "#all"))));
        Dejson.model("hbm:block/zirnox_destroyed", Model.of()
                .textures(Slots.of("particle", "hbm:blocks/block_steel")));
    }

    /** pipe_rim: a radius-6 body between two radius-7, one-pixel rims (old side v 1..15, rims v 0..1 / 15..16). */
    private static Element[] pipeRim() {
        return concat(pipeOctagon(8, 8, 6, 1, 15, 3, 8, 13, 1, 15, 1, false),
                pipeOctagon(8, 8, 7, 0, 1, 3, 8, 13, 15, 16, 1, true),
                pipeOctagon(8, 8, 7, 15, 16, 3, 8, 13, 0, 1, 1, true));
    }

    /**
     * A regular octagon prism about the Y axis (vertices on the X/Z axes, like the old OBJs), built from four
     * boxes rotated +/-22.5 degrees. A box of half-extents r*cos(22.5) x r*sin(22.5) has its four corners on the
     * circumcircle, so after rotation its two short ends are octagon edges and its long sides are hidden chords.
     * Adjacent octagon edges alternate between the u ranges [u0,u1] and [u1,u2] as in the OBJ side UVs; caps map
     * a disc of radius r*capScale around the texture centre.
     */
    private static Element[] pipeOctagon(double cx, double cz, double r, double y0, double y1,
                                         double u0, double u1, double u2, double v0, double v1,
                                         double capScale, boolean caps) {
        double a = round3(r * Math.cos(Math.PI / 8)), b = round3(r * Math.sin(Math.PI / 8));
        double ca = round3(a * capScale), cb = round3(b * capScale);
        Element[] out = new Element[4];
        int i = 0;
        for (double angle : new double[]{22.5, -22.5}) {
            double uStart = angle > 0 ? u0 : u1, uEnd = angle > 0 ? u1 : u2;
            Element xLong = Element.box(round3(cx - a), y0, round3(cz - b), round3(cx + a), y1, round3(cz + b))
                    .rotation(cx, 8, cz, "y", angle)
                    .face(Face.of("east", "#side").uv(uStart, v0, uEnd, v1))
                    .face(Face.of("west", "#side").uv(uStart, v0, uEnd, v1));
            Element zLong = Element.box(round3(cx - b), y0, round3(cz - a), round3(cx + b), y1, round3(cz + a))
                    .rotation(cx, 8, cz, "y", angle)
                    .face(Face.of("north", "#side").uv(uStart, v0, uEnd, v1))
                    .face(Face.of("south", "#side").uv(uStart, v0, uEnd, v1));
            if (caps) {
                xLong.face(Face.of("up", "#top").uv(round3(8 - ca), round3(8 - cb), round3(8 + ca), round3(8 + cb)))
                        .face(Face.of("down", "#top").uv(round3(8 - ca), round3(8 - cb), round3(8 + ca), round3(8 + cb)));
                zLong.face(Face.of("up", "#top").uv(round3(8 - cb), round3(8 - ca), round3(8 + cb), round3(8 + ca)))
                        .face(Face.of("down", "#top").uv(round3(8 - cb), round3(8 - ca), round3(8 + cb), round3(8 + ca)));
            }
            out[i++] = xLong;
            out[i++] = zLong;
        }
        return out;
    }

    private static double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private static Element[] concat(Element[]... parts) {
        return java.util.Arrays.stream(parts).flatMap(java.util.Arrays::stream).toArray(Element[]::new);
    }
}
