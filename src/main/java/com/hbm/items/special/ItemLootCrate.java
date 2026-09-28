package com.hbm.items.special;

import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.items.weapon.ItemCustomMissilePart.PartSize;
import com.hbm.items.weapon.ItemCustomMissilePart.PartType;
import dev.golem.core.world.CompatItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code ItemLootCrate}: a size 10, size 15 or general missile loot crate that gives one custom missile part,
 * re-rolling until the part's rarity check passes (common always, uncommon 1 in 5 ... strange 1 in 100).
 *
 * <p>1.7 filled the three pools as each part called {@code setRarity}: fuselages by their top size, everything
 * else into the general pool. The port's parts are built when the registry flushes, so the pools are read back
 * from the registry in registration order the first time a crate is opened.
 */
public class ItemLootCrate extends CompatItem {

    public enum Pool { SIZE_10, SIZE_15, MISC }

    private static List<ItemCustomMissilePart> list10;
    private static List<ItemCustomMissilePart> list15;
    private static List<ItemCustomMissilePart> listMisc;

    private final Pool pool;

    public ItemLootCrate(Properties properties, Pool pool) {
        super(properties);
        this.pool = pool;
    }

    private static void fill() {
        if (list10 != null) return;
        List<ItemCustomMissilePart> l10 = new ArrayList<>();
        List<ItemCustomMissilePart> l15 = new ArrayList<>();
        List<ItemCustomMissilePart> misc = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof ItemCustomMissilePart part) || part.rarity == null) continue;
            if (part.type == PartType.FUSELAGE) {
                if (part.top == PartSize.SIZE_10) l10.add(part);
                if (part.top == PartSize.SIZE_15) l15.add(part);
            } else {
                misc.add(part);
            }
        }
        list10 = l10;
        list15 = l15;
        listMisc = misc;
    }

    public static List<ItemCustomMissilePart> pool(Pool pool) {
        fill();
        return switch (pool) {
            case SIZE_10 -> list10;
            case SIZE_15 -> list15;
            case MISC -> listMisc;
        };
    }

    @Override
    protected InteractionResult compatUse(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        List<ItemCustomMissilePart> parts = pool(pool);

        if (!world.isClientSide() && !parts.isEmpty()) {
            player.getInventory().add(new ItemStack(choose(parts, world.getRandom())));
            player.inventoryMenu.broadcastChanges();
        }
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    private static ItemCustomMissilePart choose(List<ItemCustomMissilePart> parts, RandomSource rand) {
        while (true) {
            ItemCustomMissilePart item = parts.get(rand.nextInt(parts.size()));
            boolean pass = switch (item.rarity) {
                case COMMON -> true;
                case UNCOMMON -> rand.nextInt(5) == 0;
                case RARE -> rand.nextInt(10) == 0;
                case EPIC -> rand.nextInt(25) == 0;
                case LEGENDARY -> rand.nextInt(50) == 0;
                case STRANGE -> rand.nextInt(100) == 0;
            };
            if (pass) return item;
        }
    }
}
