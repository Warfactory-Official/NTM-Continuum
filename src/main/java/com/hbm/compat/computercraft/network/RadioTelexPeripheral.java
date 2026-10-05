package com.hbm.compat.computercraft.network;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.network.TileEntityRadioTelex;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.nbt.CompoundTag;

/**
 * 1.7 {@code TileEntityRadioTelex}'s component {@code ntm_telex}. Writes go through the TELEX's own
 * {@code receiveControl}, the path its screen uses, so channels and lines get the same limits and sync; the port
 * caps a channel at {@link TileEntityRadioTelex#CHANNEL_LENGTH} characters where 1.7's computer path did not.
 */
public final class RadioTelexPeripheral extends SnapshotPeripheral<TileEntityRadioTelex> {

    private static final int LINES = TileEntityRadioTelex.LINE_COUNT;
    private static final int TX = 0;
    private static final int RX = TX + LINES;
    private static final int TX_CHANNEL = RX + LINES;
    private static final int RX_CHANNEL = TX_CHANNEL + 1;

    public RadioTelexPeripheral(TileEntityRadioTelex machine) {
        super(machine, "ntm_telex", 0, RX_CHANNEL + 1);
    }

    @Override
    protected void capture(TileEntityRadioTelex machine) {
        for (int i = 0; i < LINES; i++) {
            putRef(TX + i, machine.txBuffer[i]);
            putRef(RX + i, machine.rxBuffer[i]);
        }
        putRef(TX_CHANNEL, machine.txChannel);
        putRef(RX_CHANNEL, machine.rxChannel);
    }

    @LuaFunction
    public final Object[] getChannels() {
        return read(s -> new Object[]{s.refAt(TX_CHANNEL), s.refAt(RX_CHANNEL)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setChannels(String rx, String tx) {
        TileEntityRadioTelex machine = machine();
        Object[] old = {machine.txChannel, machine.rxChannel};
        CompoundTag data = new CompoundTag();
        data.putString("cmd", "sve");
        data.putString("rxChan", rx);
        data.putString("txChan", tx);
        machine.receiveControl(data);
        return old;
    }

    @LuaFunction
    public final Object[] getSendingTexts() {
        return read(s -> new Object[]{s.refAt(TX), s.refAt(TX + 1), s.refAt(TX + 2), s.refAt(TX + 3),
                s.refAt(TX + 4)});
    }

    @LuaFunction
    public final Object[] getReceivingText() {
        return read(s -> new Object[]{s.refAt(RX), s.refAt(RX + 1), s.refAt(RX + 2), s.refAt(RX + 3),
                s.refAt(RX + 4)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setSendingText(IArguments args) throws LuaException {
        CompoundTag data = new CompoundTag();
        try {
            for (int i = 0; i < LINES; i++) {
                // OpenComputers' checkAny: an absent argument raises, a nil one clears the line
                if (args.count() <= i) throw new LuaException("bad argument #" + (i + 1) + " (value expected)");
                String line = args.get(i) == null ? "" : args.getString(i);
                // receiveControl truncates to the line width, as 1.7 did here
                data.putString("tx" + i, line);
            }
        } finally {
            // 1.7 assigned line by line, so the lines before a bad argument are kept
            machine().receiveControl(data);
        }
        return new Object[]{true};
    }

    @LuaFunction(mainThread = true)
    public final Object[] printMessage() {
        machine().print();
        return new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] clearAll() {
        CompoundTag data = new CompoundTag();
        data.putString("cmd", "rxcls");
        machine().receiveControl(data);
        return new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] sendMessage() {
        TileEntityRadioTelex machine = machine();
        if (machine.isSending) return new Object[]{false};
        CompoundTag data = new CompoundTag();
        data.putString("cmd", "snd");
        machine.receiveControl(data);
        return new Object[]{true};
    }
}
//?}
