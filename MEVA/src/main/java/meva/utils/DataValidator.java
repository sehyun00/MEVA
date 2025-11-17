package meva.utils;

import java.util.Map;

public interface DataValidator {

    /* 재료(Material) 관련 검증 */
    boolean validateMaterialProperties(String materialName, Map<String, Double> properties);
    boolean validateYoungsModulus(double value);
    boolean validateYieldStrength(double value);
    boolean validateTensileStrength(double value);

    /* 데이터 개수 / 실험 데이터 검증 */
    boolean validateDataPointCount(int count);

    /* 파일 형식 검증 */
    boolean validateImageFormat(String fileName);
    boolean validateDataFormat(String fileName);

    /* Config 설정 검증 */
    boolean validateWindowSize(int width, int height);
    boolean validateCalculationPrecision(int precision);

    /* 오류 메시지 */
    String getLastErrorMessage();
}