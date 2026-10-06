package com.hbm.advancement;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.core.dejson.Advancements;
import dev.golem.core.dejson.Advancements.Criterion;

import static dev.golem.core.dejson.Advancements.CHALLENGE;
import static dev.golem.core.dejson.Advancements.TASK;

/**
 * The 1.7 achievement page as advancements (1.7 {@code MainRegistry.load}:370-442, {@code AchievementHandler}).
 * Touches no Minecraft class: the build writes the JSON in the 26.1 shape and the legacy layer takes it down.
 *
 * <p>Mapping rules, applied to every row:
 * <ul>
 * <li>id: the 1.7 name in lowercase snake_case ({@code ZIRNOXBoom} -> {@code zirnox_boom});</li>
 * <li>parent: the 1.7 parent achievement, or {@code hbm:root} for the parentless ones (1.7 had one flat page);</li>
 * <li>frame: {@code challenge} for {@code setSpecial()}, else {@code task}; nothing is hidden, since 1.7 showed the
 *     whole page and the modern tree already reveals itself two levels at a time;</li>
 * <li>criteria: every 1.7 award route is one alternative criterion. {@code AchievementHandler}'s crafted-item table
 *     becomes vanilla {@code recipe_crafted} for every crafting/smelting recipe making the item (1.7
 *     {@code ItemCraftedEvent}/{@code ItemSmeltedEvent}) plus {@code hbm:item_produced} (1.7
 *     {@code SlotCraftingOutput} and the anvil packet). A row with no award route in 1.7 is
 *     {@code minecraft:impossible}: grant nothing 1.7 did not.</li>
 * </ul>
 */
