// src/main/java/meva/gui/GraphPanel.java

package meva.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * 응력-변형률 곡선 그래프를 표시하는 패널
 * GUI 설계 문서에 따른 완전한 구현
 * 
 * @author MEVA 개발팀
 * @version 1.0
 */
public class GraphPanel extends JPanel {
    
    // 하위 패널들
    private JPanel chartPanel;
    private JPanel chartControlPanel;
    
    // 차트 제어 버튼들
    private JButton zoomInButton;
    private JButton zoomOutButton;
    private JButton resetZoomButton;
    private JButton exportChartButton;
    
    // 이벤트 리스너들
    private ActionListener zoomInListener;
    private ActionListener zoomOutListener;
    private ActionListener resetZoomListener;
    private ActionListener exportChartListener;
    
    /**
     * GraphPanel 생성자
     */
    public GraphPanel() {
        initializeComponents();
        setupLayout();
    }
    
    /**
     * 모든 컴포넌트 초기화
     */
    private void initializeComponents() {
        // 차트 패널 초기화 (JFreeChart 통합 예정)
        chartPanel = createChartPanel();
        
        // 차트 제어 패널 초기화
        chartControlPanel = createChartControlPanel();
    }
    
    /**
     * 레이아웃 설정
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("응력-변형률 곡선"));
        
        // 차트 패널 추가 (중앙)
        add(chartPanel, BorderLayout.CENTER);
        
        // 차트 제어 패널 추가 (하단)
        add(chartControlPanel, BorderLayout.SOUTH);
    }
    
    /**
     * 차트 패널 생성
     * 현재는 플레이스홀더로 구현, 추후 JFreeChart 통합 예정
     */
    private JPanel createChartPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createLineBorder(new Color(224, 224, 224)));
        
        // 플레이스홀더 레이블 (추후 JFreeChart로 교체)
        JLabel placeholderLabel = new JLabel("Chart Area (JFreeChart will be integrated here)", 
            SwingConstants.CENTER);
        placeholderLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
        placeholderLabel.setForeground(new Color(117, 117, 117));
        panel.add(placeholderLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 차트 제어 패널 생성
     */
    private JPanel createChartControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Zoom In 버튼
        zoomInButton = new JButton("Zoom In");
        zoomInButton.addActionListener(e -> {
            if (zoomInListener != null) zoomInListener.actionPerformed(e);
        });
        panel.add(zoomInButton);
        
        // Zoom Out 버튼
        zoomOutButton = new JButton("Zoom Out");
        zoomOutButton.addActionListener(e -> {
            if (zoomOutListener != null) zoomOutListener.actionPerformed(e);
        });
        panel.add(zoomOutButton);
        
        // Reset Zoom 버튼
        resetZoomButton = new JButton("Reset Zoom");
        resetZoomButton.addActionListener(e -> {
            if (resetZoomListener != null) resetZoomListener.actionPerformed(e);
        });
        panel.add(resetZoomButton);
        
        // Export Chart 버튼
        exportChartButton = new JButton("Export Chart");
        exportChartButton.addActionListener(e -> {
            if (exportChartListener != null) exportChartListener.actionPerformed(e);
        });
        panel.add(exportChartButton);
        
        return panel;
    }
    
    /**
     * 그래프 업데이트 메서드
     * 추후 JFreeChart 데이터 업데이트 로직 구현 예정
     */
    public void updateGraph() {
        // TODO: 그래프 업데이트 로직
        // JFreeChart 데이터셋 업데이트 및 차트 리페인트
        repaint();
    }
    
    /**
     * 차트 패널 반환 (JFreeChart 통합 시 사용)
     */
    public JPanel getChartPanel() {
        return chartPanel;
    }
    
    // ========== 이벤트 리스너 설정 메서드들 ==========
    
    public void setZoomInListener(ActionListener listener) {
        this.zoomInListener = listener;
    }
    
    public void setZoomOutListener(ActionListener listener) {
        this.zoomOutListener = listener;
    }
    
    public void setResetZoomListener(ActionListener listener) {
        this.resetZoomListener = listener;
    }
    
    public void setExportChartListener(ActionListener listener) {
        this.exportChartListener = listener;
    }
}