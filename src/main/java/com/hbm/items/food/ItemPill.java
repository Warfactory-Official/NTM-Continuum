package com.hbm.items.food;

import com.hbm.capability.HbmLivingProps;
import com.hbm.lib.ModDamageSources;
import com.hbm.potion.ModEffects;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.Effects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/** Edible HBM pills, ported from the old {@code ItemPill}. */
public final class ItemPill extends CompatItem {

    private final String id;

    public ItemPill(Item.Properties properties, String id) {
        super(properties);
        this.id = id;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (Effects.hasRegistered(player, ModEffects.POTION_SICKNESS.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return compatVanillaUse(level, player, hand);
    }

    @Override
    protected int compatUseDuration(ItemStack stack, LivingEntity entity) {
        return 10;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide() && entity instanceof Player player) apply(player, level);
        return result;
    }

    private void apply(Player player, Level level) {
        Effects.addRegistered(player, ModEffects.POTION_SICKNESS.get(), 5 * 20, 0);

        switch (id) {
            case "pill_iodine" -> {
                Effects.remove(player, MobEffects.BLINDNESS);
                Effects.remove(player, MobEffects.NAUSEA);
                Effects.remove(player, MobEffects.MINING_FATIGUE);
                Effects.remove(player, MobEffects.HUNGER);
                Effects.remove(player, MobEffects.SLOWNESS);
                Effects.remove(player, MobEffects.POISON);
                Effects.remove(player, MobEffects.WEAKNESS);
                Effects.remove(player, MobEffects.WITHER);
                Effects.removeRegistered(player, ModEffects.RADIATION.get());
            }
            case "plan_c" -> {
                for (int i = 0; i < 10; i++) {
                    if (level.getRandom().nextBoolean()) {
                        dev.golem.core.world.Compat.hurt(player, (net.minecraft.server.level.ServerLevel) level,
                                ModDamageSources.euthanizedSelf((net.minecraft.server.level.ServerLevel) level), 1000F);
                    } else {
                        dev.golem.core.world.Compat.hurt(player, (net.minecraft.server.level.ServerLevel) level,
                                ModDamageSources.euthanizedSelf2((net.minecraft.server.level.ServerLevel) level), 1000F);
                    }
                }
            }
            case "pill_red" -> Effects.addRegistered(player, ModEffects.DEATH.get(), 60 * 60 * 20, 0);
            case "radx" -> Effects.addRegistered(player, ModEffects.RADX.get(), 3 * 60 * 20, 3);
            case "siox" -> {
                HbmLivingProps.setAsbestos(player, 0);
                HbmLivingProps.setBlackLung(player,
                        Math.min(HbmLivingProps.getBlackLung(player), HbmLivingProps.maxBlacklung / 5));
            }
            case "pill_herbal" -> {
                HbmLivingProps.setAsbestos(player, 0);
                HbmLivingProps.setBlackLung(player,
                        Math.min(HbmLivingProps.getBlackLung(player), HbmLivingProps.maxBlacklung / 5));
                HbmLivingProps.incrementRadiation(player, -100F);
                Effects.add(player, MobEffects.NAUSEA, 10 * 20, 0);
                Effects.add(player, MobEffects.WEAKNESS, 10 * 60 * 20, 2);
                Effects.add(player, MobEffects.MINING_FATIGUE, 10 * 60 * 20, 2);
                Effects.add(player, MobEffects.POISON, 5 * 20, 2);
                Effects.addRegistered(player, ModEffects.POTION_SICKNESS.get(), 10 * 60 * 20, 0);
            }
            case "xanax" -> HbmLivingProps.setDigamma(player,
                    Math.max(HbmLivingProps.getDigamma(player) - 0.5D, 0D));
            case "chocolate" -> {
                if (level.getRandom().nextInt(25) == 0) {
                    dev.golem.core.world.Compat.hurt(player, (net.minecraft.server.level.ServerLevel) level,
                            ModDamageSources.overdose((net.minecraft.server.level.ServerLevel) level), 1000F);
                }
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
            default -> {
            }
        }
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        switch (id) {
            case "pill_iodine" -> tooltip.add(Component.literal("Removes negative effects"));
            case "plan_c" -> tooltip.add(Component.literal("Deadly"));
            case "radx" -> tooltip.add(Component.literal("Increases radiation resistance by 0.4 for 3 minutes"));
            case "siox" -> tooltip.add(Component.literal("Reverses mesothelioma with the power of Asbestos!"));
            case "pill_herbal" -> {
                tooltip.add(Component.literal("Effective treatment against lung disease and mild radiation poisoning"));
                tooltip.add(Component.literal("Comes with side effects"));
            }
            case "xanax" -> tooltip.add(Component.literal("Removes 500mDRX"));
            case "fmn" -> tooltip.add(Component.literal("Removes all DRX above 2,000mDRX"));
            case "five_htp" -> tooltip.add(Component.literal("Removes all DRX, Stability for 10 minutes"));
            default -> {
            }
        }
    }
}
