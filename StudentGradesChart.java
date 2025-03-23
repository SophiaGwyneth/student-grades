import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.*;

public class StudentGradesChart extends JFrame {
    private final Map<String, int[]> gradeData = new LinkedHashMap<>();
    private final String[] GRADE_ORDER = {"A", "B", "C", "D", "F"};
    
    private final JButton exportButton;
    private final JTable summaryTable;
    private final DefaultTableModel tableModel;
    private final ChartPanel chartPanel;

    public StudentGradesChart() {
        super("Student Grades vs Internet Access");
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException ignored) {}

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLayout(new BorderLayout());

        // Top panel with buttons
        JPanel topPanel = new JPanel();
        JButton loadButton = new JButton("Load CSV");
        loadButton.setFont(new Font("Arial", Font.BOLD, 14));
        loadButton.addActionListener(_ -> chooseCSVFile());

        exportButton = new JButton("Export CSV");
        exportButton.setFont(new Font("Arial", Font.BOLD, 14));
        exportButton.setEnabled(false);
        exportButton.addActionListener(_ -> exportData());

        topPanel.add(loadButton);
        topPanel.add(exportButton);
        add(topPanel, BorderLayout.NORTH);

       // Center panel with a larger chart
chartPanel = new ChartPanel(null);
chartPanel.setPreferredSize(new Dimension(800, 450)); // Increased height
chartPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

// Bottom panel for summary table
String[] columnNames = {"Grade", "With Internet", "Without Internet"};
tableModel = new DefaultTableModel(columnNames, 0);
summaryTable = new JTable(tableModel);

// Reduce table height
JScrollPane tableScrollPane = new JScrollPane(summaryTable);
tableScrollPane.setPreferredSize(new Dimension(800, 150)); // Decreased height

// Use a Split Pane to control space distribution
JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chartPanel, tableScrollPane);
splitPane.setResizeWeight(0.75); // 75% space for chart, 25% for table
splitPane.setDividerSize(5);
splitPane.setBorder(null);

// Add splitPane to the main frame
add(splitPane, BorderLayout.CENTER);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void chooseCSVFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select CSV File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                parseCSV(selectedFile);
                buildChart();
                displaySummary();
                exportButton.setEnabled(true);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error processing CSV file:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void parseCSV(File csvFile) throws IOException {
        gradeData.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String headerLine = br.readLine();
            if (headerLine == null) throw new IOException("CSV file is empty.");

            headerLine = headerLine.replace("\uFEFF", "");
            String[] headers = headerLine.split(",");
            int gradeIdx = -1, internetIdx = -1;
            for (int i = 0; i < headers.length; i++) {
                String col = headers[i].trim().toLowerCase();
                if (col.contains("grade")) gradeIdx = i;
                if (col.contains("internet")) internetIdx = i;
            }
            if (gradeIdx == -1 || internetIdx == -1) {
                throw new IOException("CSV file missing required columns: Grade and/or Internet.");
            }

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length <= Math.max(gradeIdx, internetIdx)) continue;
                
                String grade = parts[gradeIdx].trim();
                String internet = parts[internetIdx].trim();

                if (grade.isEmpty() || internet.isEmpty()) continue;

                gradeData.putIfAbsent(grade, new int[]{0, 0});
                if ("yes".equalsIgnoreCase(internet)) {
                    gradeData.get(grade)[0]++;
                } else if ("no".equalsIgnoreCase(internet)) {
                    gradeData.get(grade)[1]++;
                }
            }
        }
    }

    private void buildChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (String grade : GRADE_ORDER) {
            if (gradeData.containsKey(grade)) {
                dataset.addValue(gradeData.get(grade)[0], "With Internet", grade);
                dataset.addValue(gradeData.get(grade)[1], "Without Internet", grade);
            }
        }

        JFreeChart barChart = ChartFactory.createStackedBarChart(
                "Student Grades vs Internet Access", "Grade", "Number of Students", dataset);

        barChart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 20));

        CategoryPlot plot = barChart.getCategoryPlot();
        StackedBarRenderer renderer = (StackedBarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);
        
        plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        plot.getDomainAxis().setLabelFont(new Font("SansSerif", Font.BOLD, 16));
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.BOLD, 16));

        chartPanel.setChart(barChart);
    }

    private void displaySummary() {
        tableModel.setRowCount(0);
        for (String grade : GRADE_ORDER) {
            if (gradeData.containsKey(grade)) {
                tableModel.addRow(new Object[]{grade, gradeData.get(grade)[0], gradeData.get(grade)[1]});
            }
        }
    }

    private void exportData() {
        StringBuilder sb = new StringBuilder("Grade,With Internet,Without Internet\n");
        for (String grade : GRADE_ORDER) {
            if (gradeData.containsKey(grade)) {
                sb.append(String.format("%s,%d,%d%n", grade, gradeData.get(grade)[0], gradeData.get(grade)[1]));
            }
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Data");
        fileChooser.setSelectedFile(new File("AggregatedData.csv"));
        int userSelection = fileChooser.showSaveDialog(this);
        
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(fileChooser.getSelectedFile()))) {
                pw.print(sb.toString());
                JOptionPane.showMessageDialog(this, "Data exported successfully.");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error exporting data: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(StudentGradesChart::new);
    }
}
