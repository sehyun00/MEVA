package meva.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.imageio.ImageIO;
import java.io.File;

/**
 * MEVA 프로그램 시작 시 표시되는 스플래시 스크린
 * 다크 테마 + 라운드 디자인
 * 
 * @author MEVA 개발팀
 * @version 1.0
 */
public class SplashScreen extends JWindow {

    private static final String PROGRAM_NAME = "MEVA";
    private static final String VERSION = "Version 1.0.0";
    private static final String LOADING_TEXT = "Loading...";

    // 다크 테마 색상
    private static final Color BG_COLOR = new Color(24, 24, 28);
    private static final Color ACCENT_COLOR = new Color(99, 102, 241); // 인디고
    private static final Color TEXT_PRIMARY = new Color(255, 255, 255);
    private static final Color TEXT_SECONDARY = new Color(156, 163, 175);
    private static final Color PROGRESS_BG = new Color(55, 55, 65);
    private static final Color BORDER_COLOR = new Color(50, 50, 60);

    private RoundedProgressBar progressBar;
    private JLabel statusLabel;

    public SplashScreen() {
        initComponents();
    }

    private void initComponents() {
        // 창 투명 설정 (라운드 모서리용)
        setBackground(new Color(0, 0, 0, 0));

        // 메인 패널 (라운드 모서리)
        RoundedPanel mainPanel = new RoundedPanel(20, BG_COLOR);
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 콘텐츠 패널 (중앙)
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(35, 45, 25, 45));

        // 아이콘
        JLabel iconLabel = new JLabel();
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        try {
            // JAR 내부 리소스 로딩 (클래스패스)
            String[] classpathPaths = {
                    "/meva_icon.png",
                    "meva_icon.png"
            };
            Image icon = null;

            // 1. 클래스패스에서 로드 시도
            for (String path : classpathPaths) {
                java.net.URL url = getClass().getResource(path);
                if (url != null) {
                    icon = ImageIO.read(url);
                    break;
                }
            }

            // 2. 파일 시스템에서 로드 시도 (IDE 실행 시)
            if (icon == null) {
                String[] filePaths = {
                        "resources/meva_icon.png",
                        "MEVA/resources/meva_icon.png"
                };
                for (String path : filePaths) {
                    File iconFile = new File(path);
                    if (iconFile.exists()) {
                        icon = ImageIO.read(iconFile);
                        break;
                    }
                }
            }

            if (icon != null) {
                Image scaledIcon = icon.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                iconLabel.setIcon(new ImageIcon(scaledIcon));
            } else {
                throw new Exception("Icon not found");
            }
        } catch (Exception e) {
            iconLabel.setText("🔬");
            iconLabel.setFont(new Font("Dialog", Font.PLAIN, 50));
            iconLabel.setForeground(TEXT_PRIMARY);
        }

        // 프로그램 이름
        JLabel nameLabel = new JLabel(PROGRAM_NAME);
        nameLabel.setFont(new Font("Dialog", Font.BOLD, 38));
        nameLabel.setForeground(TEXT_PRIMARY);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 부제목
        JLabel subtitleLabel = new JLabel("Materials Engineering Visualization and Analysis");
        subtitleLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 버전
        JLabel versionLabel = new JLabel(VERSION);
        versionLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
        versionLabel.setForeground(ACCENT_COLOR);
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 로딩 상태
        statusLabel = new JLabel(LOADING_TEXT);
        statusLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
        statusLabel.setForeground(TEXT_SECONDARY);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 라운드 프로그레스 바
        progressBar = new RoundedProgressBar();
        progressBar.setPreferredSize(new Dimension(260, 6));
        progressBar.setMaximumSize(new Dimension(260, 6));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 컴포넌트 추가
        contentPanel.add(iconLabel);
        contentPanel.add(Box.createVerticalStrut(18));
        contentPanel.add(nameLabel);
        contentPanel.add(Box.createVerticalStrut(4));
        contentPanel.add(subtitleLabel);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(versionLabel);
        contentPanel.add(Box.createVerticalStrut(30));
        contentPanel.add(progressBar);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(statusLabel);

        // 저작권 (하단)
        JLabel copyrightLabel = new JLabel("© 2025 MEVA Development Team");
        copyrightLabel.setFont(new Font("Dialog", Font.PLAIN, 9));
        copyrightLabel.setForeground(new Color(100, 100, 110));
        copyrightLabel.setHorizontalAlignment(SwingConstants.CENTER);
        copyrightLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 15, 0));

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setOpaque(false);
        southPanel.add(copyrightLabel, BorderLayout.CENTER);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // 크기 및 위치 설정
        setSize(380, 400);
        setLocationRelativeTo(null);
    }

    /**
     * 프로그레스 업데이트
     */
    public void setProgress(int value, String status) {
        progressBar.setValue(value);
        statusLabel.setText(status);
    }

    /**
     * 스플래시 스크린 표시 및 로딩 시뮬레이션
     */
    public void showSplash(Runnable onComplete) {
        setVisible(true);

        // 백그라운드에서 로딩 진행
        SwingWorker<Void, int[]> worker = new SwingWorker<Void, int[]>() {
            @Override
            protected Void doInBackground() throws Exception {
                String[] loadingSteps = {
                        "Initializing components...",
                        "Loading database...",
                        "Preparing user interface...",
                        "Starting application..."
                };

                for (int i = 0; i < loadingSteps.length; i++) {
                    int progress = (i + 1) * 25;
                    publish(new int[] { progress, i });
                    Thread.sleep(350);
                }
                return null;
            }

            @Override
            protected void process(java.util.List<int[]> chunks) {
                int[] latest = chunks.get(chunks.size() - 1);
                String[] steps = {
                        "Initializing components...",
                        "Loading database...",
                        "Preparing user interface...",
                        "Starting application..."
                };
                SplashScreen.this.setProgress(latest[0], steps[latest[1]]);
            }

            @Override
            protected void done() {
                dispose();
                if (onComplete != null) {
                    SwingUtilities.invokeLater(onComplete);
                }
            }
        };

        worker.execute();
    }

    /**
     * 라운드 모서리 패널
     */
    private static class RoundedPanel extends JPanel {
        private int radius;
        private Color bgColor;

        public RoundedPanel(int radius, Color bgColor) {
            this.radius = radius;
            this.bgColor = bgColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 배경
            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), radius, radius));

            // 테두리
            g2.setColor(BORDER_COLOR);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, radius, radius));

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * 라운드 프로그레스 바
     */
    private static class RoundedProgressBar extends JComponent {
        private int value = 0;
        private int max = 100;

        public void setValue(int value) {
            this.value = Math.min(value, max);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int arc = height;

            // 배경
            g2.setColor(PROGRESS_BG);
            g2.fill(new RoundRectangle2D.Double(0, 0, width, height, arc, arc));

            // 진행 바
            if (value > 0) {
                int progressWidth = (int) ((value / (double) max) * width);
                if (progressWidth > 0) {
                    // 그라데이션
                    GradientPaint gradient = new GradientPaint(
                            0, 0, ACCENT_COLOR,
                            progressWidth, 0, new Color(139, 92, 246) // 보라색
                    );
                    g2.setPaint(gradient);
                    g2.fill(new RoundRectangle2D.Double(0, 0, progressWidth, height, arc, arc));
                }
            }

            g2.dispose();
        }
    }
}
