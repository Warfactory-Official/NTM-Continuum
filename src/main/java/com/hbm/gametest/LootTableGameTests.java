package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.PoolLoot;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.Entities;
import dev.golem.core.world.ChestLoot;
import dev.golem.core.world.CompatEnchantments;
import dev.golem.core.world.EntityLoot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The structure loot tables and the looting mob loot tables against the Java they replaced: each table rolled many times,
 * every mean within six standard errors of the 1.7 formula and every count inside its 1.7 range.
 */
public final class LootTableGameTests {

    private LootTableGameTests() {}

    private static final int ROLLS = 4000;

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function CHEST_TABLES =
            TESTS.function("loot_chest_tables_match_pools", 200, () -> LootTableGameTests::chestTablesMatchPools);
    public static final GameTests.Function STRUCTURE_FILL =
            TESTS.function("loot_structure_fill", 20, () -> LootTableGameTests::structureFill);
    public static final GameTests.Function MOB_TABLES =
            TESTS.function("loot_mob_tables_match_formulas", 200, () -> LootTableGameTests::mobTablesMatchFormulas);

    public static void init() {}

    /** Every item pool has its table, which picks each item as often as the pool's weights say, in its count range. */
    public static void chestTablesMatchPools(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO));
        RandomSource seeds = RandomSource.create(0x5EEDL);
        List<String> failures = new ArrayList<>();
        for (String pool : new TreeSet<>(ItemPool.POOLS.keySet())) {
            String table = PoolLoot.table(pool);
            if (!ChestLoot.exists(level, table)) {
                failures.add(table + " missing");
                continue;
            }
            Map<Item, double[]> expected = new HashMap<>(); // item: probability, min count, max count
            int total = 0;
            for (ItemPool.Entry entry : ItemPool.getPool(pool)) total += entry.weight();
            for (ItemPool.Entry entry : ItemPool.getPool(pool)) {
                if (entry.isEmpty()) continue;
                double[] e = expected.computeIfAbsent(entry.template().getItem(), k -> new double[] {0, Integer.MAX_VALUE, 0});
                e[0] += (double) entry.weight() / total;
                e[1] = Math.min(e[1], entry.min());
                e[2] = Math.max(e[2], entry.max());
            }
            Map<Item, int[]> seen = new HashMap<>();
            for (int roll = 0; roll < ROLLS; roll++) {
                for (ItemStack stack : ChestLoot.rollUnsplit(level, table, origin, seeds.nextLong())) {
                    double[] e = expected.get(stack.getItem());
                    if (e == null) {
                        failures.add(table + " rolled " + stack.getItem() + ", not in " + pool);
                    } else if (stack.getCount() < e[1] || stack.getCount() > e[2]) {
                        failures.add(table + " rolled " + stack.getCount() + " " + stack.getItem());
                    }
                    seen.computeIfAbsent(stack.getItem(), k -> new int[1])[0]++;
                }
            }
            for (Map.Entry<Item, double[]> e : expected.entrySet()) {
                double p = e.getValue()[0];
                int count = seen.getOrDefault(e.getKey(), new int[1])[0];
                if (Math.abs(count - p * ROLLS) > 6 * Math.sqrt(ROLLS * p * (1 - p)) + 2) {
                    failures.add(table + " " + e.getKey() + " in " + count + " of " + ROLLS + " rolls, expected " + p * ROLLS);
                }
            }
        }
        helper.assertTrue(failures.isEmpty(), "chest tables differ from their item pools: " + failures);
        helper.succeed();
    }

    /** A structure fill puts the pool's items into the container, through the table. */
    public static void structureFill(GameTestHelper helper) {
        SimpleContainer chest = new SimpleContainer(27);
        PoolLoot.fill(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), chest, "POOL_MACHINE_PARTS", RandomSource.create(7L), 10);
        helper.assertFalse(chest.isEmpty(), "a structure fill from POOL_MACHINE_PARTS left the chest empty");
        helper.succeed();
    }

    public static void mobTablesMatchFormulas(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> failures = new ArrayList<>();
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        DamageSource none = level.damageSources().generic();

        for (int looting : new int[] {0, 3}) {
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            CompatEnchantments.enchantLooting(level, sword, looting);
            player.setItemInHand(InteractionHand.MAIN_HAND, sword);
            DamageSource kill = level.damageSources().playerAttack(player);
            String l = " looting " + looting;

            Rolls gold = roll(helper, "entity_mob_gold_creeper", kill, player, false);
            gold.expect(failures, "gold creeper" + l, "hbm:crystal_gold", 7.5 + looting, 5, 10 + 2 * looting);
            Rolls nuclear = roll(helper, "entity_mob_nuclear_creeper", kill, player, false);
            nuclear.expect(failures, "nuclear creeper" + l, "minecraft:tnt", 1 + looting / 2.0, 0, 2 + looting);
            nuclear.expect(failures, "nuclear creeper" + l, "hbm:coin_creeper", 1 / 3.0, 0, 1);
            roll(helper, "entity_mob_phosgene_creeper", kill, player, false)
                    .expect(failures, "phosgene creeper" + l, "minecraft:gunpowder", 1 + looting / 2.0, 0, 2 + looting);
            roll(helper, "entity_mob_tainted_creeper", kill, player, false)
                    .expect(failures, "tainted creeper" + l, "minecraft:tnt", 1 + looting / 2.0, 0, 2 + looting);
            Rolls duck = roll(helper, "entity_fucc_a_ducc", kill, player, false);
            duck.expect(failures, "duck" + l, "minecraft:feather", 1 + looting / 2.0, 0, 2 + looting);
            duck.expect(failures, "duck" + l, "minecraft:chicken", 1, 1, 1);
            roll(helper, "entity_pigeon", kill, player, false)
                    .expect(failures, "pigeon" + l, "minecraft:feather", 1 + looting / 2.0, 0, 2 + looting);
            roll(helper, "entity_radiation_blaze", kill, player, false)
                    .expect(failures, "rad beast" + l, "hbm:nugget_polonium", looting, looting, looting);
            Rolls taint = roll(helper, "entity_taint_crab", kill, player, false);
            taint.expect(failures, "taint crab" + l, "hbm:coil_copper", 1 + looting / 2.0, 0, 2 + looting);
            taint.expect(failures, "taint crab" + l, "hbm:coil_magnetized_tungsten", (5 + looting) / 200.0, 0, 1);
            roll(helper, "entity_tesla_crab", kill, player, false)
                    .expect(failures, "tesla crab" + l, "hbm:coil_copper", (5 + looting) / 200.0, 0, 1);
            glyphid(helper, failures, "entity_glyphid", 1, looting, kill, player);
            glyphid(helper, failures, "entity_glyphid_scout", 0, looting, kill, player);
            glyphid(helper, failures, "entity_glyphid_brawler", 1, looting, kill, player);
            glyphid(helper, failures, "entity_glyphid_digger", 1, looting, kill, player);
            glyphid(helper, failures, "entity_glyphid_bombardier", 1, looting, kill, player);
            glyphid(helper, failures, "entity_glyphid_blaster", 1, looting, kill, player);
            glyphid(helper, failures, "entity_glyphid_behemoth", 1, looting, kill, player);
            glyphid(helper, failures, "entity_glyphid_brenda", 2, looting, kill, player);
            glyphid(helper, failures, "entity_glyphid_nuclear", 2, looting, kill, player);
        }

        // no player: the player-only drops stop, the gold creeper drops 3
        roll(helper, "entity_mob_gold_creeper", none, null, false).expect(failures, "gold creeper unattended", "hbm:crystal_gold", 3, 3, 3);
        roll(helper, "entity_radiation_blaze", none, null, false).expect(failures, "rad beast unattended", "hbm:nugget_polonium", 0, 0, 0);
        roll(helper, "entity_taint_crab", none, null, false).expect(failures, "taint crab unattended", "hbm:coil_magnetized_tungsten", 0, 0, 0);
        roll(helper, "entity_tesla_crab", none, null, false).expect(failures, "tesla crab unattended", "hbm:coil_copper", 0, 0, 0);

        // a burning duck drops cooked chicken; a skeleton's kill drops a music disc
        Rolls burning = roll(helper, "entity_fucc_a_ducc", none, null, true);
        burning.expect(failures, "burning duck", "minecraft:cooked_chicken", 1, 1, 1);
        burning.expect(failures, "burning duck", "minecraft:chicken", 0, 0, 0);
        LivingEntity skeleton = (LivingEntity) Entities.createDetached(EntityType.SKELETON, level);
        Rolls shot = roll(helper, "entity_mob_volatile_creeper", level.damageSources().mobAttack(skeleton), null, false);
        shot.expect(failures, "volatile creeper", "hbm:sulfur", 3, 2, 4);
        shot.expect(failures, "volatile creeper", "hbm:stick_tnt", 1.5, 1, 2);
        int discs = 0;
        for (Map.Entry<Item, int[]> e : shot.counts.entrySet()) {
            if (new ItemStack(e.getKey()).is(ItemTags.CREEPER_DROP_MUSIC_DISCS)) for (int c : e.getValue()) discs += c;
        }
        if (discs != ROLLS) failures.add("skeleton-killed creeper dropped " + discs + " discs in " + ROLLS + " deaths");

        helper.assertTrue(failures.isEmpty(), "mob loot differs from the 1.7 formulas: " + failures);
        helper.succeed();
    }

    /** Half the time {@code scale * 2 + looting} meat, grilled when the glyphid burns. */
    private static void glyphid(GameTestHelper helper, List<String> failures, String id, int scale, int looting,
                                DamageSource kill, ServerPlayer player) {
        int meat = scale * 2 + looting;
        roll(helper, id, kill, player, false).expect(failures, id + " looting " + looting, "hbm:glyphid_meat", meat / 2.0, 0, meat);
        Rolls burning = roll(helper, id, kill, player, true);
        burning.expect(failures, id + " burning", "hbm:glyphid_meat_grilled", meat / 2.0, 0, meat);
        burning.expect(failures, id + " burning", "hbm:glyphid_meat", 0, 0, 0);
    }

    private static Rolls roll(GameTestHelper helper, String id, DamageSource source, @Nullable ServerPlayer player, boolean burning) {
        ServerLevel level = helper.getLevel();
        LivingEntity victim = (LivingEntity) Entities.createDetached(Entities.byId(Constants.MOD_ID + ":" + id), level);
        Vec3 at = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
        victim.setPos(at.x, at.y, at.z);
        if (burning) victim.setRemainingFireTicks(100);
        Rolls rolls = new Rolls();
        for (int roll = 0; roll < ROLLS; roll++) {
            for (ItemStack stack : EntityLoot.roll(level, Constants.MOD_ID + ":entities/" + id, victim, source, player)) {
                rolls.counts.computeIfAbsent(stack.getItem(), k -> new int[ROLLS])[roll] += stack.getCount();
            }
        }
        return rolls;
    }

    private static final class Rolls {
        final Map<Item, int[]> counts = new HashMap<>();

        void expect(List<String> failures, String label, String item, double mean, int min, int max) {
            int[] perRoll = counts.getOrDefault(BuiltInRegistries.ITEM.getValue(Ids.parse(item)), new int[ROLLS]);
            double sum = 0;
            double squares = 0;
            int low = Integer.MAX_VALUE;
            int high = Integer.MIN_VALUE;
            for (int c : perRoll) {
                sum += c;
                squares += (double) c * c;
                low = Math.min(low, c);
                high = Math.max(high, c);
            }
            double observed = sum / ROLLS;
            double error = Math.sqrt(Math.max(squares / ROLLS - observed * observed, 0) / ROLLS);
            if (Math.abs(observed - mean) > 6 * error + 0.01) failures.add(label + " " + item + " mean " + observed + ", expected " + mean);
            if (low < min || high > max) failures.add(label + " " + item + " ranged " + low + ".." + high + ", expected " + min + ".." + max);
        }
    }
}
