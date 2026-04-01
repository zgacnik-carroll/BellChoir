# Project Challenges

---

This document summarizes some of the main challenges faced while building the Bell Choir project.

---

## 1. Coordinating threads without overlapping notes

The assignment required a multi-threaded design, but it also required only one note to play at a time. That creates a tension:

- multiple `Member` objects must run in separate threads
- playback still has to remain strictly sequential

The solution was to let each `Member` wait on its own thread until the `Conductor` assigns a note. The conductor then blocks until that note finishes before moving to the next one.

---

## 2. Mapping musical ideas onto a limited note set

The project only supports a fixed 13-note scale. That means some recognizable tunes cannot be copied directly if they require notes outside the supported range.

This became especially important when creating custom songs, because a melody had to remain recognizable while still using only the notes that the parser and synthesizer support.

---

## 3. Validating bad input without breaking the program

The lab specifically expects invalid song files during demonstration, so the parser had to be resilient.

Instead of terminating immediately on the first bad line, the program:

- reports the issue clearly
- skips the invalid line
- continues processing the rest of the file

That behavior makes the project more robust and better suited for live testing.

---

## 4. Keeping timing simple and consistent

The program uses a simplified timing model where one measure equals one second. This makes note lengths straightforward to calculate, but it still requires careful conversion from musical note values into sample durations.

Small implementation choices, such as adding a short gap after a note, also affect how clearly the melody is heard.

---

## 5. Balancing simplicity with good design

Because this is a lab project, the code needed to stay understandable while still showing:

- file parsing
- note validation
- thread coordination
- conductor/member responsibilities
- audio playback

The final structure separates these concerns across `Tone`, `Conductor`, and `Member`, which keeps the design readable without making the assignment too complex..
