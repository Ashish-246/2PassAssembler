
# Two-Pass Assembler GUI

This is a GUI-based Two-Pass Assembler implemented in Java, designed to assemble programs based on an input file and an opcode table. The tool reads the input assembly code and generates the corresponding object code, symbol table, and intermediate files.

## Features
- **Graphical User Interface (GUI):** Allows users to select input and opcode table files.
- **Two-Pass Assembly Process:**
  1. **Pass One:** Generates an intermediate file and a symbol table.
  2. **Pass Two:** Produces the final object code.
- **File Browsing:** Users can easily browse and select files using the file chooser.
- **Error Handling:** Provides user-friendly error messages for missing files or other issues.

## Requirements
- Java 8 or higher
- Java Swing library (part of JDK)

## Usage
1. Compile the project:
   ```bash
   javac TwoPassAssemblerGUI.java
