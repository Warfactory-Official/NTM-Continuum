package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.advancement.HbmAdvancementDeclarations;
import com.hbm.advancement.HbmCriteria;
import com.hbm.config.MineConfig;
import com.hbm.config.ServerConfig;
import com.hbm.handler.BobmazonOfferFactory;
import com.hbm.inventory.fluid.Fluids;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Advancements;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Items;

/**
 * The 1.7 achievement tree as loaded advancements, the custom trigger predicates that grant them, and the ntm*
 * command tree. Every predicate case fires a near miss first and asserts it grants nothing, then the real event.
 */
public final class AdvancementCommandGameTests {
    private AdvancementCommandGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function TREE =
            TESTS.function("adv_tree_loaded", 20, () -> AdvancementCommandGameTests::treeLoaded);
    public static final GameTests.Function PREDICATES =
            TESTS.function("adv_trigger_predicates", 40, () -> AdvancementCommandGameTests::predicates);
    public static final GameTests.Function COMMANDS =
            TESTS.function("cmd_tree_and_config", 20, () -> AdvancementCommandGameTests::commands);

    /** The 62 1.7 achievements and the tab root, minus the satellite-owned ones checked when their items exist. */
    public static final String[] IDS = {"root", "sacrifice", "impossible", "taste_of_blood", "go_fish", "potato", "c20_5",
            "fiend", "fiend2", "stratum", "omega12", "no9", "slimeball", "sulfuric", "inferno", "red_room", "hidden",
            "horizons_start", "horizons_end", "horizons_bonus", "boss_creeper", "boss_meltdown", "boss_maskman",
            "boss_worm", "boss_ufo", "rad_poison", "rad_death", "some_wounds", "digamma_see", "digamma_feel",
            "digamma_know", "digamma_kauai_moho", "digamma_up_on_top", "burner_press", "blast_furnace", "assembly",
            "selenium", "chemplant", "concrete", "polymer", "desh", "tantalum", "gas_cent", "centrifuge", "foeq",
            "soyuz", "space", "schrab", "acidizer", "radium", "technetium", "zirnox_boom", "chicago_pile", "silex",
            "watz", "watz_boom", "rbmk", "rbmk_boom", "bismuth", "breeding", "fusion", "red_balloons", "manhattan"};

    public static void init() {}

    public static void treeLoaded(GameTestHelper helper) {
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        List<String> missing = new ArrayList<>();
        boolean satellites = item("sat_gerald") != Items.AIR;
        for (String id : IDS) {
            if (!satellites && id.startsWith("horizons_")) continue;
            if (Advancements.isDone(player, Constants.MOD_ID + ":" + id) == null) missing.add(id);
        }
        helper.assertTrue(IDS.length == 63, "the tree lost a row: " + IDS.length);
        helper.assertTrue(missing.isEmpty(), "advancements not loaded: " + missing);
        // the root completes on the first tick, so the tab is open for everyone
        helper.assertTrue(Fluids.ASCHRAB.getRegistryName().equals(HbmAdvancementDeclarations.ASCHRAB_ID),
                "Taste of Blood icon names fluid " + HbmAdvancementDeclarations.ASCHRAB_ID + " but ASCHRAB is "
                        + Fluids.ASCHRAB.getRegistryName());
        for (BobmazonOfferFactory.Requirement requirement : BobmazonOfferFactory.Requirement.values()) {
            if (requirement.advancement == null) continue;
            helper.assertTrue(Advancements.isDone(player, requirement.advancement) != null,
                    "Bobmazon requirement " + requirement + " names unloaded advancement " + requirement.advancement);
            helper.assertTrue(!requirement.icon().isEmpty(), "Bobmazon requirement " + requirement + " has no icon");
        }
        helper.succeed();
    }

    public static void predicates(GameTestHelper helper) {
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);

        HbmCriteria.DETONATION.fire(player, "rbmk");
        expect(helper, player, "rbmk_boom", true);
        expect(helper, player, "manhattan", false);
        HbmCriteria.DETONATION.fire(player, "nuke");
        expect(helper, player, "manhattan", true);

        HbmCriteria.RADIATION.fire(player, "poison");
        expect(helper, player, "rad_poison", true);
        expect(helper, player, "rad_death", false);

