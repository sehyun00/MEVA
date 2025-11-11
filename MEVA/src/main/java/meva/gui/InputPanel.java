package meva.gui;

import javax.swing.*;

/**
 * 사용자 입력을 받는 패널
 * 재료 물성값, 시편 치수 등을 입력받음
 * 
 * @author 김종현
 */
public class InputPanel extends JPanel {
    
    public InputPanel() {
        initializeComponents();
        setupLayout();
    }
    
    private void initializeComponents() {
        // TODO: 입력 컴포넌트들 초기화
        // 영률, 항복강도, 인장강도, 시편 치수 입력 필드들
    }
    
    private void setupLayout() {
        // TODO: 레이아웃 설정
        setBorder(BorderFactory.createTitledBorder("재료 물성 입력"));
    }
}