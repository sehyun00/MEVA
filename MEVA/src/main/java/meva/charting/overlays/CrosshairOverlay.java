// src/main/java/meva/charting/overlays/CrosshairOverlay.java

package meva.charting.overlays;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * [인터랙티브 십자선 오버레이]
 * - 마우스 커서 위치를 따라다니는 십자선(Crosshair)과 좌표값을 표시함
 * - X, Y축 라벨에 검은 배경의 텍스트 박스로 현재 좌표를 실시간 표시
 * - 가시성(Visibility) 및 마우스 위치 업데이트 기능 제공
 * 
 * @author MEVA 개발팀
 */
public class CrosshairOverlay extends AbstractChartOverlay {

    private final Stroke DASHED_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, 
            BasicStroke.JOIN_MITER, 10.0f, new float[]{4.0f, 4.0f}, 0.0f);
    private final Color CROSSHAIR_COLOR = new Color(100, 100, 100, 180);
    private final Color LABEL_BG_COLOR = Color.BLACK;
    private final Color LABEL_TEXT_COLOR = Color.WHITE;
    private final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 11);

    // 현재 마우스 위치 (Java2D 좌표)
    private Point2D mousePoint;

    /**
     * [마우스 위치 업데이트]
     * - 마우스 이동 시 호출되어 십자선 위치를 갱신함
     * 
     * @param point 현재 마우스 좌표 (null이면 십자선 숨김)
     */
    public void updateMousePoint(Point2D point) {
        this.mousePoint = point;
        fireOverlayChanged();
    }

    @Override
    public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
        if (!isVisible() || mousePoint == null) return;

        ChartRenderingInfo info = chartPanel.getChartRenderingInfo();
        if (info == null || info.getPlotInfo() == null) return;
        
        Rectangle2D dataArea = info.getPlotInfo().getDataArea();
        if (!dataArea.contains(mousePoint)) return;

        double x = mousePoint.getX();
        double y = mousePoint.getY();

        // 기존 그래픽 설정 백업
        Stroke originalStroke = g2.getStroke();
        Color originalColor = g2.getColor();
        Font originalFont = g2.getFont();
        g2.setFont(LABEL_FONT);

        // 1. 십자선 그리기
        g2.setStroke(DASHED_STROKE);
        g2.setColor(CROSSHAIR_COLOR);
        g2.drawLine((int)dataArea.getMinX(), (int)y, (int)dataArea.getMaxX(), (int)y);
        g2.drawLine((int)x, (int)dataArea.getMinY(), (int)x, (int)dataArea.getMaxY());

        // 2. 좌표값 라벨 표시
        if (chartPanel.getChart() != null) {
            XYPlot plot = chartPanel.getChart().getXYPlot();
            ValueAxis domainAxis = plot.getDomainAxis();
            ValueAxis rangeAxis = plot.getRangeAxis();
            
            // 화면 좌표 -> 데이터 값 변환
            double chartX = domainAxis.java2DToValue(x, dataArea, plot.getDomainAxisEdge());
            double chartY = rangeAxis.java2DToValue(y, dataArea, plot.getRangeAxisEdge());

            String xText = String.format("%.4f", chartX);
            String yText = String.format("%.1f", chartY);

            FontMetrics fm = g2.getFontMetrics();
            int padding = 4;
            int textHeight = fm.getHeight();
            
            // X축 라벨 그리기
            int labelX_W = fm.stringWidth(xText) + (padding * 2);
            int labelX_H = textHeight + (padding * 2);
            int labelX_X = (int)x - (labelX_W / 2);
            int labelX_Y = (int)dataArea.getMaxY(); // X축 아래에 표시

            g2.setColor(LABEL_BG_COLOR);
            g2.fillRect(labelX_X, labelX_Y, labelX_W, labelX_H);
            g2.setColor(LABEL_TEXT_COLOR);
            g2.drawString(xText, labelX_X + padding, labelX_Y + padding + fm.getAscent());

            // Y축 라벨 그리기
            int labelY_W = fm.stringWidth(yText) + (padding * 2);
            int labelY_H = textHeight + (padding * 2);
            int labelY_X = (int)dataArea.getMinX() - labelY_W; // Y축 왼쪽에 표시
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

