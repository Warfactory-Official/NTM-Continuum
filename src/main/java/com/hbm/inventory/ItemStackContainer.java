package com.hbm.inventory;

import dev.golem.core.data.GolemComponent;
import dev.golem.core.data.ItemContents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Source-shaped inventory whose backing store is a component on the stack being held. */
public final class ItemStackContainer extends SimpleContainer {

    private final Player owner;
    private final ItemStack target;
    private final GolemComponent<ItemContents> component;

    public ItemStackContainer(Player owner, ItemStack target, int size,
                              GolemComponent<ItemContents> component) {
        super(size);
        this.owner = owner;
        this.target = target;
        this.component = component;

        var stacks = component.getOr(target, ItemContents.EMPTY).copies();
        for (int slot = 0; slot < size && slot < stacks.size(); slot++) super.setItem(slot, stacks.get(slot));
    }

    public ItemStack target() {
        return target;
    }

    @Override
    public void setChanged() {
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>(getContainerSize());
        for (int slot = 0; slot < getContainerSize(); slot++) stacks.add(getItem(slot));
        component.set(target, new ItemContents(stacks));
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner && (player.getMainHandItem() == target || player.getOffhandItem() == target);
    }
}
