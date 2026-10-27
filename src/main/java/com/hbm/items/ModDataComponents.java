package com.hbm.items;

import com.hbm.Constants;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.machine.RBMKFuelData;
import com.hbm.items.tool.ItemModMinecart;
import com.hbm.items.weapon.grenade.GrenadeData;
import com.hbm.util.SavedIds;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.data.ItemContents;
import dev.golem.core.data.ModelData;
import dev.golem.core.world.ItemStacks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;

/** HBM's typed mutable item state, backed by Golem Core on every target. */
public final class ModDataComponents {

    private ModDataComponents() {}

    /**
     * The fluid a container/identifier/icon item carries, saved as its registry name ({@code "hbm:oil"},
     * {@code "minecraft:water"}), never as an int id: numeric ids are never persisted.
     */
    public static final GolemComponent<FluidType> FLUID =
            GolemComponent.of(Constants.MOD_ID, "fluid", () -> Fluids.CODEC);
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
    public static final GolemComponent<GrenadeData> GRENADE =
            GolemComponent.of(Constants.MOD_ID, "grenade", GrenadeData.CODEC);
    /** The old mirror-tool {@code posX}, {@code posY}, and {@code posZ} tag, as one typed position. */
    public static final GolemComponent<BlockPos> MIRROR_TARGET =
            GolemComponent.of(Constants.MOD_ID, "mirror_target", BlockPos.CODEC);
    /** 1.7 ItemModMinecart "base" - the chassis a cart item was crafted on. */
    public static final GolemComponent<ItemModMinecart.EnumCartBase> CART_BASE =
            GolemComponent.of(Constants.MOD_ID, "cart_base", ItemModMinecart.EnumCartBase.CODEC);
    /** 1.7 satellite chip "freq" NBT (ISatChip); ntm-next keeps it in its persistent-data compound. */
    public static final GolemComponent<CompoundTag> PERSISTENT_DATA =
            GolemComponent.compound(Constants.MOD_ID, "persistent_data");
    // Typed states for the items that keep one id where 1.7 used metadata, named as ntm-next names them.
    // None of them is an id.
    /** A stirling engine still carrying its gear (1.7 metadata 0); absent means true. ntm-next HAS_COG. */
    public static final GolemComponent<Boolean> HAS_COG = GolemComponent.of(Constants.MOD_ID, "has_cog", Codec.BOOL);
    /** A sawmill still carrying its blade (1.7 metadata 0); absent means true. ntm-next HAS_BLADE. */
    public static final GolemComponent<Boolean> HAS_BLADE = GolemComponent.of(Constants.MOD_ID, "has_blade", Codec.BOOL);
    /** hbm-space: the wrong water table a table-bound infinite water item found (ItemInfiniteFluid), saved by name. */
    public static final GolemComponent<FluidType> WRONG_WATER_TABLE =
            GolemComponent.of(Constants.MOD_ID, "wrong_water_table", () -> Fluids.CODEC);
    /** 1.7 ItemToolBox metadata 1: the box is open. ntm-next TOOLBOX_OPEN. */
    public static final GolemComponent<Boolean> TOOLBOX_OPEN = GolemComponent.of(Constants.MOD_ID, "toolbox_open", Codec.BOOL);
    /** An RBMK recycling pellet's depletion stage and xenon poisoning (1.7 metadata 0..9). ntm-next RBMK_PELLET. */
    public static final GolemComponent<ItemRBMKPellet.Stage> RBMK_PELLET = GolemComponent.of(
            Constants.MOD_ID, "rbmk_pellet", () -> ItemRBMKPellet.Stage.CODEC);
    /** How many times a dusted steel ingot has been forged (1.7 metadata 0..9, a count). ntm-next FORGE_COUNT. */
    public static final GolemComponent<Integer> FORGE_COUNT = GolemComponent.integer(Constants.MOD_ID, "forge_count");
    /**
     * The B92 cell's charge (1.7 NBT; 1.7 also set metadata 1 at the full 25, which the starblaster recipe matched).
     * ntm-next B92_ENERGY. A charge, not an id; the recipe matches the full value.
     */
    public static final GolemComponent<Integer> B92_ENERGY = GolemComponent.integer(Constants.MOD_ID, "b92_energy");
    /** The custom-machine configuration a controller item builds, by its {@code recordName} (1.7: index + 100). */
    public static final GolemComponent<String> CUSTOM_MACHINE = GolemComponent.string(Constants.MOD_ID, "custom_machine");
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
    // GolemComponent.itemStack, not a raw ItemStack codec: an ItemStack has no value equality, and 26.1 refuses
    // such a component ("Data components must implement equals and hashCode")
    public static final GolemComponent<ItemStack> ARTY_CARGO =
            GolemComponent.itemStack(Constants.MOD_ID, "arty_cargo");
    /** The old neutron-contamination float formerly stored directly on an item NBT tag. */
    public static final GolemComponent<Float> NEUTRON_ACTIVATION =
            GolemComponent.floatValue(Constants.MOD_ID, "neutron_activation");
    /** Pool name carried by the legacy dynamic blueprint item. */
    public static final GolemComponent<String> BLUEPRINT_POOL = GolemComponent.string(Constants.MOD_ID, "blueprint_pool");
    /** Registry id of the item represented by a broken precision-assembler output. */
    public static final GolemComponent<String> BROKEN_ITEM = GolemComponent.string(Constants.MOD_ID, "broken_item");
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
    /** 1.7 root NBT {@code ntmCyanide}: food laced on the anvil with a Plan C (AnvilSmithingCyanideRecipe). */
    public static final GolemComponent<Boolean> NTM_CYANIDE = GolemComponent.of(Constants.MOD_ID, "ntm_cyanide", Codec.BOOL);
    /** 1.7 root NBT {@code ntmRedPill}: food laced on the anvil with a red pill (AnvilSmithingCyanideRecipe). */
    public static final GolemComponent<Boolean> NTM_RED_PILL = GolemComponent.of(Constants.MOD_ID, "ntm_red_pill", Codec.BOOL);
    /** Player names a turret biometry card protects; the 1.12.2 NBT string list. */
    public static final GolemComponent<List<String>> TURRET_WHITELIST =
            GolemComponent.of(Constants.MOD_ID, "turret_whitelist", Codec.STRING.listOf());
    public static final GolemComponent<ItemStack> GAS_MASK_FILTER = GolemComponent.itemStack(Constants.MOD_ID, "gas_mask_filter");

