// src/main/java/meva/charting/style/ChartStyler.java

package meva.charting.style;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import java.awt.BasicStroke;
import java.awt.Color;

/**
 * [차트 스타일 정의 클래스]
 * - 차트의 색상, 선 두께, 폰트 등 시각적 스타일 관리
 * - 일관된 디자인 적용을 위한 상수 및 설정 메서드 제공
 * 
 * @author MEVA 개발팀
 */
public class ChartStyler {

    // --- 색상 상수 ---
    public static final Color CHART_BACKGROUND = Color.WHITE;                // 차트 배경색
    public static final Color GRID_LINE_COLOR = new Color(220, 220, 220);    // 그리드 라인 색상
    
    // 시리즈 색상
    public static final Color TRUE_STRESS_COLOR = new Color(33, 150, 243);   // 진응력 (파랑)
    public static final Color ENG_STRESS_COLOR = new Color(244, 67, 54);     // 공칭응력 (빨강)
    
    // 오버레이 및 마커 색상
    public static final Color ELASTIC_SLOPE_COLOR = new Color(0, 0, 255, 180); // 탄성 기울기 선
    public static final Color OFFSET_LINE_COLOR = new Color(0, 150, 0, 180);   // 오프셋 라인
    public static final Color UTS_POINT_COLOR = Color.RED;                     // UTS 마커
    public static final Color YIELD_POINT_COLOR = new Color(0, 150, 0);        // 항복점 마커
    public static final Color UPPER_YIELD_COLOR = new Color(255, 140, 0);      // 상항복점 마커
    
    // --- 스트로크(선 스타일) ---
    public static final BasicStroke SOLID_STROKE = new BasicStroke(2.0f);    // 실선 (진응력용)
    public static final BasicStroke DASHED_STROKE = new BasicStroke(         // 점선 (공칭응력용)
        2.0f, 
        BasicStroke.CAP_ROUND, 
        BasicStroke.JOIN_ROUND, 
        1.0f, 
        new float[]{5.0f, 5.0f}, 
        0.0f
    );
    public static final BasicStroke SLOPE_STROKE = new BasicStroke(          // 보조선 (탄성계수용)
        1.5f, 
        BasicStroke.CAP_BUTT, 
        BasicStroke.JOIN_MITER, 
        10.0f, 
        new float[]{10.0f, 5.0f}, 
        0.0f
    );

    /**
     * [차트 기본 스타일 적용]
     * - 배경색, 그리드, 시리즈 렌더러 설정 적용
     * - True Stress(실선/파랑)와 Engineering Stress(점선/빨강) 스타일 지정
     * 
     * @param chart 스타일을 적용할 JFreeChart 객체
     */
    public static void applyStyle(JFreeChart chart) {
        if (chart == null) return;

        // 1. 배경 및 플롯 설정
        chart.setBackgroundPaint(CHART_BACKGROUND);
        
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(CHART_BACKGROUND);
        plot.setDomainGridlinePaint(GRID_LINE_COLOR);
        plot.setRangeGridlinePaint(GRID_LINE_COLOR);
        
        // 2. 렌더러 설정 (데이터 시리즈)
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        
        // Series 0: True Stress-Strain (실선)
        renderer.setSeriesPaint(0, TRUE_STRESS_COLOR);
        renderer.setSeriesStroke(0, SOLID_STROKE);
        renderer.setSeriesShapesVisible(0, false);
        
        // Series 1: Engineering Stress-Strain (점선)
        renderer.setSeriesPaint(1, ENG_STRESS_COLOR);
        renderer.setSeriesStroke(1, DASHED_STROKE);
        renderer.setSeriesShapesVisible(1, false);
        
        plot.setRenderer(renderer);
    }
}

