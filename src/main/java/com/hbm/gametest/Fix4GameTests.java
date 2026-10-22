package com.hbm.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.Constants;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPools;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.ItemFluidTank;
import com.hbm.items.special.ItemCell;
import com.hbm.items.tool.ItemBlowtorch;
import com.mojang.serialization.JsonOps;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.ChestLoot;
import dev.golem.core.world.CompatEnchantments;
import dev.golem.core.world.ItemStacks;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Block drops and chest loot against 1.7
 * (1.7.10). Block drops are sampled through {@code Block.getDrops} with a plain, a Fortune III and a Silk Touch
 * diamond pickaxe, and checked against the 1.7 range and the analytic mean of the 1.7 formula. The 1.7 ore
 * fortune multiplier is {@code max(rand(f + 2) - 1, 0) + 1}; at Fortune III it is 1, 1, 2, 3, 4 with equal
 * probability, mean 2.2.
 */
public final class Fix4GameTests {

    private Fix4GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function ORE_DROPS =
            TESTS.function("fix4_ore_drops", 40, () -> Fix4GameTests::oreDrops);
    public static final GameTests.Function SILK_TOUCH =
            TESTS.function("fix4_silk_touch", 40, () -> Fix4GameTests::silkTouch);
    public static final GameTests.Function SPECIAL_DROPS =
            TESTS.function("fix4_special_drops", 40, () -> Fix4GameTests::specialDrops);
    public static final GameTests.Function ITEM_POOLS =
            TESTS.function("fix4_item_pools", 40, () -> Fix4GameTests::itemPools);
    public static final GameTests.Function CHEST_INJECTION =
            TESTS.function("fix4_chest_injection", 40, () -> Fix4GameTests::chestInjection);

    private static final int TRIALS = 600;
    private static final double FORTUNE_III = 2.2D;