    /**
     * The block a detonator is bound to (the old NBT {@code x}/{@code y}/{@code z} triple). Absent means
     * unbound, which the detonator reports rather than silently doing nothing.
     */
    public static final GolemComponent<BlockPos> DETONATOR_TARGET =
            GolemComponent.of(Constants.MOD_ID, "detonator_target", BlockPos.CODEC);

    /**
     * A designator's recorded target, X and Z (the old NBT {@code xCoord}/{@code zCoord}). Their
     * <em>presence</em> is the "has a target" flag. Upstream tested {@code hasTagCompound}, so a
     * designator aimed at (0, 0) counts as aimed and one never used does not.
     */
    public static final GolemComponent<Integer> DESIGNATOR_X = GolemComponent.integer(Constants.MOD_ID, "designator_x");
    public static final GolemComponent<Integer> DESIGNATOR_Z = GolemComponent.integer(Constants.MOD_ID, "designator_z");

    /**
     * A powered armour piece's stored HE (the old NBT {@code "charge"} long).
     *
     * <p>Kept separate from {@link #BATTERY_CHARGE}: absence means "full" here (a suit
     * spawns charged) and "empty" there, so one component cannot serve both without a flag.
     */
    public static final GolemComponent<Long> ARMOR_CHARGE = GolemComponent.longValue(Constants.MOD_ID, "armor_charge");

    /** State saved on the advanced glider stack (the old {@code hbmJetpackAdvanced} compound). */
    public static final GolemComponent<CompoundTag> JETPACK_STATE =
            GolemComponent.compound(Constants.MOD_ID, "jetpack_state");

    /** Nine ordered armor-module slots, replacing the legacy {@code ntm_armor_mods.mod_slot_*} tag. */
    public static final GolemComponent<ItemContents> ARMOR_MODS =
            GolemComponent.of(Constants.MOD_ID, "armor_mods", ModDataComponents::armorModsCodec);

