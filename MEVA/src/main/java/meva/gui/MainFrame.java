package meva.gui;

import javax.swing.*;
import java.awt.*;

/**
 * MEVA 애플리케이션의 메인 윈도우 프레임
 * 
 * @author 김종현
 */
public class MainFrame extends JFrame {
    private InputPanel inputPanel;
    private GraphPanel graphPanel;
    private ResultPanel resultPanel;
    private MenuBar menuBar;
    
    public MainFrame() {
        initializeComponents();
        setupLayout();
        setupFrame();
    }
    
    private void initializeComponents() {
        // TODO: 컴포넌트 초기화
        inputPanel = new InputPanel();
        graphPanel = new GraphPanel();
        resultPanel = new ResultPanel();
        menuBar = new MenuBar();
    }
    
    private void setupLayout() {
        // TODO: 레이아웃 설정
        setLayout(new BorderLayout());
        
        // 메뉴바 설정
        setJMenuBar(menuBar);
        
        // 패널 배치 (임시)
        add(inputPanel, BorderLayout.WEST);
        add(graphPanel, BorderLayout.CENTER);
        add(resultPanel, BorderLayout.EAST);
    }
    
    private void setupFrame() {
        setTitle("MEVA - Materials Engineering Visualization and Analysis");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
    }
}