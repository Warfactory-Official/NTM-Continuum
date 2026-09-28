package com.hbm.util;

import com.hbm.Constants;
import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.blocks.generic.BlockLoot.TileEntityLoot;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPools;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.special.ItemBookLore;
import dev.golem.core.Ids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code LootGenerator}: the named item arrangements a loot pile ({@code deco_loot}) is filled with by a
 * structure's loot placeholder.
 *
 * <p>Kept as 1.7 wrote it: {@link #applyLoot}'s switch has no {@code break}s, so a name runs its own generator and
 * then every later one; each generator only adds to an empty pile, so the first that adds anything wins (a nuke
 * storage roll that adds nothing falls through to bones, and so on to the final {@code default}).
 *
 * <p>Gaps: {@code LOOT_METEOR} needs 1.7's {@code MKUCraftingHandler} (the per-world MKU recipe book), which is not
 * ported, so it adds nothing and falls through; {@code POOL_PILE_SUPPLIES} is not in the baked pool file, so
 * {@code LOOT_SUPPLIES} draws from the backup pool.
 */
public final class LootGenerator {

    private LootGenerator() {}

    public static final String LOOT_BOOKLET = "LOOT_BOOKLET";
    public static final String LOOT_CAPNUKE = "LOOT_CAPNUKE";
    public static final String LOOT_MEDICINE = "LOOT_MEDICINE";
    public static final String LOOT_CAPSTASH = "LOOT_CAPSTASH";
    public static final String LOOT_MAKESHIFT_GUN = "LOOT_MAKESHIFT_GUN";
    public static final String LOOT_NUKE_STORAGE = "LOOT_NUKE_STORAGE";
    public static final String LOOT_BONES = "LOOT_BONES";
    public static final String LOOT_GLYPHID_HIVE = "LOOT_GLYPHID_HIVE";
    public static final String LOOT_METEOR = "LOOT_METEOR";
    public static final String LOOT_FLAREGUN = "LOOT_FLAREGUN";
    public static final String LOOT_SHIT = "LOOT_SHIT";
    public static final String LOOT_MECHANICAL = "LOOT_MECHANICAL";
    public static final String LOOT_GEAR = "LOOT_GEAR";
    public static final String LOOT_SUPPLIES = "LOOT_SUPPLIES";

    private static final String POOL_PILE_SUPPLIES = "POOL_PILE_SUPPLIES";

    @SuppressWarnings("fallthrough")
    public static void applyLoot(Level level, BlockPos pos, String name) {
        switch (name) {
            case LOOT_BOOKLET: lootBooklet(level, pos);
            case LOOT_CAPNUKE: lootCapNuke(level, pos);
            case LOOT_MEDICINE: lootMedicine(level, pos);
            case LOOT_CAPSTASH: lootCapStash(level, pos);
            case LOOT_MAKESHIFT_GUN: lootMakeshiftGun(level, pos);
            case LOOT_NUKE_STORAGE: lootNukeStorage(level, pos);
            case LOOT_BONES: lootBones(level, pos);
            case LOOT_GLYPHID_HIVE: lootGlyphidHive(level, pos);
            case LOOT_METEOR: lootBookMeteor(level, pos);
            case LOOT_FLAREGUN: lootFlareGun(level, pos);
            case LOOT_SHIT: lootShit(level, pos);
            case LOOT_MECHANICAL: lootMechanical(level, pos);
            case LOOT_GEAR: lootGear(level, pos);
            case LOOT_SUPPLIES: lootSupplies(level, pos);
            default: lootBones(level, pos); break;
        }
        if (level.getBlockEntity(pos) instanceof TileEntityLoot loot) loot.sync();
    }

    public static String[] getLootNames() {
        return new String[] {
                LOOT_BOOKLET,
                LOOT_CAPNUKE,
                LOOT_MEDICINE,
                LOOT_CAPSTASH,
                LOOT_MAKESHIFT_GUN,
                LOOT_NUKE_STORAGE,
                LOOT_BONES,
                LOOT_GLYPHID_HIVE,
                LOOT_METEOR,
                LOOT_FLAREGUN,
                LOOT_MECHANICAL,
                LOOT_SHIT,
                LOOT_GEAR,
                LOOT_SUPPLIES
        };
    }

    public static void setBlock(Level level, BlockPos pos) {
        level.setBlock(pos, ModDungeonBlocks.deco_loot.get().defaultBlockState(), 3);
    }

    public static void addItemWithDeviation(TileEntityLoot loot, RandomSource rand, ItemStack stack, double x, double y, double z) {
        loot.addItem(stack, x + rand.nextGaussian() * 0.02, y, z + rand.nextGaussian() * 0.02);
    }

    private static TileEntityLoot emptyPile(net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileEntityLoot loot && loot.items.isEmpty() ? loot : null;
    }

    private static ItemStack hbm(String id, int count) {
        Item item = BuiltInRegistries.ITEM.getOptional(Ids.of("hbm", id)).orElse(Items.AIR);
        return new ItemStack(item, count);
    }

    public static void lootBooklet(net.minecraft.world.level.LevelAccessor level, BlockPos pos) { // LevelAccessor: also from worldgen (WG-D broken lantern)
        TileEntityLoot loot = emptyPile(level, pos);
        if (loot != null) {
            loot.addItem(ItemBookLore.createBook("beacon", 12, 0x404040, 0xD637B3), 0, 0, 0);
        }
    }

    public static void lootCapNuke(Level level, BlockPos pos) {
        TileEntityLoot loot = emptyPile(level, pos);
        if (loot != null) {
            RandomSource rand = level.getRandom();
            if (rand.nextInt(5) == 0)
                loot.addItem(new ItemStack(ModAmmoItems.nuke_standard.get()), -0.25, 0, -0.125);
            else
                loot.addItem(new ItemStack(ModAmmoItems.rocket_heat.get()), -0.25, 0, -0.25);

            for (int i = 0; i < 4; i++) addItemWithDeviation(loot, rand, hbm("cap_nuka", 2), 0.125, i * 0.03125, 0.25);
            for (int i = 0; i < 2; i++) addItemWithDeviation(loot, rand, hbm("syringe_metal_stimpak", 1), -0.25, i * 0.03125, 0.25);
            for (int i = 0; i < 6; i++) addItemWithDeviation(loot, rand, hbm("cap_nuka", 2), 0.125, i * 0.03125, -0.25);
        }
    }

    public static void lootMedicine(net.minecraft.world.level.LevelAccessor level, BlockPos pos) { // LevelAccessor: also filled from worldgen (WG-C components)
        TileEntityLoot loot = emptyPile(level, pos);
        if (loot != null) {
            RandomSource rand = level.getRandom();
            for (int i = 0; i < 4; i++) addItemWithDeviation(loot, rand, ItemPool.getStack(ItemPools.POOL_PILE_MED_SYRINGE, rand), 0.125, i * 0.03125, 0.25);
            addItemWithDeviation(loot, rand, ItemPool.getStack(ItemPools.POOL_PILE_MED_PILLS, rand), -0.25, 0, -0.125);
        }
    }

    public static void lootCapStash(net.minecraft.world.level.LevelAccessor level, BlockPos pos) { // LevelAccessor: worldgen (WG-C)
        TileEntityLoot loot = emptyPile(level, pos);
        if (loot != null) {
            RandomSource rand = level.getRandom();
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    int count = rand.nextInt(5) + 3;
                    for (int k = 0; k < count; k++) {
                        addItemWithDeviation(loot, rand, ItemPool.getStack(ItemPools.POOL_PILE_CAPS, rand), i * 0.3125, k * 0.03125, j * 0.3125);
                    }
                }
            }
        }
    }

    public static void lootMakeshiftGun(net.minecraft.world.level.LevelAccessor level, BlockPos pos) { // LevelAccessor: worldgen (WG-C)
        TileEntityLoot loot = emptyPile(level, pos);
        if (loot != null) {
            RandomSource rand = level.getRandom();
            boolean r = rand.nextBoolean();
            if (r) addItemWithDeviation(loot, rand, ItemPool.getStack(ItemPools.POOL_PILE_MAKESHIFT_GUN, rand), 0.125, 0.025, 0.25);

            if (!r || rand.nextBoolean()) addItemWithDeviation(loot, rand, ItemPool.getStack(ItemPools.POOL_PILE_MAKESHIFT_WRENCH, rand), -0.25, 0, -0.28125);

            int count = rand.nextInt(2) + 1;
            for (int i = 0; i < count; i++) addItemWithDeviation(loot, rand, ItemPool.getStack(ItemPools.POOL_PILE_MAKESHIFT_PLATES, rand), -0.25, i * 0.03125, 0.3125);

            count = rand.nextInt(2) + 2;
            for (int i = 0; i < count; i++) addItemWithDeviation(loot, rand, ItemPool.getStack(ItemPools.POOL_PILE_MAKESHIFT_WIRE, rand), 0.25, i * 0.03125, 0.1875);
        }
    }

    public static void lootNukeStorage(Level level, BlockPos pos) {
        TileEntityLoot loot = emptyPile(level, pos);
        if (loot != null) {
            RandomSource rand = level.getRandom();
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    if (rand.nextBoolean()) {
                        loot.addItem(ItemPool.getStack(ItemPools.POOL_PILE_NUKE_STORAGE, rand), -0.375 + i * 0.25, 0, -0.375 + j * 0.25);
                    }
                }
            }
        }
    }

    private static void scattered(net.minecraft.world.level.LevelAccessor level, BlockPos pos, String pool, int base, int spread) {
        TileEntityLoot loot = emptyPile(level, pos);
        if (loot != null) {
            RandomSource rand = level.getRandom();
            int limit = rand.nextInt(spread) + base;
            for (int i = 0; i < limit; i++) {
                addItemWithDeviation(loot, rand, ItemPool.getStack(ItemPool.getPool(pool), rand), rand.nextDouble() - 0.5, i * 0.03125, rand.nextDouble() - 0.5);
            }
        }
    }

    public static void lootBones(net.minecraft.world.level.LevelAccessor level, BlockPos pos) { // LevelAccessor: also filled from worldgen (GlyphidHive)
        scattered(level, pos, ItemPools.POOL_PILE_BONES, 3, 3);
    }

    public static void lootGlyphidHive(net.minecraft.world.level.LevelAccessor level, BlockPos pos) {
        scattered(level, pos, ItemPools.POOL_PILE_HIVE, 3, 3);
    }

    /** 1.7 put the world's MKU item and its recipe book here; {@code MKUCraftingHandler} is not ported. */
    public static void lootBookMeteor(Level level, BlockPos pos) {
        if (emptyPile(level, pos) != null) {
            Constants.LOG.debug("[LootGenerator] LOOT_METEOR needs the unported MKU recipe book; the pile is left to the next generator");
        }
    }

    public static void lootBookLore(net.minecraft.world.level.LevelAccessor level, BlockPos pos, ItemStack book) { // LevelAccessor: worldgen (WG-C)
        TileEntityLoot loot = emptyPile(level, pos);
        if (loot != null) {
            RandomSource rand = level.getRandom();
            addItemWithDeviation(loot, rand, book, 0, 0, -0.25);

            int count = rand.nextInt(3) + 2;
            for (int k = 0; k < count; k++) addItemWithDeviation(loot, rand, new ItemStack(Items.BOOK), -0.25, k * 0.03125, 0.25);

            count = rand.nextInt(2) + 1;
            for (int k = 0; k < count; k++) addItemWithDeviation(loot, rand, new ItemStack(Items.PAPER), 0.25, k * 0.03125, 0.125);
        }
    }

    public static void lootFlareGun(Level level, BlockPos pos) {
        TileEntityLoot loot = emptyPile(level, pos);
        if (loot != null) {
            RandomSource rand = level.getRandom();
            addItemWithDeviation(loot, rand, hbm("gun_flaregun", 1), 0, 0, -0.25);

            int count = rand.nextInt(3) + 2;
            for (int k = 0; k < count; k++)
                addItemWithDeviation(loot, rand, new ItemStack(ModAmmoItems.g26_flare.get()), -0.25, k * 0.03125, 0.25);

            count = rand.nextInt(1) + 1;
            for (int k = 0; k < count; k++)
                addItemWithDeviation(loot, rand,
                        new ItemStack(rand.nextBoolean() ? ModAmmoItems.g26_flare_supply.get() : ModAmmoItems.g26_flare_weapon.get()),
                        0.25, k * 0.03125, 0.125);
        }
    }

    public static void lootShit(Level level, BlockPos pos) {
        scattered(level, pos, ItemPools.POOL_PILE_OF_GARBAGE, 3, 3);
    }

    public static void lootMechanical(Level level, BlockPos pos) {
        scattered(level, pos, ItemPools.POOL_PILE_MECHANICAL, 1, 6);
    }

    public static void lootGear(Level level, BlockPos pos) {
        scattered(level, pos, ItemPools.POOL_PILE_GEAR, 1, 6);
    }

    public static void lootSupplies(Level level, BlockPos pos) {
        scattered(level, pos, POOL_PILE_SUPPLIES, 4, 3);
    }
}
