package meva.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import meva.models.Experiment;

/**
 * 사용자 입력을 받는 패널 (Refactored)
 * 역할: NewExperimentPanel과 LoadExperimentPanel을 담는 컨테이너 및 중재자
 */
public class InputPanel extends JPanel {

    private JTabbedPane tabbedPane;
    private NewExperimentPanel newExperimentPanel;
    private LoadExperimentPanel loadExperimentPanel;

    public InputPanel() {
        initializeComponents();
        setupLayout();
    }

    private void initializeComponents() {
        newExperimentPanel = new NewExperimentPanel();
        loadExperimentPanel = new LoadExperimentPanel();

        // 중재 로직 (Load -> New 데이터 전달)
        loadExperimentPanel.setOnExperimentLoadedListener(this::onExperimentLoaded);
    }

    private void onExperimentLoaded(Experiment exp) {
        if (exp != null) {
            // 1. 데이터를 New 패널에 채움
            newExperimentPanel.setExperimentData(exp);
            // 2. 탭을 "새 실험"으로 전환
            tabbedPane.setSelectedIndex(0);
        }
    }

    private void setupLayout() {
        tabbedPane = new JTabbedPane();

        // Tab 1: New Experiment (with ScrollPane)
        JScrollPane newExperimentScroll = new JScrollPane(newExperimentPanel);
        newExperimentScroll.setBorder(null);
        newExperimentScroll.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab("📂 새 실험", newExperimentScroll);

        // Tab 2: Load Experiment
        tabbedPane.addTab("📋 이전 실험 불러오기", loadExperimentPanel);

        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        setPreferredSize(new Dimension(350, 0));
        setMinimumSize(new Dimension(350, 0));
    }

    // --- Delegation Methods (MainFrame calls these) ---

    // 1. Delegation: Listeners
    public void setCalculateListener(ActionListener listener) {
        newExperimentPanel.setCalculateListener(listener);
    }

    public void setResetListener(ActionListener listener) {
        newExperimentPanel.setResetListener(listener);
    }

    // 2. Delegation: Getters
    public double getInitialDiameter() {
        return newExperimentPanel.getInitialDiameter();
    }

    public double getGaugeLength() {
        return newExperimentPanel.getGaugeLength();
    }

    public String getMaterialName() {
        return newExperimentPanel.getMaterialName();
    }

    public String getTestDate() {
        return newExperimentPanel.getTestDate();
    }

    public String getTesterName() {
        return newExperimentPanel.getTesterName();
    }

    public String getTestMethod() {
        return newExperimentPanel.getTestMethod();
    }

    public String getRemarks() {
        return newExperimentPanel.getRemarks();
    }

    public Double getFinalCrossSectionArea() {
        return newExperimentPanel.getFinalCrossSectionArea();
    }

    public String getSelectedFilePath() {
        return newExperimentPanel.getSelectedFilePath();
    }

    public double getInitialCrossSection() {
        return newExperimentPanel.getInitialCrossSection();
    }
}
