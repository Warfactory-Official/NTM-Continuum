package com.hbm.items.special;

import com.hbm.Constants;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** The legacy heat countdown carried by forged items such as chainsteel. */
public class ItemHot extends CompatItem {

    private static final GolemComponent<Integer> HEAT =
            GolemComponent.integer(Constants.MOD_ID, "heat");

    /**
     * 1.7's heat scale. 1.7 kept it in a static field, so every hot item used the last constructor argument:
     * ingot_chainsteel's 100 (ModItems.java:2312, after the four 200s). The static getMaxHeat also hides
     * ItemHotDusted's per-forge value, so a smelted dusted ingot always gets this too.
     */
    public static final int LEGACY_HEAT = 100;

    public final int maxHeat;

    public ItemHot(Item.Properties properties, int maxHeat) {
        super(properties);
        this.maxHeat = maxHeat;
    }

    /** Forces this class's component declaration before the loader flushes deferred registries. */
    public static void init() {}

    public static int getHeatValue(ItemStack stack) {
        return stack.getItem() instanceof ItemHot ? HEAT.getOr(stack, 0) : 0;
    }

    public static void setHeat(ItemStack stack, int heat) {
        if (!(stack.getItem() instanceof ItemHot hot)) return;
        int clamped = Math.max(0, Math.min(hot.maxHeat, heat));
        if (clamped == 0) HEAT.remove(stack);
        else HEAT.set(stack, clamped);
    }

    public static ItemStack heatUp(ItemStack stack) {
        if (stack.getItem() instanceof ItemHot hot) setHeat(stack, hot.maxHeat);
        return stack;
    }

    public static ItemStack heatUp(ItemStack stack, double multiplier) {
        if (stack.getItem() instanceof ItemHot hot) setHeat(stack, (int) (multiplier * hot.maxHeat));
        return stack;
    }

    public static double getHeat(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemHot hot) || hot.maxHeat <= 0) return 0D;
        return (double) getHeatValue(stack) / hot.maxHeat;
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level,
                                       net.minecraft.world.entity.Entity owner,
                                       net.minecraft.world.entity.EquipmentSlot slot, boolean held) {
        int heat = getHeatValue(stack);
        if (heat > 0) setHeat(stack, heat - 1);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
    }
}
