package com.hbm.main;

import com.hbm.lib.Library;
import net.minecraft.resources.Identifier;

/**
 * The texture identifiers the copied ntm-next entities and their renderers name directly. ntm-next's
 * class also owns every OBJ model and shader; the entity meshes live client-side in
 * {@code com.hbm.client.render.entity.NtmEntityModels}, since loading an OBJ needs the client's
 * resource manager and this class is reachable from common code (the glyphid skins are resolved by
 * {@code EntityGlyphid.getSkin}).
 */
public final class ResourceManager {

    private ResourceManager() {}

    public static final Identifier glyphid_tex = Library.id("textures/entity/glyphid.png");
    public static final Identifier glyphid_scout_tex = Library.id("textures/entity/glyphid_scout.png");
    public static final Identifier glyphid_blaster_tex = Library.id("textures/entity/glyphid_blaster.png");
    public static final Identifier glyphid_bombardier_tex = Library.id("textures/entity/glyphid_bombardier.png");
    public static final Identifier glyphid_brawler_tex = Library.id("textures/entity/glyphid_brawler.png");
    public static final Identifier glyphid_behemoth_tex = Library.id("textures/entity/glyphid_behemoth.png");
    public static final Identifier glyphid_brenda_tex = Library.id("textures/entity/glyphid_brenda.png");
    public static final Identifier glyphid_digger_tex = Library.id("textures/entity/glyphid_digger.png");
    public static final Identifier glyphid_nuclear_tex = Library.id("textures/entity/glyphid_nuclear.png");
    public static final Identifier glyphid_infestation_tex = Library.id("textures/entity/glyphid_infestation.png");

    // Mobs
    public static final Identifier quadcopter_tex = Library.id("textures/entity/quadcopter.png");
    public static final Identifier radbeast_tex = Library.id("textures/entity/radbeast.png");
    public static final Identifier teslacrab_tex = Library.id("textures/entity/teslacrab.png");
    public static final Identifier taintcrab_tex = Library.id("textures/entity/taintcrab.png");
    public static final Identifier crab_tex = Library.id("textures/entity/crab.png");
    public static final Identifier blockspider_tex = Library.id("textures/entity/blockspider.png");
    public static final Identifier plasticbag_tex = Library.id("textures/entity/plasticbag.png");
    public static final Identifier duck_tex = Library.id("textures/entity/duck.png");
    public static final Identifier pigeon_tex = Library.id("textures/entity/pigeon.png");
    public static final Identifier dummy_tex = Library.id("textures/entity/dummy.png");
    public static final Identifier boat_rubber_tex = Library.id("textures/entity/boat_rubber.png");
    public static final Identifier maskman_tex = Library.id("textures/entity/maskman.png");
    public static final Identifier iou_tex = Library.id("textures/entity/iou.png");
    public static final Identifier ufo_tex = Library.id("textures/entity/ufo.png");
    public static final Identifier mark_zero_head_tex = Library.id("textures/entity/mark_zero_head.png");
    public static final Identifier mark_zero_body_tex = Library.id("textures/entity/mark_zero_body.png");
    public static final Identifier ghost_tex = Library.id("textures/entity/ghost.png");
    public static final Identifier fbi_tex = Library.id("textures/entity/fbi.png");
    public static final Identifier creeper_nuclear_tex = Library.id("textures/entity/creeper.png");
    public static final Identifier creeper_tainted_tex = Library.id("textures/entity/creeper_tainted.png");
    public static final Identifier creeper_phosgene_tex = Library.id("textures/entity/creeper_phosgene.png");
    public static final Identifier creeper_volatile_tex = Library.id("textures/entity/creeper_volatile.png");
    public static final Identifier creeper_gold_tex = Library.id("textures/entity/creeper_gold.png");
    public static final Identifier creeper_armor_tex = Library.id("textures/entity/creeper_armor.png");
    public static final Identifier creeper_armor_taint_tex = Library.id("textures/entity/creeper_armor_taint.png");
    public static final Identifier parasite_maggot_tex = Library.id("textures/entity/parasite_maggot.png");
    public static final Identifier chopper_tex = Library.id("textures/entity/chopper.png");
    public static final Identifier meteor_tex = Library.id("textures/entity/meteor.png");
    public static final Identifier shrapnel_tex = Library.id("textures/entity/shrapnel.png");

