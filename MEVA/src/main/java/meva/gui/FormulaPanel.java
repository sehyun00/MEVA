package meva.gui;

import javax.swing.*;
import java.awt.*;
import meva.models.AnalysisResult;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXConstants;

/**
 * 물성치 계산 공식과 대입 과정을 표시하는 패널
 * - 원본 수식 (Symbol)
 * - 대입된 값 (Substitution)
 * - 기호 범례 (Legend)
 * 
 * @author MEVA 개발팀
 * @version 1.2 (옵션 반영 및 범례 추가)
 */
public class FormulaPanel extends JPanel {

    private JLabel lblSymbolic; // 상단: 원본 수식 (기호)
    private JLabel lblSubstituted; // 중단: 대입된 수식 (숫자)
    private JTextArea txtLegend; // 하단: 기호 범례
    private JPanel contentPanel;

    // 현재 옵션 상태 (외부에서 설정)
    private boolean useTrueStress = false; // True/Engineering 응력 모드
    private boolean useTriangleResilience = true; // Triangle(Linear)/Integral(Actual) 레질리언스 모드
    private boolean useUpperYield = false; // Upper Yield / 0.2% Offset 모드

    public FormulaPanel() {
        initializeComponents();
        setupLayout();
    }

    private void initializeComponents() {
        lblSymbolic = new JLabel("항목을 선택하세요", SwingConstants.CENTER);
        lblSymbolic.setFont(new Font("SansSerif", Font.BOLD, 16));

        lblSubstituted = new JLabel("", SwingConstants.CENTER);
        lblSubstituted.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblSubstituted.setForeground(Color.DARK_GRAY);

        txtLegend = new JTextArea(3, 30);
        txtLegend.setEditable(false);
        txtLegend.setFont(new Font("SansSerif", Font.PLAIN, 11));
        txtLegend.setBackground(new Color(245, 245, 245));
        txtLegend.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("📖 기호 설명"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        txtLegend.setLineWrap(true);
        txtLegend.setWrapStyleWord(true);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("📐 계산 과정 (Calculation Process)"));
        setBackground(Color.WHITE);

        // Scrollable 구현 패널 - 뷰포트 너비에 맞게 자동 조정
        contentPanel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                // 부모 뷰포트가 있으면 그 너비에 맞춤
                Container parent = getParent();
                if (parent instanceof JViewport) {
                    int parentWidth = parent.getWidth();
                    if (parentWidth > 0) {
                        Dimension pref = super.getPreferredSize();
                        return new Dimension(parentWidth - 20, pref.height);
                    }
                }
                return super.getPreferredSize();
            }
        };
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 중앙 정렬을 위한 래퍼 패널들
        JPanel pnlSymbolic = new JPanel(new BorderLayout());
        pnlSymbolic.setBackground(Color.WHITE);
        lblSymbolic.setHorizontalAlignment(SwingConstants.CENTER);
        pnlSymbolic.add(lblSymbolic, BorderLayout.CENTER);

        JPanel pnlSubstituted = new JPanel(new BorderLayout());
        pnlSubstituted.setBackground(Color.WHITE);
        lblSubstituted.setHorizontalAlignment(SwingConstants.CENTER);
        pnlSubstituted.add(lblSubstituted, BorderLayout.CENTER);

        contentPanel.add(pnlSymbolic);
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(new JSeparator(JSeparator.HORIZONTAL));
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(pnlSubstituted);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(txtLegend);

        // ScrollPane에 리사이즈 리스너 추가
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // 뷰포트 크기 변경 시 contentPanel 재배치
        scrollPane.getViewport().addChangeListener(e -> {
            contentPanel.revalidate();
        });

        add(scrollPane, BorderLayout.CENTER);
    }

    // ========== 옵션 설정 메서드 (ResultPanel에서 호출) ==========

    /**
     * 현재 옵션 상태를 일괄 설정
     * 
     * @param trueStress         True 응력 모드 사용 여부
     * @param triangleResilience Triangle(Linear) 레질리언스 모드 사용 여부
     * @param upperYield         Upper Yield Point 사용 여부
     */
    public void setOptions(boolean trueStress, boolean triangleResilience, boolean upperYield) {
        this.useTrueStress = trueStress;
        this.useTriangleResilience = triangleResilience;
        this.useUpperYield = upperYield;
    }

    // ========== 수식 업데이트 (핵심 로직) ==========

    /**
     * 수식 패널 업데이트 (옵션 및 계산 값 반영)
     * 
     * @param propertyName    선택된 물성치 이름
     * @param result          분석 결과 객체
     * @param calculatedValue ResultPanel에서 실제로 계산된 값 (테이블 표시 값과 동일)
     */
    public void updateFormula(String propertyName, AnalysisResult result, double calculatedValue) {
        if (result == null) {
            clearDisplay("분석 결과가 없습니다.");
            return;
        }

        String latexSymbol = "";
        String latexSubst = "";
        String legend = "";

        try {
            switch (propertyName) {
                case "영률 (E)":
                    latexSymbol = "E = \\frac{\\Delta \\sigma}{\\Delta \\epsilon}";
                    latexSubst = String.format("E = %.2f \\text{ GPa}", calculatedValue);
                    legend = "E = 영률 (Young's Modulus)\n" +
                            "Δσ = 탄성 구간 응력 변화량 [MPa]\n" +
                            "Δε = 탄성 구간 변형률 변화량 [-]";
                    break;

                case "연신율":
                    double L0 = result.getInitialLength();
                    double Lf = result.getFinalLength();
                    if (Lf == 0 && result.getFracturePoint() != null) {
                        Lf = L0 * (1 + result.getFracturePoint().getEngineeringStrain());
                    }
                    latexSymbol = "\\text{El} = \\frac{L_f - L_0}{L_0} \\times 100";
                    if (L0 > 0) {
                        latexSubst = String.format("= \\frac{%.2f - %.2f}{%.2f} \\times 100 = %.2f \\%%",
                                Lf, L0, L0, calculatedValue);
                    } else {
                        latexSubst = String.format("= %.2f \\%%", calculatedValue);
                    }
                    legend = "El = 연신율 (Elongation) [%]\n" +
                            "L₀ = 초기 표점 거리 [mm]\n" +
                            "Lf = 파단 후 최종 길이 [mm]";
                    break;

                case "단면 감소율":
                    double A0 = result.getInitialArea();
                    double Af = result.getFinalArea();
                    latexSymbol = "\\text{RA} = \\frac{A_0 - A_f}{A_0} \\times 100";
                    if (A0 > 0 && Af > 0) {
                        latexSubst = String.format("= \\frac{%.2f - %.2f}{%.2f} \\times 100 = %.2f \\%%",
                                A0, Af, A0, calculatedValue);
                    } else {
                        latexSubst = String.format("= %.2f \\%%", calculatedValue);
                    }
                    legend = "RA = 단면 수축율 (Reduction of Area) [%]\n" +
                            "A₀ = 초기 단면적 [mm²]\n" +
                            "Af = 파단 후 단면적 [mm²]";
                    break;

                case "극한 인장 강도 (UTS)":
                    String stressType = useTrueStress ? "\\sigma_t" : "\\sigma_e";
                    String stressDesc = useTrueStress ? "진응력 (True Stress)" : "공칭응력 (Eng. Stress)";
                    latexSymbol = "\\text{UTS} = " + stressType + "_{max}";
                    latexSubst = String.format("= %.2f \\text{ MPa}", calculatedValue);
                    legend = "UTS = 극한 인장 강도 (Ultimate Tensile Strength) [MPa]\n" +
                            "현재 모드: " + stressDesc + "\n" +
                            "σmax = 응력-변형률 곡선의 최대 응력값";
                    break;

                case "항복 강도 (0.2% Offset)":
                    if (useUpperYield) {
                        latexSymbol = "\\sigma_y = \\sigma_{UYP} \\quad \\text{(Upper Yield Point)}";
                        legend = "σ_y = 항복 강도 [MPa]\n" +
                                "σ_UYP = 상부 항복점 (불연속 항복)\n" +
                                "모드: Upper Yield Point";
                    } else {
                        latexSymbol = "\\sigma_y : \\epsilon = \\frac{\\sigma}{E} + 0.002";
                        legend = "σ_y = 항복 강도 [MPa]\n" +
                                "0.2% Offset 직선과 곡선의 교점\n" +
                                "모드: 0.2% Offset Method";
                    }
                    latexSubst = String.format("\\sigma_y = %.2f \\text{ MPa}", calculatedValue);
                    break;

                case "인성 (Toughness)":
                    latexSymbol = "U_T = \\int_{0}^{\\epsilon_f} \\sigma \\, d\\epsilon";
                    latexSubst = String.format("\\approx %.4f \\text{ MJ/m}^3", calculatedValue);
                    legend = "U_T = 인성 (Toughness) [MJ/m³]\n" +
                            "ε_f = 파단 변형률\n" +
                            "곡선 아래 전체 면적 (파단까지)";
                    break;

                case "레질리언스 계수 (Resilience)":
                    if (useTriangleResilience) {
                        // Triangle (Linear) 모드
                        double E_GPa = result.getYoungsModulusEng();
                        if (E_GPa == 0)
                            E_GPa = result.getYoungsModulus();
                        double sigmaY = getYieldStress(result);

                        latexSymbol = "U_r \\approx \\frac{1}{2} \\cdot \\frac{\\sigma_y^2}{E}";
                        latexSubst = String.format("= \\frac{1}{2} \\cdot \\frac{(%.2f)^2}{%.2f \\times 1000} = %.4f",
                                sigmaY, E_GPa, calculatedValue);
                        legend = "U_r = 레질리언스 (탄성 에너지) [MJ/m³]\n" +
                                "모드: Triangle (Linear) 근사\n" +
                                "σ_y = 항복 강도 [MPa], E = 영률 [GPa]";
                    } else {
                        // Integral (Actual) 모드
                        latexSymbol = "U_r = \\int_{0}^{\\epsilon_y} \\sigma \\, d\\epsilon";
                        latexSubst = String.format("\\approx %.4f \\text{ MJ/m}^3", calculatedValue);
                        legend = "U_r = 레질리언스 (탄성 에너지) [MJ/m³]\n" +
                                "모드: Integral (Actual) 적분\n" +
                                "ε_y = 항복 변형률 (적분 상한)";
                    }
                    break;

                case "균일 연신율 (Uniform Elongation)":
                    latexSymbol = "\\epsilon_u = \\epsilon \\text{ at } \\sigma_{max}";
                    latexSubst = String.format("= %.4f", calculatedValue);
                    legend = "ε_u = 균일 연신율 (Uniform Elongation)\n" +
                            "UTS 시점의 변형률 (Necking 시작점)\n" +
                            "이 구간까지는 균일 변형 발생";
                    break;

                case "소성 연신율 (Plastic Elongation)":
                    double E_GPa = result.getYoungsModulusEng();
                    if (E_GPa == 0)
                        E_GPa = result.getYoungsModulus();
                    double ef = result.getFractureStrain();
                    double sf = result.getFractureStress();

                    latexSymbol = "\\epsilon_p = \\epsilon_f - \\frac{\\sigma_f}{E}";
                    latexSubst = String.format("= %.4f - \\frac{%.2f}{%.2f \\times 1000} = %.4f",
                            ef, sf, E_GPa, calculatedValue);
                    legend = "ε_p = 소성 연신율 [mm/mm]\n" +
                            "ε_f = 파단 시 총 변형률\n" +
                            "탄성 회복선(Unloading Line)과 X축 교차점";
                    break;

                case "비례 한계":
                    latexSymbol = "\\sigma_{PL} = \\text{Linear Region Limit}";
                    latexSubst = String.format("= %.2f \\text{ MPa}", calculatedValue);
                    legend = "σ_PL = 비례 한계 [MPa]\n" +
                            "응력-변형률 곡선이 직선에서\n" +
                            "벗어나기 시작하는 응력값";
                    break;

                case "파괴 응력":
                    latexSymbol = "\\sigma_f = \\sigma \\text{ at fracture}";
                    latexSubst = String.format("= %.2f \\text{ MPa}", calculatedValue);
                    legend = "σ_f = 파괴 응력 (Fracture Stress) [MPa]\n" +
                            "시편이 완전히 분리되는 순간의 응력";
                    break;

                case "파괴 변형률":
                    latexSymbol = "\\epsilon_f = \\epsilon \\text{ at fracture}";
                    latexSubst = String.format("= %.4f", calculatedValue);
                    legend = "ε_f = 파괴 변형률 (Fracture Strain) [-]\n" +
                            "시편 파단 시점의 총 변형률";
                    break;

                default:
                    clearDisplay("수식이 정의되지 않은 항목입니다.");
                    return;
            }

            renderFormula(latexSymbol, latexSubst, legend);

        } catch (Exception e) {
            e.printStackTrace();
            clearDisplay("수식 렌더링 오류: " + e.getMessage());
        }
    }

    // 하위 호환용 (옛 시그니처)
    public void updateFormula(String propertyName, AnalysisResult result) {
        updateFormula(propertyName, result, 0.0);
    }

    // ========== 헬퍼 메서드 ==========

    private double getYieldStress(AnalysisResult result) {
        if (useUpperYield && result.getUpperYieldPoint() != null) {
            return useTrueStress ? result.getUpperYieldPoint().getTrueStress()
                    : result.getUpperYieldPoint().getEngineeringStress();
        }
        if (!useTrueStress && result.getOffsetYieldPointEng() != null) {
            return result.getOffsetYieldPointEng().getEngineeringStress();
        }
        if (result.getOffsetYieldPoint() != null) {
            return useTrueStress ? result.getOffsetYieldPoint().getTrueStress()
                    : result.getOffsetYieldPoint().getEngineeringStress();
        }
        return result.getYieldStrength();
    }

    private void renderFormula(String symbol, String subst, String legend) {
        lblSymbolic.setIcon(null);
        lblSubstituted.setIcon(null);
        lblSymbolic.setText("");
        lblSubstituted.setText("");

        if (!symbol.isEmpty()) {
            TeXFormula f = new TeXFormula(symbol);
            lblSymbolic.setIcon(f.createTeXIcon(TeXConstants.STYLE_DISPLAY, 20));
        }
        if (!subst.isEmpty()) {
            TeXFormula f = new TeXFormula(subst);
            lblSubstituted.setIcon(f.createTeXIcon(TeXConstants.STYLE_DISPLAY, 18));
        }
        txtLegend.setText(legend);
    }

    public void clearDisplay(String msg) {
        lblSymbolic.setIcon(null);
        lblSubstituted.setIcon(null);
        lblSymbolic.setText(msg);
        lblSymbolic.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblSubstituted.setText("");
        txtLegend.setText("");
    }
}
