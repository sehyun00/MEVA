package meva.gui;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;

import javax.swing.SwingUtilities;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 * JFreeChart의 기본 줌 동작을 커스터마이징한 패널
 * - 좌클릭 드래그: 줌 박스 생성 (기본 동작)
 * - 휠클릭(가운데 버튼) 드래그: 화면 이동 (Panning)
 * - 우클릭: 동작 없음 (또는 추후 팝업)
 */
public class CustomChartPanel extends ChartPanel {

    private Point lastPanPoint; // 패닝 시 마지막 마우스 좌표 저장
    
    // 패닝 활성화 여부 플래그
    private boolean domainPannable = false;
    private boolean rangePannable = false;

    public CustomChartPanel(JFreeChart chart) {
        super(chart);
    }
    
    public void setDomainPannable(boolean pannable) {
        this.domainPannable = pannable;
        // 차트의 Plot에도 패닝 설정 전파
        if (getChart() != null && getChart().getPlot() instanceof XYPlot) {
            ((XYPlot) getChart().getPlot()).setDomainPannable(pannable);
        }
    }
    
    public void setRangePannable(boolean pannable) {
        this.rangePannable = pannable;
        // 차트의 Plot에도 패닝 설정 전파
        if (getChart() != null && getChart().getPlot() instanceof XYPlot) {
            ((XYPlot) getChart().getPlot()).setRangePannable(pannable);
        }
    }
    
    public boolean isDomainPannable() {
        return domainPannable;
    }
    
    public boolean isRangePannable() {
        return rangePannable;
    }

    /**
     * 마우스 버튼이 눌렸을 때 호출
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            super.mousePressed(e); // 줌 시작
        } else if (SwingUtilities.isMiddleMouseButton(e)) {
            // 휠 클릭: 패닝 시작
            if (isDomainPannable() || isRangePannable()) {
                lastPanPoint = e.getPoint();
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }
        }
    }

    /**
     * 마우스 드래그 시 호출
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            super.mouseDragged(e); // 줌 박스 그리기
        } else if (SwingUtilities.isMiddleMouseButton(e) && lastPanPoint != null) {
            // 휠 클릭 드래그: 패닝 수행
            double dx = e.getX() - lastPanPoint.getX();
            double dy = e.getY() - lastPanPoint.getY();
            
            double w = getWidth();
            double h = getHeight();
            
            if (w > 0 && h > 0) {
                Plot plot = getChart().getPlot();
                if (plot instanceof XYPlot) {
                    XYPlot xyPlot = (XYPlot) plot;
                    
                    if (isDomainPannable()) {
                        xyPlot.panDomainAxes(-dx / w, getChartRenderingInfo().getPlotInfo(), e.getPoint());
                    }
                    if (isRangePannable()) {
                        xyPlot.panRangeAxes(dy / h, getChartRenderingInfo().getPlotInfo(), e.getPoint());
                    }
                }
            }
            lastPanPoint = e.getPoint();
        }
    }

    /**
     * 마우스 버튼이 놓였을 때 호출
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            super.mouseReleased(e); // 줌 종료
        } else if (SwingUtilities.isMiddleMouseButton(e)) {
            // 패닝 종료
            lastPanPoint = null;
            setCursor(Cursor.getDefaultCursor());
        }
    }
}
