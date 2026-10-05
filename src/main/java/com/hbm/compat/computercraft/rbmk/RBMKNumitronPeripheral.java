package com.hbm.compat.computercraft.rbmk;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKNumitron;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKNumitron.DisplayUnit;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** 1.7 {@code TileEntityRBMKNumitron}'s component {@code rbmk_numitron}; displays are numbered 1-2. */
public final class RBMKNumitronPeripheral extends SnapshotPeripheral<BlockEntityRBMKNumitron> {

    private static final int N = BlockEntityRBMKNumitron.DISPLAYS;
    private static final String INVALID = "Invalid index (1-2)";
    private static final int SHORTEN = 0;
    private static final int DIGITS = 1;
    private static final int ZEROES = 2;
    private static final int ACTIVE = 3;
    private static final int POLLING = 4;
    private static final int VALUE = 5;
    private static final int STRIDE = 6;
    private static final int LABEL = 0;
    private static final int CHANNEL = 1;

    public RBMKNumitronPeripheral(BlockEntityRBMKNumitron machine) {
        super(machine, "rbmk_numitron", N * STRIDE, N * 2);
    }

    @Override
    protected void capture(BlockEntityRBMKNumitron machine) {
        for (int u = 0; u < N; u++) {
            DisplayUnit d = machine.displays[u];
            int b = u * STRIDE;
            put(b + SHORTEN, d.shorten_number);
            put(b + DIGITS, d.active_digits);
            put(b + ZEROES, d.leading_zeroes);
            put(b + ACTIVE, d.active);
            put(b + POLLING, d.polling);
            put(b + VALUE, d.value);
            putRef(u * 2 + LABEL, d.label);
            putRef(u * 2 + CHANNEL, d.rtty);
        }
    }

    @LuaFunction
    public final Object[] getDisplayInfo(double index) {
        int idx = (int) index - 1;
        if (idx < 0 || idx >= N) return new Object[]{null, INVALID};
        return read(s -> {
            int b = idx * STRIDE;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("shorten_number", s.booleanAt(b + SHORTEN));
            map.put("active_digits", s.longAt(b + DIGITS));
            map.put("leading_zeroes", s.booleanAt(b + ZEROES));
            map.put("active", s.booleanAt(b + ACTIVE));
            map.put("polling", s.booleanAt(b + POLLING));
            map.put("label", s.refAt(idx * 2 + LABEL));
            map.put("channel", s.refAt(idx * 2 + CHANNEL));
            map.put("value", s.longAt(b + VALUE));
            return new Object[]{map};
        });
    }

    private Object[] set(double index, Consumer<DisplayUnit> change) {
        int idx = (int) index - 1;
        if (idx < 0 || idx >= N) return new Object[]{false, INVALID};
        BlockEntityRBMKNumitron machine = machine();
        change.accept(machine.displays[idx]);
        machine.setChanged();
        return new Object[]{true};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setDisplayActive(double index, boolean active) {
        return set(index, d -> d.active = active);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setDisplayPolling(double index, boolean polling) {
        return set(index, d -> d.polling = polling);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setDisplayLabel(double index, String label) {
        return set(index, d -> d.label = label);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setDisplayChannel(double index, String channel) {
        return set(index, d -> d.rtty = channel);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setDisplayValue(double index, double value) {
        long val = (int) value;
        return set(index, d -> d.value = val);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setDisplayLeadingZeroes(double index, boolean zeroes) {
        return set(index, d -> d.leading_zeroes = zeroes);
    }

    // 1.7 :289 throws IllegalArgumentException, which OpenComputers raises as a Lua error
    @LuaFunction(mainThread = true)
    public final Object[] setDisplayActiveDigits(double index, double digits) throws LuaException {
        int idx = (int) index - 1;
        if (idx < 0 || idx >= N) return new Object[]{false, INVALID};
        long val = (int) digits;
        if (val < 0 || val >= 128) throw new LuaException("Invalid value (0-127)");
        return set(index, d -> d.active_digits = val);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setDisplayShortenNumber(double index, boolean shorten) {
        return set(index, d -> d.shorten_number = shorten);
    }
}
//?}
