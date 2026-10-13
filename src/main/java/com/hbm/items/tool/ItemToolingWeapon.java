package com.hbm.items.tool;

import com.hbm.api.block.IToolable;
import dev.golem.core.world.CompatWeaponItem;
import dev.golem.core.world.ItemStacks;
import dev.golem.core.world.Tools;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 1.7 {@code ItemToolingWeapon} (the Archineer's wrench): an {@code ItemTooling} of its tool type that also
 * carries an absolute {@code damage} "Weapon modifier". 1.7's knock-back and slow-down branches compared
 * {@code this == ModItems.wrench}, which is an {@code ItemWrench} and never this class, so they never ran.
 */
public class ItemToolingWeapon extends CompatWeaponItem {

    private final IToolable.ToolType type;

    public ItemToolingWeapon(Properties properties, IToolable.ToolType type, float damage) {
        super(properties, damage, Tools.SWORD_SPEED);
        this.type = type;
    }

    public IToolable.ToolType getType() {
        return type;
    }

    /** 1.7 {@code ItemTooling.onItemUse}. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Block block = level.getBlockState(context.getClickedPos()).getBlock();
        Player player = context.getPlayer();

        if (player != null && block instanceof IToolable toolable
                && toolable.onScrew(level, player, context.getClickedPos(), context.getClickedFace(),
                context.getClickLocation(), context.getHand(), type)) {
            ItemStack stack = context.getItemInHand();
            if (stack.getMaxDamage() > 0) {
                ItemStacks.hurtAndBreak(stack, 1, player, context.getHand());
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }
}
