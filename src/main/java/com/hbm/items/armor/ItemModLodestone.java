package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code ItemModLodestone}: lodestone (5), horseshoe magnet (8) and industrial magnet (12) pull
 * item entities toward the wearer. Runs on both sides, as upstream's living update did, so the pull is
 * predicted on the client. Upstream's magnet toggle keybind is not ported; the magnet is always on,
 * which was the toggle's default.
 */
public class ItemModLodestone extends ItemArmorMod {

    private final int range;

    public ItemModLodestone(int range, Item.Properties properties) {
        super(ArmorModHandler.extra, true, true, true, true, properties);
        this.range = range;
    }

    public int range() {
        return range;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(coloured(ChatFormatting.DARK_GRAY, "Attracts nearby items"));
        tooltip.add(coloured(ChatFormatting.DARK_GRAY, "Item attraction range: " + range));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(ChatFormatting.DARK_GRAY, stack, " (Magnetic range: " + range + ")"));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        // 1.7: a player who toggled the magnet off (TOGGLE_MAGNET) pulls nothing
        if (entity instanceof net.minecraft.world.entity.player.Player player
                && !com.hbm.extprop.HbmPlayerProps.getData(player).enableMagnet) return;
        List<ItemEntity> items = entity.level().getEntitiesOfClass(ItemEntity.class,
                entity.getBoundingBox().inflate(range, range, range));
        for (ItemEntity item : items) {
            Vec3 vec = new Vec3(entity.getX() - item.getX(), entity.getY() - item.getY(),
                    entity.getZ() - item.getZ()).normalize();
            Vec3 motion = item.getDeltaMovement().add(vec.x * 0.05, vec.y * 0.05, vec.z * 0.05);
            if (vec.y > 0 && motion.y < 0.04) motion = motion.add(0, 0.2, 0);
            item.setDeltaMovement(motion);
        }
    }
}
