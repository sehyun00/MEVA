// src/main/java/meva/charting/core/ChartManager.java

package meva.charting.core;

import meva.charting.interactions.ChartInputHandler;
import meva.charting.overlays.CrosshairOverlay;
import meva.charting.overlays.SlopeOverlay;
import meva.charting.style.ChartStyler;
import meva.gui.CustomChartPanel;
import meva.models.AnalysisResult;
import meva.models.AnalysisResult.YieldType;
import meva.models.StressStrainPoint;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * [차트 로직 총괄 매니저]
 * - JFreeChart 객체 생성 및 관리
 * - 오버레이(Overlay) 및 입력 핸들러(InputHandler) 조립
 * - 외부(UI)와 차트 내부 로직 간의 중재자(Facade) 역할 수행
 * 
 * @author 김종현
 */
public class ChartManager {

    // --- 핵심 컴포넌트 ---
    private JFreeChart chart;
    private CustomChartPanel chartPanel;
    
    // --- 오버레이 ---
    private CrosshairOverlay crosshairOverlay;
    private SlopeOverlay slopeOverlay;
    
    // --- 핸들러 ---
    private ChartInputHandler inputHandler;
    
    // --- 데이터 상태 ---
    private List<StressStrainPoint> currentData;
    private AnalysisResult currentResult;
    private boolean isTrueStressMode = false; // 기본값: Engineering (false)

    // --- 시각화 상태 플래그 (GraphPanel에서 제어) ---
    private boolean showUTS = false;
    private boolean showYieldPoint = false;
    private boolean showSlopeLine = true;
    private boolean showElasticRegion = false;
    private boolean showPlasticRegion = false;
    
    private int selectedYieldMode = 0; // 0: Auto, 1: Offset, 2: Upper/Lower

    /**
     * 생성자
     * 차트 객체와 필수 오버레이들을 초기화합니다.
     */
    public ChartManager() {
        initializeChart();
    }

    /**
     * 차트 및 패널 초기화
     */
    private void initializeChart() {
        // 1. 빈 차트 생성
        chart = ChartFactory.createXYLineChart(
            "Stress-Strain Curve", 
            "Strain", 
            "Stress (MPa)", 
            null, 
            PlotOrientation.VERTICAL, 
            true, true, false
        );

        // 2. 스타일 적용
        ChartStyler.applyStyle(chart);

        // 3. 차트 패널 생성 (커스텀 패널 사용)
        chartPanel = new CustomChartPanel(chart);
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setDomainPannable(true);
        chartPanel.setRangePannable(true);
        chartPanel.setBackground(Color.WHITE);

        // 4. 오버레이 생성 및 등록
        crosshairOverlay = new CrosshairOverlay();
        slopeOverlay = new SlopeOverlay();

        chartPanel.addOverlay(crosshairOverlay);
        chartPanel.addOverlay(slopeOverlay);

        // 5. 입력 핸들러 연결
        inputHandler = new ChartInputHandler(chartPanel, crosshairOverlay, slopeOverlay);
    }

    /**
     * 생성된 차트 패널(View)을 반환합니다.
     * GraphPanel에 add() 하기 위함입니다.
     */
    public JPanel getChartPanel() {
        return chartPanel;
    }

    /**
     * 입력 핸들러의 리스너를 설정합니다.
     * (핸들 조작 후 재계산 요청 등을 처리)
     */
    public void setInteractionListener(ChartInputHandler.InteractionListener listener) {
        if (inputHandler != null) {
            inputHandler.setInteractionListener(listener);
        }
    }

    /**
     * 차트 데이터를 업데이트하고 다시 그립니다.
     * 
     * @param data 표시할 응력-변형률 데이터 리스트
     */
    public void updateData(List<StressStrainPoint> data) {
        if (data == null || data.isEmpty()) return;

        this.currentData = data;
        
        // 핸들러에 데이터 최신화 (스냅핑용)
        if (inputHandler != null) {
            inputHandler.updateData(data, isTrueStressMode);
        }

        // 데이터셋 다시 만들기
        XYSeriesCollection dataset = createDataset(data);
        chart.getXYPlot().setDataset(0, dataset);
        
        // 스타일 재적용 (데이터셋 바뀌면 초기화될 수 있으므로)
        ChartStyler.applyStyle(chart);
    }

