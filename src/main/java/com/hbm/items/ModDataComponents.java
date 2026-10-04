package com.hbm.items;

import com.hbm.Constants;
import com.hbm.items.machine.RBMKFuelData;
import com.mojang.serialization.Codec;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.data.ModelData;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;

/** HBM's typed mutable item state, backed by Golem Core on every target. */
public final class ModDataComponents {

    private ModDataComponents() {}

    /**
     * The fluid a container/identifier/icon item carries, saved as its registry name ({@code "hbm:water"}). Replaced
     * the int {@code hbm:fluid_id} on 2026-10-03: numeric ids are never persisted (History section 19ej-ids).
     */
    public static final GolemComponent<com.hbm.inventory.fluid.FluidType> FLUID =
            GolemComponent.of(Constants.MOD_ID, "fluid", () -> com.hbm.inventory.fluid.Fluids.CODEC);
    public static final GolemComponent<Integer> FLUID_AMOUNT = GolemComponent.integer(Constants.MOD_ID, "fluid_amount");
    public static final GolemComponent<Integer> FLUID_PRESSURE = GolemComponent.integer(Constants.MOD_ID, "fluid_pressure");
    /** Filled giant-tank block state carried when a giant tank is broken and picked up. */
    public static final GolemComponent<Integer> GIANT_TANK_MODE =
            GolemComponent.integer(Constants.MOD_ID, "giant_tank_mode");
    public static final GolemComponent<String> MATERIAL = GolemComponent.string(Constants.MOD_ID, "material");
    public static final GolemComponent<Integer> MAT_AMOUNT = GolemComponent.integer(Constants.MOD_ID, "mat_amount");
    public static final GolemComponent<Long> BATTERY_CHARGE = GolemComponent.longValue(Constants.MOD_ID, "battery_charge");
    public static final GolemComponent<Integer> PLATE_FUEL_LIFE = GolemComponent.integer(Constants.MOD_ID, "plate_fuel_life");
    /** Fixed-fuel amount carried by ordinary jetpacks. */
    public static final GolemComponent<Integer> JETPACK_FUEL = GolemComponent.integer(Constants.MOD_ID, "jetpack_fuel");
    public static final GolemComponent<Float> HAZARD_RADIATION = GolemComponent.floatValue(Constants.MOD_ID, "hazard_radiation");
    public static final GolemComponent<Integer> ZIRNOX_LIFE = GolemComponent.integer(Constants.MOD_ID, "zirnox_life");
    public static final GolemComponent<RBMKFuelData> RBMK_FUEL = GolemComponent.of(Constants.MOD_ID, "rbmk_fuel", RBMKFuelData.CODEC);
    /** The universal grenade's four 1.7 NBT selectors (shell, filling, fuze, extra). */
    public static final GolemComponent<com.hbm.items.weapon.grenade.GrenadeData> GRENADE =
            GolemComponent.of(Constants.MOD_ID, "grenade", com.hbm.items.weapon.grenade.GrenadeData.CODEC);
    /** 1.7 ItemModMinecart "base" - the chassis a cart item was crafted on. */
    public static final GolemComponent<com.hbm.items.tool.ItemModMinecart.EnumCartBase> CART_BASE =
            GolemComponent.of(Constants.MOD_ID, "cart_base", com.hbm.items.tool.ItemModMinecart.EnumCartBase.CODEC);
    /** 1.7 satellite chip "freq" NBT (ISatChip); ntm-next keeps it in its persistent-data compound. */
    public static final GolemComponent<net.minecraft.nbt.CompoundTag> PERSISTENT_DATA =
            GolemComponent.compound(Constants.MOD_ID, "persistent_data");
    public static final GolemComponent<Integer> ITEM_VARIANT = GolemComponent.integer(Constants.MOD_ID, "item_variant");
    /** PotatOS voice-line countdown (1.7 NBT {@code timer} on {@code battery_potatos}). */
    public static final GolemComponent<Integer> POTATOS_TIMER = GolemComponent.integer(Constants.MOD_ID, "potatos_timer");
    public static final GolemComponent<Double> PILE_ROD_DEPLETION =
            GolemComponent.of(Constants.MOD_ID, "pile_rod_depletion", Codec.DOUBLE);
    /** How far a stabiliser lens has been burned through; upstream keeps it in the stack's NBT. */
    public static final GolemComponent<Long> AMS_LENS_DAMAGE =
            GolemComponent.of(Constants.MOD_ID, "ams_lens_damage", Codec.LONG);
    /** The satellite frequency an {@link com.hbm.items.ISatChip} addresses. */
    public static final GolemComponent<Integer> SAT_FREQUENCY =
            GolemComponent.integer(Constants.MOD_ID, "sat_frequency");
    /** What a cargo artillery shell is carrying. It survives the flight and comes back off the shell. */
    // GolemComponent.itemStack, not a raw ItemStack codec: an ItemStack has no value equality, and 26.1 refused the
    // first set ("Data components must implement equals and hashCode"), so no cargo shell could be loaded (§19ej-1b)
    public static final GolemComponent<ItemStack> ARTY_CARGO =
            GolemComponent.itemStack(Constants.MOD_ID, "arty_cargo");
    /** The old metadata value on the long/short storage-drum waste items. */
    public static final GolemComponent<Integer> STORAGE_WASTE_CLASS =
            GolemComponent.integer(Constants.MOD_ID, "storage_waste_class");
    /** The old neutron-contamination float formerly stored directly on an item NBT tag. */
    public static final GolemComponent<Float> NEUTRON_ACTIVATION =
            GolemComponent.floatValue(Constants.MOD_ID, "neutron_activation");
    /** Pool name carried by the legacy dynamic blueprint item. */
    public static final GolemComponent<String> BLUEPRINT_POOL = GolemComponent.string(Constants.MOD_ID, "blueprint_pool");
    /** Registry id of the item represented by a broken precision-assembler output. */
    public static final GolemComponent<String> BROKEN_ITEM = GolemComponent.string(Constants.MOD_ID, "broken_item");
    /** Internal energy carried by a dropped FEnSU machine block. */
    public static final GolemComponent<Long> FENSU_POWER = GolemComponent.longValue(Constants.MOD_ID, "fensu_power");
    /** Dye metadata carried by a dropped FEnSU machine block. */
    public static final GolemComponent<Integer> FENSU_COLOR = GolemComponent.integer(Constants.MOD_ID, "fensu_color");
    /** Unpowered redstone mode carried by a dropped FEnSU machine block. */
    public static final GolemComponent<Integer> FENSU_RED_LOW = GolemComponent.integer(Constants.MOD_ID, "fensu_red_low");
    /** Powered redstone mode carried by a dropped FEnSU machine block. */
    public static final GolemComponent<Integer> FENSU_RED_HIGH = GolemComponent.integer(Constants.MOD_ID, "fensu_red_high");
    /** Network priority carried by a dropped FEnSU machine block. */
    public static final GolemComponent<Integer> FENSU_PRIORITY = GolemComponent.integer(Constants.MOD_ID, "fensu_priority");
    /** Arbitrary power carried by a dropped machine_battery_redd block; decimal encoding avoids a long cap. */
    public static final GolemComponent<BigInteger> BATTERY_REDD_POWER = GolemComponent.of(
            Constants.MOD_ID, "battery_redd_power", Codec.STRING.xmap(BigInteger::new, BigInteger::toString));
    /** Muffler state carried by a dropped machine_battery_redd block. */
    public static final GolemComponent<Boolean> BATTERY_REDD_MUFFLED = GolemComponent.of(
            Constants.MOD_ID, "battery_redd_muffled", Codec.BOOL);
    public static final GolemComponent<Long> RBMK_TOOL_TARGET = GolemComponent.longValue(Constants.MOD_ID, "rbmk_tool_target");
    /** The artillery gun a designator is bound to, as a packed core position. */
    public static final GolemComponent<Long> ARTY_DESIGNATOR_TARGET =
            GolemComponent.longValue(Constants.MOD_ID, "arty_designator_target");
    /** A Watz fuel pellet's remaining yield (the old NBT double {@code "yield"}). */
    public static final GolemComponent<Double> WATZ_YIELD = GolemComponent.of(Constants.MOD_ID, "watz_yield", Codec.DOUBLE);
    public static final GolemComponent<Boolean> FUEL_WASTE_HOT = GolemComponent.of(Constants.MOD_ID, "fuel_waste_hot", Codec.BOOL);
    /** Player names a turret biometry card protects; the 1.12.2 NBT string list. */
    public static final GolemComponent<java.util.List<String>> TURRET_WHITELIST =
            GolemComponent.of(Constants.MOD_ID, "turret_whitelist", Codec.STRING.listOf());
    public static final GolemComponent<ItemStack> GAS_MASK_FILTER = GolemComponent.itemStack(Constants.MOD_ID, "gas_mask_filter");

