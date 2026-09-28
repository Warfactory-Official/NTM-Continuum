package com.hbm.util;

/** Compact wire representation used by radio note signals. */
public class NoteBuilder {
    private String beat = "";
    public static NoteBuilder start() { return new NoteBuilder(); }
    public NoteBuilder add(Instrument instrument, Note note, Octave octave) { if (!beat.isEmpty()) beat += "-"; beat += instrument.ordinal() + ":" + note.ordinal() + ":" + octave.ordinal(); return this; }
    public String end() { return beat; }

    /** One decoded {@code instrument:note:octave} hit. */
    public record Hit(Instrument instrument, Note note, Octave octave) {}

    /** 1.7 {@code NoteBuilder.translate}: decodes a beat; any malformed hit empties the whole beat, as 1.7 did. */
    public static Hit[] translate(String beat) {
        String[] hits = beat.split("-");
        Hit[] notes = new Hit[hits.length];
        try {
            for (int i = 0; i < hits.length; i++) {
                String[] components = hits[i].split(":");
                notes[i] = new Hit(Instrument.values()[Integer.parseInt(components[0])],
                        Note.values()[Integer.parseInt(components[1])], Octave.values()[Integer.parseInt(components[2])]);
            }
            return notes;
        } catch (Exception ex) {
            return new Hit[0];
        }
    }
    public enum Instrument { PIANO, BASSDRUM, SNARE, CLICKS, BASSGUITAR }
    public enum Note { F_SHARP, G, G_SHARP, A, A_SHARP, B, C, C_SHARP, D, D_SHARP, E, F }
    public enum Octave { LOW, MID, HIGH }
}
