package meva.charting.core;

import meva.charting.style.ChartStyler;
import meva.models.AnalysisResult;
import meva.models.AnalysisResult.YieldType;
import meva.models.StressStrainPoint;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * 차트 렌더링 담당 클래스
 * JFreeChart의 Dataset 조작, Marker/Annotation 그리기 등 순수 시각화 로직 담당
 * 
 * @author MEVA 개발팀
 * @version 1.0 (Refactored)
 */
public class ChartRenderer {

    // 렌더링 옵션 플래그
    private boolean showUTS;
    private boolean showYieldPoint;
    private boolean showSlopeLine;
    private boolean showElasticRegion;
    private boolean showPlasticRegion;
    private boolean showFracturePoint; // [New] 파단점 표시 여부
    private int selectedYieldMode; // 0: Auto, 1: Offset, 2: Discontinuous

    // 상태값
    private boolean isTrueStressMode;

    public ChartRenderer() {
        // 기본값
        this.showSlopeLine = true;
        this.showFracturePoint = true; // 기본적으로 표시
    }

    /**
     * 옵션 설정
     */
    public void setVisualOptions(boolean showUTS, boolean showYield, boolean showSlope,
            boolean showElastic, boolean showPlastic, boolean showFracture, int yieldMode) {
        this.showUTS = showUTS;
        this.showYieldPoint = showYield;
        this.showSlopeLine = showSlope;
        this.showElasticRegion = showElastic;
        this.showPlasticRegion = showPlastic;
        this.showFracturePoint = showFracture;
        this.selectedYieldMode = yieldMode;
    }

    public void setMarkerMode(boolean isTrueStress) {
        this.isTrueStressMode = isTrueStress;
    }

    /**
     * 메인 렌더링 메서드
     */
    public void render(JFreeChart chart, List<StressStrainPoint> data, AnalysisResult result,
            Point2D.Double handleStart, Point2D.Double handleEnd) {

        if (chart == null || data == null || data.isEmpty())
            return;

        XYPlot plot = chart.getXYPlot();

        // 1. 렌더링 순서 & 초기화
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        plot.clearAnnotations();
        plot.clearDomainMarkers();
        plot.setDomainGridlinesVisible(true);
        plot.setDataset(1, null);
        plot.setDataset(2, null);

        if (result == null)
            return;

        // 2. 중요 포인트 결정 (항복점 등)
        StressStrainPoint utsPoint = result.getUtsPoint();
        StressStrainPoint displayYieldPoint = null;
        StressStrainPoint displayUpperYield = null;
        StressStrainPoint displayLowerYield = null;

        if (selectedYieldMode == 0) { // Auto
            if (result.getYieldType() == YieldType.DISCONTINUOUS) {
                displayUpperYield = result.getUpperYieldPoint();
                displayLowerYield = result.getLowerYieldPoint();
            } else {
                if (!isTrueStressMode && result.getOffsetYieldPointEng() != null) {
                    displayYieldPoint = result.getOffsetYieldPointEng();
                } else {
                    displayYieldPoint = result.getYieldPoint();
                }
            }
        } else if (selectedYieldMode == 1) { // Force Offset
            if (!isTrueStressMode && result.getOffsetYieldPointEng() != null) {
                displayYieldPoint = result.getOffsetYieldPointEng();
            } else {
                displayYieldPoint = result.getOffsetYieldPoint();
            }
            if (displayYieldPoint == null && result.getYieldType() == YieldType.OFFSET_02) {
                displayYieldPoint = result.getYieldPoint();
            }
        } else if (selectedYieldMode == 2) { // Force Discontinuous
            displayUpperYield = result.getUpperYieldPoint();
            displayLowerYield = result.getLowerYieldPoint();
        }

        StressStrainPoint refYield = (displayYieldPoint != null) ? displayYieldPoint
                : ((displayUpperYield != null) ? displayUpperYield : result.getYieldPoint());

        // 3. 보조선 (Slope & Offset Line) 그리기
        if (showSlopeLine) {
            renderSlopeLines(plot, result, refYield, handleStart, handleEnd);
        }

        // 4. 마커 포인트 그리기
        renderPoints(plot, utsPoint, displayYieldPoint, displayUpperYield, displayLowerYield, data);

        // 5. 영역 표시
        renderRegions(plot, data, refYield);
    }

