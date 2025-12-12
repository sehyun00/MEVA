package meva.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;
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
    private JButton saveExperimentButton;

    // 인장속도 필드
    private JTextField testSpeedField;
    private JComboBox<String> testSpeedUnitCombo;

    // 이벤트 리스너
    private ActionListener calculateListener;
    private ActionListener resetListener;
    private ActionListener saveExperimentListener;

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
        gbc.insets = new Insets(2, 2, 2, 2); // 간격 축소
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
        panel.setBorder(BorderFactory.createTitledBorder("시편 정보"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2); // 간격 축소

        // Diameter
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.4;
        panel.add(new JLabel("D₀ (직경):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        diameterField = new JTextField("", 6); // 열수 축소
        panel.add(diameterField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.1;
        panel.add(new JLabel("mm"), gbc);

        // Gauge Length
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.4;
        panel.add(new JLabel("L₀ (게이지 길이):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        gaugeLengthField = new JTextField("", 6);
        panel.add(gaugeLengthField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.1;
        panel.add(new JLabel("mm"), gbc);

        // Initial Area (Auto-calculated)
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.4;
        panel.add(new JLabel("A₀ (단면적):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        initialAreaField = new JTextField("", 6);
        initialAreaField.setEditable(false);
        initialAreaField.setBackground(new Color(240, 240, 240));
        panel.add(initialAreaField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.1;
        panel.add(new JLabel("mm²"), gbc);

        // Final Area
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.4;
        panel.add(new JLabel("Af (최종 단면적):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        finalAreaField = new JTextField("", 6);
        finalAreaField.setToolTipText("파단 후 단면적 (단면 감소율 계산용)");
        panel.add(finalAreaField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.1;
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
                    initialAreaField.setText(String.format("%.2f", area));
                } catch (NumberFormatException ex) {
                    initialAreaField.setText("");
                }
            }
        });

        // Test Speed (인장속도)
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.4;
        panel.add(new JLabel("인장속도:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        testSpeedField = new JTextField("", 6);
        testSpeedField.setToolTipText("시험 속도를 입력하세요");
        panel.add(testSpeedField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.1;
        testSpeedUnitCombo = new JComboBox<>(new String[] { "mm/s", "mm/m" });
        testSpeedUnitCombo.setToolTipText("mm/sec 또는 mm/min");
        panel.add(testSpeedUnitCombo, gbc);

        return panel;
    }

    private JPanel createFileUploadPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("파일 (드래그앤드롭 가능)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);

        // 버튼 (가운데 정렬)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        loadFileButton = new JButton("파일 선택...");
        loadFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text Files (*.txt)", "txt"));
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                setSelectedFile(fileChooser.getSelectedFile());
            }
        });
        panel.add(loadFileButton, gbc);

        // 파일명 (버튼 아래)
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        filePathLabel = new JLabel("선택되지 않음", SwingConstants.CENTER);
        filePathLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
        filePathLabel.setForeground(Color.GRAY);
        panel.add(filePathLabel, gbc);

        // 드래그앤드롭 설정
        new DropTarget(panel, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent event) {
                try {
                    event.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) event.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        File file = files.get(0);
                        if (file.getName().toLowerCase().endsWith(".txt")) {
                            setSelectedFile(file);
                        } else {
                            JOptionPane.showMessageDialog(panel,
                                    "TXT 파일만 지원됩니다.",
                                    "파일 형식 오류",
                                    JOptionPane.WARNING_MESSAGE);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return panel;
    }

    private void setSelectedFile(File file) {
        selectedFilePath = file.getAbsolutePath();
        filePathLabel.setText("📄 " + file.getName());
        filePathLabel.setForeground(new Color(0, 0, 139));
    }

    private JPanel createControlButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));

        resetButton = new JButton("초기화");
        resetButton.setPreferredSize(new Dimension(70, 30));
        resetButton.addActionListener(e -> {
            clearInputs();
            if (resetListener != null)
                resetListener.actionPerformed(e);
        });

        saveExperimentButton = new JButton("💾 저장");
        saveExperimentButton.setPreferredSize(new Dimension(80, 30));
        saveExperimentButton.setToolTipText("현재 실험 데이터를 데이터베이스에 저장합니다");
        saveExperimentButton.addActionListener(e -> {
            if (saveExperimentListener != null) {
                saveExperimentListener.actionPerformed(e);
            }
        });

        calculateButton = new JButton("계산");
        calculateButton.setPreferredSize(new Dimension(90, 30)); // 가로로 길게
        calculateButton.addActionListener(e -> {
            if (validateInputs() && calculateListener != null) {
                calculateListener.actionPerformed(e);
            }
        });

        panel.add(resetButton);
        panel.add(saveExperimentButton);
        panel.add(calculateButton);
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
     * 인장속도를 mm/sec 단위로 반환
     */
    public Double getTestSpeed() {
        try {
            String text = testSpeedField.getText();
            if (text == null || text.isEmpty()) {
                return null;
            }
            double value = Double.parseDouble(text);
            // mm/min 선택 시 mm/sec로 변환
            if (testSpeedUnitCombo.getSelectedIndex() == 1) {
                value = value / 60.0;
            }
            return value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void setSaveExperimentListener(ActionListener listener) {
        this.saveExperimentListener = listener;
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
        testSpeedField.setText("");
        testSpeedUnitCombo.setSelectedIndex(0);
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
