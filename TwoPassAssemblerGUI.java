import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import java.util.LinkedHashMap;

class TwoPassAssemblerGUI extends JFrame {

    private JTextField inputFileField, optabFileField;
    private JTextArea intermediateArea, symtabArea, outputArea;
    private JButton assembleBtn;
    private JPanel mainPanel;

    public TwoPassAssemblerGUI() {
        setTitle("Two-Pass Assembler");
        setSize(1280, 720); // Adjusted for smaller screen size
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        mainPanel = createMainPanel();
        add(mainPanel);

        setVisible(true);
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setPreferredSize(new Dimension(1280, 720));

        Font font = new Font("Arial", Font.PLAIN, 14);
        Color bgColor = new Color(245, 245, 245);
        Color buttonColor = new Color(60, 179, 113);

        JLabel inputLabel = new JLabel("Input File:");
        inputLabel.setFont(font);
        inputLabel.setBounds(30, 30, 120, 25);
        panel.add(inputLabel);

        inputFileField = new JTextField(30);
        inputFileField.setBounds(150, 30, 400, 25);
        panel.add(inputFileField);

        JButton browseInputBtn = new JButton("Browse");
        browseInputBtn.setFont(font);
        browseInputBtn.setBounds(570, 30, 100, 25);
        browseInputBtn.addActionListener(e -> browseFile(inputFileField));
        panel.add(browseInputBtn);

        JLabel optabLabel = new JLabel("Optab File:");
        optabLabel.setFont(font);
        optabLabel.setBounds(30, 70, 120, 25);
        panel.add(optabLabel);

        optabFileField = new JTextField(30);
        optabFileField.setBounds(150, 70, 400, 25);
        panel.add(optabFileField);

        JButton browseOptabBtn = new JButton("Browse");
        browseOptabBtn.setFont(font);
        browseOptabBtn.setBounds(570, 70, 100, 25);
        browseOptabBtn.addActionListener(e -> browseFile(optabFileField));
        panel.add(browseOptabBtn);

        assembleBtn = new JButton("Assemble");
        assembleBtn.setFont(new Font("Arial", Font.BOLD, 14));
        assembleBtn.setBackground(buttonColor);
        assembleBtn.setForeground(Color.WHITE);
        assembleBtn.setBounds(300, 110, 150, 30);
        assembleBtn.addActionListener(e -> runAssembler());
        panel.add(assembleBtn);

        JLabel intermediateLabel = new JLabel("Intermediate File Output:");
        intermediateLabel.setFont(font);
        intermediateLabel.setBounds(30, 150, 200, 25);
        panel.add(intermediateLabel);

        JLabel symtabLabel = new JLabel("SymTab Output:");
        symtabLabel.setFont(font);
        symtabLabel.setBounds(320, 150, 200, 25);
        panel.add(symtabLabel);

        intermediateArea = new JTextArea(10, 40);
        intermediateArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane intermediateScroll = new JScrollPane(intermediateArea);
        intermediateScroll.setBounds(30, 180, 250, 400);
        panel.add(intermediateScroll);

        symtabArea = new JTextArea(10, 40);
        symtabArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane symtabScroll = new JScrollPane(symtabArea);
        symtabScroll.setBounds(320, 180, 250, 400);
        panel.add(symtabScroll);

        JLabel outputLabel = new JLabel("Object Code Output:");
        outputLabel.setFont(font);
        outputLabel.setBounds(610, 150, 200, 25);
        panel.add(outputLabel);

        outputArea = new JTextArea(10, 40);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBounds(610, 180, 300, 400);
        panel.add(outputScroll);

        panel.setBackground(bgColor);
        return panel;
    }

