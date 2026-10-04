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

/** Simple use-only consumables: the three RadAway doses and the four IV bags. */
public final class ItemSimpleConsumable extends CompatItem {

    private final String id;

    public ItemSimpleConsumable(Item.Properties properties, String id) {
        super(properties);
        this.id = id;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel serverLevel && bloodBag(serverLevel, player, hand)) {
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel serverLevel) {
            // 1.7 ItemSimpleConsumable.doRadaway: RadAway I (1 RAD a tick) for 140 / 350 / 500 ticks, added to any
            // RadAway already running. The port had compensated a 20x-too-weak effect with amplifiers 24/99/399.
            int duration = switch (id) {
                case "radaway" -> 140;
                case "radaway_strong" -> 350;
                case "radaway_flush" -> 500;
                default -> 0;
            };
            if (duration > 0) {
                addStacking(player, ModEffects.RADAWAY.get(), duration, 0);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RADAWAY.get(),
                        SoundSource.PLAYERS, 1.0F, 1.0F);
                shrinkAndReplace(player, hand, ModMedicineItems.iv_empty.get());
            }
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * 1.7 {@code ItemSimpleConsumable.init}, "BLOOD BAGS": the empty bag draws 5 health into a blood bag (dying of it
     * if that was the last 5), the blood bag heals 5, the empty experience bag takes 100 experience when the player has
     * at least that much, and the full one gives the 100 back. Returns whether {@link #id} is one of the four.
     */
    private boolean bloodBag(ServerLevel level, Player player, InteractionHand hand) {
        switch (id) {
            case "iv_empty" -> {
                giveSoundAndDecrement(level, player, hand, ModSounds.SYRINGE.get(),
                        com.hbm.items.ModArmorSetBItems.iv_blood.get());
                player.setHealth(Math.max(player.getHealth() - 5F, 0F));
                if (player.getHealth() <= 0) player.die(player.damageSources().magic());
            }
            case "iv_blood" -> {
                giveSoundAndDecrement(level, player, hand, ModSounds.RADAWAY.get(), ModMedicineItems.iv_empty.get());
                player.heal(5F);
            }
            case "iv_xp_empty" -> {
                if (com.hbm.util.EnchantmentUtil.getTotalExperience(player) >= 100) {
                    giveSoundAndDecrement(level, player, hand, ModSounds.SYRINGE.get(),
                            com.hbm.items.ModArmorSetBItems.iv_xp.get());
                    com.hbm.util.EnchantmentUtil.setExperience(player,
                            com.hbm.util.EnchantmentUtil.getTotalExperience(player) - 100);
                }
            }
            case "iv_xp" -> {
                giveSoundAndDecrement(level, player, hand,
                        dev.golem.core.world.Sounds.minecraft("entity.experience_orb.pickup"),
                        com.hbm.items.ModArmorSetBItems.iv_xp_empty.get());
                com.hbm.util.EnchantmentUtil.addExperience(player, 100, false);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /** 1.7 {@code giveSoundAndDecrement}: one fewer, the sound at the user, the container back. */
    private static void giveSoundAndDecrement(ServerLevel level, Player player, InteractionHand hand,
                                              net.minecraft.sounds.SoundEvent sound, Item container) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        shrinkAndReplace(player, hand, container);
    }

    private static void addStacking(Player player, MobEffect effect, int duration, int amplifier) {
        // 1.7 addPotionEffect: an active effect's remaining duration is added, whatever its level
        var active = Effects.getRegistered(player, effect);
        if (active != null) duration += active.getDuration();
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
