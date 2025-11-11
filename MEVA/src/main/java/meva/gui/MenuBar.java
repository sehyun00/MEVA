// src/main/java/meva/gui/MenuBar.java

package meva.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * 애플리케이션 메뉴바
 * GUI 설계 문서에 따른 완전한 구현
 * 
 * @author MEVA 개발팀
 * @version 1.0
 */
public class MenuBar extends JMenuBar {
    
    // 메뉴 항목들
    private JMenu fileMenu;
    private JMenu editMenu;
    private JMenu viewMenu;
    private JMenu toolsMenu;
    private JMenu helpMenu;
    
    // 이벤트 리스너 (MainFrame에서 설정)
    private ActionListener fileNewListener;
    private ActionListener fileOpenListener;
    private ActionListener fileSaveListener;
    private ActionListener fileSaveAsListener;
    private ActionListener fileExportListener;
    private ActionListener fileExitListener;
    private ActionListener editUndoListener;
    private ActionListener editRedoListener;
    private ActionListener editPreferencesListener;
    private ActionListener viewZoomInListener;
    private ActionListener viewZoomOutListener;
    private ActionListener viewResetZoomListener;
    private ActionListener viewToggleGridListener;
    private ActionListener viewToggleLegendListener;
    private ActionListener toolsCalculateListener;
    private ActionListener toolsClearDataListener;
    private ActionListener toolsDataValidatorListener;
    private ActionListener helpUserGuideListener;
    private ActionListener helpAboutListener;
    
    /**
     * MenuBar 생성자
     */
    public MenuBar() {
        initializeMenus();
    }
    
    /**
     * 모든 메뉴 초기화
     */
    private void initializeMenus() {
        setFont(new Font("Dialog", Font.PLAIN, 12));
        
        // File 메뉴 생성
        fileMenu = createFileMenu();
        add(fileMenu);
        
        // Edit 메뉴 생성
        editMenu = createEditMenu();
        add(editMenu);
        
        // View 메뉴 생성
        viewMenu = createViewMenu();
        add(viewMenu);
        
        // Tools 메뉴 생성
        toolsMenu = createToolsMenu();
        add(toolsMenu);
        
        // Help 메뉴 생성
        helpMenu = createHelpMenu();
        add(helpMenu);
    }
    
    /**
     * File 메뉴 생성
     */
    private JMenu createFileMenu() {
        JMenu menu = new JMenu("File");
        
        JMenuItem newItem = createMenuItem("New", "Ctrl+N");
        newItem.addActionListener(e -> {
            if (fileNewListener != null) fileNewListener.actionPerformed(e);
        });
        menu.add(newItem);
        
        JMenuItem openItem = createMenuItem("Open...", "Ctrl+O");
        openItem.addActionListener(e -> {
            if (fileOpenListener != null) fileOpenListener.actionPerformed(e);
        });
        menu.add(openItem);
        
        JMenuItem saveItem = createMenuItem("Save", "Ctrl+S");
        saveItem.addActionListener(e -> {
            if (fileSaveListener != null) fileSaveListener.actionPerformed(e);
        });
        menu.add(saveItem);
        
        JMenuItem saveAsItem = createMenuItem("Save As...", "Ctrl+Shift+S");
        saveAsItem.addActionListener(e -> {
            if (fileSaveAsListener != null) fileSaveAsListener.actionPerformed(e);
        });
        menu.add(saveAsItem);
        
        menu.addSeparator();
        
        JMenuItem exportItem = createMenuItem("Export", null);
        exportItem.addActionListener(e -> {
            if (fileExportListener != null) fileExportListener.actionPerformed(e);
        });
        menu.add(exportItem);
        
        menu.addSeparator();
        
        JMenuItem exitItem = createMenuItem("Exit", "Alt+F4");
        exitItem.addActionListener(e -> {
            if (fileExitListener != null) fileExitListener.actionPerformed(e);
        });
        menu.add(exitItem);
        
        return menu;
    }
    
    /**
     * Edit 메뉴 생성
     */
    private JMenu createEditMenu() {
        JMenu menu = new JMenu("Edit");
        
        JMenuItem undoItem = createMenuItem("Undo", "Ctrl+Z");
        undoItem.addActionListener(e -> {
            if (editUndoListener != null) editUndoListener.actionPerformed(e);
        });
        menu.add(undoItem);
        
        JMenuItem redoItem = createMenuItem("Redo", "Ctrl+Y");
        redoItem.addActionListener(e -> {
            if (editRedoListener != null) editRedoListener.actionPerformed(e);
        });
        menu.add(redoItem);
        
        menu.addSeparator();
        
        JMenuItem preferencesItem = createMenuItem("Preferences", "Ctrl+,");
        preferencesItem.addActionListener(e -> {
            if (editPreferencesListener != null) editPreferencesListener.actionPerformed(e);
        });
        menu.add(preferencesItem);
        
        return menu;
    }
    
