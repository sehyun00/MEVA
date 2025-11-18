package meva.validation;

import static org.junit.jupiter.api.Assertions.*;

import meva.utils.DataValidator;
import meva.validation.impl.DefaultDataValidator; 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class ValidationTest {

    private DataValidator validator;

    @BeforeEach
    void setup() {
        validator = new DefaultDataValidator(); // 테스트 전 validator 초기화
    }

    @Test
    @DisplayName("재료 속성 검증 - 정상 입력")
    void testValidMaterialProperties() {
        Map<String, Double> props = new HashMap<>();
        props.put("YoungsModulus", 210.0);
        props.put("YieldStrength", 300.0);
        props.put("TensileStrength", 500.0);

        assertTrue(validator.validateMaterialProperties("Steel", props));
    }

    @Test
    @DisplayName("Young's Modulus 범위 검증")
    void testYoungsModulus() {
        assertTrue(validator.validateYoungsModulus(200));
        assertFalse(validator.validateYoungsModulus(-1));
    }

    @Test
    @DisplayName("데이터 포인트 개수 검증")
    void testDataPointCount() {
        assertTrue(validator.validateDataPointCount(50));
        assertFalse(validator.validateDataPointCount(0));
    }

    @Test
    @DisplayName("이미지 파일 형식 검증")
    void testImageFormat() {
        assertTrue(validator.validateImageFormat("graph.png"));
        assertFalse(validator.validateImageFormat("unknown.bmp"));
    }

    @Test
    @DisplayName("데이터 파일 형식 검증")
    void testDataFormat() {
        assertTrue(validator.validateDataFormat("data.csv"));
        assertFalse(validator.validateDataFormat("data.txt"));
    }

    @Test
    @DisplayName("윈도우 크기 검증")
    void testWindowSize() {
        assertTrue(validator.validateWindowSize(800, 600));
        assertFalse(validator.validateWindowSize(-1, 100));
    }

    @Test
    @DisplayName("정밀도 검증")
    void testPrecision() {
        assertTrue(validator.validateCalculationPrecision(3));
        assertFalse(validator.validateCalculationPrecision(-5));
    }
}