    public static void init() {}

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Ids.parse(id));
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(Ids.parse(id)).orElseThrow(() -> new IllegalStateException("no block " + id));
    }

    private enum Tool { PLAIN, FORTUNE, SILK }

    private static ItemStack tool(ServerLevel level, Tool tool) {
        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        if (tool == Tool.FORTUNE) CompatEnchantments.enchantFortune(level, pick, 3);
        if (tool == Tool.SILK) CompatEnchantments.enchantSilkTouch(level, pick);
        return pick;
    }

    private static List<ItemStack> drops(GameTestHelper helper, BlockState state, Tool tool) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        return Block.getDrops(state, level, pos, null, null, tool(level, tool));
    }

    /**
     * {@code TRIALS} breaks: every drop must be one of {@code allowed}, the per-break total of {@code counted}
     * within {@code min..max}, and its mean within 12% (plus 0.05) of {@code mean}.
     */
    private static void sample(GameTestHelper helper, String blockId, Tool tool, Set<String> allowed, String counted,
                               int min, int max, double mean) {
        BlockState state = block(blockId).defaultBlockState();
        Item countedItem = item(counted);
        long total = 0;
        int seenMin = Integer.MAX_VALUE;
        int seenMax = Integer.MIN_VALUE;
        for (int i = 0; i < TRIALS; i++) {
            int n = 0;
            for (ItemStack stack : drops(helper, state, tool)) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                helper.assertTrue(allowed.contains(id), blockId + " (" + tool + ") dropped " + stack + ", 1.7 allows " + allowed);
                if (stack.is(countedItem)) n += stack.getCount();
            }
            total += n;
            seenMin = Math.min(seenMin, n);
            seenMax = Math.max(seenMax, n);
        }
        double avg = (double) total / TRIALS;
        helper.assertTrue(seenMin >= min && seenMax <= max,
                blockId + " (" + tool + "): " + counted + " per break " + seenMin + ".." + seenMax + ", 1.7 " + min + ".." + max);
        helper.assertTrue(Math.abs(avg - mean) <= mean * 0.12D + 0.05D,
                blockId + " (" + tool + "): mean " + avg + " " + counted + ", 1.7 formula " + mean);
    }

    private static void ore(GameTestHelper helper, String blockId, String drop, int min, int max, boolean fortune) {
        double mean = (min + max) / 2.0D;
        sample(helper, blockId, Tool.PLAIN, Set.of(drop), drop, min, max, mean);
        if (fortune) {
            sample(helper, blockId, Tool.FORTUNE, Set.of(drop), drop, min, max * 4, mean * FORTUNE_III);
        } else {
            sample(helper, blockId, Tool.FORTUNE, Set.of(drop), drop, min, max, mean);
        }
    }

    private static void exactly(GameTestHelper helper, String blockId, Tool tool, String drop, int count) {
        List<ItemStack> drops = drops(helper, block(blockId).defaultBlockState(), tool);
        int n = 0;
        for (ItemStack stack : drops) {
            helper.assertTrue(stack.is(item(drop)), blockId + " (" + tool + ") must drop only " + drop + ", got " + drops);
            n += stack.getCount();
        }
        helper.assertTrue(n == count, blockId + " (" + tool + ") must drop " + count + "x " + drop + ", got " + drops);
    }

    private static void nothing(GameTestHelper helper, String blockId, Tool tool) {
        List<ItemStack> drops = drops(helper, block(blockId).defaultBlockState(), tool);
        helper.assertTrue(drops.isEmpty(), blockId + " (" + tool + ") must drop nothing in 1.7, got " + drops);
    }

    /** 1.7 BlockOre, BlockDepthOre, BlockSellafieldOre, WasteLog and friends: identity, counts and fortune. */
    public static void oreDrops(GameTestHelper helper) {
        ore(helper, "hbm:ore_lignite", "hbm:lignite", 1, 1, true);
        ore(helper, "hbm:ore_rare", "hbm:chunk_ore_rare", 1, 1, true);
        ore(helper, "hbm:ore_gneiss_rare", "hbm:chunk_ore_rare", 1, 1, true);
        ore(helper, "hbm:ore_niter", "hbm:niter", 2, 4, true);
        ore(helper, "hbm:ore_sulfur", "hbm:sulfur", 2, 4, true);
        ore(helper, "hbm:ore_cinnabar", "hbm:cinnabar", 1, 1, true);
        // the cobalt ores drop raw cobalt while hbm.cfg's raw ore option is on
        RawOreGameTests.legacyDrops(() -> {
            ore(helper, "hbm:ore_cobalt", "hbm:fragment_cobalt", 4, 9, true);
            ore(helper, "hbm:ore_nether_cobalt", "hbm:fragment_cobalt", 5, 12, true);
        });
        ore(helper, "hbm:ore_coltan", "hbm:fragment_coltan", 1, 1, true);
        ore(helper, "hbm:waste_planks", "minecraft:charcoal", 1, 1, true);
        ore(helper, "hbm:frozen_dirt", "minecraft:snowball", 1, 1, true);
        ore(helper, "hbm:frozen_planks", "minecraft:snowball", 1, 1, true);
        ore(helper, "hbm:ore_sellafield_diamond", "minecraft:diamond", 1, 1, true);
        ore(helper, "hbm:ore_oil", "hbm:oil_tar_crude", 1, 1, true);
        ore(helper, "hbm:ore_nether_coal", "hbm:coal_infernal", 1, 1, true);
        ore(helper, "hbm:waste_trinitite", "hbm:trinitite", 1, 1, false);
        ore(helper, "hbm:waste_trinitite_red", "hbm:trinitite", 1, 1, false);
        ore(helper, "hbm:block_meteor_cobble", "hbm:fragment_meteorite", 1, 1, false);
        ore(helper, "hbm:waste_earth", "minecraft:dirt", 1, 1, false);
        ore(helper, "hbm:waste_mycelium", "minecraft:dirt", 1, 1, false);
        ore(helper, "hbm:ore_nether_smoldering", "hbm:powder_fire", 1, 1, false);
        ore(helper, "hbm:cluster_iron", "hbm:crystal_iron", 1, 1, false);
        // depth ores: every one multiplies by fortune in 1.7
        ore(helper, "hbm:ore_depth_cinnabar", "hbm:cinnabar", 2, 4, true);
        ore(helper, "hbm:ore_depth_zirconium", "hbm:nugget_zirconium", 2, 3, true);
        ore(helper, "hbm:ore_depth_nether_neodymium", "hbm:fragment_neodymium", 2, 3, true);
        ore(helper, "hbm:ore_depth_borax", "hbm:powder_borax", 1, 1, true);
        ore(helper, "hbm:cluster_depth_iron", "hbm:crystal_iron", 1, 1, true);
        // alexandrite: 1 gem, multiplied, clamped to 2 -> 1, 1, 2, 2, 2 at Fortune III
        sample(helper, "hbm:ore_alexandrite", Tool.PLAIN, Set.of("hbm:gem_alexandrite"), "hbm:gem_alexandrite", 1, 1, 1.0D);
        sample(helper, "hbm:ore_alexandrite", Tool.FORTUNE, Set.of("hbm:gem_alexandrite"), "hbm:gem_alexandrite", 1, 2, 1.6D);
        // nether phosphorus: one item per multiplied unit, each 1 in 10 phosphorus
        Set<String> fire = Set.of("hbm:powder_fire", "hbm:ingot_phosphorus");
        sample(helper, "hbm:ore_nether_fire", Tool.PLAIN, fire, "hbm:powder_fire", 0, 1, 0.9D);
        sample(helper, "hbm:ore_nether_fire", Tool.PLAIN, fire, "hbm:ingot_phosphorus", 0, 1, 0.1D);
        // waste log: 2-4 charcoal, no fortune; burnt bark is a 1-in-1000 replacement
        Set<String> log = Set.of("minecraft:charcoal", "hbm:burnt_bark");
        sample(helper, "hbm:waste_log", Tool.PLAIN, log, "minecraft:charcoal", 0, 4, 3.0D);
        sample(helper, "hbm:waste_log", Tool.FORTUNE, log, "minecraft:charcoal", 0, 4, 3.0D);
        // glyphid spawner: 1 + rand(3) + fortune eggs
        sample(helper, "hbm:glyphid_spawner", Tool.PLAIN, Set.of("hbm:egg_glyphid"), "hbm:egg_glyphid", 1, 3, 2.0D);
        sample(helper, "hbm:glyphid_spawner", Tool.FORTUNE, Set.of("hbm:egg_glyphid"), "hbm:egg_glyphid", 4, 6, 5.0D);
        helper.succeed();
    }

    /** 1.7 canSilkHarvest: plain opaque cubes without a tile entity give themselves; the exceptions do not. */
    public static void silkTouch(GameTestHelper helper) {
        String[] selfUnderSilk = {"hbm:ore_lignite", "hbm:ore_rare", "hbm:ore_nether_fire", "hbm:ore_cobalt",
                "hbm:waste_planks", "hbm:frozen_dirt", "hbm:waste_earth", "hbm:waste_mycelium", "hbm:waste_trinitite",
                "hbm:waste_log", "hbm:block_meteor_treasure", "hbm:ore_depth_borax", "hbm:ore_alexandrite",
                "hbm:ore_sellafield_diamond", "hbm:ore_sellafield_emerald", "hbm:ore_sellafield_radgem",
                "hbm:ore_nether_coal", "hbm:ore_nether_smoldering", "hbm:cluster_iron", "hbm:stone_resource_malachite",
                "hbm:reinforced_glass", "hbm:glass_uranium", "hbm:glass_trinitite", "hbm:glass_polonium",
                "hbm:glass_ash", "hbm:glass_polarized", "hbm:hadron_plating_glass", "hbm:hadron_analysis_glass",
                "hbm:reinforced_glass_pane", "hbm:reinforced_laminate_pane", "hbm:machine_boiler_off",
                "hbm:block_meteor_molten", "hbm:taint", "hbm:det_miner", "hbm:oil_pipe", "hbm:barricade",
                "hbm:brick_jungle_fragile", "hbm:burning_earth", "hbm:impact_dirt", "hbm:crate_jungle"};
        for (String id : selfUnderSilk) exactly(helper, id, Tool.SILK, id, 1);
        // the same glass without silk touch drops nothing (1.7 doesDrop = false; both panes always)
        for (String id : new String[]{"hbm:reinforced_glass", "hbm:glass_uranium", "hbm:glass_polarized",
                "hbm:reinforced_glass_pane", "hbm:reinforced_laminate_pane", "hbm:taint", "hbm:oil_pipe"}) {
            nothing(helper, id, Tool.PLAIN);
        }
        // doesDrop = true glass drops itself either way
        exactly(helper, "hbm:reinforced_laminate", Tool.PLAIN, "hbm:reinforced_laminate", 1);
        exactly(helper, "hbm:glass_quartz", Tool.PLAIN, "hbm:glass_quartz", 1);
        // no silk touch in 1.7: ore_oil (explicit), stalagmites (not a normal block), layers, waste leaves
        exactly(helper, "hbm:ore_oil", Tool.SILK, "hbm:oil_tar_crude", 1);
        exactly(helper, "hbm:stalagmite_sulfur", Tool.SILK, "hbm:sulfur", 1);
        for (String id : new String[]{"hbm:foam_layer", "hbm:sand_boron_layer", "hbm:leaves_layer", "hbm:oil_spill",
                "hbm:waste_leaves"}) {
            nothing(helper, id, Tool.SILK);
            nothing(helper, id, Tool.PLAIN);
        }
        helper.succeed();
    }

    /** 1.7 breaks a stalagmite with drops when its floor goes. */
    public static void stalagmitePopDrops(GameTestHelper helper) {
        BlockPos floor = new BlockPos(1, 1, 1);
        BlockPos spike = floor.above();
        helper.setBlock(floor, Blocks.STONE);
        helper.setBlock(spike, block("hbm:stalagmite_sulfur"));
        helper.setBlock(floor, Blocks.AIR);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(spike)).isAir(), "the stalagmite pops with its floor");
            List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(
                    ItemEntity.class, new AABB(helper.absolutePos(spike)).inflate(3));
            helper.assertTrue(items.stream().anyMatch(i -> i.getItem().is(item("hbm:sulfur"))), "it drops its sulfur: " + items);
            helper.succeed();
        });
    }

    /** Drops with their own 1.7 logic: the meteorite treasure pool, the boiler's scraps, the self-dropping blocks. */
    public static void specialDrops(GameTestHelper helper) {
        BlockState treasure = block("hbm:block_meteor_treasure").defaultBlockState();
        for (int i = 0; i < 200; i++) {
            List<ItemStack> drops = drops(helper, treasure, Tool.FORTUNE);
            helper.assertTrue(drops.size() >= 1 && drops.size() <= 3 && drops.stream().noneMatch(ItemStack::isEmpty)
                            && drops.stream().noneMatch(s -> s.is(treasure.getBlock().asItem())),
                    "block_meteor_treasure must draw 1 + rand(3) stacks from POOL_METEORITE_TREASURE, got " + drops);
        }
        sample(helper, "hbm:machine_boiler_off", Tool.PLAIN, Set.of("hbm:scraps"), "hbm:scraps", 3, 6, 4.5D);
        for (String id : new String[]{"hbm:fusion_torus", "hbm:stone_depth", "hbm:stone_depth_nether", "hbm:depth_brick",
                "hbm:depth_dnt"}) {
            exactly(helper, id, Tool.PLAIN, id, 1);
        }
        for (String id : new String[]{"hbm:turret_howard_damaged", "hbm:machine_uf6_tank", "hbm:machine_puf6_tank",
                "hbm:block_meteor_molten", "hbm:glyphid_base"}) {
            nothing(helper, id, Tool.PLAIN);
        }
        helper.succeed();
    }

    /**
     * Every row of the baked pools names a registered item, its components decode and its fluid fills, so no entry
     * is silently dropped (a dropped row shrinks the pool and its total weight); spot checks of the flattened 1.7
     * identities, including the component-carried ones on every node.
     */
    public static void itemPools(GameTestHelper helper) {
        // the pools as loaded from data/hbm/item_pool (the 1.7 tables ntm-next baked, split into one file per pool)
        Map<String, JsonArray> root = ItemPool.rawPools();
        helper.assertTrue(root.size() >= 50, "expected HBM's 50 item pools, loaded " + root.size());
        var registries = helper.getLevel().registryAccess();
        int rows = 0;
        for (Map.Entry<String, JsonArray> pool : root.entrySet()) {
            JsonArray body = pool.getValue();
            List<ItemPool.Entry> read = ItemPool.read(registries, pool.getKey());
            helper.assertTrue(read.size() == body.size(),
                    pool.getKey() + ": " + body.size() + " rows but only " + read.size() + " resolve");
            for (int i = 0; i < body.size(); i++) {
                JsonObject row = body.get(i).getAsJsonObject();
                if (row.has("components")) {
                    ItemStack bare = new ItemStack(item(row.get("item").getAsString()));
                    helper.assertTrue(ItemStacks.withComponents(bare, row.getAsJsonObject("components"),
                                    RegistryOps.create(JsonOps.INSTANCE, registries)).isPresent(),
                            pool.getKey() + "[" + i + "] components do not decode: " + row);
                }
                rows++;
            }
        }
        helper.assertTrue(root.containsKey(ItemPools.POOL_PILE_SUPPLIES) && root.get(ItemPools.POOL_PILE_SUPPLIES).size() == 7,
                "POOL_PILE_SUPPLIES must be baked with its seven 1.7 rows");

        Map<String, List<ItemPool.Entry>> pools = new HashMap<>();
        Function<String, List<ItemPool.Entry>> get = name -> pools.computeIfAbsent(name, n -> ItemPool.read(registries, n));
        helper.assertTrue(get.apply(ItemPools.POOL_AMMO).get(0).template().is(item("hbm:ammo_standard_m357_sp")),
                "POOL_AMMO[0] is 1.7 ammo_standard M357_SP");
        ItemStack deuterium = get.apply("POOL_SPACESHIP").get(3).template();
        helper.assertTrue(ItemStacks.sameItemAndData(deuterium, ItemCell.getFullCell(Fluids.DEUTERIUM)),
                "POOL_SPACESHIP[3] is a full deuterium cell, got " + deuterium);
        ItemStack canister = get.apply(ItemPools.POOL_SUPPLIES).get(3).template();
        helper.assertTrue(canister.is(item("hbm:canister_full"))
                        && ItemFluidTank.getType(canister) == Fluids.DIESEL,
                "POOL_SUPPLIES[3] is a diesel canister, got " + canister);
        ItemStack torch = get.apply("POOL_VAULT_LAB").get(0).template();
        helper.assertTrue(torch.getItem() instanceof ItemBlowtorch blowtorch && blowtorch.getFill(torch) == 0,
                "POOL_VAULT_LAB[0] is 1.7 ItemBlowtorch.getEmptyTool, got " + torch);
        ItemStack book = get.apply("POOL_OFFICE_TRASH").get(11).template();
        helper.assertTrue(book.is(item("hbm:book_guide_starter")),
                "POOL_OFFICE_TRASH[11] is book_guide meta 3, book_guide_starter here, got " + book);
        ItemStack ammoBox = get.apply(ItemPools.POOL_PILE_SUPPLIES).get(6).template();
        helper.assertTrue(ammoBox.is(item("hbm:ammo_container")), "POOL_PILE_SUPPLIES[6] is the ammo container");
        helper.assertTrue(rows > 400, "expected the full baked pool set, read " + rows + " rows");
        helper.succeed();
    }

    /** The thirteen 1.7 ChestGenHooks additions appear in the matching vanilla chest tables on this loader. */
    public static void chestInjection(GameTestHelper helper) {
        String[][] expected = {
                {"minecraft:chests/village/village_weaponsmith", "hbm:armor_polish", "hbm:bathwater"},
                {"minecraft:chests/abandoned_mineshaft", "hbm:bathwater", "hbm:serum", "hbm:no9", "hbm:key_red_cracked"},
                {"minecraft:chests/simple_dungeon", "hbm:heart_piece", "hbm:key_red_cracked", "hbm:scrumpy"},
                {"minecraft:chests/desert_pyramid", "hbm:heart_piece", "hbm:scrumpy"},
                {"minecraft:chests/jungle_temple", "hbm:heart_piece"},
                {"minecraft:chests/spawn_bonus_chest", "hbm:no9"},
        };
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        for (String[] table : expected) {
            Map<Item, Integer> seen = new HashMap<>();
            for (int i = 0; i < 600; i++) {
                for (ItemStack stack : ChestLoot.roll(helper.getLevel(), table[0], origin)) {
                    seen.merge(stack.getItem(), stack.getCount(), Integer::sum);
                }
            }
            for (int i = 1; i < table.length; i++) {
                helper.assertTrue(seen.getOrDefault(item(table[i]), 0) > 0,
                        table[0] + " never rolled " + table[i] + " in 600 chests; HBM's 1.7 chest injection is missing");
            }
        }
        helper.succeed();
    }
}
