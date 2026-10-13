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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** One-tick-delayed, dimension-scoped radio bus. */
public final class RTTYSystem {
    public static final ConcurrentHashMap<ChannelKey, RTTYChannel> broadcast = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<ChannelKey, Object> newMessages = new ConcurrentHashMap<>();
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
    /**
     * Publishes only the named pending channels for deterministic GameTests. Unlike the server-tick
     * driver, this does not publish the synthetic melody or clear unrelated channels.
     */
    public static void updateTestChannels(MinecraftServer server, String... channelNames) {
        Set<String> requested = new HashSet<>(Arrays.asList(channelNames));
        for (Entry<ChannelKey, Object> entry : newMessages.entrySet()) {
            if (!requested.contains(entry.getKey().channel())) continue;
            ServerLevel level = server.getLevel(entry.getKey().level());
            if (level != null) {
                RTTYChannel channel = new RTTYChannel();
                channel.timeStamp = level.getGameTime();
                channel.signal = entry.getValue();
                broadcast.put(entry.getKey(), channel);
            }
            newMessages.remove(entry.getKey(), entry.getValue());
        }
    }
    public static void clearLevel(ServerLevel level) { broadcast.keySet().removeIf(key -> key.level().equals(level.dimension())); newMessages.keySet().removeIf(key -> key.level().equals(level.dimension())); }
    public static void clearChannel(Level level, String channelName) { clearChannel(level.dimension(), channelName); }
    public static void clearChannel(ResourceKey<Level> level, String channelName) { ChannelKey key = new ChannelKey(level, channelName); broadcast.remove(key); newMessages.remove(key); }
    public static void onServerStopping() { broadcast.clear(); newMessages.clear(); }
    /* Song of Storms at 300 BPM. This is kept literal: octave differences are part of the wire signal. */
    private static final Object[] TEST_SENDER_MELODY;
    static {
        int tempo = 4;
        TEST_SENDER_MELODY = new Object[tempo * 160];
        Arrays.fill(TEST_SENDER_MELODY, "");

        Instrument flute = Instrument.PIANO;
        Instrument accordion = Instrument.BASSGUITAR;

        TEST_SENDER_MELODY[tempo * 0] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 2] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 4] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 6] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 8] = NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 12] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 14] = NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).add(accordion, Note.C, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 16] = NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).add(accordion, Note.C, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 18] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 20] = NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 24] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 26] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 28] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 30] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 32] = NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 36] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 38] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 40] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 42] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 44] = NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 48] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(flute, Note.D, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 50] = NoteBuilder.start().add(flute, Note.F, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 52] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).add(flute, Note.D, Octave.MID).end();
        // Upstream stores the unterminated builder itself at tick 216; consumers see a non-String no-op.
        TEST_SENDER_MELODY[tempo * 54] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW);
        TEST_SENDER_MELODY[tempo * 56] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(flute, Note.D, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 58] = NoteBuilder.start().add(flute, Note.F, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 60] = NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.LOW).add(flute, Note.D, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 64] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(flute, Note.E, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 66] = NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).add(accordion, Note.C, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 67] = NoteBuilder.start().add(flute, Note.F, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 68] = NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).add(accordion, Note.C, Octave.MID).add(flute, Note.E, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 69] = NoteBuilder.start().add(flute, Note.F, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 70] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(flute, Note.E, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 71] = NoteBuilder.start().add(flute, Note.B, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 72] = NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.LOW).add(flute, Note.A, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 76] = NoteBuilder.start().add(accordion, Note.G, Octave.LOW).add(flute, Note.A, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 78] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.B, Octave.MID).add(flute, Note.D, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 80] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.B, Octave.MID).add(flute, Note.F, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 81] = NoteBuilder.start().add(flute, Note.G, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 82] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(flute, Note.A, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 84] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 88] = NoteBuilder.start().add(accordion, Note.G, Octave.LOW).add(flute, Note.A, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 90] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.B, Octave.MID).add(flute, Note.D, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 92] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.B, Octave.MID).add(flute, Note.F, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 93] = NoteBuilder.start().add(accordion, Note.B, Octave.MID).add(flute, Note.G, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 94] = NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(flute, Note.E, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 96] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 100] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(flute, Note.D, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 101] = NoteBuilder.start().add(flute, Note.F, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 102] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.B, Octave.MID).add(flute, Note.D, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 104] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.B, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 106] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(flute, Note.D, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 107] = NoteBuilder.start().add(flute, Note.F, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 108] = NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.MID).add(flute, Note.D, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 112] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(flute, Note.E, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 114] = NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).add(accordion, Note.C, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 115] = NoteBuilder.start().add(flute, Note.F, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 116] = NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).add(accordion, Note.C, Octave.MID).add(flute, Note.E, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 117] = NoteBuilder.start().add(flute, Note.F, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 118] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(flute, Note.E, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 119] = NoteBuilder.start().add(flute, Note.C, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 120] = NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.MID).add(flute, Note.A, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 124] = NoteBuilder.start().add(accordion, Note.G, Octave.LOW).add(flute, Note.A, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 126] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.MID).add(flute, Note.D, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 128] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.MID).add(flute, Note.F, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 129] = NoteBuilder.start().add(flute, Note.G, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 130] = NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(flute, Note.A, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 132] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(accordion, Note.E, Octave.LOW).add(accordion, Note.A, Octave.MID).add(accordion, Note.G, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 134] = NoteBuilder.start().add(flute, Note.A, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 136] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(flute, Note.D, Octave.LOW).end();
        TEST_SENDER_MELODY[tempo * 138] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.MID).end();
        TEST_SENDER_MELODY[tempo * 140] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.MID).end();
    }
    public static Object getTestSender(long stamp) { return TEST_SENDER_MELODY[(int) (stamp % TEST_SENDER_MELODY.length)]; }
    public enum RTTYSpecialSignal { BEGIN_TTY, STOP_TTY, PRINT_BUFFER }
    public record ChannelKey(ResourceKey<Level> level, String channel) { }
    public static final class RTTYChannel { public long timeStamp = -1; public Object signal; }
}
