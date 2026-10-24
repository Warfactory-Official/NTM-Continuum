package com.hbm.items.special;

import com.hbm.advancement.HbmCriteria;
import com.hbm.handler.ConsumableTreatment;
import com.hbm.handler.ConsumableTreatment.Context;
import com.hbm.handler.ConsumableTreatments;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.DropChanceMutatorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.ModConsumableTailItems;
import com.hbm.particle.helper.ExplosionCreator;
import dev.golem.core.hook.Hook;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 1.12.2's ItemEnergy: cans, bottles and coffee drunk over 32 ticks, the bottles only with an opener in the inventory.
 * Each drink's effects, its can or bottle and its ring pull or cap are its {@code hbm:consumable_treatment}.
 */
public final class ItemEnergy extends CompatItem implements ConsumableTreatments.Handled {
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
        if (ConsumableTreatments.blocked(this, player)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (container == Container.BOTTLE && !hasBottleOpener(player)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    /** Effects, the can or bottle and its ring pull or cap are the treatment; the chocolate milk's blast is not data. */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof Player player)) return stack;
        ItemStack drunk = stack.copyWithCount(1);
        if (id.equals("chocolate_milk")) blast(server, player);
        ConsumableTreatment treatment = ConsumableTreatments.of(this);
        Context context = Context.consume(server, player, stack).fallbackRemainder(emptyFor(id));
        if (treatment != null) treatment.run(context);
        // 1.7 achRadium fires inside coffee_radium's onEaten branch; the criterion names the coffee
        if (id.equals("coffee_radium") && player instanceof ServerPlayer drinker) {
            HbmCriteria.CONSUMED.fire(drinker, drunk);
        }
        return context.result;
    }

    /**
     * What a drink without a can or bottle leaves (ADDON-API-INVENTORY H-75), by its id ({@code "coffee"}); the first
     * non-null item wins, none leaves nothing (1.7). HBM registers none; NTM: Space gives coffee its mug. A treatment's
     * {@code consume_item} that names a remainder wins over it.
     */
    public static final Hook<Function<String, Item>> EMPTY = Hook.notifying();

    private static Item emptyFor(String id) {
        for (Function<String, Item> listener : EMPTY) {
            Item given = listener.apply(id);
            if (given != null) return given;
        }
        return null;
    }

    private static boolean hasBottleOpener(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
            if (player.getInventory().getItem(slot).is(ModConsumableTailItems.bottle_opener.get())) return true;
        if (player.getOffhandItem().is(ModConsumableTailItems.bottle_opener.get())) return true;
        return false;
    }

    /** 1.7: "Totally not made from nitroglycerine." A size-50 blast at chest height that drops nothing. */
    private static void blast(ServerLevel level, Player player) {
        double y = player.getY() + player.getBbHeight() / 2;
        ExplosionCreator.composeEffectStandard(level, player.getX(), y, player.getZ());
        ExplosionVNT xnt = new ExplosionVNT(level, player.getX(), y, player.getZ(), 50F);
        xnt.setBlockAllocator(new BlockAllocatorStandard(32));
        xnt.setBlockProcessor(new BlockProcessorStandard().withChance(new DropChanceMutatorStandard(0F)));
        xnt.setEntityProcessor(new EntityProcessorCross(5D));
        xnt.setPlayerProcessor(new PlayerProcessorStandard());
        xnt.explode();
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