    /**
     * View 메뉴 생성
     */
    private JMenu createViewMenu() {
        JMenu menu = new JMenu("View");
        
        JMenuItem zoomInItem = createMenuItem("Zoom In", "Ctrl++");
        zoomInItem.addActionListener(e -> {
            if (viewZoomInListener != null) viewZoomInListener.actionPerformed(e);
        });
        menu.add(zoomInItem);
        
        JMenuItem zoomOutItem = createMenuItem("Zoom Out", "Ctrl+-");
        zoomOutItem.addActionListener(e -> {
            if (viewZoomOutListener != null) viewZoomOutListener.actionPerformed(e);
        });
        menu.add(zoomOutItem);
        
        JMenuItem resetZoomItem = createMenuItem("Reset Zoom", "Ctrl+0");
        resetZoomItem.addActionListener(e -> {
            if (viewResetZoomListener != null) viewResetZoomListener.actionPerformed(e);
        });
        menu.add(resetZoomItem);
        
        menu.addSeparator();
        
        JCheckBoxMenuItem showGridItem = new JCheckBoxMenuItem("Show Grid", true);
        showGridItem.addActionListener(e -> {
            if (viewToggleGridListener != null) viewToggleGridListener.actionPerformed(e);
        });
        menu.add(showGridItem);
        
        JCheckBoxMenuItem showLegendItem = new JCheckBoxMenuItem("Show Legend", true);
        showLegendItem.addActionListener(e -> {
            if (viewToggleLegendListener != null) viewToggleLegendListener.actionPerformed(e);
        });
        menu.add(showLegendItem);
        
        return menu;
    }
    
    /**
     * Tools 메뉴 생성
     */
    private JMenu createToolsMenu() {
        JMenu menu = new JMenu("Tools");
        
        JMenuItem calculateItem = createMenuItem("Calculate", "F5");
        calculateItem.addActionListener(e -> {
            if (toolsCalculateListener != null) toolsCalculateListener.actionPerformed(e);
        });
        menu.add(calculateItem);
        
        JMenuItem clearDataItem = createMenuItem("Clear Data", null);
        clearDataItem.addActionListener(e -> {
            if (toolsClearDataListener != null) toolsClearDataListener.actionPerformed(e);
        });
        menu.add(clearDataItem);
        
        menu.addSeparator();
        
        JMenuItem dataValidatorItem = createMenuItem("Data Validator", null);
        dataValidatorItem.addActionListener(e -> {
            if (toolsDataValidatorListener != null) toolsDataValidatorListener.actionPerformed(e);
        });
        menu.add(dataValidatorItem);
        
        return menu;
    }
    
    /**
     * Help 메뉴 생성
     */
    private JMenu createHelpMenu() {
        JMenu menu = new JMenu("Help");
        
        JMenuItem userGuideItem = createMenuItem("User Guide", "F1");
        userGuideItem.addActionListener(e -> {
            if (helpUserGuideListener != null) helpUserGuideListener.actionPerformed(e);
        });
        menu.add(userGuideItem);
        
        JMenuItem aboutItem = createMenuItem("About", null);
        aboutItem.addActionListener(e -> {
            if (helpAboutListener != null) helpAboutListener.actionPerformed(e);
        });
        menu.add(aboutItem);
        
        return menu;
    }
    
    /**
     * 메뉴 아이템 생성 헬퍼 메서드
     */
    private JMenuItem createMenuItem(String text, String accelerator) {
        JMenuItem menuItem = new JMenuItem(text);
        if (accelerator != null) {
            menuItem.setAccelerator(KeyStroke.getKeyStroke(accelerator));
        }
        return menuItem;
    }
    
    // ========== 이벤트 리스너 설정 메서드들 ==========
    
    public void setFileNewListener(ActionListener listener) {
        this.fileNewListener = listener;
    }
    
    public void setFileOpenListener(ActionListener listener) {
        this.fileOpenListener = listener;
    }
    
    public void setFileSaveListener(ActionListener listener) {
        this.fileSaveListener = listener;
    }
    
    public void setFileSaveAsListener(ActionListener listener) {
        this.fileSaveAsListener = listener;
    }
    
    public void setFileExportListener(ActionListener listener) {
        this.fileExportListener = listener;
    }
    
    public void setFileExitListener(ActionListener listener) {
        this.fileExitListener = listener;
    }
    
    public void setEditUndoListener(ActionListener listener) {
        this.editUndoListener = listener;
    }
    
    public void setEditRedoListener(ActionListener listener) {
        this.editRedoListener = listener;
    }
    
    public void setEditPreferencesListener(ActionListener listener) {
        this.editPreferencesListener = listener;
    }
    
    public void setViewZoomInListener(ActionListener listener) {
        this.viewZoomInListener = listener;
    }
    
    public void setViewZoomOutListener(ActionListener listener) {
        this.viewZoomOutListener = listener;
    }
    
    public void setViewResetZoomListener(ActionListener listener) {
        this.viewResetZoomListener = listener;
    }
    
    public void setViewToggleGridListener(ActionListener listener) {
        this.viewToggleGridListener = listener;
    }
    
    public void setViewToggleLegendListener(ActionListener listener) {
        this.viewToggleLegendListener = listener;
    }
    
    public void setToolsCalculateListener(ActionListener listener) {
        this.toolsCalculateListener = listener;
    }
    
    public void setToolsClearDataListener(ActionListener listener) {
        this.toolsClearDataListener = listener;
    }
    
    public void setToolsDataValidatorListener(ActionListener listener) {
        this.toolsDataValidatorListener = listener;
    }
    
    public void setHelpUserGuideListener(ActionListener listener) {
        this.helpUserGuideListener = listener;
    }
    
    public void setHelpAboutListener(ActionListener listener) {
        this.helpAboutListener = listener;
    }
}