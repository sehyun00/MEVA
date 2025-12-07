// src/main/java/meva/charting/interactions/ChartInputHandler.java

package meva.charting.interactions;

import meva.charting.overlays.CrosshairOverlay;
import meva.charting.overlays.SlopeOverlay;
import meva.models.StressStrainPoint;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * [차트 사용자 입력 처리기]
 * - 마우스 이벤트(이동, 클릭, 드래그)를 중앙에서 처리
 * - 십자선(Crosshair) 업데이트 및 탄성계수 핸들(SlopeHandle) 조작 로직 담당
 * - 데이터 스냅(Snapping) 알고리즘 포함
 * 
 * @author MEVA 개발팀
 */
public class ChartInputHandler extends MouseAdapter {

    // --- 의존성 객체들 ---
    private final ChartPanel chartPanel;
    private final CrosshairOverlay crosshairOverlay;
    private final SlopeOverlay slopeOverlay;
    
    // 데이터 참조 (스냅핑 및 좌표 변환용)
    private List<StressStrainPoint> currentData;
    private boolean isTrueStressMode; // true: True Stress, false: Engineering Stress

    // 콜백 리스너 (핸들 조작 종료 시 알림)
    private InteractionListener interactionListener;

    /**
     * [이벤트 리스너 인터페이스]
     * - 핸들 조작 완료 시 외부(ChartManager/GraphPanel)로 알림을 보냄
     */
    public interface InteractionListener {
        void onHandleReleased(); // 핸들 드래그가 끝났을 때 호출
    }

    /**
     * 생성자
     * 
     * @param chartPanel JFreeChart 패널
     * @param crosshair 십자선 오버레이
     * @param slope 핸들 오버레이
     */
    public ChartInputHandler(ChartPanel chartPanel, CrosshairOverlay crosshair, SlopeOverlay slope) {
        this.chartPanel = chartPanel;
        this.crosshairOverlay = crosshair;
        this.slopeOverlay = slope;
        
        // 리스너 등록
        this.chartPanel.addMouseListener(this);
        this.chartPanel.addMouseMotionListener(this);
    }

    // --- 설정 메서드 ---

    public void setInteractionListener(InteractionListener listener) {
        this.interactionListener = listener;
    }

    public void updateData(List<StressStrainPoint> data, boolean isTrueStressMode) {
        this.currentData = data;
        this.isTrueStressMode = isTrueStressMode;
    }

    // --- 마우스 이벤트 처리 ---

