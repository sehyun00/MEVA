// src/main/java/meva/charting/core/ChartManager.java

package meva.charting.core;

import meva.charting.interactions.ChartInputHandler;
import meva.charting.overlays.AreaHighlightOverlay;
import meva.charting.overlays.CrosshairOverlay;
import meva.charting.overlays.SlopeOverlay;
import meva.charting.style.ChartStyler;
import meva.gui.CustomChartPanel;
import meva.models.AnalysisResult;
import meva.models.StressStrainPoint;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * [차트 로직 총괄 매니저] (Refactored)
 * - 오버레이(Overlay) 및 입력 핸들러(InputHandler) 관리
 * - 순수 렌더링 로직은 ChartRenderer에게 위임
 * 
 * @author 김종현, MEVA Refactoring Team
 */
public class ChartManager {

    // --- 핵심 컴포넌트 ---
    private JFreeChart chart;
    private CustomChartPanel chartPanel;
    private final ChartRenderer chartRenderer; // [NEW] 렌더러

    // --- 오버레이 ---
    private CrosshairOverlay crosshairOverlay;
    private SlopeOverlay slopeOverlay;
    private AreaHighlightOverlay areaHighlightOverlay;

    // --- 핸들러 ---
    private ChartInputHandler inputHandler;

    // --- 데이터 상태 ---
    private List<StressStrainPoint> currentData;
    private AnalysisResult currentResult;
    private boolean isTrueStressMode = false; // 기본값: Engineering (false)

    // --- 시각화 상태 플래그 ---
    private boolean showResilience = true;
    private boolean showToughness = true;

    private int selectedYieldMode = 0;

    // Visualization Option Flags
    private boolean showUTS;
    private boolean showYieldPoint;
    private boolean showSlopeLine;
    private boolean showElasticRegion;
    private boolean showPlasticRegion;

    /**
     * 생성자
     */
    public ChartManager() {
        this.chartRenderer = new ChartRenderer();
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
                true, true, false);

        // 2. 스타일 적용
        ChartStyler.applyStyle(chart);

        // 3. 차트 패널 생성
        chartPanel = new CustomChartPanel(chart);
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setDomainPannable(true);
        chartPanel.setRangePannable(true);
        chartPanel.setBackground(Color.WHITE);

        // 4. 오버레이 생성 및 등록
        areaHighlightOverlay = new AreaHighlightOverlay(chart.getXYPlot());
        crosshairOverlay = new CrosshairOverlay();
        slopeOverlay = new SlopeOverlay();

        chartPanel.addOverlay(areaHighlightOverlay);
        chartPanel.addOverlay(crosshairOverlay);
        chartPanel.addOverlay(slopeOverlay);

