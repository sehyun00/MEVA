package meva.test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import meva.validation.*;
import meva.database.MaterialDatabase;

/**
 * 검증 기능 JUnit 테스트
 * - 단위 테스트: 입력 검증, 결과 검증
 * - 통합 테스트: 표준 DB와 측정값 비교
 */
public class ValidationTestsJUnit {

    static InputValidator inputValidator;
    static ResultValidator resultValidator;
    static MaterialDatabase db;
    static Map<String, Double> standard;

    @BeforeAll
    static void setup() {
        inputValidator = new InputValidator();
        resultValidator = new ResultValidator();

        db = new MaterialDatabase();
        db.loadStandardMaterials(); // 알루미늄·강철 표준값 로드
        standard = db.getMaterialProperties("Aluminum");
    }

    // 단위 테스트: 입력값 검증
    @Test
    void testInputValidator() {
        assertTrue(inputValidator.isNotEmpty("123"));
        assertFalse(inputValidator.isNotEmpty(" "));
        assertTrue(inputValidator.isNumeric("12.5"));
        assertFalse(inputValidator.isNumeric("abc"));
        assertTrue(inputValidator.isWithinRange(5.0, 0.0, 10.0));
        assertFalse(inputValidator.isWithinRange(15.0, 0.0, 10.0));
    }

    // 단위 테스트: 결과값 검증 - 허용 범위 내
    @Test
    void testMeasuredValuesWithinTolerance() {
        Map<String, Double> measured = new HashMap<>();
        measured.put("youngsModulus", 69000.0);
        measured.put("yieldStrength", 260.0);
        measured.put("tensileStrength", 320.0);
        measured.put("density", 2680.0);

        assertTrue(resultValidator.validateTestResult(measured, standard));
    }

    // 단위 테스트: 결과값 검증 - 허용 범위 벗어남
    @Test
    void testMeasuredValuesOutsideTolerance() {
        Map<String, Double> measured = new HashMap<>();
        measured.put("youngsModulus", 50000.0); // 너무 낮음
        measured.put("yieldStrength", 260.0);
        measured.put("tensileStrength", 320.0);
        measured.put("density", 2680.0);

        assertFalse(resultValidator.validateTestResult(measured, standard));
    }

    // 통합 테스트: DB와 연결된 전체 흐름 검증
    @Test
    void testIntegrationWithDatabase() {
        Map<String, Double> measured = new HashMap<>();
        measured.put("youngsModulus", 70000.0);
        measured.put("yieldStrength", 276.0);
        measured.put("tensileStrength", 310.0);
        measured.put("density", 2700.0);

        // DB 표준값과 비교
        assertTrue(resultValidator.validateTestResult(measured, standard));
    }
}