package meva.validation;

import java.util.Map;

/**
 * 실험 결과 검증
 * - 표준 물성값과 비교
 * - 허용 오차 판단
 */
public class ResultValidator {

    /** 측정값이 표준값 ±허용율 범위 내인지 확인 */
    public boolean isWithinMaterialTolerance(
            double measuredValue,
            double standardValue,
            double toleranceRate
    ) {
        double min = standardValue * (1 - toleranceRate);
        double max = standardValue * (1 + toleranceRate);

        return measuredValue >= min && measuredValue <= max;
    }

    /** 전체 측정값을 표준값과 비교하여 검증 */
    public boolean validateTestResult(Map<String, Double> measured, Map<String, Double> standard) {
        if (measured == null || standard == null) return false;

        for (String key : measured.keySet()) {
            if (!standard.containsKey(key)) return false;

            double m = measured.get(key);
            double s = standard.get(key);

            // 허용 오차 20%
            if (!isWithinMaterialTolerance(m, s, 0.2)) {
                return false;
            }
        }
        return true;
    }
}