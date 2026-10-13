package com.hbm.inventory;

import com.hbm.util.AchievementHandler;
import dev.golem.core.world.ItemStacks;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code SlotCraftingOutput}: a take-only machine output slot that counts what was taken and reports it as
 * crafted, both to the item ({@code onCraftedBy}) and to the achievement handler. Containers whose 1.7 original
 * used this slot use it for the same slots.
 */
public class SlotCraftingOutput extends Slot {

    private final Player player;
    private int craftBuffer;

    public SlotCraftingOutput(Player player, Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.player = player;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    /** 1.7 {@code checkAchievements}. */
    public static void checkAchievements(Player player, ItemStack stack) {
        AchievementHandler.fire(player, stack);
    }

    @Override
    public ItemStack remove(int amount) {
        if (this.hasItem()) this.craftBuffer += Math.min(amount, this.getItem().getCount());
        return super.remove(amount);
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        this.checkTakeAchievements(stack);
        super.onTake(player, stack);
    }

    @Override
    protected void onQuickCraft(ItemStack stack, int amount) {
        this.craftBuffer += amount;
        this.checkTakeAchievements(stack);
    }

    /** 1.7 {@code onCrafting(ItemStack)}. */
    @Override
    protected void checkTakeAchievements(ItemStack stack) {
        if (this.craftBuffer > 0) ItemStacks.onCraftedBy(stack, this.player, this.craftBuffer);
        checkAchievements(this.player, stack);
        this.craftBuffer = 0;
    }
}