    /**
     * 분석 결과를 받아 차트 시각화 요소를 갱신합니다.
     * 
     * @param result 분석 결과 객체
     * @param isManualUpdate 수동 조작(핸들 드래그)에 의한 업데이트인지 여부
     *                       true면 핸들 위치를 강제로 초기화하지 않음 (튕김 방지)
     */
    public void updateAnalysisResult(AnalysisResult result, boolean isManualUpdate) {
        this.currentResult = result;
        
        // 핸들 위치 초기화 (최초 로드 시에만, 수동 조작 시 건너뜀)
        if (!isManualUpdate && result != null && result.getYoungsModulus() > 0) {
             updateSlopeHandlesFromServer(result);
        }
        
        refreshVisuals(); // 시각화 요소(마커, 라인 등) 다시 그리기
    }

    /**
     * 시각화 옵션 설정 (체크박스 상태 반영)
     */
    public void setVisualOptions(boolean showUTS, boolean showYield, boolean showSlope, 
                               boolean showElastic, boolean showPlastic, int yieldMode) {
        this.showUTS = showUTS;
        this.showYieldPoint = showYield;
        this.showSlopeLine = showSlope;
        this.showElasticRegion = showElastic;
        this.showPlasticRegion = showPlastic;
        this.selectedYieldMode = yieldMode;
        
        setShowSlopeHandle(showSlope); // 오버레이 가시성도 연동
        refreshVisuals();
    }

    /**
     * 마커 기준(Eng/True) 모드를 변경합니다.
     */
    public void setMarkerMode(boolean isTrueStress) {
        this.isTrueStressMode = isTrueStress;
        // 데이터가 있다면 다시 그림 (축 값이 바뀌므로)
        if (currentData != null) {
            updateData(currentData);
        }
        refreshVisuals();
    }

    /**
     * 탄성계수 핸들 표시 여부 토글
     */
    public void setShowSlopeHandle(boolean show) {
        if (slopeOverlay != null) {
            slopeOverlay.setVisible(show);
        }
    }

    /**
     * 현재 핸들의 시작점 좌표(Strain, Stress)를 반환합니다.
     */
    public Point2D.Double getHandleStart() {
        return (slopeOverlay != null) ? slopeOverlay.getHandleStart() : null;
    }

    /**
     * 현재 핸들의 끝점 좌표(Strain, Stress)를 반환합니다.
     */
    public Point2D.Double getHandleEnd() {
        return (slopeOverlay != null) ? slopeOverlay.getHandleEnd() : null;
    }
    
    // --- 내부 유틸리티 ---

