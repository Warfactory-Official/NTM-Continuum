package com.hbm.items.tool;

import com.hbm.entity.mob.EntityQuackos;
import dev.golem.core.world.CompatItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ItemPeas}: an offering. Every Quackos within 50 blocks of the player's box accepts it and leaves.
 * (The {@code item.peas.desc} lang line is never shown in 1.7: this item has no addInformation.)
 * The peas are spent (outside creative) whether or not anyone was there to accept them.
 */
public class ItemPeas extends CompatItem {

    public ItemPeas(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        for (EntityQuackos duck : level.getEntitiesOfClass(EntityQuackos.class, player.getBoundingBox().inflate(50))) {
            duck.despawn();
        }
        return InteractionResult.SUCCESS;
    }
}
