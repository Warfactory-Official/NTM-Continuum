package com.hbm.items;

import com.hbm.Constants;
import com.mojang.serialization.Codec;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.data.ModelData;
import net.minecraft.world.item.ItemStack;

/** HBM's typed mutable item state, backed by Golem Core on every target. */
public final class ModDataComponents {

    private ModDataComponents() {}

    public static final GolemComponent<Integer> FLUID_ID = GolemComponent.integer(Constants.MOD_ID, "fluid_id");
    public static final GolemComponent<Integer> FLUID_AMOUNT = GolemComponent.integer(Constants.MOD_ID, "fluid_amount");
    public static final GolemComponent<String> MATERIAL = GolemComponent.string(Constants.MOD_ID, "material");
    public static final GolemComponent<Integer> MAT_AMOUNT = GolemComponent.integer(Constants.MOD_ID, "mat_amount");
    public static final GolemComponent<Long> BATTERY_CHARGE = GolemComponent.longValue(Constants.MOD_ID, "battery_charge");
    public static final GolemComponent<Float> HAZARD_RADIATION = GolemComponent.floatValue(Constants.MOD_ID, "hazard_radiation");
    public static final GolemComponent<Integer> ZIRNOX_LIFE = GolemComponent.integer(Constants.MOD_ID, "zirnox_life");
    /** A Watz fuel pellet's remaining yield (the old NBT double {@code "yield"}). */
    public static final GolemComponent<Double> WATZ_YIELD = GolemComponent.of(Constants.MOD_ID, "watz_yield", Codec.DOUBLE);
    public static final GolemComponent<Boolean> FUEL_WASTE_HOT = GolemComponent.of(Constants.MOD_ID, "fuel_waste_hot", Codec.BOOL);
    public static final GolemComponent<ItemStack> GAS_MASK_FILTER = GolemComponent.itemStack(Constants.MOD_ID, "gas_mask_filter");

    /**
     * A powered armour piece's stored HE (the old NBT {@code "charge"} long).
     *
     * <p>Kept separate from {@link #BATTERY_CHARGE} deliberately: absence means "full" here (a suit
     * spawns charged) and "empty" there, so one component cannot serve both without a flag.
     */
    public static final GolemComponent<Long> ARMOR_CHARGE = GolemComponent.longValue(Constants.MOD_ID, "armor_charge");

    /**
     * A gun's state-machine scratch space — timer, state, animation timer, button flags. Rewritten
     * constantly while a gun is held, and worth nothing once it is dropped.
     *
     * <p>Kept as a raw tag rather than a typed record because the keys are dynamic: every field is
     * suffixed with a config index, so an akimbo gun carries two of each. That is upstream's own layout
     * ({@code timer_0}, {@code state_1}, …) and keeping it means the port of each accessor stayed a
     * one-liner. See {@code ItemGunBaseNT} for the write-only-on-change guard that makes per-tick
     * updates affordable.
     */
    public static final GolemComponent<net.minecraft.nbt.CompoundTag> GUN_STATE =
            GolemComponent.compound(Constants.MOD_ID, "gun_state");

    /** A gun's durable state: wear, fire mode, and its magazines' contents. */
    public static final GolemComponent<net.minecraft.nbt.CompoundTag> GUN_DATA =
            GolemComponent.compound(Constants.MOD_ID, "gun_data");

    public static int fluidId(ItemStack stack) { return FLUID_ID.getOr(stack, 0); }
    public static void fluidId(ItemStack stack, int value) { FLUID_ID.set(stack, value); }
    public static void removeFluidId(ItemStack stack) { FLUID_ID.remove(stack); }
    public static int fluidAmount(ItemStack stack) { return FLUID_AMOUNT.getOr(stack, 0); }
    public static void fluidAmount(ItemStack stack, int value) { FLUID_AMOUNT.set(stack, value); }
    public static void removeFluidAmount(ItemStack stack) { FLUID_AMOUNT.remove(stack); }
    public static String material(ItemStack stack) { return MATERIAL.get(stack).orElse(null); }
    public static void material(ItemStack stack, String value) { MATERIAL.set(stack, value); }
    public static int materialAmount(ItemStack stack, int defaultValue) { return MAT_AMOUNT.getOr(stack, defaultValue); }
    public static void setMaterialAmount(ItemStack stack, int value) { MAT_AMOUNT.set(stack, value); }
    public static long batteryCharge(ItemStack stack) { return BATTERY_CHARGE.getOr(stack, 0L); }
    public static void batteryCharge(ItemStack stack, long value) { BATTERY_CHARGE.set(stack, value); }
    public static Float hazardRadiation(ItemStack stack) { return HAZARD_RADIATION.get(stack).orElse(null); }
    public static int zirnoxLife(ItemStack stack) { return ZIRNOX_LIFE.getOr(stack, 0); }
    public static void zirnoxLife(ItemStack stack, int value) { ZIRNOX_LIFE.set(stack, value); }
    public static boolean wasteHot(ItemStack stack) { return FUEL_WASTE_HOT.getOr(stack, false); }
    public static boolean hasWasteHot(ItemStack stack) { return FUEL_WASTE_HOT.has(stack); }
    public static void wasteHot(ItemStack stack, boolean value) { FUEL_WASTE_HOT.set(stack, value); }
    public static void removeWasteHot(ItemStack stack) { FUEL_WASTE_HOT.remove(stack); }
    public static ItemStack gasMaskFilter(ItemStack stack) { return GAS_MASK_FILTER.getOr(stack, ItemStack.EMPTY).copy(); }
    public static void gasMaskFilter(ItemStack stack, ItemStack filter) { GAS_MASK_FILTER.set(stack, dev.golem.core.world.ItemStacks.copyWithCount(filter, 1)); }
    public static void removeGasMaskFilter(ItemStack stack) { GAS_MASK_FILTER.remove(stack); }
    public static void removeCustomModelData(ItemStack stack) { ModelData.removeColor(stack); }

    public static void init() {}
}
