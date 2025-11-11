// src/main/java/meva/gui/InputPanel.java

package meva.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * 사용자 입력을 받는 패널
 * 재료 물성값, 시편 치수 등을 입력받음
 * GUI 설계 문서에 따른 완전한 구현
 * 
 * @author MEVA 개발팀
 * @version 1.0
 */
public class InputPanel extends JPanel {
    
    // 하위 패널들
    private JPanel materialPropertiesPanel;
    private JPanel specimenDimensionsPanel;
    private JPanel controlButtonsPanel;
    private JPanel presetManagementPanel;
    
    // 재료 물성 입력 필드들
    private JTextField youngModulusField;
    private JTextField yieldStrengthField;
    private JTextField strengthCoefficientField;
    private JTextField hardeningExponentField;
    
    // 시편 치수 입력 필드들
    private JTextField widthField;
    private JTextField thicknessField;
    private JTextField lengthField;
    private JTextField gaugeLengthField;
    
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
        // 재료 물성 패널 초기화
        materialPropertiesPanel = createMaterialPropertiesPanel();
        
        // 시편 치수 패널 초기화
        specimenDimensionsPanel = createSpecimenDimensionsPanel();
        
        // 제어 버튼 패널 초기화
        controlButtonsPanel = createControlButtonsPanel();
        
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
        
        // 패널들 추가
        add(materialPropertiesPanel);
        add(Box.createRigidArea(new Dimension(0, 20)));
        
        add(specimenDimensionsPanel);
        add(Box.createRigidArea(new Dimension(0, 20)));
        
        add(controlButtonsPanel);
        add(Box.createRigidArea(new Dimension(0, 20)));
        
        add(presetManagementPanel);
    }
    
    /**
     * 재료 물성 패널 생성
     */
    private JPanel createMaterialPropertiesPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Material Properties"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 영계수율 (E)
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("영계수율 (E):"), gbc);
        gbc.gridx = 1;
        youngModulusField = new JTextField("200.0", 10);
        youngModulusField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(youngModulusField, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("GPa"), gbc);
        
        // 항복강도 (σy)
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("항복강도 (σy):"), gbc);
        gbc.gridx = 1;
        yieldStrengthField = new JTextField("250.0", 10);
        yieldStrengthField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(yieldStrengthField, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("MPa"), gbc);
        
        // 강도계수 (K)
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("강도계수 (K):"), gbc);
        gbc.gridx = 1;
        strengthCoefficientField = new JTextField("500.0", 10);
        strengthCoefficientField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(strengthCoefficientField, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("MPa"), gbc);
        
        // 경화지수 (n)
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("경화지수 (n):"), gbc);
        gbc.gridx = 1;
        hardeningExponentField = new JTextField("0.2", 10);
        hardeningExponentField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(hardeningExponentField, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("-"), gbc);
        
        return panel;
    }
    
    /**
     * 시편 치수 패널 생성
     */
    private JPanel createSpecimenDimensionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Specimen Dimensions"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // 넓이 (W)
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("넓이 (W):"), gbc);
        gbc.gridx = 1;
        widthField = new JTextField("10.0", 10);
        widthField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(widthField, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("mm"), gbc);
        
        // 두께 (t)
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("두께 (t):"), gbc);
        gbc.gridx = 1;
        thicknessField = new JTextField("5.0", 10);
        thicknessField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(thicknessField, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("mm"), gbc);
        
        // 길이 (L)
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("길이 (L):"), gbc);
        gbc.gridx = 1;
        lengthField = new JTextField("50.0", 10);
        lengthField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(lengthField, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("mm"), gbc);
        
        // 게이지길이
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("게이지길이:"), gbc);
        gbc.gridx = 1;
        gaugeLengthField = new JTextField("25.0", 10);
        gaugeLengthField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(gaugeLengthField, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("mm"), gbc);
        
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
        calculateButton.setBackground(new Color(33, 150, 243)); // Primary 색상
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
        String[] presets = {"Steel (Default)", "Aluminum", "Copper", "Custom"};
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
    
    public double getYoungModulus() {
        try {
            return Double.parseDouble(youngModulusField.getText());
        } catch (NumberFormatException e) {
            return 200.0;
        }
    }
    
    public double getYieldStrength() {
        try {
            return Double.parseDouble(yieldStrengthField.getText());
        } catch (NumberFormatException e) {
            return 250.0;
        }
    }
    
    public double getStrengthCoefficient() {
        try {
            return Double.parseDouble(strengthCoefficientField.getText());
        } catch (NumberFormatException e) {
            return 500.0;
        }
    }
    
    public double getHardeningExponent() {
        try {
            return Double.parseDouble(hardeningExponentField.getText());
        } catch (NumberFormatException e) {
            return 0.2;
        }
    }
    
    public double getSpecimenWidth() {
        try {
            return Double.parseDouble(widthField.getText());
        } catch (NumberFormatException e) {
            return 10.0;
        }
    }
    
    public double getThickness() {
        try {
            return Double.parseDouble(thicknessField.getText());
        } catch (NumberFormatException e) {
            return 5.0;
        }
    }
    
    public double getLength() {
        try {
            return Double.parseDouble(lengthField.getText());
        } catch (NumberFormatException e) {
            return 50.0;
        }
    }
    
    public double getGaugeLength() {
        try {
            return Double.parseDouble(gaugeLengthField.getText());
        } catch (NumberFormatException e) {
            return 25.0;
        }
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