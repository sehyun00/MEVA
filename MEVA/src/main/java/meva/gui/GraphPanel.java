// src/main/java/meva/gui/GraphPanel.java

package meva.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.event.ActionListener;
import java.util.List;

// JFreeChart imports
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.Overlay;
import java.awt.geom.Ellipse2D;
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

/**
 * 응력-변형률 곡선 그래프를 표시하는 패널
 * GUI 설계 문서에 따른 완전한 구현
 * 
 * @author MEVA 개발팀
 * @version 1.3 - 리팩토링: MVC 패턴 적용 (계산 로직 제거)
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
    private JCheckBox elasticRegionCheckBox;
    private JCheckBox plasticRegionCheckBox;

    // 상태 플래그
    private boolean showUTS;
    private boolean showYieldPoint;
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
    
    /**
     * GraphPanel 생성자
     */
    public GraphPanel() {
        initializeComponents();
        setupLayout();
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
        helpLabel.setToolTipText("<html>실험 조건(인장 속도 등)이나 재료 특성에 따라<br>자동 감지가 부정확할 수 있습니다.<br>필요 시 수동으로 모드를 변경하세요.</html>");
        
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
        yieldModeComboBox.addActionListener(e -> updateGraphVisualizations());

        // 마커 기준(Reference) 선택 콤보박스
        String[] refModes = { "Engineering (공칭)", "True (진)" };
        markerRefComboBox = new JComboBox<>(refModes);
        markerRefComboBox.setFont(new Font("SansSerif", Font.PLAIN, 11));
        markerRefComboBox.setPreferredSize(new Dimension(130, 22));
        markerRefComboBox.setSelectedIndex(0); // 기본값: Engineering
        markerRefComboBox.addActionListener(e -> updateGraphVisualizations());

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
                displayYieldPoint = analysisResult.getYieldPoint(); // Offset Point
            }
        } else if (selectedMode == 1) { // Force 0.2% Offset
            // "Dual Calculation" 덕분에 YieldType과 상관없이 항상 Offset 값을 가져올 수 있음
            displayYieldPoint = analysisResult.getOffsetYieldPoint();
            
            // 만약 (매우 드물게) 오프셋 포인트가 null이라면, Auto 로직의 결과가 Offset일 경우 그 값을 백업으로 사용
            if (displayYieldPoint == null && analysisResult.getYieldType() == AnalysisResult.YieldType.OFFSET_02) {
                displayYieldPoint = analysisResult.getYieldPoint();
            }
        } else if (selectedMode == 2) { // Force Upper/Lower
            displayUpperYield = analysisResult.getUpperYieldPoint();
            displayLowerYield = analysisResult.getLowerYieldPoint();
        }

        // 2. 특수 포인트(UTS, Yield)를 위한 별도의 데이터셋 및 렌더러 준비
        XYSeriesCollection specialPointsDataset = new XYSeriesCollection();
        XYLineAndShapeRenderer pointRenderer = new XYLineAndShapeRenderer(false, true); // 선 끔, 모양 켬
        pointRenderer.setAutoPopulateSeriesPaint(false);
        pointRenderer.setAutoPopulateSeriesShape(false);
        
        int seriesIndex = 0;

        // 3-1. UTS 포인트 처리
        if (showUTS && utsPoint != null) {
            XYSeries utsSeries = new XYSeries("UTS Point");
            // 사용자가 선택한 기준(Eng/True)에 따라 좌표값 결정
            double strain = useEngineering ? utsPoint.getEngineeringStrain() : utsPoint.getTrueStrain();
            double stress = useEngineering ? utsPoint.getEngineeringStress() : utsPoint.getTrueStress();
            
            utsSeries.add(strain, stress);
            specialPointsDataset.addSeries(utsSeries);
            
            // 렌더러 스타일 설정 (빨간색, 8x8 픽셀 원)
            pointRenderer.setSeriesPaint(seriesIndex, Color.RED);
            pointRenderer.setSeriesShape(seriesIndex, new Ellipse2D.Double(-4.0, -4.0, 8.0, 8.0));
            
            // 텍스트 라벨 (어노테이션) 추가
            XYPointerAnnotation utsAnnotation = new XYPointerAnnotation(
                String.format("UTS (%.1f MPa)", stress),
                strain,
                stress,
                -Math.PI / 4.0 // 45도
            );
            utsAnnotation.setTipRadius(10.0);
            utsAnnotation.setBaseRadius(35.0);
            utsAnnotation.setFont(new Font("SansSerif", Font.BOLD, 12));
            utsAnnotation.setPaint(Color.RED);
            utsAnnotation.setArrowPaint(Color.RED);
            plot.addAnnotation(utsAnnotation);
            
            seriesIndex++;
        }

        // 3-2. 항복점 포인트 처리 (단일 포인트 - 주로 Offset)
        if (showYieldPoint && displayYieldPoint != null) {
            XYSeries yieldSeries = new XYSeries("Yield Point");
            
            double strain = useEngineering ? displayYieldPoint.getEngineeringStrain() : displayYieldPoint.getTrueStrain();
            double stress = useEngineering ? displayYieldPoint.getEngineeringStress() : displayYieldPoint.getTrueStress();
            
            yieldSeries.add(strain, stress);
            specialPointsDataset.addSeries(yieldSeries);
            
            pointRenderer.setSeriesPaint(seriesIndex, new Color(0, 150, 0)); // 녹색
            pointRenderer.setSeriesShape(seriesIndex, new Ellipse2D.Double(-4.0, -4.0, 8.0, 8.0));
            
            XYPointerAnnotation yieldAnnotation = new XYPointerAnnotation(
                String.format("Yield (%.1f MPa)", stress),
                strain,
                stress,
                Math.PI / 2.0 
            );
            yieldAnnotation.setTipRadius(10.0);
            yieldAnnotation.setBaseRadius(35.0);
            yieldAnnotation.setFont(new Font("SansSerif", Font.BOLD, 12));
            yieldAnnotation.setPaint(new Color(0, 150, 0));
            yieldAnnotation.setArrowPaint(new Color(0, 150, 0));
            plot.addAnnotation(yieldAnnotation);
            
            seriesIndex++;
        }
        
        // 3-3. 상/하항복점 포인트 처리 (두 개 포인트)
        if (showYieldPoint && (displayUpperYield != null || displayLowerYield != null)) {
            // 상항복점
            if (displayUpperYield != null) {
                XYSeries upperSeries = new XYSeries("Upper Yield");
                
                double strain = useEngineering ? displayUpperYield.getEngineeringStrain() : displayUpperYield.getTrueStrain();
                double stress = useEngineering ? displayUpperYield.getEngineeringStress() : displayUpperYield.getTrueStress();
                
                upperSeries.add(strain, stress);
                specialPointsDataset.addSeries(upperSeries);
                
                pointRenderer.setSeriesPaint(seriesIndex, new Color(255, 140, 0)); // 주황색
                pointRenderer.setSeriesShape(seriesIndex, new Ellipse2D.Double(-4.0, -4.0, 8.0, 8.0));
                
                XYPointerAnnotation upperAnnotation = new XYPointerAnnotation(
                    String.format("UYP (%.1f)", stress),
                    strain,
                    stress,
                    Math.PI / 4.0 
                );
                upperAnnotation.setTipRadius(10.0);
                upperAnnotation.setBaseRadius(30.0);
                upperAnnotation.setFont(new Font("SansSerif", Font.BOLD, 11));
                upperAnnotation.setPaint(new Color(255, 140, 0));
                upperAnnotation.setArrowPaint(new Color(255, 140, 0));
                plot.addAnnotation(upperAnnotation);
                seriesIndex++;
            }
            
            // 하항복점
            if (displayLowerYield != null) {
                XYSeries lowerSeries = new XYSeries("Lower Yield");
                
                double strain = useEngineering ? displayLowerYield.getEngineeringStrain() : displayLowerYield.getTrueStrain();
                double stress = useEngineering ? displayLowerYield.getEngineeringStress() : displayLowerYield.getTrueStress();
                
                lowerSeries.add(strain, stress);
                specialPointsDataset.addSeries(lowerSeries);
                
                pointRenderer.setSeriesPaint(seriesIndex, new Color(255, 140, 0)); // 주황색
                pointRenderer.setSeriesShape(seriesIndex, new Ellipse2D.Double(-4.0, -4.0, 8.0, 8.0));
                
                XYPointerAnnotation lowerAnnotation = new XYPointerAnnotation(
                    String.format("LYP (%.1f)", stress),
                    strain,
                    stress,
                    -Math.PI / 2.0 
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

        // 4. Plot에 보조 데이터셋과 렌더러 적용 (Index 1번 사용)
        if (specialPointsDataset.getSeriesCount() > 0) {
            plot.setDataset(1, specialPointsDataset);
            plot.setRenderer(1, pointRenderer);
        } else {
            plot.setDataset(1, null); // 포인트가 없으면 데이터셋 제거
        }

        // 5. 영역 표시 (IntervalMarker 유지 - Domain 축)
        // 영역 표시는 현재 시각화된 항복점을 기준으로 함 (시각화 동기화)
        StressStrainPoint refYield = null;
        
        if (displayYieldPoint != null) {
            refYield = displayYieldPoint;
        } else if (displayUpperYield != null) {
            refYield = displayUpperYield;
        } else {
            // 표시된 점이 없으면 기본 계산 결과 사용 (Fallback)
            refYield = analysisResult.getYieldPoint();
        }
        
        if (refYield != null) {
            double yieldStrain = useEngineering ? refYield.getEngineeringStrain() : refYield.getTrueStrain();
            double maxStrain = useEngineering ? 
                currentData.get(currentData.size() - 1).getEngineeringStrain() : 
                currentData.get(currentData.size() - 1).getTrueStrain();

            if (showElasticRegion) {
                IntervalMarker elasticMarker = new IntervalMarker(
                    0.0, 
                    yieldStrain
                );
                elasticMarker.setPaint(new Color(0, 0, 255, 30)); // 파란색, 투명도 30
                elasticMarker.setLabel("Elastic Region");
                elasticMarker.setLabelFont(new Font("SansSerif", Font.ITALIC, 11));
                elasticMarker.setLabelAnchor(RectangleAnchor.TOP_LEFT);
                elasticMarker.setLabelTextAnchor(TextAnchor.TOP_LEFT);
                plot.addDomainMarker(elasticMarker);
            }

            if (showPlasticRegion) {
                IntervalMarker plasticMarker = new IntervalMarker(
                    yieldStrain,
                    maxStrain
                );
                plasticMarker.setPaint(new Color(255, 0, 0, 30)); // 붉은색, 투명도 30
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

    /**
     * 인터랙티브 크로스헤어 오버레이
     * 마우스 위치에 점선 십자선을 그리고, 축 영역에 현재 값을 박스 형태로 표시
     */
    private class InteractiveCrosshairOverlay implements Overlay {
        private final Stroke DASHED_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, 
                BasicStroke.JOIN_MITER, 10.0f, new float[]{4.0f, 4.0f}, 0.0f);
        private final Color CROSSHAIR_COLOR = new Color(100, 100, 100, 180); // 회색
        private final Color LABEL_BG_COLOR = Color.BLACK;
        private final Color LABEL_TEXT_COLOR = Color.WHITE;
        private final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 11);

        @Override
        public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
            if (mousePoint == null) return;

            ChartRenderingInfo info = chartPanel.getChartRenderingInfo();
            // info가 아직 초기화되지 않았거나 plot 정보가 없으면 리턴
            if (info == null || info.getPlotInfo() == null) return;
            
            Rectangle2D dataArea = info.getPlotInfo().getDataArea();

            // 마우스가 데이터 영역 안에 있는지 확인
            if (!dataArea.contains(mousePoint)) return;

            double x = mousePoint.getX();
            double y = mousePoint.getY();

            // 기존 그래픽스 설정 저장
            Stroke originalStroke = g2.getStroke();
            Color originalColor = g2.getColor();
            Font originalFont = g2.getFont();
            g2.setFont(LABEL_FONT);

            // 1. 십자선 그리기 (점선)
            g2.setStroke(DASHED_STROKE);
            g2.setColor(CROSSHAIR_COLOR);
            g2.drawLine((int)dataArea.getMinX(), (int)y, (int)dataArea.getMaxX(), (int)y); // 수평선
            g2.drawLine((int)x, (int)dataArea.getMinY(), (int)x, (int)dataArea.getMaxY()); // 수직선

            // 2. 값 계산 (화면 좌표 -> 데이터 좌표)
            if (chartPanel.getChart() != null) {
                XYPlot plot = chartPanel.getChart().getXYPlot();
                ValueAxis domainAxis = plot.getDomainAxis();
                ValueAxis rangeAxis = plot.getRangeAxis();
                
                // 데이터 영역(Rectangle2D)과 축의 방향(Edge)을 이용해 값 변환
                double chartX = domainAxis.java2DToValue(x, dataArea, plot.getDomainAxisEdge());
                double chartY = rangeAxis.java2DToValue(y, dataArea, plot.getRangeAxisEdge());

                String xText = String.format("%.4f", chartX); // Strain (소수점 4자리)
                String yText = String.format("%.1f", chartY); // Stress (소수점 1자리)

                FontMetrics fm = g2.getFontMetrics();
                int xTextWidth = fm.stringWidth(xText);
                int yTextWidth = fm.stringWidth(yText);
                int textHeight = fm.getHeight();
                int padding = 4;

                // 3. X축 라벨 그리기 (하단)
                int labelX_W = xTextWidth + (padding * 2);
                int labelX_H = textHeight + (padding * 2);
                int labelX_X = (int)x - (labelX_W / 2);
                int labelX_Y = (int)dataArea.getMaxY(); // 데이터 영역 바로 아래

                // 라벨 배경 박스
                g2.setColor(LABEL_BG_COLOR);
                g2.fillRect(labelX_X, labelX_Y, labelX_W, labelX_H);
                
                // 라벨 텍스트
                g2.setColor(LABEL_TEXT_COLOR);
                g2.drawString(xText, labelX_X + padding, labelX_Y + padding + fm.getAscent());

                // 4. Y축 라벨 그리기 (좌측)
                int labelY_W = yTextWidth + (padding * 2);
                int labelY_H = textHeight + (padding * 2);
                int labelY_X = (int)dataArea.getMinX() - labelY_W; // 데이터 영역 바로 왼쪽
                int labelY_Y = (int)y - (labelY_H / 2);

                // 라벨 배경 박스
                g2.setColor(LABEL_BG_COLOR);
                g2.fillRect(labelY_X, labelY_Y, labelY_W, labelY_H);

                // 라벨 텍스트
                g2.setColor(LABEL_TEXT_COLOR);
                g2.drawString(yText, labelY_X + padding, labelY_Y + padding + fm.getAscent());
            }

            // 그래픽스 설정 복원
            g2.setStroke(originalStroke);
            g2.setColor(originalColor);
            g2.setFont(originalFont);
        }

        @Override
        public void addChangeListener(org.jfree.chart.event.OverlayChangeListener listener) {}
        @Override
        public void removeChangeListener(org.jfree.chart.event.OverlayChangeListener listener) {}
    }
}
