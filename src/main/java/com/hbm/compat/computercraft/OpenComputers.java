package com.hbm.compat.computercraft;

/**
 * What an OpenComputers callback answered when it threw rather than returned.
 */
public final class OpenComputers {

    private OpenComputers() {}

    /**
     * OpenComputers' {@code NativeLuaArchitecture} fallback for a message-less throwable: values, not a raised
     * error. 1.7's callbacks reach it by dereferencing an empty slot or tank, so a port that returns this keeps the
     * Lua side's {@code nil, "unknown error"} pair.
     */
    public static Object[] unknownError() {
        return new Object[]{null, "unknown error"};
    }
}
