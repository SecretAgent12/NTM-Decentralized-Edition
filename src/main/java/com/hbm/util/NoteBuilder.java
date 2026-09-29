// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

/**
 * Backport: the four method bodies were inline bytecode (tenon-asm) in ntm-next,
 * rewritten as the plain Java they encode, instruction for instruction:
 * the same split/parse, the same catch-all returning an empty array.
 */
public class NoteBuilder {

    private String beat = "";

    public static NoteBuilder start() {
        return new NoteBuilder();
    }

    public static Hit[] translate(String beat) {
        try {
            String[] hits = beat.split("-");
            Hit[] notes = new Hit[hits.length];
            for (int index = 0; index < hits.length; index++) {
                String[] components = hits[index].split(":");
                notes[index] = new Hit(
                        Instrument.values()[Integer.parseInt(components[0])],
                        Note.values()[Integer.parseInt(components[1])],
                        Octave.values()[Integer.parseInt(components[2])]);
            }
            return notes;
        } catch (Exception failure) {
            return new Hit[0];
        }
    }

    public NoteBuilder add(Instrument instrument, Note note, Octave octave) {
        if (!this.beat.isEmpty()) this.beat = this.beat + "-";
        this.beat = this.beat + (instrument.ordinal() + ":" + note.ordinal() + ":" + octave.ordinal());
        return this;
    }

    public String end() {
        return this.beat;
    }

    public enum Instrument {
        PIANO,
        BASSDRUM,
        SNARE,
        CLICKS,
        BASSGUITAR
    }

    public enum Note {
        F_SHARP,
        G,
        G_SHARP,
        A,
        A_SHARP,
        B,
        C,
        C_SHARP,
        D,
        D_SHARP,
        E,
        F
    }

    public enum Octave {
        LOW,
        MID,
        HIGH
    }

    public record Hit(Instrument instrument, Note note, Octave octave) {}
}
