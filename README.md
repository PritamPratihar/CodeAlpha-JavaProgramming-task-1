# CodeAlpha-JavaProgramming-task-1
this is my intership task no 1.


# Student Grade Tracker — StudentGradeTrackerFullFinal.java

Single-file Java Swing application that manages student scores and lets you export data.

## What this project does

* Add, update, delete students (IDs start at **101**)
* Master table with all students (scrollable)
* Search by **ID** or **Name** (results shown in a separate table)
* Generate a **Report** (shows list + text summary)
* Zoom In / Zoom Out (Ctrl + `+` / Ctrl + `-` or the UI Zoom buttons)
* Export single student or all students to: **TXT**, **CSV**, **PNG** (screenshot of table), **JPG**
* Optional **PDF** export if you put Apache PDFBox on the classpath (the app will detect PDFBox at runtime)

> Everything is contained in one file: `StudentGradeTrackerFullFinal.java` — no external GUI frameworks required.

---

## Requirements

* Java 8 or later (tested on Java 8+)
* (Optional) Apache PDFBox **if** you want PDF export

If you want PDF export, add the PDFBox JAR(s) to the classpath at compile and run time (instructions below).

---

## Files

* `StudentGradeTrackerFullFinal.java` — main and only source file.

---

## How to compile and run

### 1) Compile & run (without PDFBox)

```bash
# compile
javac StudentGradeTrackerFullFinal.java
# run
java StudentGradeTrackerFullFinal
```

This runs the app without PDF export capability. The UI will show a hint if PDFBox is not present.

### 2) Compile & run (with PDFBox - optional)

1. Download PDFBox jars (for example `pdfbox-app-<version>.jar`) from the Apache PDFBox site.
2. Put the JAR in the same folder as the `.java` file or a known lib folder.

Compile:

```bash
javac -cp pdfbox-app-<version>.jar StudentGradeTrackerFullFinal.java
```

Run:

```bash
java -cp .:pdfbox-app-<version>.jar StudentGradeTrackerFullFinal   # macOS / Linux
java -cp .;pdfbox-app-<version>.jar StudentGradeTrackerFullFinal   # Windows
```

> The program uses reflection to call PDFBox classes at runtime, so the app still compiles without PDFBox. If the library is present at runtime, the PDF menu items will work.

---

## Usage notes

* **IDs**: auto-increment starting from `101` for demo and new adds.
* **Add**: type name and score (0–100) then press **Add Student**.
* **Update**: click **Update (ID/Name)** to search and load a student into the form. You can also select a master row to edit the form and press the Update button when available.
* **Delete**: select a master table row and press **Delete Selected**.
* **Search**: choose `ID` or `Name`, type your query and press **Search** — results show in the Search Results tab.
* **Get Report**: select an aggregate (Average / Highest / Lowest) and press **Get Report**. The report populates the Report tab and shows a text summary.
* **Export**: use the `Download Selected` or `Download All` buttons to choose format. The app opens a save dialog to choose filename and location.
* **Zoom**: use **Zoom + / Zoom -** buttons or keyboard shortcuts (Ctrl + `+` / Ctrl + `-`) to scale UI fonts.

---

## Export behavior

* **TXT** — plain text representation.
* **CSV** — CSV of rows; summary appended as commented lines beginning with `#`.
* **PNG/JPG** — snapshot image of the master table (or current table view).
* **PDF** — only available when PDFBox is on the classpath; the app uses reflection to avoid hard dependency.

---

## Customization & extension ideas

* Persist data between runs (save/load CSV or simple serialization / SQLite).
* Add validation for score range (currently expects numeric — you can enhance checks).
* Allow multi-row selection and batch delete / export.
* Add sorting and column filters.
* Improve PDF layout when PDFBox is present (multi-page support, fonts, table layout).

---

## Troubleshooting

* **UI looks small/large**: use Zoom buttons or the keyboard shortcuts.
* **PDF export shows "requires Apache PDFBox"**: ensure the correct PDFBox JAR is in the classpath when running.
* **Compilation errors about PDFBox classes**: you do **not** need PDFBox on compile-time classpath (the code uses reflection), but if you add it to the classpath, use the `-cp` compile/run examples above.

---

## GitHub - how to add README.md and push

```bash
git init
git add StudentGradeTrackerFullFinal.java README.md
git commit -m "Add StudentGradeTrackerFullFinal app and README"
# create remote repo on GitHub then:
git remote add origin <your-repo-url>
git push -u origin main
```

---

## License

You can add any license you prefer (MIT is a common simple choice). Example header you can place at top of file:

```
// MIT License
// Copyright (c) 2025 Your Name
// Permission is hereby granted, free of charge, to any person obtaining a copy...
```

---

## Need help?

If you want, I can also:

* Create a `build` script for compilation + PDFBox handling.
* Add `data` persistence (save/load CSV on exit/start).
* Generate a sample JAR bundle.

Tell me which of the above you want and I will prepare it.

