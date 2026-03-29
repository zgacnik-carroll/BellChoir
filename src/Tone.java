import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
     * Valid lengths: 1=WHOLE, 2=HALF, 4=QUARTER, 8=EIGTH
     * Blank lines and lines starting with '#' are ignored as comments.
     *
     * Returns null if the file cannot be loaded or contains no valid notes.
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

                try {
                    note = Note.valueOf(parts[0].toUpperCase());
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

    void playSong(List<BellNote> song) throws LineUnavailableException {
        try (final SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
            line.open();
            line.start();

            for (BellNote bn : song) {
                playNote(line, bn);
            }
            line.drain();
        }
    }

    private void playNote(SourceDataLine line, BellNote bn) {
        final int ms = Math.min(bn.length.timeMs(), Note.MEASURE_LENGTH_SEC * 1000);
        final int length = Note.SAMPLE_RATE * ms / 1000;
        line.write(bn.note.sample(), 0, length);
        line.write(Note.REST.sample(), 0, 50);
    }
}

class BellNote {
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
    EIGTH(0.125f);

    private final int timeMs;

    private NoteLength(float length) {
        timeMs = (int)(length * Note.MEASURE_LENGTH_SEC * 1000);
    }

    public int timeMs() {
        return timeMs;
    }

    public static NoteLength fromNumeric(int n) {
        switch (n) {
            case 1: return WHOLE;
            case 2: return HALF;
            case 4: return QUARTER;
            case 8: return EIGTH;
            default: throw new IllegalArgumentException(
                    "Invalid note length: " + n + ". Valid values: 1, 2, 4, 8"
            );
        }
    }
}

enum Note {
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

    public static final int SAMPLE_RATE = 48 * 1024;
    public static final int MEASURE_LENGTH_SEC = 1;

    private static final double step_alpha = (2.0d * Math.PI) / SAMPLE_RATE;

    private final double FREQUENCY_A_HZ = 440.0d;
    private final double MAX_VOLUME = 127.0d;

    private final byte[] sinSample = new byte[MEASURE_LENGTH_SEC * SAMPLE_RATE];

    private Note() {
        int n = this.ordinal();
        if (n > 0) {
            final double halfStepUpFromA = n - 1;
            final double exp = halfStepUpFromA / 12.0d;
            final double freq = FREQUENCY_A_HZ * Math.pow(2.0d, exp);

            final double sinStep = freq * step_alpha;
            for (int i = 0; i < sinSample.length; i++) {
                sinSample[i] = (byte)(Math.sin(i * sinStep) * MAX_VOLUME);
            }
        }
    }

    public byte[] sample() {
        return sinSample;
    }
}