public final class HbmAdvancementDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    /** 1.7 {@code Fluids.ASCHRAB} id, the Taste of Blood icon's fluid; held against the registry by a GameTest. */
    /** The icon's fluid, by registry name (never a numeric fluid id, History section 19ej-ids). */
    public static final String ASCHRAB_ID = "hbm:aschrab";
    public static final int ASCHRAB_COLOR = 0xb50000;

    private HbmAdvancementDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;
        // The icon items' models are on their registrations (ModAchievementItems).

        // The page itself. 1.7 had an always-open "Nuclear Tech" achievement page; a modern tab opens when its
        // root is done, so the root completes on the first tick and says nothing.
        Advancements.advancement(NS, "root")
                .icon("hbm:nuke_man")
                .title("achievement.hbm.root").description("achievement.hbm.root.desc")
                .background("minecraft:gui/advancements/backgrounds/stone")
                .toast(false).announce(false)
                .criterion("tick", Advancements.tick())
                .register();

        // 1.7: MainRegistry.java:370-379, all setSpecial
        entry("sacrifice", "sacrifice", "hbm:burnt_bark", null, CHALLENGE).criterion("impossible", Advancements.impossible()).register();
        entry("impossible", "impossible", "hbm:nothing", null, CHALLENGE).criterion("impossible", Advancements.impossible()).register();
        entry("taste_of_blood", "tasteofblood", "hbm:fluid_icon", null, CHALLENGE)
                .iconComponents(fluidIcon(ASCHRAB_ID, ASCHRAB_COLOR))
                .criterion("impossible", Advancements.impossible()).register();
        // 1.7 ItemBoltgun: the VICTIM, a player the boltgun killed
        entry("go_fish", "goFish", icon("gofish"), null, CHALLENGE).criterion("bolted", trigger("bolted")).register();
        crafted(entry("potato", "potato", "hbm:battery_potatos", null, CHALLENGE), "hbm:battery_potatos");
        entry("c20_5", "c20_5", icon("questionmark"), null, CHALLENGE).criterion("impossible", Advancements.impossible()).register();
        // 1.7 WeaponSpecial.onUpdate + ArmorUtil.checkForFiend: every tick, the jacket worn and the weapon held
        entry("fiend", "fiend", "hbm:shimmer_sledge", null, CHALLENGE)
                .criterion("worn_while_held", Advancements.tick().player(
                        Advancements.equipment("chest", "hbm:jackt", "mainhand", "hbm:shimmer_sledge"))).register();
        entry("fiend2", "fiend2", "hbm:shimmer_axe", null, CHALLENGE)
                .criterion("worn_while_held", Advancements.tick().player(
                        Advancements.equipment("chest", "hbm:jackt2", "mainhand", "hbm:shimmer_axe"))).register();
        // 1.7 ModEventHandler.onBlockBreak: the first gneiss a player breaks, plus 500 experience
        entry("stratum", "stratum", "hbm:stone_gneiss", null, CHALLENGE)
                .criterion("mined_gneiss", Advancements.tick().player(Advancements.stat("minecraft:mined", "hbm:stone_gneiss", 1)))
                .experience(500).register();
        // 1.7 TileEntityPADetector:162 (bystanders within 100/50/100) and AchievementHandler (the output slot)
        crafted(entry("omega12", "omega12", "hbm:particle_digamma", null, CHALLENGE)
                .criterion("particle_produced", trigger("particle_produced").item("hbm:particle_digamma")), "hbm:particle_digamma");

        // 1.7: MainRegistry.java:381-385
        entry("no9", "no9", "hbm:no9", null, TASK)
                .criterion("smoked", trigger("smoked").item("hbm:cigarette").player(Advancements.equipment("head", "hbm:no9")))
                .register();
        // 1.7 ModEventHandler.onItemPickup: a slime ball picked up. Vanilla counts pickups in minecraft:picked_up.
        entry("slimeball", "slimeball", icon("acid"), null, TASK)
                .criterion("picked_up", Advancements.tick().player(Advancements.stat("minecraft:picked_up", "minecraft:slime_ball", 1)))
                .register();
        entry("sulfuric", "sulfuric", icon("balls"), "slimeball", CHALLENGE)
                .criterion("dissolved", trigger("dissolved").item("minecraft:slime_ball")).register();
        entry("inferno", "inferno", "hbm:canister_napalm", null, CHALLENGE)
                .criterion("napalm", trigger("detonation").kinds("refinery", "fluid_tank")).register();
        entry("red_room", "redRoom", "hbm:key_red", null, CHALLENGE)
                .criterion("room_opened", trigger("room_opened")).register();

        // 1.7 ModEventHandler.onEntityDeath: a tainted creeper killed by a falling boxcar, players within 50
        entry("hidden", "hidden", icon("questionmark"), null, TASK)
                .criterion("crushed", trigger("crushed").kinds("hbm:entity_mob_tainted_creeper")).register();

        entry("horizons_start", "horizonsStart", "hbm:sat_gerald", null, TASK)
                .criterion("orbit", trigger("orbit").item("hbm:sat_gerald")).register();
        entry("horizons_end", "horizonsEnd", "hbm:sat_gerald", "horizons_start", TASK)
                .criterion("satellite_action", trigger("satellite_action").item("hbm:sat_gerald")).register();
        // 1.7 never awarded horizonsBonus
        entry("horizons_bonus", "horizonsBonus", "hbm:sat_gerald", "horizons_end", CHALLENGE)
                .criterion("impossible", Advancements.impossible()).register();

        entry("boss_creeper", "bossCreeper", "hbm:coin_creeper", null, TASK)
                .criterion("nuclear_creeper", trigger("detonation").kinds("nuclear_creeper")).register();
        entry("boss_meltdown", "bossMeltdown", "hbm:coin_radiation", "boss_creeper", TASK)
                .criterion("boss_killed", trigger("boss_killed").kinds("meltdown")).register();
        entry("boss_maskman", "bossMaskman", "hbm:coin_maskman", "boss_creeper", TASK)
                .criterion("boss_killed", trigger("boss_killed").kinds("maskman")).register();
        entry("boss_worm", "bossWorm", "hbm:coin_worm", "boss_maskman", CHALLENGE)
                .criterion("boss_killed", trigger("boss_killed").kinds("worm")).register();
        entry("boss_ufo", "bossUFO", "hbm:coin_ufo", "boss_worm", CHALLENGE)
                .criterion("boss_killed", trigger("boss_killed").kinds("ufo")).register();

        // 1.7 EntityEffectHandler.handleRadiation: the 200-RAD band and the 1000-RAD death
        entry("rad_poison", "radPoison", "hbm:geiger_counter", null, TASK)
                .criterion("radiation", trigger("radiation").kinds("poison")).register();
        entry("rad_death", "radDeath", "minecraft:skeleton_skull", "rad_poison", CHALLENGE)
                .criterion("radiation", trigger("radiation").kinds("death")).register();

        entry("some_wounds", "someWounds", "hbm:injector_knife", null, TASK)
                .criterion("bled_out", trigger("bled_out")).register();

        // 1.7 HbmLivingProps.setDigamma: > 0, >= 2, >= 10
        entry("digamma_see", "digammaSee", icon("digammasee"), null, TASK)
                .criterion("digamma", trigger("digamma").kinds("see")).register();
        entry("digamma_feel", "digammaFeel", icon("digammafeel"), "digamma_see", TASK)
                .criterion("digamma", trigger("digamma").kinds("feel")).register();
        entry("digamma_know", "digammaKnow", icon("digammaknow"), "digamma_feel", CHALLENGE)
                .criterion("digamma", trigger("digamma").kinds("know")).register();
        entry("digamma_kauai_moho", "digammaKauaiMoho", icon("digammakauaimoho"), "digamma_know", CHALLENGE)
                .criterion("spear", trigger("detonation").kinds("digamma_spear")).register();
        // 1.7 never awarded digammaUpOnTop
        entry("digamma_up_on_top", "digammaUpOnTop", icon("digammaupontop"), "digamma_kauai_moho", CHALLENGE)
                .criterion("impossible", Advancements.impossible()).register();

        // 1.7: progression achieves, MainRegistry.java:411-440
        crafted(entry("burner_press", "burnerPress", "hbm:machine_press", null, TASK), "hbm:machine_press");
        crafted(entry("blast_furnace", "blastFurnace", "hbm:machine_blast_furnace", "burner_press", TASK), "hbm:machine_blast_furnace");
        crafted(entry("assembly", "assembly", "hbm:machine_assembly_machine", "burner_press", TASK), "hbm:machine_assembly_machine");
        crafted(entry("selenium", "selenium", "hbm:ingot_starmetal", "burner_press", CHALLENGE), "hbm:piston_selenium", "hbm:gun_b92");
        crafted(entry("chemplant", "chemplant", "hbm:machine_chemical_plant", "assembly", TASK), "hbm:machine_chemical_plant");
        crafted(entry("concrete", "concrete", "hbm:concrete", "chemplant", TASK), "hbm:concrete_smooth", "hbm:concrete_asbestos");
        crafted(entry("polymer", "polymer", "hbm:ingot_polymer", "chemplant", TASK), "hbm:ingot_polymer");
        crafted(entry("desh", "desh", "hbm:ingot_desh", "chemplant", TASK), "hbm:ingot_desh");
        crafted(entry("tantalum", "tantalum", "hbm:gem_tantalium", "chemplant", CHALLENGE), "hbm:gem_tantalium");
        crafted(entry("gas_cent", "gasCent", "hbm:ingot_uranium_fuel", "desh", TASK), "hbm:machine_gascent");
        crafted(entry("centrifuge", "centrifuge", "hbm:machine_centrifuge", "polymer", TASK), "hbm:machine_centrifuge");
        entry("foeq", "FOEQ", "hbm:sat_foeq", "desh", CHALLENGE)
                .criterion("orbit", trigger("orbit").item("hbm:sat_foeq")).register();
        entry("soyuz", "soyuz", "minecraft:baked_potato", "desh", CHALLENGE)
                .criterion("exhaust", trigger("detonation").kinds("soyuz_exhaust")).register();
        // 1.7 EntitySoyuz:104: only the flame_pony payload; the port's call site passes the rocket for it
        entry("space", "space", "hbm:missile_soyuz_0", "desh", CHALLENGE)
                .criterion("orbit", trigger("orbit").item("hbm:missile_soyuz_0", "hbm:flame_pony")).register();
        crafted(entry("schrab", "schrab", "hbm:ingot_schrabidium", "desh", TASK), "hbm:ingot_schrabidium", "hbm:nugget_schrabidium");
        crafted(entry("acidizer", "acidizer", "hbm:machine_crystallizer", "desh", TASK), "hbm:machine_crystallizer");
        entry("radium", "radium", "hbm:coffee_radium", "centrifuge", CHALLENGE)
                .criterion("consumed", trigger("consumed").item("hbm:coffee_radium")).register();
        crafted(entry("technetium", "technetium", "hbm:ingot_tcalloy", "centrifuge", TASK), "hbm:nugget_technetium");
        entry("zirnox_boom", "ZIRNOXBoom", "hbm:debris_element", "centrifuge", CHALLENGE)
                .criterion("meltdown", trigger("detonation").kinds("zirnox")).register();
        crafted(entry("chicago_pile", "chicagoPile", "hbm:pile_rod_plutonium", "centrifuge", TASK), "hbm:billet_pu_mix");
        crafted(entry("silex", "SILEX", "hbm:machine_silex", "acidizer", TASK), "hbm:machine_silex");
        // 1.7 icon watz_pellet meta 0 = EnumWatzType.SCHRABIDIUM
        crafted(entry("watz", "watz", "hbm:watz_pellet_schrabidium", "schrab", TASK), "hbm:struct_watz_core");
        entry("watz_boom", "watzBoom", "hbm:bucket_mud", "watz", CHALLENGE)
                .criterion("meltdown", trigger("detonation").kinds("watz")).register();
        crafted(entry("rbmk", "RBMK", "hbm:rbmk_fuel_ueu", "concrete", TASK), "hbm:rbmk_fuel_empty");
        entry("rbmk_boom", "RBMKBoom", "hbm:debris_fuel", "rbmk", CHALLENGE)
                .criterion("meltdown", trigger("detonation").kinds("rbmk")).register();
        crafted(entry("bismuth", "bismuth", "hbm:ingot_bismuth", "rbmk", TASK), "hbm:nugget_bismuth");
        crafted(entry("breeding", "breeding", "hbm:ingot_am_mix", "rbmk", CHALLENGE), "hbm:nugget_am241", "hbm:nugget_am242");
        crafted(entry("fusion", "fusion", "hbm:fusion_torus", "bismuth", CHALLENGE), "hbm:struct_torus_core");
        crafted(entry("red_balloons", "redBalloons", "hbm:missile_nuclear", "polymer", CHALLENGE),
                "hbm:missile_nuclear", "hbm:missile_nuclear_cluster", "hbm:missile_doomsday", "hbm:mp_warhead_10_nuclear",
                "hbm:mp_warhead_10_nuclear_large", "hbm:mp_warhead_15_nuclear", "hbm:mp_warhead_15_nuclear_shark",
                "hbm:mp_warhead_15_boxcar");
        entry("manhattan", "manhattan", "hbm:nuke_boy", "polymer", CHALLENGE)
                .criterion("nuke", trigger("detonation").kinds("nuke")).register();
    }

    private static String icon(String variant) {
        return "hbm:achievement_icon_" + variant;
    }

    private static Advancements.Builder entry(String id, String legacyName, String icon, String parent, String frame) {
        return Advancements.advancement(NS, id)
                .parent(parent == null ? "hbm:root" : "hbm:" + parent)
                .icon(icon)
                .title("achievement." + legacyName)
                .description("achievement." + legacyName + ".desc")
                .frame(frame);
    }

    /** 1.7 {@code AchievementHandler.craftingAchievements}: crafting/smelting recipes, machine outputs, the anvil. */
    private static void crafted(Advancements.Builder builder, String... items) {
        builder.criterion("produced", trigger("item_produced").item(items)).craftedFrom(items).register();
    }

    private static Criterion trigger(String path) {
        return Advancements.trigger(NS + ":" + path);
    }

    /** The {@code fluid_icon} droplet for one fluid, in 26.1 component spelling. */
    private static JsonObject fluidIcon(String fluid, int color) {
        JsonObject components = new JsonObject();
        components.addProperty("hbm:fluid", fluid);
        JsonObject modelData = new JsonObject();
        JsonArray colors = new JsonArray();
        colors.add(color);
        modelData.add("colors", colors);
        components.add("minecraft:custom_model_data", modelData);
        return components;
    }
}
