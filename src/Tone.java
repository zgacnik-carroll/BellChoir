import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Tone {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: ant run -Dsong=<song-file.txt>");
            System.exit(1);
        }

        final AudioFormat af =
                new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, false);
        Tone t = new Tone(af);

        List<BellNote> song = t.loadSong(args[0]);
        if (song == null) {
            System.exit(1); // loadSong already printed the error
        }

        try {
            t.playSong(song);
        } catch (LineUnavailableException e) {
            System.err.println("Error: Audio line unavailable. " + e.getMessage());
            System.exit(1);
        }
    }

    private final AudioFormat af;

    Tone(AudioFormat af) {
        this.af = af;
    }

    /**
     * Loads a song from a text file. Each line should contain a note name
     * and a numeric length separated by whitespace, e.g.:
     *
     *   A5 4    (A5 quarter note)
     *   G4 2    (G4 half note)
     *   REST 1  (whole rest)
     *
     * Valid lengths: 1=WHOLE, 2=HALF, 4=QUARTER, 8=EIGHTH
     * Blank lines and lines starting with '#' are ignored as comments.
     * Flat notes are supported using 'B' suffix (e.g., E4B = D4S).
     *
     * Returns null if the file cannot be read or contains no valid notes.
     * Invalid lines are skipped with a warning rather than aborting.
     */
    List<BellNote> loadSong(String filename) {
        List<BellNote> notes = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                if (parts.length != 2) {
                    System.err.println("Skipping line " + lineNumber + ": invalid format \"" + line +
                            "\". Expected: <NOTE> <LENGTH>");
                    continue;
                }

                Note note;
                NoteLength length;

                // Resolve flat notation (e.g., E4B -> D4S)
                String noteStr = resolveFlatNotation(parts[0].toUpperCase());

                try {
                    note = Note.valueOf(noteStr);
                } catch (IllegalArgumentException e) {
                    System.err.println("Skipping line " + lineNumber + ": unknown note \"" + parts[0] + "\".");
                    continue;
                }

                try {
                    length = NoteLength.fromNumeric(Integer.parseInt(parts[1]));
                } catch (NumberFormatException e) {
                    System.err.println("Skipping line " + lineNumber + ": invalid length \"" + parts[1] +
                            "\". Expected a number: 1, 2, 4, or 8.");
                    continue;
                } catch (IllegalArgumentException e) {
                    System.err.println("Skipping line " + lineNumber + ": " + e.getMessage());
                    continue;
                }

                notes.add(new BellNote(note, length));
            }
        } catch (IOException e) {
            System.err.println("Error reading file \"" + filename + "\": " + e.getMessage());
            return null;
        }

        if (notes.isEmpty()) {
            System.err.println("Error: Song file \"" + filename + "\" contains no valid notes.");
            return null;
        }

        return notes;
    }

    /**
     * Converts flat notation (e.g., E4B) to equivalent sharp notation (D4S).
     * Flat notes are a half step below the named note, which equals the sharp of the note below.
     * Flat mapping:
     *   REST  -> REST  (unchanged)
     *   A4B   -> G4S
     *   A4SB  -> A4   (cancel out)
     *   B4B   -> A4S
     *   C4B   -> B4   (no B sharp in scale)
     *   C4SB  -> C4
     *   D4B   -> C4S
     *   D4SB  -> D4
     *   E4B   -> D4S
     *   F4B   -> E4
     *   F4SB  -> F4
     *   G4B   -> F4S
     *   G4SB  -> G4
     *   A5B   -> G4S  (A5 flat = G#4 = G4S)
     */
    private String resolveFlatNotation(String noteStr) {
        if (!noteStr.endsWith("B") || noteStr.equals("REST")) {
            return noteStr;
        }

        // Flat notation is any valid note token with an extra trailing B, e.g. A4B or A4SB.
        // Strip the trailing B
        String base = noteStr.substring(0, noteStr.length() - 1);

        Map<String, String> flatToSharp = new HashMap<>();
        flatToSharp.put("A4", "G4S");
        flatToSharp.put("A4S", "A4");
        flatToSharp.put("B4", "A4S");
        flatToSharp.put("C4", "B4");
        flatToSharp.put("C4S", "C4");
        flatToSharp.put("D4", "C4S");
        flatToSharp.put("D4S", "D4");
        flatToSharp.put("E4", "D4S");
        flatToSharp.put("F4", "E4");
        flatToSharp.put("F4S", "F4");
        flatToSharp.put("G4", "F4S");
        flatToSharp.put("G4S", "G4");
        flatToSharp.put("A5", "G4S");

        String resolved = flatToSharp.get(base);
        if (resolved == null) {
            // Not a known flat mapping — return as-is and let Note.valueOf handle the error
            return noteStr;
        }
        return resolved;
    }

    /**
     * Assigns notes to Members (threads), then uses a Conductor to play the song.
     * Each Member gets at most 2 unique notes (one per hand).
     * Notes are played one at a time, in order, with correct timing.
     */
    void playSong(List<BellNote> song) throws LineUnavailableException {
        // Collect unique non-REST notes in order of first appearance
        List<Note> uniqueNotes = new ArrayList<>();
        for (BellNote bn : song) {
            if (bn.note != Note.REST && !uniqueNotes.contains(bn.note)) {
                uniqueNotes.add(bn.note);
            }
        }

        try (final SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
            line.open();
            line.start();

            // Assign notes to Members — each Member handles 1 or 2 notes
            // and all playback goes through the same output line.
            Map<Note, Member> noteToMember = new HashMap<>();
            List<Member> members = new ArrayList<>();

            int i = 0;
            while (i < uniqueNotes.size()) {
                Note first = uniqueNotes.get(i);
                Note second = (i + 1 < uniqueNotes.size()) ? uniqueNotes.get(i + 1) : null;

                Member m = new Member(first, second, line);
                noteToMember.put(first, m);
                if (second != null) {
                    noteToMember.put(second, m);
                }
                members.add(m);
                i += (second != null) ? 2 : 1;
            }

            for (Member m : members) {
                m.start();
            }

            Conductor conductor = new Conductor(line, song, noteToMember);
            conductor.conduct();

            line.drain();
            for (Member m : members) {
                m.stopMember();
            }
            for (Member m : members) {
                try {
                    m.join();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    static class BellNote {
        final Note note;
        final NoteLength length;

        BellNote(Note note, NoteLength length) {
            this.note = note;
            this.length = length;
        }
    }

    enum NoteLength {
        WHOLE(1.0f),
        HALF(0.5f),
        QUARTER(0.25f),
        EIGHTH(0.125f);

        private final int timeMs;

        NoteLength(float length) {
            timeMs = (int) (length * Note.MEASURE_LENGTH_SEC * 1000);
        }

        public int timeMs() {
            return timeMs;
        }

        public static NoteLength fromNumeric(int n) {
            switch (n) {
                case 1: return WHOLE;
                case 2: return HALF;
                case 4: return QUARTER;
                case 8: return EIGHTH;
                default: throw new IllegalArgumentException(
                        "Invalid note length: " + n + ". Valid values: 1, 2, 4, 8"
                );
            }
        }
    }

    enum Note {
        // REST must be the first 'Note'
        REST,
        A4,
        A4S,
        B4,
        C4,
        C4S,
        D4,
        D4S,
        E4,
        F4,
        F4S,
        G4,
        G4S,
        A5;

        public static final int SAMPLE_RATE = 48 * 1024; // ~48KHz
        public static final int MEASURE_LENGTH_SEC = 1;

        private static final double step_alpha = (2.0d * Math.PI) / SAMPLE_RATE;

        private final double FREQUENCY_A_HZ = 440.0d;
        private final double MAX_VOLUME = 127.0d;

        private final byte[] sinSample = new byte[MEASURE_LENGTH_SEC * SAMPLE_RATE];

        Note() {
            int n = this.ordinal();
            if (n > 0) {
                final double halfStepUpFromA = n - 1;
                final double exp = halfStepUpFromA / 12.0d;
                final double freq = FREQUENCY_A_HZ * Math.pow(2.0d, exp);

                final double sinStep = freq * step_alpha;
                for (int i = 0; i < sinSample.length; i++) {
                    sinSample[i] = (byte) (Math.sin(i * sinStep) * MAX_VOLUME);
                }
            }
        }

        public byte[] sample() {
            return sinSample;
        }
    }
}
