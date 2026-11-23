package com.hbm.inventory;

import dev.golem.core.world.ItemStacks;
import dev.golem.core.world.Smelts;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code SlotSmelting}: a take-only furnace output that reports what was taken as smelted, as vanilla's furnace
 * slot does. 1.7 also drops the recipe's experience here.
 */
public class SlotSmelting extends Slot {

    private final Player player;
    private int countBuffer;

    public SlotSmelting(Player player, Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.player = player;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        if (this.hasItem()) this.countBuffer += Math.min(amount, this.getItem().getCount());
        return super.remove(amount);
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        this.checkTakeAchievements(stack);
        super.onTake(player, stack);
    }

    @Override
    protected void onQuickCraft(ItemStack stack, int amount) {
        this.countBuffer += amount;
        this.checkTakeAchievements(stack);
    }

    @Override
    protected void checkTakeAchievements(ItemStack stack) {
        if (this.countBuffer > 0) ItemStacks.onCraftedBy(stack, this.player, this.countBuffer);
        this.countBuffer = 0;
        Smelts.taken(this.player, stack);
    }
}
