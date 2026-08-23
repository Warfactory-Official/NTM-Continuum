package com.hbm.tileentity.network;

import com.hbm.util.NoteBuilder;
import com.hbm.util.NoteBuilder.Instrument;
import com.hbm.util.NoteBuilder.Note;
import com.hbm.util.NoteBuilder.Octave;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.HashMap;
import java.util.Map.Entry;

/** One-tick-delayed, dimension-scoped radio bus. */
public final class RTTYSystem {
    public static final HashMap<ChannelKey, RTTYChannel> broadcast = new HashMap<>();
    public static final HashMap<ChannelKey, Object> newMessages = new HashMap<>();
    private RTTYSystem() {}

    /** Pushes a signal for the next read band; numeric sends on a channel accumulate during the tick. */
    public static void broadcast(Level level, String channelName, Object signal) {
        ChannelKey key = new ChannelKey(level.dimension(), channelName);
        if (NumberUtils.isCreatable("" + signal) && newMessages.containsKey(key)) {
            Object existing = newMessages.get(key);
            if (NumberUtils.isCreatable("" + existing)) {
                try { newMessages.put(key, "" + (Long.parseLong("" + signal) + Long.parseLong("" + existing))); return; }
                catch (Exception ignored) { }
            }
        }
        newMessages.put(key, signal);
    }
    public static RTTYChannel listen(Level level, String channelName) { return broadcast.get(new ChannelKey(level.dimension(), channelName)); }
    /** Must run before machine ticks, preserving the legacy one-tick delivery delay. */
    public static void updateBroadcastQueue(MinecraftServer server) {
        for (Entry<ChannelKey, Object> entry : newMessages.entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey().level());
            if (level == null) continue;
            RTTYChannel channel = new RTTYChannel(); channel.timeStamp = level.getGameTime(); channel.signal = entry.getValue(); broadcast.put(entry.getKey(), channel);
        }
        for (ServerLevel level : server.getAllLevels()) {
            RTTYChannel channel = new RTTYChannel(); channel.timeStamp = level.getGameTime(); channel.signal = getTestSender(channel.timeStamp);
            broadcast.put(new ChannelKey(level.dimension(), "2012-08-06"), channel);
        }
        newMessages.clear();
    }
    public static void clearLevel(ServerLevel level) { broadcast.keySet().removeIf(key -> key.level().equals(level.dimension())); newMessages.keySet().removeIf(key -> key.level().equals(level.dimension())); }
    public static void onServerStopping() { broadcast.clear(); newMessages.clear(); }
    /* Song of Storms at 300 BPM, including the legacy unterminated beat at t=216. */
    public static Object getTestSender(long stamp) {
        int t = (int) (stamp % 640L); Instrument flute = Instrument.PIANO, bass = Instrument.BASSGUITAR;
        return switch (t) {
            case 0, 24, 72, 96, 120, 168 -> beat(bass, Note.C, Octave.LOW);
            case 8, 16, 104, 112, 152, 160, 184, 192, 552, 560 -> chord(bass, Note.D, Note.F, Note.A);
            case 32, 80, 128, 176, 336 -> chord(bass, Note.E, Note.G, Note.B);
            case 48, 144 -> beat(bass, Note.D, Octave.LOW);
            case 56, 64 -> chord(bass, Note.F, Note.A, Note.C);
            case 200, 224, 400, 424, 544 -> chord(bass, Note.C, Note.D, flute, Note.D);
            case 208 -> chord(bass, Note.D, Note.F, Note.A, flute, Note.D);
            case 240, 408 -> beat(flute, Note.F, Octave.LOW);
            case 256, 448 -> chord(bass, Note.D, flute, Note.E);
            case 264, 456 -> chord(bass, Note.F, Note.A, Note.C);
            case 268, 460 -> beat(flute, Note.F, Octave.MID);
            case 272, 464 -> chord(bass, Note.F, Note.A, Note.C, flute, Note.E);
            case 276, 468 -> beat(flute, Note.F, Octave.MID);
            case 280, 472 -> chord(bass, Note.C, flute, Note.E);
            case 284 -> beat(flute, Note.B, Octave.MID);
            case 288 -> chord(bass, Note.E, Note.G, Note.B, flute, Note.A);
            case 304, 496 -> chord(bass, Note.G, flute, Note.A);
            case 312, 320, 360, 368, 504, 512 -> chord(bass, Note.D, Note.F, Note.B, flute, Note.D);
            case 324, 372, 516 -> beat(flute, Note.G, Octave.MID);
            case 328 -> chord(bass, Note.D, flute, Note.A);
            case 352, 384 -> chord(bass, Note.C, Note.F, Note.A);
            case 376 -> chord(bass, Note.F, flute, Note.E);
            case 404 -> beat(flute, Note.F, Octave.LOW);
            case 440 -> chord(bass, Note.E, Note.G, Note.B, flute, Note.D);
            case 416 -> chord(bass, Note.D, Note.F, Note.B);
            case 432 -> chord(bass, Note.E, Note.G, Note.B, flute, Note.D);
            case 476 -> beat(flute, Note.C, Octave.MID);
            case 480 -> chord(bass, Note.E, Note.G, Note.B, flute, Note.A);
            case 520 -> chord(bass, Note.F, flute, Note.A);
            case 528 -> chord(bass, Note.C, Note.E, Note.A, bass, Note.G);
            case 536 -> beat(flute, Note.A, Octave.MID);
            default -> "";
        };
    }
    private static String beat(Instrument instrument, Note note, Octave octave) { return NoteBuilder.start().add(instrument, note, octave).end(); }
    private static String chord(Instrument instrument, Note a, Note b, Note c) { return NoteBuilder.start().add(instrument, a, Octave.LOW).add(instrument, b, Octave.LOW).add(instrument, c, Octave.LOW).end(); }
    private static String chord(Instrument instrument, Note a, Note b, Instrument other, Note c) { return NoteBuilder.start().add(instrument, a, Octave.LOW).add(instrument, b, Octave.LOW).add(other, c, Octave.LOW).end(); }
    private static String chord(Instrument instrument, Note a, Instrument other, Note b) { return NoteBuilder.start().add(instrument, a, Octave.LOW).add(other, b, Octave.LOW).end(); }
    private static String chord(Instrument instrument, Note a, Note b, Note c, Instrument other, Note d) { return NoteBuilder.start().add(instrument, a, Octave.LOW).add(instrument, b, Octave.LOW).add(instrument, c, Octave.MID).add(other, d, Octave.MID).end(); }
    public enum RTTYSpecialSignal { BEGIN_TTY, STOP_TTY, PRINT_BUFFER }
    public record ChannelKey(ResourceKey<Level> level, String channel) { }
    public static final class RTTYChannel { public long timeStamp = -1; public Object signal; }
}
