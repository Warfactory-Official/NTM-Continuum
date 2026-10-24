package com.hbm.gametest;

import com.hbm.capability.HbmLivingProps;
import com.hbm.config.VersatileConfig;
import com.hbm.items.ModArmorSetBItems;
import com.hbm.items.ModFinalConsumableItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModMedicineItems;
import com.hbm.items.food.ItemConserve.EnumFoodType;
import com.hbm.lib.ModDamageSources;
import com.hbm.potion.ModEffects;
import com.hbm.sound.ModGunSounds;
import com.hbm.sound.ModSounds;
import com.hbm.util.EnchantmentUtil;
import com.hbm.util.EntityDamageUtil;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Effects;
import dev.golem.core.world.Sounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The consumables' Java as it was before their treatments became data, copied from the item classes so the
 * {@code treatment_*} GameTests can run both and compare. Only what a treatment now does is here: eating, cooldowns,
 * use gates and the Java-only parts stay in the item classes and run on both sides of the comparison.
 */
final class TreatmentLegacy {

    private TreatmentLegacy() {}

    /** {@code ItemPill.apply}, after the pill was eaten. */
    static void pill(String id, Player player, ServerLevel level) {
        VersatileConfig.applyPotionSickness(player, 5);
        switch (id) {
            case "pill_iodine" -> removeNegative(player);
            case "plan_c" -> {
                for (int i = 0; i < 10; i++) {
                    Compat.hurt(player, level, level.getRandom().nextBoolean() ? ModDamageSources.euthanizedSelf(level)
                            : ModDamageSources.euthanizedSelf2(level), 1000F);
                }
            }
            case "pill_red" -> Effects.addRegistered(player, ModEffects.DEATH.get(), 60 * 60 * 20, 0);
            case "radx" -> Effects.addRegistered(player, ModEffects.RADX.get(), 3 * 60 * 20, 0);
            case "siox" -> {
                HbmLivingProps.setAsbestos(player, 0);
                HbmLivingProps.setBlackLung(player, Math.min(HbmLivingProps.getBlackLung(player), HbmLivingProps.maxBlacklung / 5));
            }
            case "pill_herbal" -> {
                HbmLivingProps.setAsbestos(player, 0);
                HbmLivingProps.setBlackLung(player, Math.min(HbmLivingProps.getBlackLung(player), HbmLivingProps.maxBlacklung / 5));
                HbmLivingProps.incrementRadiation(player, -100F);
                Effects.add(player, MobEffects.NAUSEA, 10 * 20, 0);
                Effects.add(player, MobEffects.WEAKNESS, 10 * 60 * 20, 2);
                Effects.add(player, MobEffects.MINING_FATIGUE, 10 * 60 * 20, 2);
                Effects.add(player, MobEffects.POISON, 5 * 20, 2);
                Effects.addRegistered(player, ModEffects.POTION_SICKNESS.get(), 10 * 60 * 20, 0);
            }
            case "xanax" -> HbmLivingProps.setDigamma(player, Math.max(HbmLivingProps.getDigamma(player) - 0.5D, 0D));
            case "chocolate" -> {
                if (level.getRandom().nextInt(25) == 0) Compat.hurt(player, level, ModDamageSources.overdose(level), 1000F);
                Effects.add(player, Effects.haste(), 60 * 20, 3);
                Effects.add(player, Effects.speed(), 60 * 20, 3);
                Effects.add(player, Effects.jumpBoost(), 60 * 20, 3);
            }
            case "fmn" -> {
                HbmLivingProps.setDigamma(player, Math.min(HbmLivingProps.getDigamma(player), 2D));
                Effects.add(player, MobEffects.BLINDNESS, 60, 0);
            }
            case "five_htp" -> {
                HbmLivingProps.setDigamma(player, 0D);
                Effects.addRegistered(player, ModEffects.STABILITY.get(), 10 * 60 * 20, 0);
            }
            default -> throw new IllegalArgumentException(id);
        }
    }

    /** {@code ItemCanteen.finishUsingItem}'s effects. */
    static void canteen(LivingEntity entity) {
        Effects.add(entity, MobEffects.NAUSEA, 10 * 20, 0);
        Effects.add(entity, Effects.strength(), 30 * 20, 2);
        VersatileConfig.applyPotionSickness(entity, 5);
    }

