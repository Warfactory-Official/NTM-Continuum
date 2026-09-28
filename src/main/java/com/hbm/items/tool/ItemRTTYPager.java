package com.hbm.items.tool;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.IItemControlReceiver;
import com.hbm.items.ModUtilityToolItems;
import com.hbm.packet.PlayerInformPayload;
import com.hbm.packet.PlayerInformPayload.Colored;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.tileentity.network.RTTYSystem.RTTYChannel;
import dev.golem.core.data.Tags;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ItemRTTYPager}: listens to one Redstone-over-Radio channel ({@code chan}) from anywhere in the
 * inventory and shows every fresh signal on the info HUD for five seconds ({@code ID_PAGER_DYN + slot}). The
 * signal {@code selfdestruct} blows the pager up in its carrier's hands. Right-click opens the channel screen.
 */
public class ItemRTTYPager extends CompatItem implements IItemControlReceiver {

    public static final String KEY_CHANNEL = "chan";
    /** 1.7 {@code ServerProxy.ID_PAGER_DYN}. */
    public static final int ID_PAGER_DYN = 1000;

    public ItemRTTYPager(Properties properties) {
        super(properties);
    }

    public static String channel(ItemStack stack) {
        return ModUtilityToolItems.PAGER_CHANNEL.getOr(stack, null);
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slotType,
                                       boolean held) {
        String channelFreq = channel(stack);
        if (channelFreq == null) return;
        if (!(entity instanceof ServerPlayer player)) return;

        RTTYChannel chan = RTTYSystem.listen(world, channelFreq);

        if (chan != null && chan.timeStamp >= world.getGameTime() - 1) {
            int alive = entity.tickCount % 1000;

            if ("selfdestruct".equals(chan.signal + "")) {
                ExplosionVNT vnt = new ExplosionVNT(world, entity.getX(), entity.getY() + entity.getBbHeight() / 2,
                        entity.getZ(), 5, null);
                vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 50).setupPiercing(5F, 0.5F));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
                vnt.explode();
                stack.shrink(1);
                return;
            }

            int slot = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i) == stack) {
                    slot = i;
                    break;
                }
            }
            GolemNetwork.sendToPlayer(player, PlayerInformPayload.CODEC, PlayerInformPayload.of(ID_PAGER_DYN + slot,
                    5_000, ChatFormatting.GOLD, "[ " + channelFreq + " (" + alive + ") ] ",
                    new Colored(String.valueOf(chan.signal), ChatFormatting.YELLOW)));
        }
    }

    @Override
    protected InteractionResult compatUse(Level world, Player player, InteractionHand hand) {
        if (world.isClientSide()) UtilityToolScreens.open(UtilityToolScreens.Kind.PAGER, player.getItemInHand(hand));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        String chan = channel(stack);
        if (chan == null || chan.isEmpty()) {
            list.add(Component.literal("No channel set!").withStyle(ChatFormatting.RED));
        } else {
            list.add(Component.literal("Channel: " + chan).withStyle(ChatFormatting.YELLOW));
        }
    }

    @Override
    public void receiveControl(Player player, ItemStack stack, CompoundTag data) {
        if (Tags.contains(data, KEY_CHANNEL)) {
            ModUtilityToolItems.PAGER_CHANNEL.set(stack, Tags.getStringOr(data, KEY_CHANNEL, ""));
        }
    }
}
