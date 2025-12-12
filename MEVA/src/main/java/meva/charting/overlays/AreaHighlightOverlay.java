// src/main/java/meva/charting/overlays/AreaHighlightOverlay.java

package meva.charting.overlays;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.AbstractOverlay;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.XYPlot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * 응력-변형률 곡선의 특정 영역(탄성/소성, 탄성에너지, 인성)을 색칠하여 시각화하는 오버레이 클래스.
 * - Elastic Region: 초록색 계열 (복원 가능 구간)
 * - Plastic Region: 주황색 계열 (영구 변형 구간)
 * - Resilience: Hooke's Law 근사 삼각형 (Elastic Region 위에 덧그림)
 * 
 * @author 김종현
 * @version 1.3 - Color scheme update & Region labels
 */
public class AreaHighlightOverlay extends AbstractOverlay implements Overlay {

    private final XYPlot plot;

    // 표시 여부 플래그
    private boolean showResilienceArea = true;
    private boolean showToughnessArea = true;

    // Resilience 모드 (true: 삼각형 근사, false: 실제 곡선 적분)
    private boolean useResilienceTriangle = true;

    // Resilience 데이터
    private Double epsilonY; // 항복 변형률
    private Double sigmaY; // 항복 강도
    private Double startX = 0.0; // 탄성 시작점 (X절편)

    // Resilience 곡선 데이터 (Integral 모드용)
    private List<Double> resEpsilons;
    private List<Double> resSigmas;

    // Toughness 데이터
    private List<Double> epsilons; // 전체 변형률
    private List<Double> sigmas; // 전체 응력
    private Double epsilonFracture; // 파단 변형률
    private Double sigmaFracture; // 파단 응력

    // 색상 상수 (가시성 개선)
    // 탄성: 초록색 (안전)
    private static final Color COLOR_ELASTIC_FILL = new Color(0, 200, 0, 30);
    private static final Color COLOR_ELASTIC_STROKE = new Color(0, 150, 0, 100);

    // 소성: 주황색 (경고)
    private static final Color COLOR_PLASTIC_FILL = new Color(255, 140, 0, 30);
    private static final Color COLOR_PLASTIC_STROKE = new Color(200, 100, 0, 100);

    // 탄성 에너지 삼각형 (강조): 진한 초록 또는 파랑 (여기선 탄성 영역과 어울리는 진한 초록 사용)
    private static final Color COLOR_RESILIENCE_TRIANGLE = new Color(0, 100, 0, 40);
    private static final Color COLOR_RESILIENCE_BORDER = new Color(0, 80, 0, 180);

    /**
     * 생성자
     * 
     * @param plot 오버레이를 적용할 XYPlot
     */
    public AreaHighlightOverlay(XYPlot plot) {
        this.plot = plot;
    }

    // --- Data Setters ---

    public void setResilienceData(Double epsilonY, Double sigmaY,
            List<Double> resEpsilons, List<Double> resSigmas, Double startX) {
        this.epsilonY = epsilonY;
        this.sigmaY = sigmaY;
        this.resEpsilons = resEpsilons;
        this.resSigmas = resSigmas;
        this.startX = (startX != null) ? startX : 0.0;
    }

    public void setToughnessCurve(List<Double> epsilons, List<Double> sigmas,
            Double epsilonFracture, Double sigmaFracture) {
        this.epsilons = epsilons;
        this.sigmas = sigmas;
        this.epsilonFracture = epsilonFracture;
        this.sigmaFracture = sigmaFracture;
    }

    // --- Option Setters ---

    public void setShowResilienceArea(boolean show) {
        this.showResilienceArea = show;
    }

    public void setShowToughnessArea(boolean show) {
        this.showToughnessArea = show;
    }

    public void setResilienceMode(boolean useTriangle) {
        this.useResilienceTriangle = useTriangle;
    }

    // --- Rendering Logic ---

    @Override
    public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
        Rectangle2D dataArea = chartPanel.getScreenDataArea();
        if (dataArea == null || plot == null)
            return;

        ValueAxis domainAxis = plot.getDomainAxis(); // X축
        ValueAxis rangeAxis = plot.getRangeAxis(); // Y축

        if (domainAxis == null || rangeAxis == null)
            return;

        // 클리핑 적용
        Shape originalClip = g2.getClip();
        g2.setClip(dataArea);

