package com.hbm.items.special;

import com.hbm.items.ModItems;
import com.hbm.items.ModMedicineItems;
import com.hbm.potion.ModEffects;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.Effects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Simple use-only consumables, currently the three RadAway doses. */
public final class ItemSimpleConsumable extends CompatItem {

    private final String id;

    public ItemSimpleConsumable(Item.Properties properties, String id) {
        super(properties);
        this.id = id;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel serverLevel) {
            int duration = switch (id) {
                case "radaway" -> 200;
                case "radaway_strong" -> 100;
                case "radaway_flush" -> 50;
                default -> 0;
            };
            if (duration > 0) {
                int amplifier = switch (id) {
                    case "radaway" -> 24;
                    case "radaway_strong" -> 99;
                    default -> 399;
                };
                addStacking(player, ModEffects.RADAWAY.get(), duration, amplifier);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RADAWAY.get(),
                        SoundSource.PLAYERS, 1.0F, 1.0F);
                shrinkAndReplace(player, hand, ModMedicineItems.iv_empty.get());
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static void addStacking(Player player, MobEffect effect, int duration, int amplifier) {
        var active = Effects.getRegistered(player, effect);
        if (active != null && active.getAmplifier() == amplifier) duration += active.getDuration();
        Effects.addRegistered(player, effect, duration, amplifier);
    }

    private static void shrinkAndReplace(Player player, InteractionHand hand, Item replacement) {
        ItemStack held = player.getItemInHand(hand);
        held.shrink(1);
        ItemStack result = replacement.getDefaultInstance();
        if (held.isEmpty()) player.setItemInHand(hand, result);
        else if (!player.getInventory().add(result)) player.drop(result, false);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        // The old ItemSimpleConsumable had no custom tooltip text.
    }
}