    // Vehicles, planes and props
    public static final Identifier train_tram_tex = Library.id("textures/block/models/trains/tram.png");
    public static final Identifier tram_trailer_tex = Library.id("textures/block/models/trains/tram_trailer.png");
    public static final Identifier boxcar_tex = Library.id("textures/block/models/boxcar.png");
    public static final Identifier duchessgambit_tex = Library.id("textures/block/models/duchessgambit.png");
    public static final Identifier building_tex = Library.id("textures/models/weapons/building.png");
    public static final Identifier torpedo_tex = Library.id("textures/models/weapons/torpedo.png");
    public static final Identifier c130_0_tex = Library.id("textures/models/weapons/c130_0.png");
    public static final Identifier dornier_1_tex = Library.id("textures/models/dornier_1.png");
    public static final Identifier dornier_2_tex = Library.id("textures/models/dornier_2.png");
    public static final Identifier dornier_4_tex = Library.id("textures/models/dornier_4.png");
    public static final Identifier b29_0_tex = Library.id("textures/models/b29_0.png");
    public static final Identifier b29_1_tex = Library.id("textures/models/b29_1.png");
    public static final Identifier b29_2_tex = Library.id("textures/models/b29_2.png");
    public static final Identifier b29_3_tex = Library.id("textures/models/b29_3.png");
    public static final Identifier bomblet_zeta_tex = Library.id("textures/models/bomblet_zeta_texture.png");
    public static final Identifier delivery_drone_tex = Library.id("textures/models/machines/drone.png");
    public static final Identifier delivery_drone_request_tex = Library.id("textures/models/machines/drone_request.png");
    public static final Identifier delivery_drone_express_tex = Library.id("textures/models/machines/drone_express.png");
    public static final Identifier supply_crate_tex = Library.id("textures/block/crate_can.png");
    public static final Identifier soyuz_lander_tex = Library.id("textures/models/soyuz_capsule/soyuz_lander.png");
    public static final Identifier soyuz_chute_tex = Library.id("textures/models/soyuz_capsule/soyuz_chute.png");
    public static final Identifier soyuz_memento_tex = Library.id("textures/models/misc/polaroid_memento.png");
    public static final Identifier[][] soyuz_skin_tex = soyuzSkins("soyuz", "soyuz_luna", "soyuz_authentic");
    public static final Identifier miner_rocket_tex = Library.id("textures/models/miner_rocket.png");
    public static final Identifier bobmazon_tex = Library.id("textures/models/bobmazon.png");
    public static final Identifier custom_nuke_tex = Library.id("textures/models/custom_nuke.png");
    public static final Identifier emp_ring_tex = Library.id("textures/models/emp_blast.png");
    public static final Identifier sat_foeq_burning_tex = Library.id("textures/models/sat_foeq_burning.png");
    public static final Identifier bullet_tex = Library.id("textures/models/bullet.png");
    public static final Identifier bullet_chopper_tex = Library.id("textures/models/emplacer.png");
    public static final Identifier bullet_critical_tex = Library.id("textures/models/tau.png");
    public static final Identifier chopper_bomb_tex = Library.id("textures/models/chopper_bomb.png");
    public static final Identifier tomblast_tex = Library.id("textures/models/explosion/tomblast.png");
    public static final Identifier tom_main_tex = Library.id("textures/models/weapons/tom_main.png");
    public static final Identifier tom_flame_tex = Library.id("textures/models/weapons/tom_flame.png");
    public static final Identifier mini_nuke_tex = Library.id("textures/models/projectiles/mini_nuke.png");
    public static final Identifier stirling_tex = Library.id("textures/models/machines/stirling.png");
    public static final Identifier stirling_steel_tex = Library.id("textures/models/machines/stirling_steel.png");
    public static final Identifier white_tex = Library.id("textures/models/misc/white.png");

    /**
     * 1.7's per-part Soyuz sheets: one folder per skin, one file per structural group, in the order
     * {@code SoyuzMesh} indexes them.
     */
    private static Identifier[][] soyuzSkins(String... folders) {
        String[] parts = {"engineblock", "bottomstage", "topstage", "payload", "payloadblocks", "les", "lesthrusters",
                "mainengines", "sideengines", "booster", "boosterside"};
        Identifier[][] skins = new Identifier[folders.length][parts.length];
        for (int skin = 0; skin < folders.length; skin++)
            for (int part = 0; part < parts.length; part++)
                skins[skin][part] = Library.id("textures/models/" + folders[skin] + "/" + parts[part] + ".png");
        return skins;
    }
}
