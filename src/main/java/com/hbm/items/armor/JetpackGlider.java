package com.hbm.items.armor;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.handler.JetpackHandler;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModDataComponents;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Advanced four-fuel glider with the delayed J activation and H hover mode. */
public class JetpackGlider extends ItemArmorMod implements IFillableItem {

    public final int capacity;

    public JetpackGlider(int capacity, Properties properties) {
        super(ArmorModHandler.plate_only, false, true, false, false, properties);
        this.capacity = capacity;
    }

    public FluidTankNTM getTank(ItemStack stack) {
        FluidType type = ModDataComponents.fluid(stack);
        FluidTankNTM tank = new FluidTankNTM(type, capacity);
        tank.setFill(Math.max(0, Math.min(capacity, ModDataComponents.fluidAmount(stack))));
        return tank;
    }

    public void setTank(ItemStack stack, FluidTankNTM tank) {
        if (tank == null || tank.getFill() <= 0 || tank.getTankType() == null || tank.getTankType() == Fluids.NONE) {
            ModDataComponents.removeFluid(stack);
            ModDataComponents.removeFluidAmount(stack);
            return;
        }
        ModDataComponents.fluid(stack, tank.getTankType());
        ModDataComponents.fluidAmount(stack, Math.min(capacity, Math.max(0, tank.getFill())));
    }

    /** Called by the common armor-module tick; the old client handler supplied the same input path. */
    public void tickFlight(Player player, ItemStack stack) {
        JetpackHandler.tickGlider(player);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        FluidTankNTM tank = getTank(stack);
        FluidType type = tank.getTankType();
        tooltip.add(Component.literal("Fuel Type: " + (type == Fluids.NONE ? "None" : type.getDisplayNameString()))
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("Fuel Speed: " + JetpackHandler.getSpeed(type == Fluids.NONE ? null : type))
                .withStyle(ChatFormatting.RED));
        int percent = capacity <= 0 ? 0 : (int) ((float) tank.getFill() / capacity * 100F);
        tooltip.add(Component.literal("Fuel Amount: " + tank.getFill() + "/" + capacity + " (" + percent + "%)")
                .withStyle(ChatFormatting.RED));
    }

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        FluidType current = getTank(stack).getTankType();
        int fill = getTank(stack).getFill();
        return (current == Fluids.NONE || fill == 0)
                ? type == Fluids.KEROSENE || type == Fluids.KEROSENE_REFORM
                        || type == Fluids.BALEFIRE || type == Fluids.NITAN
                : type == current;
    }

    @Override
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (stack.getCount() > 1 || amount <= 0 || !acceptsFluid(type, stack)) return amount;
        FluidTankNTM tank = getTank(stack);
        int filled = Math.min(capacity - tank.getFill(), amount);
        if (filled <= 0) return amount;
        if (tank.getFill() == 0) tank.setTankType(type);
        tank.setFill(tank.getFill() + filled);
        setTank(stack, tank);
        return amount - filled;
    }

    @Override
    public boolean providesFluid(FluidType type, ItemStack stack) {
        return getTank(stack).getTankType() == type;
    }

    @Override
    public int tryEmpty(FluidType type, int amount, ItemStack stack) {
        if (stack.getCount() > 1) return 0;
        FluidTankNTM tank = getTank(stack);
        if (tank.getTankType() != type || amount <= 0) return 0;
        int drained = Math.min(tank.getFill(), amount);
        if (drained <= 0) return 0;
        tank.setFill(tank.getFill() - drained);
        if (tank.getFill() == 0) tank.setTankType(Fluids.NONE);
        setTank(stack, tank);
        return drained;
    }

    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        FluidType type = getTank(stack).getTankType();
        return type == Fluids.NONE ? null : type;
    }

    @Override
    public int getFill(ItemStack stack) {
        return getTank(stack).getFill();
    }
}
