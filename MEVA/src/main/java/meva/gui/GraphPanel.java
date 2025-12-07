// src/main/java/meva/gui/GraphPanel.java

package meva.gui;

import meva.charting.core.ChartManager;
import meva.charting.interactions.ChartInputHandler;
import meva.calculation.MaterialProperties;
import meva.models.AnalysisResult;
import meva.models.StressStrainPoint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.border.EmptyBorder;
import meva.education.GlossaryManager;
import java.util.Map;
import java.util.List;

/**
 * [응력-변형률 그래프 패널]
 * - UI 레이아웃(버튼, 체크박스 등) 구성 및 사용자 인터랙션 담당
 * - 실제 차트 로직 및 시각화는 {@link ChartManager}에게 위임 (Facade 패턴 적용)
 * 
 * @author MEVA 개발팀
 * @version 2.0 - Refactored with ChartManager
 */
public class GraphPanel extends JPanel implements ChartInputHandler.InteractionListener {

    // --- 핵심 로직 위임 객체 ---
    private final ChartManager chartManager;

    // --- UI 컴포넌트 ---
    private JPanel graphControlPanel; // 하단 제어 영역 전체 (SOUTH)
    private JPanel placeholderPanel; // 차트가 없을 때 표시할 안내 패널
    private JPanel chartContainerPanel; // 차트가 들어갈 중앙 패널

    // 옵션 체크박스들
    private JCheckBox utsCheckBox;
    private JCheckBox yieldCheckBox;
    private JComboBox<String> yieldModeComboBox;
    private JComboBox<String> markerRefComboBox;
    private JCheckBox slopeLineCheckBox;
    // private JCheckBox elasticRegionCheckBox; // [Removed] Redundant with
    // Toughness Highlight
    // private JCheckBox plasticRegionCheckBox; // [Removed] Redundant with
    // Toughness Highlight
    // private JCheckBox elasticRegionCheckBox; // [Removed] Redundant with
    // Toughness Highlight
    // private JCheckBox plasticRegionCheckBox; // [Removed] Redundant with
    // Toughness Highlight

    // 신규 추가: 에너지 시각화 체크박스
    private JCheckBox resilienceCheckBox;
    private JCheckBox toughnessCheckBox;
    private JComboBox<String> resilienceModeComboBox;
    private JCheckBox unloadingLineCheckBox; // [New] Unloading Line Toggle // 신규 추가: 모드 선택

    // 차트 제어 버튼들
    private JButton zoomInButton;
    private JButton zoomOutButton;
    private JButton resetZoomButton;
    private JButton exportChartButton;

    // 이벤트 리스너들
    private ActionListener zoomInListener;
    private ActionListener zoomOutListener;
    private ActionListener resetZoomListener;
    private ActionListener exportChartListener;
    private ActionListener markerRefChangedListener;

    // 외부 의존성
    private ResultPanel resultPanel;
    private MaterialProperties materialCalculator = new MaterialProperties();

    // 현재 상태 데이터
    private List<StressStrainPoint> currentData;
    private AnalysisResult currentResult;

    /**
     * GraphPanel 생성자
     */
    public GraphPanel() {
        // 1. ChartManager 초기화 (차트 로직 로드)
        this.chartManager = new ChartManager();
        this.chartManager.setInteractionListener(this);

        // 2. 툴팁 지속 시간 설정
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

        // 3. UI 구성
        initializeComponents();
        setupLayout();
    }

    public void setResultPanel(ResultPanel panel) {
        this.resultPanel = panel;
    }

    // =================================================================================
    // 1. UI 초기화 및 레이아웃
    // =================================================================================

    private void initializeComponents() {
        // --- 하단 제어 패널 (옵션 및 버튼) ---
        graphControlPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        createUIComponents(); // 버튼/체크박스 생성

        // [Row 1] 옵션 그룹
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        utsCheckBox.setToolTipText("극한 인장 강도(UTS) 지점을 표시합니다.");
        row1.add(utsCheckBox); // UTS

        JPanel yieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); // Yield
        yieldCheckBox.setToolTipText("<html><b>[항복점 모드]</b><br>실험 조건에 따라 자동 감지가 부정확할 수 있습니다. 필요 시 수동 변경하세요.</html>");
        yieldPanel.add(yieldCheckBox);
        yieldPanel.add(yieldModeComboBox);
        row1.add(yieldPanel);