    /** Also reads DE's {@code [{slot, item}]} list, so a DE stack loading from another mod's storage keeps its modules. */
    private static Codec<ItemContents> armorModsCodec() {
        Codec<ItemContents> slotted = RecordCodecBuilder.<Map.Entry<Integer, ItemStack>>create(i -> i.group(
                Codec.intRange(0, ArmorModHandler.MOD_SLOTS - 1).fieldOf("slot").forGetter(Map.Entry::getKey),
                ItemStacks.optionalCodec().fieldOf("item").forGetter(Map.Entry::getValue)
        ).apply(i, Map::entry)).listOf().xmap(entries -> {
            List<ItemStack> slots = new ArrayList<>(Collections.nCopies(ArmorModHandler.MOD_SLOTS, ItemStack.EMPTY));
            for (Map.Entry<Integer, ItemStack> e : entries) slots.set(e.getKey(), e.getValue());
            return new ItemContents(slots);
        }, contents -> List.of());
        return Codec.either(ItemContents.CODEC, slotted).xmap(e -> e.map(c -> c, c -> c), Either::left);
    }

    /**
     * A gun's state-machine scratch space: timer, state, animation timer, button flags. Rewritten
     * constantly while a gun is held, and worth nothing once it is dropped.
     *
     * <p>Kept as a raw tag rather than a typed record because the keys are dynamic: every field is
     * suffixed with a config index, so an akimbo gun carries two of each. That is upstream's own layout
     * ({@code timer_0}, {@code state_1}, …), which keeps each accessor a one-liner. See {@code ItemGunBaseNT}
     * for the write-only-on-change guard that makes per-tick updates affordable.
     */
    public static final GolemComponent<CompoundTag> GUN_STATE =
            GolemComponent.compound(Constants.MOD_ID, "gun_state");

    /** A gun's durable state: wear, fire mode, and its magazines' contents. */
    public static final GolemComponent<CompoundTag> GUN_DATA =
            GolemComponent.compound(Constants.MOD_ID, "gun_data");
    /** Fixed eight-slot contents carried by the reusable ammo bag. */
    public static final GolemComponent<ItemContents> AMMO_BAG =
            GolemComponent.of(Constants.MOD_ID, "ammo_bag", ItemContents.CODEC);
    /** Fixed fifteen-slot contents carried by the spent-casing bag. */
    public static final GolemComponent<ItemContents> CASING_BAG =
            GolemComponent.of(Constants.MOD_ID, "casing_bag", ItemContents.CODEC);
    /** The single item a plastic bag carries. */
    public static final GolemComponent<ItemContents> PLASTIC_BAG =
            GolemComponent.of(Constants.MOD_ID, "plastic_bag", ItemContents.CODEC);
    /** Fractional casing progress, retained between shots until a whole casing is produced. */
    public static final GolemComponent<CompoundTag> CASING_PROGRESS =
            GolemComponent.compound(Constants.MOD_ID, "casing_progress");

    /** The fluid {@code stack} carries; NONE when it carries none. */
    public static FluidType fluid(ItemStack stack) {
        return FLUID.getOr(stack, Fluids.NONE);
    }
    public static void fluid(ItemStack stack, FluidType value) { FLUID.set(stack, value); }
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
    public static void gasMaskFilter(ItemStack stack, ItemStack filter) { GAS_MASK_FILTER.set(stack, ItemStacks.copyWithCount(filter, 1)); }
    public static void removeGasMaskFilter(ItemStack stack) { GAS_MASK_FILTER.remove(stack); }
    public static void removeCustomModelData(ItemStack stack) { ModelData.removeColor(stack); }

    public static String blueprintPool(ItemStack stack) { return BLUEPRINT_POOL.getOr(stack, null); }
    public static void blueprintPool(ItemStack stack, String value) { BLUEPRINT_POOL.set(stack, value); }
    public static String brokenItem(ItemStack stack) { return BROKEN_ITEM.getOr(stack, null); }
    public static void brokenItem(ItemStack stack, String value) { BROKEN_ITEM.set(stack, value); }
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
