package com.hbm.items.special;

import com.hbm.config.WeaponConfig;
import com.hbm.entity.effect.EntityQuasar;
import com.hbm.util.ContaminationUtil;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Digamma exposure while carried; a grounded particle becomes a quasar, as in 1.7 ItemDigamma. */
public final class ItemDigamma extends CompatItem {
    private final int digamma;

    public ItemDigamma(Properties properties, int digamma) {
        super(properties);
        this.digamma = digamma;
    }

    public static void tickDropped(ItemEntity entity) {
        if (!(entity.getItem().getItem() instanceof ItemDigamma) || !entity.onGround()
                || !(entity.level() instanceof ServerLevel level)) return;
        if (WeaponConfig.dropSing) {
            var quasar = new EntityQuasar(level, 5F);
            quasar.setPos(entity.position());
            level.addFreshEntity(quasar);
        }
        // 1.7 consumes the grounded particle even when quasar spawning is disabled.
        entity.discard();
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level, Entity owner,
                                       EquipmentSlot slot, boolean held) {
        if (owner instanceof LivingEntity living) {
            ContaminationUtil.applyDigammaData(living, 1.0 / digamma);
        }
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("trait.hlParticle", "1.67*10\u00b3\u2074 a")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("trait.hlPlayer", (digamma / 20D) + "s")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.empty());
        double mdrxPerSecond = ((int) ((1000D / digamma) * 200D)) / 10D;
        tooltip.add(Component.literal("[").append(Component.translatable("trait.digamma")).append("]")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal(mdrxPerSecond + "mDRX/s").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("[").append(Component.translatable("trait.drop")).append("]")
                .withStyle(ChatFormatting.RED));
    }
}
