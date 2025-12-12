// src/main/java/meva/charting/overlays/SlopeOverlay.java

package meva.charting.overlays;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * [탄성계수 조절 핸들 오버레이]
 * - 그래프 위에 탄성 구간 조절용 네모 핸들을 그림
 * - 드래그 상태에 따라 색상 변경 (기본: 파랑 -> 드래그 시: 빨강)
 * - 사용자 입력(InputHandler)과 연동되어 위치 갱신
 * 
 * @author MEVA 개발팀
 */
public class SlopeOverlay extends AbstractChartOverlay {

    private static final int HANDLE_SIZE = 10;
    private static final Color HANDLE_COLOR_NORMAL = new Color(33, 150, 243);
    private static final Color HANDLE_COLOR_DRAGGING = Color.RED;
    private static final Color HANDLE_BORDER_COLOR = Color.WHITE;

    // 핸들 좌표 (데이터 단위: Strain, Stress)
    private Point2D.Double handleStart;
    private Point2D.Double handleEnd;
    
    // 드래그 상태 플래그
    private boolean isDraggingStart = false;
    private boolean isDraggingEnd = false;

    /**
     * [핸들 위치 설정]
     * - 새로운 시작점/끝점을 설정하고 오버레이 갱신 요청
     * 
     * @param start 시작점 (Strain, Stress)
     * @param end 끝점 (Strain, Stress)
     */
    public void setHandles(Point2D.Double start, Point2D.Double end) {
        this.handleStart = start;
        this.handleEnd = end;
        fireOverlayChanged(); // 다시 그리기 요청
    }

    public Point2D.Double getHandleStart() {
        return handleStart;
    }

    public Point2D.Double getHandleEnd() {
        return handleEnd;
    }

    /**
     * [드래그 상태 설정]
     * - ChartInputHandler에서 마우스 상태에 따라 호출
     * 
     * @param start 시작점 드래그 여부
     * @param end 끝점 드래그 여부
     */
    public void setDragging(boolean start, boolean end) {
        this.isDraggingStart = start;
        this.isDraggingEnd = end;
        fireOverlayChanged();
    }

    public boolean isDraggingStart() { return isDraggingStart; }
    public boolean isDraggingEnd() { return isDraggingEnd; }

    @Override
    public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
        if (!isVisible() || handleStart == null || handleEnd == null) return;
        
        // 1. 좌표 변환 준비
        Rectangle2D dataArea = chartPanel.getScreenDataArea();
        XYPlot plot = chartPanel.getChart().getXYPlot();
        ValueAxis domainAxis = plot.getDomainAxis();
        ValueAxis rangeAxis = plot.getRangeAxis();
        
        // 2. 차트 값 -> 화면 좌표 변환
        double startX = domainAxis.valueToJava2D(handleStart.x, dataArea, plot.getDomainAxisEdge());
        double startY = rangeAxis.valueToJava2D(handleStart.y, dataArea, plot.getRangeAxisEdge());
        double endX = domainAxis.valueToJava2D(handleEnd.x, dataArea, plot.getDomainAxisEdge());
        double endY = rangeAxis.valueToJava2D(handleEnd.y, dataArea, plot.getRangeAxisEdge());
        
        // 3. 핸들 그리기
        drawHandle(g2, startX, startY, isDraggingStart);
        drawHandle(g2, endX, endY, isDraggingEnd);
    }
    
    private void drawHandle(Graphics2D g2, double x, double y, boolean isDragging) {
        int half = HANDLE_SIZE / 2;
        Rectangle2D rect = new Rectangle2D.Double(x - half, y - half, HANDLE_SIZE, HANDLE_SIZE);
        
        g2.setColor(isDragging ? HANDLE_COLOR_DRAGGING : HANDLE_COLOR_NORMAL);
        g2.fill(rect);
        
        g2.setColor(HANDLE_BORDER_COLOR);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(rect);
    }
}

