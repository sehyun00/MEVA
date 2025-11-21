package meva.validation;

/**
 * 사용자 입력값 검증
 * - 공백 여부
 * - 숫자 여부
 * - 값 범위
 */
public class InputValidator {

    /** 문자열이 null이나 공백인지 확인 */
    public boolean isNotEmpty(String input) {
        return input != null && !input.trim().isEmpty();
    }

    /** 문자열이 숫자로 변환 가능한지 확인 */
    public boolean isNumeric(String input) {
        if (!isNotEmpty(input)) return false;

        try {
            Double.parseDouble(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** 값이 특정 범위 안에 있는지 확인 */
    public boolean isWithinRange(double value, double min, double max) {
        return value >= min && value <= max;
    }
}