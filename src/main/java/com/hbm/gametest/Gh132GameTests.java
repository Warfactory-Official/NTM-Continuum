package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.util.TagCategories;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.ArmorSlot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/**
 * GitHub issue 132: the shipped category tags hold exactly what {@link TagCategories} says, both ways, so an item or
 * block that drifts from its category (or a generator change that drops one) fails here.
 */
public final class Gh132GameTests {

    private Gh132GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function ITEMS =
            TESTS.function("gh132_item_category_tags_match", 20, () -> Gh132GameTests::items);
    public static final GameTests.Function BLOCKS =
            TESTS.function("gh132_block_category_tags_match", 20, () -> Gh132GameTests::blocks);
    public static final GameTests.Function SPOT =
            TESTS.function("gh132_tags_spot_checks", 20, () -> Gh132GameTests::spotChecks);

    public static void init() {}

    private static void items(GameTestHelper helper) {
        List<String> wrong = new ArrayList<>();
        Set<Item> ammo = TagCategories.ammo(Constants.MOD_ID);
        Set<Item> bombs = TagCategories.tab(HbmTab.BOMBS, Constants.MOD_ID), missiles = TagCategories.tab(HbmTab.MISSILES, Constants.MOD_ID),
                machines = TagCategories.tab(HbmTab.MACHINES, Constants.MOD_ID);
        boolean vanillaArmorTags = new ItemStack(Items.IRON_HELMET).is(item("minecraft:head_armor"));
        for (Item item : BuiltInRegistries.ITEM) {
            if (!TagCategories.isFrom(Constants.MOD_ID, item)) continue;
            ItemStack stack = new ItemStack(item);
            check(wrong, stack, "hbm:guns", TagCategories.isGun(item));
            check(wrong, stack, "c:tools/ranged_weapon", TagCategories.isGun(item));
            check(wrong, stack, "hbm:ammo", ammo.contains(item));
            check(wrong, stack, "hbm:food", TagCategories.isFood(item));
            check(wrong, stack, "c:foods", TagCategories.isFood(item));
            ArmorSlot slot = TagCategories.armorSlot(item);
            check(wrong, stack, "hbm:armor", slot != null);
            check(wrong, stack, "c:armors", slot != null);
            if (vanillaArmorTags) {
                check(wrong, stack, "minecraft:head_armor", slot == ArmorSlot.HELMET);
                check(wrong, stack, "minecraft:chest_armor", slot == ArmorSlot.CHESTPLATE);
                check(wrong, stack, "minecraft:leg_armor", slot == ArmorSlot.LEGGINGS);
                check(wrong, stack, "minecraft:foot_armor", slot == ArmorSlot.BOOTS);
            }
            check(wrong, stack, "hbm:bombs", bombs.contains(item));
            check(wrong, stack, "hbm:missiles", missiles.contains(item));
            check(wrong, stack, "hbm:machines", machines.contains(item));
            Set<String> hazards = TagCategories.hazards(item, TagCategories.HAZARDS);
            for (String hazard : TagCategories.HAZARDS.keySet()) {
                check(wrong, stack, "hbm:hazardous/" + hazard, hazards.contains(hazard));
            }
            check(wrong, stack, "hbm:hazardous", !hazards.isEmpty());
            if (item instanceof BlockItem block) {
                Set<String> tiers = TagCategories.blastTiers(block.getBlock());
                for (String tier : TagCategories.BLAST_TIERS.keySet()) {
                    check(wrong, stack, "hbm:blast_resistant/" + tier, tiers.contains(tier));
                }
            }
        }
        report(helper, wrong);
    }

