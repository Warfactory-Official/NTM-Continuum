package com.hbm.itempool;

import com.hbm.Constants;
import dev.golem.core.platform.Platform;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.world.ChestLoot;
import dev.golem.core.world.ItemStacks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Structure loot: a draw from item pool {@code POOL_X} rolls the loot table {@code hbm:chests/x} once (HBM's tables are
 * generated from the pool rows), or draws from the item pool itself when no such table is loaded, as for an addon's pool.
 */
public final class PoolLoot {

    /** The most draws a generated {@link #table(String, int)} covers. */
    public static final int MAX_TABLE_DRAWS = 32;

    private PoolLoot() {}

    public static String table(String pool) {
        return Constants.MOD_ID + ":chests/" + ItemPools.slug(pool);
    }

    /** The table that makes {@code draws} draws from {@code pool}: the pool's own table for one draw. */
    public static String table(String pool, int draws) {
        return draws == 1 ? table(pool) : table(pool) + "/x" + draws;
    }

    /** A lootable container's table and seed through the keys every supported version reads them from. */
    public static void assignTable(LevelAccessor level, BlockEntity container, String table, long seed) {
        CompoundTag tag = new CompoundTag();
        tag.putString("LootTable", table);
        tag.putLong("LootTableSeed", seed);
        BlockEntities.loadWithComponents(container, tag, level.registryAccess());
    }

    public static List<ItemStack> draw(LevelAccessor level, BlockPos pos, String pool, RandomSource rand) {
        ServerLevel server = ((ServerLevelAccessor) level).getLevel();
        String table = table(pool);
        if (!ChestLoot.exists(server, table)) {
            ItemStack stack = ItemPool.getStack(pool, rand);
            return stack.isEmpty() ? List.of() : List.of(stack);
        }
        return ChestLoot.rollUnsplit(server, table, Vec3.atCenterOf(pos), rand.nextLong());
    }

    /**
     * 1.7 {@code WeightedRandomChestContent.generateChestContents}: {@code count} draws, each stack put into a random slot
     * (overwriting); a stack larger than its limit goes in as single items, each to its own random slot.
     */
    public static void fill(LevelAccessor level, BlockPos pos, Container inventory, String pool, RandomSource rand, int count) {
        int size = inventory.getContainerSize();
        if (size <= 0) return;
        // port: Lootr converts only a chest or barrel that still holds its table, and it fills that per player the vanilla way
        if (inventory instanceof RandomizableContainerBlockEntity chest && count >= 1 && count <= MAX_TABLE_DRAWS
                && Platform.isModLoaded("lootr") && ChestLoot.exists(((ServerLevelAccessor) level).getLevel(), table(pool, count))) {
            assignTable(level, chest, table(pool, count), rand.nextLong());
            return;
        }
        for (int j = 0; j < count; ++j) {
            for (ItemStack drawn : draw(level, pos, pool, rand)) {
                if (drawn.isEmpty()) continue;
                if (drawn.getCount() <= drawn.getMaxStackSize()) {
                    inventory.setItem(rand.nextInt(size), drawn);
                } else {
                    for (int i = 0; i < drawn.getCount(); i++) inventory.setItem(rand.nextInt(size), ItemStacks.copyWithCount(drawn, 1));
                }
            }
        }
        if (inventory instanceof BlockEntity be) be.setChanged();
    }
}
