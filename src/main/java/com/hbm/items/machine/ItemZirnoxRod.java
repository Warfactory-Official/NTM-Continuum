package com.hbm.items.machine;

import com.hbm.items.ModDataComponents;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A single active, component-backed ZIRNOX fuel variant. */
public class ItemZirnoxRod extends CompatItem {

    private final ZirnoxFuel fuel;

    public ItemZirnoxRod(Properties properties, ZirnoxFuel fuel) {
        super(properties);
        this.fuel = fuel;
    }

    public ZirnoxFuel fuel() {
        return fuel;
    }

    /**
     * This rod's values: the {@code hbm:zirnox_fuel} item data map, else HBM's enum constant. An addon's rod is its own
     * {@code ItemZirnoxRod} (any constant) with a data-map entry.
     */
    public com.hbm.api.data.ReactorDataMaps.ZirnoxFuel stats() {
        com.hbm.api.data.ReactorDataMaps.ZirnoxFuel data = com.hbm.api.data.ReactorDataMaps.ZIRNOX_FUEL.get(this);
        if (data != null) return data;
        return codeStats();
    }

    /** The code defaults (constructor or enum values) that the data map entry overrides. */
    public com.hbm.api.data.ReactorDataMaps.ZirnoxFuel codeStats() {
        String into = fuel == ZirnoxFuel.TH232 ? key(com.hbm.items.ModZirnoxItems.active(ZirnoxFuel.THORIUM))
                : fuel == ZirnoxFuel.LITHIUM ? key(com.hbm.items.ModItems.rod_zirnox_tritium.get())
                : key(com.hbm.items.ModZirnoxItems.depleted(fuel));
        java.util.Optional<String> waste = com.hbm.items.ModZirnoxItems.hasDepleted(fuel)
                ? java.util.Optional.of(key(com.hbm.items.ModZirnoxItems.waste(fuel))) : java.util.Optional.empty();
        return new com.hbm.api.data.ReactorDataMaps.ZirnoxFuel(fuel.maxLife, fuel.heat, fuel.breeding, fuel.baseRadiation,
                fuel.depletedRadiation, into, waste);
    }

    private static String key(net.minecraft.world.item.Item item) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
    }

    public int getLife(ItemStack stack) {
        return ModDataComponents.zirnoxLife(stack);
    }

    public void setLife(ItemStack stack, int life) {
        ModDataComponents.zirnoxLife(stack, Math.max(0, life));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getLife(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.max(0, Math.round(13F * (1F - Math.min(1F, (float) getLife(stack) / stats().maxLife()))));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x53C653;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         java.util.List<Component> tooltip, TooltipFlag flag) {
        com.hbm.api.data.ReactorDataMaps.ZirnoxFuel fuel = stats();
        tooltip.add(Component.translatable("item.hbm.zirnox_rod.life", getLife(stack), fuel.maxLife())
                .withStyle(ChatFormatting.GRAY));
        if (fuel.breeding()) {
            tooltip.add(Component.translatable("desc.item.zirnoxBreedingRod").withStyle(ChatFormatting.GOLD));
        }
    }
}
