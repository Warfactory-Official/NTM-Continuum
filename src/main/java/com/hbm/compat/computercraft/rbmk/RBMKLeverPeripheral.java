package com.hbm.compat.computercraft.rbmk;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKLever;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKLever.LeverUnit;
import dan200.computercraft.api.lua.LuaFunction;

import java.util.LinkedHashMap;
import java.util.Map;

/** 1.7 {@code TileEntityRBMKLever}'s component {@code rbmk_lever}; levers are numbered 1-2. */
public final class RBMKLeverPeripheral extends SnapshotPeripheral<BlockEntityRBMKLever> {

    private static final int N = BlockEntityRBMKLever.LEVERS;
    private static final String INVALID = "Invalid index (1-2)";
    private static final int ACTIVE = 0;
    private static final int POLLING = 1;
    private static final int PROGRESS = 2;
    private static final int STRIDE = 3;
    private static final int LABEL = 0;
    private static final int CHANNEL = 1;
    private static final int ON = 2;
    private static final int OFF = 3;
    private static final int REF_STRIDE = 4;

    public RBMKLeverPeripheral(BlockEntityRBMKLever machine) {
        super(machine, "rbmk_lever", N * STRIDE, N * REF_STRIDE);
    }

    @Override
    protected void capture(BlockEntityRBMKLever machine) {
        for (int u = 0; u < N; u++) {
            LeverUnit lever = machine.levers[u];
            put(u * STRIDE + ACTIVE, lever.active);
            put(u * STRIDE + POLLING, lever.polling);
            put(u * STRIDE + PROGRESS, lever.flipProgress);
            putRef(u * REF_STRIDE + LABEL, lever.label);
            putRef(u * REF_STRIDE + CHANNEL, lever.rtty);
            putRef(u * REF_STRIDE + ON, lever.commandOn);
            putRef(u * REF_STRIDE + OFF, lever.commandOff);
        }
    }

    @LuaFunction
    public final Object[] getLeverInfo(double index) {
        int idx = (int) index - 1;
        if (idx < 0 || idx >= N) return new Object[]{null, INVALID};
        return read(s -> {
            float progress = (float) s.doubleAt(idx * STRIDE + PROGRESS);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("active", s.booleanAt(idx * STRIDE + ACTIVE));
            map.put("polling", s.booleanAt(idx * STRIDE + POLLING));
            map.put("state", progress >= 1.0);
            map.put("progress", progress);
            map.put("label", s.refAt(idx * REF_STRIDE + LABEL));
            map.put("channel", s.refAt(idx * REF_STRIDE + CHANNEL));
            map.put("commandOn", s.refAt(idx * REF_STRIDE + ON));
            map.put("commandOff", s.refAt(idx * REF_STRIDE + OFF));
            return new Object[]{map};
        });
    }

    @LuaFunction(mainThread = true)
    public final Object[] setLeverActive(double index, boolean active) {
        int idx = (int) index - 1;
        if (idx < 0 || idx >= N) return new Object[]{false, INVALID};
        BlockEntityRBMKLever machine = machine();
        machine.levers[idx].active = active;
        machine.setChanged();
        return new Object[]{true};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setLeverLabel(double index, String label) {
        int idx = (int) index - 1;
        if (idx < 0 || idx >= N) return new Object[]{false, INVALID};
        BlockEntityRBMKLever machine = machine();
        machine.levers[idx].label = label;
        machine.setChanged();
        return new Object[]{true};
    }
}
//?}