    private void browseFile(JTextField field) {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            field.setText(file.getPath());
        }
    }

    private void runAssembler() {
        String inputFile = inputFileField.getText();
        String optabFile = optabFileField.getText();

        if (inputFile.isEmpty() || optabFile.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please provide both the input file and opcode table file!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        assembleBtn.setEnabled(false);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                TwoPassAssembler assembler = new TwoPassAssembler(inputFile, optabFile);
                try {
                    assembler.loadOptab();
                    assembler.passOne();
                    assembler.passTwo();
                    StringBuilder intermediateOutput = new StringBuilder();
                    for (Map.Entry<Integer, String> entry : assembler.getIntermediateStart().entrySet()) {
                        intermediateOutput.append(String.format("%04X  %s\n", entry.getKey(), entry.getValue()));
                    }
                    for (Map.Entry<Integer, String> entry : assembler.getIntermediate().entrySet()) {
                        intermediateOutput.append(String.format("%04X  %s\n", entry.getKey(), entry.getValue()));
                    }
                    intermediateArea.setText(intermediateOutput.toString());

                    StringBuilder symbolTableOutputStr = new StringBuilder();
                    for (Map.Entry<String, Integer> entry : assembler.getSymtab().entrySet()) {
                        symbolTableOutputStr.append(String.format("%s\t%04X\n", entry.getKey(), entry.getValue()));
                    }
                    symtabArea.setText(symbolTableOutputStr.toString());

                    StringBuilder objectCodeOutputStr = new StringBuilder();
                    for (Map.Entry<Integer, String> entry : assembler.getObjectCode().entrySet()) {
                        objectCodeOutputStr.append(entry.getValue()).append("\n");
                    }
                    outputArea.setText(objectCodeOutputStr.toString());

                } catch (IOException e) {
                    JOptionPane.showMessageDialog(TwoPassAssemblerGUI.this, "Error running the assembler: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
                return null;
            }

            @Override
            protected void done() {
                assembleBtn.setEnabled(true);
            }
        };

        worker.execute();
    }

    private void displayFileContent(String filename, JTextArea textArea) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        textArea.setText("");
        String line;
        while ((line = reader.readLine()) != null) {
            textArea.append(line + "\n");
        }
        reader.close();
    }
}

class TwoPassAssembler {
    private final String inputFile;
    private final String optabFile;
    private final Map<String, String> optab = new HashMap<>();
    private final Map<String, Integer> symtab = new LinkedHashMap<>();
    private final Map<Integer, String> intermediate = new LinkedHashMap<>();
    private final Map<Integer, String> intermediateStart = new LinkedHashMap<>();
    private final Map<Integer, String> objectCode = new LinkedHashMap<>();
    private int locctr = 0;
    private int start = 0;
    private int length = 0;

    public TwoPassAssembler(String inputFile, String optabFile) {
        this.inputFile = inputFile;
        this.optabFile = optabFile;
    }

