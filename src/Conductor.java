import java.util.List;
import java.util.Map;

import javax.sound.sampled.SourceDataLine;

/**
 * Coordinates playback for the Bell Choir by advancing through the song in order
 * and signaling the assigned {@link Member} for each bell note.
 */
public class Conductor {

    /** Shared output line used for audible notes and rests. */
    private final SourceDataLine line;
    /** Ordered list of bell notes that make up the current song. */
    private final List<Tone.BellNote> song;
    /** Lookup table that binds each playable note to its assigned member thread. */
    private final Map<Tone.Note, Member> noteToMember;

    /**
     * Creates a conductor for a loaded song and the current member assignment map.
     *
     * @param line shared output line for writing generated samples
     * @param song ordered sequence of bell notes to perform
     * @param noteToMember mapping from each note to the member responsible for it
     */
    Conductor(SourceDataLine line, List<Tone.BellNote> song, Map<Tone.Note, Member> noteToMember) {
        this.line = line;
        this.song = song;
        this.noteToMember = noteToMember;
    }

    /**
     * Performs the loaded song from start to finish.
     * REST notes are written directly to the shared audio line, while audible notes
     * are delegated to the member thread that owns the assigned bell.
     */
    void conduct() {
        for (Tone.BellNote bn : song) {
            if (bn.note == Tone.Note.REST) {
                // REST has no owner, so the conductor emits silence itself.
                playRest(bn.length);
            } else {
                Member member = noteToMember.get(bn.note);
                if (member == null) {
                    // This guards against a malformed assignment map without crashing playback.
                    System.err.println("Warning: No member assigned for note " + bn.note);
                    continue;
                }
                // Member.playNote blocks until that note has finished sounding.
                member.playNote(bn);
            }
        }
    }

    /**
     * Writes silence for the requested rest duration.
     *
     * @param length musical duration of the rest to emit
     */
    private void playRest(Tone.NoteLength length) {
        final int ms = Math.min(length.timeMs(), Tone.Note.MEASURE_LENGTH_SEC * 1000);
        final int samples = Tone.Note.SAMPLE_RATE * ms / 1000;
        line.write(Tone.Note.REST.sample(), 0, samples);
    }
}