    /**
     * A designator's recorded target, X and Z (the old NBT {@code xCoord}/{@code zCoord}). Their
     * <em>presence</em> is the "has a target" flag — upstream tested {@code hasTagCompound}, so a
     * designator aimed at (0, 0) counts as aimed and one never used does not.
     */
    /**
     * The block a detonator is bound to (the old NBT {@code x}/{@code y}/{@code z} triple). Absent means
     * unbound, which the detonator reports rather than silently doing nothing.
     */
    public static final GolemComponent<net.minecraft.core.BlockPos> DETONATOR_TARGET =
            GolemComponent.of(Constants.MOD_ID, "detonator_target", net.minecraft.core.BlockPos.CODEC);

    public static final GolemComponent<Integer> DESIGNATOR_X = GolemComponent.integer(Constants.MOD_ID, "designator_x");
    public static final GolemComponent<Integer> DESIGNATOR_Z = GolemComponent.integer(Constants.MOD_ID, "designator_z");

    /**
     * A powered armour piece's stored HE (the old NBT {@code "charge"} long).
     *
     * <p>Kept separate from {@link #BATTERY_CHARGE} deliberately: absence means "full" here (a suit
     * spawns charged) and "empty" there, so one component cannot serve both without a flag.
     */
    public static final GolemComponent<Long> ARMOR_CHARGE = GolemComponent.longValue(Constants.MOD_ID, "armor_charge");

