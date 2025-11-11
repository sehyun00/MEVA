package meva.gui;

import javax.swing.*;
import java.awt.*;

/**
 * MEVA 애플리케이션의 메인 윈도우 프레임
 * GUI 설계 문서에 따른 완전한 구현
 * 
 * @author MEVA 개발팀
 * @version 1.0
 */
public class MainFrame extends JFrame {
    // UI 컴포넌트
    private MenuBar menuBar;
    private JToolBar toolBar;
    private JPanel mainPanel;
    private InputPanel inputPanel;
    private GraphPanel visualizationPanel;
    private ResultPanel resultsPanel;
    private JPanel statusBar;
    
    // 상태바 컴포넌트
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JLabel timeLabel;
    
    /**
     * MainFrame 생성자
     */
    public MainFrame() {
        initializeLookAndFeel();
        initializeComponents();
        setupLayout();
        setupFrame();
    }
    
    /**
     * Look and Feel 초기화
     */
    private void initializeLookAndFeel() {
        try {
            // 시스템 Look and Feel 사용
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            try {
                // Fallback to Nimbus
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ex) {
                System.err.println("Failed to set Look and Feel: " + ex.getMessage());
            }
        }
    }
    
    /**
     * 모든 컴포넌트 초기화
     */
    private void initializeComponents() {
        // MenuBar 초기화
        menuBar = new MenuBar();
        setupMenuBarListeners();
        
        // ToolBar 초기화
        toolBar = createToolBar();
        
        // 메인 패널 초기화 (BorderLayout)
        mainPanel = new JPanel(new BorderLayout());
        
        // InputPanel 초기화 (WEST)
        inputPanel = new InputPanel();
        setupInputPanelListeners();
        
        // VisualizationPanel 초기화 (CENTER)
        visualizationPanel = new GraphPanel();
        setupGraphPanelListeners();
        
        // ResultsPanel 초기화 (EAST)
        resultsPanel = new ResultPanel();
        
        // StatusBar 초기화 (SOUTH)
        statusBar = createStatusBar();
    }
    
    /**
     * MenuBar 이벤트 리스너 설정
     */
    private void setupMenuBarListeners() {
        menuBar.setFileNewListener(e -> onNewProject());
        menuBar.setFileOpenListener(e -> onOpenProject());
        menuBar.setFileSaveListener(e -> onSaveProject());
        menuBar.setFileSaveAsListener(e -> onSaveAsProject());
        menuBar.setFileExportListener(e -> onExportData());
        menuBar.setFileExitListener(e -> onExit());
        menuBar.setEditUndoListener(e -> onUndo());
        menuBar.setEditRedoListener(e -> onRedo());
        menuBar.setEditPreferencesListener(e -> onPreferences());
        menuBar.setViewZoomInListener(e -> onZoomIn());
        menuBar.setViewZoomOutListener(e -> onZoomOut());
        menuBar.setViewResetZoomListener(e -> onResetZoom());
        menuBar.setViewToggleGridListener(e -> onToggleGrid());
        menuBar.setViewToggleLegendListener(e -> onToggleLegend());
        menuBar.setToolsCalculateListener(e -> onCalculate());
        menuBar.setToolsClearDataListener(e -> onClearData());
        menuBar.setToolsDataValidatorListener(e -> onDataValidator());
        menuBar.setHelpUserGuideListener(e -> onUserGuide());
        menuBar.setHelpAboutListener(e -> onAbout());
    }
    
    /**
     * InputPanel 이벤트 리스너 설정
     */
    private void setupInputPanelListeners() {
        inputPanel.setCalculateListener(e -> onCalculateClicked());
        inputPanel.setResetListener(e -> onResetClicked());
        inputPanel.setClearGraphListener(e -> onClearGraphClicked());
        inputPanel.setPresetChangedListener(e -> onPresetChanged());
        inputPanel.setSavePresetListener(e -> onSavePreset());
        inputPanel.setDeletePresetListener(e -> onDeletePreset());
    }
    
    /**
     * GraphPanel 이벤트 리스너 설정
     */
    private void setupGraphPanelListeners() {
        visualizationPanel.setZoomInListener(e -> onZoomIn());
        visualizationPanel.setZoomOutListener(e -> onZoomOut());
        visualizationPanel.setResetZoomListener(e -> onResetZoom());
        visualizationPanel.setExportChartListener(e -> onExportChart());
    }
    
    /**
     * 툴바 생성
     */
    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setMargin(new Insets(5, 5, 5, 5));
        
        // 툴바 버튼 추가
        toolBar.add(createToolButton("New", "New Project", this::onNewProject));
        toolBar.add(createToolButton("Open", "Open Project", this::onOpenProject));
        toolBar.add(createToolButton("Save", "Save Project", this::onSaveProject));
        toolBar.addSeparator();
        toolBar.add(createToolButton("Export", "Export Data", this::onExportData));
        toolBar.addSeparator();
        toolBar.add(createToolButton("Settings", "Settings", this::onPreferences));
        
