package meva;

import meva.gui.SplashScreen;
import meva.gui.MainFrame;
import javax.swing.SwingUtilities;

/**
 * MEVA (Materials Engineering Visualization and Analysis) 메인 애플리케이션
 * 재료공학 인장시험 데이터 시각화 및 분석 프로그램
 * 
 * @author 5조 - 김세현, 김종현, 박성빈, 이태윤
 * @version 1.0
 */
public class Main {
    public static void main(String[] args) {
        // Look and Feel 설정
        try {
            javax.swing.UIManager.setLookAndFeel(
                    javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 스플래시 스크린 표시 후 메인 프레임 시작
        SwingUtilities.invokeLater(() -> {
            SplashScreen splash = new SplashScreen();
            splash.showSplash(() -> {
                // 스플래시 종료 후 메인 프레임 표시
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
            });
        });
    }
}