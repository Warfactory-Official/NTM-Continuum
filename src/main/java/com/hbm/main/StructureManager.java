package com.hbm.main;

import com.hbm.Constants;
import com.hbm.world.gen.nbt.NBTStructure;

/**
 * 1.7 {@code StructureManager}: every bundled {@code .nbt} structure, loaded from the jar when this class is first
 * touched (as 1.7's static initialiser did). Files are copied unchanged from 1.7 under
 * {@code assets/hbm/structures}, except that {@code ntmruinsA..J.nbt} are lower-cased: resource paths may not
 * contain capitals on any supported version.
 */
public final class StructureManager {

    private StructureManager() {}

    // METEOR DUNGEON
    public static final NBTStructure meteor_spike = new NBTStructure(Constants.MOD_ID, "structures/meteor/meteor-spike.nbt");
    public static final NBTStructure meteor_core = new NBTStructure(Constants.MOD_ID, "structures/meteor/meteor-core.nbt");
    public static final NBTStructure meteor_corner = new NBTStructure(Constants.MOD_ID, "structures/meteor/meteor-corner.nbt");
    public static final NBTStructure meteor_t = new NBTStructure(Constants.MOD_ID, "structures/meteor/meteor-t.nbt");
    public static final NBTStructure meteor_stairs = new NBTStructure(Constants.MOD_ID, "structures/meteor/meteor-stairs.nbt");
    public static final NBTStructure meteor_fallback = new NBTStructure(Constants.MOD_ID, "structures/meteor/meteor-fallback.nbt");

    public static final NBTStructure meteor_3_bale = new NBTStructure(Constants.MOD_ID, "structures/meteor/loot3x3/meteor-3-bale.nbt");
    public static final NBTStructure meteor_3_blank = new NBTStructure(Constants.MOD_ID, "structures/meteor/loot3x3/meteor-3-blank.nbt");
    public static final NBTStructure meteor_3_block = new NBTStructure(Constants.MOD_ID, "structures/meteor/loot3x3/meteor-3-block.nbt");
    public static final NBTStructure meteor_3_crab = new NBTStructure(Constants.MOD_ID, "structures/meteor/loot3x3/meteor-3-crab.nbt");
    public static final NBTStructure meteor_3_crab_tesla = new NBTStructure(Constants.MOD_ID, "structures/meteor/loot3x3/meteor-3-crab-tesla.nbt");
    public static final NBTStructure meteor_3_crate = new NBTStructure(Constants.MOD_ID, "structures/meteor/loot3x3/meteor-3-crate.nbt");
    public static final NBTStructure meteor_3_dirt = new NBTStructure(Constants.MOD_ID, "structures/meteor/loot3x3/meteor-3-dirt.nbt");
    public static final NBTStructure meteor_3_lead = new NBTStructure(Constants.MOD_ID, "structures/meteor/loot3x3/meteor-3-lead.nbt");
    public static final NBTStructure meteor_3_ooze = new NBTStructure(Constants.MOD_ID, "structures/meteor/loot3x3/meteor-3-ooze.nbt");
    public static final NBTStructure meteor_3_pillar = new NBTStructure(Constants.MOD_ID, "structures/meteor/loot3x3/meteor-3-pillar.nbt");
    public static final NBTStructure meteor_3_star = new NBTStructure(Constants.MOD_ID, "structures/meteor/loot3x3/meteor-3-star.nbt");
    public static final NBTStructure meteor_3_tesla = new NBTStructure(Constants.MOD_ID, "structures/meteor/loot3x3/meteor-3-tesla.nbt");
    public static final NBTStructure meteor_3_book = new NBTStructure(Constants.MOD_ID, "structures/meteor/loot3x3/meteor-3-book.nbt");
    public static final NBTStructure meteor_3_mku = new NBTStructure(Constants.MOD_ID, "structures/meteor/loot3x3/meteor-3-mku.nbt");
    public static final NBTStructure meteor_3_statue = new NBTStructure(Constants.MOD_ID, "structures/meteor/loot3x3/meteor-3-statue.nbt");
    public static final NBTStructure meteor_3_glow = new NBTStructure(Constants.MOD_ID, "structures/meteor/loot3x3/meteor-3-glow.nbt");

