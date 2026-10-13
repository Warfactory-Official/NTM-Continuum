package com.hbm.inventory;

import com.hbm.api.data.FluidDataMaps;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModEntityItems;
import com.hbm.items.ModFluidItems;
import com.hbm.items.ModFluidNetItems;
import com.hbm.items.ModSingularityItems;
import com.hbm.items.machine.ItemFluidTank;
import com.hbm.items.special.ItemCell;
import com.hbm.lib.ItemIds;
import dev.golem.core.Ids;
import dev.golem.core.world.FilledContainers;
import dev.golem.core.world.Ingredients;
import java.util.TreeSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 1.7 {@code FluidContainerRegistry}, as far as a crafting recipe sees it: the {@code container<amount><fluid>}
 * ore-dictionary entries that {@code FluidType.getDict(amount)} names, in the port's items. A full container
 * matches only at exactly the registered amount (1.7 containers were full by construction; a port tank with
 * 500 of 1,000 mB is not a 1,000 mB container).
 *
 * <p>What a crafting grid keeps is 1.7's {@code Item.getContainerItem}, <i>not</i> the registry's empty
 * container, because vanilla crafting only ever asked the item: the water bottle, ore blocks, barrels,
 * mercury droplet, experience bottle and mug are consumed whole, while buckets, tanks, canisters, cells,
 * particle capsules, the tritium ZIRNOX rod and the dispersers leave their empty container.
 *
 * <p>Left out, with the reason: 1.7's {@code Compat.registerCompatFluidContainers} (other mods), the IV bags
 * (no port item holds blood/XP juice by that name), the port-only {@code fluid_bucket} and the 32,000 mB
 * {@code fluid_pack_full}, which 1.7 never registered.
 */
public final class FluidContainerRegistry implements FilledContainers.Dictionary {

    public static final FluidContainerRegistry INSTANCE = new FluidContainerRegistry();

    /** One fixed item that always holds {@code amount} of {@code fluid}; {@code container} is what crafting leaves. */
    private record Fixed(String full, String container, String fluid, int amount) {}

    /**
     * A component-backed container item full at {@code capacity}; {@code container} is what crafting leaves.
     * {@code dispersableOnly}: 1.7 registered the disperser canister and glyphid gland only for
     * {@code isDispersable()} fluids, and did so before its lead-container cut-off.
     */
    private record Typed(Supplier<? extends Item> item, Supplier<ItemStack> container, boolean leadOnlyAllowed,
                         boolean dispersableOnly) {
        Typed(Supplier<? extends Item> item, Supplier<ItemStack> container, boolean leadOnlyAllowed) {
            this(item, container, leadOnlyAllowed, false);
        }

        boolean admits(FluidType type) {
            if (FluidContainers.hasNoContainer(type)) return false;
            if (dispersableOnly) return type.isDispersable();
            return leadOnlyAllowed || !FluidContainers.needsLeadContainer(type);
        }
    }

    /**
     * The fixed containers: the {@code hbm:fluid_container} item data map ({@link com.hbm.api.data.FluidDataMaps#FLUID_CONTAINER},
     * 1.7's {@code FluidContainerRegistry.registerContainer} calls), read per use. An addon's bucket or drink is one entry.
     */
    private static List<Fixed> fixed() {
        List<Fixed> rows = new ArrayList<>();
        var map = FluidDataMaps.FLUID_CONTAINER.byId();
        for (String full : new TreeSet<>(map.keySet())) {
            FluidDataMaps.Container c = map.get(full);
            FluidType type = Fluids.byRegistryName(c.fluid());
            if (type == Fluids.NONE) continue;
            rows.add(new Fixed(full, c.empty().orElse(""), type.getName(), c.amount()));
        }
        return rows;
    }

    private static List<Typed> typed() {
        return List.of(
                new Typed(ModFluidNetItems.canister_full, () -> item(ItemIds.canister_empty), false),
                new Typed(ModFluidNetItems.gas_full, () -> item(ItemIds.gas_empty), false),
                new Typed(ModFluidNetItems.fluid_tank_lead_full, () -> item(ItemIds.fluid_tank_lead_empty), true),
                // the universal tank and barrel are one item full or empty: an empty one is the bare item
                new Typed(ModFluidItems.fluid_tank, () -> new ItemStack(ModFluidItems.fluid_tank.get()), false),
                new Typed(ModFluidItems.fluid_barrel, () -> new ItemStack(ModFluidItems.fluid_barrel.get()), false),
                new Typed(ModEntityItems.disperser_canister, () -> item(ItemIds.disperser_canister_empty), true, true),
                new Typed(ModEntityItems.glyphid_gland, () -> item(ItemIds.glyphid_gland_empty), true, true));
    }

    private FluidContainerRegistry() {}

    /** Hands the dictionary to Core's recipe ingredients. Called once from common bootstrap. */
    public static void register() {
        FilledContainers.register(INSTANCE);
    }

