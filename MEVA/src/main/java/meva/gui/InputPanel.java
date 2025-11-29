// src/main/java/meva/gui/InputPanel.java

package meva.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

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
    // private JPanel materialPropertiesPanel; // 봉재 시편에서는 미사용
    private JPanel materialSelectionPanel; // 재료 선택 패널
    private JPanel specimenDimensionsPanel; // 시편 치수 입력 영역을 담는 패널
    private JPanel controlButtonsPanel; // 계산, 초기화 등 제어 버튼을 담는 패널
    private JPanel fileUploadPanel; // 데이터 파일 선택 및 경로 표시 패널
    private JPanel presetManagementPanel; // 입력값 프리셋 저장/로드 관리 패널

    // 재료 물성 입력 필드들 (봉재 시편에서는 미사용 - 실험 데이터 기반)
    // private JTextField youngModulusField;
    // private JTextField yieldStrengthField;
    // private JTextField strengthCoefficientField;
    // private JTextField hardeningExponentField;

    // 재료 선택 컴포넌트
    private JComboBox<String> materialComboBox;
    private JTextField customMaterialField;
    private JPanel customMaterialPanel;
    private String selectedMaterialName;
    private int selectedMaterialId;

    // 시편 치수 입력 필드들 (봉재용)
    private JTextField diameterField; // 시편의 초기 직경 (D₀) 입력
    private JTextField gaugeLengthField; // 시편의 초기 게이지 길이 (L₀) 입력

    // 데이터 파일 업로드 컴포넌트
    private JButton loadFileButton; // 실험 데이터(.txt) 파일 선택 다이얼로그 열기
    private JLabel filePathLabel; // 선택된 파일의 이름 표시
    private String selectedFilePath; // 선택된 파일의 절대 경로 저장

    // 제어 버튼들
    private JButton calculateButton; // 입력된 데이터로 시뮬레이션 및 결과 계산 실행
    private JButton resetButton; // 모든 입력 필드 및 상태 초기화
    private JButton clearGraphButton; // 그래프 영역만 초기화

    // 프리셋 관리 컴포넌트
    private JComboBox<String> presetComboBox; // 저장된 프리셋 목록 선택
    private JButton savePresetButton; // 현재 입력값을 새 프리셋으로 저장
    private JButton deletePresetButton; // 선택된 프리셋 삭제

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

        // 재료 선택 패널 초기화
        materialSelectionPanel = createMaterialSelectionPanel();

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
        // JTabbedPane 생성
        JTabbedPane tabbedPane = new JTabbedPane();

        // Tab 1: 새 실험 (기존 패널들)
        JPanel newExperimentPanel = createNewExperimentPanel();
        tabbedPane.addTab("📂 새 실험", newExperimentPanel);

        // Tab 2: 이전 실험 불러오기
        JPanel loadExperimentPanel = createLoadExperimentPanel();
        tabbedPane.addTab("📋 이전 실험 불러오기", loadExperimentPanel);

        // 메인 레이아웃 설정
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 패널의 너비 설정
        // 의도: 초기 실행 시 최소 너비(280px)로 시작하도록 설정 (그래프 영역 확보 위함)
        setPreferredSize(new Dimension(280, 0));
        // 의도: JSplitPane 사용 시 패널이 너무 작아져서 UI가 깨지는 것을 방지하기 위한 최소 너비 설정
        setMinimumSize(new Dimension(280, 0));
    }

    /**
     * Tab 1: 새 실험 패널 생성 (기존 패널들을 포함)
     */
    private JPanel createNewExperimentPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // 기존 패널들 추가
        panel.add(materialSelectionPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        panel.add(specimenDimensionsPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        panel.add(fileUploadPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        panel.add(controlButtonsPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        panel.add(presetManagementPanel);

        return panel;
    }

    /**
     * Tab 2: 이전 실험 불러오기 패널 생성
     */
    private JPanel createLoadExperimentPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 상단 검색 및 필터 패널
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));

        // 검색 필드
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("🔍 검색:"));
        JTextField searchField = new JTextField(20);
        searchPanel.add(searchField);
        filterPanel.add(searchPanel);

        // 날짜 필터
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        datePanel.add(new JLabel("📅 기간:"));
        JTextField startDateField = new JTextField("2025-01-01", 10);
        datePanel.add(startDateField);
        datePanel.add(new JLabel("~"));
        JTextField endDateField = new JTextField("2025-12-31", 10);
        datePanel.add(endDateField);
        filterPanel.add(datePanel);

        // 재료 필터
        JPanel materialPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        materialPanel.add(new JLabel("🏷️ 재료:"));
        String[] materials = { "전체", "강재", "알루미늄", "기타" };
        JComboBox<String> materialCombo = new JComboBox<>(materials);
        materialPanel.add(materialCombo);
        filterPanel.add(materialPanel);

        panel.add(filterPanel, BorderLayout.NORTH);

        // 중앙 - 실험 목록 테이블
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("📋 저장된 실험 목록"));

        // 테이블 모델 생성
        String[] columnNames = { "ID", "재료명", "날짜", "직경(mm)", "게이지길이(mm)" };
        javax.swing.table.DefaultTableModel tableModel = new javax.swing.table.DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 셀 편집 불가
            }
        };
        JTable table = new JTable(tableModel);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(table);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(tablePanel, BorderLayout.CENTER);

        // ⭐ DB에서 실험 목록 불러오기
        loadExperimentListToTable(tableModel);

        // 하단 - 액션 버튼
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton refreshButton = new JButton("🔄 새로고침");
        refreshButton.addActionListener(e -> {
            loadExperimentListToTable(tableModel);
            JOptionPane.showMessageDialog(this, "실험 목록을 새로고침했습니다.");
        });

        JButton loadButton = new JButton("✅ 불러오기");
        loadButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "불러올 실험을 선택해주세요.", "선택 필요", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int experimentId = (int) tableModel.getValueAt(selectedRow, 0);
            loadExperimentById(experimentId);
        });

        JButton deleteButton = new JButton("🗑️ 삭제");
        deleteButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "삭제할 실험을 선택해주세요.", "선택 필요", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "선택한 실험을 삭제하시겠습니까?",
                    "삭제 확인",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                int experimentId = (int) tableModel.getValueAt(selectedRow, 0);
                deleteExperimentById(experimentId, tableModel, selectedRow);
            }
        });

        JButton searchButton = new JButton("🔍 검색");
        searchButton.addActionListener(e -> {
            String searchText = searchField.getText().trim();
            String materialCategory = (String) materialCombo.getSelectedItem();
            searchExperiments(tableModel, searchText, materialCategory);
        });

        buttonPanel.add(refreshButton);
        buttonPanel.add(searchButton);
        buttonPanel.add(loadButton);
        buttonPanel.add(deleteButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * DB에서 실험 목록을 가져와 테이블에 표시
     */
    private void loadExperimentListToTable(javax.swing.table.DefaultTableModel tableModel) {
        // 기존 데이터 삭제
        tableModel.setRowCount(0);

        // DAO를 통해 DB 조회
        meva.database.ExperimentDAO dao = new meva.database.ExperimentDAO();
        java.util.List<meva.models.Experiment> experiments = dao.getAllExperiments();

        // 테이블에 추가
        for (meva.models.Experiment exp : experiments) {
            Object[] row = {
                    exp.getId(),
                    exp.getMaterialName() != null ? exp.getMaterialName() : "Unknown",
                    exp.getTestDate(),
                    exp.getSpecimenDiameter(),
                    exp.getGaugeLength()
            };
            tableModel.addRow(row);
        }

        System.out.println("[정보] " + experiments.size() + "개의 실험 데이터 로드됨");
    }

    /**
     * ID로 실험 데이터 불러오기
     */
    private void loadExperimentById(int experimentId) {
        meva.database.ExperimentDAO dao = new meva.database.ExperimentDAO();
        meva.models.Experiment exp = dao.getExperimentById(experimentId);

        if (exp != null) {
            // Tab 1의 입력 필드에 데이터 채우기
            diameterField.setText(String.valueOf(exp.getSpecimenDiameter()));
            gaugeLengthField.setText(String.valueOf(exp.getGaugeLength()));

            // 재료 선택 컴포넌트에 데이터 채우기
            if (exp.getMaterialName() != null) {
                selectedMaterialName = exp.getMaterialName();
                selectedMaterialId = exp.getMaterialId();

                // 재료 콤보박스에 데이터 채우기
                boolean found = false;
                for (int i = 0; i < materialComboBox.getItemCount(); i++) {
                    if (materialComboBox.getItemAt(i).equals(exp.getMaterialName())) {
                        materialComboBox.setSelectedIndex(i);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    materialComboBox.setSelectedItem("➕ 추가");
                    customMaterialField.setText(exp.getMaterialName());
                }
            }

            // 데이터 파일 경로 표시
            if (exp.getDataFilePath() != null && !exp.getDataFilePath().isEmpty()) {
                selectedFilePath = exp.getDataFilePath();
                filePathLabel.setText("파일: " + new java.io.File(exp.getDataFilePath()).getName());
            }

            // Tab 1로 전환
            Component parent = this.getParent();
            while (parent != null && !(parent instanceof JTabbedPane)) {
                parent = parent.getParent();
            }
            if (parent instanceof JTabbedPane) {
                ((JTabbedPane) parent).setSelectedIndex(0); // 첫 번째 탭으로 이동
            }

            JOptionPane.showMessageDialog(this,
                    "실험 ID " + experimentId + "를 불러왔습니다.\n그래프를 생성 중입니다...",
                    "불러오기 완료",
                    JOptionPane.INFORMATION_MESSAGE);

            // SwingUtilities.invokeLater로 UI 업데이트 후 실행
            javax.swing.SwingUtilities.invokeLater(() -> {
                if (calculateListener != null) {
                    calculateListener.actionPerformed(
                            new java.awt.event.ActionEvent(calculateButton,
                                    java.awt.event.ActionEvent.ACTION_PERFORMED,
                                    "auto-calculate"));
                }
            });

        } else {
            JOptionPane.showMessageDialog(this,
                    "실험 데이터를 찾을 수 없습니다.",
                    "오류",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * ID로 실험 삭제
     */
    private void deleteExperimentById(int experimentId, javax.swing.table.DefaultTableModel tableModel, int rowIndex) {
        meva.database.ExperimentDAO dao = new meva.database.ExperimentDAO();
        boolean success = dao.deleteExperiment(experimentId);

        if (success) {
            tableModel.removeRow(rowIndex);
            JOptionPane.showMessageDialog(this,
                    "실험 ID " + experimentId + "가 삭제되었습니다.",
                    "삭제 완료",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "실험 삭제에 실패했습니다.",
                    "오류",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 검색 및 필터링
     */
    private void searchExperiments(javax.swing.table.DefaultTableModel tableModel, String searchText,
            String materialCategory) {
        tableModel.setRowCount(0);

        meva.database.ExperimentDAO dao = new meva.database.ExperimentDAO();
        java.util.List<meva.models.Experiment> experiments = dao.searchExperiments(searchText, materialCategory);

        for (meva.models.Experiment exp : experiments) {
            Object[] row = {
                    exp.getId(),
                    exp.getMaterialName() != null ? exp.getMaterialName() : "Unknown",
                    exp.getTestDate(),
                    exp.getSpecimenDiameter(),
                    exp.getGaugeLength()
            };
            tableModel.addRow(row);
        }

        System.out.println("[정보] 검색 결과: " + experiments.size() + "개");
    }

    /**
     * 재료 선택 패널 생성
     */
    private JPanel createMaterialSelectionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("🔬 재료 선택"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 재료 ComboBox
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("재료:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        String[] materials = { "S45C", "Al-Si_alloy", "Ti6Al4V", "➕ 추가" };
        materialComboBox = new JComboBox<>(materials);
        materialComboBox.setFont(new Font("Dialog", Font.PLAIN, 12));

        // 선택 이벤트
        materialComboBox.addActionListener(e -> {
            String selected = (String) materialComboBox.getSelectedItem();
            if ("➕ 추가".equals(selected)) {
                customMaterialPanel.setVisible(true);
                selectedMaterialName = null;
                selectedMaterialId = -1;
            } else {
                customMaterialPanel.setVisible(false);
                selectedMaterialName = selected;
                selectedMaterialId = getMaterialIdByName(selected);
            }
            panel.revalidate();
            panel.repaint();
        });

        panel.add(materialComboBox, gbc);

        // 사용자 정의 입력 패널
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        customMaterialPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        customMaterialPanel.setVisible(false);

        customMaterialPanel.add(new JLabel("재료명:"));
        customMaterialField = new JTextField(15);
        customMaterialField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        customMaterialPanel.add(customMaterialField);

        JButton confirmButton = new JButton("✓ 확인");
        confirmButton.setPreferredSize(new Dimension(60, 25));
        confirmButton.addActionListener(e -> {
            String customName = customMaterialField.getText().trim();
            if (!customName.isEmpty()) {
                selectedMaterialName = customName;
                selectedMaterialId = -1;

                JOptionPane.showMessageDialog(this,
                        "재료 '" + customName + "'가 설정되었습니다.",
                        "재료 설정",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "재료명을 입력해주세요.",
                        "입력 필요",
                        JOptionPane.WARNING_MESSAGE);
            }
        });
        customMaterialPanel.add(confirmButton);

        gbc.gridwidth = 3;
        panel.add(customMaterialPanel, gbc);

        // 초기값 설정
        selectedMaterialName = "S45C";
        selectedMaterialId = getMaterialIdByName("S45C");

        return panel;
    }

    /**
     * 재료명으로 DB에서 재료 ID 조회
     */
    private int getMaterialIdByName(String materialName) {
        try {
            meva.database.MaterialDAO dao = new meva.database.MaterialDAO();
            Integer id = dao.getMaterialIdByName(materialName);
            return id != null ? id : -1;
        } catch (Exception e) {
            System.err.println("재료 ID 조회 실패: " + e.getMessage());
            return -1;
        }
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
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("초기 직경 (D₀):"), gbc);
        gbc.gridx = 1;
        diameterField = new JTextField("10.0", 10);
        diameterField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(diameterField, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("mm"), gbc);

        // 초기 게이지 길이 (L₀)
        gbc.gridx = 0;
        gbc.gridy = 1;
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
        gbc.gridx = 0;
        gbc.gridy = 0;
        loadFileButton = new JButton("파일 선택...");
        loadFileButton.setPreferredSize(new Dimension(120, 30));
        loadFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Text Files (*.txt)", "txt"));
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFilePath = fileChooser.getSelectedFile().getAbsolutePath();
                filePathLabel.setText("파일: " + fileChooser.getSelectedFile().getName());
            }
        });
        panel.add(loadFileButton, gbc);

        // 파일 경로 표시 레이블
        gbc.gridx = 1;
        gbc.gridwidth = 2;
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
        calculateButton.setBackground(new Color(33, 150, 243)); // Primary 색상
        calculateButton.setForeground(Color.WHITE);
        calculateButton.setFont(new Font("Dialog", Font.BOLD, 12));
        calculateButton.addActionListener(e -> {
            if (calculateListener != null)
                calculateListener.actionPerformed(e);
        });

        // Reset 버튼
        resetButton = new JButton("Reset");
        resetButton.setPreferredSize(new Dimension(100, 30));
        resetButton.addActionListener(e -> {
            if (resetListener != null)
                resetListener.actionPerformed(e);
        });

        // Clear Graph 버튼
        clearGraphButton = new JButton("Clear Graph");
        clearGraphButton.setPreferredSize(new Dimension(100, 30));
        clearGraphButton.addActionListener(e -> {
            if (clearGraphListener != null)
                clearGraphListener.actionPerformed(e);
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
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Preset:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        String[] presets = { "Standard Round (Default)", "Custom" };
        presetComboBox = new JComboBox<>(presets);
        presetComboBox.addActionListener(e -> {
            if (presetChangedListener != null)
                presetChangedListener.actionPerformed(e);
        });
        panel.add(presetComboBox, gbc);

        // 저장 버튼
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        savePresetButton = new JButton("Save");
        savePresetButton.addActionListener(e -> {
            if (savePresetListener != null)
                savePresetListener.actionPerformed(e);
        });
        panel.add(savePresetButton, gbc);

        // 삭제 버튼
        gbc.gridx = 1;
        deletePresetButton = new JButton("Delete");
        deletePresetButton.addActionListener(e -> {
            if (deletePresetListener != null)
                deletePresetListener.actionPerformed(e);
        });
        panel.add(deletePresetButton, gbc);

        return panel;
    }

    // ========== 입력값 가져오기 메서드들 ==========

    /**
     * 선택된 재료명 가져오기
     */
    public String getSelectedMaterialName() {
        return selectedMaterialName;
    }

    /**
     * 선택된 재료 ID 가져오기
     * 
     * @return 재료 ID (-1: 새 재료, 0 이상: 기존 재료)
     */
    public int getSelectedMaterialId() {
        return selectedMaterialId;
    }

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
