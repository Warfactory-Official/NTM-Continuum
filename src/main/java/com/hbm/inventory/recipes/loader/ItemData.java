package com.hbm.inventory.recipes.loader;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.machine.ItemFluidTank;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.data.ModelData;
import dev.golem.core.world.ItemStacks;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * The item data a machine-recipe row can name ({@code "item_data"}), in one form on every Minecraft version: HBM's own
 * values by meaning, never raw components or NBT (which differ between 1.20.1 and 1.20.5+). These are the values 1.7
 * spelt as metadata or NBT in its recipe tables: a fluid container's or fluid icon's fluid, fill and pressure
 * ({@code fluid_pack_full}, {@code canister_full}, {@code gas_full}, {@code fluid_icon}), a broken item's original
 * ({@code ItemBroken}) and a blueprint's pool.
 *
 * <pre>
 * {"id": "hbm:fluid_pack_full", "item_data": {"fluid": "hbm:oxygen", "fluid_amount": 32000}}
 * </pre>
 *
 * A row is read without building a stack (26.1 decodes recipes before item components exist); {@link #apply} builds
 * the data onto a stack when the table rebuilds. A stack whose data this form cannot state is refused when a table is
 * written out ({@link #of}), never silently simplified.
 */
public record ItemData(Optional<FluidType> fluid, Optional<Integer> fluidAmount, Optional<Integer> fluidPressure,
                       Optional<String> brokenItem, Optional<String> blueprintPool) {

    public static final ItemData NONE = new ItemData(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty());

    /** {@link GenericRecipeCodec#FLUID}, restated: the two classes initialise each other's codecs otherwise. */
    private static final Codec<FluidType> FLUID = BuiltInRegistries.FLUID.byNameCodec().comapFlatMap(
            fluid -> {
                FluidType type = com.hbm.inventory.fluid.Fluids.fromVanilla(fluid);
                return type == com.hbm.inventory.fluid.Fluids.NONE && fluid != net.minecraft.world.level.material.Fluids.EMPTY
                        ? DataResult.error(() -> "not an HBM fluid: " + fluid)
                        : DataResult.success(type);
            },
            com.hbm.inventory.fluid.Fluids::toVanilla);

    public static final Codec<ItemData> CODEC = RecordCodecBuilder.create(i -> i.group(
            FLUID.optionalFieldOf("fluid").forGetter(ItemData::fluid),
            Codec.INT.optionalFieldOf("fluid_amount").forGetter(ItemData::fluidAmount),
            Codec.INT.optionalFieldOf("fluid_pressure").forGetter(ItemData::fluidPressure),
            Codec.STRING.optionalFieldOf("broken_item").forGetter(ItemData::brokenItem),
            Codec.STRING.optionalFieldOf("blueprint_pool").forGetter(ItemData::blueprintPool)
    ).apply(i, ItemData::new));

    public boolean isEmpty() {
        return equals(NONE);
    }

    /** Writes this data onto {@code stack} (as the items' own factories do) and returns it. */
    public ItemStack apply(ItemStack stack) {
        if (stack.getItem() instanceof ItemFluidTank tank && fluid.isPresent() && fluidAmount.isPresent()) {
            // a container fills as its own make() does (fluid, fill and the container's tint of it)
            tank.setFluid(stack, fluid.get(), fluidAmount.get());
        } else {
            fluid.ifPresent(type -> {
                ModDataComponents.FLUID.set(stack, type);
                // ItemFluidIcon.make tints the droplet with the fluid's colour
                if (stack.getItem() instanceof ItemFluidIcon) ModelData.setColor(stack, type.getColor());
            });
            fluidAmount.ifPresent(amount -> ModDataComponents.FLUID_AMOUNT.set(stack, amount));
        }
        fluidPressure.ifPresent(pressure -> ModDataComponents.FLUID_PRESSURE.set(stack, pressure));
        brokenItem.ifPresent(id -> ModDataComponents.BROKEN_ITEM.set(stack, id));
        blueprintPool.ifPresent(pool -> ModDataComponents.BLUEPRINT_POOL.set(stack, pool));
        return stack;
    }

    /**
     * The data of {@code stack} in this form, or an error naming the stack when it carries anything else (the
     * rebuilt stack must equal the original, components and all).
     */
    public static DataResult<ItemData> of(ItemStack stack) {
        ItemStack fresh = new ItemStack(stack.getItem(), stack.getCount());
        ItemData data = new ItemData(
                differing(ModDataComponents.FLUID.get(stack), ModDataComponents.FLUID.get(fresh)),
                differing(ModDataComponents.FLUID_AMOUNT.get(stack), ModDataComponents.FLUID_AMOUNT.get(fresh)),
                differing(ModDataComponents.FLUID_PRESSURE.get(stack), ModDataComponents.FLUID_PRESSURE.get(fresh)),
                differing(ModDataComponents.BROKEN_ITEM.get(stack), ModDataComponents.BROKEN_ITEM.get(fresh)),
                differing(ModDataComponents.BLUEPRINT_POOL.get(stack), ModDataComponents.BLUEPRINT_POOL.get(fresh)));
        return ItemStacks.sameItemAndData(data.apply(fresh), stack) ? DataResult.success(data)
                : DataResult.error(() -> "item data the recipe schema cannot state: " + stack);
    }

    private static <T> Optional<T> differing(Optional<T> value, Optional<T> fresh) {
        return value.equals(fresh) ? Optional.empty() : value;
    }

    /** A short form for {@link GenericRecipeCodec#describe}; empty for no data. */
    public String describe() {
        if (isEmpty()) return "";
        List<String> parts = new ArrayList<>();
        fluid.ifPresent(type -> parts.add("fluid=" + type.getKey()));
        fluidAmount.ifPresent(amount -> parts.add("amount=" + amount));
        fluidPressure.ifPresent(pressure -> parts.add("pressure=" + pressure));
        brokenItem.ifPresent(id -> parts.add("broken=" + id));
        blueprintPool.ifPresent(pool -> parts.add("pool=" + pool));
        return "{" + String.join(";", parts) + "}";
    }
}