    public static final NBTStructure meteor_room_base_end = new NBTStructure(Constants.MOD_ID, "structures/meteor/room10/room-base-end.nbt");
    public static final NBTStructure meteor_room_base_thru = new NBTStructure(Constants.MOD_ID, "structures/meteor/room10/room-base-thru.nbt");

    public static final NBTStructure meteor_room_balcony = new NBTStructure(Constants.MOD_ID, "structures/meteor/room10/room-balcony.nbt");
    public static final NBTStructure meteor_room_basic = new NBTStructure(Constants.MOD_ID, "structures/meteor/room10/room-basic.nbt");
    public static final NBTStructure meteor_room_dragon = new NBTStructure(Constants.MOD_ID, "structures/meteor/room10/room-dragon.nbt");
    public static final NBTStructure meteor_room_ladder = new NBTStructure(Constants.MOD_ID, "structures/meteor/room10/room-ladder.nbt");
    public static final NBTStructure meteor_room_ooze = new NBTStructure(Constants.MOD_ID, "structures/meteor/room10/room-ooze.nbt");
    public static final NBTStructure meteor_room_split = new NBTStructure(Constants.MOD_ID, "structures/meteor/room10/room-split.nbt");
    public static final NBTStructure meteor_room_stairs = new NBTStructure(Constants.MOD_ID, "structures/meteor/room10/room-stairs.nbt");
    public static final NBTStructure meteor_room_triple = new NBTStructure(Constants.MOD_ID, "structures/meteor/room10/room-triple.nbt");
    public static final NBTStructure meteor_room_fallback = new NBTStructure(Constants.MOD_ID, "structures/meteor/room10/room-fallback.nbt");

    public static final NBTStructure meteor_dragon_chest = new NBTStructure(Constants.MOD_ID, "structures/meteor/room10/headloot/loot-chest.nbt");
    public static final NBTStructure meteor_dragon_tesla = new NBTStructure(Constants.MOD_ID, "structures/meteor/room10/headloot/loot-tesla.nbt");
    public static final NBTStructure meteor_dragon_trap = new NBTStructure(Constants.MOD_ID, "structures/meteor/room10/headloot/loot-trap.nbt");
    public static final NBTStructure meteor_dragon_crate_crab = new NBTStructure(Constants.MOD_ID, "structures/meteor/room10/headloot/loot-crate-crab.nbt");
    public static final NBTStructure meteor_dragon_fallback = new NBTStructure(Constants.MOD_ID, "structures/meteor/room10/headloot/loot-fallback.nbt");

    public static final NBTStructure vertibird = new NBTStructure(Constants.MOD_ID, "structures/vertibird.nbt");
    public static final NBTStructure crashed_vertibird = new NBTStructure(Constants.MOD_ID, "structures/crashed-vertibird.nbt");
    public static final NBTStructure aircraft_carrier = new NBTStructure(Constants.MOD_ID, "structures/aircraft_carrier.nbt");
    public static final NBTStructure oil_rig = new NBTStructure(Constants.MOD_ID, "structures/oil_rig.nbt");
    public static final NBTStructure beached_patrol = new NBTStructure(Constants.MOD_ID, "structures/beached_patrol.nbt");
    public static final NBTStructure lighthouse = new NBTStructure(Constants.MOD_ID, "structures/lighthouse.nbt");
    public static final NBTStructure dish = new NBTStructure(Constants.MOD_ID, "structures/dish.nbt");
    public static final NBTStructure water_pump = new NBTStructure(Constants.MOD_ID, "structures/water_pump.nbt");
    public static final NBTStructure dead_dish_small = new NBTStructure(Constants.MOD_ID, "structures/dead_dish_small.nbt");
    public static final NBTStructure desert_shack_1 = new NBTStructure(Constants.MOD_ID, "structures/desert_shack_1.nbt");
    public static final NBTStructure desert_shack_2 = new NBTStructure(Constants.MOD_ID, "structures/desert_shack_2.nbt");
    public static final NBTStructure desert_shack_3 = new NBTStructure(Constants.MOD_ID, "structures/desert_shack_3.nbt");
    public static final NBTStructure laboratory = new NBTStructure(Constants.MOD_ID, "structures/laboratory.nbt");
    public static final NBTStructure radio_house = new NBTStructure(Constants.MOD_ID, "structures/radio_house.nbt");

