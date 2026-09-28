package com.hbm.items.special;

import com.hbm.config.WeaponConfig;
import com.hbm.items.ModSingularityItems;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** The old antimatter cluster, including its real dirty-bomb drop behavior. */
public final class ItemAntimatterPellet extends CompatItem {
    public ItemAntimatterPellet(Item.Properties properties) {
        super(properties);
    }

    public static Item.Properties properties(Item.Properties properties) {
        return Compat.craftingRemainder(properties, ModSingularityItems.cell.get());
    }

    /**
     * 1.7 {@code ItemDrop.onEntityItemUpdate} for {@code pellet_antimatter}: on the ground (not while burning in the
     * air) {@code new ExplosionVNT(..., 20F).makeAmat().explode()} when {@code dropCell} is on; the entity is removed
     * either way, as 1.7's {@code setDead} sat outside the config check.
     */
    public static void tickDropped(ItemEntity entity) {
        if (!(entity.getItem().getItem() instanceof ItemAntimatterPellet) || !entity.onGround()
                || !(entity.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        if (WeaponConfig.dropCell) {
            new com.hbm.explosion.vanillant.ExplosionVNT(level, entity.getX(), entity.getY(), entity.getZ(), 20F)
                    .makeAmat().explode();
        }
        entity.discard();
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Very heavy antimatter cluster."));
        tooltip.add(Component.literal("Gets rid of black holes."));
        tooltip.add(Component.literal("[Dangerous Drop]").withStyle(ChatFormatting.RED));
    }
}