    private void renderSlopeLines(XYPlot plot, AnalysisResult result, StressStrainPoint refYield,
            Point2D.Double hStart, Point2D.Double hEnd) {
        if (result.getYoungsModulus() <= 0)
            return;

        XYSeriesCollection slopeDataset = new XYSeriesCollection();

        double E_MPa = isTrueStressMode ? result.getYoungsModulus() * 1000.0 : result.getYoungsModulusEng() * 1000.0;

        if (!isTrueStressMode && E_MPa == 0.0 && result.getYoungsModulus() > 0) {
            E_MPa = result.getYoungsModulus() * 1000.0;
        }

        double intercept = isTrueStressMode ? result.getElasticLineIntercept() : result.getElasticLineInterceptEng();

        double maxY = (refYield != null)
                ? (isTrueStressMode ? refYield.getTrueStress() : refYield.getEngineeringStress()) * 1.2
                : 500.0;

        // 3-1. 탄성 기울기 선
        XYSeries elasticSeries = new XYSeries("Elastic Slope");
        if (hStart != null && hEnd != null) {
            double startY = E_MPa * hStart.x + intercept;
            double endY = E_MPa * hEnd.x + intercept;
            elasticSeries.add(hStart.x, startY);
            elasticSeries.add(hEnd.x, endY);
        } else {
            elasticSeries.add(0.0, intercept);
            double endX = (maxY - intercept) / E_MPa;
            if (endX < 0)
                endX = 0;
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

        plot.setDataset(2, slopeDataset);

        XYLineAndShapeRenderer slopeRenderer = new XYLineAndShapeRenderer(true, false);
        slopeRenderer.setSeriesPaint(0, ChartStyler.ELASTIC_SLOPE_COLOR);
        slopeRenderer.setSeriesStroke(0, ChartStyler.SLOPE_STROKE);
        slopeRenderer.setSeriesPaint(1, ChartStyler.OFFSET_LINE_COLOR);
        slopeRenderer.setSeriesStroke(1, ChartStyler.SLOPE_STROKE);
        plot.setRenderer(2, slopeRenderer);
    }

    private void renderPoints(XYPlot plot, StressStrainPoint uts, StressStrainPoint yield,
            StressStrainPoint upper, StressStrainPoint lower, List<StressStrainPoint> data) {
        XYSeriesCollection pointDataset = new XYSeriesCollection();
        XYLineAndShapeRenderer pointRenderer = new XYLineAndShapeRenderer(false, true);
        pointRenderer.setAutoPopulateSeriesPaint(false);
        pointRenderer.setAutoPopulateSeriesShape(false);

        int seriesIndex = 0;

        if (showUTS && uts != null) {
            XYSeries s = new XYSeries("UTS");
            double x = isTrueStressMode ? uts.getTrueStrain() : uts.getEngineeringStrain();
            double y = isTrueStressMode ? uts.getTrueStress() : uts.getEngineeringStress();
            s.add(x, y);
            pointDataset.addSeries(s);

            pointRenderer.setSeriesPaint(seriesIndex, ChartStyler.UTS_POINT_COLOR);
            pointRenderer.setSeriesShape(seriesIndex, new java.awt.geom.Rectangle2D.Double(-4, -4, 8, 8));
            addAnnotation("UTS", x, y, -Math.PI / 4, ChartStyler.UTS_POINT_COLOR, plot);
            seriesIndex++;
        }

        if (showYieldPoint && yield != null) {
            XYSeries s = new XYSeries("Yield");
            double x = isTrueStressMode ? yield.getTrueStrain() : yield.getEngineeringStrain();
            double y = isTrueStressMode ? yield.getTrueStress() : yield.getEngineeringStress();
            s.add(x, y);
            pointDataset.addSeries(s);

            pointRenderer.setSeriesPaint(seriesIndex, ChartStyler.YIELD_POINT_COLOR);
            pointRenderer.setSeriesShape(seriesIndex, new java.awt.geom.Rectangle2D.Double(-4, -4, 8, 8));
            addAnnotation("Yield", x, y, Math.PI / 2, ChartStyler.YIELD_POINT_COLOR, plot);
            seriesIndex++;
        }

        if (showYieldPoint && (upper != null || lower != null)) {
            if (upper != null) {
                XYSeries s = new XYSeries("UYP");
                double x = isTrueStressMode ? upper.getTrueStrain() : upper.getEngineeringStrain();
                double y = isTrueStressMode ? upper.getTrueStress() : upper.getEngineeringStress();
                s.add(x, y);
                pointDataset.addSeries(s);

                pointRenderer.setSeriesPaint(seriesIndex, ChartStyler.UPPER_YIELD_COLOR);
                pointRenderer.setSeriesShape(seriesIndex, new java.awt.geom.Rectangle2D.Double(-4, -4, 8, 8));
                addAnnotation("UYP", x, y, Math.PI / 4, ChartStyler.UPPER_YIELD_COLOR, plot);
                seriesIndex++;
            }
            if (lower != null) {
                XYSeries s = new XYSeries("LYP");
                double x = isTrueStressMode ? lower.getTrueStrain() : lower.getEngineeringStrain();
                double y = isTrueStressMode ? lower.getTrueStress() : lower.getEngineeringStress();
                s.add(x, y);
                pointDataset.addSeries(s);

                pointRenderer.setSeriesPaint(seriesIndex, ChartStyler.UPPER_YIELD_COLOR);
                pointRenderer.setSeriesShape(seriesIndex, new java.awt.geom.Rectangle2D.Double(-4, -4, 8, 8));
                addAnnotation("LYP", x, y, -Math.PI / 2, ChartStyler.UPPER_YIELD_COLOR, plot);
                seriesIndex++;
            }
        }

        if (showFracturePoint && data != null && !data.isEmpty()) {
            StressStrainPoint fracture = data.get(data.size() - 1);
            XYSeries s = new XYSeries("Fracture");
            double x = isTrueStressMode ? fracture.getTrueStrain() : fracture.getEngineeringStrain();
            double y = isTrueStressMode ? fracture.getTrueStress() : fracture.getEngineeringStress();
            s.add(x, y);
            pointDataset.addSeries(s);

            // Series Index가 정확한지 확인 (uts, yield, upper, lower 순서에 따라 증가됨)
            pointRenderer.setSeriesPaint(seriesIndex, Color.MAGENTA);
            pointRenderer.setSeriesShape(seriesIndex, new java.awt.geom.Rectangle2D.Double(-4, -4, 8, 8));
            addAnnotation("Fracture", x, y, -Math.PI / 4, Color.MAGENTA, plot);
            seriesIndex++;

            // 연신율 표시 (수직 점선)
            org.jfree.chart.annotations.XYLineAnnotation elongationLine = new org.jfree.chart.annotations.XYLineAnnotation(
                    x, 0, x, y,
                    new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] { 5.0f },
                            0.0f),
                    Color.DARK_GRAY);
            plot.addAnnotation(elongationLine);

            // 연신율 텍스트
            XYPointerAnnotation elAnn = new XYPointerAnnotation(
                    String.format("Elongation (%.1f%%)", x * 100), x, y / 2, 0);
            elAnn.setTipRadius(0);
            elAnn.setBaseRadius(20);
            elAnn.setFont(new Font("SansSerif", Font.PLAIN, 11));
            elAnn.setPaint(Color.DARK_GRAY);
            elAnn.setArrowPaint(new Color(0, 0, 0, 0)); // 화살표 숨김
            plot.addAnnotation(elAnn);
        }