    public void loadOptab() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(optabFile));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split("\\s+");
            optab.put(parts[0], parts[1]);
        }
        reader.close();
    }

    public void passOne() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        String line = reader.readLine();
        String[] parts = line.split("\\s+");

        if (parts[1].equals("START")) {
            start = Integer.parseInt(parts[2], 16); // Parse start as hexadecimal
            locctr = start;
            intermediateStart.put(locctr, String.format("\t%s\t%s\t%s", parts[0], parts[1], parts[2]));
            line = reader.readLine();
        } else {
            locctr = 0;
        }

        // Process each line
        while (line != null) {
            parts = line.split("\\s+");
            if (parts[1].equals("END")) break;

            // Store intermediate with location counter in hex format
            intermediate.put(locctr, String.format("\t%s\t%s\t%s", parts[0], parts[1], parts[2]));

            // Insert into symbol table if label exists
            if (!parts[0].equals("-")) {
                symtab.put(parts[0], locctr);
            }

            // Update locctr based on the opcode
            if (optab.containsKey(parts[1])) {
                locctr += 3;
            } else if (parts[1].equals("WORD")) {
                locctr += 3;
            } else if (parts[1].equals("BYTE")) {
                locctr += parts[2].length() - 3;
            } else if (parts[1].equals("RESW")) {
                locctr += 3 * Integer.parseInt(parts[2]);
            } else if (parts[1].equals("RESB")) {
                locctr += Integer.parseInt(parts[2]);
            }

            line = reader.readLine();
        }

        // Store final line in intermediate and calculate program length
        intermediate.put(locctr, String.format("\t%s\t%s\t%s", parts[0], parts[1], parts[2]));
        length = locctr - start;

        reader.close();
    }

    public void passTwo(){
        String line;
        String[] parts;

        String startLine = intermediateStart.get(start);
        String[] startParts = startLine.trim().split("\\s+");

        // Handle "START" directive
        if (startParts[1].equals("START")) {
            objectCode.put(0, "H^" + startParts[0] + "^" + String.format("%06X", start) + "^" + String.format("%06X", length));
            //line = intermediate.get(start + 1);
        } else {
            objectCode.put(0, "H^" + " " + "^0000^" + String.format("%06X", length));
        }

        StringBuilder textRecord1 = new StringBuilder();
        StringBuilder textRecord2 = new StringBuilder();
        int textStartAddr = 0;
        int textLength = 0;

        for (int loc : intermediate.keySet()) {

            line = intermediate.get(loc);
            parts = line.trim().split("\\s+");
            if (parts.length < 3) continue;

            if (parts[2].equals("END")) break;

            if (textLength == 0) {
                textStartAddr = loc;
                textRecord1.append("T^").append(String.format("%06X", textStartAddr)).append("^");
            }

            // Generate object code for each line
            if (optab.containsKey(parts[1])) {
                String machineCode = optab.get(parts[1]);
                int address = symtab.getOrDefault(parts[2], 0);
                String code = machineCode + String.format("%04X", address);
                textRecord2.append(code).append("^");
                textLength += code.length() / 2;
            } else if (parts[1].equals("WORD")) {
                String wordCode = String.format("%06X", Integer.parseInt(parts[2]));
                textRecord2.append(wordCode).append("^");
                textLength += wordCode.length() / 2;
            } else if (parts[1].equals("BYTE")) {
                String byteCode = parts[2].substring(2, parts[2].length() - 1); // Extract value from BYTE literal
                // bytecode to Ascii
                StringBuilder asciiString = new StringBuilder();
                for (int i = 0; i < byteCode.length(); i++) {
                    int asciiValue = byteCode.charAt(i); // Convert each character to ASCII
                    asciiString.append(asciiValue); // Append the ASCII value
                }
                textRecord2.append(String.format("%02X", Integer.parseInt(asciiString.toString()))).append("^");
                textLength += byteCode.length() / 2;
            } else if (parts[1].equals("RESW") || parts[1].equals("RESB")) {
                // If we hit RESW/RESB, flush the current text record and start a new one after reserving memory
                if (textLength > 0) {
                    textRecord1.append(String.format("%02X", textLength)).append("^").append(textRecord2);
                    objectCode.put(textStartAddr, textRecord1.toString());
                    textRecord1 = new StringBuilder();
                    textRecord2 = new StringBuilder();
                    textLength = 0;
                }
                continue; // Do not generate object code for reserved space
            }

            if (textLength >= 30) { // Text records should not exceed 30 bytes (60 hex characters)
                textRecord1.append(String.format("%02X", textLength)).append("^").append(textRecord2);
                objectCode.put(textStartAddr, textRecord1.toString());
                textRecord1 = new StringBuilder();
                textRecord2 = new StringBuilder();
                textLength = 0;
            }
        }

        // Write remaining text record if not empty
        if (textLength > 0) {
            textRecord1.append(String.format("%02X", textLength)).append("^").append(textRecord2);
            objectCode.put(textStartAddr, textRecord1.toString());
        }

        // Write End record
        objectCode.put(locctr, "E^" + String.format("%06X", start));
    }


    public Map<Integer, String> getIntermediate() {
        return intermediate;
    }

    public Map<Integer, String> getIntermediateStart() {
        return intermediateStart;
    }

    public Map<String, Integer> getSymtab() {
        return symtab;
    }

    public Map<Integer, String> getObjectCode() {
        return objectCode;
    }

}

class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(TwoPassAssemblerGUI::new);
    }
}