        return toolBar;
    }
    
    /**
     * 툴바 버튼 생성 헬퍼 메서드
     */
    private JButton createToolButton(String text, String tooltip, Runnable action) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(32, 32));
        button.addActionListener(e -> action.run());
        return button;
    }
    
    
    /**
     * 상태바 생성 (SOUTH)
     */
    private JPanel createStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(0, 25));
        panel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        
        // 상태 레이블 (WEST)
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
        panel.add(statusLabel, BorderLayout.WEST);
        
        // 진행바 (CENTER)
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(200, 15));
        progressBar.setVisible(false);
        panel.add(progressBar, BorderLayout.CENTER);
        
        // 시간 레이블 (EAST)
        timeLabel = new JLabel(java.time.LocalTime.now().toString());
        timeLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
        panel.add(timeLabel, BorderLayout.EAST);
        
        // 시간 업데이트 타이머
        Timer timer = new Timer(1000, e -> 
            timeLabel.setText(java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))));
        timer.start();
        
        return panel;
    }
    
    /**
     * 레이아웃 설정
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // MenuBar 설정
        setJMenuBar(menuBar);
        
        // ToolBar 추가 (NORTH)
        add(toolBar, BorderLayout.NORTH);
        
        // 메인 패널 구성
        mainPanel.add(inputPanel, BorderLayout.WEST);
        mainPanel.add(visualizationPanel, BorderLayout.CENTER);
        mainPanel.add(resultsPanel, BorderLayout.EAST);
        add(mainPanel, BorderLayout.CENTER);
        
        // StatusBar 추가 (SOUTH)
        add(statusBar, BorderLayout.SOUTH);
    }
    
    /**
     * 프레임 설정
     */
    private void setupFrame() {
        setTitle("MEVA - Materials Engineering Visualization and Analysis");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1024, 768));
        setLocationRelativeTo(null);
    }
    
    // ========== Event Handlers ==========
    
    private void onNewProject() {
        updateStatus("New project created");
    }
    
    private void onOpenProject() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            updateStatus("File loaded: " + fileChooser.getSelectedFile().getName());
        }
    }
    
    private void onSaveProject() {
        updateStatus("Project saved");
    }
    
    private void onSaveAsProject() {
        updateStatus("Project saved as...");
    }
    
    private void onExportData() {
        updateStatus("Data exported");
    }
    
    private void onExit() {
        System.exit(0);
    }
    
    private void onUndo() {
        updateStatus("Undo");
    }
    
    private void onRedo() {
        updateStatus("Redo");
    }
    
    private void onPreferences() {
        updateStatus("Preferences");
    }
    
    private void onZoomIn() {
        updateStatus("Zoom in");
    }
    
    private void onZoomOut() {
        updateStatus("Zoom out");
    }
    
    private void onResetZoom() {
        updateStatus("Reset zoom");
    }
    
    private void onToggleGrid() {
        updateStatus("Toggle grid");
    }
    
    private void onToggleLegend() {
        updateStatus("Toggle legend");
    }
    
    private void onCalculate() {
        onCalculateClicked();
    }
    
    private void onClearData() {
        updateStatus("Data cleared");
    }
    
    private void onDataValidator() {
        updateStatus("Data validator");
    }
    
    private void onUserGuide() {
        updateStatus("User guide");
    }
    
    private void onAbout() {
        JOptionPane.showMessageDialog(this,
            "MEVA - Materials Engineering Visualization and Analysis\n" +
            "Version 1.0\n" +
            "© 2025 MEVA Development Team",
            "About MEVA",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void onCalculateClicked() {
        updateStatus("Calculating...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        
        // TODO: 실제 계산 로직 연동
        // SimulationController.calculate();
        
        // 임시: 2초 후 완료
        Timer timer = new Timer(2000, e -> {
            progressBar.setVisible(false);
            updateStatus("Calculation completed");
        });
        timer.setRepeats(false);
        timer.start();
    }
    
    private void onResetClicked() {
        updateStatus("Input reset");
    }
    
    private void onClearGraphClicked() {
        updateStatus("Graph cleared");
    }
    
    private void onPresetChanged() {
        updateStatus("Preset changed");
    }
    
    private void onSavePreset() {
        updateStatus("Preset saved");
    }
    
    private void onDeletePreset() {
        updateStatus("Preset deleted");
    }
    
    private void onExportChart() {
        updateStatus("Chart exported");
    }
    
    /**
     * 상태바 메시지 업데이트
     */
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }
    
    /**
     * 메인 메서드 - 애플리케이션 실행
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}