    public static final NBTStructure ntmruinsA = new NBTStructure(Constants.MOD_ID, "structures/ntmruinsa.nbt");
    public static final NBTStructure ntmruinsB = new NBTStructure(Constants.MOD_ID, "structures/ntmruinsb.nbt");
    public static final NBTStructure ntmruinsC = new NBTStructure(Constants.MOD_ID, "structures/ntmruinsc.nbt");
    public static final NBTStructure ntmruinsD = new NBTStructure(Constants.MOD_ID, "structures/ntmruinsd.nbt");
    public static final NBTStructure ntmruinsE = new NBTStructure(Constants.MOD_ID, "structures/ntmruinse.nbt");
    public static final NBTStructure ntmruinsF = new NBTStructure(Constants.MOD_ID, "structures/ntmruinsf.nbt");
    public static final NBTStructure ntmruinsG = new NBTStructure(Constants.MOD_ID, "structures/ntmruinsg.nbt");
    public static final NBTStructure ntmruinsH = new NBTStructure(Constants.MOD_ID, "structures/ntmruinsh.nbt");
    public static final NBTStructure ntmruinsI = new NBTStructure(Constants.MOD_ID, "structures/ntmruinsi.nbt");
    public static final NBTStructure ntmruinsJ = new NBTStructure(Constants.MOD_ID, "structures/ntmruinsj.nbt");

    public static final NBTStructure forest_post = new NBTStructure(Constants.MOD_ID, "structures/forest_post.nbt");

    public static final NBTStructure forest_chem = new NBTStructure(Constants.MOD_ID, "structures/forest_chem.nbt");
    public static final NBTStructure plane1 = new NBTStructure(Constants.MOD_ID, "structures/crashed_plane_1.nbt");
    public static final NBTStructure plane2 = new NBTStructure(Constants.MOD_ID, "structures/crashed_plane_2.nbt");
    public static final NBTStructure tower_base = new NBTStructure(Constants.MOD_ID, "structures/tower_base.nbt");

    public static final NBTStructure factory = new NBTStructure(Constants.MOD_ID, "structures/factory.nbt");
    public static final NBTStructure crane = new NBTStructure(Constants.MOD_ID, "structures/crane_mod.nbt");
    public static final NBTStructure broadcasting_tower = new NBTStructure(Constants.MOD_ID, "structures/broadcasting_tower.nbt");

    //public static final NBTStructure excavator = new NBTStructure(Constants.MOD_ID, "structures/excavator.nbt");
    public static final NBTStructure repeater_radio = new NBTStructure(Constants.MOD_ID, "structures/repeater_radio.nbt");

    public static final NBTStructure spire = new NBTStructure(Constants.MOD_ID, "structures/spire.nbt");

    public static final NBTStructure falloutshelter_house = new NBTStructure(Constants.MOD_ID, "structures/falloutshelter_house.nbt");
    public static final NBTStructure abandoned_arcticstation = new NBTStructure(Constants.MOD_ID, "structures/abandoned_arcticstation.nbt");
    public static final NBTStructure military_base = new NBTStructure(Constants.MOD_ID, "structures/military_base.nbt");
    public static final NBTStructure submarine = new NBTStructure(Constants.MOD_ID, "structures/submarine.nbt");

    //public static final NBTStructure test_rot = new NBTStructure(Constants.MOD_ID, "structures/test-rot.nbt");
    //public static final NBTStructure test_jigsaw = new NBTStructure(Constants.MOD_ID, "structures/test-jigsaw.nbt");
    //public static final NBTStructure test_jigsaw_core = new NBTStructure(Constants.MOD_ID, "structures/test-jigsaw-core.nbt");
    //public static final NBTStructure test_jigsaw_hall = new NBTStructure(Constants.MOD_ID, "structures/test-jigsaw-hall.nbt");
    //public static final NBTStructure test_tandem_core = new NBTStructure(Constants.MOD_ID, "structures/test-tandem-core.nbt");
    //public static final NBTStructure test_tandem = new NBTStructure(Constants.MOD_ID, "structures/test-tandem.nbt");
}
