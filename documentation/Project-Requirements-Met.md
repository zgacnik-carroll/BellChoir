# Bell Choir Requirements Summary

---

This document explains how the Bell Choir project satisfies the given requirements.

---

## Core Program Requirements

### Read and validate song files

The program reads a song file line by line in `Tone.loadSong`. Each line is validated for:

- correct two-token format: `<NOTE> <LENGTH>`
- supported note names
- supported note lengths: `1`, `2`, `4`, or `8`

Invalid lines are skipped with a warning instead of crashing the program. Empty lines and comment lines are ignored.

### Hand out note assignments to Members

After a song is loaded, `Tone.playSong` collects the unique notes used in the song and assigns them to `Member` objects. Each `Member` receives:

- at least one note
- no more than two notes

This models one bell per hand.

### Once assigned, only that Member plays the note

`Tone.playSong` builds a `noteToMember` map, and `Conductor.conduct` uses that map every time a note must be played. `Member.playNote` also checks that the requested note belongs to that member and throws an error if it does not.

### Conductor controls tempo and note order

`Conductor.conduct` steps through the loaded song in order. For each bell note, it either:

- plays a `REST` directly, or
- signals the assigned `Member`

The conductor waits for each note to finish before moving on to the next one, so the song stays in sequence with the intended rhythm.

### Only one note plays at a time

The design intentionally serializes playback:

- the conductor issues one note at a time
- `Member.playNote` blocks until that note finishes
- the conductor does not continue until the member signals completion

This ensures that the song is played as a single melodic line rather than overlapping notes.

### Correct note timing

The `Tone.NoteLength` enum converts note values into durations:

- `1` = whole note
- `2` = half note
- `4` = quarter note
- `8` = eighth note

Playback length is then derived from those values and written to the audio line.

### Able to play multiple songs

The Ant build accepts a configurable `song` property, so the same program can play different correctly formatted files.
Running without a song property defaults to `MaryLamb.txt`:

```bash
ant run clean
ant run clean -Dsong=AmazingGrace.txt
```

This satisfies the requirement that the assignment can play instructor-provided and student-provided songs.

---

## Note and Song Format Requirements

### Supported note scale

The project defines a 13-note scale plus `REST` in `Tone.Note`:

- `A4`
- `A4S`
- `B4`
- `C4`
- `C4S`
- `D4`
- `D4S`
- `E4`
- `F4`
- `F4S`
- `G4`
- `G4S`
- `A5`
- `REST`

### Sharps and flats

Sharps are supported directly with the `S` suffix, such as `A4S`.

Flat-note support was also implemented as extra credit. Flat tokens with a trailing `B` are translated into their sharp equivalents in `Tone.resolveFlatNotation`.

### Bell note representation

Each bell note is stored as a `Tone.BellNote`, which contains:

- a `Tone.Note`
- a `Tone.NoteLength`

Each line in a song file represents exactly one bell note.

---

## Build and Execution Requirements

### Must use ANT to build and run

The project includes `build.xml` with:

- `clean`
- `compile`
- `run`

This satisfies the ANT requirement.

### Each Member must play in a separate thread

`Member` extends `Thread`, and `Tone.playSong` creates and starts a thread for each assigned member.

### Must play Mary Had a Little Lamb

The project includes `MaryLamb.txt`, and the build is configured so it can be run directly through Ant. The file format is valid for the parser, and the song plays through the same Bell Choir pipeline as any other song.

### Must handle improper song files

The parser is defensive:

- malformed lines are reported and skipped
- unknown notes are reported and skipped
- invalid lengths are reported and skipped
- unreadable files cause a clear error message
- files with no valid notes are rejected

This behavior supports the final demonstration requirement involving invalid input.

---

## Extra Credit

### Custom song

A song file that I created is included and contains more than the required minimum:

- at least 5 distinct notes (mine has 6)
- at least 15 bell notes (mine has 20)

### Flat-note support

The project includes flat-note translation logic, which directly addresses the extra-credit option for flat notes.

---

## Conclusion

The Bell Choir project meets the required architectural, threading, parsing, timing, and build expectations for the lab. It also includes both extra-credit features: a custom song and support for flat notes.
