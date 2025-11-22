// src/main/java/meva/gui/InputPanel.java

package meva.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * 사용자 입력을 받는 패널
 * 재료 물성값, 시편 치수 등을 입력받음
 * GUI 설계 문서에 따른 완전한 구현
 * 봉재(Round Bar) 시편용으로 수정됨
 *
 * @author MEVA 개발팀
 * @version 1.1
 */
public class InputPanel extends JPanel {

    // 하위 패널들
    // private JPanel materialPropertiesPanel;  // 봉재 시편에서는 미사용
    private JPanel specimenDimensionsPanel;
    private JPanel controlButtonsPanel;
    private JPanel fileUploadPanel;
    private JPanel presetManagementPanel;

    // 재료 물성 입력 필드들 (봉재 시편에서는 미사용 - 실험 데이터 기반)
    // private JTextField youngModulusField;
    // private JTextField yieldStrengthField;
    // private JTextField strengthCoefficientField;
    // private JTextField hardeningExponentField;

    // 시편 치수 입력 필드들 (봉재용)
    private JTextField diameterField;  // 초기 직경 (D₀)
    private JTextField gaugeLengthField;  // 초기 게이지 길이 (L₀)

        // 데이터 파일 업로드 컴포넌트
    private JButton loadFileButton;
    private JLabel filePathLabel;
    private String selectedFilePath;

    // 제어 버튼들
    private JButton calculateButton;
    private JButton resetButton;
    private JButton clearGraphButton;

    // 프리셋 관리 컴포넌트
    private JComboBox<String> presetComboBox;
    private JButton savePresetButton;
    private JButton deletePresetButton;

    // 이벤트 리스너들
    private ActionListener calculateListener;
    private ActionListener resetListener;
    private ActionListener clearGraphListener;
    private ActionListener presetChangedListener;
    private ActionListener savePresetListener;
    private ActionListener deletePresetListener;

    /**
     * InputPanel 생성자
     */
    public InputPanel() {
        initializeComponents();
        setupLayout();
    }

    /**
     * 모든 컴포넌트 초기화
     */
    private void initializeComponents() {
        // 재료 물성 패널 초기화 (봉재 시편에서는 주석 처리)
        // materialPropertiesPanel = createMaterialPropertiesPanel();

        // 시편 치수 패널 초기화
        specimenDimensionsPanel = createSpecimenDimensionsPanel();

        // 제어 버튼 패널 초기화
        controlButtonsPanel = createControlButtonsPanel();


                // 데이터 파일 업로드 초기화
                fileUploadPanel = createFileUploadPanel();
        // 프리셋 관리 패널 초기화
        presetManagementPanel = createPresetManagementPanel();
    }