    @Override
    public boolean holds(ItemStack stack, String fluid, int amount) {
        return isFull(stack, fluid, amount);
    }

    private boolean isFull(ItemStack stack, String fluid, int amount) {
        FluidType type = Fluids.fromName(fluid);
        if (type == null || type == Fluids.NONE) return false;
        for (Fixed fixed : fixed()) {
            if (fixed.fluid.equals(fluid) && fixed.amount == amount && stack.is(item(fixed.full).getItem())
                    && !item(fixed.full).isEmpty()) return true;
        }
        // 1.7 registered new ItemStack(Items.potionitem) - meta 0, the water bottle - as 250 mB of water
        if (fluid.equals("WATER") && amount == 250 && stack.is(Items.POTION)
                && "minecraft:water".equals(Ingredients.potionId(stack))) return true;
        if (stack.getItem() instanceof ItemCell) {
            return amount == ItemCell.CAPACITY && ItemCell.isFullCell(stack, type);
        }
        for (Typed typed : typed()) {
            if (stack.getItem() != typed.item.get() || !(stack.getItem() instanceof ItemFluidTank tank)) continue;
            if (typed.dispersableOnly && !type.isDispersable()) return false;
            if (!typed.leadOnlyAllowed && FluidContainers.needsLeadContainer(type)) return false;
            return tank.capacity == amount && ItemFluidTank.getType(stack) == type && tank.getFill(stack) == amount;
        }
        return false;
    }

    @Override
    public ItemStack emptied(ItemStack stack) {
        for (Fixed fixed : fixed()) {
            ItemStack full = item(fixed.full);
            if (!full.isEmpty() && stack.is(full.getItem())) return item(fixed.container);
        }
        if (stack.is(Items.POTION)) return ItemStack.EMPTY; // ItemPotion had no container item
        if (stack.getItem() instanceof ItemCell) return new ItemStack(ModSingularityItems.cell.get());
        for (Typed typed : typed()) {
            if (stack.getItem() == typed.item.get()) return typed.container.get();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public List<ItemStack> examples(String fluid, int amount) {
        List<ItemStack> examples = new ArrayList<>();
        FluidType type = Fluids.fromName(fluid);
        if (type == null || type == Fluids.NONE) return examples;
        for (Fixed fixed : fixed()) {
            if (fixed.fluid.equals(fluid) && fixed.amount == amount && !item(fixed.full).isEmpty()) {
                examples.add(item(fixed.full));
            }
        }
        if (fluid.equals("WATER") && amount == 250) examples.add(Ingredients.potionStack("minecraft:potion", "minecraft:water"));
        if (amount == ItemCell.CAPACITY) {
            ItemStack cell = ItemCell.getFullCell(type);
            if (!cell.isEmpty()) examples.add(cell);
        }
        for (Typed typed : typed()) {
            if (!(typed.item.get() instanceof ItemFluidTank tank) || tank.capacity != amount) continue;
            if (!typed.admits(type)) continue;
            if (!tank.acceptsFluid(type, new ItemStack(tank))) continue;
            ItemStack full = tank.make(type, amount);
            if (holds(full, fluid, amount)) examples.add(full);
        }
        return examples;
    }

    // The same table as another mod's fluid handler sees it

    /** A fixed container: {@code full} holds {@code amount} of {@code fluid}; draining all of it leaves {@code empty}. */
    public record ForeignRow(Item full, ItemStack empty, FluidType fluid, int amount) {}

    /** An empty item that a fill turns into a full member of a component-backed container family, at its capacity. */
    public record ForeignFill(Item empty, ItemFluidTank family, Predicate<FluidType> admits) {}

    /**
     * The fixed rows whose full item is not vanilla's: vanilla's own items (buckets, bottles, potions) keep the handlers their
     * loader gives them. The empty item is whatever 1.7 left ({@link ItemStack#EMPTY} when the container is used up).
     */
    public List<ForeignRow> foreignRows() {
        List<ForeignRow> rows = new ArrayList<>();
        for (Fixed fixed : fixed()) {
            // vanilla's items only: an addon's container rows (V-040) get the foreign handler like HBM's
            if (fixed.full.startsWith("minecraft:")) continue;
            ItemStack full = item(fixed.full);
            if (full.isEmpty()) continue;
            rows.add(new ForeignRow(full.getItem(), item(fixed.container), Fluids.fromName(fixed.fluid), fixed.amount));
        }
        return rows;
    }

    /** The empty canister, gas tank, lead tank, disperser canister and gland, each filling into its family. */
    public List<ForeignFill> foreignFills() {
        List<ForeignFill> fills = new ArrayList<>();
        for (Typed typed : typed()) {
            ItemStack container = typed.container.get();
            if (container.isEmpty() || !(typed.item.get() instanceof ItemFluidTank family) || container.getItem() == family) continue;
            fills.add(new ForeignFill(container.getItem(), family, typed::admits));
        }
        return fills;
    }

    private static ItemStack item(String id) {
        if (id.isEmpty()) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.getValue(Ids.parse(id));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
