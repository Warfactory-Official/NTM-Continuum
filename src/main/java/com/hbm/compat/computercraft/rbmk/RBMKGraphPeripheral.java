package com.hbm.compat.computercraft.rbmk;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKGraph;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKGraph.GraphUnit;
import dan200.computercraft.api.lua.LuaFunction;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** 1.7 {@code TileEntityRBMKGraph}'s component {@code rbmk_graph}; graphs are numbered 1-2, 30 samples each. */
public final class RBMKGraphPeripheral extends SnapshotPeripheral<BlockEntityRBMKGraph> {

    private static final int N = BlockEntityRBMKGraph.GRAPHS;
    private static final int SAMPLES = 30;
    private static final String INVALID = "Invalid index (1-2)";
    private static final int ACTIVE = 0;
    private static final int POLLING = 1;
    private static final int VALUES = 2;
    private static final int STRIDE = VALUES + SAMPLES;
    private static final int LABEL = 0;
    private static final int CHANNEL = 1;

    public RBMKGraphPeripheral(BlockEntityRBMKGraph machine) {
        super(machine, "rbmk_graph", N * STRIDE, N * 2);
    }

    @Override
    protected void capture(BlockEntityRBMKGraph machine) {
        for (int u = 0; u < N; u++) {
            GraphUnit graph = machine.graphs[u];
            int b = u * STRIDE;
            put(b + ACTIVE, graph.active);
            put(b + POLLING, graph.polling);
            for (int i = 0; i < SAMPLES; i++) put(b + VALUES + i, graph.values[i]);
            putRef(u * 2 + LABEL, graph.label);
            putRef(u * 2 + CHANNEL, graph.rtty);
        }
    }

    @LuaFunction
    public final Object[] getGraphInfo(double index) {
        int idx = (int) index - 1;
        if (idx < 0 || idx >= N) return new Object[]{null, INVALID};
        return read(s -> {
            int b = idx * STRIDE;
            // CC turns an Object[] into a table but has no conversion for a long[]
            Long[] values = new Long[SAMPLES];
            for (int i = 0; i < SAMPLES; i++) values[i] = s.longAt(b + VALUES + i);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("active", s.booleanAt(b + ACTIVE));
            map.put("polling", s.booleanAt(b + POLLING));
            map.put("label", s.refAt(idx * 2 + LABEL));
            map.put("channel", s.refAt(idx * 2 + CHANNEL));
            map.put("values", values);
            return new Object[]{map};
        });
    }

    @LuaFunction
    public final Object[] getGraphMin(double index) {
        int idx = (int) index - 1;
        if (idx < 0 || idx >= N) return new Object[]{0L, INVALID};
        return read(s -> {
            long min = Long.MAX_VALUE;
            for (int i = 0; i < SAMPLES; i++) min = Math.min(min, s.longAt(idx * STRIDE + VALUES + i));
            return new Object[]{min == Long.MAX_VALUE ? 0L : min};
        });
    }

    @LuaFunction
    public final Object[] getGraphMax(double index) {
        int idx = (int) index - 1;
        if (idx < 0 || idx >= N) return new Object[]{0L, INVALID};
        return read(s -> {
            long max = Long.MIN_VALUE;
            for (int i = 0; i < SAMPLES; i++) max = Math.max(max, s.longAt(idx * STRIDE + VALUES + i));
            return new Object[]{max == Long.MIN_VALUE ? 0L : max};
        });
    }

    @LuaFunction
    public final Object[] getGraphAvg(double index) {
        int idx = (int) index - 1;
        if (idx < 0 || idx >= N) return new Object[]{0.0, INVALID};
        return read(s -> {
            long sum = 0;
            for (int i = 0; i < SAMPLES; i++) sum += s.longAt(idx * STRIDE + VALUES + i);
            return new Object[]{(double) sum / SAMPLES};
        });
    }

    private Object[] set(double index, Consumer<GraphUnit> change) {
        int idx = (int) index - 1;
        if (idx < 0 || idx >= N) return new Object[]{false, INVALID};
        BlockEntityRBMKGraph machine = machine();
        change.accept(machine.graphs[idx]);
        machine.setChanged();
        return new Object[]{true};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setGraphActive(double index, boolean active) {
        return set(index, g -> g.active = active);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setGraphPolling(double index, boolean polling) {
        return set(index, g -> g.polling = polling);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setGraphLabel(double index, String label) {
        return set(index, g -> g.label = label);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setGraphChannel(double index, String channel) {
        return set(index, g -> g.rtty = channel);
    }

    @LuaFunction(mainThread = true)
    public final Object[] pushGraphValue(double index, double value) {
        long val = (int) value;
        return set(index, g -> g.pushValue(val));
    }
}
//?}
