package meva.gui;

import meva.database.ExperimentDAO;
import meva.models.Experiment;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * 이전 실험 데이터 불러오기 다이얼로그
 * 데이터베이스에 저장된 실험 목록을 보여주고 선택할 수 있음
 */
public class LoadExperimentDialog extends JDialog {

    private JTable experimentTable;
    private DefaultTableModel tableModel;
    private ExperimentDAO experimentDAO;
    private Experiment selectedExperiment;
    private JButton loadButton;
    private JButton deleteButton;
    private JButton cancelButton;

    public LoadExperimentDialog(Frame parent) {
        super(parent, "이전 실험 불러오기", true);
        setDialogIcon();
        experimentDAO = new ExperimentDAO();
        initializeComponents();
        loadExperiments();
        setSize(800, 500);
        setLocationRelativeTo(parent);
    }

    private void setDialogIcon() {
        try {
            String[] possiblePaths = {
                    "resources/meva_icon.png",
                    "MEVA/resources/meva_icon.png",
                    "../resources/meva_icon.png"
            };
            for (String path : possiblePaths) {
                java.io.File iconFile = new java.io.File(path);
                if (iconFile.exists()) {
                    java.awt.Image icon = javax.imageio.ImageIO.read(iconFile);
                    setIconImage(icon);
                    break;
                }
            }
        } catch (Exception e) {
            // 아이콘 로드 실패 시 기본 아이콘 사용
        }
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(10, 10));

        // 상단: 제목
        JLabel titleLabel = new JLabel("저장된 실험 목록");
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(titleLabel, BorderLayout.NORTH);

        // 중앙: 테이블
        String[] columnNames = { "ID", "재료명", "실험 날짜", "최대 응력 (MPa)", "UTS (MPa)" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        experimentTable = new JTable(tableModel);
        experimentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        experimentTable.setRowHeight(25);
        experimentTable.getTableHeader().setReorderingAllowed(false);

        // 테이블 더블 클릭 시 불러오기
        experimentTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    loadSelectedExperiment();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(experimentTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);

        // 하단: 버튼
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        loadButton = new JButton("불러오기");
        loadButton.setPreferredSize(new Dimension(100, 30));
        loadButton.addActionListener(e -> loadSelectedExperiment());

        deleteButton = new JButton("삭제");
        deleteButton.setPreferredSize(new Dimension(100, 30));
        deleteButton.addActionListener(e -> deleteSelectedExperiment());

        cancelButton = new JButton("취소");
        cancelButton.setPreferredSize(new Dimension(100, 30));
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(loadButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * 데이터베이스에서 실험 목록 불러오기
     */
    private void loadExperiments() {
        tableModel.setRowCount(0);
        List<Experiment> experiments = experimentDAO.getAllExperiments();

        for (Experiment exp : experiments) {
            Object[] row = {
                    exp.getId(),
                    exp.getMaterialName() != null ? exp.getMaterialName() : "Unknown",
                    exp.getTestDate() != null ? exp.getTestDate() : "N/A",
                    exp.getMaxStress() != null ? String.format("%.2f", exp.getMaxStress()) : "-",
                    exp.getUts() != null ? String.format("%.2f", exp.getUts()) : "-"
            };
            tableModel.addRow(row);
        }
    }

    /**
     * 선택된 실험 불러오기
     */
    private void loadSelectedExperiment() {
        int selectedRow = experimentTable.getSelectedRow();

        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "실험을 선택해주세요.",
                    "알림",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int experimentId = (int) tableModel.getValueAt(selectedRow, 0);
        selectedExperiment = experimentDAO.getExperimentById(experimentId);

        if (selectedExperiment != null) {
            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    "실험 데이터를 불러올 수 없습니다.",
                    "오류",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 선택된 실험 삭제
     */
    private void deleteSelectedExperiment() {
        int selectedRow = experimentTable.getSelectedRow();

        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "삭제할 실험을 선택해주세요.",
                    "알림",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "선택한 실험을 삭제하시겠습니까?",
                "삭제 확인",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            int experimentId = (int) tableModel.getValueAt(selectedRow, 0);
            boolean deleted = experimentDAO.deleteExperiment(experimentId);

            if (deleted) {
                JOptionPane.showMessageDialog(this,
                        "삭제가 완료되었습니다.",
                        "완료",
                        JOptionPane.INFORMATION_MESSAGE);
                loadExperiments(); // 목록 새로고침
            } else {
                JOptionPane.showMessageDialog(this,
                        "삭제에 실패했습니다.",
                        "오류",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 선택된 실험 가져오기
     */
    public Experiment getSelectedExperiment() {
        return selectedExperiment;
    }
}
