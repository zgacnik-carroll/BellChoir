# Bell Choir

---

## Description

This project simulates a multithreaded bell choir system in Java. A `Conductor` controls the tempo and cues `Member` threads to play assigned bell notes one at a time, while the program reads song data from text files, validates the input, and converts notes into generated audio. Overall, this project demonstrates multithreaded coordination, synchronization, file validation, and basic sound synthesis using Java.

---

## Requirements

- Java JDK 21
- Apache Ant 1.10.15

Links to these required resources:

- Java JDK 21: https://www.oracle.com/java/technologies/downloads/
- Apache Ant: https://ant.apache.org/bindownload.cgi

---

## Outline

Within this project in GitHub, there are two main directories:

- `src`
- `documentation`

Within the `src` directory, you will find all Java source files used in the Bell Choir program.

Within the `documentation` directory, you will find:

- a [UML diagram](documentation/Bell-Choir-UML-Diagram.pdf) of the project
- a file explaining how the [project requirements](documentation/Project-Requirements-Met.md) were met
- a file describing [challenges faced](documentation/Project-Challenges.md) during development

You will also find song text files in the project root, including:

- `MaryLamb.txt`
- `AmazingGrace.txt`

---

## How to Run

To run this program, clone this GitHub repository into your desired directory. After that, navigate to the cloned directory
and run using Ant:

First, ensure you have Ant installed. This is listed in the Requirements section above. You can confirm with:

```bash
ant -version
```

You should see output similar to:

```text
Apache Ant(TM) version 1.10.15 compiled on August 25 2024
```

Once Ant is installed, navigate to the project directory and run:

```bash
ant run clean
```

This compiles the project and runs the default song file, which is `MaryLamb.txt`.

To run a different song file, use:

```bash
ant run clean -Dsong=your_song_file
```

The program mainly produces sound output rather than console output. If a song file contains invalid lines, warning messages will be printed to the console describing the issue.

---

## Project Features

- Reads and validates song files in bell-note format
- Supports `REST` notes
- Supports note lengths `1`, `2`, `4`, and `8`
- Uses a multithreaded design with separate `Member` threads
- Ensures only one note plays at a time
- Allows different songs to be loaded without code changes
- Includes support for flat-note input by translating flats to equivalent sharp notes

---

## Documentation

The `documentation` directory includes:

- `Bell-Choir-UML-Code.puml`
- `Bell-Choir-UML-Diagram.pdf`
- `Project-Challenges.md`
- `Project-Requirements-Met.md`

These files provide a structural view of the system and explain how the lab requirements were addressed.

---

## Closing Remarks

In conclusion, this project provided experience with multithreaded program design in Java. Coordinating threads while preserving strict note order required careful synchronization between the conductor and choir members. The project also strengthened my understanding of input validation, timing control, and organizing a program into clear cooperating classes.
