package meva.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * MEVA 프로그램 정보를 표시하는 About 다이얼로그
 * 
 * @author MEVA 개발팀
 * @version 1.0
 */
public class AboutDialog extends JDialog {

    private static final String PROGRAM_NAME = "MEVA";
    private static final String FULL_NAME = "Materials Engineering Visualization and Analysis";
    private static final String VERSION = "1.0.0";
    private static final String COPYRIGHT = "© 2025 MEVA Development Team";
    private static final String DESCRIPTION = "재료공학 학부생을 위한 인장시험 데이터 시각화 분석 프로그램입니다.\n" +
            "응력-변형률 곡선을 자동 생성하고 핵심 물성값을 계산합니다.";
    private static final String PROJECT_INFO = "본 프로젝트는 선문대학교 JAVA응용프로젝트 과정의 일환으로 진행했습니다.";
    private static final String GITHUB_URL = "https://github.com/sehyun00/MEVA";

    public AboutDialog(Frame parent) {
        super(parent, "프로그램 정보", true);
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setResizable(true);

        // 콘텐츠 패널 (스크롤 가능)
        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(Color.WHITE);

        // 아이콘 패널 (상단)
        JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        iconPanel.setBackground(Color.WHITE);

        // 아이콘 로드 시도
        JLabel iconLabel = new JLabel();
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        try {
            // 여러 경로에서 아이콘 찾기
            String[] possiblePaths = {
                    "resources/meva_icon.png",
                    "MEVA/resources/meva_icon.png",
                    "../resources/meva_icon.png"
            };

            Image icon = null;
            for (String path : possiblePaths) {
                File iconFile = new File(path);
                if (iconFile.exists()) {
                    icon = ImageIO.read(iconFile);
                    break;
                }
            }

            if (icon != null) {
                // 아이콘 크기 조정 (100x100)
                Image scaledIcon = icon.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                iconLabel.setIcon(new ImageIcon(scaledIcon));
            } else {
                // 아이콘을 찾을 수 없는 경우 텍스트 대체
                iconLabel.setText("🔬");
                iconLabel.setFont(new Font("Dialog", Font.PLAIN, 48));
            }
        } catch (IOException e) {
            iconLabel.setText("🔬");
            iconLabel.setFont(new Font("Dialog", Font.PLAIN, 48));
        }
        iconPanel.add(iconLabel);

        // 정보 패널 (중앙)
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);

        // 프로그램명
        JLabel nameLabel = new JLabel(PROGRAM_NAME);
        nameLabel.setFont(new Font("Dialog", Font.BOLD, 28));
        nameLabel.setForeground(new Color(41, 128, 185));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 전체 이름
        JLabel fullNameLabel = new JLabel(FULL_NAME);
        fullNameLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
        fullNameLabel.setForeground(Color.GRAY);
        fullNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 버전
        JLabel versionLabel = new JLabel("Version " + VERSION);
        versionLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 설명
        JTextArea descArea = new JTextArea(DESCRIPTION);
        descArea.setFont(new Font("Dialog", Font.PLAIN, 12));
        descArea.setEditable(false);
        descArea.setBackground(Color.WHITE);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        descArea.setMaximumSize(new Dimension(350, 60));

        // 프로젝트 정보
        JTextArea projectArea = new JTextArea(PROJECT_INFO);
        projectArea.setFont(new Font("Dialog", Font.PLAIN, 11));
        projectArea.setForeground(new Color(80, 80, 80));
        projectArea.setEditable(false);
        projectArea.setBackground(Color.WHITE);
        projectArea.setLineWrap(true);
        projectArea.setWrapStyleWord(true);
        projectArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        projectArea.setMaximumSize(new Dimension(350, 40));

        // GitHub 링크
        JLabel githubLabel = new JLabel("<html><a href='" + GITHUB_URL + "'>GitHub: " + GITHUB_URL + "</a></html>");
        githubLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
        githubLabel.setForeground(new Color(41, 128, 185));
        githubLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        githubLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        githubLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(GITHUB_URL));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AboutDialog.this,
                            "브라우저를 열 수 없습니다.\nURL: " + GITHUB_URL,
                            "오류", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // 저작권
        JLabel copyrightLabel = new JLabel(COPYRIGHT);
        copyrightLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
        copyrightLabel.setForeground(Color.GRAY);
        copyrightLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 컴포넌트 추가
        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(2));
        infoPanel.add(fullNameLabel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(versionLabel);
        infoPanel.add(Box.createVerticalStrut(15));
        infoPanel.add(descArea);
        infoPanel.add(Box.createVerticalStrut(15));
        infoPanel.add(projectArea);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(githubLabel);
        infoPanel.add(Box.createVerticalStrut(15));
        infoPanel.add(copyrightLabel);

        contentPanel.add(iconPanel, BorderLayout.NORTH);
        contentPanel.add(infoPanel, BorderLayout.CENTER);

        // 스크롤 패널
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // 확인 버튼 패널 (하단)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.WHITE);

        JButton okButton = new JButton("확인");
        okButton.setPreferredSize(new Dimension(80, 30));
        okButton.addActionListener(e -> dispose());
        buttonPanel.add(okButton);

        // 메인 레이아웃에 추가
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // 다이얼로그 크기 설정
        setSize(420, 400);
        setMinimumSize(new Dimension(350, 300));
    }

    /**
     * About 다이얼로그를 표시하는 정적 메서드
     */
    public static void showDialog(Frame parent) {
        AboutDialog dialog = new AboutDialog(parent);
        dialog.setVisible(true);
    }
}