        if (pointDataset.getSeriesCount() > 0) {
            plot.setDataset(1, pointDataset);
            plot.setRenderer(1, pointRenderer);
        }
    }

    private void renderRegions(XYPlot plot, List<StressStrainPoint> data, StressStrainPoint refYield) {
        if (refYield == null || data == null || data.isEmpty())
            return;

        double yieldStrain = isTrueStressMode ? refYield.getTrueStrain() : refYield.getEngineeringStrain();
        double maxStrain = isTrueStressMode ? data.get(data.size() - 1).getTrueStrain()
                : data.get(data.size() - 1).getEngineeringStrain();

        if (showElasticRegion) {
            IntervalMarker m = new IntervalMarker(0.0, yieldStrain);
            m.setPaint(new Color(0, 0, 255, 20));
            m.setLabel("Elastic Region");
            m.setLabelAnchor(RectangleAnchor.TOP_LEFT);
            m.setLabelTextAnchor(TextAnchor.TOP_LEFT);
            plot.addDomainMarker(m, org.jfree.chart.ui.Layer.BACKGROUND);
        }

        if (showPlasticRegion) {
            IntervalMarker m = new IntervalMarker(yieldStrain, maxStrain);
            m.setPaint(new Color(255, 0, 0, 20));
            m.setLabel("Plastic Region");
            m.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
            m.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
            plot.addDomainMarker(m, org.jfree.chart.ui.Layer.BACKGROUND);
        }
    }

    private void addAnnotation(String label, double x, double y, double angle, Color color, XYPlot plot) {
        XYPointerAnnotation annotation = new XYPointerAnnotation(
                String.format("%s (%.1f)", label, y), x, y, angle);
        annotation.setTipRadius(10.0);
        annotation.setBaseRadius(35.0);
        annotation.setFont(new Font("SansSerif", Font.BOLD, 11));
        annotation.setPaint(color);
        annotation.setArrowPaint(color);
        plot.addAnnotation(annotation);
    }

    // Dataset 생성 헬퍼 (Main 데이터셋용)
    public XYSeriesCollection createMainDataset(List<StressStrainPoint> data) {
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
}
