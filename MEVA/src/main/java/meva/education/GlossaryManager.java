package meva.education;

import java.util.HashMap;
import java.util.Map;

/**
 * 재료 역학 용어 및 정의를 관리하는 매니저 클래스
 * 툴팁 및 교육용 팝업에 사용되는 텍스트를 제공합니다.
 */
public class GlossaryManager {
        private static final Map<String, String> glossary = new HashMap<>();

        static {
                // 주요 물성 정의 (HTML 포맷 사용하여 툴팁 가독성 향상)
                // 주요 물성 정의 (HTML 포맷 사용하여 툴팁 가독성 향상)
                glossary.put("극한 인장 강도 (UTS)",
                                "<html><b>극한 인장 강도 (Ultimate Tensile Strength)</b><br>" +
                                                "재료가 파단되기 전까지 견딜 수 있는 최대 응력입니다.<br>" +
                                                "이 지점을 지나면 단면이 급격히 줄어드는 네킹(Necking)이 시작됩니다.</html>");

                glossary.put("영률 (E)",
                                "<html><b>영률 (Young's Modulus, E)</b><br>" +
                                                "재료의 강성(Stiffness)을 나타내는 척도입니다.<br>" +
                                                "값이 클수록 변형하기 어려운 단단한 재료임을 의미합니다.<br>" +
                                                "그래프의 초기 직선 구간 기울기에 해당합니다.</html>");

                glossary.put("항복 강도 (0.2% Offset)",
                                "<html><b>항복 강도 (Yield Strength, 0.2% Offset)</b><br>" +
                                                "재료가 영구 변형(소성 변형)을 시작하는 기준점입니다.<br>" +
                                                "항복점이 불분명한 재료에서 0.2%의 영구 변형이 남는 지점을 기준으로 합니다.</html>");

                glossary.put("항복 강도",
                                "<html><b>항복 강도 (Yield Strength)</b><br>" +
                                                "재료가 탄성 거동을 멈추고 소성 변형을 시작하는 응력입니다.</html>");

                glossary.put("연신율",
                                "<html><b>연신율 (Elongation)</b><br>" +
                                                "재료가 파단될 때까지 늘어난 비율입니다.<br>" +
                                                "재료의 연성(Ductility)을 나타내는 대표적인 지표입니다.<br>" +
                                                "공식: ε = (L_final - L_initial) / L_initial * 100%</html>");

                glossary.put("균일 연신율 (Uniform Elongation)",
                                "<html><b>균일 연신율 (Uniform Elongation)</b><br>" +
                                                "최대 하중(UTS)에 도달했을 때의 변형률입니다.<br>" +
                                                "네킹(Necking)이 시작되기 전까지 시편 전체가 균일하게 늘어나는 한계를 의미합니다.<br>" +
                                                "성형성(Formability) 평가에 중요한 지표입니다.</html>");

                glossary.put("가공경화지수 (n)",
                                "<html><b>가공경화지수 (Strain Hardening Exponent, n)</b><br>" +
                                                "재료가 가공(변형)될 때 얼마나 단단해지는지를 나타내는 지수입니다.<br>" +
                                                "0.1 ~ 0.5 사이의 값을 가지며, 값이 클수록 국부 변형을 지연시켜<br>" +
                                                "성형성이 우수함을 의미합니다. (식: σ = Kεⁿ)</html>");

                glossary.put("강도 계수 (K)",
                                "<html><b>강도 계수 (Strength Coefficient, K)</b><br>" +
                                                "재료의 진응력-진변형률 관계식(σ = Kεⁿ)에서 도출되는 계수입니다.<br>" +
                                                "진변형률이 1.0일 때의 가상의 진응력 값을 의미합니다.</html>");

                glossary.put("스프링백 (Springback)",
                                "<html><b>스프링백 (Springback)</b><br>" +
                                                "하중을 제거했을 때 재료가 탄성적으로 원래 모양으로 돌아가려는 현상입니다.<br>" +
                                                "정밀한 치수의 제품을 만들 때 반드시 고려해야 하는 회복량입니다.</html>");

                glossary.put("단면 감소율",
                                "<html><b>단면 감소율 (Reduction of Area)</b><br>" +
                                                "파단 후 시편의 단면적이 줄어든 비율입니다.<br>" +
                                                "연신율과 함께 재료의 연성을 평가하는 데 사용됩니다.</html>");

                glossary.put("변형률 에너지 밀도 (Toughness)",
                                "<html><b>인성 (Toughness)</b><br>" +
                                                "재료가 파괴될 때까지 흡수할 수 있는 총 에너지입니다.<br>" +
                                                "그래프 전체 면적에 해당하며, 값이 클수록 충격에 강합니다.</html>");

                glossary.put("레질리언스 계수 (Resilience)",
                                "<html><b>레질리언스 (Resilience)</b><br>" +
                                                "탄성 변형 구간에서 재료가 흡수할 수 있는 에너지입니다.<br>" +
                                                "스프링처럼 에너지를 저장했다가 다시 방출할 수 있는 능력을 의미합니다.</html>");

                glossary.put("탄성 한계",
                                "<html><b>탄성 한계 (Elastic Limit)</b><br>" +
                                                "하중을 제거했을 때 영구 변형 없이 원래대로 돌아오는 최대 응력입니다.</html>");

                glossary.put("비례 한계",
                                "<html><b>비례 한계 (Proportional Limit)</b><br>" +
                                                "응력과 변형률이 정비례(Hooke's Law)하는 구간의 끝 지점입니다.</html>");

                glossary.put("파괴 응력",
                                "<html><b>파괴 응력 (Fracture Stress)</b><br>" +
                                                "시편이 완전히 끊어지는 순간의 응력입니다.<br>" +
                                                "진응력 기준으로는 보통 UTS보다 높게 나타납니다.</html>");

                glossary.put("파괴 변형률",
                                "<html><b>파괴 변형률 (Fracture Strain)</b><br>" +
                                                "시편이 끊어지는 순간의 변형률입니다.</html>");
        }

        public static String getDefinition(String term) {
                // 정확히 일치하는 키 찾기
                if (glossary.containsKey(term)) {
                        return glossary.get(term);
                }

                // 부분 일치 검색 (예: "항복 강도 (0.2% Offset)" -> "항복 강도" 검색)
                for (String key : glossary.keySet()) {
                        if (term.startsWith(key) || term.contains(key)) {
                                return glossary.get(key);
                        }
                }

                return null;
        }
}
