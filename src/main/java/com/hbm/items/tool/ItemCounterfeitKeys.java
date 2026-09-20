package com.hbm.items.tool;

import com.hbm.items.ModItems;
import com.hbm.tileentity.machine.TileEntityLockableBase;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import com.hbm.util.LockTargets;

/**
 * The key imitation kit. Port of 1.7.10's {@code ItemCounterfeitKeys}: used on a locked, cheesable
 * {@link TileEntityLockableBase} it turns itself into a counterfeit key with the lock's pins and hands
 * the player a second copy. Elaborate (non-cheesable) locks refuse with the old chat lines.
 */
public class ItemCounterfeitKeys extends CompatItem {

    public ItemCounterfeitKeys(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.TRY_WITH_EMPTY_HAND;

        BlockEntity te = LockTargets.coreAt(level, pos);

        if (te instanceof TileEntityLockableBase locked) {

            if (locked.isLocked() && locked.cheesable) {
                if (!level.isClientSide()) {
                    ItemStack st = new ItemStack(ModItems.key_fake.get());
                    ItemKeyPin.setPins(st, locked.getPins());

                    player.getInventory().setItem(Compat.selectedSlot(player), st.copy());

                    if (!player.getInventory().add(st.copy())) {
                        player.drop(st.copy(), false);
                    }

                    player.inventoryMenu.broadcastChanges();
                }

                player.swing(context.getHand());

                return InteractionResult.SUCCESS;
            } else if (!locked.cheesable) {
                if (!level.isClientSide()) {
                    player.sendSystemMessage(Component.literal("This lock is too elaborate for a counterfeit key to be made")
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                    player.sendSystemMessage(Component.literal("Perhaps there is another way around here to unlock it")
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Use on a locked container to create two counterfeit keys!"));
    }
}
