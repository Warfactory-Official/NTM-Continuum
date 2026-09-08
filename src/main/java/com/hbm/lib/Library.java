package com.hbm.lib;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyConnectorMK2;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.api.fluidmk2.IFluidConnectorMK2;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.fluid.FluidType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.text.DecimalFormat;

/**
 * HBM utility constants/helpers. Ported incrementally — currently just the directional aliases used
 * by the energy/fluid networks. More is added as later content is ported.
 */
public final class Library {

    private Library() {}

    private static final DecimalFormat SHORT_NUMBER_FORMAT = new DecimalFormat("0.00");
    private static final String[] SHORT_SUFFIXES = {"", "k", "M", "G", "T", "P", "E", "Z", "Y", "R", "Q"};

    /**
     * Abbreviated number for GUIs, e.g. {@code 1500000 -> "1.50M"}, {@code 250 -> "250"}. Ported from
     * the 1.12.2 {@code Library.getShortNumber} (2-decimal {@code DecimalFormat}, metric suffixes).
     */
    public static String getShortNumber(long value) {
        boolean negative = value < 0;
        double d = Math.abs((double) value);
        int idx = 0;
        while (d >= 1000.0 && idx < SHORT_SUFFIXES.length - 1) {
            d /= 1000.0;
            idx++;
        }
        String result = idx == 0 ? Long.toString((long) d) : SHORT_NUMBER_FORMAT.format(d);
        return (negative ? "-" : "") + result + SHORT_SUFFIXES[idx];
    }

    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.##");

    /** A red→green progress colour (RGB int, no blue), ported from 1.12.2 {@code Library.getColorProgress}. */
    public static int getColorProgress(double fraction) {
        int r = (int) (255 * Math.min(1, fraction * -2 + 2));
        int g = (int) (255 * Math.min(1, fraction * 2));
        return 65536 * r + 256 * g;
    }

    /** A 2-decimal percentage string for a 0..1 fraction (e.g. {@code 0.5 -> "50"}). */
    public static String getPercentage(double fraction) {
        return PERCENT_FORMAT.format(fraction * 100D);
    }

    public static final ForgeDirection POS_X = ForgeDirection.EAST;
    public static final ForgeDirection NEG_X = ForgeDirection.WEST;
    public static final ForgeDirection POS_Y = ForgeDirection.UP;
    public static final ForgeDirection NEG_Y = ForgeDirection.DOWN;
    public static final ForgeDirection POS_Z = ForgeDirection.SOUTH;
    public static final ForgeDirection NEG_Z = ForgeDirection.NORTH;

