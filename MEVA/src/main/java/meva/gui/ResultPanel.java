// src/main/java/meva/gui/ResultPanel.java

package meva.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 계산 결과를 표시하는 패널
 * GUI 설계 문서에 따른 완전한 구현
 * 
 * @author MEVA 개발팀
 * @version 1.0
 */
public class ResultPanel extends JPanel {

    // 결과 테이블 및 관련 컴포넌트
    private JTable resultsTable;        // 계산 결과를 표시하는 표 컴포넌트
    private DefaultTableModel tableModel; // 테이블의 데이터 모델 (행/열 관리)
    private JScrollPane scrollPane;     // 테이블에 스크롤 기능을 제공하는 컨테이너
    private JButton saveButton;         // 결과를 CSV 파일로 저장하는 버튼
    private JLabel titleLabel;          // 패널 제목 레이블 (동적 변경용)
    // 외부 리스너
    private ActionListener saveButtonListener;

    // 테이블 데이터
    private static final String[] COLUMN_NAMES = { "속성", "값", "단위" };
    private static final Object[][] INITIAL_DATA = {
            { "최대 응력 (σmax)", "-", "MPa" },
            { "최대 응력 시 변형률 (εmax)", "-", "-" },
            { "극한 인장 강도 (UTS)", "-", "MPa" },
            { "영률 (E)", "-", "GPa" },
            { "항복 강도 (0.2% Offset)", "-", "MPa" }, // 라벨 명확화
            { "연신율", "-", "%" },
            { "단면 감소율", "-", "%" },
            { "변형률 에너지 밀도 (Toughness)", "-", "MJ/m³" },
            { "레질리언스 계수 (Resilience)", "-", "MJ/m³" },
            { "탄성 한계", "-", "MPa" },
            { "비례 한계", "-", "MPa" },
            { "네킹 시작 변형률", "-", "-" },
            { "파괴 응력", "-", "MPa" },
            { "파괴 변형률", "-", "-" }
    };
    
    // 현재 데이터 상태 저장 (모드 변경 시 재계산용)
    private meva.models.AnalysisResult currentResult;
    private meva.calculation.MaterialProperties calculator = new meva.calculation.MaterialProperties();

    /**
     * ResultPanel 생성자
     * 테이블 UI를 초기화하고 레이아웃을 설정합니다.
     */
    public ResultPanel() {
        initializeComponents();
        setupLayout();
    }

