import java.util.List;
import java.util.Map;

import javax.sound.sampled.SourceDataLine;

/**
 * The Conductor controls the tempo of the Bell Choir.
 * It iterates through each BellNote in the song, signals the appropriate
 * Member to play, and then waits for the note to finish before moving on.
 * Only one note plays at a time.
 */
public class Conductor {

    private final SourceDataLine line;
    private final List<Tone.BellNote> song;
    private final Map<Tone.Note, Member> noteToMember;

    Conductor(SourceDataLine line, List<Tone.BellNote> song, Map<Tone.Note, Member> noteToMember) {
        this.line = line;
        this.song = song;
        this.noteToMember = noteToMember;
    }

    /**
     * Plays the song by signaling members one note at a time.
     * For REST notes, the conductor plays silence directly without involving a Member.
     */
    void conduct() {
        for (Tone.BellNote bn : song) {
            if (bn.note == Tone.Note.REST) {
                // Play REST directly — no Member owns REST
                playRest(bn.length);
            } else {
                Member member = noteToMember.get(bn.note);
                if (member == null) {
                    // Should never happen if assignment was done correctly
                    System.err.println("Warning: No member assigned for note " + bn.note);
                    continue;
                }
                // Tell the member which note/length to play, then wait for it to finish
                member.playNote(bn);
            }
        }
    }

    /**
     * Plays silence for a REST of the given length.
     */
    private void playRest(Tone.NoteLength length) {
        final int ms = Math.min(length.timeMs(), Tone.Note.MEASURE_LENGTH_SEC * 1000);
        final int samples = Tone.Note.SAMPLE_RATE * ms / 1000;
        line.write(Tone.Note.REST.sample(), 0, samples);
    }
}
