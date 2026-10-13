package com.hbm.items.tool;

import dev.golem.core.world.CompatSwordItem;
import dev.golem.core.world.ToolTier;
import dev.golem.core.world.Tools;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code BigSword}: a plain {@code EMERALD} sword ({@code 4 + 3} attack) whose right-click does nothing.
 * its plasma-beam {@code onUsingTick} was commented out in the original, and {@code onItemRightClick}
 * returned the stack without starting the sword block. Its unused {@code worldObj/posX/Y/Z} fields are dropped.
 */
public class BigSword extends CompatSwordItem {

    public BigSword(Properties properties, ToolTier tier) {
        super(properties, tier, 4.0F + tier.attackDamageBonus(), Tools.SWORD_SPEED, 0.0D);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