    /**
     * 컴포넌트들을 초기화합니다.
     */
    private void initializeComponents() {
        // 테이블 모델 생성 (편집 불가능)
        tableModel = new DefaultTableModel(INITIAL_DATA, COLUMN_NAMES) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 모든 셀 편집 불가
            }
        };

        // 테이블 생성 및 설정
        resultsTable = new JTable(tableModel);
        resultsTable.setRowHeight(25);
        resultsTable.setFont(new Font("Malgun Gothic", Font.PLAIN, 12)); 
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 테이블 헤더 설정
        JTableHeader header = resultsTable.getTableHeader();
        header.setFont(new Font("Malgun Gothic", Font.BOLD, 12));
        header.setBackground(new Color(230, 230, 230));
        header.setReorderingAllowed(false); // 컨럼 순서 변경 불가

        // 컨럼 너비 설정
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(180); // Property
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Value
        resultsTable.getColumnModel().getColumn(2).setPreferredWidth(60); // Unit

        // 스크롤 패널 생성
        scrollPane = new JScrollPane(resultsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // Save Results 버튼 생성
        saveButton = new JButton("Save Results");
        saveButton.setPreferredSize(new Dimension(120, 35));
        saveButton.setFont(new Font("Arial", Font.BOLD, 12));
        saveButton.setBackground(new Color(76, 175, 80)); // 녹색
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.setBorderPainted(false);
        saveButton.addActionListener(e -> {
            // CSV 로 저장
            saveResultsToCSV();
            // 외부 리스너 호출 (DB 저장)
            if (saveButtonListener != null) {
                saveButtonListener.actionPerformed(e);
            }
        });
    }

    /**
     * 레이아웃을 설정합니다.
     */
    private void setupLayout() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 의도: 결과 테이블의 모든 컬럼(Property, Value, Unit)이 잘리지 않고 보이도록 최소 너비 확보
        // 컬럼 너비 합계(180+100+60 = 340) + 스크롤바 및 여백 고려
        setMinimumSize(new Dimension(300, 0));
        // 의도: 초기 실행 시 최소 너비(300px)로 시작하도록 설정
        setPreferredSize(new Dimension(300, 0));

        // 타이틀 레이블
        titleLabel = new JLabel("Results");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        add(titleLabel, BorderLayout.NORTH);

        // 테이블 (중앙)
        add(scrollPane, BorderLayout.CENTER);

        // 버튼 패널 (하단)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.add(saveButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * 결과를 CSV 파일로 저장합니다.
     */
    private void saveResultsToCSV() {
        // 결과가 비어있는지 확인
        boolean hasData = false;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String value = tableModel.getValueAt(i, 1).toString();
            if (!value.equals("-")) {
                hasData = true;
                break;
            }
        }

        if (!hasData) {
            JOptionPane.showMessageDialog(this,
                    "No results to save. Please calculate first.",
                    "No Data",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 파일 선택 다이얼로그
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Results");

        // 기본 파일명 설정
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        fileChooser.setSelectedFile(new File("MEVA_Results_" + timestamp + ".csv"));

        // CSV 파일 필터 추가
        javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter(
                "CSV files (*.csv)", "csv");
        fileChooser.setFileFilter(filter);

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            // 파일 확장자 확인 및 추가
            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
            }

            // 파일이 이미 존재하는 경우 확인
            if (fileToSave.exists()) {
                int response = JOptionPane.showConfirmDialog(this,
                        "File already exists. Do you want to overwrite it?",
                        "Confirm Overwrite",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if (response != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            // 파일 저장
            try (FileWriter writer = new FileWriter(fileToSave)) {
                // 헤더 정보 작성
                writer.write("MEVA - Materials Engineering Visualization and Analysis\n");
                writer.write("Results Export\n");
                writer.write("Date/Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
                writer.write("\n");

                // 테이블 헤더 작성
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    writer.write(tableModel.getColumnName(i));
                    if (i < tableModel.getColumnCount() - 1) {
                        writer.write(",");
                    }
                }
                writer.write("\n");

                // 테이블 데이터 작성
                for (int row = 0; row < tableModel.getRowCount(); row++) {
                    for (int col = 0; col < tableModel.getColumnCount(); col++) {
                        Object value = tableModel.getValueAt(row, col);
                        writer.write(value != null ? value.toString() : "");
                        if (col < tableModel.getColumnCount() - 1) {
                            writer.write(",");
                        }
                    }
                    writer.write("\n");
                }

                JOptionPane.showMessageDialog(this,
                        "Results saved successfully to:\n" + fileToSave.getAbsolutePath(),
                        "Save Successful",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Error saving file: " + e.getMessage(),
                        "Save Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 결과 테이블을 업데이트합니다. (AnalysisResult 객체 직접 수신)
     * 기본적으로 Engineering Stress / Triangle Resilience 모드로 표시합니다.
     */
    public void setAnalysisResult(meva.models.AnalysisResult result) {
        this.currentResult = result;
        updateMode(false, true); // 기본값: Engineering, Triangle
    }

    /**
     * 현재 모드 설정에 따라 테이블 값을 갱신합니다. (레거시 호환용)
     * 기본 항복점 모드(Auto=0)를 사용합니다.
     */
    public void updateMode(boolean isTrueStress, boolean useTriangleResilience) {
        updateMode(isTrueStress, useTriangleResilience, 0); // 0: Auto
    }

    /**
     * 현재 모드 설정에 따라 테이블 값을 갱신합니다.
     * @param isTrueStress True Stress 모드 여부
     * @param useTriangleResilience 탄성 에너지 삼각형 모드 여부
     * @param yieldMode 선택된 항복점 모드 (0:Auto, 1:Offset, 2:Upper/Lower)
     */
    public void updateMode(boolean isTrueStress, boolean useTriangleResilience, int yieldMode) {
        if (currentResult == null) {
            clearResults();
            return;
        }

        String modeText = isTrueStress ? "(True)" : "(Engineering)";
        setTitleText("Results " + modeText);

        // 1. 공통 값 (True/Eng 차이가 없거나 미미한 것들)
        updateValueByProperty("연신율", String.format("%.2f", currentResult.getElongation()));
        updateValueByProperty("단면 감소율", String.format("%.2f", currentResult.getReductionOfArea()));
        updateValueByProperty("변형률 에너지 밀도", String.format("%.3f", currentResult.getToughness()));

        // 2. 모드에 따라 달라지는 값 (Stress/Modulus)
        double E_GPa = isTrueStress ? currentResult.getYoungsModulus() : currentResult.getYoungsModulusEng();
        if (!isTrueStress && E_GPa == 0) E_GPa = currentResult.getYoungsModulus();
        
        updateValueByProperty("영률", String.format("%.2f", E_GPa));

        // UTS
        meva.models.StressStrainPoint utsPt = currentResult.getUtsPoint();
        if (utsPt != null) {
            double uts = isTrueStress ? utsPt.getTrueStress() : utsPt.getEngineeringStress();
            updateValueByProperty("극한 인장 강도", String.format("%.2f", uts));
            updateValueByProperty("최대 응력 (σmax)", String.format("%.2f", uts));
            double utsStrain = isTrueStress ? utsPt.getTrueStrain() : utsPt.getEngineeringStrain();
            updateValueByProperty("최대 응력 시 변형률", String.format("%.4f", utsStrain));
        }

        // 항복 강도 (선택된 모드에 따라 결정)
        double yieldStr = 0.0;
        boolean isDiscontinuousMode = false;

        if (yieldMode == 2) { // Upper/Lower 강제
            isDiscontinuousMode = true;
            if (currentResult.getUpperYieldPoint() != null) {
                yieldStr = isTrueStress ? currentResult.getUpperYieldPoint().getTrueStress() : currentResult.getUpperYieldPoint().getEngineeringStress();
            }
        } else if (yieldMode == 1) { // Offset 강제
            if (!isTrueStress && currentResult.getOffsetYieldPointEng() != null) {
                yieldStr = currentResult.getOffsetYieldPointEng().getEngineeringStress();
            } else if (currentResult.getOffsetYieldPoint() != null) {
                yieldStr = isTrueStress ? currentResult.getOffsetYieldPoint().getTrueStress() : currentResult.getOffsetYieldPoint().getEngineeringStress();
            }
        } else { // Auto (0)
            // Auto 모드에서는 AnalysisResult의 yieldType을 따름
            if (currentResult.getYieldType() == meva.models.AnalysisResult.YieldType.DISCONTINUOUS) {
                isDiscontinuousMode = true;
                if (currentResult.getUpperYieldPoint() != null) {
                    yieldStr = isTrueStress ? currentResult.getUpperYieldPoint().getTrueStress() : currentResult.getUpperYieldPoint().getEngineeringStress();
                }
            } else {
                // Offset Point 우선
                if (!isTrueStress && currentResult.getOffsetYieldPointEng() != null) {
                    yieldStr = currentResult.getOffsetYieldPointEng().getEngineeringStress();
                } else if (currentResult.getOffsetYieldPoint() != null) {
                    yieldStr = isTrueStress ? currentResult.getOffsetYieldPoint().getTrueStress() : currentResult.getOffsetYieldPoint().getEngineeringStress();
                } else {
                    yieldStr = currentResult.getYieldStrength(); // Fallback
                }
            }
        }
        updateValueByProperty("항복 강도", String.format("%.2f", yieldStr));

        // 3. 탄성 에너지 (Resilience) - 모드 반영
        double resilience = 0.0;
        if (useTriangleResilience) {
            // Triangle Mode: 0.5 * σ_y^2 / E
            if (E_GPa > 0) {
                resilience = (0.5 * yieldStr * yieldStr) / (E_GPa * 1000.0); // MPa단위 맞춤
            }
        } else {
            // Integral Mode: 실제 적분값 사용
            // [Fix] 화면에 표시된 항복 모드와 일치하는 적분값 사용
            if (isDiscontinuousMode) {
                resilience = currentResult.getResilienceIntegral(); // UYP 기준 (넓은 범위)
            } else {
                resilience = currentResult.getResilienceIntegralOffset(); // Offset 기준 (좁은 범위)
            }
        }
        updateValueByProperty("레질리언스 계수", String.format("%.3f", resilience));

        // 기타
        updateValueByProperty("탄성 한계", String.format("%.2f", currentResult.getElasticLimit()));
        updateValueByProperty("비례 한계", String.format("%.2f", currentResult.getProportionalLimit()));
        updateValueByProperty("파괴 응력", String.format("%.2f", currentResult.getFractureStress()));
        updateValueByProperty("파괴 변형률", String.format("%.4f", currentResult.getFractureStrain()));
        
        // 4. 네킹 시작 (UTS Strain)
        if (utsPt != null) {
             double necking = isTrueStress ? utsPt.getTrueStrain() : utsPt.getEngineeringStrain();
             updateValueByProperty("네킹 시작 변형률", String.format("%.4f", necking));
        }
    }

    /**
     * 결과 테이블을 업데이트합니다. (기존 호환성 유지)
     */
    public void updateResults(Object[][] results) {
        if (results == null) {
            clearResults();
            return;
        }

        for (int i = 0; i < results.length && i < tableModel.getRowCount(); i++) {
            for (int j = 0; j < results[i].length && j < tableModel.getColumnCount(); j++) {
                tableModel.setValueAt(results[i][j], i, j);
            }
        }
        tableModel.fireTableDataChanged();
    }

    /**
     * 특정 행의 결과값을 업데이트합니다.
     * 
     * @param row   행 인덱스
     * @param value 설정할 값
     */
    public void updateValue(int row, Object value) {
        if (row >= 0 && row < tableModel.getRowCount()) {
            tableModel.setValueAt(value, row, 1); // Value 컨럼 (1번 인덱스)
        }
    }

    /**
     * 특정 속성명으로 결과값을 업데이트합니다.
     * 
     * @param property 속성명
     * @param value    설정할 값
     */
    public void updateValueByProperty(String property, Object value) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String prop = tableModel.getValueAt(i, 0).toString();
            if (prop.contains(property)) {
                tableModel.setValueAt(value, i, 1);
                break;
            }
        }
    }

    /**
     * 모든 결과를 초기 상태("-")로 리셋합니다.
     */
    public void clearResults() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt("-", i, 1);
        }
        tableModel.fireTableDataChanged();
    }

    /**
     * 테이블 모델을 반환합니다.
     * 
     * @return DefaultTableModel 객체
     */
    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    /**
     * 결과 테이블을 반환합니다.
     * 
     * @return JTable 객체
     */
    public JTable getResultsTable() {
        return resultsTable;
    }

    /**
     * 특정 행의 배경을 깜빡이게 하여 값이 변경되었음을 알림
     */
    public void flashRows(int[] rowIndices) {
        new Thread(() -> {
            try {
                // 임시로 다중 선택 모드로 변경 (여러 행 동시 강조를 위해)
                int originalMode = resultsTable.getSelectionModel().getSelectionMode();
                SwingUtilities.invokeLater(() -> resultsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION));

                for(int i=0; i<3; i++) {
                    SwingUtilities.invokeAndWait(() -> {
                        resultsTable.clearSelection();
                        for (int row : rowIndices) {
                            if (row < resultsTable.getRowCount()) {
                                resultsTable.addRowSelectionInterval(row, row);
                            }
                        }
                    });
                    Thread.sleep(150);
                    
                    SwingUtilities.invokeAndWait(() -> resultsTable.clearSelection());
                    Thread.sleep(150);
                }
                
                // 선택 모드 복구
                SwingUtilities.invokeLater(() -> resultsTable.setSelectionMode(originalMode));
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Save 버튼의 외부 리스너를 설정합니다.
     * 
     * @param listener 리스너
     */
    public void setSaveButtonListener(ActionListener listener) {
        this.saveButtonListener = listener;
    }

    /**
     * 패널의 제목 텍스트를 변경합니다.
     * @param text 새로운 제목
     */
    public void setTitleText(String text) {
        if (titleLabel != null) {
            titleLabel.setText(text);
        }
    }
}