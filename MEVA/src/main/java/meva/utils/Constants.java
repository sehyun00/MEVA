package meva.utils;

/**
 * MEVA 애플리케이션에서 사용하는 상수 정의
 * 
 * @author 김세현
 */
public class Constants {
    
    // 애플리케이션 정보
    public static final String APP_NAME = "MEVA";
    public static final String APP_VERSION = "1.0";
    public static final String APP_DESCRIPTION = "Materials Engineering Visualization and Analysis";
    
    // 물리 상수
    public static final double DEFAULT_POISSON_RATIO = 0.3;
    public static final double MIN_YOUNGS_MODULUS = 1.0; // GPa
    public static final double MAX_YOUNGS_MODULUS = 1000.0; // GPa
    
    // UI 관련 상수
    public static final int DEFAULT_WINDOW_WIDTH = 1200;
    public static final int DEFAULT_WINDOW_HEIGHT = 800;
    public static final int GRAPH_UPDATE_DELAY_MS = 100;
    
    // 파일 관련 상수
    public static final String[] SUPPORTED_IMAGE_FORMATS = {"PNG", "JPG", "SVG"};
    public static final String[] SUPPORTED_DATA_FORMATS = {"CSV", "XLSX"};
    public static final int MAX_DATA_POINTS = 10000;
    
    // 계산 관련 상수
    public static final double YIELD_OFFSET = 0.002; // 0.2% offset for yield strength
    public static final double CALCULATION_TOLERANCE = 1e-6;
    
    private Constants() {
        // 유틸리티 클래스이므로 인스턴스 생성 방지
    }
}