    /** State saved on the advanced glider stack (the old {@code hbmJetpackAdvanced} compound). */
    public static final GolemComponent<net.minecraft.nbt.CompoundTag> JETPACK_STATE =
            GolemComponent.compound(Constants.MOD_ID, "jetpack_state");

    /** Nine ordered armor-module slots, replacing the legacy {@code ntm_armor_mods.mod_slot_*} tag. */
    public static final GolemComponent<dev.golem.core.data.ItemContents> ARMOR_MODS =
            GolemComponent.of(Constants.MOD_ID, "armor_mods", dev.golem.core.data.ItemContents.CODEC);

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
    /** Fixed eight-slot contents carried by the reusable ammo bag. */
    public static final GolemComponent<dev.golem.core.data.ItemContents> AMMO_BAG =
            GolemComponent.of(Constants.MOD_ID, "ammo_bag", dev.golem.core.data.ItemContents.CODEC);
    /** Fixed fifteen-slot contents carried by the spent-casing bag. */
    public static final GolemComponent<dev.golem.core.data.ItemContents> CASING_BAG =
            GolemComponent.of(Constants.MOD_ID, "casing_bag", dev.golem.core.data.ItemContents.CODEC);
    /** Fractional casing progress, retained between shots until a whole casing is produced. */
    public static final GolemComponent<net.minecraft.nbt.CompoundTag> CASING_PROGRESS =
            GolemComponent.compound(Constants.MOD_ID, "casing_progress");

