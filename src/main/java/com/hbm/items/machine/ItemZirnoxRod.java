package com.hbm.items.machine;

import com.hbm.api.data.ReactorDataMaps;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.ModZirnoxItems;
import com.hbm.lib.Library;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
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
    public ReactorDataMaps.ZirnoxFuel stats() {
        ReactorDataMaps.ZirnoxFuel data = ReactorDataMaps.ZIRNOX_FUEL.get(this);
        if (data != null) return data;
        return codeStats();
    }

    /** The code defaults (constructor or enum values) that the data map entry overrides. */
    public ReactorDataMaps.ZirnoxFuel codeStats() {
        String into = fuel == ZirnoxFuel.TH232 ? key(ModZirnoxItems.active(ZirnoxFuel.THORIUM))
                : fuel == ZirnoxFuel.LITHIUM ? key(ModItems.rod_zirnox_tritium.get())
                : key(ModZirnoxItems.depleted(fuel));
        Optional<String> waste = ModZirnoxItems.hasDepleted(fuel)
                ? Optional.of(key(ModZirnoxItems.waste(fuel))) : Optional.empty();
        return new ReactorDataMaps.ZirnoxFuel(fuel.maxLife, fuel.heat, fuel.breeding, fuel.baseRadiation,
                fuel.depletedRadiation, into, waste);
    }

    private static String key(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
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
                                         List<Component> tooltip, TooltipFlag flag) {
        ReactorDataMaps.ZirnoxFuel fuel = stats();
        double depletion = (int) ((double) getLife(stack) / fuel.maxLife() * 100000) / 1000D;
        tooltip.add(Component.translatable("trait.rbmk.depletion", depletion + "%").withStyle(ChatFormatting.YELLOW));
        String key = fuel.breeding() ? "desc.item.zirnoxBreedingRod" : "desc.item.zirnoxRod";
        String life = Library.getShortNumber(fuel.maxLife());
        tooltip.add(Component.translatable(key + ".0"));
        if (fuel.breeding()) {
            tooltip.add(Component.translatable(key + ".1"));
        } else {
            tooltip.add(Component.translatable(key + ".1", fuel.heat()));
        }
        tooltip.add(Component.translatable(key + ".2", life));
    }
}
