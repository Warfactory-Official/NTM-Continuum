package com.hbm.items.special;

import com.hbm.handler.ConsumableTreatment;
import com.hbm.handler.ConsumableTreatment.Context;
import com.hbm.handler.ConsumableTreatment.Trigger;
import com.hbm.handler.ConsumableTreatments;
import com.hbm.util.EnchantmentUtil;
import com.hbm.util.EntityDamageUtil;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** The three RadAway doses and the four IV bags, used on right-click; each is its {@code hbm:consumable_treatment}. */
public final class ItemSimpleConsumable extends CompatItem implements ConsumableTreatments.Handled {

    private final String id;

    public ItemSimpleConsumable(Item.Properties properties, String id) {
        super(properties);
        this.id = id;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ConsumableTreatment treatment = ConsumableTreatments.of(this);
        if (level instanceof ServerLevel serverLevel && treatment != null && treatment.has(Trigger.USE)
                && allowed(player) && !treatment.blocked(player)) {
            treatment.run(Context.use(serverLevel, player, player, hand));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * 1.7 {@code ItemSimpleConsumable.init}, "BLOOD BAGS": the empty bags work only for a real player, and the empty
     * experience bag only when the player has the 100 experience it takes. These checks stay in Java.
     */
    private boolean allowed(Player player) {
        return switch (id) {
            case "iv_empty" -> EntityDamageUtil.isRealPlayer(player);
            case "iv_xp_empty" -> EntityDamageUtil.isRealPlayer(player)
                    && EnchantmentUtil.getTotalExperience(player) >= 100;
            default -> true;
        };
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        // The old ItemSimpleConsumable had no custom tooltip text.
    }
}
