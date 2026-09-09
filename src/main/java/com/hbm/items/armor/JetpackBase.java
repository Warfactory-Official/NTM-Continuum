package com.hbm.items.armor;

import com.hbm.handler.JetpackHandler;
import com.hbm.items.HbmArmorMaterials;
import com.hbm.items.ModDataComponents;
import com.hbm.items.gear.ArmorFSB;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Wearable chest jetpack base, also accepted as the legacy chestplate armor module. */
public abstract class JetpackBase extends ArmorFSB {

    protected JetpackBase(Properties properties) {
        super(HbmArmorMaterials.JETPACK, ArmorSlot.CHESTPLATE, properties);
    }

    public static int getFuel(ItemStack stack) {
        return ModDataComponents.JETPACK_FUEL.getOr(stack, 0);
    }

    public static void setFuel(ItemStack stack, int fuel) {
        ModDataComponents.JETPACK_FUEL.set(stack, Math.max(0, fuel));
    }

    @Override
    public void armorTick(Level level, Player player, ItemStack stack) {
        tickFlight(level, player, stack);
    }

    protected abstract void tickFlight(Level level, Player player, ItemStack stack);

    protected void useUpFuel(Player player, ItemStack stack, int rate) {
        if (rate > 0 && player.tickCount % rate == 0) setFuel(stack, getFuel(stack) - 1);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        super.appendCompatHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("Can be worn on its own!").withStyle(ChatFormatting.GOLD));
    }

    /** The old module table treats every JetpackBase as the chestplate-only module. */
    public final int moduleSlot() {
        return com.hbm.handler.ArmorModHandler.plate_only;
    }

    public final boolean isActive(Player player) {
        return JetpackHandler.isJetpackActive(player);
    }
}
