package com.hbm.handler;

import com.hbm.api.data.HbmDataMaps;
import com.hbm.handler.ConsumableTreatment.Context;
import com.hbm.handler.ConsumableTreatment.Trigger;
import dev.golem.core.world.ItemClicks;
import dev.golem.core.world.ItemUses;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Runs {@code hbm:consumable_treatment} for items that do not run it themselves (vanilla foods, other mods' items):
 * {@code consume} after Core's finished-use hook, {@code use} on Core's item right-click, and potion sickness keeps
 * either from starting. HBM's consumables implement {@link Handled} and run their treatment at the 1.7 point.
 */
public final class ConsumableTreatments {

    /** An item whose class runs its own treatment; the global hooks leave it alone. */
    public interface Handled {}

    private static boolean installed;

    private ConsumableTreatments() {}

    public static ConsumableTreatment of(ItemStack stack) {
        return stack.isEmpty() ? null : HbmDataMaps.CONSUMABLE_TREATMENT.get(stack.getItem());
    }

    public static ConsumableTreatment of(Item item) {
        return HbmDataMaps.CONSUMABLE_TREATMENT.get(item);
    }

    /** Whether {@code item}'s treatment has {@code trigger}. */
    public static boolean has(Item item, Trigger trigger) {
        ConsumableTreatment treatment = of(item);
        return treatment != null && treatment.has(trigger);
    }

    /** Whether potion sickness on {@code entity} blocks using {@code item}. */
    public static boolean blocked(Item item, LivingEntity entity) {
        ConsumableTreatment treatment = of(item);
        return treatment != null && treatment.blocked(entity);
    }

    public static synchronized void init() {
        if (installed) return;
        installed = true;
        ItemUses.onFinish(ConsumableTreatments::finished);
        ItemClicks.register(ConsumableTreatments::clicked);
    }

    static void finished(LivingEntity entity, ItemStack before) {
        if (before.getItem() instanceof Handled || !(entity.level() instanceof ServerLevel level)) return;
        ConsumableTreatment treatment = of(before);
        if (treatment != null && treatment.has(Trigger.CONSUME)) treatment.run(Context.eaten(level, entity));
    }

    /** Public for the GameTests, which cannot raise the loader event. */
    public static InteractionResult clicked(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof Handled) return InteractionResult.TRY_WITH_EMPTY_HAND;
        ConsumableTreatment treatment = of(held);
        if (treatment == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (treatment.has(Trigger.USE)) {
            if (treatment.blocked(player)) return InteractionResult.FAIL;
            if (player.level() instanceof ServerLevel level) treatment.run(Context.use(level, player, player, hand));
            return InteractionResult.SUCCESS;
        }
        if (treatment.has(Trigger.CONSUME) && treatment.blocked(player)) return InteractionResult.FAIL;
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
