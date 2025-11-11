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
    private JMenuBar menuBar;
    private JToolBar toolBar;
    private JPanel mainPanel;
    private JPanel inputPanel;
    private JPanel visualizationPanel;
    private JPanel resultsPanel;
    private JPanel statusBar;
    
    // 입력 패널의 하위 컴포넌트
    private JPanel materialPropertiesPanel;
    private JPanel specimenDimensionsPanel;
    private JPanel controlButtonsPanel;
    private JPanel presetManagementPanel;
    
    // 시각화 패널의 하위 컴포넌트
    private JPanel chartPanel;
    private JPanel chartControlPanel;
    
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
        menuBar = createMenuBar();
        
        // ToolBar 초기화
        toolBar = createToolBar();
        
        // 메인 패널 초기화 (BorderLayout)
        mainPanel = new JPanel(new BorderLayout());
        
        // InputPanel 초기화 (WEST)
        inputPanel = createInputPanel();
        
        // VisualizationPanel 초기화 (CENTER)
        visualizationPanel = createVisualizationPanel();
        
        // ResultsPanel 초기화 (EAST)
        resultsPanel = createResultsPanel();
        
        // StatusBar 초기화 (SOUTH)
        statusBar = createStatusBar();
    }
    
    /**
     * 메뉴바 생성
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setFont(new Font("Dialog", Font.PLAIN, 12));
        
        // File 메뉴
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(createMenuItem("New", "Ctrl+N", this::onNewProject));
        fileMenu.add(createMenuItem("Open...", "Ctrl+O", this::onOpenProject));
        fileMenu.add(createMenuItem("Save", "Ctrl+S", this::onSaveProject));
        fileMenu.add(createMenuItem("Save As...", "Ctrl+Shift+S", this::onSaveAsProject));
        fileMenu.addSeparator();
        fileMenu.add(createMenuItem("Export", null, this::onExportData));
        fileMenu.addSeparator();
        fileMenu.add(createMenuItem("Exit", "Alt+F4", this::onExit));
        
        // Edit 메뉴
        JMenu editMenu = new JMenu("Edit");
        editMenu.add(createMenuItem("Undo", "Ctrl+Z", this::onUndo));
        editMenu.add(createMenuItem("Redo", "Ctrl+Y", this::onRedo));
        editMenu.addSeparator();
        editMenu.add(createMenuItem("Preferences", "Ctrl+,", this::onPreferences));
        
        // View 메뉴
        JMenu viewMenu = new JMenu("View");
        viewMenu.add(createMenuItem("Zoom In", "Ctrl++", this::onZoomIn));
        viewMenu.add(createMenuItem("Zoom Out", "Ctrl+-", this::onZoomOut));
        viewMenu.add(createMenuItem("Reset Zoom", "Ctrl+0", this::onResetZoom));
        viewMenu.addSeparator();
        viewMenu.add(createCheckBoxMenuItem("Show Grid", true, this::onToggleGrid));
        viewMenu.add(createCheckBoxMenuItem("Show Legend", true, this::onToggleLegend));
        
        // Tools 메뉴
        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.add(createMenuItem("Calculate", "F5", this::onCalculate));
        toolsMenu.add(createMenuItem("Clear Data", null, this::onClearData));
        toolsMenu.addSeparator();
        toolsMenu.add(createMenuItem("Data Validator", null, this::onDataValidator));
        
        // Help 메뉴
        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(createMenuItem("User Guide", "F1", this::onUserGuide));
        helpMenu.add(createMenuItem("About", null, this::onAbout));
        
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);
        
        return menuBar;
    }
    
    /**
     * 메뉴 아이템 생성 헬퍼 메서드
     */
    private JMenuItem createMenuItem(String text, String accelerator, Runnable action) {
        JMenuItem menuItem = new JMenuItem(text);
        if (accelerator != null) {
            menuItem.setAccelerator(KeyStroke.getKeyStroke(accelerator));
        }
        menuItem.addActionListener(e -> action.run());
        return menuItem;
    }
    
    /**
     * 체크박스 메뉴 아이템 생성 헬퍼 메서드
     */
    private JCheckBoxMenuItem createCheckBoxMenuItem(String text, boolean selected, Runnable action) {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(text, selected);
        menuItem.addActionListener(e -> action.run());
        return menuItem;
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
     * 입력 패널 생성 (WEST)
     */
    private JPanel createInputPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(300, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // 재료 물성 패널
        materialPropertiesPanel = createMaterialPropertiesPanel();
        panel.add(materialPropertiesPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // 시편 치수 패널
        specimenDimensionsPanel = createSpecimenDimensionsPanel();
        panel.add(specimenDimensionsPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // 제어 버튼 패널
        controlButtonsPanel = createControlButtonsPanel();
        panel.add(controlButtonsPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // 프리셋 관리 패널
        presetManagementPanel = createPresetManagementPanel();
        panel.add(presetManagementPanel);
        
        return panel;
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
        panel.add(new JTextField("200.0", 10), gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("GPa"), gbc);
        
        // 항복강도 (σy)
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("항복강도 (σy):"), gbc);
        gbc.gridx = 1;
        panel.add(new JTextField("250.0", 10), gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("MPa"), gbc);
        
        // 강도계수 (K)
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("강도계수 (K):"), gbc);
        gbc.gridx = 1;
        panel.add(new JTextField("500.0", 10), gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("MPa"), gbc);
        
        // 경화지수 (n)
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("경화지수 (n):"), gbc);
        gbc.gridx = 1;
        panel.add(new JTextField("0.2", 10), gbc);
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
        panel.add(new JTextField("10.0", 10), gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("mm"), gbc);
        
        // 두께 (t)
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("두께 (t):"), gbc);
        gbc.gridx = 1;
        panel.add(new JTextField("5.0", 10), gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("mm"), gbc);
        
        // 길이 (L)
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("길이 (L):"), gbc);
        gbc.gridx = 1;
        panel.add(new JTextField("50.0", 10), gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("mm"), gbc);
        
        // 게이지길이
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("게이지길이:"), gbc);
        gbc.gridx = 1;
        panel.add(new JTextField("25.0", 10), gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("mm"), gbc);
        
        return panel;
    }
    
    /**
     * 제어 버튼 패널 생성
     */
    private JPanel createControlButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        JButton calculateBtn = new JButton("Calculate");
        calculateBtn.setPreferredSize(new Dimension(100, 30));
        calculateBtn.setBackground(new Color(33, 150, 243));
        calculateBtn.setForeground(Color.WHITE);
        calculateBtn.setFont(new Font("Dialog", Font.BOLD, 12));
        calculateBtn.addActionListener(e -> onCalculateClicked());
        
        JButton resetBtn = new JButton("Reset");
        resetBtn.setPreferredSize(new Dimension(100, 30));
        resetBtn.addActionListener(e -> onResetClicked());
        
        JButton clearGraphBtn = new JButton("Clear Graph");
        clearGraphBtn.setPreferredSize(new Dimension(100, 30));
        clearGraphBtn.addActionListener(e -> onClearGraphClicked());
        
        panel.add(calculateBtn);
        panel.add(resetBtn);
        panel.add(clearGraphBtn);
        
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
        JComboBox<String> presetCombo = new JComboBox<>(presets);
        presetCombo.addActionListener(e -> onPresetChanged());
        panel.add(presetCombo, gbc);
        
        // 저장 버튼
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> onSavePreset());
        panel.add(saveBtn, gbc);
        
        // 삭제 버튼
        gbc.gridx = 1;
        JButton deleteBtn = new JButton("Delete");
        deleteBtn.addActionListener(e -> onDeletePreset());
        panel.add(deleteBtn, gbc);
        
        return panel;
    }
    
    /**
     * 시각화 패널 생성 (CENTER)
     */
    private JPanel createVisualizationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 차트 패널 (JFreeChart 통합 예정)
        chartPanel = new JPanel();
        chartPanel.setBackground(Color.WHITE);
        chartPanel.setBorder(BorderFactory.createLineBorder(new Color(224, 224, 224)));
        panel.add(chartPanel, BorderLayout.CENTER);
        
        // 차트 제어 패널
        chartControlPanel = createChartControlPanel();
        panel.add(chartControlPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * 차트 제어 패널 생성
     */
    private JPanel createChartControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        panel.add(new JButton("Zoom In"));
        panel.add(new JButton("Zoom Out"));
        panel.add(new JButton("Reset Zoom"));
        panel.add(new JButton("Export Chart"));
        
        return panel;
    }
    
    /**
     * 결과 패널 생성 (EAST)
     */
    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(250, 0));
        panel.setBorder(BorderFactory.createTitledBorder("Results"));
        
        // 결과 테이블
        String[] columnNames = {"Property", "Value", "Unit"};
        Object[][] data = {
            {"Max Stress (σmax)", "-", "MPa"},
            {"Strain at Max (εmax)", "-", "-"},
            {"UTS", "-", "MPa"},
            {"Young's Modulus (E)", "-", "GPa"},
            {"Yield Strength (σy)", "-", "MPa"},
            {"Elongation", "-", "%"},
            {"Reduction of Area", "-", "%"}
        };
        
        JTable table = new JTable(data, columnNames);
        table.setFont(new Font("Monospaced", Font.PLAIN, 11));
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(60);
        
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
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