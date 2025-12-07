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
import meva.education.GlossaryManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * 계산 결과를 표시하는 패널
 * GUI 설계 문서에 따른 완전한 구현
 * 
 * @author MEVA 개발팀
 * @version 1.0
 */
public class ResultPanel extends JPanel {

    // 결과 테이블 및 관련 컴포넌트
    private JTable resultsTable; // 계산 결과를 표시하는 표 컴포넌트
    private DefaultTableModel tableModel; // 테이블의 데이터 모델 (행/열 관리)
    private JScrollPane scrollPane; // 테이블에 스크롤 기능을 제공하는 컨테이너
    private JButton saveButton; // 결과를 CSV 파일로 저장하는 버튼
    private JButton referenceButton; // [New] 참고 자료 링크 버튼
    private JCheckBox unitToggle; // [New] 에너지 단위 변환 토글 (MJ/m³ <-> J/mm³)
    private JLabel titleLabel; // 패널 제목 레이블 (동적 변경용)

    // [New] 수식 표시용 패널 (Refactored)
    private FormulaPanel formulaPanel;
    private JScrollPane formulaScrollPane;

    // 외부 리스너
    private ActionListener saveButtonListener;

    // 현재 선택된 에너지 단위 (Default: MJ/m³)
    private boolean useJouleMM3 = false; // false=MJ/m³, true=J/mm³

    // 테이블 데이터
    private static final String[] COLUMN_NAMES = { "속성", "값", "단위" };
    private static final Object[][] INITIAL_DATA = {
            { "극한 인장 강도 (UTS)", "-", "MPa" },
            { "영률 (E)", "-", "GPa" },
            { "항복 강도 (0.2% Offset)", "-", "MPa" },
            { "연신율", "-", "%" },
            { "균일 연신율 (Uniform Elongation)", "-", "-" },
            { "단면 감소율", "-", "%" },
            { "인성 (Toughness)", "-", "MJ/m³" },
            { "레질리언스 계수 (Resilience)", "-", "MJ/m³" },
            { "비례 한계", "-", "MPa" },
            { "파괴 응력", "-", "MPa" },
            { "파괴 변형률", "-", "-" },
            { "소성 연신율 (Plastic Elongation)", "-", "-" }
    };

    // 현재 데이터 상태 저장 (모드 변경 시 재계산용)
    private meva.models.AnalysisResult currentResult;

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

