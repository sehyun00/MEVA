// src/main/java/meva/gui/GraphPanel.java

package meva.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Ellipse2D;
import java.util.List;

// JFreeChart imports
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.panel.AbstractOverlay;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;

// MEVA models
import meva.models.StressStrainPoint;
import meva.models.AnalysisResult;
import meva.calculation.MaterialProperties;

/**
 * 응력-변형률 곡선 그래프를 표시하는 패널
 * GUI 설계 문서에 따른 완전한 구현
 * 
 * @author MEVA 개발팀
 * @version 1.4 - Interactive Slope Handle 추가
 */
public class GraphPanel extends JPanel {
    
    // UI 컴포넌트
    private CustomChartPanel chartPanel;          // JFreeChart를 표시하는 메인 패널 (CENTER)
    private JPanel graphControlPanel;       // 하단 제어 영역 전체 (SOUTH)
    private JPanel placeholderPanel;        // 차트가 없을 때 표시할 안내 패널

    // 옵션 체크박스들
    private JCheckBox utsCheckBox;
    private JCheckBox yieldCheckBox;
    private JComboBox<String> yieldModeComboBox; // 항복점 모드 선택 콤보박스
    private JComboBox<String> markerRefComboBox; // 마커 기준 선택 (Engineering vs True)
    private JCheckBox slopeLineCheckBox; // [New] 탄성/Offset 라인 표시 토글
    private JCheckBox elasticRegionCheckBox;
    private JCheckBox plasticRegionCheckBox;

    // 상태 플래그
    private boolean showUTS;
    private boolean showYieldPoint;
    private boolean showSlopeLine = true; // 기본값: 표시
    private boolean showElasticRegion;
    private boolean showPlasticRegion;
    
    // JFreeChart 관련
    private JFreeChart currentChart;       // 현재 차트 객체
    private List<StressStrainPoint> currentData; // 현재 표시 중인 데이터 (시각화 업데이트용)
    private AnalysisResult analysisResult;       // 물성 분석 결과 (그래프 마킹용)
    private Point2D mousePoint; // 크로스헤어 표시용 마우스 좌표
    
    // 차트 제어 버튼들
    private JButton zoomInButton;      // 차트 확대
    private JButton zoomOutButton;     // 차트 축소
    private JButton resetZoomButton;   // 줌 초기화
    private JButton exportChartButton; // 차트 이미지 저장
    
    // 이벤트 리스너들
    private ActionListener zoomInListener;
    private ActionListener zoomOutListener;
    private ActionListener resetZoomListener;
    private ActionListener exportChartListener;
    private ActionListener markerRefChangedListener; // 마커 기준 변경 리스너 추가
    
    // 핸들 조작 관련
    private SlopeHandleOverlay handleOverlay;
    private Point2D.Double handleStart = null; // 수동 조작된 시작점 (Strain, Stress)
    private Point2D.Double handleEnd = null;   // 수동 조작된 끝점
    private boolean isDraggingStart = false;
    private boolean isDraggingEnd = false;
    private MaterialProperties materialCalculator = new MaterialProperties(); // 재계산용
    private ResultPanel resultPanel; // 결과 갱신용 참조

    /**
     * GraphPanel 생성자
     */
    public GraphPanel() {
        // 툴팁 지속 시간을 무제한(최대값)으로 설정하여 사용자가 읽는 동안 사라지지 않게 함
        javax.swing.ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
        
        initializeComponents();
        setupLayout();
    }

    /**
     * 결과 패널 참조 설정 (재계산 시 업데이트용)
     */
    public void setResultPanel(ResultPanel panel) {
        this.resultPanel = panel;
    }
    
