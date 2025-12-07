package meva.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import meva.models.Experiment;

/**
 * 새 실험 데이터를 입력하고 파일을 업로드하는 패널
 * (InputPanel에서 분리됨)
 */
public class NewExperimentPanel extends JPanel {

    // 하위 패널들
    private JPanel specimenDimensionsPanel;
    private JPanel testConditionPanel;
    private JPanel controlButtonsPanel;
    private JPanel fileUploadPanel;

    // 실험 조건 입력 필드들
    private JTextField materialNameField;
    private JTextField testDateField;
    private JTextField testerNameField;
    private JTextField finalAreaField;
    private JTextField initialAreaField; // [New] Auto-calculated
    private JTextField remarksField;

    // 시편 치수 입력 필드들
    private JTextField diameterField;
    private JTextField gaugeLengthField;

    // 데이터 파일 업로드 컴포넌트
    private JButton loadFileButton;
    private JLabel filePathLabel;
    private String selectedFilePath;

    // 제어 버튼들
    private JButton calculateButton;
    private JButton resetButton;

    // 이벤트 리스너
    private ActionListener calculateListener;
    private ActionListener resetListener;

    public NewExperimentPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        initializeComponents();
        setupLayout();
    }

    private void initializeComponents() {
        testConditionPanel = createTestConditionPanel();
        specimenDimensionsPanel = createSpecimenDimensionsPanel();
        controlButtonsPanel = createControlButtonsPanel();
        fileUploadPanel = createFileUploadPanel();
    }

    private void setupLayout() {
        add(testConditionPanel);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(specimenDimensionsPanel);
        add(Box.createRigidArea(new Dimension(0, 20)));
        add(fileUploadPanel);
        add(Box.createRigidArea(new Dimension(0, 20)));
        add(controlButtonsPanel);
        add(Box.createRigidArea(new Dimension(0, 20)));
    }

    // --- Sub-panel Creation Methods (Moved from InputPanel) ---

    private JPanel createTestConditionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("실험 정보"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.weightx = 1.0;

        // 1. Material Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        panel.add(new JLabel("재료명:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        materialNameField = new JTextField("");
        materialNameField.setToolTipText("재료명을 입력하세요");
        panel.add(materialNameField, gbc);

        // 2. Test Date
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        panel.add(new JLabel("시험 일시:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        testDateField = new JTextField(sdf.format(new java.util.Date()));
        panel.add(testDateField, gbc);

        // 3. Tester Name
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.3;
        panel.add(new JLabel("시험자:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        testerNameField = new JTextField("");
        panel.add(testerNameField, gbc);

        // 4. Remarks
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.3;
        panel.add(new JLabel("비고:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        remarksField = new JTextField("");
        panel.add(remarksField, gbc);

        return panel;
    }

    private JPanel createSpecimenDimensionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("시편 및 단면적 정보"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Diameter
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("초기 직경 (D₀):"), gbc);
        gbc.gridx = 1;
        diameterField = new JTextField("", 10);
        panel.add(diameterField, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("mm"), gbc);

        // Gauge Length
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("초기 게이지 길이 (L₀):"), gbc);
        gbc.gridx = 1;
        gaugeLengthField = new JTextField("", 10);
        panel.add(gaugeLengthField, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("mm"), gbc);

        // Initial Area (Auto-calculated)
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("초기 단면적 (A₀):"), gbc);
        gbc.gridx = 1;
        initialAreaField = new JTextField("", 10);
        initialAreaField.setEditable(false); // Read-only
        initialAreaField.setBackground(new Color(240, 240, 240));
        panel.add(initialAreaField, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("mm²"), gbc);

        // Final Area
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("최종 단면적 (Af):"), gbc);
        gbc.gridx = 1;
        finalAreaField = new JTextField("");
        finalAreaField.setToolTipText("파단 후 단면적 입력 (단면 감소율 계산용)");
        panel.add(finalAreaField, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("mm²"), gbc);

        // Logic: Diameter 입력 시 Area 자동 계산
        diameterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                calculateArea();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                calculateArea();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                calculateArea();
            }

            private void calculateArea() {
                try {
                    String text = diameterField.getText();
                    if (text == null || text.isEmpty()) {
                        initialAreaField.setText("");
                        return;
                    }
                    double d = Double.parseDouble(text);
                    double area = Math.PI * Math.pow(d / 2.0, 2);
                    initialAreaField.setText(String.format("%.4f", area));
                } catch (NumberFormatException ex) {
                    initialAreaField.setText("");
                }
            }
        });

        return panel;
    }

    private JPanel createFileUploadPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("데이터 파일 업로드"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        loadFileButton = new JButton("파일 선택...");
        loadFileButton.setPreferredSize(new Dimension(120, 30));
        loadFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text Files (*.txt)", "txt"));
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFilePath = fileChooser.getSelectedFile().getAbsolutePath();
                filePathLabel.setText("파일: " + fileChooser.getSelectedFile().getName());
                filePathLabel.setForeground(new Color(0, 0, 139));
            }
        });
        panel.add(loadFileButton, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        filePathLabel = new JLabel("파일이 선택되지 않음");
        filePathLabel.setFont(new Font("Dialog", Font.BOLD, 12));
        filePathLabel.setForeground(Color.GRAY);
        panel.add(filePathLabel, gbc);

        return panel;
    }

    private JPanel createControlButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        calculateButton = new JButton("계산 (Calculate)");
        calculateButton.setPreferredSize(new Dimension(140, 35));
        calculateButton.addActionListener(e -> {
            if (validateInputs() && calculateListener != null) {
                calculateListener.actionPerformed(e);
            }
        });

        resetButton = new JButton("초기화 (Reset)");
        resetButton.setPreferredSize(new Dimension(120, 35));
        resetButton.addActionListener(e -> {
            clearInputs();
            if (resetListener != null)
                resetListener.actionPerformed(e);
        });

        panel.add(calculateButton);
        panel.add(resetButton);
        return panel;
    }

    private boolean validateInputs() {
        try {
            double d = Double.parseDouble(diameterField.getText());
            if (d <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "초기 직경(Diameter)은 0보다 큰 숫자여야 합니다.", "입력 오류",
                    JOptionPane.WARNING_MESSAGE);
            diameterField.requestFocus();
            return false;
        }
        try {
            double l = Double.parseDouble(gaugeLengthField.getText());
            if (l <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "게이지 길이(Gauge Length)는 0보다 큰 숫자여야 합니다.", "입력 오류",
                    JOptionPane.WARNING_MESSAGE);
            gaugeLengthField.requestFocus();
            return false;
        }
        return true;
    }

    // --- Getters & Setters ---

    public void setCalculateListener(ActionListener listener) {
        this.calculateListener = listener;
    }

    public void setResetListener(ActionListener listener) {
        this.resetListener = listener;
    }

    public double getInitialDiameter() {
        try {
            return Double.parseDouble(diameterField.getText());
        } catch (NumberFormatException e) {
            return 10.0;
        }
    }

    public double getGaugeLength() {
        try {
            return Double.parseDouble(gaugeLengthField.getText());
        } catch (NumberFormatException e) {
            return 50.0;
        }
    }

    public String getMaterialName() {
        return materialNameField.getText();
    }

    public String getTestDate() {
        return testDateField.getText();
    }

    public String getTesterName() {
        String name = testerNameField.getText();
        if (name == null || name.trim().isEmpty()) {
            return "Unknown";
        }
        return name;
    }

    public String getTestMethod() {
        return "Tensile Test";
    }

    public String getRemarks() {
        return remarksField.getText();
    }

    public Double getFinalCrossSectionArea() {
        try {
            return Double.parseDouble(finalAreaField.getText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getSelectedFilePath() {
        return selectedFilePath;
    }

    public double getInitialCrossSection() {
        double diameter = getInitialDiameter();
        return Math.PI * Math.pow(diameter / 2.0, 2);
    }

    /**
     * 입력 필드 초기화
     */
    public void clearInputs() {
        materialNameField.setText("");
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        testDateField.setText(sdf.format(new java.util.Date()));
        testerNameField.setText("");
        remarksField.setText("");
        diameterField.setText("");
        gaugeLengthField.setText("");
        finalAreaField.setText("");
        selectedFilePath = null;
        filePathLabel.setText("파일이 선택되지 않음");
        filePathLabel.setForeground(Color.GRAY);
    }

    /**
     * 외부(LoadExperimentPanel 등)에서 데이터를 주입할 때 사용
     */
    public void setExperimentData(Experiment exp) {
        if (exp == null)
            return;

        materialNameField.setText(exp.getMaterialName() != null ? exp.getMaterialName() : "");
        testDateField.setText(exp.getTestDate() != null ? exp.getTestDate() : "");
        testerNameField.setText(exp.getTesterName() != null ? exp.getTesterName() : "");
        remarksField.setText(exp.getRemarks() != null ? exp.getRemarks() : "");

        diameterField.setText(String.valueOf(exp.getSpecimenDiameter()));
        gaugeLengthField.setText(String.valueOf(exp.getGaugeLength()));

        if (exp.getFinalCrossSectionArea() != null) {
            finalAreaField.setText(String.valueOf(exp.getFinalCrossSectionArea()));
        } else {
            finalAreaField.setText("");
        }

        if (exp.getDataFilePath() != null && !exp.getDataFilePath().isEmpty()) {
            selectedFilePath = exp.getDataFilePath();
            filePathLabel.setText("파일: " + new java.io.File(exp.getDataFilePath()).getName());
            filePathLabel.setForeground(new Color(0, 0, 139));
        } else {
            selectedFilePath = null;
            filePathLabel.setText("파일이 선택되지 않음");
            filePathLabel.setForeground(Color.GRAY);
        }
    }
}
