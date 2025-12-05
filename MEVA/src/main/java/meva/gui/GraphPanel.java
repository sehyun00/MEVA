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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private JPanel graphControlPanel;       // 하단 제어 영역 전체 (SOUTH)
    private JPanel placeholderPanel;        // 차트가 없을 때 표시할 안내 패널
    private JPanel chartContainerPanel;     // 차트가 들어갈 중앙 패널

    // 옵션 체크박스들
    private JCheckBox utsCheckBox;
    private JCheckBox yieldCheckBox;
    private JComboBox<String> yieldModeComboBox;
    private JComboBox<String> markerRefComboBox;
    private JCheckBox slopeLineCheckBox;
    private JCheckBox elasticRegionCheckBox;
    private JCheckBox plasticRegionCheckBox;
    
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
        
        row1.add(utsCheckBox); // UTS
        
        JPanel yieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); // Yield
        yieldPanel.add(yieldCheckBox);
        yieldPanel.add(yieldModeComboBox);
        yieldPanel.add(createHelpLabel("항복점 모드 안내", 
            "<b>[항복점 모드]</b><br>실험 조건에 따라 자동 감지가 부정확할 수 있습니다. 필요 시 수동 변경하세요."));
        row1.add(yieldPanel);
        
        JPanel slopePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); // Slope
        slopePanel.add(slopeLineCheckBox);
        slopePanel.add(createHelpLabel("탄성 구간 조절", 
            "<b>[탄성 구간 조절]</b><br>파란색 점선 끝의 <b>네모 핸들</b>을 드래그하여 구간을 수동 설정하세요."));
        row1.add(slopePanel);
        
        JPanel refPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); // Marker Ref
        refPanel.add(new JLabel("마커 기준:"));
        refPanel.add(markerRefComboBox);
        row1.add(refPanel);

        // [Row 2] 영역 표시 및 줌 버튼
        JPanel row2 = new JPanel(new BorderLayout());
        JPanel row2Left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row2Left.add(elasticRegionCheckBox);
        row2Left.add(plasticRegionCheckBox);
        
        JPanel row2Right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        row2Right.add(zoomInButton);
        row2Right.add(zoomOutButton);
        row2Right.add(resetZoomButton);
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
        utsCheckBox.addActionListener(e -> updateVisualization());

        yieldCheckBox = new JCheckBox("항복점 표시");
        yieldCheckBox.addActionListener(e -> {
            yieldModeComboBox.setEnabled(yieldCheckBox.isSelected());
            updateVisualization();
        });
        
        yieldModeComboBox = new JComboBox<>(new String[]{ "Auto (자동)", "0.2% Offset", "상/하항복점" });
        yieldModeComboBox.setEnabled(false);
        yieldModeComboBox.addActionListener(e -> {
            updateVisualization();
            notifyMarkerRefChanged("MODE_CHANGED");
        });

        markerRefComboBox = new JComboBox<>(new String[]{ "Engineering (공칭)", "True (진)" });
        markerRefComboBox.addActionListener(e -> {
            boolean isTrue = markerRefComboBox.getSelectedIndex() == 1;
            chartManager.setMarkerMode(isTrue);
            updateVisualization();
            notifyMarkerRefChanged("REF_CHANGED");
        });

        slopeLineCheckBox = new JCheckBox("보조선 보기");
        slopeLineCheckBox.setSelected(true);
        slopeLineCheckBox.addActionListener(e -> {
            chartManager.setShowSlopeHandle(slopeLineCheckBox.isSelected());
            updateVisualization();
        });

        elasticRegionCheckBox = new JCheckBox("탄성 영역");
        elasticRegionCheckBox.addActionListener(e -> updateVisualization());

        plasticRegionCheckBox = new JCheckBox("소성 영역");
        plasticRegionCheckBox.addActionListener(e -> updateVisualization());
        
        // 버튼 생성
        zoomInButton = new JButton("Zoom In");
        zoomInButton.addActionListener(e -> {
            chartManager.zoomIn();
            if (zoomInListener != null) zoomInListener.actionPerformed(e);
        });
        
        zoomOutButton = new JButton("Zoom Out");
        zoomOutButton.addActionListener(e -> {
            chartManager.zoomOut();
            if (zoomOutListener != null) zoomOutListener.actionPerformed(e);
        });
        
        resetZoomButton = new JButton("Reset Zoom");
        resetZoomButton.addActionListener(e -> {
            chartManager.resetZoom();
            if (resetZoomListener != null) resetZoomListener.actionPerformed(e);
        });
        
        exportChartButton = new JButton("Export Chart");
        exportChartButton.addActionListener(e -> {
            chartManager.doSaveAs();
            if (exportChartListener != null) exportChartListener.actionPerformed(e);
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
            JOptionPane.showMessageDialog(this, "표시할 데이터가 없습니다.", "Warning", JOptionPane.WARNING_MESSAGE);
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
        // 초기 로드 시에는 핸들 위치도 자동 계산 (isManualUpdate = false)
        chartManager.updateAnalysisResult(result, false);
        updateVisualization();
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
        chartManager.setVisualOptions(
            utsCheckBox.isSelected(),
            yieldCheckBox.isSelected(),
            slopeLineCheckBox.isSelected(),
            elasticRegionCheckBox.isSelected(),
            plasticRegionCheckBox.isSelected(),
            yieldModeComboBox.getSelectedIndex()
        );
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
        if (currentResult == null || currentData == null) return;
        
        // 1. 핸들 위치 가져오기
        java.awt.geom.Point2D.Double handleStart = chartManager.getHandleStart();
        java.awt.geom.Point2D.Double handleEnd = chartManager.getHandleEnd();
        
        if (handleStart == null || handleEnd == null) return;

        // 2. 마커 기준 확인 (0: Eng, 1: True)
        boolean useEngineering = (markerRefComboBox.getSelectedIndex() == 0);
        
        // 3. 재계산 수행
        AnalysisResult newResult = materialCalculator.recalculateFromManualSlope(
            currentData, currentResult, handleStart.x, handleEnd.x, useEngineering
        );
        
        // 4. 결과 갱신 및 UI 업데이트
        this.currentResult = newResult;
        // 수동 조작이므로 핸들 위치를 초기화하지 않음 (true)
        chartManager.updateAnalysisResult(newResult, true);
        updateVisualization();
        
        // 5. 결과 패널 알림 및 깜빡임 효과
        if (resultPanel != null && markerRefChangedListener != null) {
            markerRefChangedListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "RECALCULATED"));
            resultPanel.flashRows(new int[]{3, 4}); // Young's Modulus & Yield Strength 행 깜빡임
        }
    }

    // --- Helper ---
    
    private JLabel createHelpLabel(String title, String content) {
        JLabel label = new JLabel("(?)");
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        label.setForeground(Color.GRAY);
        label.setToolTipText("<html><div style='width:250px;'>" + content + "</div></html>");
        label.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { label.setForeground(new Color(33, 150, 243)); label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
            public void mouseExited(MouseEvent e) { label.setForeground(Color.GRAY); label.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)); }
        });
        return label;
    }

    // --- Setters ---
    public void setZoomInListener(ActionListener l) { this.zoomInListener = l; }
    public void setZoomOutListener(ActionListener l) { this.zoomOutListener = l; }
    public void setResetZoomListener(ActionListener l) { this.resetZoomListener = l; }
    public void setExportChartListener(ActionListener l) { this.exportChartListener = l; }
    public void setMarkerRefChangedListener(ActionListener l) { this.markerRefChangedListener = l; }
    public int getMarkerRefMode() { return markerRefComboBox.getSelectedIndex(); }
    public int getSelectedYieldMode() { return yieldModeComboBox.getSelectedIndex(); }
}

