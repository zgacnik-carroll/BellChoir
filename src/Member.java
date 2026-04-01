import javax.sound.sampled.SourceDataLine;

/**
 * Represents one bell choir performer.
 * Each member owns one or two notes and waits on its thread until the
 * conductor signals that one of those notes should be played.
 */
public class Member extends Thread {

    /** First assigned note, corresponding to one hand. */
    private final Tone.Note noteOne;
    /** Optional second assigned note, corresponding to the other hand. */
    private final Tone.Note noteTwo;
    /** Shared audio line used to write the generated waveform. */
    private final SourceDataLine line;

    /** Pending note to play; null while the member is idle. */
    private Tone.BellNote pendingNote;
    /** Flag that keeps the thread alive until the choir is finished. */
    private boolean running;

    /**
     * Creates a member that can play one or two assigned notes.
     *
     * @param noteOne first required note assignment
     * @param noteTwo optional second note assignment; may be {@code null}
     * @param line shared audio line for playback
     */
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
        // Reject programming errors where a note is routed to the wrong member.
        if (bn.note != noteOne && bn.note != noteTwo) {
            throw new IllegalArgumentException(
                    "Member assigned to [" + noteOne + (noteTwo != null ? ", " + noteTwo : "") +
                            "] cannot play " + bn.note
            );
        }

        // Publish the requested note and wake the waiting worker thread.
        this.pendingNote = bn;
        notifyAll();

        // The conductor waits here so notes remain strictly sequential.
        while (pendingNote != null) {
            try {
                wait();
            } catch (InterruptedException ignored) {
                // Interruption is ignored.
            }
        }
    }

    /**
     * Signals the member thread to shut down after any current note completes.
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
                // Sleep until the conductor posts the next note assignment.
                while (pendingNote == null && running) {
                    try {
                        wait();
                    } catch (InterruptedException ignored) {
                        // Ignore and keep waiting.
                    }
                }

                if (!running && pendingNote == null) {
                    break;
                }

                // Snapshot the work item before playback, then clear it afterward in finally.
                if (pendingNote != null) {
                    Tone.BellNote toPlay = pendingNote;

                    try {
                        writeAudio(toPlay);
                    } finally {
                        // Wake the conductor so it can continue with the next bell note.
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
     *
     * @param bn note and duration pair to render
     */
    private void writeAudio(Tone.BellNote bn) {
        final int ms = Math.min(bn.length.timeMs(), Tone.Note.MEASURE_LENGTH_SEC * 1000);
        final int length = Tone.Note.SAMPLE_RATE * ms / 1000;
        line.write(bn.note.sample(), 0, length);
        // Add a tiny gap after each note to keep the bell boundaries perceptible.
        line.write(Tone.Note.REST.sample(), 0, 50);
    }

    /**
     * Returns a readable description of the member's assigned notes.
     *
     * @return display string used for debugging
     */
    @Override
    public String toString() {
        return "Member[" + noteOne + (noteTwo != null ? ", " + noteTwo : "") + "]";
    }
}