    /**
     * 레이아웃 설정
     */
    private void setupLayout() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(300, 0));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 패널들 추가 (Material Properties 패널 제외)
        // add(materialPropertiesPanel);
        // add(Box.createRigidArea(new Dimension(0, 20)));

        add(specimenDimensionsPanel);
        add(Box.createRigidArea(new Dimension(0, 20)));

                add(fileUploadPanel);
                add(Box.createRigidArea(new Dimension(0, 20)));

        add(controlButtonsPanel);
        add(Box.createRigidArea(new Dimension(0, 20)));

        add(presetManagementPanel);
    }

    /**
     * 시편 치수 패널 생성 (봉재용)
     */
    private JPanel createSpecimenDimensionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Specimen Dimensions (Round Bar)"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 초기 직경 (D₀)
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("초기 직경 (D₀):"), gbc);
        gbc.gridx = 1;
        diameterField = new JTextField("10.0", 10);
        diameterField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(diameterField, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("mm"), gbc);

        // 초기 게이지 길이 (L₀)
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("초기 게이지 길이 (L₀):"), gbc);
        gbc.gridx = 1;
        gaugeLengthField = new JTextField("50.0", 10);
        gaugeLengthField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(gaugeLengthField, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("mm"), gbc);

        return panel;
    }

        /**
     * 데이터 파일 업로드 패널 생성
     */
    private JPanel createFileUploadPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("데이터 파일 업로드"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 파일 선택 버튼
        gbc.gridx = 0; gbc.gridy = 0;
        loadFileButton = new JButton("파일 선택...");
        loadFileButton.setPreferredSize(new Dimension(120, 30));
        loadFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Text Files (*.txt)", "txt"
            ));
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFilePath = fileChooser.getSelectedFile().getAbsolutePath();
                filePathLabel.setText("파일: " + fileChooser.getSelectedFile().getName());
            }
        });
        panel.add(loadFileButton, gbc);

        // 파일 경로 표시 레이블
        gbc.gridx = 1; gbc.gridwidth = 2;
        filePathLabel = new JLabel("파일이 선택되지 않음");
        filePathLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
        filePathLabel.setForeground(Color.GRAY);
        panel.add(filePathLabel, gbc);

        return panel;
    }

    /**
     * 제어 버튼 패널 생성
     */
    private JPanel createControlButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        // Calculate 버튼
        calculateButton = new JButton("Calculate");
        calculateButton.setPreferredSize(new Dimension(100, 30));
        calculateButton.setBackground(new Color(33, 150, 243));  // Primary 색상
        calculateButton.setForeground(Color.WHITE);
        calculateButton.setFont(new Font("Dialog", Font.BOLD, 12));
        calculateButton.addActionListener(e -> {
            if (calculateListener != null) calculateListener.actionPerformed(e);
        });

        // Reset 버튼
        resetButton = new JButton("Reset");
        resetButton.setPreferredSize(new Dimension(100, 30));
        resetButton.addActionListener(e -> {
            if (resetListener != null) resetListener.actionPerformed(e);
        });

        // Clear Graph 버튼
        clearGraphButton = new JButton("Clear Graph");
        clearGraphButton.setPreferredSize(new Dimension(100, 30));
        clearGraphButton.addActionListener(e -> {
            if (clearGraphListener != null) clearGraphListener.actionPerformed(e);
        });

        panel.add(calculateButton);
        panel.add(resetButton);
        panel.add(clearGraphButton);

        return panel;
    }

    /**
     * 프리셋 관리 패널 생성
     */
    private JPanel createPresetManagementPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Preset Management"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 프리셋 선택
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Preset:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        String[] presets = {"Standard Round (Default)", "Custom"};
        presetComboBox = new JComboBox<>(presets);
        presetComboBox.addActionListener(e -> {
            if (presetChangedListener != null) presetChangedListener.actionPerformed(e);
        });
        panel.add(presetComboBox, gbc);

        // 저장 버튼
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        savePresetButton = new JButton("Save");
        savePresetButton.addActionListener(e -> {
            if (savePresetListener != null) savePresetListener.actionPerformed(e);
        });
        panel.add(savePresetButton, gbc);

        // 삭제 버튼
        gbc.gridx = 1;
        deletePresetButton = new JButton("Delete");
        deletePresetButton.addActionListener(e -> {
            if (deletePresetListener != null) deletePresetListener.actionPerformed(e);
        });
        panel.add(deletePresetButton, gbc);

        return panel;
    }

    // ========== 입력값 가져오기 메서드들 ==========

    /**
     * 초기 직경 (D₀) 가져오기
     */
    public double getInitialDiameter() {
        try {
            return Double.parseDouble(diameterField.getText());
        } catch (NumberFormatException e) {
            return 10.0;
        }
    }

    /**
     * 초기 게이지 길이 (L₀) 가져오기
     */
    public double getGaugeLength() {
        try {
            return Double.parseDouble(gaugeLengthField.getText());
        } catch (NumberFormatException e) {
            return 50.0;
        }
    }

        /**
             * 선택된 파일 경로 가져오기
                  */
        public String getSelectedFilePath() {
                    return selectedFilePath;
                }

    /**
     * 초기 단면적 (A₀) 계산
     */
    public double getInitialCrossSection() {
        double diameter = getInitialDiameter();
        return Math.PI * Math.pow(diameter / 2.0, 2);
    }

    public String getSelectedPreset() {
        return (String) presetComboBox.getSelectedItem();
    }

    // ========== 이벤트 리스너 설정 메서드들 ==========

    public void setCalculateListener(ActionListener listener) {
        this.calculateListener = listener;
    }

    public void setResetListener(ActionListener listener) {
        this.resetListener = listener;
    }

    public void setClearGraphListener(ActionListener listener) {
        this.clearGraphListener = listener;
    }

    public void setPresetChangedListener(ActionListener listener) {
        this.presetChangedListener = listener;
    }

    public void setSavePresetListener(ActionListener listener) {
        this.savePresetListener = listener;
    }

    public void setDeletePresetListener(ActionListener listener) {
        this.deletePresetListener = listener;
    }
}