        JPanel slopePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); // Slope
        slopeLineCheckBox
                .setToolTipText("<html><b>[탄성 구간 조절]</b><br>파란색 점선 끝의 <b>네모 핸들</b>을 드래그하여 직선 구간을 수동 설정하세요.</html>");
        slopePanel.add(slopeLineCheckBox);
        row1.add(slopePanel);

        JPanel refPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); // Marker Ref
        refPanel.add(new JLabel("마커 기준:"));
        refPanel.add(markerRefComboBox);
        row1.add(refPanel);

        // [Row 2] 영역 표시 및 줌 버튼
        JPanel row2 = new JPanel(new BorderLayout());
        JPanel row2Left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        // row2Left.add(elasticRegionCheckBox); // (기존 탄/소성 체크박스 제거 또는 유지)
        // row2Left.add(plasticRegionCheckBox);

        // 에너지 시각화 체크박스 배치
        JPanel resPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        resPanel.add(resilienceCheckBox);
        resPanel.add(resilienceModeComboBox);

        resilienceCheckBox.setToolTipText("<html><b>[탄성 에너지 (Resilience)]</b><br>재료가 영구 변형 없이 저장할 수 있는 에너지입니다.<br>" +
                "- <b>Triangle:</b> 훅의 법칙을 가정한 이론적 값 (삼각형)<br>" +
                "- <b>Integral:</b> 실제 실험 데이터를 적분한 값 (곡선 아래 면적)</html>");

        row2Left.add(resPanel);

        toughnessCheckBox.setToolTipText(
                "<html><b>[영역 표시 (인성)]</b><br>그래프의 전체 면적을 탄성(초록)/소성(주황) 구간으로 나누어 시각화합니다.<br>(전체 면적 = 인성)</html>");
        row2Left.add(toughnessCheckBox);

        JPanel extraPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        unloadingLineCheckBox.setToolTipText(
                "<html><b>[탄성 회복선 (Unloading Line)]</b><br>파단점에서 초기 영률(Elastic Slope)과 평행하게 그은 선입니다.<br>X축 교차점이 소성 연신율(Plastic Elongation)입니다.</html>");
        extraPanel.add(unloadingLineCheckBox);
        row2Left.add(extraPanel);

        JPanel row2Right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        row2Right.add(zoomInButton);
        row2Right.add(zoomOutButton);
        row2Right.add(resetZoomButton);

        // [New] 용어 설명 버튼
        JButton tipsButton = new JButton("용어 설명");
        tipsButton.addActionListener(e -> showTipsDialog());
        row2Right.add(tipsButton);

        row2Right.add(Box.createHorizontalStrut(10));
        row2Right.add(exportChartButton);

        row2.add(row2Left, BorderLayout.WEST);
        row2.add(row2Right, BorderLayout.EAST);

        graphControlPanel.add(row1);
        graphControlPanel.add(row2);

        // --- 플레이스홀더 패널 ---
        placeholderPanel = new JPanel(new BorderLayout());
        placeholderPanel.setBackground(Color.WHITE);
        JLabel placeholderLabel = new JLabel("데이터를 불러와 계산을 수행하면 그래프가 표시됩니다.", SwingConstants.CENTER);
        placeholderLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
        placeholderLabel.setForeground(Color.GRAY);
        placeholderPanel.add(placeholderLabel, BorderLayout.CENTER);

        // --- 차트 컨테이너 (CardLayout 유사 효과) ---
        chartContainerPanel = new JPanel(new BorderLayout());
        chartContainerPanel.add(placeholderPanel, BorderLayout.CENTER);
    }

    private void createUIComponents() {
        // 체크박스 및 콤보박스 생성 로직 (리스너 연결)
        utsCheckBox = new JCheckBox("UTS 표시");
        utsCheckBox.setSelected(true); // [Default: True]
        utsCheckBox.addActionListener(e -> updateVisualization());

        yieldCheckBox = new JCheckBox("항복점 표시");
        yieldCheckBox.setSelected(true); // [Default: True]
        yieldCheckBox.addActionListener(e -> {
            yieldModeComboBox.setEnabled(yieldCheckBox.isSelected());
            updateVisualization();
        });

        yieldModeComboBox = new JComboBox<>(new String[] { "Auto (자동)", "0.2% Offset", "상/하항복점" });
        yieldModeComboBox.setEnabled(false);
        yieldModeComboBox.addActionListener(e -> {
            triggerRecalculation();
            updateVisualization();
            notifyMarkerRefChanged("MODE_CHANGED");
        });

        markerRefComboBox = new JComboBox<>(new String[] { "Engineering (공칭)", "True (진)" });
        markerRefComboBox.addActionListener(e -> {
            boolean isTrue = markerRefComboBox.getSelectedIndex() == 1;
            chartManager.setMarkerMode(isTrue);
            triggerRecalculation();
            updateVisualization();
            notifyMarkerRefChanged("REF_CHANGED");
        });

        slopeLineCheckBox = new JCheckBox("보조선 보기");
        slopeLineCheckBox.setSelected(true);
        slopeLineCheckBox.addActionListener(e -> {
            chartManager.setShowSlopeHandle(slopeLineCheckBox.isSelected());
            updateVisualization();
        });

        // [Removed] Elastic/Plastic checkboxes - redundant
        // elasticRegionCheckBox ...
        // plasticRegionCheckBox ...

        // 신규 체크박스 및 모드 선택 생성
        resilienceCheckBox = new JCheckBox("탄성 에너지");
        resilienceCheckBox.setSelected(false); // [Default: False] "탄성 에너지 제외 모두 True" 요청 반영
        resilienceCheckBox.addActionListener(e -> {
            resilienceModeComboBox.setEnabled(resilienceCheckBox.isSelected());
            updateVisualization();
        });

        resilienceModeComboBox = new JComboBox<>(new String[] { "Triangle (Linear)", "Integral (Actual)" });
        resilienceModeComboBox.setToolTipText(
                "<html><b>[계산 모드 선택]</b><br>Triangle: Hooke's Law 근사 (삼각형)<br>Integral: 실제 곡선 적분</html>");
        resilienceModeComboBox.addActionListener(e -> updateVisualization());

        toughnessCheckBox = new JCheckBox("영역 표시 (인성)");
        toughnessCheckBox.setSelected(true);
        toughnessCheckBox.addActionListener(e -> updateVisualization());

        toughnessCheckBox.setSelected(true);
        toughnessCheckBox.addActionListener(e -> updateVisualization());

        unloadingLineCheckBox = new JCheckBox("탄성 회복선");
        unloadingLineCheckBox.setSelected(false);
        unloadingLineCheckBox.addActionListener(e -> updateVisualization());
        zoomInButton = new JButton("확대");
        zoomInButton.addActionListener(e -> {
            chartManager.zoomIn();
            if (zoomInListener != null)
                zoomInListener.actionPerformed(e);
        });

        zoomOutButton = new JButton("축소");
        zoomOutButton.addActionListener(e -> {
            chartManager.zoomOut();
            if (zoomOutListener != null)
                zoomOutListener.actionPerformed(e);
        });

        resetZoomButton = new JButton("초기화");
        resetZoomButton.addActionListener(e -> {
            chartManager.resetZoom();
            if (resetZoomListener != null)
                resetZoomListener.actionPerformed(e);
        });

        exportChartButton = new JButton("차트 저장");
        exportChartButton.addActionListener(e -> {
            String defaultName = "MEVA_Chart";
            if (currentResult != null && currentResult.getExperimentName() != null) {
                String matName = currentResult.getExperimentName().replaceAll("[^a-zA-Z0-9가-힣]", "_");
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
                defaultName = matName + "_" + timestamp;
            }
            chartManager.saveChartImage(defaultName);

            if (exportChartListener != null)
                exportChartListener.actionPerformed(e);
        });

    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("응력-변형률 곡선"));
        setPreferredSize(new Dimension(800, 600));

        add(chartContainerPanel, BorderLayout.CENTER);
        add(graphControlPanel, BorderLayout.SOUTH);
    }

    // =================================================================================
    // 2. 외부 인터페이스 (MainFrame에서 호출)
    // =================================================================================

    /**
     * 그래프에 데이터를 표시합니다.
     */
    public void plotStressStrainCurve(List<StressStrainPoint> data) {
        if (data == null || data.isEmpty()) {
            JOptionPane.showMessageDialog(this, "표시할 데이터가 없습니다.", "경고", JOptionPane.WARNING_MESSAGE);
            return;
        }

        this.currentData = data;

        // 1. ChartManager에게 데이터 전달
        chartManager.updateData(data);

        // 2. 화면 전환 (Placeholder -> Chart)
        chartContainerPanel.removeAll();
        chartContainerPanel.add(chartManager.getChartPanel(), BorderLayout.CENTER);
        chartContainerPanel.revalidate();
        chartContainerPanel.repaint();
    }

    /**
     * 분석 결과를 설정하고 그래프를 갱신합니다.
     */
    public void setAnalysisResult(AnalysisResult result) {
        this.currentResult = result;

        // 1. ChartManager에게 결과 전달 (시각화용)
        // 초기 로드 시에는 핸들 위치도 자동 계산 (isManualUpdate = false)
        chartManager.updateAnalysisResult(result, false);

        // 2. ResultPanel에게 결과 전달 (수치 표시용)
        if (this.resultPanel != null) {
            this.resultPanel.setAnalysisResult(result);
        }

        // 3. UI 옵션에 맞춰 최종 시각화 및 수치 업데이트 수행
        updateVisualization();
    }

    /**
     * 그래프 데이터를 초기화합니다. [New]
     */
    public void clearGraph() {
        if (chartManager != null) {
            chartManager.clearData();
        }
        currentResult = null;
        currentData = null;
        if (resultPanel != null) {
            resultPanel.clearResults();
        }
        chartContainerPanel.removeAll();
        chartContainerPanel.add(placeholderPanel, BorderLayout.CENTER);
        chartContainerPanel.revalidate();
        chartContainerPanel.repaint();
    }

    public JPanel getChartPanel() {
        return chartManager.getChartPanel();
    }

    public void updateGraph() {
        revalidate();
        repaint();
    }

    // =================================================================================
    // 3. 내부 로직 및 이벤트 처리
    // =================================================================================

    /**
     * 현재 UI 옵션 상태를 기반으로 차트 시각화 업데이트
     * ChartManager에게 현재 체크박스 상태를 전달합니다.
     */
    private void updateVisualization() {
        // [Fix] UI 동기화: 항복점 표시 체크박스와 모드 선택 콤보박스 상태 연동
        yieldModeComboBox.setEnabled(yieldCheckBox.isSelected());

        // 기존 시각화 옵션
        chartManager.setVisualOptions(
                utsCheckBox.isSelected(),
                yieldCheckBox.isSelected(),
                slopeLineCheckBox.isSelected(),
                false, // elasticRegion (Disabled)
                false, // plasticRegion (Disabled)
                yieldModeComboBox.getSelectedIndex());

        // 신규 에너지 시각화 옵션 (모드 추가)
        boolean useTriangle = (resilienceModeComboBox.getSelectedIndex() == 0);
        chartManager.setAreaHighlightOptions(
                resilienceCheckBox.isSelected(),
                toughnessCheckBox.isSelected(),
                useTriangle);

        // [Fix] ResultPanel 수치 업데이트 연동
        if (resultPanel != null) {
            boolean isTrueMode = (markerRefComboBox.getSelectedIndex() == 1);
            int yieldMode = yieldModeComboBox.getSelectedIndex();
            resultPanel.updateMode(isTrueMode, useTriangle, yieldMode);
            // Unloading Line Update
            if (unloadingLineCheckBox != null) {
                chartManager.setUnloadingLineVisible(unloadingLineCheckBox.isSelected());
            }
        }
    }

    private void notifyMarkerRefChanged(String command) {
        if (markerRefChangedListener != null) {
            markerRefChangedListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command));
        }
    }

    // --- InteractionListener 구현 (ChartInputHandler로부터 콜백) ---

    @Override
    public void onHandleReleased() {
        // 핸들 조작 종료 시 재계산 로직 수행
        recalculateManualProperties();
    }

    /**
     * 수동 조작에 의한 재계산 실행
     */
    private void recalculateManualProperties() {
        if (currentResult == null || currentData == null)
            return;

        // 1. 핸들 위치 가져오기
        java.awt.geom.Point2D.Double handleStart = chartManager.getHandleStart();
        java.awt.geom.Point2D.Double handleEnd = chartManager.getHandleEnd();

        if (handleStart == null || handleEnd == null)
            return;

        // 2. 마커 기준 확인 (0: Eng, 1: True)
        boolean useEngineering = (markerRefComboBox.getSelectedIndex() == 0);

        // 3. 재계산 수행
        AnalysisResult newResult = materialCalculator.recalculateFromManualSlope(
                currentData, currentResult, handleStart.x, handleEnd.x, useEngineering);

        // 4. 결과 갱신 및 UI 업데이트
        this.currentResult = newResult;
        // 수동 조작이므로 핸들 위치를 초기화하지 않음 (true)
        chartManager.updateAnalysisResult(newResult, true);

        // [안전장치] ResultPanel에도 최신 결과 객체 전달 (동기화 보장)
        if (resultPanel != null) {
            resultPanel.setAnalysisResult(newResult);
        }

        updateVisualization();

        // 5. 결과 패널 알림 및 깜빡임 효과
        if (resultPanel != null && markerRefChangedListener != null) {
            markerRefChangedListener
                    .actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "RECALCULATED"));
        }

        // [Fix] 수동 조작 시 항복점 모드를 'Offset Method'로 자동 전환하여 변경된 항복점이 반영되도록 함
        if (currentResult.getYieldType() != AnalysisResult.YieldType.DISCONTINUOUS
                || yieldModeComboBox.getSelectedIndex() == 0) {
            yieldCheckBox.setSelected(true);
            yieldModeComboBox.setEnabled(true);
            yieldModeComboBox.setSelectedIndex(1); // 0.2% Offset 강제 선택
        }

        // 시각화 즉시 업데이트
        updateVisualization();
    }

    /**
     * [신규 추가] 콤보박스 옵션(Yield Mode, Marker Ref) 변경 시
     * 핸들 위치는 유지한 채 에너지와 항복점만 재계산합니다.
     */
    private void triggerRecalculation() {
        if (currentResult == null || currentData == null)
            return;

        boolean useEngineering = (markerRefComboBox.getSelectedIndex() == 0);

        // MaterialProperties의 신규 메서드 호출하여 결과 갱신
        AnalysisResult updated = materialCalculator.recalculatePropertiesBasedOnMode(
                currentResult, currentData, useEngineering);

        this.currentResult = updated;

        // 차트 매니저 업데이트 (isManualUpdate = true -> 핸들 위치 초기화 방지)
        chartManager.updateAnalysisResult(updated, true);

        // ResultPanel 수치 업데이트
        if (this.resultPanel != null) {
            this.resultPanel.setAnalysisResult(updated);
        }
        if (this.resultPanel != null) {
            this.resultPanel.setAnalysisResult(updated);
        }
    }

    private void showTipsDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "재료 역학 용어 설명", false);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 600);
        dialog.setLocationRelativeTo(this);

        StringBuilder html = new StringBuilder("<html><body style='padding: 10px; font-family: sans-serif;'>");
        for (Map.Entry<String, String> entry : GlossaryManager.getAllDefinitions().entrySet()) {
            // Remove <html> tag from value if starts with it, to merge into body
            String val = entry.getValue();
            if (val.startsWith("<html>"))
                val = val.substring(6);
            if (val.endsWith("</html>"))
                val = val.substring(0, val.length() - 7);

            html.append("<div style='margin-bottom: 15px; border-bottom: 1px solid #ccc; padding-bottom: 10px;'>");
            html.append(val);
            html.append("</div>");
        }
        html.append("</body></html>");

        JEditorPane editorPane = new JEditorPane("text/html", html.toString());
        editorPane.setEditable(false);
        editorPane.setCaretPosition(0);

        dialog.add(new JScrollPane(editorPane), BorderLayout.CENTER);

        JButton closeButton = new JButton("닫기");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel btnPanel = new JPanel();
        btnPanel.add(closeButton);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    // --- Helper ---

    // --- Setters ---
    public void setZoomInListener(ActionListener l) {
        this.zoomInListener = l;
    }

    public void setZoomOutListener(ActionListener l) {
        this.zoomOutListener = l;
    }

    public void setResetZoomListener(ActionListener l) {
        this.resetZoomListener = l;
    }

    public void setExportChartListener(ActionListener l) {
        this.exportChartListener = l;
    }

    public void setMarkerRefChangedListener(ActionListener l) {
        this.markerRefChangedListener = l;
    }

    public int getMarkerRefMode() {
        return markerRefComboBox.getSelectedIndex();
    }

    public int getSelectedYieldMode() {
        return yieldModeComboBox.getSelectedIndex();
    }
}
