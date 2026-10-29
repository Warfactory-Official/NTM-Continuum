package com.hbm.delegacy.legacy;

import com.hbm.items.ILoadOnlyItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A DE item id Continuum split into several items; it shows as {@code target} and converts once a player holds it. */
public class LegacyItem extends Item implements ILoadOnlyItem {
    private final ResourceLocation target;

    public LegacyItem(ResourceLocation target, Properties properties) {
        super(properties);
        this.target = target;
    }

    @Override
    public String getDescriptionId() {
        return BuiltInRegistries.ITEM.get(target).getDescriptionId();
    }

    /** The target's shown name: some targets build theirs in getName and have no lang key of their own. */
    @Override
    public Component getName(ItemStack stack) {
        var item = BuiltInRegistries.ITEM.get(target);
        return item.getName(new ItemStack(item));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (entity instanceof ServerPlayer player) {
            LegacyStacks.convertAll(player.getInventory(), player.server, "player " + player.getScoreboardName());
        }
    }
}