        try {
            // 1. 인성 (Toughness) -> 탄성/소성 영역으로 분할 표시
            if (showToughnessArea && epsilons != null && sigmas != null &&
                    epsilonFracture != null && sigmaFracture != null) {

                if (epsilonY != null) {
                    // (A) 탄성 영역 (Elastic Region) - 초록
                    paintPartialArea(g2, dataArea, domainAxis, rangeAxis, 0.0, epsilonY,
                            COLOR_ELASTIC_FILL, null);

                    // (B) 소성 영역 (Plastic Region) - 주황
                    paintPartialArea(g2, dataArea, domainAxis, rangeAxis, epsilonY, epsilonFracture,
                            COLOR_PLASTIC_FILL, null);

                    // (C) 텍스트 라벨 추가 (하단 우측 정렬)
                    drawRegionLabel(g2, dataArea, domainAxis, rangeAxis,
                            epsilonY, 0.0, "Elastic Region");

                    drawRegionLabel(g2, dataArea, domainAxis, rangeAxis,
                            epsilonFracture, 0.0, "Plastic Region");

                } else {
                    // 항복점이 없으면 전체 소성으로 간주 (또는 회색)
                    paintPolygonArea(g2, dataArea, domainAxis, rangeAxis,
                            epsilons, sigmas, epsilonFracture, sigmaFracture,
                            COLOR_PLASTIC_FILL, COLOR_PLASTIC_STROKE);
                }
            }

            // 2. 탄성 에너지 (Resilience) - 삼각형 강조
            if (showResilienceArea && epsilonY != null && sigmaY != null) {
                if (useResilienceTriangle) {
                    paintResilienceTriangle(g2, dataArea, domainAxis, rangeAxis);
                } else {
                    // 곡선 적분 모드
                    if (resEpsilons != null && resSigmas != null) {
                        paintPolygonArea(g2, dataArea, domainAxis, rangeAxis,
                                resEpsilons, resSigmas, epsilonY, sigmaY,
                                COLOR_RESILIENCE_TRIANGLE, COLOR_RESILIENCE_BORDER);
                    }
                }
            }
        } finally {
            g2.setClip(originalClip);
        }
    }

    /**
     * 탄성 에너지 영역 (삼각형 근사) 렌더링
     */
    private void paintResilienceTriangle(Graphics2D g2, Rectangle2D dataArea,
            ValueAxis domainAxis, ValueAxis rangeAxis) {
        double x0 = domainAxis.valueToJava2D(startX, dataArea, plot.getDomainAxisEdge());
        double y0 = rangeAxis.valueToJava2D(0.0, dataArea, plot.getRangeAxisEdge());

        double xY = domainAxis.valueToJava2D(epsilonY, dataArea, plot.getDomainAxisEdge());
        double yY = rangeAxis.valueToJava2D(sigmaY, dataArea, plot.getRangeAxisEdge());

        Polygon poly = new Polygon();
        poly.addPoint((int) x0, (int) y0);
        poly.addPoint((int) xY, (int) yY);
        poly.addPoint((int) xY, (int) y0);

        g2.setPaint(COLOR_RESILIENCE_TRIANGLE);
        g2.fill(poly);

        g2.setStroke(new BasicStroke(1.5f));
        g2.setPaint(COLOR_RESILIENCE_BORDER);
        g2.draw(poly);

        g2.setStroke(new BasicStroke(2.0f));
        g2.draw(new Line2D.Double(x0, y0, xY, y0));
    }

    /**
     * 영역 텍스트 라벨 그리기 (하단 우측 정렬 + 배경 박스)
     */
    private void drawRegionLabel(Graphics2D g2, Rectangle2D dataArea,
            ValueAxis domainAxis, ValueAxis rangeAxis,
            double xEnd, double yBase, String text) {

        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();

        // 좌표 변환: 영역의 끝(xEnd)에서 약간 왼쪽, 바닥(yBase)에서 약간 위쪽
        double x = domainAxis.valueToJava2D(xEnd, dataArea, plot.getDomainAxisEdge());
        double y = rangeAxis.valueToJava2D(0.0, dataArea, plot.getRangeAxisEdge()); // 바닥 기준

        // 여백 설정
        int paddingX = 8;
        int paddingY = 5;

        float drawX = (float) (x - textWidth - paddingX);
        float drawY = (float) (y - paddingY - fm.getDescent());

        // 배경 박스 그리기 (어두운 반투명)
        g2.setColor(new Color(30, 30, 30, 180));
        g2.fillRoundRect((int) drawX - 4, (int) drawY - textHeight + 4,
                textWidth + 8, textHeight + 2, 6, 6);

        // 텍스트 그리기 (흰색)
        g2.setColor(Color.WHITE);
        g2.drawString(text, drawX, drawY);
    }

    /**
     * 특정 구간(xStart ~ xEnd)만 잘라서 영역을 채우는 메서드
     */
    private void paintPartialArea(Graphics2D g2, Rectangle2D dataArea,
            ValueAxis domainAxis, ValueAxis rangeAxis,
            double xStart, double xEnd, Color fillColor, Color strokeColor) {
        if (epsilons == null || sigmas == null)
            return;

        Polygon poly = new Polygon();

        double x0 = domainAxis.valueToJava2D(xStart, dataArea, plot.getDomainAxisEdge());
        double y0 = rangeAxis.valueToJava2D(0.0, dataArea, plot.getRangeAxisEdge());
        poly.addPoint((int) x0, (int) y0);

        int n = Math.min(epsilons.size(), sigmas.size());
        boolean started = false;

        for (int i = 0; i < n; i++) {
            double eps = epsilons.get(i);
            double sig = sigmas.get(i);

            if (eps < xStart)
                continue;
            if (eps > xEnd)
                break;

            if (!started)
                started = true;

            double x = domainAxis.valueToJava2D(eps, dataArea, plot.getDomainAxisEdge());
            double y = rangeAxis.valueToJava2D(sig, dataArea, plot.getRangeAxisEdge());
            poly.addPoint((int) x, (int) y);
        }

        double xLast = domainAxis.valueToJava2D(xEnd, dataArea, plot.getDomainAxisEdge());
        poly.addPoint((int) xLast, (int) y0);

        if (fillColor != null) {
            g2.setPaint(fillColor);
            g2.fill(poly);
        }
        if (strokeColor != null) {
            g2.setStroke(new BasicStroke(1.0f));
            g2.setPaint(strokeColor);
            g2.draw(poly);
        }
    }

    /**
     * 전체 영역 그리기 (Fallback용)
     */
    private void paintPolygonArea(Graphics2D g2, Rectangle2D dataArea,
            ValueAxis domainAxis, ValueAxis rangeAxis,
            List<Double> xData, List<Double> yData,
            Double xLimit, Double yLimit,
            Color fillColor, Color strokeColor) {
        // [Modified] startX를 사용하여 시작점 설정 (기존에는 0.0 고정이었음)
        double startXVal = (this.startX != null) ? this.startX : 0.0;

        double x0 = domainAxis.valueToJava2D(startXVal, dataArea, plot.getDomainAxisEdge());
        double y0 = rangeAxis.valueToJava2D(0.0, dataArea, plot.getRangeAxisEdge());

        Polygon poly = new Polygon();
        poly.addPoint((int) x0, (int) y0);

        int n = Math.min(xData.size(), yData.size());
        for (int i = 0; i < n; i++) {
            double xVal = xData.get(i);
            double yVal = yData.get(i);

            // 데이터가 startX보다 작은 경우 무시 (혹은 포함? 여기선 시작점 이후 데이터만 그림)
            if (xVal < startXVal)
                continue;
            if (xVal > xLimit)
                break;

            double x = domainAxis.valueToJava2D(xVal, dataArea, plot.getDomainAxisEdge());
            double y = rangeAxis.valueToJava2D(yVal, dataArea, plot.getRangeAxisEdge());
            poly.addPoint((int) x, (int) y);
        }

        // 마지막 항복점까지 닫기
        double xEnd = domainAxis.valueToJava2D(xLimit, dataArea, plot.getDomainAxisEdge());
        double yEnd = rangeAxis.valueToJava2D(yLimit, dataArea, plot.getRangeAxisEdge());
        poly.addPoint((int) xEnd, (int) yEnd);
        poly.addPoint((int) xEnd, (int) y0); // 바닥으로 내리기

        g2.setPaint(fillColor);
        g2.fill(poly);

        g2.setStroke(new BasicStroke(1.0f));
        g2.setPaint(strokeColor);
        g2.draw(poly);

        // 바닥 선 (startX ~ xLimit)
        g2.setStroke(new BasicStroke(2.0f));
        g2.draw(new Line2D.Double(x0, y0, xEnd, y0));
    }
}