    /** The fluid {@code stack} carries; NONE when it carries none. */
    public static com.hbm.inventory.fluid.FluidType fluid(ItemStack stack) {
        return FLUID.getOr(stack, com.hbm.inventory.fluid.Fluids.NONE);
    }
    public static void fluid(ItemStack stack, com.hbm.inventory.fluid.FluidType value) { FLUID.set(stack, value); }
    public static void removeFluid(ItemStack stack) { FLUID.remove(stack); }
    public static int fluidAmount(ItemStack stack) { return FLUID_AMOUNT.getOr(stack, 0); }
    public static void fluidAmount(ItemStack stack, int value) { FLUID_AMOUNT.set(stack, value); }
    public static void removeFluidAmount(ItemStack stack) { FLUID_AMOUNT.remove(stack); }
    public static int fluidPressure(ItemStack stack) { return FLUID_PRESSURE.getOr(stack, 0); }
    public static void fluidPressure(ItemStack stack, int value) { FLUID_PRESSURE.set(stack, value); }
    public static void removeFluidPressure(ItemStack stack) { FLUID_PRESSURE.remove(stack); }
    public static String material(ItemStack stack) { return MATERIAL.get(stack).orElse(null); }
    public static void material(ItemStack stack, String value) { MATERIAL.set(stack, value); }
    public static int materialAmount(ItemStack stack, int defaultValue) { return MAT_AMOUNT.getOr(stack, defaultValue); }
    public static void setMaterialAmount(ItemStack stack, int value) { MAT_AMOUNT.set(stack, value); }
    public static long batteryCharge(ItemStack stack) { return BATTERY_CHARGE.getOr(stack, 0L); }
    public static void batteryCharge(ItemStack stack, long value) { BATTERY_CHARGE.set(stack, value); }
    public static int plateFuelLife(ItemStack stack) { return PLATE_FUEL_LIFE.getOr(stack, 0); }
    public static void plateFuelLife(ItemStack stack, int value) { PLATE_FUEL_LIFE.set(stack, value); }
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
    public static int storageWasteClass(ItemStack stack) { return STORAGE_WASTE_CLASS.getOr(stack, 0); }
    public static void storageWasteClass(ItemStack stack, int value) { STORAGE_WASTE_CLASS.set(stack, value); }

    public static String blueprintPool(ItemStack stack) { return BLUEPRINT_POOL.getOr(stack, null); }
    public static void blueprintPool(ItemStack stack, String value) { BLUEPRINT_POOL.set(stack, value); }
    public static String brokenItem(ItemStack stack) { return BROKEN_ITEM.getOr(stack, null); }
    public static void brokenItem(ItemStack stack, String value) { BROKEN_ITEM.set(stack, value); }
    public static long fensuPower(ItemStack stack) { return FENSU_POWER.getOr(stack, 0L); }
    public static void fensuPower(ItemStack stack, long value) { FENSU_POWER.set(stack, value); }
    public static int fensuColor(ItemStack stack, int defaultValue) { return FENSU_COLOR.getOr(stack, defaultValue); }
    public static void setFensuColor(ItemStack stack, int value) { FENSU_COLOR.set(stack, value); }
    public static int fensuRedLow(ItemStack stack, int defaultValue) { return FENSU_RED_LOW.getOr(stack, defaultValue); }
    public static void setFensuRedLow(ItemStack stack, int value) { FENSU_RED_LOW.set(stack, value); }
    public static int fensuRedHigh(ItemStack stack, int defaultValue) { return FENSU_RED_HIGH.getOr(stack, defaultValue); }
    public static void setFensuRedHigh(ItemStack stack, int value) { FENSU_RED_HIGH.set(stack, value); }
    public static int fensuPriority(ItemStack stack, int defaultValue) { return FENSU_PRIORITY.getOr(stack, defaultValue); }
    public static void setFensuPriority(ItemStack stack, int value) { FENSU_PRIORITY.set(stack, value); }
    public static BigInteger batteryReddPower(ItemStack stack) {
        return BATTERY_REDD_POWER.getOr(stack, BigInteger.ZERO);
    }
    public static void setBatteryReddPower(ItemStack stack, BigInteger value) {
        BATTERY_REDD_POWER.set(stack, value.max(BigInteger.ZERO));
    }
    public static boolean hasBatteryReddPower(ItemStack stack) { return BATTERY_REDD_POWER.has(stack); }
    public static boolean batteryReddMuffled(ItemStack stack) { return BATTERY_REDD_MUFFLED.getOr(stack, false); }
    public static void setBatteryReddMuffled(ItemStack stack, boolean value) { BATTERY_REDD_MUFFLED.set(stack, value); }

    public static void init() {}
}
