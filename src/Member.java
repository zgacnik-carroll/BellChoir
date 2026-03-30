import javax.sound.sampled.SourceDataLine;

/**
 * A Member of the Bell Choir. Each Member runs in its own thread and
 * is responsible for playing 1 or 2 assigned notes (one per hand).
 *
 * The Conductor signals a Member by calling playNote(), which unblocks
 * the Member's thread to play the note, then waits for it to complete
 * before the Conductor moves on.
 */
public class Member extends Thread {

    private final Tone.Note noteOne;      // First (always assigned) note
    private final Tone.Note noteTwo;      // Second (optional) note — null if only one note
    private final SourceDataLine line;

    private Tone.BellNote pendingNote;    // The note the Conductor wants played next
    private boolean running;         // Whether this Member is still active

    Member(Tone.Note noteOne, Tone.Note noteTwo, SourceDataLine line) {
        this.noteOne = noteOne;
        this.noteTwo = noteTwo;
        this.line = line;
        this.running = true;
        this.pendingNote = null;
    }

    /**
     * Called by the Conductor to assign a note for the Member to play.
     * Blocks until the Member has finished playing the note.
     *
     * @param bn the BellNote to play (must be one of this Member's assigned notes)
     */
    synchronized void playNote(Tone.BellNote bn) {
        // Sanity check — only this Member's notes should ever be passed in
        if (bn.note != noteOne && bn.note != noteTwo) {
            throw new IllegalArgumentException(
                    "Member assigned to [" + noteOne + (noteTwo != null ? ", " + noteTwo : "") +
                            "] cannot play " + bn.note
            );
        }

        // Post the note and wake the Member thread
        this.pendingNote = bn;
        notifyAll();

        // Wait until the Member finishes playing
        while (pendingNote != null) {
            try {
                wait();
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * Signals the Member thread to stop after finishing any current note.
     */
    synchronized void stopMember() {
        running = false;
        notifyAll();
    }

    /**
     * The Member's main loop: wait for the Conductor's signal, play the note,
     * then signal back that it's done.
     */
    @Override
    public void run() {
        synchronized (this) {
            while (running) {
                // Wait for the Conductor to assign a note
                while (pendingNote == null && running) {
                    try {
                        wait();
                    } catch (InterruptedException ignored) {
                    }
                }

                if (!running && pendingNote == null) {
                    break;
                }

                // Play the assigned note
                if (pendingNote != null) {
                    Tone.BellNote toPlay = pendingNote;

                    try {
                        writeAudio(toPlay);
                    } finally {
                        // Signal back to the Conductor that we're done
                        pendingNote = null;
                        notifyAll();
                    }
                }
            }
        }
    }

    /**
     * Writes audio samples for the given note to this Member's audio line.
     * Called while holding the monitor (Conductor is blocked in wait).
     */
    private void writeAudio(Tone.BellNote bn) {
        final int ms = Math.min(bn.length.timeMs(), Tone.Note.MEASURE_LENGTH_SEC * 1000);
        final int length = Tone.Note.SAMPLE_RATE * ms / 1000;
        line.write(bn.note.sample(), 0, length);
        line.write(Tone.Note.REST.sample(), 0, 50);
    }

    @Override
    public String toString() {
        return "Member[" + noteOne + (noteTwo != null ? ", " + noteTwo : "") + "]";
    }
}
