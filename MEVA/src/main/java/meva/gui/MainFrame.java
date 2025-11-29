package meva.gui;

import javax.swing.*;
import java.awt.*;
import meva.fileio.TxtDataParser;
import meva.calculation.StressStrainCalculator;
import meva.models.DataPoint;
import meva.models.StressStrainPoint;
import meva.models.Experiment;
import meva.database.ExperimentDAO;
import meva.models.AnalysisResult;
import javax.swing.SwingWorker;
import java.io.IOException;
import java.util.List;
import java.time.LocalDate;
import meva.calculation.MaterialProperties;

/**
 * MEVA 애플리케이션의 메인 윈도우 프레임
 * GUI 설계 문서에 따른 완전한 구현
 * 
 * @author MEVA 개발팀
 * @version 1.0
 */
public class MainFrame extends JFrame {
    // UI 컴포넌트
    private MenuBar menuBar;              // 상단 메뉴바 (File, Edit, View 등)
    private JToolBar toolBar;             // 자주 사용하는 기능을 아이콘으로 제공하는 툴바
    private JPanel mainPanel;             // 전체 레이아웃을 담는 메인 컨테이너
    private InputPanel inputPanel;        // 좌측: 사용자 입력 패널
    private GraphPanel visualizationPanel; // 중앙: 그래프 시각화 패널
    private ResultPanel resultsPanel;     // 우측: 계산 결과 패널
    private JPanel statusBar;             // 하단: 상태 메시지 및 진행률 표시줄
    
    // 상태바 컴포넌트
    private JLabel statusLabel;   // 현재 작업 상태 텍스트 표시
    private JProgressBar progressBar; // 긴 작업(계산 등) 진행률 표시
    private JLabel timeLabel;     // 현재 시간 표시

    // 현재 실험 ID (저장된 실험 추적용)
    private int currentExperimentId = -1;
    
    // 현재 분석 결과 데이터 (재사용용)
    private AnalysisResult currentAnalysisResult;
    private boolean isManualCalculation = false; // 수동 보정 여부 추적

    /**
     * MainFrame 생성자
     */
    public MainFrame() {
        // DB 초기화
        meva.database.DatabaseManager.initializeDatabase();

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
        visualizationPanel.setResultPanel(resultsPanel); // [New] 연결
        setupResultPanelListeners();

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
        
        // 마커 기준 변경 또는 수동 재계산 시 결과 패널 업데이트
        visualizationPanel.setMarkerRefChangedListener(e -> {
            if ("RECALCULATED".equals(e.getActionCommand())) {
                // 수동 재계산인 경우
                isManualCalculation = true;
                updateResultPanelBasedOnRefMode();
                updateStatus("Manual recalculation completed");
            } else {
                // 일반적인 마커 기준 변경
                updateResultPanelBasedOnRefMode();
            }
        });
    }

