# CodeAlpha-JavaProgramming-task-1
this is my intership task no 1.


# StudentGradeTrackerFullFinal

Single-file Java Swing application to manage student grades.

## Overview
`StudentGradeTrackerFullFinal.java` is a single-file Swing application that provides:
- Add / Update / Delete students (IDs start at 101)
- Master students table with scrollbar
- Search by ID or Name (search results shown)
- Generate report (with average/highest/lowest)
- Zoom in / out (Ctrl + '+' / Ctrl + '-')
- Export selected or all students to: TXT, CSV, PNG (table snapshot), JPG
- Optional PDF export if **Apache PDFBox** is added to the classpath (otherwise the PDF menu shows a message)

Everything is implemented in one Java source file for easy sharing and learning.

## Files
- `StudentGradeTrackerFullFinal.java` — main source file (single-file app)
- `README.md` — this file
- `.gitignore` — recommended

## Requirements
- Java JDK 8+ (JDK 11 or 17 recommended)
- (Optional) Apache PDFBox jars in classpath for PDF export

## How to compile & run

### Windows (PowerShell)
```powershell
javac StudentGradeTrackerFullFinal.java
java StudentGradeTrackerFullFinal