    /**
     * Whether a fluid duct of the given {@code type} should connect to the neighbour at {@code pos},
     * approached along {@code dir} (the direction outwards from the duct). Mirrors the 1.12.2
     * {@code Library.canConnectFluid}: a block-level {@link IFluidConnectorBlockMK2} (so multiblock
     * machine footprints connect with no BlockEntity per cell) OR a tile-level {@link IFluidConnectorMK2}.
     * {@code dir.getOpposite()} is the neighbour's side facing the duct.
     */
    public static boolean canConnectFluid(BlockGetter world, BlockPos pos, ForgeDirection dir, FluidType type) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        if (world.getBlockState(pos).getBlock() instanceof IFluidConnectorBlockMK2 con) {
            if (con.canConnect(type, world, x, y, z, dir.getOpposite())) return true;
        }

        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof IFluidConnectorMK2 con) {
            return con.canConnect(type, dir.getOpposite());
        }

        return false;
    }

    /**
     * The energy twin of {@link #canConnectFluid}: whether the block at {@code pos} accepts a power
     * connection on the face turned towards the asker, where {@code dir} points outwards from the
     * asker. A block-level {@link IEnergyConnectorBlock} answers first, so a multiblock footprint
     * connects without a BlockEntity in every cell.
     */
    public static boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        if (dir == ForgeDirection.UNKNOWN) return false;

        if (world.getBlockState(pos).getBlock() instanceof IEnergyConnectorBlock con) {
            if (con.canConnect(world, pos, dir.getOpposite())) return true;
        }

        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof IEnergyConnectorMK2 con) {
            return con.canConnect(dir.getOpposite());
        }

        return false;
    }

    // --- battery items (the machine power-slot energy source) ------------------------------------
    // Ported from the 1.12.2 Library, IBatteryItem-only (the Forge-Energy / FE branch is deferred to a
    // per-loader capability bridge, see IMPLEMENTATION.md §13e). These drive the "battery in slot 0"
    // power input every HBM machine uses.

    /** Whether {@code stack} is an HBM battery item (can store HE). */
    public static boolean isBattery(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IBatteryItem;
    }

    /** Whether {@code stack} is a machine upgrade module (1.12.2 {@code Library.isMachineUpgrade}). */
    public static boolean isMachineUpgrade(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof com.hbm.items.machine.ItemMachineUpgrade;
    }

    /** Whether {@code stack} is a battery with charge left and a non-zero discharge rate. */
    public static boolean isDischargeableBattery(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IBatteryItem battery)) return false;
        return battery.getCharge(stack) > 0 && battery.getDischargeRate(stack) > 0;
    }

    /** Whether {@code stack} is a battery item that is fully drained. */
    public static boolean isEmptyBattery(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IBatteryItem battery)) return false;
        return battery.getCharge(stack) <= 0;
    }

    /**
     * Pulls energy from a battery item into a machine. Removes (rate-limited) charge from {@code stack}
     * up to {@code chargeAmountHE} and returns the amount actually extracted. {@code instant} ignores the
     * battery's discharge rate (for one-shot transfers).
     */
    public static long dischargeBatteryIfValid(ItemStack stack, long chargeAmountHE, boolean instant) {
        if (stack.isEmpty() || chargeAmountHE <= 0) return 0;
        if (!(stack.getItem() instanceof IBatteryItem battery)) return 0;
        long cur = Math.max(0L, battery.getCharge(stack));
        long rate = Math.max(0L, battery.getDischargeRate(stack));
        long req = instant ? chargeAmountHE : Math.min(chargeAmountHE, rate);
        long take = Math.min(req, cur);
        if (take > 0) battery.dischargeBattery(stack, take);
        return take;
    }

    /**
     * Pushes energy from a machine into a battery item. Adds (rate-limited) charge to {@code stack} up to
     * {@code chargeAmountHE} and returns the amount actually inserted.
     */
    public static long chargeBatteryIfValid(ItemStack stack, long chargeAmountHE, boolean instant) {
        if (stack.isEmpty() || chargeAmountHE <= 0) return 0;
        if (!(stack.getItem() instanceof IBatteryItem battery)) return 0;
        long max = Math.max(0L, battery.getMaxCharge(stack));
        long cur = Math.max(0L, Math.min(max, battery.getCharge(stack)));
        long room = Math.max(0L, max - cur);
        long rate = Math.max(0L, battery.getChargeRate(stack));
        long req = instant ? chargeAmountHE : Math.min(chargeAmountHE, rate);
        long added = Math.min(req, room);
        if (added > 0) battery.chargeBattery(stack, added);
        return added;
    }

    /**
     * Tops a machine's internal {@code power} up from a battery item in slot {@code index}, capped at
     * {@code maxPower}. Returns the new power level. Faithful port of the 1.12.2
     * {@code Library.chargeTEFromItems} (battery-item path).
     */
    public static long chargeTEFromItems(NTMItemHandler inventory, int index, long power, long maxPower) {
        ItemStack stack = inventory.getStackInSlot(index);
        long powerNeeded = maxPower - power;
        if (powerNeeded <= 0) return power;
        return power + dischargeBatteryIfValid(stack, powerNeeded, false);
    }

    /**
     * Drains a machine's internal {@code power} into a battery item in slot {@code index}, capped at
     * {@code maxPower} (the parameter mirrors the old signature — it doesn't bound the transfer here).
     * Returns the machine's new power level. Faithful port of the 1.12.2 {@code Library.chargeItemsFromTE}
     * (the inverse of {@link #chargeTEFromItems}, used by generators like the diesel gen).
     */
    public static long chargeItemsFromTE(NTMItemHandler inventory, int index, long power, long maxPower) {
        if (power <= 0) return power;
        ItemStack stack = inventory.getStackInSlot(index);
        long moved = chargeBatteryIfValid(stack, power, false);
        return power - moved;
    }

    /** FNV-1a 64-bit hash over a byte array; used to dedupe identical per-tick sync packets. */
    public static long fnv1a64(byte[] data) {
        long hash = 0xcbf29ce484222325L;
        for (byte b : data) {
            hash ^= (b & 0xFFL);
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
