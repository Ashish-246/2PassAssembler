import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

class TwoPassAssemblerGUI extends JFrame {

    private JTextField inputFileField, optabFileField;
    private JTextArea intermediateArea, symtabArea, outputArea;
    private JButton assembleBtn;
    private JPanel mainPanel;

    public TwoPassAssemblerGUI() {
        setTitle("Two-Pass Assembler");
        setSize(1920, 1080);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        mainPanel = createMainPanel();
        add(mainPanel);

        // Show the main panel directly
        setVisible(true);
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setPreferredSize(new Dimension(1920, 1080));

        Font font = new Font("Arial", Font.PLAIN, 16);
        Color bgColor = new Color(245, 245, 245);
        Color buttonColor = new Color(60, 179, 113); // New color for the assemble button

        JLabel inputLabel = new JLabel("Input File:");
        inputLabel.setFont(font);
        inputLabel.setBounds(50, 50, 120, 30);
        panel.add(inputLabel);

        inputFileField = new JTextField(30);
        inputFileField.setBounds(180, 50, 400, 30);
        panel.add(inputFileField);

        JButton browseInputBtn = new JButton("Browse");
        browseInputBtn.setFont(font);
        browseInputBtn.setBounds(600, 50, 120, 30);
        browseInputBtn.addActionListener(e -> browseFile(inputFileField));
        panel.add(browseInputBtn);

        JLabel optabLabel = new JLabel("Optab File:");
        optabLabel.setFont(font);
        optabLabel.setBounds(50, 100, 150, 30);
        panel.add(optabLabel);

        optabFileField = new JTextField(30);
        optabFileField.setBounds(180, 100, 400, 30);
        panel.add(optabFileField);

        JButton browseOptabBtn = new JButton("Browse");
        browseOptabBtn.setFont(font);
        browseOptabBtn.setBounds(600, 100, 120, 30);
        browseOptabBtn.addActionListener(e -> browseFile(optabFileField));
        panel.add(browseOptabBtn);

        // Assemble button
        assembleBtn = new JButton("Assemble");
        assembleBtn.setFont(new Font("Arial", Font.BOLD, 16));
        assembleBtn.setBackground(buttonColor);
        assembleBtn.setForeground(Color.WHITE);
        assembleBtn.setBounds(300, 150, 200, 40);
        assembleBtn.addActionListener(e -> runAssembler());
        panel.add(assembleBtn);

        JLabel intermediateLabel = new JLabel("Intermediate File Output:");
        intermediateLabel.setFont(font);
        intermediateLabel.setBounds(800, 50, 250, 30);
        panel.add(intermediateLabel);

        JLabel symtabLabel = new JLabel("SymTab Output:");
        symtabLabel.setFont(font);
        symtabLabel.setBounds(1250, 50, 250, 30);
        panel.add(symtabLabel);

        intermediateArea = new JTextArea(8, 70);
        intermediateArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane intermediateScroll = new JScrollPane(intermediateArea);
        intermediateScroll.setBounds(800, 80, 375, 600);
        panel.add(intermediateScroll);

        symtabArea = new JTextArea(8, 70);
        symtabArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane symtabScroll = new JScrollPane(symtabArea);
        symtabScroll.setBounds(1250, 80, 375, 600);
        panel.add(symtabScroll);

        JLabel outputLabel = new JLabel("Object Code Output:");
        outputLabel.setFont(font);
        outputLabel.setBounds(800, 700, 250, 30);
        panel.add(outputLabel);

        outputArea = new JTextArea(7, 70);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBounds(800, 740, 750, 200);
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

                    displayFileContent("intermediate.txt", intermediateArea);
                    displayFileContent("symtab.txt", symtabArea);
                    displayFileContent("output.txt", outputArea);

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

// The Assembler class that will run in the background
class TwoPassAssembler {

    private String inputFile;
    private String optabFile;
    private Map<String, String> optab = new HashMap<>();
    private Map<String, Integer> symtab = new HashMap<>();
    private int locctr = 0;
    private int start = 0;
    private int length = 0;

    public TwoPassAssembler(String inputFile, String optabFile) {
        this.inputFile = inputFile;
        this.optabFile = optabFile;
    }

    // Load the opcode table
    public void loadOptab() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(optabFile));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split("\\s+");
            optab.put(parts[0], parts[1]);
        }
        reader.close();
    }

    // First pass: generate symbol table and intermediate file
    public void passOne() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        BufferedWriter intermediateWriter = new BufferedWriter(new FileWriter("intermediate.txt"));
        BufferedWriter symtabWriter = new BufferedWriter(new FileWriter("symtab.txt"));

        String line = reader.readLine();
        String[] parts = line.split("\\s+");

        // Check if the first line has "START"
        if (parts[1].equals("START")) {
            start = Integer.parseInt(parts[2]);
            locctr = start;
            intermediateWriter.write("\t" + parts[0] + "\t" + parts[1] + "\t" + parts[2] + "\n");
            line = reader.readLine();
        } else {
            locctr = 0;
        }

        while (line != null) {
            parts = line.split("\\s+");
            if (parts[1].equals("END")) break;

            intermediateWriter.write(locctr + "\t" + parts[0] + "\t" + parts[1] + "\t" + parts[2] + "\n");

            if (!parts[0].equals("-")) {
                symtab.put(parts[0], locctr);
                symtabWriter.write(parts[0] + "\t" + locctr + "\n");
            }

            if (optab.containsKey(parts[1])) {
                locctr += 3;
            } else if (parts[1].equals("WORD")) {
                locctr += 3;
            } else if (parts[1].equals("RESW")) {
                locctr += 3 * Integer.parseInt(parts[2]);
            } else if (parts[1].equals("RESB")) {
                locctr += Integer.parseInt(parts[2]);
            } else if (parts[1].equals("BYTE")) {
                locctr += parts[2].length() - 3;
            }

            line = reader.readLine();
        }

        intermediateWriter.write(locctr + "\t" + parts[0] + "\t" + parts[1] + "\t" + parts[2] + "\n");
        symtabWriter.write(parts[0] + "\t" + locctr + "\n");

        reader.close();
        intermediateWriter.close();
        symtabWriter.close();
    }

    // Second pass: generate object code
    public void passTwo() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        BufferedWriter outputWriter = new BufferedWriter(new FileWriter("output.txt"));

        String line = reader.readLine();
        String[] parts = line.split("\\s+");

        // Skip the first line
        line = reader.readLine();

        while (line != null) {
            parts = line.split("\\s+");
            if (parts[1].equals("END")) break;

            StringBuilder objectCode = new StringBuilder();
            if (optab.containsKey(parts[1])) {
                objectCode.append(optab.get(parts[1])).append("\t");
                if (!parts[0].equals("-")) {
                    objectCode.append(symtab.get(parts[0])).append("\t");
                } else {
                    objectCode.append("0\t");
                }
            }

            outputWriter.write(objectCode.toString().trim() + "\n");
            line = reader.readLine();
        }

        reader.close();
        outputWriter.close();
    }
}

// Main class to run the application
class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TwoPassAssemblerGUI());
    }
}
