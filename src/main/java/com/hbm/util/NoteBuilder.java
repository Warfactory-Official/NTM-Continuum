package com.hbm.util;

/** Compact wire representation used by radio note signals. */
public class NoteBuilder {
    private String beat = "";
    public static NoteBuilder start() { return new NoteBuilder(); }
    public NoteBuilder add(Instrument instrument, Note note, Octave octave) { if (!beat.isEmpty()) beat += "-"; beat += instrument.ordinal() + ":" + note.ordinal() + ":" + octave.ordinal(); return this; }
    public String end() { return beat; }
    public enum Instrument { PIANO, BASSDRUM, SNARE, CLICKS, BASSGUITAR }
    public enum Note { F_SHARP, G, G_SHARP, A, A_SHARP, B, C, C_SHARP, D, D_SHARP, E, F }
    public enum Octave { LOW, MID, HIGH }
}