    /**
     * [핵심] 차트 위의 모든 시각화 요소(마커, 라인, 영역)를 다시 그립니다.
     */
    private void refreshVisuals() {
        if (chart == null || currentData == null || currentData.isEmpty()) return;

        XYPlot plot = chart.getXYPlot();
        
        // 1. 렌더링 순서: 데이터(Line) -> 마커(Shape) 순서로 그려야 점이 선 위에 보임
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        
        // 2. 기존 요소 클리어
        plot.clearAnnotations();
        plot.clearDomainMarkers(); // 영역 표시 제거
        
        // 데이터셋 슬롯 1, 2 비우기 (1: 마커, 2: 보조선)
        plot.setDataset(1, null);
        plot.setDataset(2, null);

        if (currentResult == null) return;

        // --- 로직 시작 ---
        StressStrainPoint utsPoint = currentResult.getUtsPoint();
        StressStrainPoint displayYieldPoint = null;
        StressStrainPoint displayUpperYield = null;
        StressStrainPoint displayLowerYield = null;

        // 항복점 모드 선택 로직
        if (selectedYieldMode == 0) { // Auto
            if (currentResult.getYieldType() == YieldType.DISCONTINUOUS) {
                displayUpperYield = currentResult.getUpperYieldPoint();
                displayLowerYield = currentResult.getLowerYieldPoint();
            } else {
                // Engineering 모드일 때 Eng Offset Point 우선 사용
                if (!isTrueStressMode && currentResult.getOffsetYieldPointEng() != null) {
                    displayYieldPoint = currentResult.getOffsetYieldPointEng();
                } else {
                    displayYieldPoint = currentResult.getYieldPoint();
                }
            }
        } else if (selectedYieldMode == 1) { // Force 0.2% Offset
            if (!isTrueStressMode && currentResult.getOffsetYieldPointEng() != null) {
                displayYieldPoint = currentResult.getOffsetYieldPointEng();
            } else {
                displayYieldPoint = currentResult.getOffsetYieldPoint();
            }
            // Fallback
            if (displayYieldPoint == null && currentResult.getYieldType() == YieldType.OFFSET_02) {
                displayYieldPoint = currentResult.getYieldPoint();
            }
        } else if (selectedYieldMode == 2) { // Force Upper/Lower
            displayUpperYield = currentResult.getUpperYieldPoint();
            displayLowerYield = currentResult.getLowerYieldPoint();
        }

        // 기준 항복점 (영역 표시에 사용)
        StressStrainPoint refYield = (displayYieldPoint != null) ? displayYieldPoint : 
                                   ((displayUpperYield != null) ? displayUpperYield : currentResult.getYieldPoint());

        // 3. 보조선 (Slope & Offset Line) 그리기
        XYSeriesCollection slopeDataset = new XYSeriesCollection();
        
        if (currentResult.getYoungsModulus() > 0) {
            double E_MPa = isTrueStressMode ? 
                currentResult.getYoungsModulus() * 1000.0 : 
                currentResult.getYoungsModulusEng() * 1000.0;
            
            // Fallback (Eng 값이 없으면 True 값 사용)
            if (!isTrueStressMode && E_MPa == 0.0 && currentResult.getYoungsModulus() > 0) {
                E_MPa = currentResult.getYoungsModulus() * 1000.0;
            }
            
            double intercept = isTrueStressMode ? 
                currentResult.getElasticLineIntercept() : 
                currentResult.getElasticLineInterceptEng();

            // Y축 최대값 추정 (선 길이 제한용)
            double maxY = (refYield != null) ? 
                (isTrueStressMode ? refYield.getTrueStress() : refYield.getEngineeringStress()) * 1.2 : 500.0;

            // 3-1. 탄성 기울기 선 (Elastic Slope)
            XYSeries elasticSeries = new XYSeries("Elastic Slope");
            
            Point2D.Double hStart = getHandleStart();
            Point2D.Double hEnd = getHandleEnd();

            if (hStart != null && hEnd != null) {
                // 핸들이 있으면 핸들 구간만 그림 (ChartManager가 관리하는 SlopeOverlay와 일치)
                double startY = E_MPa * hStart.x + intercept;
                double endY = E_MPa * hEnd.x + intercept;
                elasticSeries.add(hStart.x, startY);
                elasticSeries.add(hEnd.x, endY);
            } else {
                // 핸들 없으면 기본 계산
                elasticSeries.add(0.0, intercept);
                double endX = (maxY - intercept) / E_MPa;
                if (endX < 0) endX = 0;
                elasticSeries.add(endX, maxY);
            }
            slopeDataset.addSeries(elasticSeries);

            // 3-2. 0.2% Offset Line
            double offsetX = 0.002;
            XYSeries offsetSeries = new XYSeries("0.2% Offset");
            
            double startX = (hStart != null) ? hStart.x : 0.0;
            double offX1 = startX + offsetX;
            double offY1 = E_MPa * startX + intercept;
            
            double offX2 = (maxY - intercept) / E_MPa + offsetX;
            double offY2 = maxY;
            
            offsetSeries.add(offX1, offY1);
            offsetSeries.add(offX2, offY2);
            slopeDataset.addSeries(offsetSeries);
        }

        // 4. 마커 포인트 (Points) 그리기
        XYSeriesCollection pointDataset = new XYSeriesCollection();
        XYLineAndShapeRenderer pointRenderer = new XYLineAndShapeRenderer(false, true); // 선 끔, 모양 켬
        pointRenderer.setAutoPopulateSeriesPaint(false);
        pointRenderer.setAutoPopulateSeriesShape(false);
        
        int seriesIndex = 0;

        // UTS
        if (showUTS && utsPoint != null) {
            XYSeries utsSeries = new XYSeries("UTS");
            double x = isTrueStressMode ? utsPoint.getTrueStrain() : utsPoint.getEngineeringStrain();
            double y = isTrueStressMode ? utsPoint.getTrueStress() : utsPoint.getEngineeringStress();
            utsSeries.add(x, y);
            pointDataset.addSeries(utsSeries);
            
            pointRenderer.setSeriesPaint(seriesIndex, ChartStyler.UTS_POINT_COLOR);
            pointRenderer.setSeriesShape(seriesIndex, new Ellipse2D.Double(-4, -4, 8, 8));
            
            addAnnotation("UTS", x, y, -Math.PI/4, ChartStyler.UTS_POINT_COLOR, plot);
            seriesIndex++;
        }

        // Yield Point (Single)
        if (showYieldPoint && displayYieldPoint != null) {
            XYSeries yieldSeries = new XYSeries("Yield");
            double x = isTrueStressMode ? displayYieldPoint.getTrueStrain() : displayYieldPoint.getEngineeringStrain();
            double y = isTrueStressMode ? displayYieldPoint.getTrueStress() : displayYieldPoint.getEngineeringStress();
            yieldSeries.add(x, y);
            pointDataset.addSeries(yieldSeries);
            
            pointRenderer.setSeriesPaint(seriesIndex, ChartStyler.YIELD_POINT_COLOR);
            pointRenderer.setSeriesShape(seriesIndex, new Ellipse2D.Double(-4, -4, 8, 8));
            
            addAnnotation("Yield", x, y, Math.PI/2, ChartStyler.YIELD_POINT_COLOR, plot);
            seriesIndex++;
        }

        // Upper/Lower Yield
        if (showYieldPoint && (displayUpperYield != null || displayLowerYield != null)) {
            if (displayUpperYield != null) {
                XYSeries uypSeries = new XYSeries("UYP");
                double x = isTrueStressMode ? displayUpperYield.getTrueStrain() : displayUpperYield.getEngineeringStrain();
                double y = isTrueStressMode ? displayUpperYield.getTrueStress() : displayUpperYield.getEngineeringStress();
                uypSeries.add(x, y);
                pointDataset.addSeries(uypSeries);
                
                pointRenderer.setSeriesPaint(seriesIndex, ChartStyler.UPPER_YIELD_COLOR);
                pointRenderer.setSeriesShape(seriesIndex, new Ellipse2D.Double(-4, -4, 8, 8));
                addAnnotation("UYP", x, y, Math.PI/4, ChartStyler.UPPER_YIELD_COLOR, plot);
                seriesIndex++;
            }
            if (displayLowerYield != null) {
                XYSeries lypSeries = new XYSeries("LYP");
                double x = isTrueStressMode ? displayLowerYield.getTrueStrain() : displayLowerYield.getEngineeringStrain();
                double y = isTrueStressMode ? displayLowerYield.getTrueStress() : displayLowerYield.getEngineeringStress();
                lypSeries.add(x, y);
                pointDataset.addSeries(lypSeries);
                
                pointRenderer.setSeriesPaint(seriesIndex, ChartStyler.UPPER_YIELD_COLOR);
                pointRenderer.setSeriesShape(seriesIndex, new Ellipse2D.Double(-4, -4, 8, 8));
                addAnnotation("LYP", x, y, -Math.PI/2, ChartStyler.UPPER_YIELD_COLOR, plot);
                seriesIndex++;
            }
        }

        // 5. 데이터셋 및 렌더러 적용
        if (pointDataset.getSeriesCount() > 0) {
            plot.setDataset(1, pointDataset);
            plot.setRenderer(1, pointRenderer);
        }
        
        if (showSlopeLine && slopeDataset.getSeriesCount() > 0) {
            plot.setDataset(2, slopeDataset);
            
            XYLineAndShapeRenderer slopeRenderer = new XYLineAndShapeRenderer(true, false);
            slopeRenderer.setSeriesPaint(0, ChartStyler.ELASTIC_SLOPE_COLOR); // 탄성선
            slopeRenderer.setSeriesStroke(0, ChartStyler.SLOPE_STROKE);
            
            slopeRenderer.setSeriesPaint(1, ChartStyler.OFFSET_LINE_COLOR); // Offset선
            slopeRenderer.setSeriesStroke(1, ChartStyler.SLOPE_STROKE);
            
            plot.setRenderer(2, slopeRenderer);
        }

        // 6. 영역 표시 (Background Regions)
        if (refYield != null) {
            double yieldStrain = isTrueStressMode ? refYield.getTrueStrain() : refYield.getEngineeringStrain();
            double maxStrain = isTrueStressMode ? 
                currentData.get(currentData.size()-1).getTrueStrain() : 
                currentData.get(currentData.size()-1).getEngineeringStrain();

            if (showElasticRegion) {
                IntervalMarker elasticMarker = new IntervalMarker(0.0, yieldStrain);
                elasticMarker.setPaint(new Color(0, 0, 255, 30)); // 연한 파랑
                elasticMarker.setLabel("Elastic");
                elasticMarker.setLabelAnchor(RectangleAnchor.TOP_LEFT);
                elasticMarker.setLabelTextAnchor(TextAnchor.TOP_LEFT);
                plot.addDomainMarker(elasticMarker);
            }
            
            if (showPlasticRegion) {
                IntervalMarker plasticMarker = new IntervalMarker(yieldStrain, maxStrain);
                plasticMarker.setPaint(new Color(255, 0, 0, 30)); // 연한 빨강
                plasticMarker.setLabel("Plastic");
                plasticMarker.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
                plasticMarker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
                plot.addDomainMarker(plasticMarker);
            }
        }
    }

