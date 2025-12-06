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
        glossary.put("최대 응력 (σmax)",
                "<html><b>최대 응력 (Maximum Stress)</b><br>" +
                        "재료가 견딜 수 있는 최대 하중을 초기 단면적으로 나눈 값입니다.<br>" +
                        "보통 극한 인장 강도(UTS)와 동일합니다.</html>");

        glossary.put("극한 인장 강도 (UTS)",
                "<html><b>극한 인장 강도 (Ultimate Tensile Strength)</b><br>" +
                        "재료가 파단되기 전까지 견딜 수 있는 최대 응력입니다.<br>" +
                        "이 지점을 지나면 네킹(Necking)이 시작되어 응력이 감소합니다.</html>");

        glossary.put("영률 (E)",
                "<html><b>영률 (Young's Modulus)</b><br>" +
                        "재료의 강성(Stiffness)을 나타내는 척도입니다.<br>" +
                        "탄성 구간에서 응력과 변형률의 비례 상수(기울기)입니다.<br>" +
                        "값이 클수록 변형하기 어려운 단단한 재료입니다.</html>");

        glossary.put("항복 강도 (0.2% Offset)",
                "<html><b>항복 강도 (Yield Strength)</b><br>" +
                        "재료가 영구 변형(소성 변형)을 시작하는 응력 지점입니다.<br>" +
                        "명확한 항복점이 없는 경우 0.2% 오프셋 방법을 사용하여 결정합니다.</html>");

        glossary.put("항복 강도",
                "<html><b>항복 강도 (Yield Strength)</b><br>" +
                        "재료가 영구 변형(소성 변형)을 시작하는 응력 지점입니다.</html>");

        glossary.put("연신율",
                "<html><b>연신율 (Elongation)</b><br>" +
                        "파단 후 시편의 길이가 늘어난 비율입니다.<br>" +
                        "재료의 연성(Ductility)을 나타내는 지표입니다.<br>" +
                        "공식: ε = (L_final - L_initial) / L_initial * 100%</html>");

        glossary.put("단면 감소율",
                "<html><b>단면 감소율 (Reduction of Area)</b><br>" +
                        "파단 후 시편의 단면적이 줄어든 비율입니다.<br>" +
                        "연신율과 함께 재료의 연성을 평가하는 데 사용됩니다.</html>");

        glossary.put("변형률 에너지 밀도 (Toughness)",
                "<html><b>인성 (Toughness)</b><br>" +
                        "재료가 파괴될 때까지 흡수할 수 있는 총 에너지의 양입니다.<br>" +
                        "응력-변형률 곡선의 전체 면적으로 계산됩니다.<br>" +
                        "인성이 클수록 충격에 강한 재료입니다.</html>");

        glossary.put("레질리언스 계수 (Resilience)",
                "<html><b>레질리언스 (Resilience)</b><br>" +
                        "탄성 변형 구간에서 재료가 흡수할 수 있는 에너지의 양입니다.<br>" +
                        "항복점까지의 곡선 아래 면적으로 계산됩니다.<br>" +
                        "스프링과 같이 탄성 에너지를 저장하는 부품 설계에 중요합니다.</html>");

        glossary.put("탄성 한계",
                "<html><b>탄성 한계 (Elastic Limit)</b><br>" +
                        "하중을 제거했을 때 원래 길이로 완전히 돌아오는 최대 응력입니다.<br>" +
                        "이 지점을 넘어가면 영구 변형이 남습니다.</html>");

        glossary.put("비례 한계",
                "<html><b>비례 한계 (Proportional Limit)</b><br>" +
                        "응력과 변형률이 정비례(Hooke's Law)하는 마지막 지점입니다.<br>" +
                        "보통 탄성 한계보다 약간 낮거나 같습니다.</html>");

        glossary.put("네킹 시작 변형률",
                "<html><b>네킹 시작점 (Necking Point)</b><br>" +
                        "단면적이 국부적으로 급격히 줄어드는 네킹 현상이 시작되는 변형률입니다.<br>" +
                        "보통 UTS 지점의 변형률과 일치합니다.</html>");

        glossary.put("파괴 응력",
                "<html><b>파괴 응력 (Fracture Stress)</b><br>" +
                        "시편이 완전히 끊어지는 순간의 응력입니다.</html>");

        glossary.put("파괴 변형률",
                "<html><b>파괴 변형률 (Fracture Strain)</b><br>" +
                        "시편이 끊어지는 순간의 변형률입니다.</html>");

        glossary.put("최대 응력 시 변형률 (εmax)",
                "<html><b>최대 응력 시 변형률</b><br>" +
                        "최대 하중(UTS)에 도달했을 때의 변형률입니다.<br>" +
                        "균일 연신율(Uniform Elongation)이라고도 합니다.</html>");
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
