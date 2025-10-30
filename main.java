// Save as ItineraryPlanner.java
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.awt.datatransfer.StringSelection; // clipboard
import java.awt.datatransfer.Clipboard;

public class ItineraryPlanner {
    private JFrame frame;
    private JTable table;
    private DefaultTableModel model;
    private static final String[] COLS = {"Date", "Time", "City", "Activity", "Duration", "Notes"};
    private JTextField cityField;
    private JSpinner daysSpinner;

    public ItineraryPlanner() {
        frame = new JFrame("Travel Itinerary Planner ✈️");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(980, 560);
        frame.setLocationRelativeTo(null);

        // Top panel (controls)
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10,10,6,10));
        topPanel.setBackground(new Color(245, 247, 250));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6,6,6,6);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0;
        topPanel.add(new JLabel("City:"), g);

        cityField = new JTextField(18);
        g.gridx = 1; topPanel.add(cityField, g);

        g.gridx = 2; topPanel.add(new JLabel("Days:"), g);
        daysSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 14, 1));
        daysSpinner.setPreferredSize(new Dimension(60, daysSpinner.getPreferredSize().height));
        g.gridx = 3; topPanel.add(daysSpinner, g);

        JButton genBtn = new JButton("Generate Itinerary");
        styleButton(genBtn, new Color(72, 133, 237));
        g.gridx = 4; topPanel.add(genBtn, g);

        // Table model
        model = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);

        // Color the table rows and header
        table.setDefaultRenderer(Object.class, new AlternatingRowRenderer());
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(20, 90, 160));
        header.setForeground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD));

        JScrollPane scroll = new JScrollPane(table);

        // Buttons
        JButton addBtn = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");
        JButton moveUpBtn = new JButton("Move Up");
        JButton moveDownBtn = new JButton("Move Down");
        JButton saveBtn = new JButton("Save CSV");
        JButton loadBtn = new JButton("Load CSV");
        JButton exportBtn = new JButton("Export Summary");
        JButton copyBtn = new JButton("Copy Summary");

        styleButton(addBtn, new Color(60, 179, 113));
        styleButton(editBtn, new Color(255, 165, 0));
        styleButton(deleteBtn, new Color(220, 53, 69));
        styleButton(moveUpBtn, new Color(108, 117, 125));
        styleButton(moveDownBtn, new Color(108, 117, 125));
        styleButton(saveBtn, new Color(72, 133, 237));
        styleButton(loadBtn, new Color(72, 133, 237));
        styleButton(exportBtn, new Color(102, 16, 242));
        styleButton(copyBtn, new Color(23, 162, 184));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(6,10,10,10));
        btnPanel.setBackground(new Color(250,250,250));
        btnPanel.add(addBtn);
        btnPanel.add(editBtn);
        btnPanel.add(deleteBtn);
        btnPanel.add(moveUpBtn);
        btnPanel.add(moveDownBtn);
        btnPanel.add(saveBtn);
        btnPanel.add(loadBtn);
        btnPanel.add(exportBtn);
        btnPanel.add(copyBtn);

        frame.getContentPane().setLayout(new BorderLayout(8, 8));
        frame.getContentPane().add(topPanel, BorderLayout.NORTH);
        frame.getContentPane().add(scroll, BorderLayout.CENTER);
        frame.getContentPane().add(btnPanel, BorderLayout.SOUTH);

        // Button actions
        addBtn.addActionListener(e -> showItemDialog(null));
        editBtn.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r == -1) { JOptionPane.showMessageDialog(frame, "Select an item to edit."); return; }
            int modelRow = table.convertRowIndexToModel(r);
            showItemDialog(modelRow);
        });
        deleteBtn.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r == -1) { JOptionPane.showMessageDialog(frame, "Select an item to delete."); return; }
            int modelRow = table.convertRowIndexToModel(r);
            int confirm = JOptionPane.showConfirmDialog(frame, "Delete selected item?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) model.removeRow(modelRow);
        });
        moveUpBtn.addActionListener(e -> swapSelected(-1));
        moveDownBtn.addActionListener(e -> swapSelected(1));
        saveBtn.addActionListener(e -> saveCSV());
        loadBtn.addActionListener(e -> loadCSV());
        exportBtn.addActionListener(e -> exportSummary());
        copyBtn.addActionListener(e -> copySummaryToClipboard());

        genBtn.addActionListener(e -> {
            String city = cityField.getText().trim();
            int days = (Integer) daysSpinner.getValue();
            if (city.isEmpty()) { JOptionPane.showMessageDialog(frame, "Enter a city name to generate an itinerary."); return; }
            generateItineraryForCity(city, days);
        });

        // sample data
        addSampleData();

        frame.setVisible(true);
    }

    private void styleButton(JButton b, Color bg) {
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6,12,6,12));
        b.setOpaque(true);
    }

    private void addSampleData() {
        DateTimeFormatter d = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        model.addRow(new Object[]{LocalDate.now().plusDays(1).format(d), "09:00", "Barcelona", "Sagrada Familia visit", "2h", "Buy tickets online"});
        model.addRow(new Object[]{LocalDate.now().plusDays(2).format(d), "14:00", "Madrid", "Prado Museum", "3h", "Check guided tour times"});
    }

    private void swapSelected(int offset) {
        int r = table.getSelectedRow();
        if (r == -1) { JOptionPane.showMessageDialog(frame, "Select a row to move."); return; }
        int modelRow = table.convertRowIndexToModel(r);
        int target = modelRow + offset;
        if (target < 0 || target >= model.getRowCount()) return;
        Vector<?> rowData = (Vector<?>) model.getDataVector().elementAt(modelRow);
        Vector<?> targetData = (Vector<?>) model.getDataVector().elementAt(target);
        model.getDataVector().set(target, rowData);
        model.getDataVector().set(modelRow, targetData);
        model.fireTableDataChanged();
        table.setRowSelectionInterval(target, target);
    }

    private void showItemDialog(Integer editRow) {
        JDialog dialog = new JDialog(frame, (editRow == null ? "Add Item" : "Edit Item"), true);
        dialog.setSize(460, 380);
        dialog.setLocationRelativeTo(frame);
        dialog.setLayout(new BorderLayout(8,8));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6,6,6,6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0; g.gridy = 0;

        JLabel dateL = new JLabel("Date (yyyy-MM-dd):"); JTextField dateF = new JTextField();
        JLabel timeL = new JLabel("Time (HH:mm):"); JTextField timeF = new JTextField();
        JLabel cityL = new JLabel("City:"); JTextField cityF = new JTextField();
        JLabel actL = new JLabel("Activity:"); JTextField actF = new JTextField();
        JLabel durL = new JLabel("Duration (e.g. 2h):"); JTextField durF = new JTextField();
        JLabel notesL = new JLabel("Notes:"); JTextArea notesA = new JTextArea(4, 20);
        JScrollPane notesScroll = new JScrollPane(notesA);

        form.add(dateL, g); g.gridx = 1; form.add(dateF, g); g.gridx = 0; g.gridy++;
        form.add(timeL, g); g.gridx = 1; form.add(timeF, g); g.gridx = 0; g.gridy++;
        form.add(cityL, g); g.gridx = 1; form.add(cityF, g); g.gridx = 0; g.gridy++;
        form.add(actL, g); g.gridx = 1; form.add(actF, g); g.gridx = 0; g.gridy++;
        form.add(durL, g); g.gridx = 1; form.add(durF, g); g.gridx = 0; g.gridy++;
        form.add(notesL, g); g.gridx = 1; form.add(notesScroll, g);

        if (editRow != null) {
            for (int c = 0; c < model.getColumnCount(); c++) {
                Object val = model.getValueAt(editRow, c);
                switch(c) {
                    case 0: dateF.setText(val == null ? "" : val.toString()); break;
                    case 1: timeF.setText(val == null ? "" : val.toString()); break;
                    case 2: cityF.setText(val == null ? "" : val.toString()); break;
                    case 3: actF.setText(val == null ? "" : val.toString()); break;
                    case 4: durF.setText(val == null ? "" : val.toString()); break;
                    case 5: notesA.setText(val == null ? "" : val.toString()); break;
                }
            }
        } else {
            // default values
            dateF.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            timeF.setText("09:00");
        }

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        styleButton(ok, new Color(40, 167, 69));
        bottom.add(cancel); bottom.add(ok);

        ok.addActionListener(ev -> {
            String date = dateF.getText().trim();
            String time = timeF.getText().trim();
            String city = cityF.getText().trim();
            String act = actF.getText().trim();
            String dur = durF.getText().trim();
            String notes = notesA.getText().trim();

            if (date.isEmpty() || city.isEmpty() || act.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Date, City and Activity are required.");
                return;
            }
            // Basic validation for date/time
            try {
                LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
                LocalTime.parse(time, DateTimeFormatter.ofPattern("H:mm"));
            } catch (Exception ex) {
                int ans = JOptionPane.showConfirmDialog(dialog, "Date/time format looks unusual. Continue anyway?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (ans != JOptionPane.YES_OPTION) return;
            }

            if (editRow == null) {
                model.addRow(new Object[]{date, time, city, act, dur, notes});
            } else {
                model.setValueAt(date, editRow, 0);
                model.setValueAt(time, editRow, 1);
                model.setValueAt(city, editRow, 2);
                model.setValueAt(act, editRow, 3);
                model.setValueAt(dur, editRow, 4);
                model.setValueAt(notes, editRow, 5);
            }
            dialog.dispose();
        });

        cancel.addActionListener(ev -> dialog.dispose());

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(bottom, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void generateItineraryForCity(String city, int days) {
        // We'll produce 3 entries per day: Morning, Afternoon, Evening
        DateTimeFormatter d = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate start = LocalDate.now().plusDays(1); // start tomorrow for convenience

        // Optionally clear existing items? Ask behaviour: we'll append after confirming with user
        int choice = JOptionPane.showConfirmDialog(frame, "Append generated itinerary to existing items?\nChoose No to clear existing items.", "Append or Replace", JOptionPane.YES_NO_CANCEL_OPTION);
        if (choice == JOptionPane.CANCEL_OPTION) return;
        if (choice == JOptionPane.NO_OPTION) model.setRowCount(0);

        for (int day = 0; day < days; day++) {
            LocalDate date = start.plusDays(day);
            String dateStr = date.format(d);
            // Activities template - simple and generic; you can change these templates
            List<ActivityTemplate> templates = Arrays.asList(
                    new ActivityTemplate("09:00", "Morning: Explore " + city + " landmarks", "3h", "Start early to avoid crowds"),
                    new ActivityTemplate("13:00", "Afternoon: Local food & market in " + city, "2h", "Try recommended local dishes"),
                    new ActivityTemplate("18:30", "Evening: Relax / nightlife / sunset views in " + city, "2h", "Great time for photos")
            );
            for (ActivityTemplate t : templates) {
                model.addRow(new Object[]{dateStr, t.time, city, t.activity, t.duration, t.notes});
            }
        }
    }

    private static class ActivityTemplate {
        String time, activity, duration, notes;
        ActivityTemplate(String time, String activity, String duration, String notes) {
            this.time = time; this.activity = activity; this.duration = duration; this.notes = notes;
        }
    }

    private void saveCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("itinerary.csv"));
        int r = fc.showSaveDialog(frame);
        if (r != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(f.toPath()))) {
            // header
            pw.println(String.join(",", COLS));
            for (int i = 0; i < model.getRowCount(); i++) {
                List<String> cells = new ArrayList<>();
                for (int c = 0; c < model.getColumnCount(); c++) {
                    String s = Objects.toString(model.getValueAt(i, c), "");
                    s = s.replace("\"", "\"\""); // simple escape
                    if (s.contains(",") || s.contains("\"") || s.contains("\n")) s = "\"" + s + "\"";
                    cells.add(s);
                }
                pw.println(String.join(",", cells));
            }
            JOptionPane.showMessageDialog(frame, "Saved to " + f.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error saving file: " + ex.getMessage());
        }
    }

    private void loadCSV() {
        JFileChooser fc = new JFileChooser();
        int r = fc.showOpenDialog(frame);
        if (r != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try (BufferedReader br = Files.newBufferedReader(f.toPath())) {
            String header = br.readLine(); // ignore or validate
            model.setRowCount(0);
            String line;
            while ((line = br.readLine()) != null) {
                List<String> parts = parseCSVLine(line);
                while (parts.size() < COLS.length) parts.add("");
                model.addRow(parts.subList(0, COLS.length).toArray());
            }
            JOptionPane.showMessageDialog(frame, "Loaded " + f.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error loading file: " + ex.getMessage());
        }
    }

    private List<String> parseCSVLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else inQuotes = false;
                } else cur.append(ch);
            } else {
                if (ch == '"') inQuotes = true;
                else if (ch == ',') { out.add(cur.toString()); cur.setLength(0); }
                else cur.append(ch);
            }
        }
        out.add(cur.toString());
        return out;
    }

    private void exportSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Travel Itinerary Summary\n\n");
        for (int i = 0; i < model.getRowCount(); i++) {
            sb.append(String.format("%d. %s %s — %s (%s)\n    Notes: %s\n\n",
                    i + 1,
                    model.getValueAt(i, 0),
                    model.getValueAt(i, 1),
                    model.getValueAt(i, 3),
                    model.getValueAt(i, 2),
                    model.getValueAt(i, 5)
            ));
        }
        JTextArea ta = new JTextArea(sb.toString());
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(700, 420));
        int choice = JOptionPane.showOptionDialog(frame, sp, "Export Summary",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new String[]{"Save as TXT", "Copy to Clipboard", "Close"}, "Close");

        if (choice == 0) {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("itinerary_summary.txt"));
            if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                try {
                    Files.write(fc.getSelectedFile().toPath(), sb.toString().getBytes());
                    JOptionPane.showMessageDialog(frame, "Saved summary.");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Error saving: " + ex.getMessage());
                }
            }
        } else if (choice == 1) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
            JOptionPane.showMessageDialog(frame, "Copied to clipboard.");
        }
    }

    private void copySummaryToClipboard() {
        StringBuilder sb = new StringBuilder();
        sb.append("Travel Itinerary Summary\n\n");
        for (int i = 0; i < model.getRowCount(); i++) {
            sb.append(String.format("%d. %s %s — %s (%s)\n    Notes: %s\n\n",
                    i + 1,
                    model.getValueAt(i, 0),
                    model.getValueAt(i, 1),
                    model.getValueAt(i, 3),
                    model.getValueAt(i, 2),
                    model.getValueAt(i, 5)
            ));
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
        JOptionPane.showMessageDialog(frame, "Copied itinerary summary to clipboard.");
    }

    // Custom renderer for alternating row colors and selection
    private static class AlternatingRowRenderer extends DefaultTableCellRenderer {
        private static final Color EVEN = new Color(250, 250, 253);
        private static final Color ODD = new Color(238, 246, 255);
        private static final Color SELECT_BG = new Color(204, 229, 255);
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            if (isSelected) {
                c.setBackground(SELECT_BG);
            } else {
                c.setBackground((row % 2 == 0) ? EVEN : ODD);
            }
            setBorder(noFocusBorder);
            return c;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ItineraryPlanner::new);
    }
}
Added main Java code
