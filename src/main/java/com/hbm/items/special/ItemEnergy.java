package com.hbm.items.special;

import com.hbm.advancement.HbmCriteria;
import com.hbm.capability.HbmLivingProps;
import com.hbm.config.VersatileConfig;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.DropChanceMutatorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.ModConsumableTailItems;
import com.hbm.items.ModItems;
import com.hbm.particle.helper.ExplosionCreator;
import dev.golem.core.hook.Hook;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.Effects;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Modern port of 1.12.2's ItemEnergy. The old item family used one implementation with per-item
 * branches; keeping the id table here preserves the exact source effects while sharing the modern
 * use and container handling.
 */
public final class ItemEnergy extends CompatItem {
    public enum Container { NONE, CAN, BOTTLE }

    private final String id;
    private final Container container;

    public ItemEnergy(Item.Properties properties, String id, Container container) {
        super(properties);
        this.id = id;
        this.container = container;
    }

    @Override
    protected int compatUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    protected UseAnimation compatUseAnimation(ItemStack stack) {
        return UseAnimation.DRINK;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (VersatileConfig.hasPotionSickness(player)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (container == Container.BOTTLE && !hasBottleOpener(player)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof Player player)) return stack;
        apply(server, player);
        // 1.7 achRadium fires inside coffee_radium's onEaten branch; the criterion names the coffee
        if (id.equals("coffee_radium") && player instanceof ServerPlayer drinker) {
            HbmCriteria.CONSUMED.fire(drinker, stack.copyWithCount(1));
        }
        // 1.7 VersatileConfig.applyPotionSickness(player, 5): off at the default PotionConfig
        VersatileConfig.applyPotionSickness(player, 5);
        if (player.getAbilities().instabuild) return stack;

        stack.shrink(1);
        ItemStack empty;
        if (container == Container.NONE) {
            Item given = null;
            for (Function<String, Item> listener : EMPTY) {
                given = listener.apply(id);
                if (given != null) break;
            }
            if (given == null) return stack;
            empty = given.getDefaultInstance();
        } else {
            empty = (container == Container.CAN ? ModItems.can_empty.get() : ModItems.bottle_empty.get()).getDefaultInstance();
        }
        ItemStack cap = capFor(id);
        if (!stack.isEmpty()) {
            if (!player.getInventory().add(empty)) player.drop(empty, false);
        } else {
            stack = empty;
        }
        if (cap != null && !player.getInventory().add(cap)) player.drop(cap, false);
        return stack;
    }

    /**
     * What a drink without a can or bottle leaves (ADDON-API-INVENTORY H-75), by its id ({@code "coffee"}); the first
     * non-null item wins, none leaves nothing (1.7). HBM registers none; NTM: Space gives coffee its mug.
     */
    public static final Hook<Function<String, Item>> EMPTY = Hook.notifying();

    private static ItemStack capFor(String id) {
        return switch (id) {
            case "can_smart", "can_creature", "can_redbomb", "can_mrsugar", "can_overcharge",
                    "can_luna", "can_bepis", "can_breen", "can_mug" -> ModItems.ring_pull.get().getDefaultInstance();
            case "bottle_nuka", "bottle_cherry" -> ModItems.cap_nuka.get().getDefaultInstance();
            case "bottle_quantum" -> ModItems.cap_quantum.get().getDefaultInstance();
            case "bottle_sparkle" -> ModItems.cap_sparkle.get().getDefaultInstance();
            case "bottle_rad" -> ModItems.cap_rad.get().getDefaultInstance();
            case "bottle2_korl" -> ModItems.cap_korl.get().getDefaultInstance();
            case "bottle2_fritz" -> ModItems.cap_fritz.get().getDefaultInstance();
            default -> null;
        };
    }

    private static boolean hasBottleOpener(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
            if (player.getInventory().getItem(slot).is(ModConsumableTailItems.bottle_opener.get())) return true;
        if (player.getOffhandItem().is(ModConsumableTailItems.bottle_opener.get())) return true;
        return false;
    }


    private void apply(ServerLevel level, Player player) {
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
            // 1.7: "Totally not made from nitroglycerine." A size-50 blast at chest height that drops nothing.
            case "chocolate_milk" -> {
                double y = player.getY() + player.getBbHeight() / 2;
                ExplosionCreator.composeEffectStandard(level, player.getX(), y, player.getZ());
                ExplosionVNT xnt = new ExplosionVNT(level, player.getX(), y, player.getZ(), 50F);
                xnt.setBlockAllocator(new BlockAllocatorStandard(32));
                xnt.setBlockProcessor(new BlockProcessorStandard().withChance(new DropChanceMutatorStandard(0F)));
                xnt.setEntityProcessor(new EntityProcessorCross(5D));
                xnt.setPlayerProcessor(new PlayerProcessorStandard());
                xnt.explode();
            }
            case "coffee", "coffee_radium" -> { player.heal(10F); add(player, Effects.speed(), 60 * 20, 2); if (id.equals("coffee_radium")) HbmLivingProps.incrementRadiation(player, 500F); }
            default -> { }
        }
    }

    private static void add(Player player, Object effect, int duration, int amplifier) {
        Effects.add(player, effect, duration, amplifier);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        switch (id) {
            case "can_smart" -> tooltip.add(Component.literal("Cheap and full of bubbles"));
            case "can_creature" -> tooltip.add(Component.literal("Basically gasoline in a tin can"));
            case "can_redbomb" -> tooltip.add(Component.literal("Liquefied explosives"));
            case "can_mrsugar" -> tooltip.add(Component.literal("An intellectual drink, for the chosen ones!"));
            case "can_overcharge" -> tooltip.add(Component.literal("Possible side effects include heart attacks, seizures or zombification"));
            case "can_luna" -> tooltip.add(Component.literal("Contains actual selenium and star metal. Tastes like night."));
            case "can_bepis" -> tooltip.add(Component.literal("beppp"));
            case "can_breen" -> tooltip.add(Component.literal("Don't drink the water. They put something in it, to make you forget."));
            case "chocolate_milk" -> {
                tooltip.add(Component.literal("Regular chocolate milk. Safe to drink."));
                tooltip.add(Component.literal("Totally not made from nitroglycerine."));
            }
            case "bottle_nuka" -> tooltip.add(Component.literal("Contains about 210 kcal and 1500 mSv."));
            case "bottle_cherry" -> tooltip.add(Component.literal("Now with severe radiation poisoning in every seventh bottle!"));
            case "bottle_quantum" -> tooltip.add(Component.literal("Comes with a colorful mix of over 70 isotopes!"));
            case "bottle2_korl" -> tooltip.add(Component.literal("Contains actual orange juice!"));
            case "bottle2_fritz" -> tooltip.add(Component.literal("moremore caffeine"));
            case "bottle_sparkle" -> tooltip.add(Component.literal("The most delicious beverage in the wasteland!"));
            case "bottle_rad" -> tooltip.add(Component.literal("Tastes like radish and radiation."));
            default -> { }
        }
        if (container == Container.BOTTLE) tooltip.add(Component.literal("[Requires bottle opener]"));
    }
}