    /** {@code ItemConsumable.apply} for everything but the jetpack tank. */
    static void syringe(String id, ServerLevel level, Player user, LivingEntity target, InteractionHand hand) {
        switch (id) {
            case "syringe_antidote" -> {
                if (!VersatileConfig.hasPotionSickness(target)) {
                    Effects.clear(target);
                    play(level, target, ModSounds.SYRINGE.get());
                    shrinkAndReplace(user, hand, ModItems.syringe_empty.get());
                    VersatileConfig.applyPotionSickness(target, 5);
                }
            }
            case "syringe_awesome" -> {
                if (VersatileConfig.hasPotionSickness(target)) return;
                Effects.add(target, MobEffects.REGENERATION, 50 * 20, 9);
                Effects.add(target, Effects.resistance(), 50 * 20, 9);
                Effects.add(target, MobEffects.FIRE_RESISTANCE, 50 * 20, 0);
                Effects.add(target, Effects.strength(), 50 * 20, 24);
                Effects.add(target, Effects.haste(), 50 * 20, 9);
                Effects.add(target, Effects.speed(), 50 * 20, 6);
                Effects.add(target, Effects.jumpBoost(), 50 * 20, 9);
                Effects.add(target, MobEffects.HEALTH_BOOST, 50 * 20, 9);
                Effects.add(target, MobEffects.ABSORPTION, 50 * 20, 4);
                Effects.add(target, MobEffects.NAUSEA, 5 * 20, 4);
                Effects.addRegistered(target, ModEffects.RADX.get(), 50 * 20, 9);
                play(level, target, ModSounds.SYRINGE.get());
                shrinkAndReplace(user, hand, ModItems.syringe_empty.get());
                VersatileConfig.applyPotionSickness(target, 5);
            }
            case "syringe_poison" -> {
                play(level, target, ModSounds.SYRINGE.get());
                shrinkAndReplace(user, hand, ModItems.syringe_empty.get());
                Compat.hurt(target, level, target == user ? (level.getRandom().nextBoolean()
                        ? ModDamageSources.euthanizedSelf(level) : ModDamageSources.euthanizedSelf2(level))
                        : ModDamageSources.euthanized(level, user), 30F);
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
                Effects.add(target, Effects.resistance(), 4 * 60 * 20, 2);
                VersatileConfig.applyPotionSickness(target, 5);
                play(level, target, ModSounds.SYRINGE.get());
                shrinkAndReplace(user, hand, ModItems.syringe_metal_empty.get());
            }
            case "syringe_metal_psycho" -> {
                if (VersatileConfig.hasPotionSickness(target)) return;
                Effects.add(target, Effects.resistance(), 2 * 60 * 20, 0);
                Effects.add(target, Effects.strength(), 2 * 60 * 20, 0);
                VersatileConfig.applyPotionSickness(target, 5);
                play(level, target, ModSounds.SYRINGE.get());
                shrinkAndReplace(user, hand, ModItems.syringe_metal_empty.get());
            }
            case "syringe_metal_super" -> {
                if (VersatileConfig.hasPotionSickness(target)) return;
                target.heal(25F);
                Effects.add(target, MobEffects.SLOWNESS, 10 * 20, 0);
                VersatileConfig.applyPotionSickness(target, 15);
                play(level, target, ModSounds.SYRINGE.get());
                shrinkAndReplace(user, hand, ModItems.syringe_metal_empty.get());
            }
            case "med_bag" -> {
                if (VersatileConfig.hasPotionSickness(target)) return;
                target.setHealth(target.getMaxHealth());
                removeNegative(target);
                VersatileConfig.applyPotionSickness(target, 15);
                user.getItemInHand(hand).shrink(1);
            }
            case "syringe_taint" -> {
                Effects.addRegistered(target, ModEffects.TAINT.get(), 60 * 20, 0);
                Effects.add(target, MobEffects.NAUSEA, 5 * 20, 0);
                play(level, target, ModSounds.SYRINGE.get());
                shrinkAndReplace(user, hand, ModItems.syringe_metal_empty.get());
                giveOrDrop(user, ModItems.bottle2_empty.get().getDefaultInstance());
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
                Effects.add(target, MobEffects.WEAKNESS, 30 * 20, 9);
                Effects.add(target, MobEffects.SLOWNESS, 30 * 20, 9);
                Effects.add(target, MobEffects.MINING_FATIGUE, 30 * 20, 9);
            }
            default -> throw new IllegalArgumentException(id);
        }
    }