        // 테이블 생성 및 설정 (툴팁 기능 추가를 위해 익명 클래스 사용)
        resultsTable = new JTable(tableModel) {
            @Override
            public String getToolTipText(java.awt.event.MouseEvent e) {
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);

                // 첫 번째 열(속성명)에 마우스가 있을 때만 툴팁 표시
                if (rowIndex >= 0 && colIndex == 0) {
                    Object value = getValueAt(rowIndex, 0);
                    if (value != null) {
                        return GlossaryManager.getDefinition(value.toString());
                    }
                }
                return super.getToolTipText(e);
            }
        };
        resultsTable.setRowHeight(25);

        // [New] 테이블 행 선택 리스너 추가 (수식 표시)
        resultsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int selectedRow = resultsTable.getSelectedRow();
                    if (selectedRow != -1 && formulaPanel != null && currentResult != null) {
                        String propertyName = (String) resultsTable.getValueAt(selectedRow, 0);

                        // FormulaPanel에 현재 옵션 상태 전달
                        boolean useUpperYield = (lastYieldMode == 2);
                        formulaPanel.setOptions(lastIsTrueStress, lastUseTriangleResilience, useUpperYield);

                        // 테이블에 표시된 값을 파싱하여 전달 (불일치 방지)
                        double displayedValue = parseDisplayedValue(selectedRow);
                        formulaPanel.updateFormula(propertyName, currentResult, displayedValue);
                    }
                }
            }
        });

        // [New] 수식 패널 초기화 (Refactored to use FormulaPanel)
        formulaPanel = new FormulaPanel();

        formulaScrollPane = new JScrollPane(formulaPanel);
        formulaScrollPane.setPreferredSize(new Dimension(0, 200)); // Height regarding two formulas
        resultsTable.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 테이블 헤더 설정
        JTableHeader header = resultsTable.getTableHeader();
        header.setFont(new Font("Malgun Gothic", Font.BOLD, 12));
        header.setBackground(new Color(230, 230, 230));
        header.setReorderingAllowed(false); // 컬럼 순서 변경 불가

        // 컬럼 너비 설정
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(180); // Property
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Value
        resultsTable.getColumnModel().getColumn(2).setPreferredWidth(60); // Unit

        // 스크롤 패널 생성
        scrollPane = new JScrollPane(resultsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // [New] 단위 변환 토글
        unitToggle = new JCheckBox("J/mm³ 단위 변환");
        unitToggle.setToolTipText("체크 시 에너지 단위를 J/mm³로 표시합니다. (1 MJ/m³ = 0.001 J/mm³)");
        unitToggle.addActionListener(e -> {
            useJouleMM3 = unitToggle.isSelected();
            // 현재 결과가 있으면 즉시 업데이트
            if (currentResult != null) {
                refreshValues();
            }
        });

        // [New] 참고 자료 버튼 생성
        referenceButton = new JButton("📚 참고 자료");
        referenceButton.setToolTipText("재료 역학 참고 문서 (Engineering ToolBox)");
        referenceButton.setFont(new Font("Dialog", Font.PLAIN, 12));
        referenceButton.setFocusPainted(false);
        referenceButton.addActionListener(e -> openReferenceLink());

        // Save Results 버튼 생성
        saveButton = new JButton("결과 파일 내보내기 (CSV)");
        saveButton.setPreferredSize(new Dimension(180, 35)); // 너비 확장
        saveButton.setFont(new Font("Dialog", Font.BOLD, 12));
        saveButton.setFocusPainted(false);
        saveButton.addActionListener(e -> {
            saveResultsToCSV();
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

        // [User Request] 좌우 너비 조절 한계 완화 (Min size reduced)
        setMinimumSize(new Dimension(150, 0));
        // 의도: 초기 실행 시 너비(350px)로 시작하도록 설정
        setPreferredSize(new Dimension(350, 0));

        // 타이틀 레이블
        titleLabel = new JLabel("분석 결과");
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 14));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        add(titleLabel, BorderLayout.NORTH);

        // 상단: 타이틀 + 단위 토글
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(unitToggle, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 테이블 (중앙)
        // [Modified] JSplitPane 사용 (테이블 + 수식)
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, formulaScrollPane);
        splitPane.setResizeWeight(0.6); // 테이블에 60% 공간 할당 (수식 공간 확보)
        splitPane.setDividerLocation(350); // 초기 분할 위치

        add(splitPane, BorderLayout.CENTER);

        // 버튼 패널 (하단)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.add(referenceButton); // 참고 자료 버튼 (왼쪽)
        buttonPanel.add(saveButton); // 내보내기 버튼 (오른쪽)

        // [New] 면책사항 레이블
        JLabel disclaimerLabel = new JLabel(
                "<html><center><font size='2' color='gray'>⚠ 계산 결과는 참고용입니다. 공인 시험 성적서와 비교 검증하세요.</font></center></html>");
        disclaimerLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 하단 패널 (버튼 + 면책사항)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(buttonPanel, BorderLayout.CENTER);
        bottomPanel.add(disclaimerLabel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
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
                    "저장할 결과가 없습니다. 먼저 계산을 진행해 주세요.",
                    "데이터 없음",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 파일 선택 다이얼로그
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("결과 저장");

        // 기본 파일명 설정
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String defaultName = "MEVA_Results_" + timestamp;

        if (currentResult != null && currentResult.getExperimentName() != null) {
            String matName = currentResult.getExperimentName().replaceAll("[^a-zA-Z0-9가-힣]", "_");
            defaultName = matName + "_" + timestamp;
        }

        fileChooser.setSelectedFile(new File(defaultName + ".csv"));

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
                        "파일이 이미 존재합니다. 덮어쓰시겠습니까?",
                        "덮어쓰기 확인",
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
                writer.write("Export Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");

                // [New] 메타데이터 출력
                if (currentResult != null) {
                    writer.write("Material: " + getSafeString(currentResult.getExperimentName()) + "\n");
                    writer.write("Test Date: " + getSafeString(currentResult.getTestDate()) + "\n");
                    writer.write("Tester: " + getSafeString(currentResult.getExperimenter()) + "\n");
                    writer.write("Remarks: " + getSafeString(currentResult.getRemarks()) + "\n");
                }
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

                // [New] 면책사항 추가
                writer.write("\n");
                writer.write("===== DISCLAIMER =====\n");
                writer.write("본 계산 결과는 참고용이며, 공인 시험 성적서를 대체하지 않습니다.\n");
                writer.write("중요한 의사결정에는 반드시 공식 문서와 비교 검증하시기 바랍니다.\n");

                JOptionPane.showMessageDialog(this,
                        "결과가 성공적으로 저장되었습니다:\n" + fileToSave.getAbsolutePath(),
                        "저장 완료",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "파일 저장 중 오류 발생: " + e.getMessage(),
                        "저장 오류",
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
     */
    public void updateMode(boolean isTrueStress, boolean useTriangleResilience) {
        updateMode(isTrueStress, useTriangleResilience, 0); // 0: Auto
    }

    // 현재 표시 모드 상태 저장
    private boolean lastIsTrueStress;
    private boolean lastUseTriangleResilience;
    private int lastYieldMode;

    public void updateMode(boolean isTrueStress, boolean useTriangleResilience, int yieldMode) {
        // 상태 저장 (단위 토글 시 재사용)
        this.lastIsTrueStress = isTrueStress;
        this.lastUseTriangleResilience = useTriangleResilience;
        this.lastYieldMode = yieldMode;

        if (currentResult == null) {
            clearResults();
            return;
        }

        String modeText = isTrueStress ? "(True)" : "(Engineering)";
        setTitleText("분석 결과 " + modeText);

        // 1. 공통 값
        updateValueByProperty("연신율", String.format("%.2f", currentResult.getElongation()));
        updateValueByProperty("단면 감소율", String.format("%.2f", currentResult.getReductionOfArea()));

        // 인성 (단위 변환 적용)
        double toughness = currentResult.getToughness();
        updateEnergyValue("인성 (Toughness)", toughness);

        // 2. 모드에 따라 달라지는 값 (Stress/Modulus)
        double E_GPa = isTrueStress ? currentResult.getYoungsModulus() : currentResult.getYoungsModulusEng();
        if (!isTrueStress && E_GPa == 0)
            E_GPa = currentResult.getYoungsModulus();

        updateValueByProperty("영률", String.format("%.3f", E_GPa));

        // UTS
        meva.models.StressStrainPoint utsPt = currentResult.getUtsPoint();
        if (utsPt != null) {
            double uts = isTrueStress ? utsPt.getTrueStress() : utsPt.getEngineeringStress();
            updateValueByProperty("극한 인장 강도", String.format("%.3f", uts));
            double utsStrain = isTrueStress ? utsPt.getTrueStrain() : utsPt.getEngineeringStrain();
            updateValueByProperty("균일 연신율", String.format("%.4f", utsStrain));
        }

        // 항복 강도 (선택된 모드에 따라 결정)
        double yieldStr = 0.0;
        boolean isDiscontinuousMode = false;

        if (yieldMode == 2) { // Upper/Lower 강제
            isDiscontinuousMode = true;
            if (currentResult.getUpperYieldPoint() != null) {
                yieldStr = isTrueStress ? currentResult.getUpperYieldPoint().getTrueStress()
                        : currentResult.getUpperYieldPoint().getEngineeringStress();
            }
        } else if (yieldMode == 1) { // Offset 강제
            if (!isTrueStress && currentResult.getOffsetYieldPointEng() != null) {
                yieldStr = currentResult.getOffsetYieldPointEng().getEngineeringStress();
            } else if (currentResult.getOffsetYieldPoint() != null) {
                yieldStr = isTrueStress ? currentResult.getOffsetYieldPoint().getTrueStress()
                        : currentResult.getOffsetYieldPoint().getEngineeringStress();
            }
        } else { // Auto (0)
            if (currentResult.getYieldType() == meva.models.AnalysisResult.YieldType.DISCONTINUOUS) {
                isDiscontinuousMode = true;
                if (currentResult.getUpperYieldPoint() != null) {
                    yieldStr = isTrueStress ? currentResult.getUpperYieldPoint().getTrueStress()
                            : currentResult.getUpperYieldPoint().getEngineeringStress();
                }
            } else {
                if (!isTrueStress && currentResult.getOffsetYieldPointEng() != null) {
                    yieldStr = currentResult.getOffsetYieldPointEng().getEngineeringStress();
                } else if (currentResult.getOffsetYieldPoint() != null) {
                    yieldStr = isTrueStress ? currentResult.getOffsetYieldPoint().getTrueStress()
                            : currentResult.getOffsetYieldPoint().getEngineeringStress();
                } else {
                    yieldStr = currentResult.getYieldStrength();
                }
            }
        }
        updateValueByProperty("항복 강도", String.format("%.3f", yieldStr));

        // 3. 탄성 에너지 (Resilience) - 모드 반영
        double resilience = 0.0;
        if (useTriangleResilience) {
            // Triangle Mode: 0.5 * σ_y^2 / E
            if (E_GPa > 0) {
                resilience = (0.5 * yieldStr * yieldStr) / (E_GPa * 1000.0); // MPa단위 맞춤
            }
        } else {
            // Integral Mode: 실제 적분값 사용
            if (isDiscontinuousMode) {
                resilience = currentResult.getResilienceIntegral(); // UYP 기준
            } else {
                resilience = currentResult.getResilienceIntegralOffset(); // Offset 기준
            }
        }
        // 레질리언스 (단위 변환 적용)
        updateEnergyValue("레질리언스 계수", resilience);

        // 기타
        updateValueByProperty("비례 한계", String.format("%.3f", currentResult.getProportionalLimit()));
        updateValueByProperty("파괴 응력", String.format("%.3f", currentResult.getFractureStress()));
        updateValueByProperty("파괴 변형률", String.format("%.4f", currentResult.getFractureStrain()));

        // [New] 소성 연신율 (Plastic Elongation) 계산
        double fractureStrain = currentResult.getFractureStrain();
        double fractureStress = currentResult.getFractureStress(); // MPa
        if (E_GPa > 0) {
            double plasticStrain = fractureStrain - (fractureStress / (E_GPa * 1000.0));
            if (plasticStrain < 0)
                plasticStrain = 0;
            updateValueByProperty("소성 연신율 (Plastic Elongation)", String.format("%.4f", plasticStrain));
        } else {
            updateValueByProperty("소성 연신율 (Plastic Elongation)", "-");
        }
    }

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

    public void updateValue(int row, Object value) {
        if (row >= 0 && row < tableModel.getRowCount()) {
            tableModel.setValueAt(value, row, 1); // Value 컨럼 (1번 인덱스)
        }
    }

    public void updateValueByProperty(String property, Object value) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String prop = tableModel.getValueAt(i, 0).toString();
            if (prop.contains(property)) {
                tableModel.setValueAt(value, i, 1);
                break;
            }
        }
    }

    public void clearResults() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt("-", i, 1);
        }
        tableModel.fireTableDataChanged();
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public JTable getResultsTable() {
        return resultsTable;
    }

    public void flashRows(int[] rowIndices) {
        new Thread(() -> {
            try {
                int originalMode = resultsTable.getSelectionModel().getSelectionMode();
                SwingUtilities.invokeLater(
                        () -> resultsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION));

                for (int i = 0; i < 3; i++) {
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

                SwingUtilities.invokeLater(() -> resultsTable.setSelectionMode(originalMode));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void setSaveButtonListener(ActionListener listener) {
        this.saveButtonListener = listener;
    }

    public void setTitleText(String text) {
        if (titleLabel != null) {
            titleLabel.setText(text);
        }
    }

    private void refreshValues() {
        updateMode(lastIsTrueStress, lastUseTriangleResilience, lastYieldMode);
    }

    private void updateEnergyValue(String propertyName, double valueMJ) {
        double displayValue = valueMJ;
        String unit = "MJ/m³";

        if (useJouleMM3) {
            displayValue = valueMJ * 0.001;
            unit = "J/mm³";
        }

        updateValueByProperty(propertyName, String.format("%.4f", displayValue));
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String prop = tableModel.getValueAt(i, 0).toString();
            if (prop.contains(propertyName)) {
                tableModel.setValueAt(unit, i, 2);
                break;
            }
        }
    }

    private String getSafeString(String input) {
        return (input != null) ? input : "-";
    }

    /**
     * 테이블에서 표시된 값을 파싱하여 double로 반환합니다.
     * 
     * @param row 테이블 행 인덱스
     * @return 파싱된 값, 실패 시 0.0
     */
    private double parseDisplayedValue(int row) {
        try {
            Object valueObj = tableModel.getValueAt(row, 1);
            if (valueObj == null || "-".equals(valueObj.toString())) {
                return 0.0;
            }
            return Double.parseDouble(valueObj.toString().replaceAll("[^0-9.\\-]", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * 참고 자료 웹 링크를 기본 브라우저에서 엽니다.
     */
    private void openReferenceLink() {
        String url = "https://www.matweb.com/index.aspx";
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "링크를 열 수 없습니다:\n" + url,
                    "오류",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
}