        // 5. 입력 핸들러 연결
        inputHandler = new ChartInputHandler(chartPanel, crosshairOverlay, slopeOverlay);
    }

    public JPanel getChartPanel() {
        return chartPanel;
    }

    public void setInteractionListener(ChartInputHandler.InteractionListener listener) {
        if (inputHandler != null) {
            inputHandler.setInteractionListener(listener);
        }
    }

    /**
     * 차트 데이터를 업데이트하고 다시 그립니다.
     */
    public void updateData(List<StressStrainPoint> data) {
        if (data == null || data.isEmpty())
            return;

        this.currentData = data;

        // 핸들러에 데이터 최신화
        if (inputHandler != null) {
            inputHandler.updateData(data, isTrueStressMode);
        }

        // [Delegation] 데이터셋 생성은 렌더러 헬퍼 사용
        XYSeriesCollection dataset = chartRenderer.createMainDataset(data);
        chart.getXYPlot().setDataset(0, dataset);

        ChartStyler.applyStyle(chart);
    }

    /**
     * 분석 결과를 받아 차트 시각화 요소를 갱신합니다.
     */
    public void updateAnalysisResult(AnalysisResult result, boolean isManualUpdate) {
        this.currentResult = result;

        // 핸들 위치 초기화 (최초 로드 시에만)
        if (!isManualUpdate && result != null && result.getYoungsModulus() > 0) {
            updateSlopeHandlesFromServer(result);
        }

        // 오버레이 데이터 갱신 (Resilience & Toughness)
        updateOverlayData(result);

        refreshVisuals(); // [Delegation]
    }

    private void refreshVisuals() {
        // [Delegation] 렌더러에게 그리기 위임
        // 필요한 정보: 차트 객체, 데이터, 결과, 핸들 위치
        chartRenderer.render(
                chart,
                currentData,
                currentResult,
                getHandleStart(),
                getHandleEnd());
    }

    /**
     * AreaHighlightOverlay에 필요한 데이터를 설정합니다.
     * (Highlight Overlay는 렌더러가 아닌 매니저/패널 레벨에서 관리됨)
     */
    public void setVisualOptions(boolean showUTS, boolean showYield, boolean showSlope,
            boolean showElastic, boolean showPlastic, int yieldMode) {
        this.showUTS = showUTS;
        this.showYieldPoint = showYield;
        this.showSlopeLine = showSlope;
        this.showElasticRegion = showElastic;
        this.showPlasticRegion = showPlastic;
        this.selectedYieldMode = yieldMode;

        // Delegate to Renderer
        chartRenderer.setVisualOptions(showUTS, showYield, showSlope, showElastic, showPlastic, true, yieldMode);

        // Update Overlays (Yield Mode determines which yield point is used for
        // Resilience)
        if (currentResult != null) {
            updateOverlayData(currentResult);
        }

        // Update Slope Overlay Visibility
        if (slopeOverlay != null) {
            slopeOverlay.setVisible(showSlope);
        }

        refreshVisuals();
    }

    public void setAreaHighlightOptions(boolean showResilience, boolean showToughness, boolean useResilienceTriangle) {
        this.showResilience = showResilience;
        this.showToughness = showToughness;

        if (areaHighlightOverlay != null) {
            areaHighlightOverlay.setShowResilienceArea(showResilience);
            areaHighlightOverlay.setShowToughnessArea(showToughness);
            areaHighlightOverlay.setResilienceMode(useResilienceTriangle);
            chartPanel.repaint();
        }
    }

    // ... (markers, zoom methods) ...

    /**
     * AreaHighlightOverlay에 필요한 데이터를 설정합니다.
     */
    private void updateOverlayData(AnalysisResult result) {
        if (result == null || areaHighlightOverlay == null)
            return;

        StressStrainPoint yieldPt = getActiveYieldPoint(result);

        if (yieldPt != null && currentData != null) {
            double ey = isTrueStressMode ? yieldPt.getTrueStrain() : yieldPt.getEngineeringStrain();
            double sy = isTrueStressMode ? yieldPt.getTrueStress() : yieldPt.getEngineeringStress();

            double E_GPa = isTrueStressMode ? result.getYoungsModulus() : result.getYoungsModulusEng();
            double intercept = isTrueStressMode ? result.getElasticLineIntercept()
                    : result.getElasticLineInterceptEng();

            if (!isTrueStressMode && E_GPa == 0) {
                E_GPa = result.getYoungsModulus();
                intercept = result.getElasticLineIntercept();
            }

            // [Modified] User Feedback: Graph start point and area bottom mismatch fix
            // Force startX to 0 unless significant offset is detected.
            // Old version likely assumed origin.
            double startX = 0.0;

            // Only calculate offset if intercept is significantly non-zero and relevant
            // But for visual consistency with a "Zero Offset" curve, standardizing to 0 is
            // safer
            // unless the curve clearly starts elsewhere.
            // Checking the first data point:
            if (!currentData.isEmpty()) {
                StressStrainPoint p0 = currentData.get(0);
                double e0 = isTrueStressMode ? p0.getTrueStrain() : p0.getEngineeringStrain();
                // If the curve starts near 0, force startX to 0 (or e0)
                if (Math.abs(e0) < 0.002) {
                    startX = 0.0;
                } else {
                    // If curve starts far from 0, maybe use intercept-based start
                    if (E_GPa > 0) {
                        startX = -intercept / (E_GPa * 1000.0);
                        // Sanity check
                        if (startX < -0.05 || startX > ey)
                            startX = 0.0;
                    }
                }
            }

            java.util.List<Double> resEps = new java.util.ArrayList<>();
            java.util.List<Double> resSig = new java.util.ArrayList<>();

            for (StressStrainPoint p : currentData) {
                double e = isTrueStressMode ? p.getTrueStrain() : p.getEngineeringStrain();
                double s = isTrueStressMode ? p.getTrueStress() : p.getEngineeringStress();
                if (e > ey)
                    break;
                resEps.add(e);
                resSig.add(s);
            }

            areaHighlightOverlay.setResilienceData(ey, sy, resEps, resSig, startX);
        }

        // ... (Toughness part remains same, re-paste it to be safe or use multi-replace
        // if I was smarter,
        // but replace_file_content needs contiguous block. I'll include the rest of the
        // method.)

        // 2. Toughness Data
        if (currentData != null && !currentData.isEmpty()) {
            java.util.List<Double> eps = new java.util.ArrayList<>();
            java.util.List<Double> sig = new java.util.ArrayList<>();

            for (StressStrainPoint p : currentData) {
                eps.add(isTrueStressMode ? p.getTrueStrain() : p.getEngineeringStrain());
                sig.add(isTrueStressMode ? p.getTrueStress() : p.getEngineeringStress());
            }

            StressStrainPoint fracturePt = result.getFracturePoint();
            if (fracturePt == null)
                fracturePt = currentData.get(currentData.size() - 1);

            double ef = isTrueStressMode ? fracturePt.getTrueStrain() : fracturePt.getEngineeringStrain();
            double sf = isTrueStressMode ? fracturePt.getTrueStress() : fracturePt.getEngineeringStress();

            areaHighlightOverlay.setToughnessCurve(eps, sig, ef, sf);
        }

        chartPanel.repaint();
    }

    // (Helper for Overlay) Refactored to fetch from result directly if possible, or
    // duplicate logic
    // Since Renderer handles logic for "Display Yield", Overlay needs similar logic
    // to match.
    // For now, duplicate simple logic or create a shared utility?
    // Just keep private helper here.
    private StressStrainPoint getActiveYieldPoint(AnalysisResult result) {
        if (result == null)
            return null;

        // [Modifed] 사용자 선택 모드에 따라 반환할 항복점 결정 로직 강화
        // 0: Auto (YieldType에 따름)
        // 1: Offset Method 강제
        // 2: Upper/Lower Yield (Discontinuous) 강제

        if (selectedYieldMode == 1) {
            // Force Offset
            if (!isTrueStressMode && result.getOffsetYieldPointEng() != null) {
                return result.getOffsetYieldPointEng();
            }
            return result.getOffsetYieldPoint();
        } else if (selectedYieldMode == 2) {
            // Force Discontinuous (Upper Yield)
            return result.getUpperYieldPoint();
        } else {
            // Auto (Default)
            if (result.getYieldType() == AnalysisResult.YieldType.DISCONTINUOUS) {
                return result.getUpperYieldPoint();
            }
            if (!isTrueStressMode && result.getOffsetYieldPointEng() != null) {
                return result.getOffsetYieldPointEng();
            }
            return result.getYieldPoint();
        }
    }

    public void setMarkerMode(boolean isTrueStress) {
        this.isTrueStressMode = isTrueStress;
        // [Delegation]
        chartRenderer.setMarkerMode(isTrueStress);

        if (currentData != null) {
            updateData(currentData);
        }
        // updateData inside calls refreshVisuals? No, updateData sets dataset.
        // refreshVisuals draws markers.
        refreshVisuals();
    }

    public void setShowSlopeHandle(boolean show) {
        if (slopeOverlay != null) {
            slopeOverlay.setVisible(show);
        }
    }

    public Point2D.Double getHandleStart() {
        return (slopeOverlay != null) ? slopeOverlay.getHandleStart() : null;
    }

    public Point2D.Double getHandleEnd() {
        return (slopeOverlay != null) ? slopeOverlay.getHandleEnd() : null;
    }

    // --- 차트 제어 메서드 ---
    public void zoomIn() {
        if (chartPanel != null)
            chartPanel.zoomInBoth(chartPanel.getWidth() / 2.0, chartPanel.getHeight() / 2.0);
    }

    public void zoomOut() {
        if (chartPanel != null)
            chartPanel.zoomOutBoth(chartPanel.getWidth() / 2.0, chartPanel.getHeight() / 2.0);
    }

    public void resetZoom() {
        if (chartPanel != null)
            chartPanel.restoreAutoBounds();
    }

    public void doSaveAs() {
        if (chartPanel != null)
            try {
                chartPanel.doSaveAs();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
    }

    /**
     * 분석 결과로부터 초기 핸들 위치를 계산하여 오버레이에 설정
     */
    private void updateSlopeHandlesFromServer(AnalysisResult result) {
        double E_GPa = isTrueStressMode ? result.getYoungsModulus() : result.getYoungsModulusEng();
        double intercept = isTrueStressMode ? result.getElasticLineIntercept() : result.getElasticLineInterceptEng();

        if (E_GPa == 0)
            E_GPa = result.getYoungsModulus();
        if (intercept == 0)
            intercept = result.getElasticLineIntercept();

        double maxStress = (result.getUtsPoint() != null) ? (isTrueStressMode ? result.getUtsPoint().getTrueStress()
                : result.getUtsPoint().getEngineeringStress()) : 500;

        double endStress = maxStress * 0.4;
        double endStrain = (endStress - intercept) / (E_GPa * 1000.0);

        double startStress = maxStress * 0.1;
        double startStrain = (startStress - intercept) / (E_GPa * 1000.0);
        if (startStrain < 0)
            startStrain = 0;

        if (slopeOverlay != null) {
            slopeOverlay.setHandles(
                    new Point2D.Double(startStrain, startStress),
                    new Point2D.Double(endStrain, endStress));
        }
    }
}