    @Override
    public void mouseMoved(MouseEvent e) {
        Point2D p = e.getPoint();

        // 1. 십자선 업데이트
        if (crosshairOverlay != null && crosshairOverlay.isVisible()) {
            crosshairOverlay.updateMousePoint(p);
        }

        // 2. 핸들 위에 있는지 감지하여 커서 변경
        if (slopeOverlay != null && slopeOverlay.isVisible()) {
            checkHandleHover(p);
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // 마우스가 차트 밖으로 나가면 십자선 숨김
        if (crosshairOverlay != null) {
            crosshairOverlay.updateMousePoint(null);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (slopeOverlay == null || !slopeOverlay.isVisible()) return;

        Point2D p = e.getPoint();
        
        // 핸들 클릭 감지
        Point2D.Double start = slopeOverlay.getHandleStart();
        Point2D.Double end = slopeOverlay.getHandleEnd();

        if (start == null || end == null) return;

        Point2D pStart = valueToScreen(start);
        Point2D pEnd = valueToScreen(end);

        // 감지 범위 (15px)
        if (pStart != null && p.distance(pStart) < 15) {
            slopeOverlay.setDragging(true, false);
            chartPanel.setMouseZoomable(false); // 드래그 중 줌 방지
        } else if (pEnd != null && p.distance(pEnd) < 15) {
            slopeOverlay.setDragging(false, true);
            chartPanel.setMouseZoomable(false);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (slopeOverlay == null) return;

        if (slopeOverlay.isDraggingStart() || slopeOverlay.isDraggingEnd()) {
            // 드래그 종료 상태 설정
            slopeOverlay.setDragging(false, false);
            chartPanel.setMouseZoomable(true); // 줌 기능 복구
            
            // 재계산 요청 알림
            if (interactionListener != null) {
                interactionListener.onHandleReleased();
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (slopeOverlay == null) return;

        boolean draggingStart = slopeOverlay.isDraggingStart();
        boolean draggingEnd = slopeOverlay.isDraggingEnd();

        if (!draggingStart && !draggingEnd) return;
        if (currentData == null || currentData.isEmpty()) return;

        // 1. 마우스 위치를 차트 값(Strain)으로 변환
        double mouseX = e.getX();
        Rectangle2D dataArea = chartPanel.getScreenDataArea();
        XYPlot plot = chartPanel.getChart().getXYPlot();
        ValueAxis domainAxis = plot.getDomainAxis();
        
        double mouseStrain = domainAxis.java2DToValue(mouseX, dataArea, plot.getDomainAxisEdge());

        // 2. 가장 가까운 데이터 포인트 찾기 (Snapping)
        StressStrainPoint closest = findClosestPoint(mouseStrain);
        
        if (closest != null) {
            double snapX = isTrueStressMode ? closest.getTrueStrain() : closest.getEngineeringStrain();
            double snapY = isTrueStressMode ? closest.getTrueStress() : closest.getEngineeringStress();

            Point2D.Double handleStart = slopeOverlay.getHandleStart();
            Point2D.Double handleEnd = slopeOverlay.getHandleEnd();

            // 3. 핸들 위치 업데이트 (제약 조건 적용)
            if (draggingStart) {
                // 끝점보다 뒤로 가지 못하게
                if (snapX < handleEnd.x) {
                    slopeOverlay.setHandles(new Point2D.Double(snapX, snapY), handleEnd);
                }
            } else if (draggingEnd) {
                // 시작점보다 앞으로 가지 못하게
                if (snapX > handleStart.x) {
                    slopeOverlay.setHandles(handleStart, new Point2D.Double(snapX, snapY));
                }
            }
        }
    }

    // --- 내부 유틸리티 메서드 ---

    /**
     * 차트 값(Strain, Stress)을 화면 좌표(Pixel)로 변환
     */
    private Point2D valueToScreen(Point2D.Double valuePoint) {
        if (valuePoint == null) return null;
        
        Rectangle2D dataArea = chartPanel.getScreenDataArea();
        if (dataArea == null) return null; // 차트가 아직 안 그려졌을 때

        XYPlot plot = chartPanel.getChart().getXYPlot();
        ValueAxis domainAxis = plot.getDomainAxis();
        ValueAxis rangeAxis = plot.getRangeAxis();

        double x = domainAxis.valueToJava2D(valuePoint.x, dataArea, plot.getDomainAxisEdge());
        double y = rangeAxis.valueToJava2D(valuePoint.y, dataArea, plot.getRangeAxisEdge());

        return new Point2D.Double(x, y);
    }

    /**
     * 마우스가 핸들 위에 있는지 확인하고 커서 변경
     */
    private void checkHandleHover(Point2D mouseP) {
        Point2D.Double start = slopeOverlay.getHandleStart();
        Point2D.Double end = slopeOverlay.getHandleEnd();

        if (start == null || end == null) return;

        Point2D pStart = valueToScreen(start);
        Point2D pEnd = valueToScreen(end);

        boolean hover = (pStart != null && mouseP.distance(pStart) < 10) || 
                        (pEnd != null && mouseP.distance(pEnd) < 10);

        if (hover) {
            chartPanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        } else {
            chartPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * 주어진 Strain 값에 가장 가까운 데이터 포인트를 검색
     */
    private StressStrainPoint findClosestPoint(double targetStrain) {
        StressStrainPoint closest = null;
        double minDist = Double.MAX_VALUE;

        for (StressStrainPoint p : currentData) {
            double strain = isTrueStressMode ? p.getTrueStrain() : p.getEngineeringStrain();
            double dist = Math.abs(strain - targetStrain);
            if (dist < minDist) {
                minDist = dist;
                closest = p;
            }
        }
        return closest;
    }
}

