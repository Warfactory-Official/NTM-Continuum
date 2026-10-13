package com.hbm.items.machine;

import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import com.hbm.sound.ModPowerGenSounds;

/**
 * One flattened variant of the 1.7 {@code siren_track} metadata item ({@code ItemCassette}). The 1.7 item was
 * one registry entry with 21 damage values; here every {@link TrackType} but {@code NULL} is its own item,
 * {@code siren_track_<variant>}. Names, types, colours and ranges are the 1.7 table verbatim, including the
 * {@code ATUOPILOT} spelling, which is the enum constant the registry id is flattened from. {@link TrackType} names
 * its sound by key rather than by registry object so the Dejson declaration class can read the table off-game.
 */
public class ItemCassette extends CompatItem {

    public final TrackType track;

    public ItemCassette(Item.Properties properties, TrackType track) {
        super(properties);
        this.track = track;
    }

    /** 1.7: every cassette is "Siren Track"; the GUI and tooltip carry the track title. */
    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.hbm.siren_track");
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Siren sound cassette:"));
        tooltip.add(Component.literal("   Name: " + track.getTrackTitle()));
        tooltip.add(Component.literal("   Type: " + track.getType().name()));
        tooltip.add(Component.literal("   Volume: " + track.getVolume()));
    }

    /** 1.7 {@code ItemCassette.getType}: the track a stack carries, {@code NULL} for anything else. */
    public static TrackType getType(@Nullable ItemStack stack) {
        if (stack != null && stack.getItem() instanceof ItemCassette cassette) return cassette.track;
        return TrackType.NULL;
    }

    /**
     * A siren track (1.7 {@code ItemCassette.TrackType}): GUI title, sound, whether it loops, cassette colour and audible
     * range. A registry since addons add tracks: {@link #register} at mod construction, then register a cassette item
     * {@code new ItemCassette(props, track)}. The sound is an HBM siren key ({@code alarm.hatch}) or any sound event id
     * ({@code <namespace>:<path>}). Ids are the 1.7 names for HBM's tracks; {@link #ordinal()} is the runtime position
     * the siren packet carries (never saved).
     */
    public static final class TrackType {

        private static final List<TrackType> ALL = new CopyOnWriteArrayList<>();
        private static final Map<String, TrackType> BY_NAME = new ConcurrentHashMap<>();

        public static final TrackType NULL = register("NULL", " ", null, SoundType.SOUND, 0, 0);
        public static final TrackType HATCH = register("HATCH", "Hatch Siren", "alarm.hatch", SoundType.LOOP, 3358839, 250);
        public static final TrackType ATUOPILOT = register("ATUOPILOT", "Autopilot Disconnected", "alarm.autopilot", SoundType.LOOP, 11908533, 50);
        public static final TrackType AMS_SIREN = register("AMS_SIREN", "AMS Siren", "alarm.ams_siren", SoundType.LOOP, 15055698, 50);
        public static final TrackType BLAST_DOOR = register("BLAST_DOOR", "Blast Door Alarm", "alarm.blast_door_alarm", SoundType.LOOP, 11665408, 50);
        public static final TrackType APC_LOOP = register("APC_LOOP", "APC Siren", "alarm.apc_loop", SoundType.LOOP, 3565216, 50);
        public static final TrackType KLAXON = register("KLAXON", "Klaxon", "alarm.klaxon", SoundType.LOOP, 8421504, 50);
        public static final TrackType KLAXON_A = register("KLAXON_A", "Vault Door Alarm", "alarm.fo_klaxon_a", SoundType.LOOP, 0x8c810b, 50);
        public static final TrackType KLAXON_B = register("KLAXON_B", "Security Alert", "alarm.fo_klaxon_b", SoundType.LOOP, 0x76818e, 50);
        public static final TrackType SIREN = register("SIREN", "Standard Siren", "alarm.regular_siren", SoundType.LOOP, 6684672, 100);
        public static final TrackType CLASSIC = register("CLASSIC", "Classic Siren", "alarm.classic", SoundType.LOOP, 0xc0cfe8, 100);
        public static final TrackType BANK_ALARM = register("BANK_ALARM", "Bank Alarm", "alarm.bank_alarm", SoundType.LOOP, 3572962, 100);
        public static final TrackType BEEP_SIREN = register("BEEP_SIREN", "Beep Siren", "alarm.beep_siren", SoundType.LOOP, 13882323, 100);
        public static final TrackType CONTAINER_ALARM = register("CONTAINER_ALARM", "Container Alarm", "alarm.container_alarm", SoundType.LOOP, 14727839, 100);
        public static final TrackType SWEEP_SIREN = register("SWEEP_SIREN", "Sweep Siren", "alarm.sweep_siren", SoundType.LOOP, 15592026, 500);
        public static final TrackType STRIDER_SIREN = register("STRIDER_SIREN", "Missile Silo Siren", "alarm.strider_siren", SoundType.LOOP, 11250586, 500);
        public static final TrackType AIR_RAID = register("AIR_RAID", "Air Raid Siren", "alarm.air_raid", SoundType.LOOP, 0xDF3795, 500);
        public static final TrackType NOSTROMO_SIREN = register("NOSTROMO_SIREN", "Nostromo Self Destruct", "alarm.nostromo_siren", SoundType.LOOP, 0x5dd800, 100);
        public static final TrackType EAS_ALARM = register("EAS_ALARM", "EAS Alarm Screech", "alarm.eas_alarm", SoundType.LOOP, 0xb3a8c1, 50);
        public static final TrackType APC_PASS = register("APC_PASS", "APC Pass", "alarm.apc_pass", SoundType.PASS, 3422163, 50);
        public static final TrackType RAZORTRAIN = register("RAZORTRAIN", "Razortrain Horn", "alarm.razortrain_horn", SoundType.SOUND, 7819501, 250);

        /** Every registered track in registration order, HBM's 1.7 tracks first. */
        public static final List<TrackType> VALUES = Collections.unmodifiableList(ALL);

        private final String name;
        private final int ordinal;
        // Name of the track shown in GUI
        private final String title;
        // Location of the sound: an HBM siren key resolved through ModPowerGenSounds, or a sound event id
        private final @Nullable String sound;
        // Sound type, whether the sound should be repeated or not
        private final SoundType type;
        // Color of the cassette
        private final int color;
        // Range where the sound can be heard
        private final int volume;

        private TrackType(String name, int ordinal, String title, @Nullable String sound, SoundType type, int color, int volume) {
            this.name = name;
            this.ordinal = ordinal;
            this.title = title;
            this.sound = sound;
            this.type = type;
            this.color = color;
            this.volume = volume;
        }

        /** Registers a track; {@code name} is its id (an addon should use {@code <namespace>:<name>}). */
        public static synchronized TrackType register(String name, String title, @Nullable String sound, SoundType type,
                                                      int color, int volume) {
            if (BY_NAME.containsKey(name)) throw new IllegalStateException("Siren track " + name + " registered twice");
            TrackType track = new TrackType(name, ALL.size(), title, sound, type, color, volume);
            ALL.add(track);
            BY_NAME.put(name, track);
            return track;
        }

        public String name() { return name; }
        public int ordinal() { return ordinal; }
        @Override public String toString() { return name; }
        public String getTrackTitle() { return title; }
        public @Nullable String getSoundKey() { return sound; }
        public SoundType getType() { return type; }
        public int getColor() { return color; }
        public int getVolume() { return volume; }

        /** Whether this is one of HBM's own tracks (an id without a namespace), whose cassette HBM registers. */
        public boolean isBuiltIn() {
            return name.indexOf(':') < 0;
        }

        /** The flattened registry id suffix of HBM's cassette: {@code siren_track_<this>}; HBM's own tracks only. */
        public String registryName() {
            return "siren_track_" + name.toLowerCase(Locale.ROOT);
        }

        public static @Nullable TrackType byName(String name) {
            return BY_NAME.get(name);
        }

        public static TrackType getEnum(int i) {
            if (i >= 0 && i < ALL.size()) return ALL.get(i);
            return NULL;
        }
    }

    public enum SoundType {
        LOOP,
        PASS,
        SOUND
    }
}
