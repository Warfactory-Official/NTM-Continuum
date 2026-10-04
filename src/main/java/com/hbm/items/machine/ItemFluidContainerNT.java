package com.hbm.items.machine;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import dev.golem.core.data.ModelData;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import com.hbm.inventory.FluidContainers;

/**
 * The 1.7 metadata fluid containers ({@code canister_full}, {@code gas_full}, {@code fluid_pack_full},
 * {@code fluid_tank_lead_full}) in the port's component-backed form: one item, the fluid on the
 * {@code FLUID} component, {@link #capacity} mB (1,000 for all but the 32,000 mB pack). Each
 * container names which fluids it may hold — 1.7 registered a canister/gas-tank container per fluid and
 * marked the lead-only and container-less ones with traits ({@link FluidContainers}) — and how its
 * overlay is tinted (the canister's own colour rather than the fluid's, for the canister and gas tank).
 *
 * <p>A container is only machine-fillable when 1.7's {@code FluidContainerRegistry} knew it: the
 * canister, gas tank and lead tank were registered there, the {@code fluid_pack_full} was not — it is
 * packed and unpacked by the assembler alone ({@code ass.package<fluid>} / {@code ass.unpackage<fluid>}),
 * so the {@link com.hbm.api.fluidmk2.IFillableItem} side of a non-fillable container answers no to
 * everything.
 */
public class ItemFluidContainerNT extends ItemFluidTank {

    private final Predicate<FluidType> allowed;
    private final ToIntFunction<FluidType> tint;
    private final boolean fillable;

    public ItemFluidContainerNT(Properties properties, String base, Predicate<FluidType> allowed, ToIntFunction<FluidType> tint) {
        this(properties, base, 1000, true, allowed, tint);
    }

    public ItemFluidContainerNT(Properties properties, String base, int capacity, boolean fillable,
                                Predicate<FluidType> allowed, ToIntFunction<FluidType> tint) {
        super(properties, capacity, base);
        this.allowed = allowed;
        this.tint = tint;
        this.fillable = fillable;
    }

    public boolean allows(FluidType type) {
        return type != null && type != Fluids.NONE && allowed.test(type);
    }

    /** Can a machine's loader/unloader slot fill or drain this container? */
    public boolean isFillable() {
        return fillable;
    }

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        return fillable && allows(type) && super.acceptsFluid(type, stack);
    }

    @Override
    public boolean providesFluid(FluidType type, ItemStack stack) {
        return fillable && super.providesFluid(type, stack);
    }

    @Override
    public void setFluid(ItemStack stack, FluidType type, int amount) {
        super.setFluid(stack, type, amount);
        if (type != null && type != Fluids.NONE && amount > 0) ModelData.setColor(stack, tint.applyAsInt(type));
    }
}
