package com.hbm.inventory;

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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
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

    private static final List<Fixed> FIXED = List.of(
            new Fixed("minecraft:water_bucket", "minecraft:bucket", "WATER", 1000),
            new Fixed("minecraft:lava_bucket", "minecraft:bucket", "LAVA", 1000),
            new Fixed(ItemIds.bucket_mud, "minecraft:bucket", "WATZ", 1000),
            new Fixed(ItemIds.bucket_schrabidic_acid, "minecraft:bucket", "SCHRABIDIC", 1000),
            new Fixed(ItemIds.bucket_sulfuric_acid, "minecraft:bucket", "SULFURIC_ACID", 1000),
            // blocks: no container item, consumed whole
            new Fixed(ItemIds.red_barrel, "", "DIESEL", 10000),
            new Fixed(ItemIds.pink_barrel, "", "KEROSENE", 10000),
            new Fixed(ItemIds.lox_barrel, "", "OXYGEN", 10000),
            new Fixed(ItemIds.ore_oil, "", "OIL", 250),
            new Fixed(ItemIds.ore_gneiss_gas, "", "PETROLEUM", 250), // 50 with enable528, off by default
            new Fixed(ItemIds.bottle_mercury, "minecraft:glass_bottle", "MERCURY", 1000),
            new Fixed(ItemIds.nugget_mercury, "", "MERCURY", 125), // 1.7 ingot_mercury, registered as "nugget_mercury"
            new Fixed(ItemIds.rod_zirnox_tritium, ItemIds.rod_zirnox_empty, "TRITIUM", 2000),
            new Fixed(ItemIds.particle_hydrogen, ItemIds.particle_empty, "HYDROGEN", 1000),
            new Fixed(ItemIds.particle_amat, ItemIds.particle_empty, "AMAT", 1000),
            new Fixed(ItemIds.particle_aschrab, ItemIds.particle_empty, "ASCHRAB", 1000),
            // vanilla 1.7 gave the experience bottle no container item
            new Fixed("minecraft:experience_bottle", "", "XPJUICE", 100),
            new Fixed("hbm:iv_blood", "hbm:iv_empty", "BLOOD", 100),
            new Fixed("hbm:iv_xp", "hbm:iv_xp_empty", "XPJUICE", 100),
            new Fixed(ItemIds.can_mug, "", "MUG", 100));

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
        for (Fixed fixed : FIXED) {
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
        for (Fixed fixed : FIXED) {
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
        for (Fixed fixed : FIXED) {
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

    private static ItemStack item(String id) {
        if (id.isEmpty()) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.getValue(Ids.parse(id));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
