package com.hbm.lib;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.api.fluidmk2.IFluidConnectorMK2;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.fluid.FluidType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.text.DecimalFormat;

public final class Library {

    private Library() {}

    private static final DecimalFormat SHORT_NUMBER_FORMAT = new DecimalFormat("0.00");
    private static final String[] SHORT_SUFFIXES = {"", "k", "M", "G", "T", "P", "E", "Z", "Y", "R", "Q"};

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

    public static int getColorProgress(double fraction) {
        int r = (int) (255 * Math.min(1, fraction * -2 + 2));
        int g = (int) (255 * Math.min(1, fraction * 2));
        return 65536 * r + 256 * g;
    }

    public static String getPercentage(double fraction) {
        return PERCENT_FORMAT.format(fraction * 100D);
    }

    public static final ForgeDirection POS_X = ForgeDirection.EAST;
    public static final ForgeDirection NEG_X = ForgeDirection.WEST;
    public static final ForgeDirection POS_Y = ForgeDirection.UP;
    public static final ForgeDirection NEG_Y = ForgeDirection.DOWN;
    public static final ForgeDirection POS_Z = ForgeDirection.SOUTH;
    public static final ForgeDirection NEG_Z = ForgeDirection.NORTH;

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

    public static boolean isBattery(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IBatteryItem;
    }

    public static boolean isDischargeableBattery(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IBatteryItem battery)) return false;
        return battery.getCharge(stack) > 0 && battery.getDischargeRate(stack) > 0;
    }

    public static boolean isEmptyBattery(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IBatteryItem battery)) return false;
        return battery.getCharge(stack) <= 0;
    }

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

    public static long chargeTEFromItems(NTMItemHandler inventory, int index, long power, long maxPower) {
        ItemStack stack = inventory.getStackInSlot(index);
        long powerNeeded = maxPower - power;
        if (powerNeeded <= 0) return power;
        return power + dischargeBatteryIfValid(stack, powerNeeded, false);
    }

    public static long fnv1a64(byte[] data) {
        long hash = 0xcbf29ce484222325L;
        for (byte b : data) {
            hash ^= (b & 0xFFL);
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