    /** {@code ItemSimpleConsumable.compatUse}: the blood bags, then RadAway. */
    static void simple(String id, ServerLevel level, Player player, InteractionHand hand) {
        switch (id) {
            case "iv_empty" -> {
                if (!EntityDamageUtil.isRealPlayer(player)) return;
                giveSoundAndDecrement(level, player, hand, ModSounds.SYRINGE.get(), ModArmorSetBItems.iv_blood.get());
                player.setHealth(Math.max(player.getHealth() - 5F, 0F));
                if (player.getHealth() <= 0) player.die(player.damageSources().magic());
            }
            case "iv_blood" -> {
                giveSoundAndDecrement(level, player, hand, ModSounds.RADAWAY.get(), ModMedicineItems.iv_empty.get());
                player.heal(5F);
            }
            case "iv_xp_empty" -> {
                if (!EntityDamageUtil.isRealPlayer(player)) return;
                if (EnchantmentUtil.getTotalExperience(player) >= 100) {
                    giveSoundAndDecrement(level, player, hand, ModSounds.SYRINGE.get(), ModArmorSetBItems.iv_xp.get());
                    EnchantmentUtil.setExperience(player, EnchantmentUtil.getTotalExperience(player) - 100);
                }
            }
            case "iv_xp" -> {
                giveSoundAndDecrement(level, player, hand, Sounds.minecraft("entity.experience_orb.pickup"),
                        ModArmorSetBItems.iv_xp_empty.get());
                EnchantmentUtil.addExperience(player, 100, false);
            }
            default -> {
                int duration = switch (id) {
                    case "radaway" -> 140;
                    case "radaway_strong" -> 350;
                    case "radaway_flush" -> 500;
                    default -> throw new IllegalArgumentException(id);
                };
                MobEffect effect = ModEffects.RADAWAY.get();
                var active = Effects.getRegistered(player, effect);
                Effects.addRegistered(player, effect, duration + (active != null ? active.getDuration() : 0), 0);
                play(level, player, ModSounds.RADAWAY.get());
                shrinkAndReplace(player, hand, ModMedicineItems.iv_empty.get());
            }
        }
    }

    /** {@code ItemEnergy.finishUsingItem} without the chocolate milk's blast and the coffee criterion. */
    static ItemStack energy(String id, ItemStack stack, ServerLevel level, Player player) {
        switch (id) {
            case "can_smart", "can_overcharge" -> { add(player, Effects.speed(), 30 * 20, 1); add(player, Effects.resistance(), 30 * 20, 2); add(player, Effects.strength(), 30 * 20, 0); }
            case "can_creature" -> { add(player, Effects.speed(), 30 * 20, 0); add(player, Effects.resistance(), 30 * 20, 2); add(player, MobEffects.REGENERATION, 30 * 20, 1); }
            case "can_redbomb" -> { add(player, Effects.speed(), 30 * 20, 0); add(player, Effects.resistance(), 30 * 20, 2); add(player, Effects.jumpBoost(), 30 * 20, 1); }
            case "can_mrsugar" -> { add(player, Effects.speed(), 30 * 20, 0); add(player, Effects.haste(), 30 * 20, 1); add(player, Effects.jumpBoost(), 30 * 20, 2); }
            case "can_luna" -> { add(player, Effects.speed(), 30 * 20, 1); add(player, Effects.resistance(), 30 * 20, 2); add(player, Effects.strength(), 30 * 20, 1); add(player, MobEffects.REGENERATION, 30 * 20, 2); }
            case "can_bepis" -> { add(player, Effects.speed(), 30 * 20, 3); add(player, Effects.resistance(), 30 * 20, 3); }
            case "can_breen" -> add(player, MobEffects.NAUSEA, 30 * 20, 0);
            case "can_mug" -> { add(player, Effects.resistance(), 3 * 60 * 20, 2); add(player, MobEffects.REGENERATION, 60 * 20, 2); }
            case "bottle_nuka" -> { player.heal(4F); add(player, Effects.speed(), 30 * 20, 1); add(player, Effects.haste(), 30 * 20, 1); HbmLivingProps.incrementRadiation(player, 5F); }
            case "bottle_cherry" -> { player.heal(6F); add(player, Effects.speed(), 30 * 20, 0); add(player, Effects.jumpBoost(), 30 * 20, 2); HbmLivingProps.incrementRadiation(player, 5F); }
            case "bottle_quantum" -> { player.heal(10F); add(player, Effects.speed(), 30 * 20, 1); add(player, Effects.resistance(), 30 * 20, 2); add(player, Effects.strength(), 30 * 20, 1); HbmLivingProps.incrementRadiation(player, 15F); }
            case "bottle2_korl" -> { player.heal(6F); add(player, Effects.speed(), 30 * 20, 1); add(player, Effects.haste(), 30 * 20, 2); add(player, Effects.strength(), 30 * 20, 2); }
            case "bottle2_fritz" -> { player.heal(6F); add(player, Effects.speed(), 30 * 20, 1); add(player, Effects.resistance(), 30 * 20, 2); add(player, Effects.jumpBoost(), 30 * 20, 2); }
            case "bottle_sparkle" -> { player.heal(10F); add(player, Effects.speed(), 120 * 20, 1); add(player, Effects.resistance(), 120 * 20, 2); add(player, Effects.strength(), 120 * 20, 2); add(player, Effects.haste(), 120 * 20, 1); HbmLivingProps.incrementRadiation(player, 5F); }
            case "bottle_rad" -> { player.heal(10F); add(player, Effects.speed(), 120 * 20, 1); add(player, Effects.resistance(), 120 * 20, 2); add(player, MobEffects.FIRE_RESISTANCE, 120 * 20, 0); add(player, Effects.strength(), 120 * 20, 4); add(player, Effects.haste(), 120 * 20, 1); HbmLivingProps.incrementRadiation(player, 15F); }
            case "coffee", "coffee_radium" -> { player.heal(10F); add(player, Effects.speed(), 60 * 20, 2); if (id.equals("coffee_radium")) HbmLivingProps.incrementRadiation(player, 500F); }
            default -> { }
        }
        VersatileConfig.applyPotionSickness(player, 5);
        if (player.getAbilities().instabuild) return stack;
        stack.shrink(1);
        Item container = id.startsWith("can_") ? ModItems.can_empty.get()
                : id.startsWith("bottle") ? ModItems.bottle_empty.get() : null;
        if (container == null) return stack;
        ItemStack empty = container.getDefaultInstance();
        ItemStack cap = switch (id) {
            case "bottle_nuka", "bottle_cherry" -> ModItems.cap_nuka.get().getDefaultInstance();
            case "bottle_quantum" -> ModItems.cap_quantum.get().getDefaultInstance();
            case "bottle_sparkle" -> ModItems.cap_sparkle.get().getDefaultInstance();
            case "bottle_rad" -> ModItems.cap_rad.get().getDefaultInstance();
            case "bottle2_korl" -> ModItems.cap_korl.get().getDefaultInstance();
            case "bottle2_fritz" -> ModItems.cap_fritz.get().getDefaultInstance();
            default -> ModItems.ring_pull.get().getDefaultInstance();
        };
        if (!stack.isEmpty()) giveOrDrop(player, empty);
        else stack = empty;
        giveOrDrop(player, cap);
        return stack;
    }

