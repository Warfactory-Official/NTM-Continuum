package com.hbm.items.special;

import com.hbm.capability.HbmLivingProps;
import com.hbm.config.VersatileConfig;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageSources;
import com.hbm.potion.ModEffects;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.armor.JetpackFueledBase;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.Effects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Right-click and melee-injection consumables, ported from the old handler tables. */
public final class ItemConsumable extends CompatItem {

    private final String id;

    public ItemConsumable(Item.Properties properties, String id) {
        super(properties);
        this.id = id;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel serverLevel) apply(serverLevel, player, player, hand);
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected boolean compatHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (isMeleeConsumable() && attacker instanceof Player player && target.level() instanceof ServerLevel level) {
            apply(level, player, target, InteractionHand.MAIN_HAND);
        }
        return false;
    }

    private boolean isMeleeConsumable() {
        return switch (id) {
            case "syringe_antidote", "syringe_awesome", "syringe_poison", "syringe_metal_stimpak",
                    "syringe_metal_medx", "syringe_metal_psycho", "syringe_metal_super", "syringe_taint",
                    "syringe_mkunicorn" -> true;
            default -> false;
        };
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return id.equals("syringe_awesome") || super.isFoil(stack);
    }

    private void apply(ServerLevel level, Player user, LivingEntity target, InteractionHand hand) {
        switch (id) {
            case "syringe_antidote" -> {
                // 1.7 ItemSimpleConsumable.effectAntidote: sickness only through the (default-off) config
                if (!VersatileConfig.hasPotionSickness(target)) {
                    Effects.clear(target);
                    play(level, target, ModSounds.SYRINGE.get());
                    shrinkAndReplace(user, hand, ModItems.syringe_empty.get());
                    VersatileConfig.applyPotionSickness(target, 5);
                }
            }
            case "syringe_awesome" -> {
                if (VersatileConfig.hasPotionSickness(target)) return;
                add(target, MobEffects.REGENERATION, 50 * 20, 9);
                add(target, Effects.resistance(), 50 * 20, 9);
                add(target, MobEffects.FIRE_RESISTANCE, 50 * 20, 0);
                add(target, Effects.strength(), 50 * 20, 24);
                add(target, Effects.haste(), 50 * 20, 9);
                add(target, Effects.speed(), 50 * 20, 6);
                add(target, Effects.jumpBoost(), 50 * 20, 9);
                add(target, MobEffects.HEALTH_BOOST, 50 * 20, 9);
                add(target, MobEffects.ABSORPTION, 50 * 20, 4);
                add(target, MobEffects.NAUSEA, 5 * 20, 4);
                play(level, target, ModSounds.SYRINGE.get());
                shrinkAndReplace(user, hand, ModItems.syringe_empty.get());
                VersatileConfig.applyPotionSickness(target, 5);
            }
            case "syringe_poison" -> {
                play(level, target, ModSounds.SYRINGE.get());
                shrinkAndReplace(user, hand, ModItems.syringe_empty.get());
                Compat.hurt(target, level, level.getRandom().nextBoolean()
                        ? ModDamageSources.euthanizedSelf(level) : ModDamageSources.euthanizedSelf2(level), 30F);
            }
            case "syringe_metal_stimpak" -> {
                if (VersatileConfig.hasPotionSickness(target)) return;
                target.heal(5F);
                VersatileConfig.applyPotionSickness(target, 5);
                play(level, target, ModSounds.SYRINGE.get());
                shrinkAndReplace(user, hand, ModItems.syringe_metal_empty.get());
            }
            case "syringe_metal_medx" -> {
                if (VersatileConfig.hasPotionSickness(target)) return;
                add(target, Effects.resistance(), 4 * 60 * 20, 2);
                VersatileConfig.applyPotionSickness(target, 5);
                play(level, target, ModSounds.SYRINGE.get());
                shrinkAndReplace(user, hand, ModItems.syringe_metal_empty.get());
            }
            case "syringe_metal_psycho" -> {
                if (VersatileConfig.hasPotionSickness(target)) return;
                add(target, Effects.resistance(), 2 * 60 * 20, 0);
                add(target, Effects.strength(), 2 * 60 * 20, 0);
                VersatileConfig.applyPotionSickness(target, 5);
                play(level, target, ModSounds.SYRINGE.get());
                shrinkAndReplace(user, hand, ModItems.syringe_metal_empty.get());
            }
            case "syringe_metal_super" -> {
                if (VersatileConfig.hasPotionSickness(target)) return;
                target.heal(25F);
                add(target, MobEffects.SLOWNESS, 10 * 20, 0);
                VersatileConfig.applyPotionSickness(target, 15);
                play(level, target, ModSounds.SYRINGE.get());
                shrinkAndReplace(user, hand, ModItems.syringe_metal_empty.get());
            }
            case "med_bag" -> {
                if (VersatileConfig.hasPotionSickness(target)) return;
                target.setHealth(target.getMaxHealth());
                Effects.remove(target, MobEffects.BLINDNESS);
                Effects.remove(target, MobEffects.NAUSEA);
                Effects.remove(target, MobEffects.MINING_FATIGUE);
                Effects.remove(target, MobEffects.HUNGER);
                Effects.remove(target, MobEffects.SLOWNESS);
                Effects.remove(target, MobEffects.POISON);
                Effects.remove(target, MobEffects.WEAKNESS);
                Effects.remove(target, MobEffects.WITHER);
                Effects.removeRegistered(target, ModEffects.RADIATION.get());
                VersatileConfig.applyPotionSickness(target, 15);
                user.getItemInHand(hand).shrink(1);
            }
            case "syringe_taint" -> {
                Effects.addRegistered(target, ModEffects.TAINT.get(), 60 * 20, 0);
                add(target, MobEffects.NAUSEA, 5 * 20, 0);
                play(level, target, ModSounds.SYRINGE.get());
                shrinkAndReplace(user, hand, level.getRandom().nextBoolean()
                        ? ModItems.syringe_metal_empty.get()
                        : ModItems.bottle2_empty.get());
            }
            case "cbt_device" -> {
                Effects.addRegistered(target, ModEffects.BANG.get(), 30, 0);
                user.getItemInHand(hand).shrink(1);
                play(level, target, ModSounds.VICE.get());
            }
            case "syringe_mkunicorn" -> {
                HbmLivingProps.setContagion(target, 3 * 60 * 60 * 20);
                play(level, target, ModSounds.SYRINGE.get());
                user.getItemInHand(hand).shrink(1);
            }
            case "euphemium_stopper" -> {
                add(target, MobEffects.WEAKNESS, 30 * 20, 9);
                add(target, MobEffects.SLOWNESS, 30 * 20, 9);
                add(target, MobEffects.MINING_FATIGUE, 30 * 20, 9);
            }
            case "jetpack_tank" -> {
                ItemStack jetpack = target.getItemBySlot(EquipmentSlot.CHEST);
                if (jetpack.getItem() instanceof JetpackFueledBase jetItem && jetItem.fuel == Fluids.KEROSENE) {
                    int oldFuel = JetpackFueledBase.getFuel(jetpack);
                    int newFuel = Math.min(oldFuel + 1000, jetItem.maxFuel);
                    if (oldFuel != newFuel) {
                        JetpackFueledBase.setFuel(jetpack, newFuel);
                        play(level, target, ModSounds.JETPACK_TANK.get());
                        user.getItemInHand(hand).shrink(1);
                    }
                }
            }
            default -> {
            }
        }
    }

    private static void add(LivingEntity target, Object effect, int duration, int amplifier) {
        Effects.add(target, effect, duration, amplifier);
    }

    private static void play(ServerLevel level, LivingEntity target, SoundEvent sound) {
        level.playSound(null, target.getX(), target.getY(), target.getZ(), sound,
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void shrinkAndReplace(Player player, InteractionHand hand, Item replacement) {
        ItemStack held = player.getItemInHand(hand);
        held.shrink(1);
        ItemStack result = replacement.getDefaultInstance();
        if (held.isEmpty()) {
            player.setItemInHand(hand, result);
        } else if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        switch (id) {
            case "syringe_antidote" -> tooltip.add(Component.literal("Removes all potion effects"));
            case "syringe_awesome" -> tooltip.add(Component.literal("§2Every good effect for 50 seconds"));
            case "syringe_metal_stimpak" -> tooltip.add(Component.literal("§aHeals 2.5 hearts"));
            case "syringe_metal_medx" -> tooltip.add(Component.literal("§dResistance III for 4 minutes"));
            case "syringe_metal_psycho" -> {
                tooltip.add(Component.literal("§dResistance I for 2 minutes"));
                tooltip.add(Component.literal("§6Strength I for 2 minutes"));
            }
            case "syringe_metal_super" -> {
                tooltip.add(Component.literal("§aHeals 25 hearts"));
                tooltip.add(Component.literal("§bSlowness I for 10 seconds"));
            }
            case "syringe_poison" -> tooltip.add(Component.literal("Deadly"));
            case "syringe_taint" -> {
                tooltip.add(Component.literal("§5Tainted I for 60 seconds"));
                tooltip.add(Component.literal("§eNausea I for 5 seconds"));
                tooltip.add(Component.literal("Cloud damage + taint = ghoulified effect"));
            }
            case "med_bag" -> {
                tooltip.add(Component.literal("§aFull heal, regardless of max health"));
                tooltip.add(Component.literal("Removes negative effects"));
            }
            case "syringe_mkunicorn" -> tooltip.add(Component.literal("§c?"));
            default -> {
            }
        }
    }
}
