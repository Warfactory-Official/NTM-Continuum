package com.hbm.items;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.ItemModels;
import dev.golem.core.dejson.Model;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;

/**
 * Item definitions drawn by hbm's own item renderers: OBJ machines ({@code hbm:machine_obj}, directly
 * or inside a {@code minecraft:special}), RBMK columns and the jetpack glider. Each chain is the
 * 1.7 {@code IItemRenderer}'s numbers as {@link MachineObjModel} spells them.
 *
 * <p>Touches no Minecraft class: Dejson runs {@link #declare()} in a plain JVM at build time.
 */
public final class ObjItemDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private ObjItemDeclarations() {}

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
        part8();
        part9();
        part10();
        part11();
        part12();
    }

    private static void part0() {
        Dejson.item(NS, "ammo_himars")
                .models(Models.parented("hbm:item/machine_obj_context_legacy", Slots.EMPTY))
                .definition(ItemModels.select("hbm:item_variant")
                        .when("1", MachineObjModel.of("hbm:models/turrets/turret_himars.obj", "hbm:textures/models/projectiles/himars_standard_he.png")
                                .scale(1.0).ty(0.0).yaw(-45.0).pitch(90.0).guiScale(2.75).guiSpinSpeed(0.1)
                                .rawLegacyTransform().itemTranslate(0.0, 1.5, 0.0)
                                .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -2.0, 0.0)
                                .translate(0.75, 0.0, 0.0).parts("RocketStandard")
                                .passes(MachineObjModel.pass("hbm:textures/models/projectiles/himars_standard_he.png").parts("TubeStandard").translate(-0.75, -3.375, 0.0)))
                        .when("2", MachineObjModel.of("hbm:models/turrets/turret_himars.obj", "hbm:textures/models/projectiles/himars_standard_wp.png")
                                .scale(1.0).ty(0.0).yaw(-45.0).pitch(90.0).guiScale(2.75).guiSpinSpeed(0.1)
                                .rawLegacyTransform().itemTranslate(0.0, 1.5, 0.0)
                                .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -2.0, 0.0)
                                .translate(0.75, 0.0, 0.0).parts("RocketStandard")
                                .passes(MachineObjModel.pass("hbm:textures/models/projectiles/himars_standard_wp.png").parts("TubeStandard").translate(-0.75, -3.375, 0.0)))
                        .when("3", MachineObjModel.of("hbm:models/turrets/turret_himars.obj", "hbm:textures/models/projectiles/himars_standard_tb.png")
                                .scale(1.0).ty(0.0).yaw(-45.0).pitch(90.0).guiScale(2.75).guiSpinSpeed(0.1)
                                .rawLegacyTransform().itemTranslate(0.0, 1.5, 0.0)
                                .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -2.0, 0.0)
                                .translate(0.75, 0.0, 0.0).parts("RocketStandard")
                                .passes(MachineObjModel.pass("hbm:textures/models/projectiles/himars_standard_tb.png").parts("TubeStandard").translate(-0.75, -3.375, 0.0)))
                        .when("4", MachineObjModel.of("hbm:models/turrets/turret_himars.obj", "hbm:textures/models/projectiles/himars_standard_lava.png")
                                .scale(1.0).ty(0.0).yaw(-45.0).pitch(90.0).guiScale(2.75).guiSpinSpeed(0.1)
                                .rawLegacyTransform().itemTranslate(0.0, 1.5, 0.0)
                                .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -2.0, 0.0)
                                .translate(0.75, 0.0, 0.0).parts("RocketStandard")
                                .passes(MachineObjModel.pass("hbm:textures/models/projectiles/himars_standard_lava.png").parts("TubeStandard").translate(-0.75, -3.375, 0.0)))
                        .when("5", MachineObjModel.of("hbm:models/turrets/turret_himars.obj", "hbm:textures/models/projectiles/himars_standard_mini_nuke.png")
                                .scale(1.0).ty(0.0).yaw(-45.0).pitch(90.0).guiScale(2.75).guiSpinSpeed(0.1)
                                .rawLegacyTransform().itemTranslate(0.0, 1.5, 0.0)
                                .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -2.0, 0.0)
                                .translate(0.75, 0.0, 0.0).parts("RocketStandard")
                                .passes(MachineObjModel.pass("hbm:textures/models/projectiles/himars_standard_mini_nuke.png").parts("TubeStandard").translate(-0.75, -3.375, 0.0)))
                        .when("6", MachineObjModel.of("hbm:models/turrets/turret_himars.obj", "hbm:textures/models/projectiles/himars_single.png")
                                .scale(1.0).ty(0.0).yaw(-45.0).pitch(90.0).guiScale(2.75).guiSpinSpeed(0.1)
                                .rawLegacyTransform().itemTranslate(0.0, 1.5, 0.0)
                                .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -2.0, 0.0)
                                .translate(0.75, 0.0, 0.0).parts("RocketSingle")
                                .passes(MachineObjModel.pass("hbm:textures/models/projectiles/himars_single.png").parts("TubeSingle").translate(-0.75, -3.375, 0.0)))
                        .when("7", MachineObjModel.of("hbm:models/turrets/turret_himars.obj", "hbm:textures/models/projectiles/himars_single_tb.png")
                                .scale(1.0).ty(0.0).yaw(-45.0).pitch(90.0).guiScale(2.75).guiSpinSpeed(0.1)
                                .rawLegacyTransform().itemTranslate(0.0, 1.5, 0.0)
                                .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -2.0, 0.0)
                                .translate(0.75, 0.0, 0.0).parts("RocketSingle")
                                .passes(MachineObjModel.pass("hbm:textures/models/projectiles/himars_single_tb.png").parts("TubeSingle").translate(-0.75, -3.375, 0.0)))
                        .fallback(MachineObjModel.of("hbm:models/turrets/turret_himars.obj", "hbm:textures/models/projectiles/himars_standard.png")
                                .scale(1.0).ty(0.0).yaw(-45.0).pitch(90.0).guiScale(2.75).guiSpinSpeed(0.1)
                                .rawLegacyTransform().itemTranslate(0.0, 1.5, 0.0)
                                .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -2.0, 0.0)
                                .translate(0.75, 0.0, 0.0).parts("RocketStandard")
                                .passes(MachineObjModel.pass("hbm:textures/models/projectiles/himars_standard.png").parts("TubeStandard").translate(-0.75, -3.375, 0.0)))).oversizedInGui().register();
        Dejson.item(NS, "anvil_arsenic_bronze")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/anvils/anvil_arsenic_bronze.obj", "hbm:textures/models/anvils/anvil_arsenic_bronze.png")
                                .scale(1.3).ty(-0.1).yaw(0))).register();
        Dejson.item(NS, "anvil_bismuth_bronze")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/anvils/anvil_bismuth_bronze.obj", "hbm:textures/models/anvils/anvil_bismuth_bronze.png")
                                .scale(1.3).ty(-0.1).yaw(0))).register();
        Dejson.item(NS, "anvil_desh")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/anvils/anvil_desh.obj", "hbm:textures/models/anvils/anvil_desh.png").scale(1.3)
                                .ty(-0.1).yaw(0))).register();
        Dejson.item(NS, "anvil_dnt")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/anvils/anvil_dnt.obj", "hbm:textures/models/anvils/anvil_dnt.png").scale(1.3)
                                .ty(-0.1).yaw(0))).register();
        Dejson.item(NS, "anvil_ferrouranium")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/anvils/anvil_ferrouranium.obj", "hbm:textures/models/anvils/anvil_ferrouranium.png")
                                .scale(1.3).ty(-0.1).yaw(0))).register();
        Dejson.item(NS, "anvil_iron")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/anvils/anvil_iron.obj", "hbm:textures/models/anvils/anvil_iron.png").scale(1.3)
                                .ty(-0.1).yaw(0))).register();
        Dejson.item(NS, "anvil_lead")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/anvils/anvil_lead.obj", "hbm:textures/models/anvils/anvil_lead.png").scale(1.3)
                                .ty(-0.1).yaw(0))).register();
        Dejson.item(NS, "anvil_murky")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/anvils/anvil_murky.obj", "hbm:textures/models/anvils/anvil_murky.png")
                                .scale(1.3).ty(-0.1).yaw(0))).register();
        Dejson.item(NS, "anvil_osmiridium")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/anvils/anvil_osmiridium.obj", "hbm:textures/models/anvils/anvil_osmiridium.png")
                                .scale(1.3).ty(-0.1).yaw(0))).register();
        Dejson.item(NS, "anvil_saturnite")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/anvils/anvil_saturnite.obj", "hbm:textures/models/anvils/anvil_saturnite.png")
                                .scale(1.3).ty(-0.1).yaw(0))).register();
        Dejson.item(NS, "anvil_schrabidate")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/anvils/anvil_schrabidate.obj", "hbm:textures/models/anvils/anvil_schrabidate.png")
                                .scale(1.3).ty(-0.1).yaw(0))).register();
        Dejson.item(NS, "anvil_steel")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/anvils/anvil_steel.obj", "hbm:textures/models/anvils/anvil_steel.png")
                                .scale(1.3).ty(-0.1).yaw(0))).register();
        Dejson.item(NS, "barbed_wire")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/barbed_wire.obj", "hbm:textures/blocks/barbed_wire_model.png")
                                .scale(1.0).ty(0.0).yaw(0))).register();
        Dejson.item(NS, "barbed_wire_acid")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/barbed_wire.obj", "hbm:textures/blocks/barbed_wire_acid_model.png")
                                .scale(1.0).ty(0.0).yaw(0))).register();
        Dejson.item(NS, "barbed_wire_fire")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/barbed_wire.obj", "hbm:textures/blocks/barbed_wire_fire_model.png")
                                .scale(1.0).ty(0.0).yaw(0))).register();
        Dejson.item(NS, "barbed_wire_poison")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/barbed_wire.obj", "hbm:textures/blocks/barbed_wire_poison_model.png")
                                .scale(1.0).ty(0.0).yaw(0))).register();
        Dejson.item(NS, "barbed_wire_ultradeath")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/barbed_wire.obj", "hbm:textures/blocks/barbed_wire_ultradeath_model.png")
                                .scale(1.0).ty(0.0).yaw(0))).register();
        Dejson.item(NS, "barbed_wire_wither")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/barbed_wire.obj", "hbm:textures/blocks/barbed_wire_wither_model.png")
                                .scale(1.0).ty(0.0).yaw(0))).register();
        Dejson.item(NS, "barrel_antimatter")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/barrel.obj", "hbm:textures/blocks/barrel_antimatter.png").scale(0.5)
                                .ty(-0.5).yaw(0).exclude("Connector"))).register();
        Dejson.item(NS, "barrel_corroded")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/barrel.obj", "hbm:textures/blocks/barrel_corroded.png").scale(0.5)
                                .ty(-0.5).yaw(0).exclude("Connector"))).register();
        Dejson.item(NS, "barrel_plastic")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/barrel.obj", "hbm:textures/blocks/barrel_plastic.png").scale(0.5)
                                .ty(-0.5).yaw(0).exclude("Connector"))).register();
        Dejson.item(NS, "barrel_steel")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/barrel.obj", "hbm:textures/blocks/barrel_steel.png").scale(0.5).ty(-0.5)
                                .yaw(0).exclude("Connector"))).register();
        Dejson.item(NS, "barrel_tcalloy")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/barrel.obj", "hbm:textures/blocks/barrel_tcalloy.png").scale(0.5)
                                .ty(-0.5).yaw(0).exclude("Connector"))).register();
        Dejson.item(NS, "battery_lead")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/battery_socket.obj", "hbm:textures/models/machines/battery_lead.png")
                                .scale(0.45).ty(0.0).yaw(180).exclude("Socket", "Capacitor"))).register();
        Dejson.item(NS, "battery_lithium")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/battery_socket.obj", "hbm:textures/models/machines/battery_lithium.png")
                                .scale(0.45).ty(0.0).yaw(180).exclude("Socket", "Capacitor"))).register();
        Dejson.item(NS, "battery_quantum")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/battery_socket.obj", "hbm:textures/models/machines/battery_quantum.png")
                                .scale(0.45).ty(0.0).yaw(180).exclude("Socket", "Capacitor"))).register();
        Dejson.item(NS, "battery_redstone")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/battery_socket.obj", "hbm:textures/models/machines/battery_redstone.png")
                                .scale(0.45).ty(0.0).yaw(180).exclude("Socket", "Capacitor"))).register();
        Dejson.item(NS, "battery_schrabidium")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/battery_socket.obj", "hbm:textures/models/machines/battery_schrabidium.png")
                                .scale(0.45).ty(0.0).yaw(180).exclude("Socket", "Capacitor"))).register();
        Dejson.item(NS, "battery_sodium")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/battery_socket.obj", "hbm:textures/models/machines/battery_sodium.png")
                                .scale(0.45).ty(0.0).yaw(180).exclude("Socket", "Capacitor"))).register();
    }

    private static void part1() {
        Dejson.item(NS, "bj_boots")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/armor/bj.obj", "hbm:textures/armor/bj_leg.png").scale(0.099).yaw(155.0)
                                .pitch(180.0).noCull()
                                .exclude("Body", "Head", "Jetpack", "LeftArm", "LeftBoot", "LeftLeg", "RightArm", "RightBoot", "RightLeg")
                                .animation("armor_bj_feet")
                                .partTextures("LeftFoot", "hbm:textures/armor/bj_leg.png", "RightFoot", "hbm:textures/armor/bj_leg.png"))).register();
        Dejson.item(NS, "bj_helmet")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/armor/bj.obj", "hbm:textures/armor/bj_eyepatch.png").scale(0.078).yaw(155.0)
                                .pitch(180.0).flag("noCull", false)
                                .exclude("Body", "Jetpack", "LeftArm", "LeftBoot", "LeftFoot", "LeftLeg", "RightArm", "RightBoot", "RightFoot", "RightLeg")
                                .animation("armor_bj_head")
                                .partTextures("Head", "hbm:textures/armor/bj_eyepatch.png"))).register();
        Dejson.item(NS, "bj_legs")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/armor/bj.obj", "hbm:textures/armor/bj_leg.png").scale(0.099).yaw(155.0)
                                .pitch(180.0).noCull()
                                .exclude("Body", "Head", "Jetpack", "LeftArm", "LeftBoot", "LeftFoot", "RightArm", "RightBoot", "RightFoot")
                                .animation("armor_bj_legs")
                                .partTextures("LeftLeg", "hbm:textures/armor/bj_leg.png", "RightLeg", "hbm:textures/armor/bj_leg.png"))).register();
        Dejson.item(NS, "bj_plate")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/armor/bj.obj", "hbm:textures/armor/bj_chest.png").scale(0.044).yaw(155.0)
                                .pitch(180.0).flag("noCull", false)
                                .exclude("Head", "Jetpack", "LeftBoot", "LeftFoot", "LeftLeg", "RightBoot", "RightFoot", "RightLeg")
                                .animation("armor_bj_chest")
                                .partTextures("Body", "hbm:textures/armor/bj_chest.png", "LeftArm", "hbm:textures/armor/bj_arm.png", "RightArm", "hbm:textures/armor/bj_arm.png"))).register();
        Dejson.item(NS, "bj_plate_jetpack")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/armor/bj.obj", "hbm:textures/armor/bj_chest.png").scale(0.044).yaw(155.0)
                                .pitch(180.0).flag("noCull", false)
                                .exclude("Head", "LeftBoot", "LeftFoot", "LeftLeg", "RightBoot", "RightFoot", "RightLeg")
                                .animation("armor_bj_jetpack_chest")
                                .partTextures("Body", "hbm:textures/armor/bj_chest.png", "LeftArm", "hbm:textures/armor/bj_arm.png", "RightArm", "hbm:textures/armor/bj_arm.png", "Jetpack", "hbm:textures/armor/bj_jetpack.png"))).register();
        Dejson.item(NS, "blast_door")
                .definition(MachineObjModel.of("hbm:models/doors/blast_door_base.obj", "hbm:textures/models/doors/blast_door_base.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(3.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -3.0, 0.0)
                        .passes(MachineObjModel.pass("hbm:textures/models/doors/blast_door_tooth.png").obj("hbm:models/doors/blast_door_tooth.obj"),
                                MachineObjModel.pass("hbm:textures/models/doors/blast_door_slider.png").obj("hbm:models/doors/blast_door_slider.obj"),
                                MachineObjModel.pass("hbm:textures/models/doors/blast_door_block.png").obj("hbm:models/doors/blast_door_block.obj"))).oversizedInGui().register();
        Dejson.item(NS, "boat")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/duchessgambit.obj", "hbm:textures/models/misc/duchessgambit.png").scale(0.14)
                                .ty(0.0).yaw(0))).register();
        Dejson.item(NS, "bomb_multi")
                .definition(MachineObjModel.of("hbm:models/bombs/bomb_generic.obj", "hbm:textures/models/bombs/bomb_generic.png")
                        .scale(1.0).ty(0.0).yaw(90.0).guiScale(2.5).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, 0.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "boxcar")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/boxcar.obj", "hbm:textures/models/misc/boxcar.png").scale(0.18).ty(0.0)
                                .yaw(270))).register();
        Dejson.item(NS, "capacitor_bismuth")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/battery_socket.obj", "hbm:textures/models/machines/capacitor_bismuth.png")
                                .scale(0.45).ty(0.0).yaw(180).exclude("Socket", "Battery"))).register();
        Dejson.item(NS, "capacitor_copper")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/battery_socket.obj", "hbm:textures/models/machines/capacitor_copper.png")
                                .scale(0.45).ty(0.0).yaw(180).exclude("Socket", "Battery"))).register();
        Dejson.item(NS, "capacitor_gold")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/battery_socket.obj", "hbm:textures/models/machines/capacitor_gold.png")
                                .scale(0.45).ty(0.0).yaw(180).exclude("Socket", "Battery"))).register();
        Dejson.item(NS, "capacitor_niobium")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/battery_socket.obj", "hbm:textures/models/machines/capacitor_niobium.png")
                                .scale(0.45).ty(0.0).yaw(180).exclude("Socket", "Battery"))).register();
        Dejson.item(NS, "capacitor_spark")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/battery_socket.obj", "hbm:textures/models/machines/capacitor_spark.png")
                                .scale(0.45).ty(0.0).yaw(180).exclude("Socket", "Battery"))).register();
        Dejson.item(NS, "capacitor_tantalum")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/battery_socket.obj", "hbm:textures/models/machines/capacitor_tantalum.png")
                                .scale(0.45).ty(0.0).yaw(180).exclude("Socket", "Battery"))).register();
        Dejson.item(NS, "cargo_door")
                .definition(MachineObjModel.of("hbm:models/pheodoors/cargo_door.obj", "hbm:textures/models/pheodoors/cargo_door.png")
                        .scale(0.45).ty(0.0).yaw(0.0).guiScale(4.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.15, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -4.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "cargo_elevator")
                .definition(MachineObjModel.of("hbm:models/machines/elevator.obj", "hbm:textures/models/machines/elevator.png").scale(1.0)
                        .ty(0.0).yaw(0.0).guiScale(3.25).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -2.75, 0.0)
                        .parts("Base", "Piston", "Guides")
                        .passes(MachineObjModel.pass("hbm:textures/models/machines/elevator.png").parts("Piston", "Guides", "Platform").translate(0.0, 1.0, 0.0),
                                MachineObjModel.pass("hbm:textures/models/machines/elevator.png").parts("Guides").translate(0.0, 2.0, 0.0))).oversizedInGui().register();
        Dejson.item(NS, "charge_c4")
                .definition(MachineObjModel.of("hbm:models/blocks/charge_c4.obj", "hbm:textures/blocks/charge_c4.png").scale(1.0).ty(0.0)
                        .yaw(0.0).guiScale(3.0).rawLegacyTransform().noCull().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, 0.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "charge_dynamite")
                .definition(MachineObjModel.of("hbm:models/blocks/charge_dynamite.obj", "hbm:textures/blocks/charge_dynamite.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(3.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, 0.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "charge_miner")
                .definition(MachineObjModel.of("hbm:models/blocks/charge_dynamite.obj", "hbm:textures/blocks/charge_miner.png").scale(1.0)
                        .ty(0.0).yaw(0.0).guiScale(3.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, 0.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "charge_semtex")
                .definition(MachineObjModel.of("hbm:models/blocks/charge_c4.obj", "hbm:textures/blocks/charge_semtex.png").scale(1.0)
                        .ty(0.0).yaw(0.0).guiScale(3.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, 0.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "chimney_brick")
                .definition(MachineObjModel.of("hbm:models/machines/chimney_brick.obj", "hbm:textures/models/machines/chimney_brick.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(2.25).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -5.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "chimney_industrial")
                .definition(MachineObjModel.of("hbm:models/machines/chimney_industrial.obj", "hbm:textures/models/machines/chimney_industrial.png")
                        .scale(0.25).ty(0.0).yaw(0.0).guiScale(2.75).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -5.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "crane_partitioner")
                .definition(MachineObjModel.of("hbm:models/blocks/crane_buffer.obj", "hbm:textures/blocks/crane_partitioner_side.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(5.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -2.0, 0.0)
                        .partTextures("Side", "hbm:textures/blocks/crane_partitioner_side.png", "Back", "hbm:textures/blocks/crane_partitioner_back.png", "Top_Top.001", "hbm:textures/blocks/crane_top.png", "Inner", "hbm:textures/blocks/crane_splitter_inner.png", "InnerSide", "hbm:textures/blocks/crane_splitter_inner_side.png", "Belt", "hbm:textures/blocks/crane_splitter_belt_still.png")).oversizedInGui().register();
        Dejson.item(NS, "crane_splitter")
                .definition(MachineObjModel.of("hbm:models/blocks/splitter.obj", "hbm:textures/blocks/crane_splitter_inner.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(5.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -2.0, 0.0).exclude("Right")
                        .partTextures("Top", "hbm:textures/blocks/crane_splitter_top_left.png", "Bottom", "hbm:textures/blocks/crane_splitter_top_right.png", "Left", "hbm:textures/blocks/crane_splitter_left.png", "Right", "hbm:textures/blocks/crane_splitter_right.png", "Back", "hbm:textures/blocks/crane_splitter_back_left.png", "Front", "hbm:textures/blocks/crane_splitter_front_left.png", "Inner", "hbm:textures/blocks/crane_splitter_inner.png", "InnerLeft", "hbm:textures/blocks/crane_splitter_inner_side.png", "InnerRight", "hbm:textures/blocks/crane_splitter_inner_side.png", "InnerTop", "hbm:textures/blocks/crane_splitter_inner_side.png", "InnerBottom", "hbm:textures/blocks/crane_splitter_belt_still.png")).oversizedInGui().register();
        Dejson.item(NS, "crashed_bomb_balefire")
                .definition(MachineObjModel.of("hbm:models/bombs/dud_balefire.obj", "hbm:textures/models/bombs/dud_balefire.png")
                        .scale(1.0).ty(0.0).yaw(90.0).guiScale(2.125).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, 0.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "crashed_bomb_conventional")
                .definition(MachineObjModel.of("hbm:models/bombs/dud_conventional.obj", "hbm:textures/models/bombs/dud_conventional.png")
                        .scale(1.0).ty(0.0).yaw(90.0).guiScale(2.125).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, 0.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "crashed_bomb_nuke")
                .definition(MachineObjModel.of("hbm:models/bombs/dud_nuke.obj", "hbm:textures/models/bombs/dud_nuke.png").scale(1.0)
                        .ty(0.0).yaw(90.0).guiScale(2.125).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, 0.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "crashed_bomb_salted")
                .definition(MachineObjModel.of("hbm:models/bombs/dud_salted.obj", "hbm:textures/models/bombs/dud_salted.png").scale(1.0)
                        .ty(0.0).yaw(90.0).guiScale(2.125).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, 0.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "crate_supply")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/conservecrate.obj", "hbm:textures/block/crate_can.png").scale(1.0)
                                .ty(0.0).yaw(0))).register();
    }

    private static void part2() {
        Dejson.item(NS, "deco_computer_ibm_300pl")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/puter.obj", "hbm:textures/blocks/deco_computer.png").scale(1.2).ty(0.1)
                                .yaw(0).noCull())).register();
        Dejson.item(NS, "deco_crt_blinking")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/crt.obj", "hbm:textures/blocks/crt_blinking.png").scale(1.0).ty(-0.5)
                                .yaw(0).noCull())).register();
        Dejson.item(NS, "deco_crt_broken")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/crt.obj", "hbm:textures/blocks/crt_broken.png").scale(1.0).ty(-0.5)
                                .yaw(0).noCull())).register();
        Dejson.item(NS, "deco_crt_bsod")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/crt.obj", "hbm:textures/blocks/crt_bsod.png").scale(1.0).ty(-0.5).yaw(0)
                                .noCull())).register();
        Dejson.item(NS, "deco_crt_clean")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/crt.obj", "hbm:textures/blocks/crt_clean.png").scale(1.0).ty(-0.5)
                                .yaw(0).noCull())).register();
        Dejson.item(NS, "deco_sat_foeq")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/sat_foeq.obj", "hbm:textures/models/sat/sat_foeq.png").scale(0.22).ty(0.0)
                                .yaw(0))).register();
        Dejson.item(NS, "deco_sat_laser")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/sat_laser.obj", "hbm:textures/models/sat/sat_laser.png").scale(0.22).ty(0.0)
                                .yaw(0))).register();
        Dejson.item(NS, "deco_sat_mapper")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/sat_mapper.obj", "hbm:textures/models/sat/sat_mapper.png").scale(0.22).ty(0.0)
                                .yaw(0))).register();
        Dejson.item(NS, "deco_sat_radar")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/sat_radar.obj", "hbm:textures/models/sat/sat_radar.png").scale(0.22).ty(0.0)
                                .yaw(0))).register();
        Dejson.item(NS, "deco_sat_resonator")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/sat_resonator.obj", "hbm:textures/models/sat/sat_resonator.png").scale(0.22)
                                .ty(0.0).yaw(0))).register();
        Dejson.item(NS, "deco_sat_scanner")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/sat_scanner.obj", "hbm:textures/models/sat/sat_scanner.png").scale(0.22)
                                .ty(0.0).yaw(0))).register();
        Dejson.item(NS, "deco_toaster_iron")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/toaster.obj", "hbm:textures/blocks/toaster_iron.png").scale(1.0)
                                .ty(-0.5).yaw(0).noCull())).register();
        Dejson.item(NS, "deco_toaster_steel")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/toaster.obj", "hbm:textures/blocks/toaster_steel.png").scale(1.0)
                                .ty(-0.5).yaw(0).noCull())).register();
        Dejson.item(NS, "deco_toaster_wood")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/toaster.obj", "hbm:textures/blocks/toaster_wood.png").scale(1.0)
                                .ty(-0.5).yaw(0).noCull())).register();
        Dejson.item(NS, "det_cord")
                .definition(MachineObjModel.of("hbm:models/blocks/cable_neo.obj", "hbm:textures/blocks/det_cord.png").scale(1.25).ty(0.0)
                        .yaw(180.0).guiScale(3.0).rawLegacyTransform().noCull().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, 0.0, 0.0)
                        .exclude("posX", "negX", "posY", "negY", "posZ", "negZ", "Core", "CX", "CY")).oversizedInGui().register();
        Dejson.item(NS, "dfc_emitter")
                .definition(MachineObjModel.of("hbm:models/core_emitter.obj", "hbm:textures/models/machines/core_emitter.png").scale(2.0)
                        .ty(0.0).yaw(90.0).guiScale(5.0).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -2.5, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "dfc_injector")
                .definition(MachineObjModel.of("hbm:models/core_injector.obj", "hbm:textures/models/machines/core_injector.png")
                        .scale(2.0).ty(0.0).yaw(90.0).guiScale(5.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -2.5, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "dfc_receiver")
                .definition(MachineObjModel.of("hbm:models/core_receiver.obj", "hbm:textures/models/machines/core_receiver.png")
                        .scale(2.0).ty(0.0).yaw(90.0).guiScale(5.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -2.5, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "dfc_stabilizer")
                .definition(MachineObjModel.of("hbm:models/core_injector.obj", "hbm:textures/models/machines/core_stabilizer.png")
                        .scale(2.0).ty(0.0).yaw(90.0).guiScale(5.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -2.5, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "dns_boots")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/armor/dnt.obj", "hbm:textures/armor/dnt_leg.png").scale(0.099).yaw(155.0)
                                .pitch(180.0).noCull()
                                .exclude("Body", "Head", "Jetpack", "LeftArm", "LeftFoot", "LeftLeg", "RightArm", "RightFoot", "RightLeg")
                                .animation("armor_dnt_feet")
                                .partTextures("LeftBoot", "hbm:textures/armor/dnt_leg.png", "RightBoot", "hbm:textures/armor/dnt_leg.png"))).register();
        Dejson.item(NS, "dns_helmet")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/armor/dnt.obj", "hbm:textures/armor/dnt_helmet.png").scale(0.078).yaw(155.0)
                                .pitch(180.0).flag("noCull", false)
                                .exclude("Body", "Jetpack", "LeftArm", "LeftBoot", "LeftFoot", "LeftLeg", "RightArm", "RightBoot", "RightFoot", "RightLeg")
                                .animation("armor_dnt_head")
                                .partTextures("Head", "hbm:textures/armor/dnt_helmet.png"))).register();
        Dejson.item(NS, "dns_legs")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/armor/dnt.obj", "hbm:textures/armor/dnt_leg.png").scale(0.099).yaw(155.0)
                                .pitch(180.0).noCull()
                                .exclude("Body", "Head", "Jetpack", "LeftArm", "LeftBoot", "LeftFoot", "RightArm", "RightBoot", "RightFoot")
                                .animation("armor_dnt_legs")
                                .partTextures("LeftLeg", "hbm:textures/armor/dnt_leg.png", "RightLeg", "hbm:textures/armor/dnt_leg.png"))).register();
        Dejson.item(NS, "dns_plate")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/armor/dnt.obj", "hbm:textures/armor/dnt_chest.png").scale(0.044).yaw(155.0)
                                .pitch(180.0).flag("noCull", false)
                                .exclude("Head", "Jetpack", "LeftBoot", "LeftFoot", "LeftLeg", "RightBoot", "RightFoot", "RightLeg")
                                .animation("armor_dnt_chest")
                                .partTextures("Body", "hbm:textures/armor/dnt_chest.png", "LeftArm", "hbm:textures/armor/dnt_arm.png", "RightArm", "hbm:textures/armor/dnt_arm.png"))).register();
        Dejson.item(NS, "fan")
                .definition(MachineObjModel.of("hbm:models/machines/fan.obj", "hbm:textures/models/machines/fan.png").scale(2.0).ty(0.0)
                        .yaw(0.0).guiScale(5.0).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -2.5, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "fire_door")
                .definition(MachineObjModel.of("hbm:models/pheodoors/fire_door.obj", "hbm:textures/models/pheodoors/fire_door.png")
                        .scale(0.5).ty(0.0).yaw(90.0).guiScale(3.5).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -3.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "floodlight")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/floodlight.obj", "hbm:textures/models/machines/floodlight.png")
                                .scale(1.0).ty(-0.5).yaw(90).noCull())).register();
        Dejson.item(NS, "fluid_duct_mk2")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/pipe_neo.obj", "hbm:textures/models/blocks/fluid_duct_mk2.png")
                                .scale(1.1).ty(0.0).yaw(0)
                                .exclude("pY", "nY", "pZ", "nZ", "ppp", "ppn", "pnp", "pnn", "npp", "npn", "nnp", "nnn"))).register();
        Dejson.item(NS, "fluid_duct_neo")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/pipe_neo.obj", "hbm:textures/blocks/pipe_neo.png").scale(1.1).ty(0.0)
                                .yaw(0)
                                .exclude("pY", "nY", "pZ", "nZ", "ppp", "ppn", "pnp", "pnn", "npp", "npn", "nnp", "nnn"))).register();
        Dejson.item(NS, "fluid_duct_neo_colored")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/pipe_neo.obj", "hbm:textures/blocks/pipe_colored.png").scale(1.1)
                                .ty(0.0).yaw(0)
                                .exclude("pY", "nY", "pZ", "nZ", "ppp", "ppn", "pnp", "pnn", "npp", "npn", "nnp", "nnn"))).register();
        Dejson.item(NS, "fluid_duct_neo_silver")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/pipe_neo.obj", "hbm:textures/blocks/pipe_silver.png").scale(1.1).ty(0.0)
                                .yaw(0)
                                .exclude("pY", "nY", "pZ", "nZ", "ppp", "ppn", "pnp", "pnn", "npp", "npn", "nnp", "nnn"))).register();
    }

    private static void part3() {
        Dejson.item(NS, "fluid_pump")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/network/fluid_diode.obj", "hbm:textures/models/network/fluid_diode.png")
                                .scale(0.5).ty(0.0).yaw(90))).register();
        Dejson.item(NS, "foundry_basin")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/block/foundry_basin.obj").scale(1.0).ty(0.0).yaw(0).mtl())).register();
        Dejson.item(NS, "foundry_outlet")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/block/foundry_outlet.obj").scale(1.3).ty(0.0).yaw(0).mtl())).register();
        Dejson.item(NS, "foundry_slagtap")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/block/foundry_slagtap.obj").scale(1.3).ty(0.0).yaw(0).mtl())).register();
        Dejson.item(NS, "fraction_spacer")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/fraction_spacer.obj", "hbm:textures/models/machines/fraction_spacer.png")
                                .scale(0.35).ty(0.0).yaw(0))).register();
        Dejson.item(NS, "furnace_iron")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/furnace_iron.obj", "hbm:textures/models/machines/furnace_iron.png")
                                .scale(0.525).ty(0.0).yaw(90).exclude("On"))).register();
        Dejson.item(NS, "furnace_steel")
                .definition(MachineObjModel.of("hbm:models/machines/furnace_steel.obj", "hbm:textures/models/machines/furnace_steel.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(3.25).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.5, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "fusion_boiler")
                .definition(MachineObjModel.of("hbm:models/fusion/boiler.obj", "hbm:textures/models/fusion/boiler.png").scale(0.5).ty(0.0)
                        .yaw(90.0).guiScale(3.5).guiYaw(90.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "fusion_breeder")
                .definition(MachineObjModel.of("hbm:models/fusion/breeder.obj", "hbm:textures/models/fusion/breeder.png").scale(0.5)
                        .ty(0.0).yaw(90.0).guiScale(5.0).guiYaw(90.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -3.0, 0.0).exclude("BreederAlt")).oversizedInGui().register();
        Dejson.item(NS, "fusion_collector")
                .definition(MachineObjModel.of("hbm:models/fusion/collector.obj", "hbm:textures/models/fusion/collector.png").scale(0.5)
                        .ty(0.0).yaw(90.0).guiScale(5.0).guiYaw(90.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -2.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "fusion_coupler")
                .definition(MachineObjModel.of("hbm:models/fusion/coupler.obj", "hbm:textures/models/fusion/coupler.png").scale(0.5)
                        .ty(0.0).yaw(90.0).guiScale(6.0).guiYaw(90.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -3.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "fusion_klystron")
                .definition(MachineObjModel.of("hbm:models/fusion/klystron.obj", "hbm:textures/models/fusion/klystron.png").scale(0.5)
                        .ty(0.0).yaw(90.0).guiScale(3.5).guiYaw(90.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -3.0, 1.0)).oversizedInGui().register();
        Dejson.item(NS, "fusion_klystron_creative")
                .definition(MachineObjModel.of("hbm:models/fusion/klystron.obj", "hbm:textures/models/fusion/klystron_creative.png")
                        .scale(0.5).ty(0.0).yaw(90.0).guiScale(3.5).guiYaw(90.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -3.0, 1.0)).oversizedInGui().register();
        Dejson.item(NS, "fusion_mhdt")
                .definition(MachineObjModel.of("hbm:models/fusion/mhdt.obj", "hbm:textures/models/fusion/mhdt.png").scale(0.5).ty(0.0)
                        .yaw(90.0).guiScale(2.5).guiYaw(90.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, 0.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "fusion_plasma_forge")
                .definition(MachineObjModel.of("hbm:models/fusion/plasma_forge.obj", "hbm:textures/models/fusion/plasma_forge.png")
                        .scale(0.5).ty(0.0).yaw(90.0).guiScale(2.75).guiYaw(90.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.0, 0.0).exclude("Plasma").blackParts("Plasma")).oversizedInGui().register();
        Dejson.item(NS, "fusion_torus")
                .definition(MachineObjModel.of("hbm:models/fusion/torus.obj", "hbm:textures/models/fusion/torus.png").scale(0.5).ty(0.0)
                        .yaw(90.0).guiScale(2.0).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, 0.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "gear_large")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("1", ItemModels.special("hbm:item/machine_obj_base",
                                MachineObjModel.of("hbm:models/machines/stirling.obj", "hbm:textures/models/machines/stirling_steel.png")
                                        .scale(0.372).yaw(-45.0).pitch(30.0).spin()
                                        .exclude("Base", "CogSmall", "Piston")))
                        .fallback(ItemModels.special("hbm:item/machine_obj_base",
                                MachineObjModel.of("hbm:models/machines/stirling.obj", "hbm:textures/models/machines/stirling.png")
                                        .scale(0.372).yaw(-45.0).pitch(30.0).spin()
                                        .exclude("Base", "CogSmall", "Piston")))).register();
        Dejson.item(NS, "geiger")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/geiger_counter.obj", "hbm:textures/blocks/geiger.png").scale(1.0)
                                .ty(0.0).yaw(90).noCull())).register();
        Dejson.item(NS, "heat_boiler")
                .definition(MachineObjModel.of("hbm:models/machines/boiler.obj", "hbm:textures/models/machines/boiler.png").scale(1.0)
                        .ty(0.0).yaw(90.0).guiScale(3.0).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -3.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "heater_electric")
                .definition(MachineObjModel.of("hbm:models/machines/electric_heater.obj", "hbm:textures/models/machines/electric_heater.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(3.0).rawLegacyTransform()
                        .itemTranslate(-0.5, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, 0.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "heater_firebox")
                .definition(MachineObjModel.of("hbm:models/machines/firebox.obj", "hbm:textures/models/machines/firebox.png").scale(1.0)
                        .ty(0.0).yaw(0.0).guiScale(3.25).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -1.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "heater_heatex")
                .definition(MachineObjModel.of("hbm:models/machines/heatex.obj", "hbm:textures/models/machines/heater_heatex.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(3.25).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "heater_oilburner")
                .definition(MachineObjModel.of("hbm:models/machines/oilburner.obj", "hbm:textures/models/machines/oilburner.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(3.25).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.5, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "heater_oven")
                .definition(MachineObjModel.of("hbm:models/machines/heating_oven.obj", "hbm:textures/models/machines/heating_oven.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(3.25).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "hev_battery")
                .definition(MachineObjModel.of("hbm:models/blocks/battery.obj", "hbm:textures/blocks/hev_battery.png").scale(1.0).ty(0.0)
                        .yaw(0.0).guiScale(3.0).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -0.625, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "jetpack_glider")
                .models(Models.parented("hbm:item/machine_obj_legacy", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/jetpack_glider_base",
                        ItemModels.custom("hbm:jetpack_glider").with("scale", 1.0).numbers("translate", 0, 0, 0))).register();
        Dejson.item(NS, "lamp_demon")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/demon_lamp.obj", "hbm:textures/models/machines/demon_lamp.png")
                                .scale(1.0).ty(-0.5).yaw(0).noCull())).register();
        Dejson.item(NS, "large_vehicle_door")
                .definition(MachineObjModel.of("hbm:models/pheodoors/vehicle_door.obj", "hbm:textures/models/pheodoors/vehicle_door.png")
                        .scale(0.5).ty(0.0).yaw(90.0).guiScale(1.8).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -4.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "launch_pad")
                .definition(MachineObjModel.of("hbm:models/weapons/launch_pad_silo.obj", "hbm:textures/models/launchpad/silo.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(3.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "lox_barrel")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/barrel.obj", "hbm:textures/blocks/barrel_lox.png").scale(0.5).ty(-0.5)
                                .yaw(0).exclude("Connector"))).register();
    }

    private static void part4() {
        Dejson.item(NS, "machine_ammo_press")
                .definition(MachineObjModel.of("hbm:models/machines/ammo_press.obj", "hbm:textures/models/machines/ammo_press.png")
                        .scale(1.0).ty(0.0).yaw(90.0).guiScale(5.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -2.5, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_annihilator")
                .definition(MachineObjModel.of("hbm:models/machines/annihilator.obj", "hbm:textures/models/machines/annihilator.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(2.75).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -3.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_arc_furnace")
                .definition(MachineObjModel.of("hbm:models/machines/arc_furnace.obj", "hbm:textures/models/machines/arc_furnace.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(3.5).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -3.0, 0.0)
                        .exclude("ContentsHot", "ContentsCold", "Electrode1Hot", "Electrode2Hot", "Electrode3Hot", "Electrode1Short", "Electrode2Short", "Electrode3Short")).oversizedInGui().register();
        Dejson.item(NS, "machine_arc_welder")
                .definition(MachineObjModel.of("hbm:models/machines/arc_welder.obj", "hbm:textures/models/machines/arc_welder.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(4.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -2.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_ashpit")
                .definition(MachineObjModel.of("hbm:models/machines/heating_oven.obj", "hbm:textures/models/machines/ashpit.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(3.25).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.0, 0.0).exclude("Inner", "InnerBurning")).oversizedInGui().register();
        Dejson.item(NS, "machine_assembly_factory")
                .definition(MachineObjModel.of("hbm:models/machines/assembly_factory.obj", "hbm:textures/models/machines/assembly_factory.png")
                        .scale(0.75).ty(0.0).yaw(0.0).guiScale(3.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.5, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_assembly_machine")
                .definition(MachineObjModel.of("hbm:models/machines/assembly_machine.obj", "hbm:textures/models/machines/assembly_machine.png")
                        .scale(0.75).ty(0.0).yaw(90.0).guiScale(4.5).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -2.75, 0.0).exclude("Ring2")).oversizedInGui().register();
        Dejson.item(NS, "machine_autosaw")
                .definition(MachineObjModel.of("hbm:models/machines/autosaw.obj", "hbm:textures/models/machines/autosaw.png").scale(0.5)
                        .ty(0.0).yaw(-90.0).guiScale(5.0).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -3.5, -3.0).animation("autosaw")).oversizedInGui().register();
        Dejson.item(NS, "machine_bat9000")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/bat9000.obj", "hbm:textures/models/machines/bat9000.png").scale(0.2)
                                .ty(0.0).yaw(0))).register();
        Dejson.item(NS, "machine_battery_redd")
                .definition(MachineObjModel.of("hbm:models/machines/fensu2.obj", "hbm:textures/models/machines/fensu2.png").scale(0.5)
                        .ty(0.0).yaw(-90.0).guiScale(2.5).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -3.0, 0.0).exclude("Plasma")).oversizedInGui().register();
        Dejson.item(NS, "machine_battery_socket")
                .definition(MachineObjModel.of("hbm:models/battery_socket.obj", "hbm:textures/models/machines/battery_socket.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(5.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -2.0, 0.0).exclude("Battery", "Capacitor")).oversizedInGui().register();
        Dejson.item(NS, "machine_bigasstank")
                .definition(MachineObjModel.of("hbm:models/machines/bigasstank.obj", "hbm:textures/models/machines/bigasstank.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(2.5).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_blast_furnace")
                .definition(MachineObjModel.of("hbm:models/machines/blast_furnace.obj", "hbm:textures/models/machines/blast_furnace.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(4.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -4.5, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_catalytic_cracker")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/cracking_tower.obj", "hbm:textures/models/machines/cracking_tower.png")
                                .scale(0.15).ty(0.0).yaw(180))).register();
        Dejson.item(NS, "machine_catalytic_reformer")
                .definition(MachineObjModel.of("hbm:models/machines/catalytic_reformer.obj", "hbm:textures/models/machines/catalytic_reformer.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(3.5).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -3.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_centrifuge")
                .definition(MachineObjModel.of("hbm:models/machines/centrifuge.obj", "hbm:textures/models/machines/centrifuge_new.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(3.5).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -4.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_chemical_factory")
                .definition(MachineObjModel.of("hbm:models/machines/chemical_factory.obj", "hbm:textures/models/machines/chemical_factory.png")
                        .scale(0.75).ty(0.0).yaw(0.0).guiScale(3.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.5, 0.0).exclude("Plane.001")).oversizedInGui().register();
        Dejson.item(NS, "machine_chemical_plant")
                .definition(MachineObjModel.of("hbm:models/machines/chemical_plant.obj", "hbm:textures/models/machines/chemical_plant.png")
                        .scale(0.75).ty(0.0).yaw(90.0).guiScale(4.5).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -2.75, 0.0).exclude("Fluid")).oversizedInGui().register();
        Dejson.item(NS, "machine_chungus")
                .definition(MachineObjModel.of("hbm:models/machines/chungus.obj", "hbm:textures/models/machines/chungus.png").scale(0.5)
                        .ty(0.0).yaw(90.0).guiScale(2.5).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.5, 0.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_coker")
                .definition(MachineObjModel.of("hbm:models/machines/coker.obj", "hbm:textures/models/machines/coker.png").scale(0.25)
                        .ty(0.0).yaw(0.0).guiScale(2.75).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -5.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_combustion_engine")
                .definition(MachineObjModel.of("hbm:models/machines/combustion_engine.obj", "hbm:textures/models/machines/combustion_engine.png")
                        .scale(1.0).ty(0.0).yaw(90.0).guiScale(2.75).rawLegacyTransform()
                        .itemTranslate(2.75, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_compressor")
                .definition(MachineObjModel.of("hbm:models/machines/compressor.obj", "hbm:textures/models/machines/compressor.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(3.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -4.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_condenser_powered")
                .definition(MachineObjModel.of("hbm:models/machines/condenser.obj", "hbm:textures/models/machines/condenser.png")
                        .scale(0.75).ty(0.0).yaw(0.0).guiScale(2.75).rawLegacyTransform()
                        .itemTranslate(0.375, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(-1.0, -1.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_conveyor_press")
                .definition(MachineObjModel.of("hbm:models/machines/conveyor_press.obj", "hbm:textures/models/machines/conveyor_press.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(4.5).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -4.0, 0.0).overlayParts("Belt")
                        .overlayTexture("hbm:textures/models/machines/conveyor_press_belt.png")).oversizedInGui().register();
        Dejson.item(NS, "machine_crucible")
                .definition(MachineObjModel.of("hbm:models/machines/crucible.obj", "hbm:textures/models/machines/crucible_heat.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(3.25).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.5, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_crystallizer")
                .definition(MachineObjModel.of("hbm:models/machines/crystallizer.obj", "hbm:textures/models/machines/crystallizer.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(4.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -4.0, 0.0).exclude("Fluid")).oversizedInGui().register();
        Dejson.item(NS, "machine_cyclotron")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/cyclotron.obj", "hbm:textures/models/machines/cyclotron.png")
                                .scale(0.25).ty(0.0).yaw(0)
                                .partTextures("B1", "hbm:textures/models/machines/cyclotron_ashes.png", "B2", "hbm:textures/models/machines/cyclotron_book.png", "B3", "hbm:textures/models/machines/cyclotron_gavel.png", "B4", "hbm:textures/models/machines/cyclotron_coin.png"))).register();
        Dejson.item(NS, "machine_deuterium_tower")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/machine_deuterium_tower.obj", "hbm:textures/models/machines/machine_deuterium_tower.png")
                                .scale(0.099275384).ty(0.0).yaw(180).noCull())).register();
        Dejson.item(NS, "machine_diesel")
                .definition(MachineObjModel.of("hbm:models/machines/dieselgen.obj", "hbm:textures/models/machines/dieselgen.png")
                        .scale(2.0).ty(0.0).yaw(0.0).guiScale(5.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -2.5, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_difurnace_ext")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/block/difurnace_extension.obj", "hbm:textures/blocks/difurnace_extension.png")
                                .scale(1.0).ty(0.0).yaw(0).mtl())).register();
    }

    private static void part5() {
        Dejson.item(NS, "machine_drain")
                .definition(MachineObjModel.of("hbm:models/machines/drain.obj", "hbm:textures/models/machines/drain.png").scale(1.0)
                        .ty(0.0).yaw(180.0).guiScale(5.0).rawLegacyTransform().itemTranslate(-0.75, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(-1.0, -1.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_electrolyser")
                .definition(MachineObjModel.of("hbm:models/machines/electrolyser.obj", "hbm:textures/models/machines/electrolyser.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(2.5).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(-1.0, -1.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_epress")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/epress_body.obj", "hbm:textures/models/machines/epress_body.png").scale(0.46)
                                .ty(0.0).yaw(0))).register();
        Dejson.item(NS, "machine_excavator")
                .definition(MachineObjModel.of("hbm:models/machines/mining_drill.obj", "hbm:textures/models/machines/mining_drill.png")
                        .scale(0.5).ty(0.0).yaw(90.0).guiScale(3.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -2.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_exposure_chamber")
                .definition(MachineObjModel.of("hbm:models/machines/exposure_chamber.obj", "hbm:textures/models/machines/exposure_chamber.png")
                        .scale(0.5).ty(0.0).yaw(90.0).guiScale(3.0).rawLegacyTransform().noCull()
                        .itemTranslate(1.5, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.5, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_fel")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/fel.obj", "hbm:textures/models/machines/fel.png").scale(0.28).ty(0.0)
                                .yaw(180))).register();
        Dejson.item(NS, "machine_fensu")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/fensu.obj", "hbm:textures/models/machines/fensus/fensu_light_blue.png")
                                .scale(0.21).ty(0.0).yaw(90.0))).register();
        Dejson.item(NS, "machine_flare")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/flare_stack.obj", "hbm:textures/models/machines/flare_stack.png")
                                .scale(0.12).ty(0.0).yaw(180))).register();
        Dejson.item(NS, "machine_fluidtank")
                .definition(MachineObjModel.of("hbm:models/fluidtank.obj", "hbm:textures/models/machines/tank.png").scale(0.75).ty(0.0)
                        .yaw(90.0).guiScale(3.5).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -2.0, 0.0).exclude("Tank")
                        .overlayParts("Tank").overlayTexture("hbm:textures/models/tank/tank_none.png")).oversizedInGui().register();
        Dejson.item(NS, "machine_fracking_tower")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/fracking_tower.obj", "hbm:textures/models/machines/fracking_tower.png")
                                .scale(0.15).ty(0.0).yaw(180))).register();
        Dejson.item(NS, "machine_fraction_tower")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/fraction_tower.obj", "hbm:textures/models/machines/fraction_tower.png")
                                .scale(0.35).ty(0.0).yaw(0))).register();
        Dejson.item(NS, "machine_furnace_combination")
                .definition(MachineObjModel.of("hbm:models/machines/combination_oven.obj", "hbm:textures/models/machines/combination_oven.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(3.25).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.5, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_gascent")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/centrifuge_gas.obj", "hbm:textures/models/machines/centrifuge_gas.png")
                                .scale(0.2625).ty(0.0).yaw(0))).register();
        Dejson.item(NS, "machine_hephaestus")
                .definition(MachineObjModel.of("hbm:models/machines/hephaestus.obj", "hbm:textures/models/machines/hephaestus.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(2.25).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -4.5, 0.0).overlayParts("Core")
                        .overlayTexture("hbm:textures/models/machines/cobblestone.png")).oversizedInGui().register();
        Dejson.item(NS, "machine_hydrotreater")
                .definition(MachineObjModel.of("hbm:models/machines/hydrotreater.obj", "hbm:textures/models/machines/hydrotreater.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(4.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -4.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_industrial_boiler")
                .definition(MachineObjModel.of("hbm:models/machines/industrial_boiler.obj", "hbm:textures/models/machines/industrial_boiler.png")
                        .scale(1.0).ty(0.0).yaw(90.0).guiScale(2.5).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -3.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_industrial_generator")
                .definition(MachineObjModel.of("hbm:models/machines/igen.obj", "hbm:textures/models/machines/igen.png").scale(0.75)
                        .ty(0.0).yaw(0.0).guiScale(4.0).guiYaw(90.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, -0.5)
                        .guiTranslate(0.0, -1.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_industrial_turbine")
                .definition(MachineObjModel.of("hbm:models/machines/industrial_turbine.obj", "hbm:textures/models/machines/industrial_turbine.png")
                        .scale(0.75).ty(0.0).yaw(90.0).guiScale(3.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, -0.375).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(1.0, 0.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_intake")
                .definition(MachineObjModel.of("hbm:models/machines/intake.obj", "hbm:textures/models/machines/intake.png").scale(1.0)
                        .ty(0.0).yaw(0.0).guiScale(5.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, 0.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_large_turbine")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/turbine.obj", "hbm:textures/models/machines/turbine.png").scale(0.2)
                                .ty(0.0).yaw(90)
                                .overlayTexture("hbm:textures/models/machines/turbofan_blades.png"))).register();
        Dejson.item(NS, "machine_liquefactor")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/liquefactor.obj", "hbm:textures/models/machines/liquefactor.png")
                                .scale(0.3).ty(0.0).yaw(0))).register();
        Dejson.item(NS, "machine_microwave")
                .definition(MachineObjModel.of("hbm:models/machines/microwave.obj", "hbm:textures/models/machines/microwave.png")
                        .scale(3.0).ty(0.0).yaw(0.0).guiScale(5.0).rawLegacyTransform()
                        .itemTranslate(-2.0, -2.0, 1.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -4.0, 4.0).exclude("plate_Cylinder")).oversizedInGui().register();
        Dejson.item(NS, "machine_mining_laser")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/mining_laser.obj", "hbm:textures/models/machines/mining_laser_base.png")
                                .scale(0.4).ty(0.5).yaw(180).rotateX(-90.0).rotateZ(-90.0)
                                .rotatePivot(-0.5, -0.5, 0.0).rotateParts("Laser").overlayParts("Pivot")
                                .overlayTexture("hbm:textures/models/machines/mining_laser_laser.png"))).register();
        Dejson.item(NS, "machine_minirtg")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/rtg.obj", "hbm:textures/models/machines/rtg_cell.png").scale(1.0)
                                .ty(0.0).yaw(0).noCull())).register();
        Dejson.item(NS, "machine_mixer")
                .definition(MachineObjModel.of("hbm:models/machines/mixer.obj", "hbm:textures/models/machines/mixer.png").scale(1.0)
                        .ty(0.0).yaw(180.0).guiScale(5.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -5.0, 0.0).exclude("Fluid")).oversizedInGui().register();
        Dejson.item(NS, "machine_ore_slopper")
                .definition(MachineObjModel.of("hbm:models/machines/ore_slopper.obj", "hbm:textures/models/machines/ore_slopper.png")
                        .scale(0.5).ty(0.0).yaw(-90.0).guiScale(3.75).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -3.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_precass")
                .definition(MachineObjModel.of("hbm:models/machines/assembly_machine.obj", "hbm:textures/models/machines/precass.png")
                        .scale(0.75).ty(0.0).yaw(90.0).guiScale(4.5).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -2.75, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_press")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/press_body.obj", "hbm:textures/models/machines/press_body.png").scale(0.48)
                                .ty(0.0).yaw(180))).register();
        Dejson.item(NS, "machine_puf6_tank")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/tank.obj", "hbm:textures/models/machines/puf6tank.png").scale(0.525).ty(0.0)
                                .yaw(270))).register();
        Dejson.item(NS, "machine_pump_electric")
                .definition(MachineObjModel.of("hbm:models/machines/pump.obj", "hbm:textures/models/machines/pump_electric.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(2.5).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -3.0, 0.0).animation("pump")).oversizedInGui().register();
    }

    private static void part6() {
        Dejson.item(NS, "machine_pump_steam")
                .definition(MachineObjModel.of("hbm:models/machines/pump.obj", "hbm:textures/models/machines/pump_steam.png").scale(1.0)
                        .ty(0.0).yaw(0.0).guiScale(2.5).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -3.0, 0.0).animation("pump")).oversizedInGui().register();
        Dejson.item(NS, "machine_pumpjack")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/pumpjack.obj", "hbm:textures/models/machines/pumpjack.png")
                                .scale(0.22).ty(0.0).yaw(90))).register();
        Dejson.item(NS, "machine_purex")
                .definition(MachineObjModel.of("hbm:models/machines/purex.obj", "hbm:textures/models/machines/purex.png").scale(0.75)
                        .ty(0.0).yaw(0.0).guiScale(2.5).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -2.5, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_pyrooven")
                .definition(MachineObjModel.of("hbm:models/machines/pyrooven.obj", "hbm:textures/models/machines/pyrooven.png").scale(0.5)
                        .ty(0.0).yaw(90.0).guiScale(3.5).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -1.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_radgen")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/radgen.obj", "hbm:textures/models/machines/rad_gen_body.png")
                                .scale(0.171428571).ty(0.0).yaw(0).noCull().exclude("Glass"))).register();
        Dejson.item(NS, "machine_radiolysis")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/radiolysis.obj", "hbm:textures/models/machines/radiolysis.png")
                                .scale(0.4).ty(0.0).yaw(0).noCull())).register();
        Dejson.item(NS, "machine_reactor")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/reactors/breeder.obj", "hbm:textures/models/machines/breeder.png").scale(0.4)
                                .ty(0.0).yaw(90).noCull())).register();
        Dejson.item(NS, "machine_reactor_small")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/reactors/reactor_small_base.obj", "hbm:textures/models/reactors/reactor_small_base.png")
                                .scale(0.4).ty(0.0).yaw(180).noCull())).register();
        Dejson.item(NS, "machine_refinery")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/refinery.obj", "hbm:textures/models/machines/refinery.png")
                                .scale(0.15).ty(0.0).yaw(180))).register();
        Dejson.item(NS, "machine_rotary_furnace")
                .definition(MachineObjModel.of("hbm:models/machines/rotary_furnace.obj", "hbm:textures/models/machines/rotary_furnace.png")
                        .scale(0.625).ty(0.0).yaw(90.0).guiScale(3.5).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -2.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_rtg_grey")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/rtg.obj", "hbm:textures/models/machines/rtg.png").scale(1.0).ty(0.0)
                                .yaw(0).noCull())).register();
        Dejson.item(NS, "machine_satlink")
                .definition(MachineObjModel.of("hbm:models/machines/satlink.obj", "hbm:textures/models/machines/satlink.png").scale(0.5)
                        .ty(0.0).yaw(0.0).guiScale(3.5).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -5.0, 0.0).parts("Base")
                        .passes(MachineObjModel.pass("hbm:textures/models/machines/satlink.png").parts("Rotor").rotateStep(0.0, 15.0, 0.0),
                                MachineObjModel.pass("hbm:textures/models/machines/satlink.png").parts("Dish").rotateStep(0.0, 15.0, 0.0).translateStep(0.0, 7.375, 0.0).rotateStep(0.0, 0.0, -45.0).translateStep(0.0, -7.375, 0.0))).oversizedInGui().register();
        Dejson.item(NS, "machine_sawmill")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("1", MachineObjModel.of("hbm:models/machines/sawmill.obj", "hbm:textures/models/machines/sawmill.png").scale(1.0)
                                .ty(0.0).yaw(90.0).guiScale(3.25).rawLegacyTransform()
                                .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                                .guiTranslate(0.0, -1.5, 0.0).exclude("Blade"))
                        .fallback(MachineObjModel.of("hbm:models/machines/sawmill.obj", "hbm:textures/models/machines/sawmill.png").scale(1.0)
                                .ty(0.0).yaw(90.0).guiScale(3.25).rawLegacyTransform()
                                .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                                .guiTranslate(0.0, -1.5, 0.0))).oversizedInGui().register();
        Dejson.item(NS, "machine_silex")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/silex.obj", "hbm:textures/models/machines/silex.png").scale(0.32)
                                .ty(0.0).yaw(180))).register();
        Dejson.item(NS, "machine_solar_boiler")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/solar_boiler.obj", "hbm:textures/models/machines/solar_boiler.png")
                                .scale(0.2625).ty(0.0).yaw(0).noCull()
                                .exclude("Overglow_1", "Overglow_2", "Overglow_3"))).register();
        Dejson.item(NS, "machine_soldering_station")
                .definition(MachineObjModel.of("hbm:models/soldering_station.obj", "hbm:textures/models/machines/soldering_station.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(5.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_solidifier")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/solidifier.obj", "hbm:textures/models/machines/solidifier.png")
                                .scale(0.3).ty(0.0).yaw(0))).register();
        Dejson.item(NS, "machine_steam_engine")
                .definition(MachineObjModel.of("hbm:models/machines/steam_engine.obj", "hbm:textures/models/machines/steam_engine.png")
                        .scale(1.0).ty(0.0).yaw(90.0).guiScale(2.0).guiYaw(-90.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.5, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_stirling")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("1", MachineObjModel.of("hbm:models/machines/stirling.obj", "hbm:textures/models/machines/stirling.png").scale(1.0)
                                .ty(0.0).yaw(90.0).guiScale(3.25).rawLegacyTransform().spin()
                                .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                                .guiTranslate(0.0, -1.5, 0.0).spinPivot(0.0, 1.375, 0.0).exclude("Cog")
                                .spinParts("Stirling"))
                        .fallback(MachineObjModel.of("hbm:models/machines/stirling.obj", "hbm:textures/models/machines/stirling.png").scale(1.0)
                                .ty(0.0).yaw(90.0).guiScale(3.25).rawLegacyTransform().spin()
                                .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                                .guiTranslate(0.0, -1.5, 0.0).spinPivot(0.0, 1.375, 0.0).spinParts("Stirling"))).oversizedInGui().register();
        Dejson.item(NS, "machine_stirling_creative")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("1", MachineObjModel.of("hbm:models/machines/stirling.obj", "hbm:textures/models/machines/stirling_creative.png")
                                .scale(1.0).ty(0.0).yaw(90.0).guiScale(3.25).rawLegacyTransform().spin()
                                .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                                .guiTranslate(0.0, -1.5, 0.0).spinPivot(0.0, 1.375, 0.0).exclude("Cog")
                                .spinParts("Stirling"))
                        .fallback(MachineObjModel.of("hbm:models/machines/stirling.obj", "hbm:textures/models/machines/stirling_creative.png")
                                .scale(1.0).ty(0.0).yaw(90.0).guiScale(3.25).rawLegacyTransform().spin()
                                .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                                .guiTranslate(0.0, -1.5, 0.0).spinPivot(0.0, 1.375, 0.0).spinParts("Stirling"))).oversizedInGui().register();
        Dejson.item(NS, "machine_stirling_steel")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("1", MachineObjModel.of("hbm:models/machines/stirling.obj", "hbm:textures/models/machines/stirling_steel.png")
                                .scale(1.0).ty(0.0).yaw(90.0).guiScale(3.25).rawLegacyTransform().spin()
                                .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                                .guiTranslate(0.0, -1.5, 0.0).spinPivot(0.0, 1.375, 0.0).exclude("Cog")
                                .spinParts("Stirling"))
                        .fallback(MachineObjModel.of("hbm:models/machines/stirling.obj", "hbm:textures/models/machines/stirling_steel.png")
                                .scale(1.0).ty(0.0).yaw(90.0).guiScale(3.25).rawLegacyTransform().spin()
                                .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                                .guiTranslate(0.0, -1.5, 0.0).spinPivot(0.0, 1.375, 0.0).spinParts("Stirling"))).oversizedInGui().register();
        Dejson.item(NS, "machine_storage_drum")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/drum.obj", "hbm:textures/models/machines/drum_gray.png")
                                .scale(1.5272727).ty(0.0).yaw(0))).register();
        Dejson.item(NS, "machine_strand_caster")
                .definition(MachineObjModel.of("hbm:models/machines/strand_caster.obj", "hbm:textures/models/machines/strand_caster.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(2.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(2.0, 0.0, 2.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_thresher")
                .definition(MachineObjModel.of("hbm:models/machines/thresher.obj", "hbm:textures/models/machines/thresher.png").scale(0.5)
                        .ty(0.0).yaw(-90.0).guiScale(4.5).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, 4.0, -8.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_turbine_gas")
                .definition(MachineObjModel.of("hbm:models/machines/turbinegas.obj", "hbm:textures/models/machines/turbinegas.png")
                        .scale(0.75).ty(0.0).yaw(90.0).guiScale(2.5).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.0, 1.5)).oversizedInGui().register();
        Dejson.item(NS, "machine_turbofan")
                .definition(MachineObjModel.of("hbm:models/machines/turbofan.obj", "hbm:textures/models/machines/turbofan.png").scale(1.0)
                        .ty(0.0).yaw(0.0).guiScale(2.25).guiYaw(90.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, 0.0, 0.0).overlayParts("Afterburner")
                        .overlayTexture("hbm:textures/models/machines/turbofan_back.png")).oversizedInGui().register();
        Dejson.item(NS, "machine_uf6_tank")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/tank.obj", "hbm:textures/models/machines/uf6tank.png").scale(0.525).ty(0.0)
                                .yaw(270))).register();
        Dejson.item(NS, "machine_vacuum_distill")
                .definition(MachineObjModel.of("hbm:models/machines/vacuum_distill.obj", "hbm:textures/models/machines/vacuum_distill.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(3.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -4.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_well")
                .definition(MachineObjModel.of("hbm:models/machines/derrick.obj", "hbm:textures/models/machines/derrick.png").scale(0.5)
                        .ty(0.0).yaw(90.0).guiScale(3.0).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -4.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "machine_wood_burner")
                .definition(MachineObjModel.of("hbm:models/machines/wood_burner.obj", "hbm:textures/models/machines/wood_burner.png")
                        .scale(1.0).ty(0.0).yaw(90.0).guiScale(3.5).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -4.0, 0.0)).oversizedInGui().register();
    }

    private static void part7() {
        Dejson.item(NS, "machine_zirnox")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/zirnox.obj", "hbm:textures/models/machines/zirnox.png").scale(0.15).ty(0.0)
                                .yaw(90))).register();
        Dejson.item(NS, "mine_ap")
                .definition(MachineObjModel.of("hbm:models/bombs/ap_mine.obj", "hbm:textures/models/bombs/mine_ap_grass.png").scale(0.375)
                        .ty(0.0).yaw(180.0).guiScale(6.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, 0.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "mine_fat")
                .definition(MachineObjModel.of("hbm:models/bombs/mine_fat.obj", "hbm:textures/models/bombs/mine_fat.png").scale(0.25)
                        .ty(0.0).yaw(180.0).guiScale(5.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, 0.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "mine_he")
                .definition(MachineObjModel.of("hbm:models/bombs/marelet.obj", "hbm:textures/models/bombs/mine_marelet.png").scale(1.0)
                        .ty(0.0).yaw(90.0).guiScale(3.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, 0.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "mine_naval")
                .definition(MachineObjModel.of("hbm:models/bombs/naval_mine.obj", "hbm:textures/models/bombs/mine_naval.png").scale(1.0)
                        .ty(0.0).yaw(180.0).guiScale(2.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, 0.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "mine_shrap")
                .definition(MachineObjModel.of("hbm:models/bombs/ap_mine.obj", "hbm:textures/models/bombs/mine_shrapnel.png").scale(0.375)
                        .ty(0.0).yaw(180.0).guiScale(6.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, 0.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "missile_anti_ballistic")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_abm.obj", "hbm:textures/models/missiles/missile_abm.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -20.25, 0.0), 2.25)).oversizedInGui().register();
        Dejson.item(NS, "missile_bhole")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_micro.obj", "hbm:textures/models/missiles/missile_micro_bhole.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -19.6875, 0.0), 3.75)).oversizedInGui().register();
        Dejson.item(NS, "missile_burst")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_huge.obj", "hbm:textures/models/missiles/missile_huge.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -18.75, 0.0), 1.25)).oversizedInGui().register();
        Dejson.item(NS, "missile_buster")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_v2.obj", "hbm:textures/models/missiles/missile_v2_bu.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -18.75, 0.0), 2.5)).oversizedInGui().register();
        Dejson.item(NS, "missile_buster_strong")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_strong.obj", "hbm:textures/models/missiles/missile_strong_bu.png")
                        .scale(1.5).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -19.0, 0.0), 2.0)).oversizedInGui().register();
        Dejson.item(NS, "missile_cluster")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_v2.obj", "hbm:textures/models/missiles/missile_v2_cl.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -18.75, 0.0), 2.5)).oversizedInGui().register();
        Dejson.item(NS, "missile_cluster_strong")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_strong.obj", "hbm:textures/models/missiles/missile_strong_cl.png")
                        .scale(1.5).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -19.0, 0.0), 2.0)).oversizedInGui().register();
        Dejson.item(NS, "missile_decoy")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_v2.obj", "hbm:textures/models/missiles/missile_v2_decoy.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -18.75, 0.0), 2.5)).oversizedInGui().register();
        Dejson.item(NS, "missile_doomsday")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_atlas.obj", "hbm:textures/models/missiles/missile_atlas_doomsday.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -19.9375, 0.0), 1.375)).oversizedInGui().register();
        Dejson.item(NS, "missile_doomsday_rusted")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_atlas.obj", "hbm:textures/models/missiles/missile_atlas_doomsday_weathered.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -19.9375, 0.0), 1.375)).oversizedInGui().register();
        Dejson.item(NS, "missile_drill")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_huge.obj", "hbm:textures/models/missiles/missile_huge_bu.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -18.75, 0.0), 1.25)).oversizedInGui().register();
        Dejson.item(NS, "missile_emp")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_micro.obj", "hbm:textures/models/missiles/missile_micro_emp.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -19.6875, 0.0), 3.75)).oversizedInGui().register();
        Dejson.item(NS, "missile_emp_strong")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_strong.obj", "hbm:textures/models/missiles/missile_strong_emp.png")
                        .scale(1.5).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -19.0, 0.0), 2.0)).oversizedInGui().register();
        Dejson.item(NS, "missile_generic")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_v2.obj", "hbm:textures/models/missiles/missile_v2.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -18.75, 0.0), 2.5)).oversizedInGui().register();
        Dejson.item(NS, "missile_incendiary")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_v2.obj", "hbm:textures/models/missiles/missile_v2_inc.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -18.75, 0.0), 2.5)).oversizedInGui().register();
        Dejson.item(NS, "missile_incendiary_strong")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_strong.obj", "hbm:textures/models/missiles/missile_strong_inc.png")
                        .scale(1.5).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -19.0, 0.0), 2.0)).oversizedInGui().register();
        Dejson.item(NS, "missile_inferno")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_huge.obj", "hbm:textures/models/missiles/missile_huge_inc.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -18.75, 0.0), 1.25)).oversizedInGui().register();
        Dejson.item(NS, "missile_micro")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_micro.obj", "hbm:textures/models/missiles/missile_micro.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -19.6875, 0.0), 3.75)).oversizedInGui().register();
        Dejson.item(NS, "missile_n2")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_n2_body.obj", "hbm:textures/models/missiles/missile_n2.png")
                        .scale(1.5).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -19.9375, 0.0), 1.375)).oversizedInGui().register();
        Dejson.item(NS, "missile_nuclear")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_atlas.obj", "hbm:textures/models/missiles/missile_atlas_nuclear.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -19.9375, 0.0), 1.375)).oversizedInGui().register();
        Dejson.item(NS, "missile_nuclear_cluster")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_atlas.obj", "hbm:textures/models/missiles/missile_atlas_thermo.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -19.9375, 0.0), 1.375)).oversizedInGui().register();
        Dejson.item(NS, "missile_rain")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_huge.obj", "hbm:textures/models/missiles/missile_huge_cl.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -18.75, 0.0), 1.25)).oversizedInGui().register();
        Dejson.item(NS, "missile_schrabidium")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_micro.obj", "hbm:textures/models/missiles/missile_micro_schrab.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -19.6875, 0.0), 3.75)).oversizedInGui().register();
        Dejson.item(NS, "missile_shuttle")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_shuttle.obj", "hbm:textures/models/missiles/missile_shuttle.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -17.5, 0.0), 1.25)).oversizedInGui().register();
    }

    private static void part8() {
        Dejson.item(NS, "missile_stealth")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_stealth.obj", "hbm:textures/models/missiles/missile_stealth.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -19.6875, 0.0), 1.75)).oversizedInGui().register();
        Dejson.item(NS, "missile_strong")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_strong.obj", "hbm:textures/models/missiles/missile_strong.png")
                        .scale(1.5).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -19.0, 0.0), 2.0)).oversizedInGui().register();
        Dejson.item(NS, "missile_taint")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_micro.obj", "hbm:textures/models/missiles/missile_micro_taint.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -19.6875, 0.0), 3.75)).oversizedInGui().register();
        Dejson.item(NS, "missile_volcano")
                .definition(MachineObjModel.of("hbm:models/missiles/missile_atlas.obj", "hbm:textures/models/missiles/missile_atlas_tectonic.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiSpinSpeed(0.06666667).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0).frame("missile")
                        .guiContext(MachineObjModel.xyz(0.0, 0.0, 135.0), MachineObjModel.xyz(0.0, -19.9375, 0.0), 1.375)).oversizedInGui().register();
        Dejson.item(NS, "nuke_boy")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/bombs/lilboy.obj", "hbm:textures/models/bombs/lilboy.png").scale(0.50854)
                                .ty(0.0).yaw(0.0).noCull())).register();
        Dejson.item(NS, "nuke_custom")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/bombs/lilboy.obj", "hbm:textures/models/bombs/customnuke.png").scale(0.50854)
                                .ty(0.0).yaw(0.0).noCull())).register();
        Dejson.item(NS, "nuke_fleija")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/bombs/fleija.obj", "hbm:textures/models/bombs/fleija.png").scale(1.0).ty(0.0)
                                .yaw(90.0).noCull())).register();
        Dejson.item(NS, "nuke_fstbmb")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/bombs/fstbmb.obj", "hbm:textures/models/bombs/fstbmb.png").scale(1.0).ty(0.0)
                                .yaw(90.0).noCull())).register();
        Dejson.item(NS, "nuke_gadget")
                .definition(MachineObjModel.of("hbm:models/bombs/gadget.obj", "hbm:textures/models/bombs/gadget.png").scale(1.0).ty(0.0)
                        .yaw(-90.0).guiScale(5.0).rawLegacyTransform().noCull().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -3.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "nuke_man")
                .definition(MachineObjModel.of("hbm:models/bombs/fatman.obj", "hbm:textures/models/bombs/fatman.png").scale(1.0).ty(0.0)
                        .yaw(180.0).guiScale(5.0).rawLegacyTransform().itemTranslate(0.75, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -2.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "nuke_mike")
                .definition(MachineObjModel.of("hbm:models/bombs/ivymike.obj", "hbm:textures/models/bombs/ivymike.png").scale(1.0).ty(0.0)
                        .yaw(0.0).guiScale(2.25).rawLegacyTransform().noCull().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -5.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "nuke_n2")
                .definition(MachineObjModel.of("hbm:models/bombs/n2.obj", "hbm:textures/models/bombs/n2.png").scale(1.0).ty(0.0).yaw(0.0)
                        .guiScale(2.25).rawLegacyTransform().noCull().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -5.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "nuke_prototype")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/bombs/prototype.obj", "hbm:textures/models/bombs/prototype.png").scale(1.0)
                                .ty(0.0).yaw(90.0).noCull())).register();
        Dejson.item(NS, "nuke_solinium")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/bombs/ufp.obj", "hbm:textures/models/bombs/ufp.png").scale(1.0).ty(0.0)
                                .yaw(90.0).noCull())).register();
        Dejson.item(NS, "nuke_tsar")
                .definition(MachineObjModel.of("hbm:models/bombs/tsar.obj", "hbm:textures/models/bombs/tsar.png").scale(1.0).ty(0.0)
                        .yaw(0.0).guiScale(2.25).rawLegacyTransform().noCull().itemTranslate(1.5, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, 0.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "pa_beamline")
                .definition(MachineObjModel.of("hbm:models/particleaccelerator/beamline.obj", "hbm:textures/models/particleaccelerator/beamline.png")
                        .scale(1.0).ty(0.0).yaw(90.0).guiScale(4.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, 0.0, 0.0).parts("Beamline")).oversizedInGui().register();
        Dejson.item(NS, "pa_detector")
                .definition(MachineObjModel.of("hbm:models/particleaccelerator/detector.obj", "hbm:textures/models/particleaccelerator/detector.png")
                        .scale(0.5).ty(0.0).yaw(90.0).guiScale(3.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "pa_dipole")
                .definition(MachineObjModel.of("hbm:models/particleaccelerator/dipole.obj", "hbm:textures/models/particleaccelerator/dipole.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(3.5).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -3.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "pa_quadrupole")
                .definition(MachineObjModel.of("hbm:models/particleaccelerator/quadrupole.obj", "hbm:textures/models/particleaccelerator/quadrupole.png")
                        .scale(1.0).ty(0.0).yaw(90.0).guiScale(4.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -3.5, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "pa_rfc")
                .definition(MachineObjModel.of("hbm:models/particleaccelerator/rfc.obj", "hbm:textures/models/particleaccelerator/rfc.png")
                        .scale(0.5).ty(0.0).yaw(90.0).guiScale(4.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "pa_source")
                .definition(MachineObjModel.of("hbm:models/particleaccelerator/source.obj", "hbm:textures/models/particleaccelerator/source.png")
                        .scale(0.5).ty(0.0).yaw(90.0).guiScale(4.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "pile_device")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", MachineObjModel.of("hbm:models/pile/pile_control.obj", "hbm:textures/models/pile/pile_control.png").scale(2.0)
                                .ty(0.0).yaw(0.0).guiScale(5.0).rawLegacyTransform()
                                .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                                .guiTranslate(0.0, -3.5, 0.0))
                        .when("1", MachineObjModel.of("hbm:models/pile/pile_loader.obj", "hbm:textures/models/pile/pile_loader.png").scale(2.0)
                                .ty(0.0).yaw(0.0).guiScale(5.0).rawLegacyTransform()
                                .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                                .guiTranslate(0.0, -3.5, 0.0))
                        .when("2", MachineObjModel.of("hbm:models/pile/pile_vent.obj", "hbm:textures/models/pile/pile_vent.png").scale(2.0)
                                .ty(0.0).yaw(0.0).guiScale(5.0).rawLegacyTransform()
                                .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                                .guiTranslate(0.0, -3.5, 0.0))
                        .fallback(MachineObjModel.of("hbm:models/pile/pile_control.obj", "hbm:textures/models/pile/pile_control.png").scale(2.0)
                                .ty(0.0).yaw(0.0).guiScale(5.0).rawLegacyTransform()
                                .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                                .guiTranslate(0.0, -3.5, 0.0))).oversizedInGui().register();
        Dejson.item(NS, "pink_barrel")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/barrel.obj", "hbm:textures/blocks/barrel_pink.png").scale(0.5).ty(-0.5)
                                .yaw(0).exclude("Connector"))).register();
        Dejson.item(NS, "piston_inserter")
                .definition(MachineObjModel.of("hbm:models/machines/piston_inserter.obj", "hbm:textures/models/machines/piston_inserter.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(5.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -2.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "pole_satellite_receiver")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/deco/pole_satellite_receiver.obj", "hbm:textures/models/deco/polesatellitereceiver.png")
                                .scale(0.0625).ty(0.0).yaw(180).noCull())).register();
        Dejson.item(NS, "pole_top")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/antenna_top.obj", "hbm:textures/blocks/deco_pole_top.png").scale(0.75)
                                .ty(-0.5).yaw(0).noCull())).register();
        Dejson.item(NS, "qe_containment")
                .definition(MachineObjModel.of("hbm:models/pheodoors/containment_door.obj", "hbm:textures/models/pheodoors/containment_door.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(3.8).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -3.5, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "qe_sliding_door")
                .definition(MachineObjModel.of("hbm:models/pheodoors/sliding_door.obj", "hbm:textures/models/pheodoors/sliding_door.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(5.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -3.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "radio_autocal")
                .definition(MachineObjModel.of("hbm:models/machines/autocal.obj", "hbm:textures/models/machines/autocal.png").scale(1.0)
                        .ty(0.0).yaw(0.0).guiScale(6.25).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -4.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "radio_telex")
                .definition(MachineObjModel.of("hbm:models/machines/telex.obj", "hbm:textures/models/machines/telex.png").scale(1.0)
                        .ty(0.0).yaw(0.0).guiScale(6.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, -0.5).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -2.0, 0.0)).oversizedInGui().register();
    }

    private static void part9() {
        Dejson.item(NS, "rbmk_absorber")
                .models(Models.parented("hbm:block/rbmk/rbmk_absorber_column_none", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        ItemModels.custom("hbm:rbmk_block_column").with("side", "hbm:blocks/rbmk/rbmk_absorber_side").with("top", "hbm:blocks/rbmk/rbmk_absorber_top").with("scale", 0.35).with("yaw", 0).with("lift", -0.675))).register();
        Dejson.item(NS, "rbmk_blank")
                .models(Models.parented("hbm:block/rbmk/rbmk_blank_column_none", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        ItemModels.custom("hbm:rbmk_block_column").with("side", "hbm:blocks/rbmk/rbmk_blank_side").with("top", "hbm:blocks/rbmk/rbmk_blank_top").with("scale", 0.35).with("yaw", 0).with("lift", -0.675))).register();
        Dejson.item(NS, "rbmk_boiler")
                .models(Models.parented("hbm:block/rbmk/rbmk_boiler_column_none", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        ItemModels.custom("hbm:rbmk_block_column").with("side", "hbm:blocks/rbmk/rbmk_boiler_side").with("top", "hbm:blocks/rbmk/rbmk_boiler_top").with("scale", 0.35).with("yaw", 0).with("lift", -0.75))).register();
        Dejson.item(NS, "rbmk_console")
                .models(Models.parented("hbm:block/machine_dummy_empty", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/rbmk/rbmk_console.obj", "hbm:textures/models/machines/rbmk_console.png")
                                .scale(0.25).ty(0.0).yaw(0))).register();
        Dejson.item(NS, "rbmk_control")
                .models(Models.parented("hbm:block/rbmk/rbmk_control_column_none", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        ItemModels.custom("hbm:rbmk_block_column").with("side", "hbm:blocks/rbmk/rbmk_control_side").with("top", "hbm:blocks/rbmk/rbmk_control_top").with("scale", 0.35).with("yaw", 0).with("lift", -0.675).with("pipe_side", "hbm:blocks/rbmk/rbmk_control_pipe_side").with("pipe_top", "hbm:blocks/rbmk/rbmk_control_pipe_top").with("lid_texture", "hbm:blocks/rbmk/rbmk_control"))).register();
        Dejson.item(NS, "rbmk_control_auto")
                .models(Models.parented("hbm:block/rbmk/rbmk_control_auto_column_none", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        ItemModels.custom("hbm:rbmk_block_column").with("side", "hbm:blocks/rbmk/rbmk_control_auto_side").with("top", "hbm:blocks/rbmk/rbmk_control_auto_top").with("scale", 0.35).with("yaw", 0).with("lift", -0.675).with("pipe_side", "hbm:blocks/rbmk/rbmk_control_auto_pipe_side").with("pipe_top", "hbm:blocks/rbmk/rbmk_control_auto_pipe_top").with("lid_texture", "hbm:blocks/rbmk/rbmk_control_auto"))).register();
        Dejson.item(NS, "rbmk_control_mod")
                .models(Models.parented("hbm:block/rbmk/rbmk_control_mod_column_none", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        ItemModels.custom("hbm:rbmk_block_column").with("side", "hbm:blocks/rbmk/rbmk_control_mod_side").with("top", "hbm:blocks/rbmk/rbmk_control_mod_top").with("scale", 0.35).with("yaw", 0).with("lift", -0.675).with("pipe_side", "hbm:blocks/rbmk/rbmk_control_mod_pipe_side").with("pipe_top", "hbm:blocks/rbmk/rbmk_control_mod_pipe_top").with("lid_texture", "hbm:blocks/rbmk/rbmk_control_mod"))).register();
        Dejson.item(NS, "rbmk_control_reasim")
                .models(Models.parented("hbm:block/rbmk/rbmk_control_reasim_column_none", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        ItemModels.custom("hbm:rbmk_block_column").with("side", "hbm:blocks/rbmk/rbmk_control_reasim_side").with("top", "hbm:blocks/rbmk/rbmk_control_reasim_top").with("scale", 0.35).with("yaw", 0).with("lift", -0.675).with("pipe_side", "hbm:blocks/rbmk/rbmk_control_reasim_pipe_side").with("pipe_top", "hbm:blocks/rbmk/rbmk_control_reasim_pipe_top").with("lid_texture", "hbm:blocks/rbmk/rbmk_control_reasim_top"))).register();
        Dejson.item(NS, "rbmk_control_reasim_auto")
                .models(Models.parented("hbm:block/rbmk/rbmk_control_reasim_auto_column_none", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        ItemModels.custom("hbm:rbmk_block_column").with("side", "hbm:blocks/rbmk/rbmk_control_reasim_auto_side").with("top", "hbm:blocks/rbmk/rbmk_control_reasim_auto_top").with("scale", 0.35).with("yaw", 0).with("lift", -0.675).with("pipe_side", "hbm:blocks/rbmk/rbmk_control_reasim_auto_pipe_side").with("pipe_top", "hbm:blocks/rbmk/rbmk_control_reasim_auto_pipe_top").with("lid_texture", "hbm:blocks/rbmk/rbmk_control_reasim_auto_top"))).register();
        Dejson.item(NS, "rbmk_cooler")
                .models(Models.parented("hbm:block/rbmk/rbmk_cooler_column_none", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        ItemModels.custom("hbm:rbmk_block_column").with("side", "hbm:blocks/rbmk/rbmk_cooler_side").with("top", "hbm:blocks/rbmk/rbmk_cooler_top").with("scale", 0.35).with("yaw", 0).with("lift", -0.675))).register();
        Dejson.item(NS, "rbmk_crane_console")
                .models(Models.parented("hbm:block/machine_dummy_empty", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/rbmk/crane_console.obj", "hbm:textures/models/machines/rbmk_crane_console.png")
                                .scale(0.35).ty(0.0).yaw(0).exclude("Shotgun", "Shotgun1", "MiniNuke"))).register();
        Dejson.item(NS, "rbmk_debris")
                .models(Models.parented("hbm:block/rbmk_debris", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/rbmk/pribris.obj", "hbm:textures/blocks/rbmk/rbmk_debris.png").scale(0.28)
                                .ty(0.0).yaw(90))).register();
        Dejson.item(NS, "rbmk_debris_burning")
                .models(Models.parented("hbm:block/rbmk_debris_burning", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/rbmk/pribris_burning.obj", "hbm:textures/blocks/rbmk/rbmk_debris_burning.png")
                                .scale(0.28).ty(0.0).yaw(90))).register();
        Dejson.item(NS, "rbmk_debris_digamma")
                .models(Models.parented("hbm:block/rbmk_debris_digamma", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/rbmk/pribris_digamma.obj", "hbm:textures/blocks/rbmk/rbmk_debris_digamma.png")
                                .scale(0.28).ty(0.0).yaw(90))).register();
        Dejson.item(NS, "rbmk_debris_radiating")
                .models(Models.parented("hbm:block/rbmk_debris_radiating", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/rbmk/pribris_radiating.obj", "hbm:textures/blocks/rbmk/rbmk_debris_radiating.png")
                                .scale(0.28).ty(0.0).yaw(90))).register();
        Dejson.item(NS, "rbmk_fuel_rod")
                .models(Models.parented("hbm:block/rbmk/rbmk_element_column_none", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        ItemModels.custom("hbm:rbmk_fuel_column").with("side", "hbm:blocks/rbmk/rbmk_element_side").with("top", "hbm:blocks/rbmk/rbmk_element_top").with("inner", "hbm:blocks/rbmk/rbmk_element_inner").with("fuel", "hbm:blocks/rbmk/rbmk_element_fuel").with("scale", 0.35).with("yaw", 0).with("lift", -0.675))).register();
        Dejson.item(NS, "rbmk_fuel_rod_moderated")
                .models(Models.parented("hbm:block/rbmk/rbmk_element_mod_column_none", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        ItemModels.custom("hbm:rbmk_fuel_column").with("side", "hbm:blocks/rbmk/rbmk_element_mod_side").with("top", "hbm:blocks/rbmk/rbmk_element_mod_top").with("inner", "hbm:blocks/rbmk/rbmk_element_mod_inner").with("fuel", "hbm:blocks/rbmk/rbmk_element_mod_fuel").with("scale", 0.35).with("yaw", 0).with("lift", -0.675))).register();
        Dejson.item(NS, "rbmk_fuel_rod_reasim")
                .models(Models.parented("hbm:block/rbmk/rbmk_element_reasim_column_none", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        ItemModels.custom("hbm:rbmk_fuel_column").with("side", "hbm:blocks/rbmk/rbmk_element_reasim_side").with("top", "hbm:blocks/rbmk/rbmk_element_reasim_top").with("inner", "hbm:blocks/rbmk/rbmk_element_reasim_inner").with("fuel", "hbm:blocks/rbmk/rbmk_element_reasim_fuel").with("scale", 0.35).with("yaw", 0).with("lift", -0.675))).register();
        Dejson.item(NS, "rbmk_fuel_rod_reasim_moderated")
                .models(Models.parented("hbm:block/rbmk/rbmk_element_reasim_mod_column_none", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        ItemModels.custom("hbm:rbmk_fuel_column").with("side", "hbm:blocks/rbmk/rbmk_element_reasim_mod_side").with("top", "hbm:blocks/rbmk/rbmk_element_reasim_mod_top").with("inner", "hbm:blocks/rbmk/rbmk_element_reasim_mod_inner").with("fuel", "hbm:blocks/rbmk/rbmk_element_reasim_mod_fuel").with("scale", 0.35).with("yaw", 0).with("lift", -0.675))).register();
        Dejson.item(NS, "rbmk_gauge")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/rbmk/gauge.obj", "hbm:textures/models/machines/rbmk_gauge.png").scale(1.0)
                                .ty(0.0).yaw(0.0).rawLegacyTransform().animation("rbmk_gauge")
                                .partTextures("Panel", "hbm:textures/blocks/rbmk/rbmk_display.png", "Needle", "hbm:textures/models/misc/white.png"))).register();
        Dejson.item(NS, "rbmk_heater")
                .models(Models.parented("hbm:block/rbmk/rbmk_heater_column_none", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        ItemModels.custom("hbm:rbmk_block_column").with("side", "hbm:blocks/rbmk/rbmk_heater_side").with("top", "hbm:blocks/rbmk/rbmk_heater_top").with("scale", 0.35).with("yaw", 0).with("lift", -0.675).with("pipe_side", "hbm:blocks/rbmk/rbmk_heater_pipe_side").with("pipe_top", "hbm:blocks/rbmk/rbmk_heater_pipe_top"))).register();
        Dejson.item(NS, "rbmk_moderator")
                .models(Models.parented("hbm:block/rbmk/rbmk_moderator_column_none", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        ItemModels.custom("hbm:rbmk_block_column").with("side", "hbm:blocks/rbmk/rbmk_moderator_side").with("top", "hbm:blocks/rbmk/rbmk_moderator_top").with("scale", 0.35).with("yaw", 0).with("lift", -0.675))).register();
        Dejson.item(NS, "rbmk_outgasser")
                .models(Models.parented("hbm:block/rbmk/rbmk_outgasser_column_none", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        ItemModels.custom("hbm:rbmk_block_column").with("side", "hbm:blocks/rbmk/rbmk_outgasser_side").with("top", "hbm:blocks/rbmk/rbmk_outgasser_top").with("scale", 0.35).with("yaw", 0).with("lift", -0.675))).register();
        Dejson.item(NS, "rbmk_pellet_balefire")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_balefire_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_balefire_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_balefire_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_balefire_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_balefire_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_balefire_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_balefire_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_balefire_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_balefire_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_balefire_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_balefire_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_balefire_gold")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_balefire_gold_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_balefire_gold_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_balefire_gold_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_balefire_gold_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_balefire_gold_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_balefire_gold_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_balefire_gold_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_balefire_gold_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_balefire_gold_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_balefire_gold_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_balefire_gold_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_drx")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_drx_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_drx_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_drx_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_drx_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_drx_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_drx_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_drx_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_drx_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_drx_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_drx_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_drx_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_flashlead")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_flashlead_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_flashlead_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_flashlead_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_flashlead_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_flashlead_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_flashlead_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_flashlead_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_flashlead_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_flashlead_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_flashlead_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_flashlead_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_hea241")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_hea241_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_hea241_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_hea241_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_hea241_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_hea241_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_hea241_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_hea241_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_hea241_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_hea241_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_hea241_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_hea241_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_hea242")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_hea242_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_hea242_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_hea242_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_hea242_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_hea242_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_hea242_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_hea242_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_hea242_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_hea242_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_hea242_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_hea242_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_heaus")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_heaus_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_heaus_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_heaus_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_heaus_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_heaus_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_heaus_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_heaus_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_heaus_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_heaus_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_heaus_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_heaus_e0"))).register();
    }

    private static void part10() {
        Dejson.item(NS, "rbmk_pellet_hen")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_hen_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_hen_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_hen_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_hen_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_hen_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_hen_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_hen_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_hen_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_hen_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_hen_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_hen_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_hep")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_hep_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_hep_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_hep_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_hep_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_hep_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_hep_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_hep_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_hep_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_hep_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_hep_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_hep_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_hep241")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_hep241_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_hep241_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_hep241_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_hep241_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_hep241_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_hep241_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_hep241_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_hep241_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_hep241_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_hep241_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_hep241_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_hes")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_hes_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_hes_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_hes_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_hes_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_hes_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_hes_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_hes_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_hes_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_hes_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_hes_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_hes_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_heu233")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_heu233_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_heu233_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_heu233_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_heu233_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_heu233_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_heu233_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_heu233_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_heu233_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_heu233_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_heu233_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_heu233_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_heu235")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_heu235_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_heu235_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_heu235_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_heu235_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_heu235_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_heu235_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_heu235_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_heu235_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_heu235_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_heu235_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_heu235_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_lea")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_lea_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_lea_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_lea_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_lea_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_lea_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_lea_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_lea_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_lea_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_lea_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_lea_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_lea_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_leaus")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_leaus_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_leaus_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_leaus_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_leaus_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_leaus_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_leaus_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_leaus_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_leaus_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_leaus_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_leaus_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_leaus_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_lep")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_lep_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_lep_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_lep_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_lep_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_lep_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_lep_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_lep_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_lep_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_lep_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_lep_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_lep_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_les")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_les_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_les_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_les_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_les_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_les_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_les_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_les_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_les_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_les_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_les_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_les_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_mea")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_mea_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_mea_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_mea_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_mea_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_mea_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_mea_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_mea_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_mea_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_mea_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_mea_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_mea_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_men")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_men_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_men_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_men_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_men_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_men_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_men_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_men_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_men_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_men_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_men_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_men_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_mep")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_mep_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_mep_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_mep_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_mep_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_mep_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_mep_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_mep_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_mep_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_mep_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_mep_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_mep_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_mes")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_mes_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_mes_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_mes_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_mes_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_mes_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_mes_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_mes_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_mes_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_mes_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_mes_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_mes_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_meu")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_meu_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_meu_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_meu_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_meu_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_meu_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_meu_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_meu_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_meu_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_meu_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_meu_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_meu_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_mox")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_mox_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_mox_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_mox_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_mox_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_mox_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_mox_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_mox_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_mox_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_mox_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_mox_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_mox_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_po210be")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_po210be_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_po210be_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_po210be_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_po210be_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_po210be_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_po210be_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_po210be_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_po210be_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_po210be_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_po210be_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_po210be_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_pu238be")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_pu238be_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_pu238be_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_pu238be_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_pu238be_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_pu238be_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_pu238be_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_pu238be_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_pu238be_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_pu238be_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_pu238be_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_pu238be_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_ra226be")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_ra226be_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_ra226be_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_ra226be_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_ra226be_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_ra226be_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_ra226be_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_ra226be_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_ra226be_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_ra226be_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_ra226be_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_ra226be_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_thmeu")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_thmeu_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_thmeu_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_thmeu_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_thmeu_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_thmeu_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_thmeu_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_thmeu_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_thmeu_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_thmeu_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_thmeu_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_thmeu_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_ueu")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_ueu_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_ueu_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_ueu_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_ueu_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_ueu_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_ueu_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_ueu_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_ueu_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_ueu_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_ueu_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_ueu_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_uzh")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_uzh_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_uzh_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_uzh_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_uzh_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_uzh_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_uzh_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_uzh_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_uzh_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_uzh_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_uzh_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_uzh_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_zfb_am_mix")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_zfb_am_mix_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_zfb_am_mix_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_zfb_am_mix_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_zfb_am_mix_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_zfb_am_mix_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_zfb_am_mix_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_zfb_am_mix_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_zfb_am_mix_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_zfb_am_mix_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_zfb_am_mix_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_zfb_am_mix_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_zfb_bismuth")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_zfb_bismuth_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_zfb_bismuth_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_zfb_bismuth_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_zfb_bismuth_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_zfb_bismuth_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_zfb_bismuth_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_zfb_bismuth_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_zfb_bismuth_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_zfb_bismuth_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_zfb_bismuth_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_zfb_bismuth_e0"))).register();
        Dejson.item(NS, "rbmk_pellet_zfb_pu241")
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.model("hbm:item/rbmk_pellet_zfb_pu241_e0"))
                        .when("1", ItemModels.model("hbm:item/rbmk_pellet_zfb_pu241_e1"))
                        .when("2", ItemModels.model("hbm:item/rbmk_pellet_zfb_pu241_e2"))
                        .when("3", ItemModels.model("hbm:item/rbmk_pellet_zfb_pu241_e3"))
                        .when("4", ItemModels.model("hbm:item/rbmk_pellet_zfb_pu241_e4"))
                        .when("5", ItemModels.model("hbm:item/rbmk_pellet_zfb_pu241_e0_xe"))
                        .when("6", ItemModels.model("hbm:item/rbmk_pellet_zfb_pu241_e1_xe"))
                        .when("7", ItemModels.model("hbm:item/rbmk_pellet_zfb_pu241_e2_xe"))
                        .when("8", ItemModels.model("hbm:item/rbmk_pellet_zfb_pu241_e3_xe"))
                        .when("9", ItemModels.model("hbm:item/rbmk_pellet_zfb_pu241_e4_xe"))
                        .fallback(ItemModels.model("hbm:item/rbmk_pellet_zfb_pu241_e0"))).register();
        Dejson.item(NS, "rbmk_reflector")
                .models(Models.parented("hbm:block/rbmk/rbmk_reflector_column_none", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        ItemModels.custom("hbm:rbmk_block_column").with("side", "hbm:blocks/rbmk/rbmk_reflector_side").with("top", "hbm:blocks/rbmk/rbmk_reflector_top").with("scale", 0.35).with("yaw", 0).with("lift", -0.675))).register();
        Dejson.item(NS, "rbmk_storage")
                .models(Models.parented("hbm:block/rbmk/rbmk_storage_column_none", Slots.EMPTY))
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        ItemModels.custom("hbm:rbmk_block_column").with("side", "hbm:blocks/rbmk/rbmk_storage_side").with("top", "hbm:blocks/rbmk/rbmk_storage_top").with("scale", 0.35).with("yaw", 0).with("lift", -0.675))).register();
        Dejson.item(NS, "red_barrel")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/barrel.obj", "hbm:textures/blocks/barrel_red.png").scale(0.5).ty(-0.5)
                                .yaw(0).exclude("Connector"))).register();
        Dejson.item(NS, "red_cable")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/cable_neo.obj", "hbm:textures/models/blocks/cable_neo.png").scale(1.1)
                                .ty(0.0).yaw(0).exclude("posY", "negY", "CX", "CY", "CZ"))).register();
        Dejson.item(NS, "red_connector")
                .definition(MachineObjModel.of("hbm:models/network/connector.obj", "hbm:textures/models/network/connector.png").scale(2.0)
                        .ty(0.0).yaw(0.0).guiScale(7.0).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -3.5, 0.0)).oversizedInGui().register();
    }

    private static void part11() {
        Dejson.item(NS, "red_connector_super")
                .definition(MachineObjModel.of("hbm:models/network/connector_super.obj", "hbm:textures/models/network/connector_super.png")
                        .scale(2.0).ty(0.0).yaw(0.0).guiScale(7.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -5.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "red_pylon")
                .definition(MachineObjModel.of("hbm:models/network/pylon.obj", "hbm:textures/models/network/pylon.png").scale(1.0).ty(0.0)
                        .yaw(0.0).guiScale(2.9).rawLegacyTransform().noCull().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -5.0, 0.0).parts("Pylon")).oversizedInGui().register();
        Dejson.item(NS, "red_pylon_large")
                .definition(MachineObjModel.of("hbm:models/network/pylon_large.obj", "hbm:textures/models/network/pylon_large.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(2.25).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -5.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "red_pylon_medium_steel")
                .definition(MachineObjModel.of("hbm:models/network/pylon_medium.obj", "hbm:textures/models/network/pylon_medium_steel.png")
                        .scale(0.5).ty(0.0).yaw(90.0).guiScale(4.5).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(1.0, -5.0, 0.0).translate(0.75, 0.0, 0.0).parts("Pylon")).oversizedInGui().register();
        Dejson.item(NS, "red_pylon_medium_steel_transformer")
                .definition(MachineObjModel.of("hbm:models/network/pylon_medium.obj", "hbm:textures/models/network/pylon_medium_steel.png")
                        .scale(0.5).ty(0.0).yaw(90.0).guiScale(4.5).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(1.0, -5.0, 0.0).translate(0.75, 0.0, 0.0).parts("Pylon", "Transformer")).oversizedInGui().register();
        Dejson.item(NS, "red_pylon_medium_wood")
                .definition(MachineObjModel.of("hbm:models/network/pylon_medium.obj", "hbm:textures/models/network/pylon_medium.png")
                        .scale(0.5).ty(0.0).yaw(90.0).guiScale(4.5).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(1.0, -5.0, 0.0).translate(0.75, 0.0, 0.0).parts("Pylon")).oversizedInGui().register();
        Dejson.item(NS, "red_pylon_medium_wood_transformer")
                .definition(MachineObjModel.of("hbm:models/network/pylon_medium.obj", "hbm:textures/models/network/pylon_medium.png")
                        .scale(0.5).ty(0.0).yaw(90.0).guiScale(4.5).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(1.0, -5.0, 0.0).translate(0.75, 0.0, 0.0).parts("Pylon", "Transformer")).oversizedInGui().register();
        Dejson.item(NS, "red_pylon_steel")
                .definition(MachineObjModel.of("hbm:models/network/pylon.obj", "hbm:textures/models/network/pylon_steel.png").scale(1.0)
                        .ty(0.0).yaw(0.0).guiScale(2.9).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -5.0, 0.0).parts("Pylon_steel")).oversizedInGui().register();
        Dejson.item(NS, "refueler")
                .definition(MachineObjModel.of("hbm:models/machines/refueler.obj", "hbm:textures/models/machines/refueler.png").scale(2.0)
                        .ty(0.0).yaw(0.0).guiScale(6.0).rawLegacyTransform().itemTranslate(1.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -3.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "round_airlock_door")
                .definition(MachineObjModel.of("hbm:models/pheodoors/airlock_door.obj", "hbm:textures/models/pheodoors/airlock_door.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(3.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -3.75, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "rtg_polonium")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/rtg.obj", "hbm:textures/models/machines/rtg_polonium.png").scale(1.0)
                                .ty(0.0).yaw(0).noCull())).register();
        Dejson.item(NS, "secure_access_door")
                .definition(MachineObjModel.of("hbm:models/pheodoors/secure_door.obj", "hbm:textures/models/pheodoors/secure_door.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(2.4).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 1.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -3.75, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "silo_hatch")
                .definition(MachineObjModel.of("hbm:models/doors/silo_hatch.obj", "hbm:textures/models/doors/silo_hatch.png").scale(0.5)
                        .ty(0.0).yaw(90.0).guiScale(2.0).rotateX(-120.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -2.0, 0.0).rotatePivot(0.0, 0.875, -1.875).rotateParts("Hatch")).oversizedInGui().register();
        Dejson.item(NS, "silo_hatch_large")
                .definition(MachineObjModel.of("hbm:models/doors/silo_hatch_large.obj", "hbm:textures/models/doors/silo_hatch_large.png")
                        .scale(0.5).ty(0.0).yaw(90.0).guiScale(1.5).rotateX(-120.0).rawLegacyTransform()
                        .noCull().itemTranslate(1.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -2.0, 0.0).rotatePivot(0.0, 0.875, -2.875).rotateParts("Hatch")).oversizedInGui().register();
        Dejson.item(NS, "sliding_blast_door")
                .definition(MachineObjModel.of("hbm:models/pheodoors/blast_door.obj", "hbm:textures/models/pheodoors/blast_door.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(2.5).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -2.75, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "sliding_seal_door")
                .definition(MachineObjModel.of("hbm:models/pheodoors/seal_door.obj", "hbm:textures/models/pheodoors/seal_door.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(7.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -5.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "solar_mirror")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/machines/solar_mirror.obj", "hbm:textures/models/machines/solar_mirror.png")
                                .scale(1.0).ty(0.0).yaw(0).noCull())).register();
        Dejson.item(NS, "spikes")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/spikes.obj", "hbm:textures/blocks/spikes.png").scale(1.0).ty(0.0).yaw(0))).register();
        Dejson.item(NS, "spotlight_fluoro")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/lights/fluorescent_lamp.obj", "hbm:textures/blocks/fluorescent_lamp.png")
                                .scale(1.5).ty(0.0).yaw(-90).noCull().parts("FluoroSingle"))).register();
        Dejson.item(NS, "spotlight_fluoro_off")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/lights/fluorescent_lamp.obj", "hbm:textures/blocks/fluorescent_lamp_off.png")
                                .scale(1.5).ty(0.0).yaw(-90).noCull().parts("FluoroSingle"))).register();
        Dejson.item(NS, "spotlight_halogen")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/lights/flood_lamp.obj", "hbm:textures/blocks/flood_lamp.png").scale(1.5)
                                .ty(0.0).yaw(-90).noCull().parts("FloodLamp"))).register();
        Dejson.item(NS, "spotlight_halogen_off")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/lights/flood_lamp.obj", "hbm:textures/blocks/flood_lamp_off.png").scale(1.5)
                                .ty(0.0).yaw(-90).noCull().parts("FloodLamp"))).register();
        Dejson.item(NS, "spotlight_incandescent")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/lights/cage_lamp.obj", "hbm:textures/blocks/cage_lamp.png").scale(1.5).ty(0.0)
                                .yaw(-90).noCull().parts("CageLamp"))).register();
        Dejson.item(NS, "spotlight_incandescent_off")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/lights/cage_lamp.obj", "hbm:textures/blocks/cage_lamp_off.png").scale(1.5)
                                .ty(0.0).yaw(-90).noCull().parts("CageLamp"))).register();
        Dejson.item(NS, "steel_corner")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/deco/steel_corner.obj", "hbm:textures/models/deco/steelcorner.png")
                                .scale(0.045).ty(0.0).yaw(180))).register();
        Dejson.item(NS, "steel_poles")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/pole.obj", "hbm:textures/blocks/steel_beam.png").scale(1.0).ty(-0.5)
                                .yaw(0).noCull())).register();
        Dejson.item(NS, "steel_roof")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/deco/steel_roof.obj", "hbm:textures/models/deco/steelroof.png").scale(0.045)
                                .ty(0.0).yaw(0))).register();
        Dejson.item(NS, "steel_scaffold")
                .models(Model.parent("hbm:item/machine_obj_legacy")
                        .override("hbm:item/steel_scaffold_red", "hbm:item_variant", 1)
                        .override("hbm:item/steel_scaffold_white", "hbm:item_variant", 2)
                        .override("hbm:item/steel_scaffold_yellow", "hbm:item_variant", 3))
                .definition(ItemModels.select("hbm:item_variant")
                        .when("0", ItemModels.special("hbm:item/machine_obj_base",
                                MachineObjModel.of("hbm:models/blocks/scaffold.obj", "hbm:textures/blocks/scaffold_steel.png").scale(1.0)
                                        .ty(-0.5).yaw(180.0)))
                        .when("1", ItemModels.special("hbm:item/machine_obj_base",
                                MachineObjModel.of("hbm:models/blocks/scaffold.obj", "hbm:textures/blocks/scaffold_red.png").scale(1.0)
                                        .ty(-0.5).yaw(180.0)))
                        .when("2", ItemModels.special("hbm:item/machine_obj_base",
                                MachineObjModel.of("hbm:models/blocks/scaffold.obj", "hbm:textures/blocks/scaffold_white.png").scale(1.0)
                                        .ty(-0.5).yaw(180.0)))
                        .when("3", ItemModels.special("hbm:item/machine_obj_base",
                                MachineObjModel.of("hbm:models/blocks/scaffold.obj", "hbm:textures/blocks/scaffold_yellow.png").scale(1.0)
                                        .ty(-0.5).yaw(180.0)))
                        .fallback(ItemModels.special("hbm:item/machine_obj_base",
                                MachineObjModel.of("hbm:models/blocks/scaffold.obj", "hbm:textures/blocks/scaffold_steel.png").scale(1.0)
                                        .ty(-0.5).yaw(180.0)))).register();
        Dejson.item(NS, "steel_wall")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/deco/steel_wall.obj", "hbm:textures/models/deco/steelwall.png").scale(0.045)
                                .ty(0.0).yaw(180))).register();
        Dejson.item(NS, "substation")
                .definition(MachineObjModel.of("hbm:models/network/substation.obj", "hbm:textures/models/network/substation.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(4.5).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -2.5, 0.0)).oversizedInGui().register();
    }

    private static void part12() {
        Dejson.item(NS, "t51_boots")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/armor/t51.obj", "hbm:textures/models/armor/t51_leg.png").scale(0.099).ty(0.0)
                                .yaw(155.0).pitch(180.0)
                                .exclude("Helmet", "Chest", "LeftArm", "RightArm", "LeftLeg", "RightLeg"))).register();
        Dejson.item(NS, "t51_helmet")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/armor/t51.obj", "hbm:textures/models/armor/t51_helmet.png").scale(0.078)
                                .ty(0.0).yaw(155.0).pitch(180.0)
                                .exclude("Chest", "LeftArm", "RightArm", "LeftLeg", "RightLeg", "LeftBoot", "RightBoot"))).register();
        Dejson.item(NS, "t51_legs")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/armor/t51.obj", "hbm:textures/models/armor/t51_leg.png").scale(0.099).ty(0.0)
                                .yaw(155.0).pitch(180.0)
                                .exclude("Helmet", "Chest", "LeftArm", "RightArm", "LeftBoot", "RightBoot"))).register();
        Dejson.item(NS, "t51_plate")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/armor/t51.obj", "hbm:textures/models/armor/t51_chest.png").scale(0.044).ty(0.0)
                                .yaw(155.0).pitch(180.0)
                                .exclude("Helmet", "LeftLeg", "RightLeg", "LeftBoot", "RightBoot")
                                .overlayParts("LeftArm", "RightArm")
                                .overlayTexture("hbm:textures/models/armor/t51_arm.png"))).register();
        Dejson.item(NS, "taint_barrel")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/barrel.obj", "hbm:textures/blocks/barrel_taint.png").scale(0.5).ty(-0.5)
                                .yaw(0).exclude("Connector"))).register();
        Dejson.item(NS, "tape_recorder")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/taperecorder.obj", "hbm:textures/blocks/deco_tape_recorder.png")
                                .scale(1.0).ty(-0.5).yaw(0).noCull())).register();
        Dejson.item(NS, "tesla")
                .definition(MachineObjModel.of("hbm:models/tesla.obj", "hbm:textures/models/tesla.png").scale(1.0).ty(0.0).yaw(0.0)
                        .guiScale(6.0).rawLegacyTransform().noCull().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -3.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "turret_arty")
                .definition(MachineObjModel.of("hbm:models/turrets/turret_arty.obj", "hbm:textures/models/turrets/arty.png").scale(0.5)
                        .yaw(-90.0).guiScale(3.5).rotateX(45.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(-3.0, -4.0, 0.0).rotatePivot(0.0, 3.0, 0.0)
                        .parts("Base", "Carriage", "Cannon", "Barrel").rotateParts("Cannon", "Barrel")).oversizedInGui().register();
        Dejson.item(NS, "turret_chekhov")
                .definition(MachineObjModel.of("hbm:models/turrets/turret_chekhov.obj", "hbm:textures/models/turrets/base.png").scale(1.0)
                        .yaw(0.0).guiScale(4.0).rawLegacyTransform().itemTranslate(-0.75, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -3.0, 0.0).parts("Base")
                        .passes(MachineObjModel.pass("hbm:textures/models/turrets/carriage.png").parts("Carriage"),
                                MachineObjModel.pass("hbm:textures/models/turrets/chekhov.png").parts("Body"),
                                MachineObjModel.pass("hbm:textures/models/turrets/chekhov_barrels.png").parts("Barrels"))).oversizedInGui().register();
        Dejson.item(NS, "turret_friendly")
                .definition(MachineObjModel.of("hbm:models/turrets/turret_chekhov.obj", "hbm:textures/models/turrets/base_friendly.png")
                        .scale(1.0).yaw(0.0).guiScale(4.0).rawLegacyTransform().itemTranslate(-0.75, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -3.0, 0.0).parts("Base")
                        .passes(MachineObjModel.pass("hbm:textures/models/turrets/carriage_friendly.png").parts("Carriage"),
                                MachineObjModel.pass("hbm:textures/models/turrets/chekhov.png").parts("Body"),
                                MachineObjModel.pass("hbm:textures/models/turrets/chekhov_barrels.png").parts("Barrels"))).oversizedInGui().register();
        Dejson.item(NS, "turret_fritz")
                .definition(MachineObjModel.of("hbm:models/turrets/turret_chekhov.obj", "hbm:textures/models/turrets/base.png").scale(1.0)
                        .yaw(0.0).guiScale(4.0).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -2.0, 0.0).parts("Base")
                        .passes(MachineObjModel.pass("hbm:textures/models/turrets/carriage.png").parts("Carriage"),
                                MachineObjModel.pass("hbm:textures/models/turrets/fritz.png").obj("hbm:models/turrets/turret_fritz.obj").parts("Gun"))).oversizedInGui().register();
        Dejson.item(NS, "turret_himars")
                .definition(MachineObjModel.of("hbm:models/turrets/turret_arty.obj", "hbm:textures/models/turrets/arty.png").scale(0.5)
                        .yaw(-90.0).guiScale(3.5).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -2.0, 0.0).parts("Base")
                        .passes(MachineObjModel.pass("hbm:textures/models/turrets/himars.png").obj("hbm:models/turrets/turret_himars.obj").parts("Carriage", "Launcher", "Crane"),
                                MachineObjModel.pass("hbm:textures/models/projectiles/himars_standard.png").obj("hbm:models/turrets/turret_himars.obj").parts("TubeStandard"))).oversizedInGui().register();
        Dejson.item(NS, "turret_howard")
                .definition(MachineObjModel.of("hbm:models/turrets/turret_chekhov.obj", "hbm:textures/models/turrets/base.png").scale(1.0)
                        .yaw(0.0).guiScale(4.0).rawLegacyTransform().itemTranslate(-0.75, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -4.5, 0.0).parts("Base")
                        .passes(MachineObjModel.pass("hbm:textures/models/turrets/carriage_ciws.png").obj("hbm:models/turrets/turret_howard.obj").parts("Carriage"),
                                MachineObjModel.pass("hbm:textures/models/turrets/howard.png").obj("hbm:models/turrets/turret_howard.obj").parts("Body"),
                                MachineObjModel.pass("hbm:textures/models/turrets/howard_barrels.png").obj("hbm:models/turrets/turret_howard.obj").parts("BarrelsTop", "BarrelsBottom"))).oversizedInGui().register();
        Dejson.item(NS, "turret_howard_damaged")
                .definition(MachineObjModel.of("hbm:models/turrets/turret_chekhov.obj", "hbm:textures/models/turrets/rusted/base.png")
                        .scale(1.0).yaw(0.0).guiScale(4.0).rawLegacyTransform().itemTranslate(-0.75, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -4.5, 0.0).parts("Base")
                        .passes(MachineObjModel.pass("hbm:textures/models/turrets/rusted/carriage_ciws.png").obj("hbm:models/turrets/turret_howard.obj").parts("Carriage"),
                                MachineObjModel.pass("hbm:textures/models/turrets/rusted/howard.png").obj("hbm:models/turrets/turret_howard_damaged.obj").parts("Body"),
                                MachineObjModel.pass("hbm:textures/models/turrets/rusted/howard_barrels.png").obj("hbm:models/turrets/turret_howard_damaged.obj").parts("BarrelsTop", "BarrelsBottom"))).oversizedInGui().register();
        Dejson.item(NS, "turret_jeremy")
                .definition(MachineObjModel.of("hbm:models/turrets/turret_chekhov.obj", "hbm:textures/models/turrets/base.png").scale(1.0)
                        .yaw(0.0).guiScale(2.5).rawLegacyTransform().itemTranslate(-0.5, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -2.0, 0.0).parts("Base")
                        .passes(MachineObjModel.pass("hbm:textures/models/turrets/carriage.png").parts("Carriage"),
                                MachineObjModel.pass("hbm:textures/models/turrets/jeremy.png").obj("hbm:models/turrets/turret_jeremy.obj").parts("Gun"))).oversizedInGui().register();
        Dejson.item(NS, "turret_maxwell")
                .definition(MachineObjModel.of("hbm:models/turrets/turret_chekhov.obj", "hbm:textures/models/turrets/base.png").scale(1.0)
                        .yaw(0.0).guiScale(4.0).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(-1.0, -3.0, 0.0).parts("Base")
                        .passes(MachineObjModel.pass("hbm:textures/models/turrets/carriage_ciws.png").obj("hbm:models/turrets/turret_howard.obj").parts("Carriage"),
                                MachineObjModel.pass("hbm:textures/models/turrets/maxwell.png").obj("hbm:models/turrets/turret_microwave.obj").parts("Microwave"))).oversizedInGui().register();
        Dejson.item(NS, "turret_richard")
                .definition(MachineObjModel.of("hbm:models/turrets/turret_chekhov.obj", "hbm:textures/models/turrets/base.png").scale(1.0)
                        .yaw(0.0).guiScale(5.0).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -2.0, 0.0).parts("Base")
                        .passes(MachineObjModel.pass("hbm:textures/models/turrets/carriage.png").parts("Carriage"),
                                MachineObjModel.pass("hbm:textures/models/turrets/richard.png").obj("hbm:models/turrets/turret_richard.obj").parts("Launcher"))).oversizedInGui().register();
        Dejson.item(NS, "turret_sentry")
                .definition(MachineObjModel.of("hbm:models/turrets/turret_sentry.obj", "hbm:textures/models/turrets/sentry.png")
                        .scale(1.0).yaw(90.0).guiScale(7.0).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -4.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "turret_sentry_damaged")
                .definition(MachineObjModel.of("hbm:models/turrets/turret_sentry.obj", "hbm:textures/models/turrets/sentry_damaged.png")
                        .scale(1.0).yaw(90.0).guiScale(7.0).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -4.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "turret_tauon")
                .definition(MachineObjModel.of("hbm:models/turrets/turret_chekhov.obj", "hbm:textures/models/turrets/base.png").scale(1.0)
                        .yaw(0.0).guiScale(5.0).rawLegacyTransform().itemTranslate(0.0, 0.0, 0.0)
                        .itemPreTranslate(0.0, 0.0, 0.0).guiTranslate(0.0, -2.0, 0.0).parts("Base")
                        .passes(MachineObjModel.pass("hbm:textures/models/turrets/carriage.png").parts("Carriage"),
                                MachineObjModel.pass("hbm:textures/models/turrets/tauon.png").obj("hbm:models/turrets/turret_tauon.obj").parts("Cannon", "Rotor"))).oversizedInGui().register();
        Dejson.item(NS, "vault_door")
                .definition(MachineObjModel.of("hbm:models/pheodoors/vault_door.obj", "hbm:textures/models/pheodoors/vault/vault_door_3.png")
                        .scale(0.5).ty(0.0).yaw(0.0).guiScale(3.5).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, -1.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -3.0, 0.0).exclude("Frame").overlayParts("Label")
                        .overlayTexture("hbm:textures/models/pheodoors/vault/label_101.png")).oversizedInGui().register();
        Dejson.item(NS, "vitrified_barrel")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/barrel.obj", "hbm:textures/blocks/barrel_vitrified.png").scale(0.5)
                                .ty(-0.5).yaw(0).exclude("Connector"))).register();
        Dejson.item(NS, "water_door")
                .definition(MachineObjModel.of("hbm:models/pheodoors/water_door.obj", "hbm:textures/models/pheodoors/water_door.png")
                        .scale(0.5).ty(0.0).yaw(90.0).guiScale(4.0).rawLegacyTransform().noCull()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -4.0, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "watz")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/reactors/watz.obj", "hbm:textures/models/machines/watz.png").scale(0.16)
                                .ty(0.0).yaw(180))).register();
        Dejson.item(NS, "watz_pump")
                .definition(MachineObjModel.of("hbm:models/machines/watz_pump.obj", "hbm:textures/models/machines/watz_pump.png")
                        .scale(1.0).ty(0.0).yaw(0.0).guiScale(5.0).rawLegacyTransform()
                        .itemTranslate(0.0, 0.0, 0.0).itemPreTranslate(0.0, 0.0, 0.0)
                        .guiTranslate(0.0, -1.5, 0.0)).oversizedInGui().register();
        Dejson.item(NS, "yellow_barrel")
                .definition(ItemModels.special("hbm:item/machine_obj_base",
                        MachineObjModel.of("hbm:models/blocks/barrel.obj", "hbm:textures/blocks/barrel_yellow.png").scale(0.5)
                                .ty(-0.5).yaw(0).exclude("Connector"))).register();
    }
}
