// src/main/java/meva/gui/MainFrame.java

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
    // private MenuBar menuBar; // [Removed] UI Optimization
    // private JToolBar toolBar; // [Removed] UI Optimization
    private JPanel mainPanel; // 전체 레이아웃을 담는 메인 컨테이너
    private InputPanel inputPanel; // 좌측: 사용자 입력 패널
    private GraphPanel visualizationPanel; // 중앙: 그래프 시각화 패널
    private ResultPanel resultsPanel; // 우측: 계산 결과 패널
    private JPanel statusBar; // 하단: 상태 메시지 및 진행률 표시줄

    // 상태바 컴포넌트
    private JLabel statusLabel; // 현재 작업 상태 텍스트 표시
    private JProgressBar progressBar; // 긴 작업(계산 등) 진행률 표시
    private JLabel timeLabel; // 현재 시간 표시

    // 현재 실험 ID (저장된 실험 추적용)
    private int currentExperimentId = -1;

    // 현재 분석 결과 데이터 (재사용용)
    private AnalysisResult currentAnalysisResult;

    private List<DataPoint> currentRawData; // [New] 원본 데이터 캐싱 (재계산용)
    private double loadCorrectionFactor = 1.0; // [New] 하중 단위 자동 보정 계수

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
        // MenuBar & ToolBar 초기화 제거 (UI Optimization)

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
     * InputPanel 이벤트 리스너 설정
     */
    private void setupInputPanelListeners() {
        inputPanel.setCalculateListener(this::onCalculateClicked);
        inputPanel.setResetListener(e -> onResetClicked());
        inputPanel.setSaveExperimentListener(e -> onSaveExperimentClicked());
        inputPanel.addExperimentLoadedListener(this::onExperimentLoaded); // [New] 실험 로드 리스너 연결
    }

    /**
     * 실험 데이터 로드 시 호출됨
     */
    private void onExperimentLoaded(Experiment exp) {
        if (exp != null) {
            // [버그수정] 이전 그래프 초기화 후 새 데이터 로드
            visualizationPanel.clearGraph();
            resultsPanel.clearResults();
            currentRawData = null; // 캐시 초기화

            currentExperimentId = exp.getId(); // 로드된 ID 설정
            updateStatus("Load complete: ID " + currentExperimentId);
            // 저장 없이 분석 수행 및 그래프 표시
            performAnalysis(false);
        }
    }

    /**
     * GraphPanel 이벤트 리스너 설정
     */
    private void setupGraphPanelListeners() {
        // Zoom/Export listeners are internal to GraphPanel now or don't need MainFrame
        // feedback.

        // 마커 기준 변경 또는 수동 재계산 시 결과 패널 업데이트
        visualizationPanel.setMarkerRefChangedListener(e -> {
            if ("RECALCULATED".equals(e.getActionCommand())) {
                // 수동 재계산 완료 상태 표시
                updateStatus("수동 재계산 완료");
            }
            // 일반적인 변경은 GraphPanel 내부에서 ResultPanel.updateMode()를 통해 처리됨
        });
    }

    /**
     * ResultPanel 이벤트 리스너 설정
     */
    private void setupResultPanelListeners() {
        // ResultPanel 내부에서 저장을 처리하므로 MainFrame 리스너는 제거 또는 로깅만 수행
        // resultsPanel.setSaveButtonListener(e -> onSaveResultsClicked());
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

        // 오른쪽 패널 (시간 + 프로그램 정보 버튼)
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        rightPanel.setOpaque(false);

        // 시간 레이블
        timeLabel = new JLabel(java.time.LocalTime.now().toString());
        timeLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
        rightPanel.add(timeLabel);

        // 프로그램 정보 버튼
        JButton aboutButton = new JButton("ℹ 프로그램 정보");
        aboutButton.setFont(new Font("Dialog", Font.PLAIN, 10));
        aboutButton.setMargin(new Insets(1, 5, 1, 5));
        aboutButton.setFocusPainted(false);
        aboutButton.addActionListener(e -> AboutDialog.showDialog(this));
        rightPanel.add(aboutButton);

        panel.add(rightPanel, BorderLayout.EAST);

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

        // MenuBar 설정 제거
        // setJMenuBar(menuBar);

        // ToolBar 추가 (NORTH) 제거
        // add(toolBar, BorderLayout.NORTH);

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
        leftRightSplit.setDividerLocation(360); // [New] 초기 너비 확보 (350 + 여유)

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

    // [Removed] Placeholder event handlers for MenuBar/ToolBar

    private void onCalculateClicked(java.awt.event.ActionEvent event) {
        // 버튼 클릭(또는 수동 실행)은 DB 저장을 원칙으로 함 (단, 재계산인 경우 로직 따름)
        // 여기서는 기존 로직 유지를 위해 '저장 모드'로 실행
        performAnalysis(true);
    }

    /**
     * 실제 분석 수행 메서드 (Save Flag 추가)
     */
    private void performAnalysis(boolean saveToDb) {
        // 1. 파일 경로 확인
        String filePath = inputPanel.getSelectedFilePath();

        // [New] 재계산 여부는 saveToDb가 true이면서 기존 데이터가 있는 경우 등으로 판단 가능하나
        // 여기서는 단순화하여 UI 상태 업데이트 위주로 처리
        boolean isRecalculate = (currentAnalysisResult != null); // 결과가 이미 있으면 재계산으로 간주 (단, 로드는 제외)

        // 로드 모드(saveToDb=false)일 때는 재계산 플래그를 false로 두어 파일 우선 로드 (또는 필요시 조정)
        if (!saveToDb) {
            isRecalculate = false;
        }

        // 재계산 요청이 들어왔으나 캐시된 데이터가 없으면 경고 후 일반 모드(파일 로드)로 전환 시도
        // 하지만 파일 경로가 없으면 에러
        if (filePath == null || filePath.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "먼저 데이터 파일을 선택해주세요.",
                    "파일 선택 필요",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        updateStatus(isRecalculate ? "재계산 중..." : "파일 읽는 중...");
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        // 데이터 준비 (EDT에서 UI 값 읽기)
        final double userArea = inputPanel.getInitialCrossSection();
        final double userLength = inputPanel.getGaugeLength();
        final double userDiameter = inputPanel.getInitialDiameter(); // [New] For Formula Display
        final Double finalAreaObj = inputPanel.getFinalCrossSectionArea();
        final double finalArea = (finalAreaObj != null) ? finalAreaObj : 0.0;

        // 백그라운드 스레드에서 처리
        boolean finalIsRecalculate = isRecalculate;
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private List<StressStrainPoint> stressStrainData;
            private String errorMessage;
            private AnalysisResult analysisResult;

            @Override
            protected Void doInBackground() throws Exception {
                try {
                    List<DataPoint> rawData;
                    StressStrainCalculator calculator = new StressStrainCalculator();

                    // 1. 데이터 준비 (캐시 사용 또는 파일 로드)
                    if (finalIsRecalculate && currentRawData != null) {
                        rawData = currentRawData;
                        System.out.println("캐시된 원본 데이터 사용: " + rawData.size());
                    } else {
                        TxtDataParser parser = new TxtDataParser();
                        rawData = parser.parseFile(filePath);
                        currentRawData = rawData; // [New] 캐싱
                        System.out.println("파일에서 원본 데이터 로드: " + rawData.size());
                    }

                    // 2. 응력-변형률 변환
                    // [New] 만약 재계산(Recalculate)이거나, 초기 직경/게이지 길이가 기본값과 다르면 수동 계산 시도
                    // (userArea, userLength는 상위 스코프의 final 변수 사용)

                    List<StressStrainPoint> convertedData;

                    // 재계산 버튼을 명시적으로 눌렀을 때만 보정된 계산 수행
                    // 최초 로딩 시에는 파일 내의 Stress 값을 우선 사용하고, 추후를 위해 Factor를 계산해둠
                    if (finalIsRecalculate) {
                        System.out.println("사용자 입력 치수 및 보정 계수로 재계산 수행 (Area: "
                                + String.format("%.2f", userArea) + ", LoadFactor: "
                                + String.format("%.2f", loadCorrectionFactor) + ")");
                        convertedData = calculator.calculateFromRawData(rawData, userArea, userLength,
                                loadCorrectionFactor);
                    } else {
                        // 최초 로드: 파일 내 Stress 데이터 사용 (정확도 보장)
                        convertedData = calculator.convertToStressStrain(rawData);

                        // [Auto-Correction] 파일의 Stress 값과 사용자가 입력한(또는 기본값) Area 기반의 Load 값을 비교하여
                        // 향후 재계산에 사용할 Load Unit Correction Factor를 계산해둠.
                        if (userArea > 0) {
                            loadCorrectionFactor = calculator.calculateCorrectionFactor(rawData, convertedData,
                                    userArea);
                            System.out.println("Load Correction Factor 계산됨: " + loadCorrectionFactor);
                        }
                    }

                    // 3. 데이터 클리닝
                    convertedData = calculator.cleanData(convertedData);
                    convertedData = calculator.applyZeroOffset(convertedData);

                    // 4. 노이즈 제거
                    int windowSize = 20;
                    convertedData = calculator.smoothData(convertedData, windowSize); // Method name corrected if it was
                                                                                      // different

                    this.stressStrainData = convertedData;

                    // 5. 물성 분석
                    MaterialProperties materialProps = new MaterialProperties();
                    // 초기 단면적과 최종 단면적을 전달하여 RA 계산 지원
                    this.analysisResult = materialProps.analyze(stressStrainData, userArea, finalArea);

                    // [New] 메타데이터 주입
                    this.analysisResult.setExperimentName(inputPanel.getMaterialName()); // 재료명을 실험명으로 사용
                    this.analysisResult.setExperimenter(inputPanel.getTesterName());
                    this.analysisResult.setRemarks(inputPanel.getRemarks());
                    this.analysisResult.setTestDate(inputPanel.getTestDate());

                    // [New] Raw Data for Formula Substitution
                    this.analysisResult.setInitialLength(userLength);
                    this.analysisResult.setInitialArea(userArea);
                    this.analysisResult.setFinalArea(finalArea);
                    this.analysisResult.setInitialDiameter(userDiameter);
                    // Final Diameter Estimate (assuming circular)
                    if (finalArea > 0) {
                        this.analysisResult.setFinalDiameter(Math.sqrt(4 * finalArea / Math.PI));
                    }
                    // Max Load Calculation (Derived from UTS * Area) [N]
                    if (this.analysisResult.getUtsPoint() != null) {
                        // Use Engineering Stress which is P/A0
                        double maxStressEng = this.analysisResult.getUtsPoint().getEngineeringStress();
                        this.analysisResult.setMaxLoad(maxStressEng * userArea);
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

                visualizationPanel.plotStressStrainCurve(stressStrainData);
                visualizationPanel.setAnalysisResult(analysisResult);
                currentAnalysisResult = analysisResult;

                // [New] ResultPanel에 시편 정보 설정 (CSV 내보내기용)
                resultsPanel.setSpecimenInfo(
                        userDiameter,
                        userLength,
                        userArea,
                        inputPanel.getTestSpeed());

                // [변경] 계산 버튼은 계산만 수행, 저장은 별도 버튼으로 분리됨
                updateStatus("계산 완료 (" + stressStrainData.size() + " 데이터 포인트)");
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
            exp.setMaterialName(inputPanel.getMaterialName()); // 사용자 입력값

            exp.setSpecimenDiameter(inputPanel.getInitialDiameter());
            exp.setGaugeLength(inputPanel.getGaugeLength());
            exp.setCrossSectionArea(inputPanel.getInitialCrossSection());
            exp.setFinalCrossSectionArea(inputPanel.getFinalCrossSectionArea());

            exp.setTestDate(inputPanel.getTestDate());
            exp.setTesterName(inputPanel.getTesterName());
            exp.setTestMethod(inputPanel.getTestMethod());

            exp.setDataFilePath(filePath);
            exp.setRemarks(inputPanel.getRemarks());
            exp.setTestSpeed(inputPanel.getTestSpeed()); // [New] 인장속도

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
     * 실험 저장 버튼 클릭 핸들러
     */
    private void onSaveExperimentClicked() {
        // 계산 결과가 없으면 저장 불가
        if (currentAnalysisResult == null) {
            JOptionPane.showMessageDialog(this,
                    "먼저 계산을 수행해주세요.",
                    "저장 불가",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String filePath = inputPanel.getSelectedFilePath();
        if (filePath == null || filePath.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "데이터 파일이 선택되지 않았습니다.",
                    "저장 불가",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // DB에 저장
        saveExperimentToDatabase(filePath,
                currentAnalysisResult.getTensileStrength(),
                currentAnalysisResult.getUtsPoint() != null
                        ? currentAnalysisResult.getUtsPoint().getEngineeringStrain()
                        : 0.0);

        JOptionPane.showMessageDialog(this,
                "실험 데이터가 저장되었습니다. (ID: " + currentExperimentId + ")",
                "저장 완료",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void onResetClicked() {
        currentExperimentId = -1;
        resultsPanel.clearResults();
        visualizationPanel.clearGraph();
        updateStatus("Input reset");
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
