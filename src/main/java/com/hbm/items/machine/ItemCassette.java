package com.hbm.items.machine;

import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
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

    public enum TrackType {

        NULL(              " ",                         null,                                       SoundType.SOUND, 0,        0),
        HATCH(             "Hatch Siren",               "alarm.hatch",              SoundType.LOOP,  3358839,  250),
        ATUOPILOT(         "Autopilot Disconnected",    "alarm.autopilot",          SoundType.LOOP,  11908533, 50),
        AMS_SIREN(         "AMS Siren",                 "alarm.ams_siren",          SoundType.LOOP,  15055698, 50),
        BLAST_DOOR(        "Blast Door Alarm",          "alarm.blast_door_alarm",         SoundType.LOOP,  11665408, 50),
        APC_LOOP(          "APC Siren",                 "alarm.apc_loop",           SoundType.LOOP,  3565216,  50),
        KLAXON(            "Klaxon",                    "alarm.klaxon",             SoundType.LOOP,  8421504,  50),
        KLAXON_A(          "Vault Door Alarm",          "alarm.fo_klaxon_a",        SoundType.LOOP,  0x8c810b, 50),
        KLAXON_B(          "Security Alert",            "alarm.fo_klaxon_b",        SoundType.LOOP,  0x76818e, 50),
        SIREN(             "Standard Siren",            "alarm.regular_siren",      SoundType.LOOP,  6684672,  100),
        CLASSIC(           "Classic Siren",             "alarm.classic",            SoundType.LOOP,  0xc0cfe8, 100),
        BANK_ALARM(        "Bank Alarm",                "alarm.bank_alarm",         SoundType.LOOP,  3572962,  100),
        BEEP_SIREN(        "Beep Siren",                "alarm.beep_siren",         SoundType.LOOP,  13882323, 100),
        CONTAINER_ALARM(   "Container Alarm",           "alarm.container_alarm",    SoundType.LOOP,  14727839, 100),
        SWEEP_SIREN(       "Sweep Siren",               "alarm.sweep_siren",        SoundType.LOOP,  15592026, 500),
        STRIDER_SIREN(     "Missile Silo Siren",        "alarm.strider_siren",      SoundType.LOOP,  11250586, 500),
        AIR_RAID(          "Air Raid Siren",            "alarm.air_raid",           SoundType.LOOP,  0xDF3795, 500),
        NOSTROMO_SIREN(    "Nostromo Self Destruct",    "alarm.nostromo_siren",     SoundType.LOOP,  0x5dd800, 100),
        EAS_ALARM(         "EAS Alarm Screech",         "alarm.eas_alarm",          SoundType.LOOP,  0xb3a8c1, 50),
        APC_PASS(          "APC Pass",                  "alarm.apc_pass",           SoundType.PASS,  3422163,  50),
        RAZORTRAIN(        "Razortrain Horn",           "alarm.razortrain_horn",    SoundType.SOUND, 7819501,  250);

        public static final TrackType[] VALUES = values();

        // Name of the track shown in GUI
        private final String title;
        // Location of the sound: the hbm sound-event key, resolved through ModPowerGenSounds on the client
        private final @Nullable String sound;
        // Sound type, whether the sound should be repeated or not
        private final SoundType type;
        // Color of the cassette
        private final int color;
        // Range where the sound can be heard
        private final int volume;

        TrackType(String name, @Nullable String sound, SoundType type, int msa, int intensity) {
            this.title = name;
            this.sound = sound;
            this.type = type;
            this.color = msa;
            this.volume = intensity;
        }

        public String getTrackTitle() { return title; }
        public @Nullable String getSoundKey() { return sound; }
        public SoundType getType() { return type; }
        public int getColor() { return color; }
        public int getVolume() { return volume; }

        /** The flattened registry id suffix: {@code siren_track_<this>}. */
        public String registryName() {
            return "siren_track_" + name().toLowerCase(Locale.ROOT);
        }

        public static TrackType getEnum(int i) {
            if (i >= 0 && i < VALUES.length) return VALUES[i];
            return NULL;
        }
    }

    public enum SoundType {
        LOOP,
        PASS,
        SOUND
    }
}
