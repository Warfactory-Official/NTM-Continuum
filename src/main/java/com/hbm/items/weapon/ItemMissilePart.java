package com.hbm.items.weapon;

import com.hbm.api.fluid.FluidType;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A functional missile-part record for the legacy custom thruster family. */
public final class ItemMissilePart extends CompatItem {

    public enum PartType { THRUSTER }
    public enum PartSize { SIZE_10 }

    public final PartType type = PartType.THRUSTER;
    public final PartSize size;
    public final FluidType fuel;
    public final float consumption;
    public final float lift;
    public final float health;

    public ItemMissilePart(Item.Properties properties, FluidType fuel, float consumption, float lift,
                           PartSize size, float health) {
        super(properties);
        this.fuel = fuel;
        this.consumption = consumption;
        this.lift = lift;
        this.size = size;
        this.health = health;
    }

    public FluidType getFuel() { return fuel; }
    public float getConsumption() { return consumption; }
    public float getLift() { return lift; }
    public float getHealth() { return health; }
    public PartSize getSize() { return size; }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("desc.size").append(Component.literal(" 1.0m")));
        tooltip.add(Component.translatable("desc.fuelamnt").append(Component.literal(" "))
                .append(fuel.getDisplayName()));
        tooltip.add(Component.literal("Thrust: 1.5N"));
        tooltip.add(Component.literal("ISP: 1s"));
        tooltip.add(Component.translatable("desc.health").append(Component.literal(" 5.0HP")));
    }
}
