package meva;

import meva.database.DatabaseManager; // 데이터베이스 관리자 클래스
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
        // 데이터베이스 초기화
        System.out.println("=== 데이터베이스 초기화 시작 ===");
        DatabaseManager.initializeDatabase();
        System.out.println("=== 데이터베이스 초기화 완료 ===\n");

        // Swing 이벤트 루프 시작
        SwingUtilities.invokeLater(() -> {
            try {
                javax.swing.UIManager.setLookAndFeel(
                        javax.swing.UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 메인 프레임 생성 및 표시
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
    }
}