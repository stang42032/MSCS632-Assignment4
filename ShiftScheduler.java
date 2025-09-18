import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class ShiftScheduler {

    // -----------------------------
    // Constants
    // -----------------------------
    static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    static final String[] SHIFTS = {"Morning", "Afternoon", "Evening"};
    static final int MIN_EMP_PER_SHIFT = 2;
    static final int MAX_DAYS_PER_EMPLOYEE = 5;

    // -----------------------------
    // Data structures
    // -----------------------------
    private Map<String, Map<String, List<String>>> employees = new HashMap<>();
    private Map<String, Map<String, List<String>>> schedule = new HashMap<>();

    // GUI components
    private JFrame frame;
    private JTextField nameField;
    private Map<String, JComboBox<String>[]> prefBoxes = new HashMap<>();
    private JTextArea scheduleArea;
    private JTable summaryTable;
    private DefaultTableModel tableModel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ShiftScheduler().createAndShowGUI());
    }

    // -----------------------------
    // Create GUI
    // -----------------------------
    public void createAndShowGUI() {
        frame = new JFrame("Employee Shift Scheduler");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 1500);
        frame.setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2,2,2,2);
        gbc.anchor = GridBagConstraints.WEST;

        // Employee name input
        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("Employee Name:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 3;
        nameField = new JTextField(20);
        inputPanel.add(nameField, gbc);

        // Shift preference dropdowns per day
        prefBoxes.clear();
        int rowOffset = 1;
        for (int i = 0; i < DAYS.length; i++) {
            String day = DAYS[i];
            gbc.gridx = 0; gbc.gridy = rowOffset + i; gbc.gridwidth = 1;
            inputPanel.add(new JLabel(day), gbc);

            JComboBox<String> var1 = new JComboBox<>(SHIFTS);
            JComboBox<String> var2 = new JComboBox<>(SHIFTS);
            JComboBox<String> var3 = new JComboBox<>(SHIFTS);
            prefBoxes.put(day, new JComboBox[]{var1, var2, var3});

            gbc.gridx = 1; inputPanel.add(var1, gbc);
            gbc.gridx = 2; inputPanel.add(var2, gbc);
            gbc.gridx = 3; inputPanel.add(var3, gbc);
        }

        // Buttons
        JButton addButton = new JButton("Add Employee & Optimize Schedule");
        JButton resetButton = new JButton("Reset Scheduler");

        gbc.gridx = 0; gbc.gridy = rowOffset + DAYS.length;
        inputPanel.add(addButton, gbc);
        gbc.gridx = 1;
        inputPanel.add(resetButton, gbc);

        // Schedule output area
        scheduleArea = new JTextArea(35, 80);
        scheduleArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(scheduleArea);

        // Table to summarize total days per employee
        tableModel = new DefaultTableModel();
        tableModel.addColumn("Employee");
        tableModel.addColumn("Days Assigned");
        summaryTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(summaryTable);

        // Layout panels
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.add(scrollPane, BorderLayout.CENTER);
        outputPanel.add(tableScroll, BorderLayout.SOUTH);

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(outputPanel, BorderLayout.CENTER);

        // -----------------------------
        // Button actions
        // -----------------------------
        addButton.addActionListener(e -> addEmployeeAndOptimize());
        resetButton.addActionListener(e -> resetScheduler());

        frame.setVisible(true);
    }

    // -----------------------------
    // Add employee and re-optimize
    // -----------------------------
    private void addEmployeeAndOptimize() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter employee name.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (employees.containsKey(name)) {
            JOptionPane.showMessageDialog(frame, "Employee already added.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Map<String, List<String>> prefs = new HashMap<>();
        for (String day : DAYS) {
            JComboBox<String>[] boxes = prefBoxes.get(day);
            String s1 = (String) boxes[0].getSelectedItem();
            String s2 = (String) boxes[1].getSelectedItem();
            String s3 = (String) boxes[2].getSelectedItem();
            Set<String> unique = new HashSet<>(Arrays.asList(s1,s2,s3));
            if (unique.size() < 3) {
                JOptionPane.showMessageDialog(frame, "Please choose unique ranks for " + day, "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            prefs.put(day, Arrays.asList(s1,s2,s3));
        }

        employees.put(name, prefs);
        nameField.setText("");
        optimizeSchedule();
    }

    // -----------------------------
    // Reset Scheduler
    // -----------------------------
    private void resetScheduler() {
        employees.clear();
        schedule.clear();
        scheduleArea.setText("");
        tableModel.setRowCount(0);
        nameField.setText("");
        JOptionPane.showMessageDialog(frame, "Scheduler reset. You can add new employees now.");
    }

    // -----------------------------
    // Optimize schedule dynamically
    // -----------------------------
    private void optimizeSchedule() {
        if (employees.isEmpty()) return;

        // Initialize schedule map
        schedule.clear();
        for (String day : DAYS) {
            Map<String, List<String>> shiftMap = new HashMap<>();
            for (String shift : SHIFTS) shiftMap.put(shift, new ArrayList<>());
            schedule.put(day, shiftMap);
        }

        Map<String, Integer> daysWorked = new HashMap<>();
        for (String name : employees.keySet()) daysWorked.put(name, 0);

        // Track who is assigned each day
        Map<String, Set<String>> assignedToday = new HashMap<>();
        for (String day : DAYS) assignedToday.put(day, new HashSet<>());

        // Assign shifts by preference rank (maximize utilization)
        for (int rank = 0; rank < 3; rank++) {
            for (String day : DAYS) {
                for (String shift : SHIFTS) {
                    List<String> candidates = new ArrayList<>();
                    for (String name : employees.keySet()) {
                        if (daysWorked.get(name) < MAX_DAYS_PER_EMPLOYEE &&
                            !assignedToday.get(day).contains(name) &&
                            employees.get(name).get(day).get(rank).equals(shift)) {
                            candidates.add(name);
                        }
                    }
                    // Shuffle to randomize among candidates
                    Collections.shuffle(candidates);
                    for (String name : candidates) {
                        schedule.get(day).get(shift).add(name);
                        assignedToday.get(day).add(name);
                        daysWorked.put(name, daysWorked.get(name)+1);
                        if (schedule.get(day).get(shift).size() >= MIN_EMP_PER_SHIFT) break;
                    }
                }
            }
        }

        // Fill any shifts with fewer than MIN_EMP_PER_SHIFT randomly
        for (String day : DAYS) {
            for (String shift : SHIFTS) {
                while (schedule.get(day).get(shift).size() < MIN_EMP_PER_SHIFT) {
                    List<String> available = new ArrayList<>();
                    for (String name : employees.keySet()) {
                        if (daysWorked.get(name) < MAX_DAYS_PER_EMPLOYEE &&
                            !assignedToday.get(day).contains(name)) {
                            available.add(name);
                        }
                    }
                    if (available.isEmpty()) break;
                    String chosen = available.get(new Random().nextInt(available.size()));
                    schedule.get(day).get(shift).add(chosen);
                    assignedToday.get(day).add(chosen);
                    daysWorked.put(chosen, daysWorked.get(chosen)+1);
                }
            }
        }

        // Display schedule and summary
        showSchedule(daysWorked);
    }

    // -----------------------------
    // Display Schedule & Summary
    // -----------------------------
    private void showSchedule(Map<String, Integer> daysWorked) {
        StringBuilder sb = new StringBuilder();
        for (String day : DAYS) {
            sb.append(day).append(":\n");
            for (String shift : SHIFTS) {
                List<String> emps = schedule.get(day).get(shift);
                sb.append("  ").append(shift).append(": ").append(String.join(", ", emps)).append("\n");
            }
            sb.append("\n");
        }
        scheduleArea.setText(sb.toString());

        // Update summary table
        tableModel.setRowCount(0);
        for (String name : employees.keySet()) {
            tableModel.addRow(new Object[]{name, daysWorked.get(name)});
        }
    }
}
