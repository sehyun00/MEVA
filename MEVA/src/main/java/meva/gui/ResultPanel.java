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
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JScrollPane scrollPane;
    private JButton saveButton;

    // 외부 리스너
    private ActionListener saveButtonListener;

    // 테이블 데이터
    private static final String[] COLUMN_NAMES = { "속성", "값", "단위" };
    private static final Object[][] INITIAL_DATA = {
            { "최대 응력 (σmax)", "-", "MPa" },
            { "최대 응력 시 변형률 (εmax)", "-", "-" },
            { "극한 인장 강도 (UTS)", "-", "MPa" },
            { "영률 (E)", "-", "GPa" },
            { "항복 강도 (σy)", "-", "MPa" },
            { "연신율", "-", "%" },
            { "단면 감소율", "-", "%" },
            { "인성", "-", "MJ/m³" },
            { "탄성 에너지", "-", "MJ/m³" },
            { "탄성 한계", "-", "MPa" },
            { "비례 한계", "-", "MPa" },
            { "네킹 시작 변형률", "-", "-" },
            { "파괴 응력", "-", "MPa" },
            { "파괴 변형률", "-", "-" }
    };

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

        // 타이틀 레이블
        JLabel titleLabel = new JLabel("Results");
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
     * 결과 테이블을 업데이트합니다.
     * 
     * @param results 계산 결과 데이터
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
     * Save 버튼의 외부 리스너를 설정합니다.
     * 
     * @param listener 리스너
     */
    public void setSaveButtonListener(ActionListener listener) {
        this.saveButtonListener = listener;
    }
}