    /**
     * 모든 컴포넌트 초기화 및 레이아웃 구성 (2-Row 구조)
     */
    private void initializeComponents() {
        // 1. 하단 제어 패널 구성 (GridLayout 2 rows)
        graphControlPanel = new JPanel(new GridLayout(2, 1, 0, 5)); // 수직 간격 5px
        
        // 컴포넌트 객체 생성 및 이벤트 리스너 등록
        createUIComponents();

        // [Row 1] 주요 옵션 (UTS, Yield, Marker Reference)
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        // UTS 표시
        row1.add(utsCheckBox);
        
        // 항복점 그룹
        JPanel yieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        yieldPanel.add(yieldCheckBox);
        yieldPanel.add(yieldModeComboBox);

        // 도움말 아이콘 (?) 추가
        JLabel helpLabel = new JLabel("(?)");
        helpLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        helpLabel.setForeground(Color.GRAY);
        helpLabel.setToolTipText("<html><div style='width:300px;'>" +
                "<b>[항복점 모드 안내]</b><br>" +
                "실험 조건(인장 속도 등)이나 재료 특성에 따라 자동 감지가 부정확할 수 있습니다. " +
                "필요 시 수동으로 모드를 변경하세요.<br><br>" +
                "<b>[마커 위치 참고]</b><br>" +
                "데이터 노이즈가 있는 구간에서는 정확한 분석을 위해 " +
                "<b>수학적으로 계산된 교차점(보간값)</b>에 마커가 표시됩니다. " +
                "따라서 확대 시 실제 데이터 포인트(꺾은선)와 미세하게 떨어져 보일 수 있으나, " +
                "이는 오류가 아닌 정밀 계산 결과입니다." +
                "</div></html>");
        
        // 마우스 호버 효과 (UX)
        helpLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                helpLabel.setForeground(new Color(33, 150, 243)); // Blue
                helpLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                helpLabel.setForeground(Color.GRAY);
                helpLabel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        });
        yieldPanel.add(helpLabel);

        row1.add(yieldPanel);
        
        // 보조선(Slope) 그룹
        JPanel slopePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        slopePanel.add(slopeLineCheckBox);
        
        // 보조선 도움말 아이콘 (?) 추가
        JLabel slopeHelpLabel = new JLabel("(?)");
        slopeHelpLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        slopeHelpLabel.setForeground(Color.GRAY);
        slopeHelpLabel.setToolTipText("<html><div style='width:300px;'>" +
                "<b>[탄성 구간 수동 조절]</b><br>" +
                "파란색 점선(Elastic Slope)의 양 끝에 있는 <b>네모 핸들</b>을 드래그하여 " +
                "탄성 구간을 수동으로 지정할 수 있습니다.<br><br>" +
                "이 기능을 사용하면 영률(Young's Modulus)과 항복점이 " +
                "선택된 구간에 맞춰 실시간으로 재계산됩니다." +
                "</div></html>");
        
        slopeHelpLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                slopeHelpLabel.setForeground(new Color(33, 150, 243)); // Blue
                slopeHelpLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                slopeHelpLabel.setForeground(Color.GRAY);
                slopeHelpLabel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        });
        slopePanel.add(slopeHelpLabel);
        
        row1.add(slopePanel);
        
        // 마커 기준 그룹
        JPanel refPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel refLabel = new JLabel("마커 기준:");
        refLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        refPanel.add(refLabel);
        refPanel.add(markerRefComboBox);
        row1.add(refPanel);

        // [Row 2] 영역 표시 및 제어 버튼
        JPanel row2 = new JPanel(new BorderLayout());
        
        // Row 2 - Left: 영역 표시
        JPanel row2Left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row2Left.add(elasticRegionCheckBox);
        row2Left.add(plasticRegionCheckBox);
        
        // Row 2 - Right: 제어 버튼
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

        // 2. 플레이스홀더 패널 생성 (초기 화면용)
        placeholderPanel = new JPanel(new BorderLayout());
        placeholderPanel.setBackground(Color.WHITE);
        JLabel placeholderLabel = new JLabel("데이터를 불러와 계산을 수행하면 그래프가 표시됩니다.", 
            SwingConstants.CENTER);
        placeholderLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
        placeholderLabel.setForeground(new Color(117, 117, 117));
        placeholderPanel.add(placeholderLabel, BorderLayout.CENTER);
    }

    /**
     * UI 컴포넌트(체크박스, 버튼 등) 객체 생성 및 이벤트 설정
     */
    private void createUIComponents() {
        // --- 옵션 체크박스 ---
        
        // UTS 표시 체크박스
        utsCheckBox = new JCheckBox("UTS 표시");
        utsCheckBox.addActionListener(e -> setShowUTS(utsCheckBox.isSelected()));

        // 항복점 표시 체크박스
        yieldCheckBox = new JCheckBox("항복점 표시");
        yieldCheckBox.addActionListener(e -> {
            setShowYieldPoint(yieldCheckBox.isSelected());
            yieldModeComboBox.setEnabled(yieldCheckBox.isSelected());
        });
        
        // 항복점 모드 콤보박스
        String[] yieldModes = { "Auto (자동)", "0.2% Offset", "상/하항복점" };
        yieldModeComboBox = new JComboBox<>(yieldModes);
        yieldModeComboBox.setFont(new Font("SansSerif", Font.PLAIN, 11));
        yieldModeComboBox.setPreferredSize(new Dimension(110, 22));
        yieldModeComboBox.setEnabled(false);
        yieldModeComboBox.addActionListener(e -> {
            updateGraphVisualizations();
            // 모드 변경 시 MainFrame에 알려 결과 패널 값 갱신
            if (markerRefChangedListener != null) {
                markerRefChangedListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "MODE_CHANGED"));
            }
        });

        // 마커 기준(Reference) 선택 콤보박스
        String[] refModes = { "Engineering (공칭)", "True (진)" };
        markerRefComboBox = new JComboBox<>(refModes);
        markerRefComboBox.setFont(new Font("SansSerif", Font.PLAIN, 11));
        markerRefComboBox.setPreferredSize(new Dimension(130, 22));
        markerRefComboBox.setSelectedIndex(0); // 기본값: Engineering
        markerRefComboBox.addActionListener(e -> {
            updateGraphVisualizations();
            if (markerRefChangedListener != null) {
                markerRefChangedListener.actionPerformed(e);
            }
        });

        // 보조선(Slope) 표시 체크박스
        slopeLineCheckBox = new JCheckBox("보조선 보기");
        slopeLineCheckBox.setSelected(true); // 기본값: 켜짐
        slopeLineCheckBox.addActionListener(e -> setShowSlopeLine(slopeLineCheckBox.isSelected()));

        // 탄성 영역 표시 체크박스
        elasticRegionCheckBox = new JCheckBox("탄성 영역");
        elasticRegionCheckBox.addActionListener(e -> setShowElasticRegion(elasticRegionCheckBox.isSelected()));

        // 소성 영역 표시 체크박스
        plasticRegionCheckBox = new JCheckBox("소성 영역");
        plasticRegionCheckBox.addActionListener(e -> setShowPlasticRegion(plasticRegionCheckBox.isSelected()));
        
        // --- 제어 버튼 ---

        // Zoom In 버튼
        zoomInButton = new JButton("Zoom In");
        zoomInButton.addActionListener(e -> {
            if (chartPanel != null) {
                chartPanel.zoomInBoth(
                    chartPanel.getWidth() / 2.0,
                    chartPanel.getHeight() / 2.0
                );
            }
            if (zoomInListener != null) zoomInListener.actionPerformed(e);
        });
        
        // Zoom Out 버튼
        zoomOutButton = new JButton("Zoom Out");
        zoomOutButton.addActionListener(e -> {
            if (chartPanel != null) {
                chartPanel.zoomOutBoth(
                    chartPanel.getWidth() / 2.0,
                    chartPanel.getHeight() / 2.0
                );
            }
            if (zoomOutListener != null) zoomOutListener.actionPerformed(e);
        });
        
        // Reset Zoom 버튼
        resetZoomButton = new JButton("Reset Zoom");
        resetZoomButton.addActionListener(e -> {
            if (chartPanel != null) {
                chartPanel.restoreAutoBounds();
            }
            if (resetZoomListener != null) resetZoomListener.actionPerformed(e);
        });
        
        // Export Chart 버튼
        exportChartButton = new JButton("Export Chart");
        exportChartButton.addActionListener(e -> {
            if (exportChartListener != null) exportChartListener.actionPerformed(e);
        });
    }
    
    /**
     * 레이아웃 설정
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("응력-변형률 곡선"));
        
        // 그래프 패널의 초기 크기 및 최소 크기 설정
        setPreferredSize(new Dimension(800, 600));
        setMinimumSize(new Dimension(800, 600));
        
        // CENTER: 초기에는 플레이스홀더 표시 (데이터 로드 시 chartPanel로 교체)
        add(placeholderPanel, BorderLayout.CENTER);
        
        // SOUTH: 제어 패널 추가
        add(graphControlPanel, BorderLayout.SOUTH);
    }
    
    /**
     * 응력-변형률 곡선을 그래프에 표시
     * 
     * @param data 응력-변형률 데이터 리스트
     */
    public void plotStressStrainCurve(List<StressStrainPoint> data) {
        if (data == null || data.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "표시할 데이터가 없습니다.",
                "데이터 없음",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        this.currentData = data; // 데이터 저장 (시각화 업데이트용)
        
        // 1. True Stress-Strain 시리즈 생성
        XYSeries trueSeries = new XYSeries("True Stress-Strain");
        for (StressStrainPoint point : data) {
            trueSeries.add(point.getTrueStrain(), point.getTrueStress());
        }
        
        // 2. Engineering Stress-Strain 시리즈 생성
        XYSeries engSeries = new XYSeries("Engineering Stress-Strain");
        for (StressStrainPoint point : data) {
            engSeries.add(point.getEngineeringStrain(), point.getEngineeringStress());
        }
        
        // 3. 데이터셋 생성
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(trueSeries);
        dataset.addSeries(engSeries);
        
        // 4. 차트 생성
        currentChart = ChartFactory.createXYLineChart(
            "Stress-Strain Curve",           // 차트 제목
            "Strain",                         // X축 레이블
            "Stress (MPa)",                   // Y축 레이블
            dataset,                          // 데이터셋
            PlotOrientation.VERTICAL,         // 방향
            true,                             // 범례 표시
            true,                             // 툴팁 표시
            false                             // URL 표시 안 함
        );
        
        // 5. 차트 스타일 설정
        customizeChart(currentChart);
        
        // 차트의 Plot에 패닝 기능 활성화 (필수)
        XYPlot plot = currentChart.getXYPlot();
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        
        // 6. ChartPanel 설정 (재사용 구조)
        if (chartPanel == null) {
            chartPanel = new CustomChartPanel(currentChart);
            chartPanel.setMouseWheelEnabled(true);  // 마우스 휠 줌 활성화
            
            // 패닝(화면 이동) 기능 활성화
            chartPanel.setDomainPannable(true);
            chartPanel.setRangePannable(true);

            // 배경색을 흰색으로 설정하여 깔끔하게 표시
            chartPanel.setBackground(Color.WHITE);
            
            // 크로스헤어 오버레이 기능을 위한 마우스 리스너 등록
            chartPanel.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    mousePoint = e.getPoint();
                    chartPanel.repaint(); // 오버레이 갱신
                }
            });

            chartPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    mousePoint = null; // 마우스가 나가면 십자선 제거
                    chartPanel.repaint();
                }
            });

            // 커스텀 오버레이 등록
            chartPanel.addOverlay(new InteractiveCrosshairOverlay());

            // [New] Slope Handle Overlay 등록
            handleOverlay = new SlopeHandleOverlay();
            chartPanel.addOverlay(handleOverlay);
            setupHandleListeners();
            
            // 플레이스홀더가 있다면 제거하고 chartPanel 추가
            remove(placeholderPanel);
            add(chartPanel, BorderLayout.CENTER);
            revalidate();
        } else {
            // 이미 생성된 chartPanel이 있다면 차트 객체만 교체
            chartPanel.setChart(currentChart);
            
            // 차트 교체 후 패닝 설정 재적용 (새 Plot에 적용)
            chartPanel.setDomainPannable(true);
            chartPanel.setRangePannable(true);
        }

        // 7. 시각화 요소 업데이트 (UTS, 항복점 등)
        updateGraphVisualizations();
        
        repaint();
    }
    
    /**
     * 차트 스타일 커스터마이징
     * 
     * @param chart 커스터마이징할 차트
     */
    private void customizeChart(JFreeChart chart) {
        // 배경색 설정
        chart.setBackgroundPaint(Color.WHITE);
        
        // Plot 설정
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(220, 220, 220));
        plot.setRangeGridlinePaint(new Color(220, 220, 220));
        
        // 렌더러 설정 (선 스타일)
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        
        // True Stress-Strain (파란색, 굵은 선)
        renderer.setSeriesPaint(0, new Color(33, 150, 243));
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesShapesVisible(0, false);
        
        // Engineering Stress-Strain (빨간색, 점선)
        renderer.setSeriesPaint(1, new Color(244, 67, 54));
        renderer.setSeriesStroke(1, new BasicStroke(
            2.0f, 
            BasicStroke.CAP_ROUND, 
            BasicStroke.JOIN_ROUND, 
            1.0f, 
            new float[]{5.0f, 5.0f}, 
            0.0f
        ));
        renderer.setSeriesShapesVisible(1, false);
        
        plot.setRenderer(renderer);
    }
    
    /**
     * 그래프 업데이트 메서드 (레거시 호환용)
     */
    public void updateGraph() {
        if (chartPanel != null) {
            revalidate();
            repaint();
        }
    }
    
    /**
     * 현재 차트 객체 반환
     */
    public JFreeChart getCurrentChart() {
        return currentChart;
    }
    
    /**
     * 차트 패널 반환
     */
    public JPanel getChartPanel() {
        return chartPanel;
    }
    
    /**
     * 외부에서 계산된 분석 결과를 설정하고 그래프 시각화 요소를 갱신합니다. (MVC 패턴)
     * 
     * @param result 계산된 물성 분석 결과
     */
    public void setAnalysisResult(AnalysisResult result) {
        this.analysisResult = result;
        
        // 새로운 결과가 설정되면 핸들 위치도 초기화
        if (result != null && result.getYoungsModulus() > 0) {
            updateHandleFromAnalysis();
        }
        
        updateGraphVisualizations();
        repaint();
    }

    // ========== 상태 플래그 Setter 메서드들 (향후 확장용) ==========

    public void setShowUTS(boolean show) {
        this.showUTS = show;
        updateGraphVisualizations();
        repaint();
    }

    public void setShowYieldPoint(boolean show) {
        this.showYieldPoint = show;
        updateGraphVisualizations();
        repaint();
    }

    public void setShowSlopeLine(boolean show) {
        this.showSlopeLine = show;
        updateGraphVisualizations();
        repaint();
    }

    public void setShowElasticRegion(boolean show) {
        this.showElasticRegion = show;
        updateGraphVisualizations();
        repaint();
    }

    public void setShowPlasticRegion(boolean show) {
        this.showPlasticRegion = show;
        updateGraphVisualizations();
        repaint();
    }

    // ========== 시각화 로직 (UTS, 항복점 등) ==========

    /**
     * 현재 설정된 플래그와 데이터(AnalysisResult)에 따라 그래프 시각화 요소를 업데이트
     */
    private void updateGraphVisualizations() {
        if (currentChart == null || currentData == null || currentData.isEmpty()) {
            return;
        }

        XYPlot plot = currentChart.getXYPlot();
        
        // 렌더링 순서 설정: Main Dataset(0) -> Secondary Dataset(1) 순서로 그려 점이 선 위에 오도록 함
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        
        // 1. 기존 마커 및 어노테이션 제거
        plot.clearAnnotations();
        plot.clearDomainMarkers();

        // 분석 결과가 아직 없으면 시각화 건너뜀
        if (analysisResult == null) return;

        boolean useEngineering = markerRefComboBox.getSelectedIndex() == 0; // 0: Eng, 1: True

        StressStrainPoint utsPoint = analysisResult.getUtsPoint();
        
        // 항복점 모드에 따른 포인트 결정
        StressStrainPoint displayYieldPoint = null;
        StressStrainPoint displayUpperYield = null;
        StressStrainPoint displayLowerYield = null;
        
        int selectedMode = yieldModeComboBox.getSelectedIndex(); // 0: Auto, 1: Offset, 2: Upper/Lower
        
        if (selectedMode == 0) { // Auto
            if (analysisResult.getYieldType() == AnalysisResult.YieldType.DISCONTINUOUS) {
                displayUpperYield = analysisResult.getUpperYieldPoint();
                displayLowerYield = analysisResult.getLowerYieldPoint();
            } else {
                // Offset Type (모드에 따라 적절한 포인트 선택)
                if (useEngineering && analysisResult.getOffsetYieldPointEng() != null) {
                    displayYieldPoint = analysisResult.getOffsetYieldPointEng();
                } else {
                    displayYieldPoint = analysisResult.getYieldPoint(); // Default (True)
                }
            }
        } else if (selectedMode == 1) { // Force 0.2% Offset
            if (useEngineering && analysisResult.getOffsetYieldPointEng() != null) {
                displayYieldPoint = analysisResult.getOffsetYieldPointEng();
            } else {
                displayYieldPoint = analysisResult.getOffsetYieldPoint();
            }
            
            // Fallback
            if (displayYieldPoint == null && analysisResult.getYieldType() == AnalysisResult.YieldType.OFFSET_02) {
                displayYieldPoint = analysisResult.getYieldPoint();
            }
        } else if (selectedMode == 2) { // Force Upper/Lower
            displayUpperYield = analysisResult.getUpperYieldPoint();
            displayLowerYield = analysisResult.getLowerYieldPoint();
        }

        // 시각화 및 계산을 위한 Reference Yield Point 결정
        StressStrainPoint refYield = null;
        if (displayYieldPoint != null) {
            refYield = displayYieldPoint;
        } else if (displayUpperYield != null) {
            refYield = displayUpperYield;
        } else {
            refYield = analysisResult.getYieldPoint();
        }

        // 3. 탄성 기울기 선 및 Offset 라인 시각화
        XYSeriesCollection slopeDataset = new XYSeriesCollection();
        XYLineAndShapeRenderer slopeRenderer = new XYLineAndShapeRenderer(true, false); // 선 켬, 모양 끔
        
        if (analysisResult.getYoungsModulus() > 0) {
            double E_MPa = useEngineering ? 
                analysisResult.getYoungsModulusEng() * 1000.0 : 
                analysisResult.getYoungsModulus() * 1000.0;
            
            // Fallback: 만약 Eng E가 계산되지 않았다면 True E 사용 (안전장치)
            if (useEngineering && E_MPa == 0.0 && analysisResult.getYoungsModulus() > 0) {
                E_MPa = analysisResult.getYoungsModulus() * 1000.0;
            }
            
            double intercept = useEngineering ? 
                analysisResult.getElasticLineInterceptEng() : 
                analysisResult.getElasticLineIntercept();
            
            double maxY = (refYield != null) ? 
                (useEngineering ? refYield.getEngineeringStress() : refYield.getTrueStress()) * 1.2 : 
                (utsPoint != null ? (useEngineering ? utsPoint.getEngineeringStress() : utsPoint.getTrueStress()) * 0.8 : 500.0);
            
            // 3-1. 탄성 기울기 선
            XYSeries elasticSeries = new XYSeries("Elastic Slope");
            
            if (handleStart != null && handleEnd != null) {
                // [수정] 사용자가 지정한 구간(핸들)에 맞춰서 라인을 그립니다.
                // 기울기는 계산된 회귀선(E_MPa)을 따르며, 길이는 핸들 위치로 제한됩니다.
                double startX = handleStart.x;
                double endX = handleEnd.x;
                
                double startY = E_MPa * startX + intercept;
                double endY = E_MPa * endX + intercept;
                
                elasticSeries.add(startX, startY);
                elasticSeries.add(endX, endY);
            } else {
                // 초기 상태 또는 핸들 없음
                double x1 = 0.0;
                double y1 = intercept;
                double y2 = maxY;
                double x2 = (y2 - intercept) / E_MPa; 
                if (x2 < 0) x2 = 0;
                
                elasticSeries.add(x1, y1);
                elasticSeries.add(x2, y2);
            }
            slopeDataset.addSeries(elasticSeries);
            
            slopeRenderer.setSeriesPaint(0, new Color(0, 0, 255, 180));
            slopeRenderer.setSeriesStroke(0, new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f, 5.0f}, 0.0f));

            // 3-2. 0.2% Offset 선
            double offsetX = 0.002;
            // Offset 선은 여전히 길게 표시하여 교차점을 잘 보여주도록 함 (또는 이것도 제한할지?)
            // 보통 Offset 선은 교차점 확인용이므로 길게 두는 것이 좋음.
            // 다만 시작점은 elastic start + offset 정도면 적당함.
            double x1 = (handleStart != null) ? handleStart.x : 0.0;
            double offX1 = x1 + offsetX;
            double offY1 = E_MPa * x1 + intercept; // y값은 평행이동이므로 기울기 동일
            
            double y2 = maxY;
            double offX2 = (y2 - intercept) / E_MPa + offsetX;
            double offY2 = y2;

            XYSeries offsetSeries = new XYSeries("0.2% Offset");
            offsetSeries.add(offX1, offY1);
            offsetSeries.add(offX2, offY2);
            slopeDataset.addSeries(offsetSeries);
            
            slopeRenderer.setSeriesPaint(1, new Color(0, 150, 0, 180));
            slopeRenderer.setSeriesStroke(1, new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{5.0f, 5.0f}, 0.0f));
        }
        
        // 4. 특수 포인트(UTS, Yield) 시각화
        XYSeriesCollection specialPointsDataset = new XYSeriesCollection();
        XYLineAndShapeRenderer pointRenderer = new XYLineAndShapeRenderer(false, true);
        pointRenderer.setAutoPopulateSeriesPaint(false);
        pointRenderer.setAutoPopulateSeriesShape(false);
        
        int seriesIndex = 0;

        // 3-1. UTS 포인트
        if (showUTS && utsPoint != null) {
            XYSeries utsSeries = new XYSeries("UTS Point");
            double uStrain = useEngineering ? utsPoint.getEngineeringStrain() : utsPoint.getTrueStrain();
            double uStress = useEngineering ? utsPoint.getEngineeringStress() : utsPoint.getTrueStress();
            
            utsSeries.add(uStrain, uStress);
            specialPointsDataset.addSeries(utsSeries);
            
            pointRenderer.setSeriesPaint(seriesIndex, Color.RED);
            pointRenderer.setSeriesShape(seriesIndex, new Ellipse2D.Double(-4.0, -4.0, 8.0, 8.0));
            
            XYPointerAnnotation utsAnnotation = new XYPointerAnnotation(
                String.format("UTS (%.1f MPa)", uStress), uStrain, uStress, -Math.PI / 4.0
            );
            utsAnnotation.setTipRadius(10.0);
            utsAnnotation.setBaseRadius(35.0);
            utsAnnotation.setFont(new Font("SansSerif", Font.BOLD, 12));
            utsAnnotation.setPaint(Color.RED);
            utsAnnotation.setArrowPaint(Color.RED);
            plot.addAnnotation(utsAnnotation);
            
            seriesIndex++;
        }

        // 3-2. 항복점 포인트
        if (showYieldPoint && displayYieldPoint != null) {
            XYSeries yieldSeries = new XYSeries("Yield Point");
            double yStrain = useEngineering ? displayYieldPoint.getEngineeringStrain() : displayYieldPoint.getTrueStrain();
            double yStress = useEngineering ? displayYieldPoint.getEngineeringStress() : displayYieldPoint.getTrueStress();
            
            yieldSeries.add(yStrain, yStress);
            specialPointsDataset.addSeries(yieldSeries);
            
            pointRenderer.setSeriesPaint(seriesIndex, new Color(0, 150, 0));
            pointRenderer.setSeriesShape(seriesIndex, new Ellipse2D.Double(-4.0, -4.0, 8.0, 8.0));
            
            XYPointerAnnotation yieldAnnotation = new XYPointerAnnotation(
                String.format("Yield (%.1f MPa)", yStress), yStrain, yStress, Math.PI / 2.0 
            );
            yieldAnnotation.setTipRadius(10.0);
            yieldAnnotation.setBaseRadius(35.0);
            yieldAnnotation.setFont(new Font("SansSerif", Font.BOLD, 12));
            yieldAnnotation.setPaint(new Color(0, 150, 0));
            yieldAnnotation.setArrowPaint(new Color(0, 150, 0));
            plot.addAnnotation(yieldAnnotation);
            
            seriesIndex++;
        }
        
        // 3-3. 상/하항복점
        if (showYieldPoint && (displayUpperYield != null || displayLowerYield != null)) {
            if (displayUpperYield != null) {
                XYSeries upperSeries = new XYSeries("Upper Yield");
                double uyStrain = useEngineering ? displayUpperYield.getEngineeringStrain() : displayUpperYield.getTrueStrain();
                double uyStress = useEngineering ? displayUpperYield.getEngineeringStress() : displayUpperYield.getTrueStress();
                
                upperSeries.add(uyStrain, uyStress);
                specialPointsDataset.addSeries(upperSeries);
                
                pointRenderer.setSeriesPaint(seriesIndex, new Color(255, 140, 0));
                pointRenderer.setSeriesShape(seriesIndex, new Ellipse2D.Double(-4.0, -4.0, 8.0, 8.0));
                
                XYPointerAnnotation upperAnnotation = new XYPointerAnnotation(
                    String.format("UYP (%.1f)", uyStress), uyStrain, uyStress, Math.PI / 4.0 
                );
                upperAnnotation.setTipRadius(10.0);
                upperAnnotation.setBaseRadius(30.0);
                upperAnnotation.setFont(new Font("SansSerif", Font.BOLD, 11));
                upperAnnotation.setPaint(new Color(255, 140, 0));
                upperAnnotation.setArrowPaint(new Color(255, 140, 0));
                plot.addAnnotation(upperAnnotation);
                seriesIndex++;
            }
            
            if (displayLowerYield != null) {
                XYSeries lowerSeries = new XYSeries("Lower Yield");
                double lyStrain = useEngineering ? displayLowerYield.getEngineeringStrain() : displayLowerYield.getTrueStrain();
                double lyStress = useEngineering ? displayLowerYield.getEngineeringStress() : displayLowerYield.getTrueStress();
                
                lowerSeries.add(lyStrain, lyStress);
                specialPointsDataset.addSeries(lowerSeries);
                
                pointRenderer.setSeriesPaint(seriesIndex, new Color(255, 140, 0));
                pointRenderer.setSeriesShape(seriesIndex, new Ellipse2D.Double(-4.0, -4.0, 8.0, 8.0));
                
                XYPointerAnnotation lowerAnnotation = new XYPointerAnnotation(
                    String.format("LYP (%.1f)", lyStress), lyStrain, lyStress, -Math.PI / 2.0 
                );
                lowerAnnotation.setTipRadius(10.0);
                lowerAnnotation.setBaseRadius(30.0);
                lowerAnnotation.setFont(new Font("SansSerif", Font.BOLD, 11));
                lowerAnnotation.setPaint(new Color(255, 140, 0));
                lowerAnnotation.setArrowPaint(new Color(255, 140, 0));
                plot.addAnnotation(lowerAnnotation);
                seriesIndex++;
            }
        }

        // 4. 렌더러 적용
        if (specialPointsDataset.getSeriesCount() > 0) {
            plot.setDataset(1, specialPointsDataset);
            plot.setRenderer(1, pointRenderer);
        } else {
            plot.setDataset(1, null);
        }
        
        if (showSlopeLine && slopeDataset.getSeriesCount() > 0) {
            plot.setDataset(2, slopeDataset);
            plot.setRenderer(2, slopeRenderer);
        } else {
            plot.setDataset(2, null);
        }

        // 5. 영역 표시
        if (refYield != null) {
            double yieldStrain = useEngineering ? refYield.getEngineeringStrain() : refYield.getTrueStrain();
            double maxStrain = useEngineering ? 
                currentData.get(currentData.size() - 1).getEngineeringStrain() : 
                currentData.get(currentData.size() - 1).getTrueStrain();

            if (showElasticRegion) {
                IntervalMarker elasticMarker = new IntervalMarker(0.0, yieldStrain);
                elasticMarker.setPaint(new Color(0, 0, 255, 30));
                elasticMarker.setLabel("Elastic Region");
                elasticMarker.setLabelFont(new Font("SansSerif", Font.ITALIC, 11));
                elasticMarker.setLabelAnchor(RectangleAnchor.TOP_LEFT);
                elasticMarker.setLabelTextAnchor(TextAnchor.TOP_LEFT);
                plot.addDomainMarker(elasticMarker);
            }

            if (showPlasticRegion) {
                IntervalMarker plasticMarker = new IntervalMarker(yieldStrain, maxStrain);
                plasticMarker.setPaint(new Color(255, 0, 0, 30));
                plasticMarker.setLabel("Plastic Region");
                plasticMarker.setLabelFont(new Font("SansSerif", Font.ITALIC, 11));
                plasticMarker.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
                plasticMarker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
                plot.addDomainMarker(plasticMarker);
            }
        }
    }

    // ========== 이벤트 리스너 설정 메서드들 ==========
    
    public void setZoomInListener(ActionListener listener) {
        this.zoomInListener = listener;
    }
    
    public void setZoomOutListener(ActionListener listener) {
        this.zoomOutListener = listener;
    }
    
    public void setResetZoomListener(ActionListener listener) {
        this.resetZoomListener = listener;
    }
    
    public void setExportChartListener(ActionListener listener) {
        this.exportChartListener = listener;
    }

    public void setMarkerRefChangedListener(ActionListener listener) {
        this.markerRefChangedListener = listener;
    }

    /**
     * 현재 마커 기준 모드를 반환합니다.
     * @return 0: Engineering, 1: True
     */
    public int getMarkerRefMode() {
        return markerRefComboBox.getSelectedIndex();
    }

    /**
     * 인터랙티브 크로스헤어 오버레이
     */
    private class InteractiveCrosshairOverlay extends AbstractOverlay implements Overlay {
        private final Stroke DASHED_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, 
                BasicStroke.JOIN_MITER, 10.0f, new float[]{4.0f, 4.0f}, 0.0f);
        private final Color CROSSHAIR_COLOR = new Color(100, 100, 100, 180);
        private final Color LABEL_BG_COLOR = Color.BLACK;
        private final Color LABEL_TEXT_COLOR = Color.WHITE;
        private final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 11);

        @Override
        public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
            if (mousePoint == null) return;

            ChartRenderingInfo info = chartPanel.getChartRenderingInfo();
            if (info == null || info.getPlotInfo() == null) return;
            
            Rectangle2D dataArea = info.getPlotInfo().getDataArea();
            if (!dataArea.contains(mousePoint)) return;

            double x = mousePoint.getX();
            double y = mousePoint.getY();

            // 기존 설정 저장
            Stroke originalStroke = g2.getStroke();
            Color originalColor = g2.getColor();
            Font originalFont = g2.getFont();
            g2.setFont(LABEL_FONT);

            // 1. 십자선 그리기
            g2.setStroke(DASHED_STROKE);
            g2.setColor(CROSSHAIR_COLOR);
            g2.drawLine((int)dataArea.getMinX(), (int)y, (int)dataArea.getMaxX(), (int)y);
            g2.drawLine((int)x, (int)dataArea.getMinY(), (int)x, (int)dataArea.getMaxY());

            // 2. 값 표시
            if (chartPanel.getChart() != null) {
                XYPlot plot = chartPanel.getChart().getXYPlot();
                ValueAxis domainAxis = plot.getDomainAxis();
                ValueAxis rangeAxis = plot.getRangeAxis();
                
                double chartX = domainAxis.java2DToValue(x, dataArea, plot.getDomainAxisEdge());
                double chartY = rangeAxis.java2DToValue(y, dataArea, plot.getRangeAxisEdge());

                String xText = String.format("%.4f", chartX);
                String yText = String.format("%.1f", chartY);

                FontMetrics fm = g2.getFontMetrics();
                int padding = 4;
                int textHeight = fm.getHeight();
                
                // X축 라벨
                int labelX_W = fm.stringWidth(xText) + (padding * 2);
                int labelX_H = textHeight + (padding * 2);
                int labelX_X = (int)x - (labelX_W / 2);
                int labelX_Y = (int)dataArea.getMaxY();

                g2.setColor(LABEL_BG_COLOR);
                g2.fillRect(labelX_X, labelX_Y, labelX_W, labelX_H);
                g2.setColor(LABEL_TEXT_COLOR);
                g2.drawString(xText, labelX_X + padding, labelX_Y + padding + fm.getAscent());

                // Y축 라벨
                int labelY_W = fm.stringWidth(yText) + (padding * 2);
                int labelY_H = textHeight + (padding * 2);
                int labelY_X = (int)dataArea.getMinX() - labelY_W;
                int labelY_Y = (int)y - (labelY_H / 2);

                g2.setColor(LABEL_BG_COLOR);
                g2.fillRect(labelY_X, labelY_Y, labelY_W, labelY_H);
                g2.setColor(LABEL_TEXT_COLOR);
                g2.drawString(yText, labelY_X + padding, labelY_Y + padding + fm.getAscent());
            }

            // 설정 복원
            g2.setStroke(originalStroke);
            g2.setColor(originalColor);
            g2.setFont(originalFont);
        }
    }

    /**
     * Interactive Slope Handle Overlay (수정됨: 라인 길이 제한)
     */
    private class SlopeHandleOverlay extends AbstractOverlay implements Overlay {
        private static final int HANDLE_SIZE = 10; // 핸들 크기 약간 키움
        
        @Override
        public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
            if (!showSlopeLine || handleStart == null || handleEnd == null) return;
            
            Rectangle2D dataArea = chartPanel.getScreenDataArea();
            XYPlot plot = chartPanel.getChart().getXYPlot();
            ValueAxis domainAxis = plot.getDomainAxis();
            ValueAxis rangeAxis = plot.getRangeAxis();
            
            double startX = domainAxis.valueToJava2D(handleStart.x, dataArea, plot.getDomainAxisEdge());
            double startY = rangeAxis.valueToJava2D(handleStart.y, dataArea, plot.getRangeAxisEdge());
            double endX = domainAxis.valueToJava2D(handleEnd.x, dataArea, plot.getDomainAxisEdge());
            double endY = rangeAxis.valueToJava2D(handleEnd.y, dataArea, plot.getRangeAxisEdge());
            
            // 1. 파란색 점선 그리기 (삭제됨: XYSeries로 대체하여 중복 방지)
            // g2.setColor(new Color(0, 0, 255, 180));
            // g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{5.0f, 5.0f}, 0.0f));
            // g2.drawLine((int)startX, (int)startY, (int)endX, (int)endY);
            
            // 2. 핸들 그리기
            drawHandle(g2, startX, startY, isDraggingStart);
            drawHandle(g2, endX, endY, isDraggingEnd);
        }
        
        private void drawHandle(Graphics2D g2, double x, double y, boolean isDragging) {
            int half = HANDLE_SIZE / 2;
            Rectangle2D rect = new Rectangle2D.Double(x - half, y - half, HANDLE_SIZE, HANDLE_SIZE);
            
            g2.setColor(isDragging ? Color.RED : new Color(33, 150, 243)); // 파란색 계열
            g2.fill(rect);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.5f)); // 테두리 두께
            g2.draw(rect);
        }
    }

    /**
     * 현재 선택된 항복점 모드를 반환합니다.
     * @return 0: Auto, 1: Offset, 2: Upper/Lower
     */
    public int getSelectedYieldMode() {
        return yieldModeComboBox.getSelectedIndex();
    }

    /**
     * 핸들 조작을 위한 마우스 리스너 설정 (수정됨: 데이터 스냅 기능 추가)
     */
    private void setupHandleListeners() {
        if (chartPanel == null) return;
        
        // 마우스 클릭 (핸들 선택)
        chartPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!showSlopeLine || handleStart == null || handleEnd == null) return;
                
                Point2D p = e.getPoint();
                Rectangle2D dataArea = chartPanel.getScreenDataArea();
                XYPlot plot = chartPanel.getChart().getXYPlot();
                ValueAxis domainAxis = plot.getDomainAxis();
                ValueAxis rangeAxis = plot.getRangeAxis();
                
                double startX = domainAxis.valueToJava2D(handleStart.x, dataArea, plot.getDomainAxisEdge());
                double startY = rangeAxis.valueToJava2D(handleStart.y, dataArea, plot.getRangeAxisEdge());
                double endX = domainAxis.valueToJava2D(handleEnd.x, dataArea, plot.getDomainAxisEdge());
                double endY = rangeAxis.valueToJava2D(handleEnd.y, dataArea, plot.getRangeAxisEdge());
                
                // 클릭 감지 범위 (10px -> 15px)
                if (p.distance(startX, startY) < 15) {
                    isDraggingStart = true;
                    chartPanel.setMouseZoomable(false);
                } else if (p.distance(endX, endY) < 15) {
                    isDraggingEnd = true;
                    chartPanel.setMouseZoomable(false);
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDraggingStart || isDraggingEnd) {
                    isDraggingStart = false;
                    isDraggingEnd = false;
                    chartPanel.setMouseZoomable(true);
                    recalculateManualProperties(); // 드래그 종료 시 재계산
                }
            }
        });
        
        // 마우스 드래그 (핸들 이동 및 데이터 스냅)
        chartPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!showSlopeLine || handleStart == null || handleEnd == null) return;
                
                Point2D p = e.getPoint();
                Rectangle2D dataArea = chartPanel.getScreenDataArea();
                XYPlot plot = chartPanel.getChart().getXYPlot();
                ValueAxis domainAxis = plot.getDomainAxis();
                ValueAxis rangeAxis = plot.getRangeAxis();
                
                double startX = domainAxis.valueToJava2D(handleStart.x, dataArea, plot.getDomainAxisEdge());
                double startY = rangeAxis.valueToJava2D(handleStart.y, dataArea, plot.getRangeAxisEdge());
                double endX = domainAxis.valueToJava2D(handleEnd.x, dataArea, plot.getDomainAxisEdge());
                double endY = rangeAxis.valueToJava2D(handleEnd.y, dataArea, plot.getRangeAxisEdge());
                
                if (p.distance(startX, startY) < 10 || p.distance(endX, endY) < 10) {
                    chartPanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                } else {
                    chartPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!isDraggingStart && !isDraggingEnd) return;
                if (currentData == null || currentData.isEmpty()) return;

                Rectangle2D dataArea = chartPanel.getScreenDataArea();
                XYPlot plot = chartPanel.getChart().getXYPlot();
                ValueAxis domainAxis = plot.getDomainAxis();
                
                // 1. 마우스 X위치를 차트 값(Strain)으로 변환
                double mouseStrain = domainAxis.java2DToValue(e.getX(), dataArea, plot.getDomainAxisEdge());
                
                // 2. 가장 가까운 데이터 포인트 찾기 (Snapping)
                StressStrainPoint closest = null;
                double minDist = Double.MAX_VALUE;
                
                for (StressStrainPoint p : currentData) {
                    double strain = (markerRefComboBox.getSelectedIndex() == 0) ? p.getEngineeringStrain() : p.getTrueStrain();
                    double dist = Math.abs(strain - mouseStrain);
                    if (dist < minDist) {
                        minDist = dist;
                        closest = p;
                    }
                }

                if (closest != null) {
                    double snapX = (markerRefComboBox.getSelectedIndex() == 0) ? closest.getEngineeringStrain() : closest.getTrueStrain();
                    double snapY = (markerRefComboBox.getSelectedIndex() == 0) ? closest.getEngineeringStress() : closest.getTrueStress();

                    // 3. 핸들 위치 업데이트 (데이터 위로 강제 이동)
                    if (isDraggingStart) {
                        // 끝점보다 뒤로 가지 못하게 제한
                        if (snapX < handleEnd.x) {
                            handleStart.setLocation(snapX, snapY);
                        }
                    } else if (isDraggingEnd) {
                        // 시작점보다 앞으로 가지 못하게 제한
                        if (snapX > handleStart.x) {
                            handleEnd.setLocation(snapX, snapY);
                        }
                    }
                    chartPanel.repaint();
                }
            }
        });
    }

    /**
     * 분석 결과가 처음 나왔을 때 핸들 위치 초기화
     */
    private void updateHandleFromAnalysis() {
        if (analysisResult == null || analysisResult.getYoungsModulus() <= 0) return;
        
        double E_GPa = analysisResult.getYoungsModulus();
        double intercept = analysisResult.getElasticLineIntercept();
        
        double maxStress = (analysisResult.getUtsPoint() != null) ? analysisResult.getUtsPoint().getTrueStress() : 500;
        double endStress = maxStress * 0.4;
        double endStrain = (endStress - intercept) / (E_GPa * 1000.0);
        
        double startStress = maxStress * 0.1;
        double startStrain = (startStress - intercept) / (E_GPa * 1000.0);
        if(startStrain < 0) startStrain = 0;

        handleStart = new Point2D.Double(startStrain, startStress);
        handleEnd = new Point2D.Double(endStrain, endStress);
    }

    /**
     * 수동 조작 후 재계산 실행
     */
    private void recalculateManualProperties() {
        if (analysisResult == null || currentData == null) return;
        
        // 마커 기준 모드 확인 (0: Eng, 1: True)
        boolean useEngineering = (getMarkerRefMode() == 0);
        
        // 1. 재계산 (모드에 따라 적절한 Strain/Stress 사용)
        AnalysisResult newResult = materialCalculator.recalculateFromManualSlope(
            currentData, analysisResult, handleStart.x, handleEnd.x, useEngineering
        );
        
        // 2. 결과 갱신
        this.analysisResult = newResult;
        
        // [수정] 핸들 Y좌표 보정 로직 제거
        // 사용자가 그래프 선상에 놓은 핸들 위치를 유지하여 "튕기는" 현상 방지
        // handleStart.y = E_MPa * handleStart.x + intercept;
        // handleEnd.y = E_MPa * handleEnd.x + intercept;
        
        // 3. 그래프 다시 그리기
        updateGraphVisualizations();
        
        // 4. 결과 패널 업데이트 및 깜빡임 효과
        if (resultPanel != null && markerRefChangedListener != null) {
            markerRefChangedListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "RECALCULATED"));
            resultPanel.flashRows(new int[]{3, 4}); // Young's Modulus & Yield Strength
        }
    }
}
