// src/main/java/meva/charting/overlays/AbstractChartOverlay.java

package meva.charting.overlays;

import org.jfree.chart.panel.AbstractOverlay;
import org.jfree.chart.panel.Overlay;

/**
 * [차트 오버레이 추상 클래스]
 * - JFreeChart의 AbstractOverlay를 상속받아 기본 기능 제공
 * - 오버레이의 가시성(Visibility) 제어 기능 추가
 * - 모든 사용자 정의 오버레이는 이 클래스를 상속받아야 함
 * 
 * @author MEVA 개발팀
 */
public abstract class AbstractChartOverlay extends AbstractOverlay implements Overlay {
    
    // 오버레이 표시 여부 (기본값: true)
    private boolean visible = true;

    /**
     * [오버레이 표시 여부 설정]
     * - 가시성 상태를 변경하고 차트 패널에 갱신 요청
     * 
     * @param visible true: 표시, false: 숨김
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
        fireOverlayChanged(); // 변경 사실을 ChartPanel에 알림
    }

    /**
     * [현재 표시 상태 확인]
     * 
     * @return visible 상태 값
     */
    public boolean isVisible() {
        return visible;
    }
}