    /** The {@code ItemModFood.onFoodEaten} overrides, minus the vortex, waffle and pancake. */
    static void food(String id, ServerLevel level, Player player) {
        if (id.startsWith("canned_conserve_")) {
            player.getInventory().add(new ItemStack(ModItems.can_key.get()));
            if (id.equals("canned_conserve_recursion") && level.getRandom().nextInt(10) > 0) {
                player.getInventory().add(new ItemStack(ModFinalConsumableItems.canned(EnumFoodType.RECURSION)));
            } else if (id.equals("canned_conserve_fist")) {
                player.hurt(level.damageSources().magic(), 2F);
            }
            return;
        }
        int max = Integer.MAX_VALUE;
        switch (id) {
            case "apple_schrabidium" -> {
                Effects.add(player, MobEffects.REGENERATION, 600, 4);
                Effects.add(player, Effects.resistance(), 6000, 0);
                Effects.add(player, MobEffects.FIRE_RESISTANCE, 6000, 0);
            }
            case "apple_schrabidium_ingot" -> {
                Effects.add(player, MobEffects.REGENERATION, 1200, 4);
                Effects.add(player, Effects.resistance(), 1200, 4);
                Effects.add(player, MobEffects.FIRE_RESISTANCE, 1200, 0);
                Effects.add(player, Effects.strength(), 1200, 4);
                Effects.add(player, Effects.haste(), 1200, 2);
                Effects.add(player, Effects.speed(), 1200, 2);
                Effects.add(player, Effects.jumpBoost(), 1200, 4);
                Effects.add(player, MobEffects.HEALTH_BOOST, 1200, 9);
                Effects.add(player, MobEffects.ABSORPTION, 1200, 4);
                Effects.add(player, MobEffects.SATURATION, 1200, 9);
            }
            case "apple_schrabidium_block" -> {
                Effects.add(player, MobEffects.REGENERATION, max, 4);
                Effects.add(player, Effects.resistance(), max, 1);
                Effects.add(player, MobEffects.FIRE_RESISTANCE, max, 0);
                Effects.add(player, Effects.strength(), max, 9);
                Effects.add(player, Effects.haste(), max, 4);
                Effects.add(player, Effects.speed(), max, 3);
                Effects.add(player, Effects.jumpBoost(), max, 4);
                Effects.add(player, MobEffects.HEALTH_BOOST, max, 24);
                Effects.add(player, MobEffects.ABSORPTION, max, 14);
                Effects.add(player, MobEffects.SATURATION, max, 99);
            }
            case "apple_lead" -> Effects.addRegistered(player, ModEffects.LEAD.get(), 15 * 20, 2);
            case "apple_lead_ingot" -> Effects.addRegistered(player, ModEffects.LEAD.get(), 60 * 20, 4);
            case "apple_lead_block" -> player.hurt(ModDamageSources.lead(level), 500F);
            case "apple_euphemium" -> {
                Effects.add(player, Effects.resistance(), max, 120);
                Effects.add(player, MobEffects.FIRE_RESISTANCE, max, 0);
                Effects.add(player, MobEffects.SATURATION, max, 120);
            }
            case "cotton_candy" -> {
                Effects.add(player, MobEffects.POISON, 15 * 20, 0);
                Effects.add(player, MobEffects.WITHER, 5 * 20, 0);
                Effects.add(player, MobEffects.WEAKNESS, 25 * 20, 2);
                Effects.add(player, Effects.speed(), 25 * 20, 2);
                Effects.add(player, Effects.resistance(), 30 * 20, 4);
            }
            case "mucho_mango" -> Effects.add(player, Effects.speed(), 200, 0);
            case "schnitzel_vegan" -> {
                Effects.add(player, MobEffects.BLINDNESS, 10 * 20, 0);
                Effects.add(player, MobEffects.NAUSEA, 30 * 20, 0);
                Effects.add(player, MobEffects.HUNGER, 3 * 60 * 20, 4);
                Effects.add(player, MobEffects.WITHER, 3 * 20, 0);
                Effects.ignite(player, 5 * 20);
                Vec3 motion = player.getDeltaMovement();
                player.setDeltaMovement(motion.x, 2, motion.z);
                player.hurtMarked = true;
            }
            case "tem_flakes", "tem_flakes_normal", "tem_flakes_expensive" -> player.heal(2F);
            default -> throw new IllegalArgumentException(id);
        }
    }

