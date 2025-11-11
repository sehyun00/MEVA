// src/main/java/meva/gui/ResultPanel.java

package meva.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;

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
    
    // 테이블 데이터
    private static final String[] COLUMN_NAMES = {"Property", "Value", "Unit"};
    private static final Object[][] INITIAL_DATA = {
        {"Max Stress (σmax)", "-", "MPa"},
        {"Strain at Max (εmax)", "-", "-"},
        {"UTS", "-", "MPa"},
        {"Young's Modulus (E)", "-", "GPa"},
        {"Yield Strength (σy)", "-", "MPa"},
        {"Elongation", "-", "%"},
        {"Reduction of Area", "-", "%"}
    };
    
    /**
     * ResultPanel 생성자
     */
    public ResultPanel() {
        initializeComponents();
        setupLayout();
    }
    
    /**
     * 모든 컴포넌트 초기화
     */
    private void initializeComponents() {
        // 테이블 모델 생성
        tableModel = new DefaultTableModel(INITIAL_DATA, COLUMN_NAMES) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Value 열만 편집 가능
                return column == 1;
            }
        };
        
        // 테이블 생성
        resultsTable = new JTable(tableModel);
        resultsTable.setFont(new Font("Monospaced", Font.PLAIN, 11));
        resultsTable.setRowHeight(25);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // 테이블 헤더 스타일 설정
        JTableHeader header = resultsTable.getTableHeader();
        header.setFont(new Font("Dialog", Font.BOLD, 11));
        header.setBackground(new Color(224, 224, 224));
        header.setReorderingAllowed(false);
        
        // 열 너비 설정
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        resultsTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        
        // 교차 행 색상 설정
        resultsTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 245, 245));
                }
                return c;
            }
        });
        
        // 스크롤 패널 생성
        scrollPane = new JScrollPane(resultsTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
    }
    
    /**
     * 레이아웃 설정
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(250, 0));
        setBorder(BorderFactory.createTitledBorder("Results"));
        
        // 스크롤 패널 추가
        add(scrollPane, BorderLayout.CENTER);
    }
    
    /**
     * 결과 표시 메서드
     * 계산 결과를 테이블에 업데이트
     */
    public void displayResults() {
        // TODO: 결과 표시 로직
        // 계산 결과를 받아서 테이블의 Value 열 업데이트
    }
    
    /**
     * 특정 행의 값을 업데이트
     */
    public void setResultValue(int rowIndex, String value) {
        if (rowIndex >= 0 && rowIndex < tableModel.getRowCount()) {
            tableModel.setValueAt(value, rowIndex, 1);
        }
    }
    
    /**
     * 모든 결과 값 초기화
     */
    public void clearResults() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt("-", i, 1);
        }
    }
    
    /**
     * 테이블 모델 반환
     */
    public DefaultTableModel getTableModel() {
        return tableModel;
    }
    
    /**
     * 테이블 반환
     */
    public JTable getResultsTable() {
        return resultsTable;
    }
}