    /**
     * ResultPanel 이벤트 리스너 설정
     */
    private void setupResultPanelListeners() {
        resultsPanel.setSaveButtonListener(e -> onSaveResultsClicked());
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
        Timer timer = new Timer(1000, e -> timeLabel.setText(java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))));
        timer.start();

        return panel;
    }

    /**
     * 레이아웃 설정
     * 변경사항: JSplitPane을 사용하여 패널 간 크기 조절이 가능하도록 구조 변경
     */
    private void setupLayout() {
        setLayout(new BorderLayout());

        // MenuBar 설정
        setJMenuBar(menuBar);

        // ToolBar 추가 (NORTH)
        add(toolBar, BorderLayout.NORTH);

        // 메인 패널 구성
        // 의도: 기존 BorderLayout 고정 배치 대신, 사용자가 각 패널(입력, 그래프, 결과)의
        // 너비를 작업 환경에 맞춰 유동적으로 조절할 수 있도록 JSplitPane 구조 도입
        mainPanel.add(createSplitPaneLayout(), BorderLayout.CENTER);
        
        add(mainPanel, BorderLayout.CENTER);

        // StatusBar 추가 (SOUTH)
        add(statusBar, BorderLayout.SOUTH);
    }

    /**
     * 입력, 시각화, 결과 패널을 포함하는 JSplitPane 레이아웃을 생성합니다.
     * 구조: [InputPanel] <-> [RightPanel: [VisualizationPanel] <-> [ResultsPanel]]
     * 
     * @return 구성이 완료된 최상위 JSplitPane
     */
    private JSplitPane createSplitPaneLayout() {
        // 1. 오른쪽 영역 분할 (Visualization vs Results)
        // 의도: 시각화 그래프가 가장 중요한 정보이므로 여유 공간을 모두 그래프에 할당 (Weight 1.0)
        // 결과 테이블은 preferredSize(최소값)를 유지
        JSplitPane graphResultSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                visualizationPanel, resultsPanel);
        graphResultSplit.setResizeWeight(1.0); // 그래프 패널이 남는 공간을 모두 차지
        graphResultSplit.setOneTouchExpandable(true); // 원터치 접기/펴기 기능 활성화

        // 2. 오른쪽 패널 컨테이너 (JSplitPane 래퍼)
        // 의도: 중첩된 구조를 안정적으로 배치하기 위해 JPanel로 감쌈
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(graphResultSplit, BorderLayout.CENTER);

        // 3. 전체 영역 분할 (Input vs RightPanel)
        // 의도: 입력 패널은 최소 너비(preferredSize) 유지, 나머지 공간은 오른쪽(그래프+결과)에 할당 (Weight 0.0)
        // rightPanel 내부에서 다시 그래프가 공간을 가져감 -> 최종적으로 그래프가 최대화됨
        JSplitPane leftRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                inputPanel, rightPanel);
        leftRightSplit.setResizeWeight(0.0); // 입력 패널은 고정, 오른쪽 패널 확장
        leftRightSplit.setOneTouchExpandable(true);

        return leftRightSplit;
    }

    /**
     * 프레임 설정
     */
    private void setupFrame() {
        setTitle("MEVA - Materials Engineering Visualization and Analysis");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1600, 900);
        
        // 최소 크기 설정 (각 패널의 최소 너비 합계 고려)
        // Input(280) + Graph(800) + Result(300) + Dividers/Borders ≈ 1400
        // 의도: 사용자가 설정한 각 패널의 최소 너비를 보장하기 위해 프레임 전체의 최소 크기를 제한함
        setMinimumSize(new Dimension(1400, 768));
        
        setLocationRelativeTo(null);
    }

    // ========== Event Handlers ==========

    private void onNewProject() {
        currentExperimentId = -1;
        resultsPanel.clearResults();
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
        // 1. 파일 경로 확인
        String filePath = inputPanel.getSelectedFilePath();
        if (filePath == null || filePath.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "먼저 데이터 파일을 선택해주세요.",
                    "파일 선택 필요",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        updateStatus("파일 읽는 중...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        isManualCalculation = false; // 새로운 계산 시 수동 모드 초기화

        // 백그라운드 스레드에서 처리
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private List<StressStrainPoint> stressStrainData;
            private String errorMessage;
            private AnalysisResult analysisResult; // 분석 결과 객체

            @Override
            protected Void doInBackground() throws Exception {
                try {
                    // 1. 파일 파싱
                    TxtDataParser parser = new TxtDataParser();
                    List<DataPoint> rawData = parser.parseFile(filePath);
                    System.out.println("원본 데이터 포인트: " + rawData.size());

                    // 2. 응력-변형률 변환
                    StressStrainCalculator calculator = new StressStrainCalculator();
                    List<StressStrainPoint> convertedData = calculator.convertToStressStrain(rawData);

                    // 3. 데이터 클리닝 (음수 제거 + 파단 후 제거)
                    convertedData = calculator.cleanData(convertedData);
                    convertedData = calculator.applyZeroOffset(convertedData); 

                    // 4. 노이즈 제거 (스무딩)
                    int windowSize = 20;
                    convertedData = calculator.smoothData(convertedData, windowSize);
                    System.out.println("스무딩 완료 (window size: " + windowSize + ")");

                    // ⭐ 필드에 할당 (done()에서 사용)
                    this.stressStrainData = convertedData;

                    // 5. 통합 재료 물성 분석 수행 (MaterialProperties.analyze)
                    MaterialProperties materialProps = new MaterialProperties();
                    // analyze 메서드 내부에서 UTS, Yield 등 모든 물성을 한 번에 계산
                    this.analysisResult = materialProps.analyze(stressStrainData);

                    System.out.println("\n=== 분석 완료 ===");
                    System.out.println("UTS Point: " + (analysisResult.getUtsPoint() != null ? "Found" : "Not Found"));
                    System.out.println("Yield Point: " + (analysisResult.getYieldPoint() != null ? "Found" : "Not Found"));

                    // 6. DB에 실험 데이터 저장
                    // UTS와 그 시점의 변형률(Engineering Strain)을 저장
                    if (analysisResult.getUtsPoint() != null) {
                        saveExperimentToDatabase(filePath, 
                            analysisResult.getTensileStrength(), 
                            analysisResult.getUtsPoint().getEngineeringStrain());
                    }

                } catch (IOException e) {
                    errorMessage = "파일 읽기 실패: " + e.getMessage();
                    e.printStackTrace();
                } catch (Exception e) {
                    errorMessage = "계산 중 오류 발생: " + e.getMessage();
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);

                if (errorMessage != null) {
                    JOptionPane.showMessageDialog(MainFrame.this,
                            errorMessage,
                            "오류",
                            JOptionPane.ERROR_MESSAGE);
                    updateStatus("계산 실패");
                    return;
                }

                // 4. 그래프에 데이터 및 분석 결과 표시
                visualizationPanel.plotStressStrainCurve(stressStrainData);
                visualizationPanel.setAnalysisResult(analysisResult); // ⭐ 마커 표시를 위해 필수

                // 메인 프레임의 현재 분석 결과 업데이트
                currentAnalysisResult = analysisResult;
                
                // 5. 결과 패널 업데이트 (Ref Mode 반영)
                updateResultPanelBasedOnRefMode();

                updateStatus("계산 완료 (" + stressStrainData.size() + " 데이터 포인트) - 실험 ID: " + currentExperimentId);
            }
        };

        worker.execute();
    }

    /**
     * 실험 데이터를 데이터베이스에 저장
     */
    private void saveExperimentToDatabase(String filePath, double maxStress, double strainAtMaxStress) {
        try {
            // Experiment 객체 생성
            Experiment exp = new Experiment();
            exp.setMaterialId(1); // 기본 재료 ID (TODO: 사용자 선택 기능 추가)
            exp.setSpecimenDiameter(inputPanel.getInitialDiameter());
            exp.setGaugeLength(inputPanel.getGaugeLength());
            exp.setCrossSectionArea(inputPanel.getInitialCrossSection());
            exp.setTestDate(LocalDate.now().toString());
            exp.setDataFilePath(filePath);
            exp.setRemarks("자동 저장된 실험");

            // DAO를 통해 저장
            ExperimentDAO dao = new ExperimentDAO();
            currentExperimentId = dao.saveExperiment(exp);

            if (currentExperimentId > 0) {
                // 계산 결과 저장
                dao.saveCalculationResults(currentExperimentId, maxStress, strainAtMaxStress, maxStress);
                System.out.println("실험 데이터 DB 저장 성공 (ID: " + currentExperimentId + ")");
            } else {
                System.err.println("실험 데이터 DB 저장 실패");
            }
        } catch (Exception e) {
            System.err.println("데이터베이스 저장 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Save Results 버튼 클릭 이벤트
     */
    private void onSaveResultsClicked() {
        // 1. 계산된 결과 데이터가 있는지 확인 (테이블 모델에서 확인)
        if (resultsPanel.getTableModel().getRowCount() == 0 ||
                resultsPanel.getTableModel().getValueAt(0, 1).equals("-")) {
            JOptionPane.showMessageDialog(this,
                    "먼저 Calculate 버튼을 눌러 계산을 수행하세요.",
                    "계산 필요",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 2. CSV 파일 저장 다이얼로그 열기
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("결과 저장 (CSV)");

        // 기본 파일명 설정
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss");
        String timeStamp = sdf.format(new java.util.Date());
        String defaultFileName = "MEVA_Results_" + timeStamp + ".csv";
        fileChooser.setSelectedFile(new java.io.File(defaultFileName));

        // CSV 필터 설정
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV 파일", "csv"));

        int userSelection = fileChooser.showSaveDialog(this);

        // 3. 사용자가 저장을 눌렀을 때 실제 파일 쓰기
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToSave = fileChooser.getSelectedFile();

            // 확장자가 없으면 자동으로 .csv 붙여주기
            if (!fileToSave.getAbsolutePath().endsWith(".csv")) {
                fileToSave = new java.io.File(fileToSave.getAbsolutePath() + ".csv");
            }

            try (java.io.PrintWriter writer = new java.io.PrintWriter(fileToSave)) {
                // CSV 헤더 작성
                writer.println("Property,Value,Unit");

                // 결과 데이터 작성 (테이블 모델에서 읽어오기)
                javax.swing.table.DefaultTableModel model = resultsPanel.getTableModel();
                for (int i = 0; i < model.getRowCount(); i++) {
                    Object prop = model.getValueAt(i, 0);
                    Object val = model.getValueAt(i, 1);
                    Object unit = model.getValueAt(i, 2);
                    writer.println(String.format("\"%s\",\"%s\",\"%s\"", prop, val, unit));
                }

                JOptionPane.showMessageDialog(this,
                        "파일이 성공적으로 저장되었습니다:\n" + fileToSave.getAbsolutePath(),
                        "저장 완료",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "파일 저장 중 오류가 발생했습니다: " + e.getMessage(),
                        "저장 오류",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onResetClicked() {
        currentExperimentId = -1;
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
     * 현재 마커 기준(Engineering/True)에 따라 결과 패널의 값을 업데이트합니다.
     */
    private void updateResultPanelBasedOnRefMode() {
        if (currentAnalysisResult == null) return;

        int refMode = visualizationPanel.getMarkerRefMode();
        boolean useEngineering = (refMode == 0);
        String modeSuffix = useEngineering ? " (Eng)" : " (True)";
        if (isManualCalculation) modeSuffix += " (Manual)";
        
        resultsPanel.setTitleText("Results" + modeSuffix);

        // Helper to get stress value
        java.util.function.Function<StressStrainPoint, Double> getStress = p -> 
            (p == null) ? 0.0 : (useEngineering ? p.getEngineeringStress() : p.getTrueStress());
            
        // Helper to get strain value
        java.util.function.Function<StressStrainPoint, Double> getStrain = p -> 
            (p == null) ? 0.0 : (useEngineering ? p.getEngineeringStrain() : p.getTrueStrain());

        // 1. 항복점 라벨 및 포인트 결정 (동적 반영)
        String yieldLabel = "항복 강도 (σy)";
        StressStrainPoint yieldPoint = null;
        
        // 현재 선택된 모드 확인 (0: Auto, 1: Offset, 2: Upper/Lower)
        int selectedMode = visualizationPanel.getSelectedYieldMode();
        
        if (selectedMode == 1) { // Force 0.2% Offset
            yieldLabel += " (0.2% Offset)";
            yieldPoint = useEngineering ? 
                currentAnalysisResult.getOffsetYieldPointEng() : 
                currentAnalysisResult.getOffsetYieldPoint();
        } else if (selectedMode == 2) { // Force Upper/Lower
            yieldLabel += " (Upper Yield)";
            yieldPoint = currentAnalysisResult.getUpperYieldPoint();
        } else { // Auto
            // AnalysisResult의 yieldType에 따라 결정
            if (currentAnalysisResult.getYieldType() == AnalysisResult.YieldType.OFFSET_02) {
                yieldLabel += " (0.2% Offset)";
                yieldPoint = useEngineering ? 
                    currentAnalysisResult.getOffsetYieldPointEng() : 
                    currentAnalysisResult.getOffsetYieldPoint();
            } else {
                yieldLabel += " (Upper Yield)";
                yieldPoint = currentAnalysisResult.getUpperYieldPoint();
            }
        }
        
        // 만약 선택된 포인트가 null이면 기본값(대표값) 사용 (안전장치)
        if (yieldPoint == null) {
            yieldPoint = currentAnalysisResult.getYieldPoint();
        }

        // 2. 값 계산 (현재 모드 반영)
        double tensileStrength = getStress.apply(currentAnalysisResult.getUtsPoint());
        double strainAtMax = getStrain.apply(currentAnalysisResult.getUtsPoint());
        double yieldStrength = getStress.apply(yieldPoint);
        double fractureStress = getStress.apply(currentAnalysisResult.getFracturePoint());
        double fractureStrain = getStrain.apply(currentAnalysisResult.getFracturePoint());
        
        double youngsModulus = useEngineering ? currentAnalysisResult.getYoungsModulusEng() : currentAnalysisResult.getYoungsModulus();
        if (useEngineering && youngsModulus == 0.0 && currentAnalysisResult.getYoungsModulus() > 0) {
             youngsModulus = currentAnalysisResult.getYoungsModulus(); // Fallback
        }

        // 2. 결과 리스트 동적 생성
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        
        rows.add(new Object[]{ "최대 응력 (σmax)", String.format("%.2f", tensileStrength), "MPa" });
        rows.add(new Object[]{ "최대 응력 시 변형률 (εmax)", String.format("%.4f", strainAtMax), "-" });
        rows.add(new Object[]{ "극한 인장 강도 (UTS)", String.format("%.2f", tensileStrength), "MPa" });
        rows.add(new Object[]{ "영률 (E)", String.format("%.2f", youngsModulus), "GPa" });

        // [수정됨] 항복점 표시 로직 분기
        boolean showDiscontinuous = false;
        
        if (selectedMode == 2) { // Force Upper/Lower
            showDiscontinuous = true;
        } else if (selectedMode == 0) { // Auto
            if (currentAnalysisResult.getYieldType() == AnalysisResult.YieldType.DISCONTINUOUS) {
                showDiscontinuous = true;
            }
        }
        
        if (showDiscontinuous) {
            // 상/하항복점 모두 표시
            double upperVal = getStress.apply(currentAnalysisResult.getUpperYieldPoint());
            double lowerVal = getStress.apply(currentAnalysisResult.getLowerYieldPoint());
            
            rows.add(new Object[]{ "상항복 강도 (Upper Yield)", String.format("%.2f", upperVal), "MPa" });
            rows.add(new Object[]{ "하항복 강도 (Lower Yield)", String.format("%.2f", lowerVal), "MPa" });
        } else {
            // 0.2% Offset 표시
            StressStrainPoint offsetPoint = useEngineering ? 
                currentAnalysisResult.getOffsetYieldPointEng() : 
                currentAnalysisResult.getOffsetYieldPoint();
                
            // 만약 null이면 대표값 사용
            if (offsetPoint == null) offsetPoint = currentAnalysisResult.getYieldPoint();
            
            double offsetVal = getStress.apply(offsetPoint);
            rows.add(new Object[]{ "항복 강도 (0.2% Offset)", String.format("%.2f", offsetVal), "MPa" });
        }

        // 나머지 공통 물성치 추가
        rows.add(new Object[]{ "연신율", String.format("%.2f", currentAnalysisResult.getElongation()), "%" });
        rows.add(new Object[]{ "단면 감소율", String.format("%.2f", currentAnalysisResult.getReductionOfArea()), "%" });
        rows.add(new Object[]{ "인성", String.format("%.2f", currentAnalysisResult.getToughness()), "MJ/m³" });
        rows.add(new Object[]{ "탄성 에너지", String.format("%.2f", currentAnalysisResult.getResilience()), "MJ/m³" });
        rows.add(new Object[]{ "탄성 한계", currentAnalysisResult.getElasticLimit() > 0 ? String.format("%.2f", currentAnalysisResult.getElasticLimit()) : "-", "MPa" });
        rows.add(new Object[]{ "비례 한계", currentAnalysisResult.getProportionalLimit() > 0 ? String.format("%.2f", currentAnalysisResult.getProportionalLimit()) : "-", "MPa" });
        rows.add(new Object[]{ "네킹 시작 변형률", currentAnalysisResult.getNeckingStartStrain() > 0 ? String.format("%.4f", currentAnalysisResult.getNeckingStartStrain()) : "-", "-" });
        rows.add(new Object[]{ "파괴 응력", fractureStress > 0 ? String.format("%.2f", fractureStress) : "-", "MPa" });
        rows.add(new Object[]{ "파괴 변형률", fractureStrain > 0 ? String.format("%.4f", fractureStrain) : "-", "-" });

        // 3. 테이블 업데이트 (List -> Array 변환)
        Object[][] resultsData = rows.toArray(new Object[0][]);
        resultsPanel.updateResults(resultsData);
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
