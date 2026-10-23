package com.hbm.items.tool;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.items.IItemControlReceiver;
import com.hbm.items.ModUtilityToolItems;
import com.hbm.tileentity.network.RTTYSystem;
import dev.golem.core.data.Tags;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.SubLevels;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * 1.7 {@code ItemRTTYDesignator}: right-click broadcasts the coordinates of the block face looked at, up to 300
 * blocks away, as {@code x:y:z} on its Redstone-over-Radio channel; sneak-right-click opens the channel screen.
 */
public class ItemRTTYDesignator extends CompatItem implements IItemControlReceiver {

    public static final String KEY_CHANNEL = "chan";

    public ItemRTTYDesignator(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown() && level.isClientSide()) {
            UtilityToolScreens.open(UtilityToolScreens.Kind.RTTY_DESIGNATOR, stack);
        } else if (!level.isClientSide() && !player.isShiftKeyDown()) {
            String chanFreq = ItemRTTYPager.channel(stack);
            if (chanFreq == null) return InteractionResult.SUCCESS;

            // 1.7 Library.rayTrace(player, 300, 1) answers a block position even on a miss
            HitResult hit = player.pick(300.0D, 1.0F, false);
            if (hit instanceof BlockHitResult blockHit) {
                BlockPos pos = SubLevels.toWorldBlock(level,
                        blockHit.getBlockPos().relative(blockHit.getDirection()));
                String coords = pos.getX() + IRORInteractive.PARAM_SEPARATOR + pos.getY()
                        + IRORInteractive.PARAM_SEPARATOR + pos.getZ();
                RTTYSystem.broadcast(level, chanFreq, coords);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        String chan = ItemRTTYPager.channel(stack);
        if (chan == null || chan.isEmpty()) {
            list.add(Component.literal("No channel set!").withStyle(ChatFormatting.RED));
        } else {
            list.add(Component.literal("Channel: " + chan).withStyle(ChatFormatting.YELLOW));
        }
    }

    @Override
    public void receiveControl(Player player, ItemStack stack, CompoundTag data) {
        ModUtilityToolItems.PAGER_CHANNEL.set(stack, Tags.getStringOr(data, KEY_CHANNEL, ""));
    }
}