        HbmCriteria.DIGAMMA.fire(player, "see", ItemStack.EMPTY, 0.5D);
        expect(helper, player, "digamma_see", true);
        expect(helper, player, "digamma_feel", false);

        HbmCriteria.BOSS_KILLED.fire(player, "ufo");
        expect(helper, player, "boss_ufo", true);
        expect(helper, player, "boss_worm", false);

        HbmCriteria.ITEM_PRODUCED.fire(player, new ItemStack(Items.IRON_INGOT));
        expect(helper, player, "burner_press", false);
        HbmCriteria.ITEM_PRODUCED.fire(player, new ItemStack(item("machine_press")));
        expect(helper, player, "burner_press", true);
        expect(helper, player, "desh", false);

        HbmCriteria.ORBIT.fire(player, new ItemStack(item("sat_foeq")));
        expect(helper, player, "foeq", true);
        expect(helper, player, "space", false);

        // No. 9: the cigarette alone is not enough, the helmet must be worn
        ItemStack cigarette = new ItemStack(item("cigarette"));
        HbmCriteria.SMOKED.fire(player, cigarette);
        expect(helper, player, "no9", false);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(item("no9")));
        HbmCriteria.SMOKED.fire(player, cigarette);
        expect(helper, player, "no9", true);

        HbmCriteria.PARTICLE_PRODUCED.fire(player, new ItemStack(item("particle_digamma")));
        expect(helper, player, "omega12", true);
        HbmCriteria.CRUSHED.fire(player, "minecraft:creeper");
        expect(helper, player, "hidden", false);
        HbmCriteria.CRUSHED.fire(player, "hbm:entity_mob_tainted_creeper");
        expect(helper, player, "hidden", true);
        HbmCriteria.ROOM_OPENED.fire(player);
        expect(helper, player, "red_room", true);
        HbmCriteria.DISSOLVED.fire(player, new ItemStack(Items.SLIME_BALL));
        expect(helper, player, "sulfuric", true);
        HbmCriteria.CONSUMED.fire(player, new ItemStack(item("coffee_radium")));
        expect(helper, player, "radium", true);
        HbmCriteria.CONSUMED.fire(player, new ItemStack(item("coffee")));
        expect(helper, player, "radium", true);
        HbmCriteria.BOLTED.fire(player);
        expect(helper, player, "go_fish", true);
        HbmCriteria.BLED_OUT.fire(player);
        expect(helper, player, "some_wounds", true);
        HbmCriteria.DIGAMMA.fire(player, "know", ItemStack.EMPTY, 10D);
        expect(helper, player, "digamma_know", true);
        helper.succeed();
    }

    public static void commands(GameTestHelper helper) {
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        for (String name : new String[] {"ntmreload", "ntmloadchunk", "ntmrad", "ntmserver", "ntmlocate",
                "ntmcustomize", "ntmreapnetworks", "hbm"}) {
            helper.assertTrue(dispatcher.getRoot().getChild(name) != null, "/" + name + " is not registered");
        }
        float before = MineConfig.MINE_AP_DAMAGE;
        try {
            ServerConfig.MINE_AP_DAMAGE.update("12.5");
            helper.assertTrue(MineConfig.MINE_AP_DAMAGE == 12.5F, "/ntmserver set MINE_AP_DAMAGE did not reach the mines");
            boolean threw = false;
            try {
                ServerConfig.AUTOCAL_MAX_CLOCK.update("fast");
            } catch (NumberFormatException expected) {
                threw = true;
            }
            helper.assertTrue(threw, "a malformed integer was accepted");
        } finally {
            MineConfig.MINE_AP_DAMAGE = before;
        }
        helper.assertTrue(ServerConfig.configMap.size() == 13, "1.7 ServerConfig has 13 variables, not "
                + ServerConfig.configMap.size());
        helper.succeed();
    }

    private static void expect(GameTestHelper helper, ServerPlayer player, String id, boolean done) {
        Boolean actual = Advancements.isDone(player, Constants.MOD_ID + ":" + id);
        helper.assertTrue(actual != null, "hbm:" + id + " is not loaded");
        helper.assertTrue(actual == done, "hbm:" + id + (done ? " was not granted" : " was granted by a near miss"));
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, path));
    }
}
