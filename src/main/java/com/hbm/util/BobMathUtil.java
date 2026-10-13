package com.hbm.util;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class BobMathUtil {

    private BobMathUtil() {
    }

    // 1.7: shuffled order de-biases flood-fills
    public static Direction[] getShuffledDirs() {
        List<Direction> dirs = new ArrayList<>(List.of(Direction.values()));
        Collections.shuffle(dirs);
        return dirs.toArray(new Direction[0]);
    }

    public static long min(long... nums) {
        long smallest = Long.MAX_VALUE;
        for (long num : nums) if (num < smallest) smallest = num;
        return smallest;
    }

    public static long max(long... nums) {
        long largest = Long.MIN_VALUE;
        for (long num : nums) if (num > largest) largest = num;
        return largest;
    }

    /**
     * Soft peak sine
     */
    public static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    // 1.12.2: CE-reworked radiation: sqrt(x + 1/(x+2)^2) - 1/(x+2)
    public static double sqrt(double x) {
        return Math.sqrt(x + 1D / ((x + 2D) * (x + 2D))) - 1D / (x + 2D);
    }

    public static boolean getBlink() {
        return System.currentTimeMillis() % 1000 < 500;
    }

    // 1.7: BobMathUtil.getCrossAngle (util/BobMathUtil.java:95)
    public static double getCrossAngle(Vec3 vel, Vec3 rel) {
        double vecProd = rel.x * vel.x + rel.y * vel.y + rel.z * vel.z;
        double bot = rel.length() * vel.length();
        double angle = Math.acos(vecProd / bot) * 180 / Math.PI;

        if (angle >= 180)
            angle -= 180;

        return angle;
    }

    public static double interp(double x, double y, double interp) {
        return x + (y - x) * interp;
    }

    public static int min(int... nums) {
        int min = nums[0];
        for (int i = 1; i < nums.length; i++) min = Math.min(min, nums[i]);
        return min;
    }

    // 26.1: caller supplies level's RandomSource
    public static void shuffleIntArray(int[] array, RandomSource rand) {
        for (int i = array.length - 1; i > 0; i--) {
            int r = rand.nextInt(i + 1);
            int temp = array[r];
            array[r] = array[i];
            array[i] = temp;
        }
    }

    public static void reverseIntArray(int[] array) {
        int len = array.length;
        for (int i = 0; i < len / 2; i++) {
            int temp = array[i];
            array[i] = array[len - 1 - i];
            array[len - 1 - i] = temp;
        }
    }

    /** 1.7 BobMathUtil.format: grouped decimals, US separators. */
    public static String format(Number amount) {
        return String.format(Locale.US, "%,d", amount);
    }

    /** 1.7 {@code BobMathUtil.getShortNumber}: sign kept, two decimals (one for values <= -100 of a unit). */
    public static String getShortNumber(long l) {
        double res;
        String suffix;
        long abs = Math.abs(l);

        if (abs >= Math.pow(10, 18)) {
            res = l / Math.pow(10, 18);
            suffix = "E";
        } else if (abs >= Math.pow(10, 15)) {
            res = l / Math.pow(10, 15);
            suffix = "P";
        } else if (abs >= Math.pow(10, 12)) {
            res = l / Math.pow(10, 12);
            suffix = "T";
        } else if (abs >= Math.pow(10, 9)) {
            res = l / Math.pow(10, 9);
            suffix = "G";
        } else if (abs >= Math.pow(10, 6)) {
            res = l / Math.pow(10, 6);
            suffix = "M";
        } else if (abs >= Math.pow(10, 3)) {
            res = l / Math.pow(10, 3);
            suffix = "k";
        } else {
            return Long.toString(l);
        }

        // 1.7: a negative triple-digit value loses one decimal place to stay short.
        if (res <= -100.0) {
            res = Math.round(res * 10.0) / 10.0;
        } else {
            res = Math.round(res * 100.0) / 100.0;
        }

        return res + suffix;
    }

    // 1.7: ticks -> {year, day, time}, on a 48000-tick day and a 100-day year, time rescaled onto a 0..10 clock.
    public static String[] ticksToDate(long ticks) {
        int tickDay = 48000;
        int tickYear = tickDay * 100;

        long year = Math.floorDiv(ticks, tickYear);
        byte day = (byte) Math.floorDiv(ticks - (long) tickYear * year, tickDay);
        float time = ticks - ((long) tickYear * year + (long) tickDay * day);
        time = time / tickDay * 10F;
        return new String[]{String.valueOf(year), String.valueOf(day), String.valueOf(time)};
    }

    public static double angularDifference(double alpha, double beta) {
        double delta = (beta - alpha + 180) % 360 - 180;
        return delta < -180 ? delta + 360 : delta;
    }
}