    /** {@code ItemCigarette.finishUsingItem} without the No. 9 criterion. */
    static ItemStack cigarette(boolean cigarette, ItemStack stack, ServerLevel level, LivingEntity entity) {
        stack.shrink(1);
        if (cigarette) {
            HbmLivingProps.incrementBlackLung(entity, 2000);
            HbmLivingProps.incrementAsbestos(entity, 2000);
            HbmLivingProps.incrementRadiation(entity, 100F);
        } else {
            HbmLivingProps.incrementBlackLung(entity, 500);
            Effects.add(entity, MobEffects.NAUSEA, 200, 0);
            entity.heal(10F);
        }
        play(level, entity, ModSounds.COUGH.get());
        return stack;
    }

    /** {@code ItemBDCL.finishUsingItem}. */
    static ItemStack bdcl(ItemStack stack, ServerLevel level, Player player) {
        if (!player.getAbilities().instabuild) stack.shrink(1);
        play(level, player, ModGunSounds.PLAYER_GROAN.get());
        return stack;
    }

    private static void removeNegative(LivingEntity entity) {
        Effects.remove(entity, MobEffects.BLINDNESS);
        Effects.remove(entity, MobEffects.NAUSEA);
        Effects.remove(entity, MobEffects.MINING_FATIGUE);
        Effects.remove(entity, MobEffects.HUNGER);
        Effects.remove(entity, MobEffects.SLOWNESS);
        Effects.remove(entity, MobEffects.POISON);
        Effects.remove(entity, MobEffects.WEAKNESS);
        Effects.remove(entity, MobEffects.WITHER);
        Effects.removeRegistered(entity, ModEffects.RADIATION.get());
    }

    private static void add(Player player, Object effect, int duration, int amplifier) {
        Effects.add(player, effect, duration, amplifier);
    }

    private static void play(ServerLevel level, LivingEntity target, SoundEvent sound) {
        level.playSound(null, target.getX(), target.getY(), target.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void giveSoundAndDecrement(ServerLevel level, Player player, InteractionHand hand, SoundEvent sound,
                                              Item container) {
        play(level, player, sound);
        shrinkAndReplace(player, hand, container);
    }

    private static void shrinkAndReplace(Player player, InteractionHand hand, Item replacement) {
        ItemStack held = player.getItemInHand(hand);
        held.shrink(1);
        ItemStack result = replacement.getDefaultInstance();
        if (held.isEmpty()) player.setItemInHand(hand, result);
        else giveOrDrop(player, result);
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
}