    private void addAnnotation(String label, double x, double y, double angle, Color color, XYPlot plot) {
        XYPointerAnnotation annotation = new XYPointerAnnotation(
            String.format("%s (%.1f)", label, y), x, y, angle
        );
        annotation.setTipRadius(10.0);
        annotation.setBaseRadius(35.0);
        annotation.setFont(new Font("SansSerif", Font.BOLD, 11));
        annotation.setPaint(color);
        annotation.setArrowPaint(color);
        plot.addAnnotation(annotation);
    }

    private XYSeriesCollection createDataset(List<StressStrainPoint> data) {
        XYSeries trueSeries = new XYSeries("True Stress-Strain");
        XYSeries engSeries = new XYSeries("Engineering Stress-Strain");

        for (StressStrainPoint p : data) {
            trueSeries.add(p.getTrueStrain(), p.getTrueStress());
            engSeries.add(p.getEngineeringStrain(), p.getEngineeringStress());
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(trueSeries);
        dataset.addSeries(engSeries);
        
        return dataset;
    }

    /**
     * 분석 결과로부터 초기 핸들 위치를 계산하여 오버레이에 설정
     */
    private void updateSlopeHandlesFromServer(AnalysisResult result) {
        // 기존 GraphPanel 로직 이식
        double E_GPa = isTrueStressMode ? result.getYoungsModulus() : result.getYoungsModulusEng();
        double intercept = isTrueStressMode ? result.getElasticLineIntercept() : result.getElasticLineInterceptEng();
        
        // 값이 없으면 True 값으로 Fallback
        if (E_GPa == 0) E_GPa = result.getYoungsModulus();
        if (intercept == 0) intercept = result.getElasticLineIntercept();

        double maxStress = (result.getUtsPoint() != null) ? 
            (isTrueStressMode ? result.getUtsPoint().getTrueStress() : result.getUtsPoint().getEngineeringStress()) : 500;

        // 핸들 위치 계산 (10% ~ 40% 지점)
        double endStress = maxStress * 0.4;
        double endStrain = (endStress - intercept) / (E_GPa * 1000.0);
        
        double startStress = maxStress * 0.1;
        double startStrain = (startStress - intercept) / (E_GPa * 1000.0);
        if (startStrain < 0) startStrain = 0;

        if (slopeOverlay != null) {
            slopeOverlay.setHandles(
                new Point2D.Double(startStrain, startStress),
                new Point2D.Double(endStrain, endStress)
            );
        }
    }
}