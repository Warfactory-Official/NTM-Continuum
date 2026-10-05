package com.hbm.compat.computercraft.rbmk;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKKeyPad;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKKeyPad.KeyUnit;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.util.Mth;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** 1.7 {@code TileEntityRBMKKeyPad}'s component {@code rbmk_keypad}; keys are numbered 1-4. */
public final class RBMKKeyPadPeripheral extends SnapshotPeripheral<BlockEntityRBMKKeyPad> {

    private static final int N = BlockEntityRBMKKeyPad.KEYS;
    private static final String INVALID = "Invalid index (1-4)";
    private static final int ACTIVE = 0;
    private static final int POLLING = 1;
    private static final int PRESSED = 2;
    private static final int COLOR = 3;
    private static final int STRIDE = 4;
    private static final int LABEL = 0;
    private static final int CHANNEL = 1;
    private static final int COMMAND = 2;
    private static final int REF_STRIDE = 3;

    public RBMKKeyPadPeripheral(BlockEntityRBMKKeyPad machine) {
        super(machine, "rbmk_keypad", N * STRIDE, N * REF_STRIDE);
    }

    @Override
    protected void capture(BlockEntityRBMKKeyPad machine) {
        for (int u = 0; u < N; u++) {
            KeyUnit key = machine.keys[u];
            put(u * STRIDE + ACTIVE, key.active);
            put(u * STRIDE + POLLING, key.polling);
            put(u * STRIDE + PRESSED, key.isPressed);
            put(u * STRIDE + COLOR, key.color);
            putRef(u * REF_STRIDE + LABEL, key.label);
            putRef(u * REF_STRIDE + CHANNEL, key.rtty);
            putRef(u * REF_STRIDE + COMMAND, key.command);
        }
    }

    @LuaFunction
    public final Object[] getKeyInfo(double index) {
        int idx = (int) index - 1;
        if (idx < 0 || idx >= N) return new Object[]{null, INVALID};
        return read(s -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("active", s.booleanAt(idx * STRIDE + ACTIVE));
            map.put("polling", s.booleanAt(idx * STRIDE + POLLING));
            map.put("pressed", s.booleanAt(idx * STRIDE + PRESSED));
            map.put("color", s.intAt(idx * STRIDE + COLOR));
            map.put("label", s.refAt(idx * REF_STRIDE + LABEL));
            map.put("channel", s.refAt(idx * REF_STRIDE + CHANNEL));
            map.put("command", s.refAt(idx * REF_STRIDE + COMMAND));
            return new Object[]{map};
        });
    }

    private Object[] set(double index, Consumer<KeyUnit> change) {
        int idx = (int) index - 1;
        if (idx < 0 || idx >= N) return new Object[]{false, INVALID};
        BlockEntityRBMKKeyPad machine = machine();
        change.accept(machine.keys[idx]);
        machine.setChanged();
        return new Object[]{true};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setKeyActive(double index, boolean active) {
        return set(index, k -> k.active = active);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setKeyPolling(double index, boolean polling) {
        return set(index, k -> k.polling = polling);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setKeyColor(double index, double color) {
        int clamped = Mth.clamp((int) color, 0, 0xffffff);
        return set(index, k -> k.color = clamped);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setKeyLabel(double index, String label) {
        return set(index, k -> k.label = label);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setKeyChannel(double index, String channel) {
        return set(index, k -> k.rtty = channel);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setKeyCommand(double index, String command) {
        return set(index, k -> k.command = command);
    }

    @LuaFunction(mainThread = true)
    public final Object[] pressKey(double index) {
        int idx = (int) index - 1;
        if (idx < 0 || idx >= N) return new Object[]{false, INVALID};
        BlockEntityRBMKKeyPad machine = machine();
        if (!machine.keys[idx].active) return new Object[]{false, "Key is not active"};
        machine.keys[idx].click();
        machine.setChanged();
        return new Object[]{true};
    }

    @LuaFunction
    public final Object[] getKeyPressed(double index) {
        int idx = (int) index - 1;
        if (idx < 0 || idx >= N) return new Object[]{false, INVALID};
        return read(s -> new Object[]{s.booleanAt(idx * STRIDE + PRESSED)});
    }
}
//?}
