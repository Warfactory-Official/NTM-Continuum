package com.hbm.util;

public class EnumUtil {

    public static <T extends Enum<T>> T grabEnumSafely(Class<T> theEnum, int index) {
        T[] values = theEnum.getEnumConstants();
        index = Math.abs(index % values.length);
        return values[index];
    }
}
