package com.hbm.items.special;

import com.hbm.config.WeaponConfig;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModSingularityItems;
import com.hbm.api.fluidmk2.IFillableItem;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Component-backed replacement for the old metadata fluid cell used by the antimatter recipe. */
public final class ItemCell extends CompatItem implements IFillableItem {
    public static final int CAPACITY = 1_000;
    public ItemCell(Item.Properties properties) {
        super(properties);
    }

    public static boolean isEmptyCell(ItemStack stack) {
        return stack.getItem() instanceof ItemCell && getFluidType(stack) == null
                && ModDataComponents.fluidAmount(stack) == 0;
    }

    public static boolean isFullCell(ItemStack stack, FluidType fluid) {
        return storedAmount(stack) == CAPACITY && getFluidType(stack) == fluid;
    }

    public static FluidType getFluidType(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemCell)) return null;
        FluidType fluid = ModDataComponents.fluid(stack);
        return fluid == Fluids.NONE ? null : fluid;
    }

    public static ItemStack getFullCell(FluidType fluid) {
        return getFullCell(fluid, 1);
    }

    /** The second argument is the old stack count, not a fluid amount. */
    public static ItemStack getFullCell(FluidType fluid, int count) {
        if (!isCellFluid(fluid) || count <= 0) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(ModSingularityItems.cell.get(), count);
        setFluid(stack, fluid, CAPACITY);
        return stack;
    }

    private static void setFluid(ItemStack stack, FluidType fluid, int amount) {
        if (!isCellFluid(fluid) || amount <= 0) {
            ModDataComponents.removeFluid(stack);
            ModDataComponents.removeFluidAmount(stack);
            ModDataComponents.ITEM_VARIANT.remove(stack);
            return;
        }
        ModDataComponents.fluid(stack, fluid);
        ModDataComponents.fluidAmount(stack, Math.min(CAPACITY, amount));
        ModDataComponents.ITEM_VARIANT.set(stack, modelVariant(fluid));
    }

    /** {@code cell.json} cases: 1.7 cell metadata, not fluid indexes (UF6 = 28). */
    private static int modelVariant(FluidType fluid) {
        return fluid == Fluids.DEUTERIUM ? 8 : fluid == Fluids.TRITIUM ? 9 : fluid == Fluids.UF6 ? 28
                : fluid == Fluids.PUF6 ? 29 : fluid == Fluids.SAS3 ? 30 : fluid == Fluids.AMAT ? 33 : 34;
    }

    private static boolean isCellFluid(FluidType fluid) {
        return fluid == Fluids.UF6 || fluid == Fluids.PUF6 || fluid == Fluids.AMAT
                || fluid == Fluids.DEUTERIUM || fluid == Fluids.TRITIUM || fluid == Fluids.SAS3
                || fluid == Fluids.ASCHRAB;
    }

    /** Full cells leave one empty cell, while an empty cell is deliberately not a container item. */
    public static ItemStack remainder(ItemStack stack) {
        return isEmptyCell(stack) ? ItemStack.EMPTY
                : isFullCell(stack, getFluidType(stack)) ? new ItemStack(ModSingularityItems.cell.get())
                : ItemStack.EMPTY;
    }

    @Override
    public int getFill(ItemStack stack) {
        return storedAmount(stack);
    }

    private static int storedAmount(ItemStack stack) {
        return Math.max(0, Math.min(CAPACITY, ModDataComponents.fluidAmount(stack)));
    }

    @Override
    public boolean acceptsFluid(FluidType fluid, ItemStack stack) {
        return isCellFluid(fluid) && isEmptyCell(stack);
    }

    @Override
    public int tryFill(FluidType fluid, int amount, ItemStack stack) {
        if (amount < CAPACITY || !acceptsFluid(fluid, stack)) return amount;
        setFluid(stack, fluid, CAPACITY);
        return amount - CAPACITY;
    }

    @Override
    public boolean providesFluid(FluidType fluid, ItemStack stack) {
        return isFullCell(stack, fluid);
    }

    @Override
    public int tryEmpty(FluidType fluid, int amount, ItemStack stack) {
        if (amount < CAPACITY || !providesFluid(fluid, stack)) return 0;
        setFluid(stack, fluid, 0);
        return CAPACITY;
    }

    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        FluidType fluid = getFluidType(stack);
        return isFullCell(stack, fluid) ? fluid : Fluids.NONE;
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidType fluid = getFluidType(stack);
        if (fluid == null || fluid == Fluids.NONE) return Component.translatable("item.hbm.cell_empty");
        return Component.translatable("item.hbm.cell_" + (fluid == Fluids.DEUTERIUM ? "deuterium"
                : fluid == Fluids.TRITIUM ? "tritium" : fluid == Fluids.UF6 ? "uf6" : fluid == Fluids.PUF6 ? "puf6"
                : fluid == Fluids.SAS3 ? "sas3" : fluid == Fluids.AMAT ? "antimatter"
                : fluid == Fluids.ASCHRAB ? "anti_schrabidium" : "empty"));
    }

    /** Preserves the old AMAT ground/burning trigger for the dependency used by Spark production. */
    public static void tickDropped(ItemEntity entity) {
        if (!(entity.getItem().getItem() instanceof ItemCell)
                || (!entity.onGround() && !entity.isOnFire()) || entity.level().isClientSide()) return;
        if (WeaponConfig.dropCell && getFluidType(entity.getItem()) == Fluids.AMAT
                && entity.level() instanceof net.minecraft.server.level.ServerLevel level) {
            entity.discard();
            level.explode(entity, entity.getX(), entity.getY(), entity.getZ(), 10F, true,
                    Level.ExplosionInteraction.TNT);
        }
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        if (getFluidType(stack) == Fluids.AMAT) {
            tooltip.add(Component.literal("Exposure to matter will lead to violent annihilation!")
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("[Dangerous Drop]").withStyle(ChatFormatting.RED));
        }
    }
}