    private static void blocks(GameTestHelper helper) {
        List<String> wrong = new ArrayList<>();
        Set<Block> machines = new HashSet<>();
        for (Item item : TagCategories.tab(HbmTab.MACHINES, Constants.MOD_ID)) if (item instanceof BlockItem block) machines.add(block.getBlock());
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!TagCategories.isFrom(Constants.MOD_ID, block)) continue;
            Set<String> tiers = TagCategories.blastTiers(block);
            for (Map.Entry<String, Float> tier : TagCategories.BLAST_TIERS.entrySet()) {
                boolean tagged = block.defaultBlockState().is(block("hbm:blast_resistant/" + tier.getKey()));
                if (tagged != tiers.contains(tier.getKey())) wrong.add(name(block) + " hbm:blast_resistant/" + tier.getKey() + " " + tagged);
                if (tagged && (block.getExplosionResistance() < tier.getValue() || TagCategories.blastExcluded(block))) {
                    wrong.add(name(block) + " is tagged " + tier.getKey() + " at " + block.getExplosionResistance());
                }
            }
            if (block.defaultBlockState().is(block("hbm:blast_resistant")) != !tiers.isEmpty()) wrong.add(name(block) + " hbm:blast_resistant");
            if (block.defaultBlockState().is(block("hbm:machines")) != machines.contains(block)) wrong.add(name(block) + " hbm:machines");
            if (block.defaultBlockState().is(block("hbm:radiation_shielding")) != TagCategories.shieldsRadiation(block)) {
                wrong.add(name(block) + " hbm:radiation_shielding");
            }
            Set<String> hazards = TagCategories.hazards(block, TagCategories.HAZARDS);
            for (String hazard : TagCategories.HAZARDS.keySet()) {
                if (block.defaultBlockState().is(block("hbm:hazardous/" + hazard)) != hazards.contains(hazard)) {
                    wrong.add(name(block) + " hbm:hazardous/" + hazard);
                }
            }
        }
        report(helper, wrong);
    }

    /** Against 1.7's setResistance and HazardRegistry. */
    private static void spotChecks(GameTestHelper helper) {
        tiers(helper, "hbm:concrete", "large_explosion");
        tiers(helper, "hbm:ducrete", "nuclear");
        tiers(helper, "hbm:cmb_brick", "obsidian");
        helper.assertTrue(stack("hbm:ingot_uranium").is(item("hbm:hazardous/radioactive")), "uranium is radioactive");
        helper.assertTrue(stack("hbm:ingot_pu238").is(item("hbm:hazardous/hot")), "Pu-238 is hot");
        helper.assertTrue(stack("hbm:powder_coal").is(item("hbm:hazardous/coal_dust")), "coal dust is coal dust");
        helper.assertFalse(stack("hbm:ingot_steel").is(item("hbm:hazardous")), "steel is harmless");
        helper.assertFalse(stack("hbm:nuke_boy").is(item("hbm:blast_resistant")), "a bomb is not blast resistant");
        helper.succeed();
    }

    private static void tiers(GameTestHelper helper, String id, String highest) {
        ItemStack stack = stack(id);
        boolean below = true;
        for (String tier : TagCategories.BLAST_TIERS.keySet()) {
            helper.assertTrue(stack.is(item("hbm:blast_resistant/" + tier)) == below, id + " in " + tier + ": " + below);
            if (tier.equals(highest)) below = false;
        }
    }

    private static void check(List<String> wrong, ItemStack stack, String tag, boolean expected) {
        if (stack.is(item(tag)) != expected) wrong.add(BuiltInRegistries.ITEM.getKey(stack.getItem()) + " " + tag + " " + !expected);
    }

    private static void report(GameTestHelper helper, List<String> wrong) {
        helper.assertTrue(wrong.isEmpty(), wrong.size() + " drifted: " + wrong.subList(0, Math.min(10, wrong.size())));
        helper.succeed();
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Ids.parse(id)));
    }

    private static String name(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private static TagKey<Item> item(String id) {
        return TagKey.create(Registries.ITEM, Ids.parse(id));
    }

    private static TagKey<Block> block(String id) {
        return TagKey.create(Registries.BLOCK, Ids.parse(id));
    }
}
