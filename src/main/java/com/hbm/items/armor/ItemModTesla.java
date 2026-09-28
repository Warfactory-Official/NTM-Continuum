package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.hbm.items.gear.ArmorFSBPowered;
import com.hbm.util.TeslaZap;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code ItemModTesla} (Back-Mounted Tesla Coil): with a complete electric suit, zaps everything
 * within 5 blocks every tick like a tesla coil, draining the suit one hit's worth 20% of the time it hits
 * something. The coil is drawn on the back by {@code ArmorModRenderLayer}; upstream's arc beams were
 * commented out, so there are none here either.
 */
public class ItemModTesla extends ItemArmorMod {

    public ItemModTesla(Item.Properties properties) {
        super(ArmorModHandler.plate_only, false, true, false, false, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(coloured(ChatFormatting.YELLOW, "Zaps nearby entities (requires full electric set)"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        // No leading indent: upstream's line is the bare name.
        tooltip.add(stack.getHoverName().copy().append(" (zaps nearby entities)").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!(entity.level() instanceof ServerLevel level) || !(entity instanceof Player player)
                || !(armor.getItem() instanceof ArmorFSBPowered powered) || !ArmorFSBPowered.hasFSBArmor(player)) return;
        List<Vec3> targets = TeslaZap.zap(level, entity.getX(), entity.getY() + 1.25, entity.getZ(), 5, entity);
        if (targets != null && !targets.isEmpty() && entity.getRandom().nextInt(5) == 0) {
            powered.dischargeBattery(armor, powered.consumption);
        }
    }
}
