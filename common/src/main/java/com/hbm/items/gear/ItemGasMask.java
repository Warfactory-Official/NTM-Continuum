package com.hbm.items.gear;

import com.hbm.api.item.IGasMask;
import com.hbm.items.ModDataComponents;
import com.hbm.util.ArmorRegistry.HazardClass;
import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Component-backed, filterable helmet. Right-click with a filter in the other hand to install it; sneak to remove it. */
public class ItemGasMask extends Item implements IGasMask {
    private final List<HazardClass> blacklist;

    public ItemGasMask(Properties properties, List<HazardClass> blacklist) {
        super(properties);
        this.blacklist = List.copyOf(blacklist);
    }

    @Override
    public List<HazardClass> getBlacklist(ItemStack stack) {
        return blacklist;
    }

    @Override
    public ItemStack getFilter(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.GAS_MASK_FILTER.get(), ItemStack.EMPTY).copy();
    }

    @Override
    public boolean isFilterApplicable(ItemStack stack, ItemStack filter) {
        return !filter.isEmpty() && filter.isDamageableItem();
    }

    @Override
    public void installFilter(ItemStack stack, ItemStack filter) {
        if (isFilterApplicable(stack, filter)) stack.set(ModDataComponents.GAS_MASK_FILTER.get(), filter.copyWithCount(1));
    }

    public ItemStack removeFilter(ItemStack stack) {
        ItemStack filter = getFilter(stack);
        stack.remove(ModDataComponents.GAS_MASK_FILTER.get());
        return filter;
    }

    @Override
    public void damageFilter(ItemStack stack, int damage) {
        if (damage <= 0) return;
        ItemStack filter = getFilter(stack);
        if (filter.isEmpty() || !filter.isDamageableItem()) return;

        int remaining = filter.getMaxDamage() - filter.getDamageValue();
        if (damage >= remaining) {
            removeFilter(stack);
        } else {
            filter.setDamageValue(filter.getDamageValue() + damage);
            installFilter(stack, filter);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack mask = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            ItemStack filter = removeFilter(mask);
            if (!filter.isEmpty()) {
                if (!player.getInventory().add(filter)) player.drop(filter, false);
            }
            return InteractionResult.SUCCESS;
        }

        ItemStack offered = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!isFilterApplicable(mask, offered)) return InteractionResult.PASS;

        ItemStack previous = getFilter(mask);
        installFilter(mask, offered);
        if (!player.isCreative()) offered.shrink(1);
        if (!previous.isEmpty() && !player.getInventory().add(previous)) player.drop(previous, false);
        return InteractionResult.SUCCESS;
    }
}
