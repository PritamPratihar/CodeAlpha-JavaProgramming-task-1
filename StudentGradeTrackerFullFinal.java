/*
 StudentGradeTrackerFullFinal.java
 Single-file Swing application combining:
  - Add / Update (by ID or Name) / Delete students
  - Main students table with scrollbar (master list)
  - Search by ID/Name (results shown in search table)
  - Get Report (results shown in report table; summary shown next to it)
  - Zoom In/Out (Ctrl + '+' / Ctrl + '-') — scales fonts/components
  - Export (download) single student OR all students + summary to:
      CSV, TXT, PNG (screenshot of table), JPG
    PDF export attempted only if Apache PDFBox is on classpath — otherwise disabled with message.
 Notes:
  - IDs start at 101
  - Everything in one file. No external GUI frameworks used.
  - For PDF support: add PDFBox jars (optional). See compile/run notes below.
*/

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import java.lang.reflect.Method;

public class StudentGradeTrackerFullFinal extends JFrame {
    // student model
    static class Student {
        int id;
        String name;
        double score;

        Student(int id, String name, double score) {
            this.id = id;
            this.name = name;
            this.score = score;
        }
    }

    // data
    private final List<Student> students = new ArrayList<>();
    private int nextId = 101;

    // UI components
    private final DefaultTableModel masterModel = new DefaultTableModel(new Object[] { "ID", "Name", "Score" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable masterTable = new JTable(masterModel);
    private final DefaultTableModel searchModel = new DefaultTableModel(new Object[] { "ID", "Name", "Score" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable searchTable = new JTable(searchModel);
    private final DefaultTableModel reportModel = new DefaultTableModel(new Object[] { "ID", "Name", "Score" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable reportTable = new JTable(reportModel);

    private final JTextField tfName = new JTextField();
    private final JTextField tfScore = new JTextField();
    private final JTextField tfSearch = new JTextField(12);
    private final JComboBox<String> cbSearchType = new JComboBox<>(new String[] { "ID", "Name" });
    private final JComboBox<String> cbAgg = new JComboBox<>(new String[] { "Average", "Highest", "Lowest" });

    private final JLabel lblSummary = new JLabel("No students yet");
    private float zoom = 1.0f;

    // PDFBox availability flag
    private final boolean pdfBoxAvailable;

    public StudentGradeTrackerFullFinal() {
        super("Student Grade Tracker - Full");
        pdfBoxAvailable = detectPDFBox();
        buildUI();
        attachHandlers();
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        // demo sample (optional) - you may remove
        addDemoData();
        refreshMaster();
    }

    private boolean detectPDFBox() {
        try {
            Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void buildUI() {
        setLayout(new BorderLayout(8, 8));
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(8, 8, 0, 8));
        JLabel title = new JLabel("Student Grade Tracker", SwingConstants.LEFT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(20, 80, 120));
        topPanel.add(title, BorderLayout.WEST);

        // Controls row (Add/Update/Delete)
        JPanel ctrl = new JPanel(new GridBagLayout());
        ctrl.setBorder(new EmptyBorder(8, 8, 8, 8));
        ctrl.setBackground(new Color(245, 250, 255));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.gridy = 0;
        g.gridx = 0;
        g.anchor = GridBagConstraints.WEST;
        ctrl.add(new JLabel("Name:"), g);
        g.gridx = 1;
        g.weightx = 0.4;
        g.fill = GridBagConstraints.HORIZONTAL;
        ctrl.add(tfName, g);

        g.gridx = 2;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        ctrl.add(new JLabel("Score (0-100):"), g);
        g.gridx = 3;
        g.weightx = 0.2;
        g.fill = GridBagConstraints.HORIZONTAL;
        ctrl.add(tfScore, g);

        JButton btnAdd = styledButton("➕ Add Student", new Color(90, 180, 90));
        JButton btnUpdate = styledButton("✏️ Update (ID/Name)", new Color(255, 200, 100));
        JButton btnDelete = styledButton("❌ Delete Selected", new Color(250, 110, 110));

        

        g.gridx = 4;
        g.weightx = 0;
        ctrl.add(btnAdd, g);
        g.gridx = 5;
        ctrl.add(btnUpdate, g);
        g.gridx = 6;
        ctrl.add(btnDelete, g);

        // Right side download (global)
        JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightTop.setBackground(new Color(245, 250, 255));
        JButton btnDownloadAll = styledButton("⬇ Download All", new Color(60, 130, 180));
        JPopupMenu downloadAllMenu = downloadMenuForAll();
        btnDownloadAll.addActionListener(e -> downloadAllMenu.show(btnDownloadAll, 0, btnDownloadAll.getHeight()));
        rightTop.add(btnDownloadAll);
        topPanel.add(ctrl, BorderLayout.CENTER);
        topPanel.add(rightTop, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Center split: left master table & summary, right search+report
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(550);

        // Left panel: master table + summary
        JPanel left = new JPanel(new BorderLayout(6, 6));
        left.setBorder(new EmptyBorder(4, 8, 8, 4));
        JScrollPane masterScroll = new JScrollPane(masterTable);
        styleTable(masterTable);
        left.add(masterScroll, BorderLayout.CENTER);

        JPanel leftSouth = new JPanel(new BorderLayout());
        leftSouth.setPreferredSize(new Dimension(250, 140));
        leftSouth.setBorder(BorderFactory.createTitledBorder("Summary"));
        lblSummary.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSummary.setBorder(new EmptyBorder(6, 6, 6, 6));
        leftSouth.add(lblSummary, BorderLayout.CENTER);
        left.add(leftSouth, BorderLayout.SOUTH);

        mainSplit.setLeftComponent(left);

        // Right panel: search area and report area + exports for selected
        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.setBorder(new EmptyBorder(4, 4, 8, 8));

        // search controls
        JPanel searchCtrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        searchCtrl.setBorder(BorderFactory.createTitledBorder("Search (results shown below)"));
        cbSearchType.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchCtrl.add(new JLabel("Search by:"));
        searchCtrl.add(cbSearchType);
        searchCtrl.add(tfSearch);
        JButton btnSearch = styledButton("🔎 Search", new Color(100, 160, 220));
        searchCtrl.add(btnSearch);
        searchCtrl.add(new JLabel("    Aggregate:"));
        searchCtrl.add(cbAgg);
        JButton btnGetReport = styledButton("📋 Get Report", new Color(100, 160, 120));
        searchCtrl.add(btnGetReport);

        // download for selected (shows popup menu)
        JButton btnDownloadOne = styledButton("⬇ Download Selected", new Color(80, 120, 200));
        JPopupMenu downloadOneMenu = downloadMenuForSelected();
        btnDownloadOne.addActionListener(e -> downloadOneMenu.show(btnDownloadOne, 0, btnDownloadOne.getHeight()));
        searchCtrl.add(btnDownloadOne);

        right.add(searchCtrl, BorderLayout.NORTH);

        // center area: search result table + report table (tabs)
        JTabbedPane tabbed = new JTabbedPane();

        JScrollPane searchScroll = new JScrollPane(searchTable);
        styleTable(searchTable);
        tabbed.addTab("Search Results", searchScroll);

        JPanel reportPanel = new JPanel(new BorderLayout(6, 6));
        JScrollPane reportScroll = new JScrollPane(reportTable);
        styleTable(reportTable);
        reportPanel.add(reportScroll, BorderLayout.CENTER);
        JTextArea reportSummaryArea = new JTextArea();
        reportSummaryArea.setEditable(false);
        reportSummaryArea.setBackground(new Color(250, 250, 250));
        reportSummaryArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        reportSummaryArea.setBorder(BorderFactory.createTitledBorder("Report Summary"));
        reportSummaryArea.setPreferredSize(new Dimension(200, 120));
        reportPanel.add(reportSummaryArea, BorderLayout.SOUTH);

        tabbed.addTab("Report", reportPanel);

        right.add(tabbed, BorderLayout.CENTER);

        mainSplit.setRightComponent(right);

        add(mainSplit, BorderLayout.CENTER);

        // bottom: instructions & zoom
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(new EmptyBorder(6, 8, 8, 8));
        JLabel instr = new JLabel("<html>Tip: Select a row in Master or Search table to Edit/Delete.<br>" +
                "Zoom: Ctrl + '+' to zoom in, Ctrl + '-' to zoom out.</html>");
        instr.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bottom.add(instr, BorderLayout.WEST);

        // zoom controls
        JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton zoomIn = styledButton("Zoom +", new Color(200, 200, 255));
        JButton zoomOut = styledButton("Zoom -", new Color(200, 200, 255));
        zoomPanel.add(zoomIn);
        zoomPanel.add(zoomOut);
        bottom.add(zoomPanel, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);

        // expose important components via action listeners
        // internal bindings will be attached later, but provide return objects for
        // local use
        // show/hide pdf option info
        if (!pdfBoxAvailable) {
            // add small hint to download menus (will show if clicked)
        }

        // action hooks created in attachHandlers()
        // wire button objects:
        btnAdd.addActionListener(e -> doAdd());
        btnUpdate.addActionListener(e -> doUpdateByLookup()); // update via lookup by id/name then modify selected
        btnDelete.addActionListener(e -> doDeleteSelected());
        btnSearch.addActionListener(e -> doSearch());
        btnGetReport.addActionListener(e -> {
            doGetReport(reportModel, reportSummaryArea);
            tabbed.setSelectedIndex(1);
        });
        zoomIn.addActionListener(e -> {
            zoom = Math.min(2.5f, zoom + 0.1f);
            applyZoom();
        });
        zoomOut.addActionListener(e -> {
            zoom = Math.max(0.6f, zoom - 0.1f);
            applyZoom();
        });

        // double click on search table loads into top fields for editing convenience
        searchTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int r = searchTable.getSelectedRow();
                    if (r >= 0) {
                        tfName.setText(String.valueOf(searchModel.getValueAt(r, 1)));
                        tfScore.setText(String.valueOf(searchModel.getValueAt(r, 2)));
                        // select master row
                        int id = (int) searchModel.getValueAt(r, 0);
                        selectMasterById(id);
                    }
                }
            }
        });

        // selecting master row fills form
        masterTable.getSelectionModel().addListSelectionListener(ev -> {
            int r = masterTable.getSelectedRow();
            if (r >= 0) {
                tfName.setText(String.valueOf(masterModel.getValueAt(r, 1)));
                tfScore.setText(String.valueOf(masterModel.getValueAt(r, 2)));
            }
        });
    }

    // create popup menu for download of single selected
    private JPopupMenu downloadMenuForSelected() {
        JPopupMenu m = new JPopupMenu();
        m.add(menuItem("Export TXT", e -> exportSelectedAs("txt")));
        m.add(menuItem("Export CSV", e -> exportSelectedAs("csv")));
        m.add(menuItem("Export PNG (table snapshot)", e -> exportSelectedAs("png")));
        m.add(menuItem("Export JPG (table snapshot)", e -> exportSelectedAs("jpg")));
        JMenuItem pdfItem = new JMenuItem("Export PDF (if PDFBox available)");
        pdfItem.addActionListener(e -> exportSelectedAs("pdf"));
        m.add(pdfItem);
        return m;
    }

    // create popup menu for download all
    private JPopupMenu downloadMenuForAll() {
        JPopupMenu m = new JPopupMenu();
        m.add(menuItem("Export All TXT", e -> exportAllAs("txt")));
        m.add(menuItem("Export All CSV", e -> exportAllAs("csv")));
        m.add(menuItem("Export All PNG (table snapshot)", e -> exportAllAs("png")));
        m.add(menuItem("Export All JPG (table snapshot)", e -> exportAllAs("jpg")));
        JMenuItem pdfItem = new JMenuItem("Export All PDF (if PDFBox available)");
        pdfItem.addActionListener(e -> exportAllAs("pdf"));
        m.add(pdfItem);
        return m;
    }

    private JMenuItem menuItem(String text, ActionListener a) {
        JMenuItem mi = new JMenuItem(text);
        mi.addActionListener(a);
        return mi;
    }

    private JButton styledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return b;
    }

    private void attachHandlers() {
        // keyboard zoom shortcuts
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
                    if (e.getKeyCode() == KeyEvent.VK_EQUALS || e.getKeyCode() == KeyEvent.VK_PLUS) {
                        zoom = Math.min(2.5f, zoom + 0.1f);
                        applyZoom();
                        return true;
                    } else if (e.getKeyCode() == KeyEvent.VK_MINUS) {
                        zoom = Math.max(0.6f, zoom - 0.1f);
                        applyZoom();
                        return true;
                    }
                }
            }
            return false;
        });
    }

    // core actions
    private void doAdd() {
        String name = tfName.getText().trim();
        String sc = tfScore.getText().trim();
        if (name.isEmpty() || sc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter name and score.");
            return;
        }
        double score;
        try {
            score = Double.parseDouble(sc);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid score number.");
            return;
        }
        Student s = new Student(nextId++, name, score);
        students.add(s);
        masterModel.addRow(new Object[] { s.id, s.name, s.score });
        refreshSummary();
        clearForm();
    }

    // Update flow: first ask user for ID or Name to look up — then select matching
    // list and update selected
    private void doUpdateByLookup() {
        String lookup = JOptionPane.showInputDialog(this, "Enter Student ID or Name to find to update:");
        if (lookup == null)
            return;
        lookup = lookup.trim();
        if (lookup.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Empty input.");
            return;
        }

        List<Student> found = new ArrayList<>();
        try {
            int idq = Integer.parseInt(lookup);
            for (Student s : students)
                if (s.id == idq)
                    found.add(s);
        } catch (Exception ex) {
            String ql = lookup.toLowerCase();
            for (Student s : students)
                if (s.name.toLowerCase().contains(ql))
                    found.add(s);
        }

        if (found.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No student found for: " + lookup);
            return;
        }
        if (found.size() == 1) {
            Student s = found.get(0);
            // auto-fill form
            tfName.setText(s.name);
            tfScore.setText(String.valueOf(s.score));
            // select in master table
            selectMasterById(s.id);
            JOptionPane.showMessageDialog(this,
                    "Loaded student ID:" + s.id + " into form. Edit fields and press Update Selected in UI.");
            // now wait for user to select master row and press update selected button
            // (we'll provide direct update via menu below)
            // Provide immediate update option:
            int resp = JOptionPane.showConfirmDialog(this, "Do you want to update now with current form values?",
                    "Update Now", JOptionPane.YES_NO_OPTION);
            if (resp == JOptionPane.YES_OPTION) {
                // apply update to that student
                applyUpdateToStudent(s);
            }
            return;
        }

        // multiple found: show selection dialog (list)
        String[] opts = new String[found.size()];
        for (int i = 0; i < found.size(); i++) {
            opts[i] = String.format("ID:%d  Name:%s  Score:%.2f", found.get(i).id, found.get(i).name,
                    found.get(i).score);
        }
        String sel = (String) JOptionPane.showInputDialog(this, "Multiple matches. Choose one:", "Choose student",
                JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);
        if (sel == null)
            return;
        int idx = 0;
        for (int i = 0; i < opts.length; i++)
            if (opts[i].equals(sel)) {
                idx = i;
                break;
            }
        Student chosen = found.get(idx);
        tfName.setText(chosen.name);
        tfScore.setText(String.valueOf(chosen.score));
        selectMasterById(chosen.id);
        int resp = JOptionPane.showConfirmDialog(this, "Update loaded student now with current form values?",
                "Update Now", JOptionPane.YES_NO_OPTION);
        if (resp == JOptionPane.YES_OPTION)
            applyUpdateToStudent(chosen);
    }

    // apply update using master table selected row or the provided student
    // reference
    private void applyUpdateToStudent(Student s) {

        

        String name = tfName.getText().trim();
        String sc = tfScore.getText().trim();
        if (name.isEmpty() || sc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter name and score in form.");
            return;
        }
        double score;
        try {
            score = Double.parseDouble(sc);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid score.");
            return;
        }
        s.name = name;
        s.score = score;
        // update masterModel row
        for (int r = 0; r < masterModel.getRowCount(); r++) {
            if ((int) masterModel.getValueAt(r, 0) == s.id) {
                masterModel.setValueAt(s.name, r, 1);
                masterModel.setValueAt(s.score, r, 2);
                break;
            }
        }
        refreshSummary();
        JOptionPane.showMessageDialog(this, "Student updated.");
        clearForm();
    }

    // this is used by "Update Selected" button flows too
    private void doUpdateSelected() {
        int r = masterTable.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a master row to update.");
            return;
        }
        int id = (int) masterModel.getValueAt(r, 0);
        Student s = findById(id);
        if (s == null) {
            JOptionPane.showMessageDialog(this, "Selected student not found.");
            return;
        }
        applyUpdateToStudent(s);
    }

    private void doDeleteSelected() {
        int r = masterTable.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a master row to delete.");
            return;
        }
        int id = (int) masterModel.getValueAt(r, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Delete selected student ID:" + id + "?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION)
            return;
        // remove from data & model
        students.removeIf(s -> s.id == id);
        masterModel.removeRow(r);
        refreshSummary();
    }

    private void doSearch() {
        searchModel.setRowCount(0);
        String type = (String) cbSearchType.getSelectedItem();
        String q = tfSearch.getText().trim();
        if (q.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter search query.");
            return;
        }
        if (type.equals("ID")) {
            try {
                int idq = Integer.parseInt(q);
                for (Student s : students)
                    if (s.id == idq)
                        searchModel.addRow(new Object[] { s.id, s.name, s.score });
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "ID must be numeric.");
                return;
            }
        } else {
            String ql = q.toLowerCase();
            for (Student s : students)
                if (s.name.toLowerCase().contains(ql))
                    searchModel.addRow(new Object[] { s.id, s.name, s.score });
        }
        if (searchModel.getRowCount() == 0)
            JOptionPane.showMessageDialog(this, "No matches found.");
    }

    // produce a report in reportModel and summary area text
    private void doGetReport(DefaultTableModel reportModel, JTextArea summaryArea) {
        reportModel.setRowCount(0);
        String agg = (String) cbAgg.getSelectedItem();
        if (students.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No students to report.");
            return;
        }
        // For demonstration: report all students (you can modify to filter)
        for (Student s : students)
            reportModel.addRow(new Object[] { s.id, s.name, s.score });

        // summary
        double avg = students.stream().mapToDouble(st -> st.score).average().orElse(0);
        Student hi = Collections.max(students, Comparator.comparingDouble(st -> st.score));
        Student lo = Collections.min(students, Comparator.comparingDouble(st -> st.score));
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Count: %d%n", students.size()));
        sb.append(String.format("Average: %.2f%n", avg));
        sb.append(String.format("Highest: %.2f (ID:%d, %s)%n", hi.score, hi.id, hi.name));
        sb.append(String.format("Lowest: %.2f (ID:%d, %s)%n", lo.score, lo.id, lo.name));

        // also compute the selected aggregate if needed
        if (agg.equals("Average"))
            sb.append(String.format("-> Selected aggregate (Average): %.2f%n", avg));
        else if (agg.equals("Highest"))
            sb.append(String.format("-> Selected aggregate (Highest): %.2f (ID:%d, %s)%n", hi.score, hi.id, hi.name));
        else
            sb.append(String.format("-> Selected aggregate (Lowest): %.2f (ID:%d, %s)%n", lo.score, lo.id, lo.name));

        summaryArea.setText(sb.toString());
    }

    private void refreshMaster() {
        // keep masterModel synced with students
        // easiest: clear and readd
        masterModel.setRowCount(0);
        for (Student s : students)
            masterModel.addRow(new Object[] { s.id, s.name, s.score });
        // refresh summary label
        if (students.isEmpty()) {
            lblSummary.setText("No students yet");
            return;
        }
        double avg = students.stream().mapToDouble(st -> st.score).average().orElse(0);
        Student hi = Collections.max(students, Comparator.comparingDouble(st -> st.score));
        Student lo = Collections.min(students, Comparator.comparingDouble(st -> st.score));
        lblSummary.setText("<html>Number of Students: " + students.size() + "<br>Average: " + String.format("%.2f", avg)
                + "<br>Highest: " + String.format("%.2f", hi.score) + " (ID:" + hi.id + ", " + hi.name + ")"
                + "<br>Lowest: " + String.format("%.2f", lo.score) + " (ID:" + lo.id + ", " + lo.name + ")</html>");
    }

    private void clearForm() {
        tfName.setText("");
        tfScore.setText("");
    }

    private Student findById(int id) {
        for (Student s : students)
            if (s.id == id)
                return s;
        return null;
    }

    private void selectMasterById(int id) {
        for (int r = 0; r < masterModel.getRowCount(); r++) {
            if ((int) masterModel.getValueAt(r, 0) == id) {
                masterTable.setRowSelectionInterval(r, r);
                masterTable.scrollRectToVisible(masterTable.getCellRect(r, 0, true));
                break;
            }
        }
    }

    // EXPORTS
    // export selected single student: txt/csv/png/jpg/pdf(if available)
    private void exportSelectedAs(String fmt) {
        int r = masterTable.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a student in main table first.");
            return;
        }
        int id = (int) masterModel.getValueAt(r, 0);
        Student s = findById(id);
        if (s == null) {
            JOptionPane.showMessageDialog(this, "Student not found.");
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("student_" + s.id + "." + fmt));
        int rv = fc.showSaveDialog(this);
        if (rv != JFileChooser.APPROVE_OPTION)
            return;
        File f = fc.getSelectedFile();
        try {
            if (fmt.equals("txt"))
                writeSingleTxt(s, f);
            else if (fmt.equals("csv"))
                writeSingleCsv(s, f);
            else if (fmt.equals("png") || fmt.equals("jpg"))
                writeTableImage(masterTable, f, fmt);
            else if (fmt.equals("pdf")) {
                if (!pdfBoxAvailable) {
                    JOptionPane.showMessageDialog(this, "PDF export requires Apache PDFBox on classpath.");
                    return;
                }
                writeSinglePdfIfAvailable(s, f);
            } else
                JOptionPane.showMessageDialog(this, "Unknown format");
            JOptionPane.showMessageDialog(this, "Saved: " + f.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // export all students + summary
    private void exportAllAs(String fmt) {
        if (students.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No students to export.");
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("students_all." + fmt));
        int rv = fc.showSaveDialog(this);
        if (rv != JFileChooser.APPROVE_OPTION)
            return;
        File f = fc.getSelectedFile();
        try {
            if (fmt.equals("txt"))
                writeAllTxt(f);
            else if (fmt.equals("csv"))
                writeAllCsv(f);
            else if (fmt.equals("png") || fmt.equals("jpg"))
                writeTableImage(masterTable, f, fmt);
            else if (fmt.equals("pdf")) {
                if (!pdfBoxAvailable) {
                    JOptionPane.showMessageDialog(this, "PDF export requires Apache PDFBox on classpath.");
                    return;
                }
                writeAllPdfIfAvailable(f);
            } else
                JOptionPane.showMessageDialog(this, "Unknown format");
            JOptionPane.showMessageDialog(this, "Saved: " + f.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // write single student text
    private void writeSingleTxt(Student s, File f) throws IOException {
        try (FileWriter fw = new FileWriter(f)) {
            fw.write("ID: " + s.id + "\n");
            fw.write("Name: " + s.name + "\n");
            fw.write("Score: " + s.score + "\n");
        }
    }

    private void writeSingleCsv(Student s, File f) throws IOException {
        try (FileWriter fw = new FileWriter(f)) {
            fw.write("ID,Name,Score\n");
            fw.write(s.id + "," + escapeCsv(s.name) + "," + s.score + "\n");
        }
    }

    private void writeAllTxt(File f) throws IOException {
        try (FileWriter fw = new FileWriter(f)) {
            fw.write("All Students\n");
            for (Student s : students)
                fw.write(String.format("ID: %d\tName: %s\tScore: %.2f%n", s.id, s.name, s.score));
            fw.write("\nSummary:\n");
            double avg = students.stream().mapToDouble(st -> st.score).average().orElse(0);
            Student hi = Collections.max(students, Comparator.comparingDouble(st -> st.score));
            Student lo = Collections.min(students, Comparator.comparingDouble(st -> st.score));
            fw.write(String.format("Count: %d%nAverage: %.2f%nHighest: %.2f (ID:%d,%s)%nLowest: %.2f (ID:%d,%s)%n",
                    students.size(), avg, hi.score, hi.id, hi.name, lo.score, lo.id, lo.name));
        }
    }

    private void writeAllCsv(File f) throws IOException {
        try (FileWriter fw = new FileWriter(f)) {
            fw.write("ID,Name,Score\n");
            for (Student s : students)
                fw.write(s.id + "," + escapeCsv(s.name) + "," + s.score + "\n");
            // add summary as commented lines
            fw.write("# Summary\n");
            double avg = students.stream().mapToDouble(st -> st.score).average().orElse(0);
            Student hi = Collections.max(students, Comparator.comparingDouble(st -> st.score));
            Student lo = Collections.min(students, Comparator.comparingDouble(st -> st.score));
            fw.write("# Count," + students.size() + "\n");
            fw.write("# Average," + String.format("%.2f", avg) + "\n");
            fw.write("# Highest," + hi.score + ",ID:" + hi.id + ",Name:" + escapeCsv(hi.name) + "\n");
            fw.write("# Lowest," + lo.score + ",ID:" + lo.id + ",Name:" + escapeCsv(lo.name) + "\n");
        }
    }

    private String escapeCsv(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    // write image of table component
    private void writeTableImage(JTable t, File f, String fmt) throws Exception {
        int w = Math.max(600, t.getWidth());
        int h = Math.max(300, t.getHeight());
        // render table to image
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);
        // optionally increase table size for clear snapshot
        t.setSize(w, h);
        t.printAll(g2);
        g2.dispose();
        ImageIO.write(img, fmt.equals("jpg") ? "jpg" : "png", f);
    }

    // PDF: if PDFBox is on classpath, use reflection to avoid compile-time
    // dependency
    private void writeSinglePdfIfAvailable(Student s, File f) throws Exception {
        // using PDFBox via reflection
        // create document
        Class<?> pdDocClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
        Object doc = pdDocClass.getDeclaredConstructor().newInstance();
        Class<?> pdPageClass = Class.forName("org.apache.pdfbox.pdmodel.PDPage");
        Object page = pdPageClass.getDeclaredConstructor().newInstance();
        Method addPage = pdDocClass.getMethod("addPage", Class.forName("org.apache.pdfbox.pdmodel.PDPage"));
        addPage.invoke(doc, page);

        // create content stream and draw text
        Class<?> pdContentClass = Class.forName("org.apache.pdfbox.pdmodel.PDPageContentStream");
        Object content = pdContentClass
                .getConstructor(Class.forName("org.apache.pdfbox.pdmodel.PDDocument"),
                        Class.forName("org.apache.pdfbox.pdmodel.PDPage"))
                .newInstance(doc, page);
        // set font and write
        Class<?> pdType0 = Class.forName("org.apache.pdfbox.pdmodel.font.PDType1Font");
        Object font = pdType0.getField("HELVETICA").get(null);
        pdContentClass.getMethod("beginText").invoke(content);
        pdContentClass.getMethod("setFont", Class.forName("org.apache.pdfbox.pdmodel.font.PDFont"), float.class)
                .invoke(content, font, 12f);
        pdContentClass.getMethod("newLineAtOffset", float.class, float.class).invoke(content, 50f, 700f);
        String[] lines = new String[] { "ID: " + s.id, "Name: " + s.name, "Score: " + s.score };
        for (String ln : lines) {
            pdContentClass.getMethod("showText", String.class).invoke(content, ln);
            pdContentClass.getMethod("newLine").invoke(content);
        }
        pdContentClass.getMethod("endText").invoke(content);
        pdContentClass.getMethod("close").invoke(content);

        // save
        pdDocClass.getMethod("save", File.class).invoke(doc, f);
        pdDocClass.getMethod("close").invoke(doc);
    }

    private void writeAllPdfIfAvailable(File f) throws Exception {
        Class<?> pdDocClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
        Object doc = pdDocClass.getDeclaredConstructor().newInstance();
        Class<?> pdPageClass = Class.forName("org.apache.pdfbox.pdmodel.PDPage");
        Object page = pdPageClass.getDeclaredConstructor().newInstance();
        pdDocClass.getMethod("addPage", Class.forName("org.apache.pdfbox.pdmodel.PDPage")).invoke(doc, page);
        Class<?> pdContentClass = Class.forName("org.apache.pdfbox.pdmodel.PDPageContentStream");
        Object content = pdContentClass
                .getConstructor(Class.forName("org.apache.pdfbox.pdmodel.PDDocument"),
                        Class.forName("org.apache.pdfbox.pdmodel.PDPage"))
                .newInstance(doc, page);
        Class<?> pdType0 = Class.forName("org.apache.pdfbox.pdmodel.font.PDType1Font");
        Object font = pdType0.getField("HELVETICA").get(null);
        pdContentClass.getMethod("beginText").invoke(content);
        pdContentClass.getMethod("setFont", Class.forName("org.apache.pdfbox.pdmodel.font.PDFont"), float.class)
                .invoke(content, font, 10f);
        pdContentClass.getMethod("newLineAtOffset", float.class, float.class).invoke(content, 40f, 750f);
        for (Student s : students) {
            String line = String.format("ID:%d  Name:%s  Score:%.2f", s.id, s.name, s.score);
            pdContentClass.getMethod("showText", String.class).invoke(content, line);
            pdContentClass.getMethod("newLine").invoke(content);
        }
        // summary
        pdContentClass.getMethod("newLine").invoke(content);
        double avg = students.stream().mapToDouble(st -> st.score).average().orElse(0);
        Student hi = Collections.max(students, Comparator.comparingDouble(st -> st.score));
        Student lo = Collections.min(students, Comparator.comparingDouble(st -> st.score));
        pdContentClass.getMethod("showText", String.class).invoke(content, "Summary:");
        pdContentClass.getMethod("newLine").invoke(content);
        pdContentClass.getMethod("showText", String.class).invoke(content, "Count: " + students.size());
        pdContentClass.getMethod("newLine").invoke(content);
        pdContentClass.getMethod("showText", String.class).invoke(content, "Average: " + String.format("%.2f", avg));
        pdContentClass.getMethod("newLine").invoke(content);
        pdContentClass.getMethod("showText", String.class).invoke(content,
                "Highest: " + String.format("%.2f", hi.score) + " (ID:" + hi.id + "," + hi.name + ")");
        pdContentClass.getMethod("newLine").invoke(content);
        pdContentClass.getMethod("showText", String.class).invoke(content,
                "Lowest: " + String.format("%.2f", lo.score) + " (ID:" + lo.id + "," + lo.name + ")");

        pdContentClass.getMethod("endText").invoke(content);
        pdContentClass.getMethod("close").invoke(content);
        pdDocClass.getMethod("save", File.class).invoke(doc, f);
        pdDocClass.getMethod("close").invoke(doc);
    }

    // demo data
    private void addDemoData() {
        students.add(new Student(nextId++, "rahul", 80.0));
        students.add(new Student(nextId++, "sam", 92.0));
        students.add(new Student(nextId++, "anita", 75.0));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            StudentGradeTrackerFullFinal app = new StudentGradeTrackerFullFinal();
            app.setVisible(true);
        });
    }

    // utility: apply zoom to whole frame by scaling fonts recursively
    private void applyZoom() {
        SwingUtilities.invokeLater(() -> {
            scaleComponent(this.getContentPane(), zoom);
            revalidate();
            repaint();
        });
    }

    private void scaleComponent(Component c, float factor) {
        Font f = c.getFont();
        if (f != null) {
            float newSize = Math.max(9f, f.getSize2D() * factor);
            c.setFont(f.deriveFont(newSize));
        }
        if (c instanceof Container) {
            for (Component ch : ((Container) c).getComponents())
                scaleComponent(ch, factor);
        }
    }

    // helper: style a JTable (headers, row height, fonts, etc.)
    private void styleTable(JTable table) {
        table.setRowHeight(25);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(220, 220, 220));
        table.setShowGrid(true);

        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setReorderingAllowed(false);
            header.setFont(new Font("Segoe UI", Font.BOLD, 13));
            header.setBackground(new Color(200, 220, 240));
        }
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }

    // helper: refresh summary label (called after add/update/delete)
    private void refreshSummary() {
        if (students == null || students.isEmpty()) {
            lblSummary.setText("No students yet");
            return;
        }

        double avg = students.stream().mapToDouble(st -> st.score).average().orElse(0);
        Student hi = Collections.max(students, Comparator.comparingDouble(st -> st.score));
        Student lo = Collections.min(students, Comparator.comparingDouble(st -> st.score));

        lblSummary.setText("<html>Number of Students: " + students.size()
                + "<br>Average: " + String.format("%.2f", avg)
                + "<br>Highest: " + String.format("%.2f", hi.score) + " (ID:" + hi.id + ", " + hi.name + ")"
                + "<br>Lowest: " + String.format("%.2f", lo.score) + " (ID:" + lo.id + ", " + lo.name + ")</html>");
    }


    // helper to write a table snapshot to image (already used)
    // small utility: expose function to write master table image when required
    // (done above)

}
