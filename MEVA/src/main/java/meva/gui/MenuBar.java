package meva.gui;

import javax.swing.*;

/**
 * 애플리케이션 메뉴바
 * 
 * @author 김종현
 */
public class MenuBar extends JMenuBar {
    
    public MenuBar() {
        initializeMenus();
    }
    
    private void initializeMenus() {
        // TODO: 메뉴 항목들 초기화
        // 파일, 편집, 보기, 도움말 메뉴
        
        JMenu fileMenu = new JMenu("파일");
        JMenu editMenu = new JMenu("편집");
        JMenu viewMenu = new JMenu("보기");
        JMenu helpMenu = new JMenu("도움말");
        
        add(fileMenu);
        add(editMenu);
        add(viewMenu);
        add(helpMenu);
    }
}