package com.hbm.items.machine;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IInfiniteFluidItem;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * The 1.7 {@code ItemInfiniteFluid}: {@code inf_water} (50 mB/t of water), {@code inf_water_mk2}
 * (500 mB/t), {@code chlorine_pinwheel} (1 mB of chlorine one tick in two) and
 * {@code fluid_barrel_infinite} (a billion mB of anything, and the only one that also drains a
 * pressurised tank). See {@link IInfiniteFluidItem} for how a tank reads it.
 *
 * <p>Also an {@link IFillableItem} that provides its fluid without ever emptying, so the machine slot
 * validators that only know fillables (the fluid tank's load slot) accept it. The infinite barrel names
 * no fluid, so those validators still turn it away; it works from the barrel and the container-IO path.
 */
public class ItemInfiniteFluid extends CompatItem implements IInfiniteFluidItem, IFillableItem {

    /**
     * hbm-space's {@code noAtmo}/{@code fluid} tags, saved by name: the water table found where a
     * {@linkplain #requiresTable table-bound} item was last carried, present only while it does not match the item's
     * fluid ({@link Fluids#NONE}: none at all, e.g. in orbit). Never set on plain HBM.
     */
    public static final dev.golem.core.data.GolemComponent<FluidType> WRONG_WATER_TABLE =
            com.hbm.items.ModDataComponents.WRONG_WATER_TABLE;

    private final @Nullable Supplier<FluidType> type;
    private final int amount;
    private final int chance;
    private final boolean anyPressure;
    /** hbm-space {@code requiresTable}: works only where the water table is its fluid (inf_water, inf_water_mk2). */
    private final boolean requiresTable;

    public ItemInfiniteFluid(Properties properties, @Nullable Supplier<FluidType> type, int amount, int chance, boolean anyPressure) {
        this(properties, type, amount, chance, anyPressure, false);
    }

    public ItemInfiniteFluid(Properties properties, @Nullable Supplier<FluidType> type, int amount, int chance,
                             boolean anyPressure, boolean requiresTable) {
        super(properties);
        this.type = type;
        this.amount = amount;
        this.chance = chance;
        this.anyPressure = anyPressure;
        this.requiresTable = requiresTable;
    }

    /**
     * hbm-space {@code canOperate(stack)}: false once the item was carried where the water table is not its fluid
     * (com.hbm.api.environment.Environment.GROUNDWATER, orbit included). Like hbm-space, the check is made while the
     * item sits in an entity's inventory and remembered on the stack.
     */
    public static boolean canOperate(ItemStack stack) {
        return !WRONG_WATER_TABLE.has(stack);
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level,
                                       net.minecraft.world.entity.Entity owner,
                                       net.minecraft.world.entity.EquipmentSlot slot, boolean held) {
        checkWaterTable(stack, level, owner.blockPosition());
    }

    /** hbm-space {@code onUpdate}: remembers whether the water table at {@code pos} feeds this item. */
    public void checkWaterTable(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        if (!requiresTable) return;
        FluidType table = com.hbm.api.environment.Environment.declaredGroundwater(level, pos);
        if (table == null || table == infiniteType()) {
            if (WRONG_WATER_TABLE.has(stack)) WRONG_WATER_TABLE.remove(stack);
        } else if (WRONG_WATER_TABLE.getOr(stack, null) != table) {
            WRONG_WATER_TABLE.set(stack, table);
        }
    }

    @Override public @Nullable FluidType infiniteType() { return type == null ? null : type.get(); }
    @Override public int infiniteAmount() { return amount; }
    @Override public int infiniteChance() { return chance; }
    @Override public boolean allowPressure(int pressure) { return anyPressure || pressure == 0; }

    // --- IFillableItem: a bottomless source that never fills ---

    @Override public boolean acceptsFluid(FluidType type, ItemStack stack) { return false; }
    @Override public int tryFill(FluidType type, int amount, ItemStack stack) { return amount; }

    @Override
    public boolean providesFluid(FluidType type, ItemStack stack) {
        FluidType mine = infiniteType();
        return type != null && type != Fluids.NONE && (mine == null || mine == type) && canOperate(stack);
    }

    @Override
    public int tryEmpty(FluidType type, int amount, ItemStack stack) {
        return providesFluid(type, stack) ? Math.min(amount, this.amount) : 0;
    }

    @Override
    public int getFill(ItemStack stack) {
        return amount;
    }

    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        FluidType mine = infiniteType();
        return mine == null ? Fluids.NONE : mine;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FluidType mine = infiniteType();
        String rate = chance <= 1 ? amount + " mB/t" : amount + " mB every " + chance + " ticks";
        tooltip.add(Component.literal((mine == null ? "Any fluid" : mine.getDisplayNameString()) + ", " + rate).withStyle(ChatFormatting.GRAY));
        // hbm-space's lines for an item that found the wrong water table
        FluidType table = WRONG_WATER_TABLE.getOr(stack, null);
        if (table != null && mine != null) {
            tooltip.add(Component.literal(table == Fluids.NONE ? "Current environment has insufficient vapor pressure"
                    : "Current environment has an incompatible fluid table"));
            tooltip.add(Component.literal("Requires: " + mine.getDisplayNameString() + " table"));
            tooltip.add(Component.literal(table == Fluids.NONE
                    ? "Releasing 0.2atm of vapor into the atmosphere will add a table on dry celestial bodies"
                    : "Current environment: " + table.getDisplayNameString() + " table"));
        }
    }
}
