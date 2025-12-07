package meva.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import javax.swing.table.TableRowSorter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableCellRenderer;
import java.util.function.Consumer;
import meva.models.Experiment;
import meva.database.ExperimentDAO;
import java.util.List;

/**
 * DB에 저장된 실험 목록을 보여주고, 불러오거나 삭제하는 패널
 * (InputPanel에서 분리됨)
 */
public class LoadExperimentPanel extends JPanel {

    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JTextField startDateField;
    private JTextField endDateField;
    private JTextArea remarksArea; // [New] 비고 표시용
    // private JComboBox<String> materialCombo; // [Removed] User request

    // 실험이 로드되었을 때 호출할 콜백 (부모 패널로 데이터 전달용)
    private Consumer<Experiment> onExperimentLoaded;

    public LoadExperimentPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setPreferredSize(new Dimension(450, 600)); // [New] 기본 크기 명시하여 레이아웃 축소 방지
        initializeComponents();
    }

    /**
     * 실험 로드 시 실행될 콜백 설정
     */
    public void setOnExperimentLoadedListener(Consumer<Experiment> listener) {
        this.onExperimentLoaded = listener;
    }

    private void initializeComponents() {
        // 상단 검색 및 필터 패널
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));

        // 검색 필드
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("🔍 검색 (ID 또는 재료명):")); // [Modified] 안내 문구 변경
        searchField = new JTextField(15); // [Modified] 너비 축소 (20 -> 15)
        searchField.addActionListener(e -> { // [New] 엔터키 입력 시 검색 수행
            String searchText = searchField.getText().trim();
            searchExperiments(searchText, null);
        });
        searchPanel.add(searchField);
        filterPanel.add(searchPanel);

        // 날짜 필터 (올해 1월 1일 ~ 12월 31일 자동 설정)
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        datePanel.add(new JLabel("📅 기간:"));

        int currentYear = java.time.LocalDate.now().getYear();
        startDateField = new JTextField(currentYear + "-01-01", 10);
        datePanel.add(startDateField);
        datePanel.add(new JLabel("~"));
        endDateField = new JTextField(currentYear + "-12-31", 10);
        datePanel.add(endDateField);
        filterPanel.add(datePanel);

        datePanel.add(endDateField);
        filterPanel.add(datePanel);

        // 재료 필터 [Removed] User request
        // JPanel materialPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        // materialPanel.add(new JLabel("🏷️ 재료:"));
        // String[] materials = { "전체", "강재", "알루미늄", "기타" };
        // materialCombo = new JComboBox<>(materials);
        // materialPanel.add(materialCombo);
        // filterPanel.add(materialPanel);

        add(filterPanel, BorderLayout.NORTH);

        // 중앙 - 실험 목록 테이블
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("📋 저장된 실험 목록"));

        // 테이블 모델 생성
        String[] columnNames = { "ID", "재료명", "시험일시", "직경(mm)", "게이지길이(mm)", "비고" }; // [Modified] "비고" 컬럼 추가 (Hidden)
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 셀 편집 불가
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                // ID(0), 직경(3), 게이지길이(4)는 숫자 정렬을 위해 클래스 명시
                if (columnIndex == 0)
                    return Integer.class;
                if (columnIndex == 3 || columnIndex == 4)
                    return Double.class;
                return String.class;
            }
        };
        table = new JTable(tableModel); // [Modified] 툴팁 오버라이드 제거
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // "비고" 컬럼 숨기기 (모델에는 존재하지만 뷰에서는 제거)
        table.getColumnModel().removeColumn(table.getColumnModel().getColumn(5));

        // [New] 정렬 기능 추가 (TableRowSorter)
        table.setAutoCreateRowSorter(true);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        List<RowSorter.SortKey> sortKeys = new java.util.ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(2, SortOrder.DESCENDING));
        sorter.setSortKeys(sortKeys);

        // [New] 테이블 셀 정렬 (좌측 정렬)
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);
        table.setDefaultRenderer(String.class, leftRenderer);
        table.setDefaultRenderer(Integer.class, leftRenderer);
        table.setDefaultRenderer(Double.class, leftRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        // [New] 하단 비고 영역 추가
        remarksArea = new JTextArea(3, 20); // 3줄 높이
        remarksArea.setEditable(false);
        remarksArea.setLineWrap(true);
        remarksArea.setWrapStyleWord(true);
        remarksArea.setBackground(new Color(240, 240, 240)); // 초기 비활성 색상
        remarksArea.setEnabled(false); // 초기 비활성
        remarksArea.setDisabledTextColor(Color.DARK_GRAY); // 비활성 시 텍스트 가독성 확보

        JScrollPane remarksScroll = new JScrollPane(remarksArea);
        remarksScroll.setBorder(BorderFactory.createTitledBorder("비고"));
        tablePanel.add(remarksScroll, BorderLayout.SOUTH);

        // [New] 테이블 선택 리스너 추가 (비고 연동)
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateRemarksArea();
            }
        });

        // 초기 데이터 로드
        loadExperimentListToTable();

        add(tablePanel, BorderLayout.CENTER);

        // 하단 - 액션 버튼
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton refreshButton = new JButton("🔄 새로고침");
        refreshButton.addActionListener(e -> {
            loadExperimentListToTable();
            JOptionPane.showMessageDialog(this, "실험 목록을 새로고침했습니다.");
        });

        JButton loadButton = new JButton("✅ 불러오기");
        loadButton.addActionListener(e -> onLoadExperiment());

        JButton deleteButton = new JButton("🗑️ 삭제");
        deleteButton.addActionListener(e -> onDeleteExperiment());

        // [Removed] 하단 검색 버튼 제거 (상단 검색창 엔터키로 대체)

        buttonPanel.add(refreshButton);
        // buttonPanel.add(searchButton);
        buttonPanel.add(loadButton);
        buttonPanel.add(deleteButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadExperimentListToTable() {
        tableModel.setRowCount(0); // 기존 데이터 초기화
        ExperimentDAO dao = new ExperimentDAO();
        try {
            List<Experiment> experiments = dao.getAllExperiments();
            for (Experiment exp : experiments) {
                Object[] row = {
                        exp.getId(),
                        exp.getMaterialName(),
                        exp.getTestDate(),
                        exp.getSpecimenDiameter(),
                        exp.getGaugeLength(),
                        exp.getRemarks() // [New] 비고 추가
                };
                tableModel.addRow(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "실험 목록 로드 실패: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void searchExperiments(String searchText, String materialCategory) {
        tableModel.setRowCount(0);
        ExperimentDAO dao = new ExperimentDAO();
        // DAO를 사용하여 검색
        List<Experiment> results = dao.searchExperiments(searchText, materialCategory);

        for (Experiment exp : results) {
            Object[] row = {
                    exp.getId(),
                    exp.getMaterialName(),
                    exp.getTestDate(),
                    exp.getSpecimenDiameter(),
                    exp.getGaugeLength(),
                    exp.getRemarks() // [New] 비고 추가
            };
            tableModel.addRow(row);
        }

        // 만약 실제 필터링이 필요하다면 아래와 같이 구현 (메모리 필터링):
        /*
         * List<Experiment> all = dao.getAllExperiments();
         * for (Experiment exp : all) {
         * // ... 조건 검사 ...
         * // tableModel.addRow(...);
         * }
         */

    }

    private void onLoadExperiment() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "불러올 실험을 선택해주세요.", "선택 필요", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int experimentId = (int) tableModel.getValueAt(selectedRow, 0);

        // DB에서 상세 조회
        ExperimentDAO dao = new ExperimentDAO();
        try {
            Experiment exp = dao.getExperimentById(experimentId);
            if (exp != null) {
                // 콜백을 통해 상위로 데이터 전달
                if (onExperimentLoaded != null) {
                    onExperimentLoaded.accept(exp);
                }

                // 성공 메시지는 로그로 대체 (InputPanel 변경사항 반영)
                System.out.println("Experiment Loaded ID: " + experimentId);
            } else {
                JOptionPane.showMessageDialog(this, "실험 데이터를 찾을 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "데이터 로드 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDeleteExperiment() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "삭제할 실험을 선택해주세요.", "선택 필요", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                selectedRows.length + "개의 실험을 삭제하시겠습니까?",
                "삭제 확인",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            ExperimentDAO dao = new ExperimentDAO();
            int successCount = 0;
            int failCount = 0;

            // 선택된 행들의 ID 수집 (삭제 시 인덱스 변화 방지 위해 ID 먼저 수집)
            List<Integer> idsToDelete = new java.util.ArrayList<>();
            for (int row : selectedRows) {
                // 정렬된 상태를 고려하여 모델 인덱스 변환
                int modelRow = table.convertRowIndexToModel(row);
                idsToDelete.add((int) tableModel.getValueAt(modelRow, 0));
            }

            for (int id : idsToDelete) {
                if (dao.deleteExperiment(id)) {
                    successCount++;
                } else {
                    failCount++;
                }
            }

            // 결과 리포트 및 테이블 갱신
            String message = successCount + "개 삭제 완료.";
            if (failCount > 0) {
                message += "\n" + failCount + "개 삭제 실패.";
            }
            JOptionPane.showMessageDialog(this, message);

            // 테이블 데이터 갱신 (삭제 반영)
            loadExperimentListToTable();
        }
    }

    // [New] 비고 영역 업데이트 메서드
    private void updateRemarksArea() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            int modelRow = table.convertRowIndexToModel(selectedRow);
            Object value = tableModel.getValueAt(modelRow, 5); // 5: 비고 컬럼
            String remark = (value != null) ? value.toString() : "";

            if (!remark.trim().isEmpty()) {
                remarksArea.setText(remark);
                remarksArea.setEnabled(true);
                remarksArea.setBackground(Color.WHITE);
            } else {
                remarksArea.setText("비고 없음");
                remarksArea.setEnabled(false);
                remarksArea.setBackground(new Color(240, 240, 240));
            }
        } else {
            remarksArea.setText("");
            remarksArea.setEnabled(false);
            remarksArea.setBackground(new Color(240, 240, 240));
        }
    }
}
