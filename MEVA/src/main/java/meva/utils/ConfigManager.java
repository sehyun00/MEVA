package meva.utils;

import java.io.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MEVA 애플리케이션의 설정을 관리하는 클래스
 * Properties 파일을 사용하여 설정값을 저장하고 로드
 * 
 * @author 김세현
 */
public class ConfigManager {
    private static final Logger LOGGER = Logger.getLogger(ConfigManager.class.getName());
    private static final String CONFIG_FILE = "meva.properties";
    private static ConfigManager instance;
    private Properties properties;
    
    // 설정 키 상수
    public static final String KEY_LAST_DIRECTORY = "last.directory";
    public static final String KEY_WINDOW_WIDTH = "window.width";
    public static final String KEY_WINDOW_HEIGHT = "window.height";
    public static final String KEY_WINDOW_X = "window.x";
    public static final String KEY_WINDOW_Y = "window.y";
    public static final String KEY_THEME = "ui.theme";
    public static final String KEY_LANGUAGE = "ui.language";
    public static final String KEY_AUTO_SAVE = "data.auto_save";
    public static final String KEY_RECENT_FILES = "recent.files";
    public static final String KEY_MAX_RECENT_FILES = "recent.max_count";
    public static final String KEY_GRAPH_ANTI_ALIASING = "graph.anti_aliasing";
    public static final String KEY_GRAPH_GRID = "graph.show_grid";
    public static final String KEY_DEFAULT_MATERIAL = "default.material";
    public static final String KEY_CALCULATION_PRECISION = "calculation.precision";
    
    /**
     * Private 생성자 (Singleton 패턴)
     */
    private ConfigManager() {
        properties = new Properties();
        loadConfig();
    }
    
    /**
     * ConfigManager 인스턴스 반환 (Singleton)
     * 
     * @return ConfigManager 인스턴스
     */
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    /**
     * 설정 파일 로드
     */
    private void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        
        if (!configFile.exists()) {
            LOGGER.info("설정 파일이 없습니다. 기본 설정을 사용합니다.");
            setDefaultConfig();
            return;
        }
        
        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);
            LOGGER.info("설정 파일 로드 완료: " + CONFIG_FILE);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "설정 파일 로드 실패. 기본 설정을 사용합니다.", e);
            setDefaultConfig();
        }
    }
    
    /**
     * 기본 설정값 설정
     */
    private void setDefaultConfig() {
        properties.setProperty(KEY_WINDOW_WIDTH, String.valueOf(Constants.DEFAULT_WINDOW_WIDTH));
        properties.setProperty(KEY_WINDOW_HEIGHT, String.valueOf(Constants.DEFAULT_WINDOW_HEIGHT));
        properties.setProperty(KEY_WINDOW_X, "100");
        properties.setProperty(KEY_WINDOW_Y, "100");
        properties.setProperty(KEY_THEME, "system");
        properties.setProperty(KEY_LANGUAGE, "ko");
        properties.setProperty(KEY_AUTO_SAVE, "true");
        properties.setProperty(KEY_MAX_RECENT_FILES, "10");
        properties.setProperty(KEY_GRAPH_ANTI_ALIASING, "true");
        properties.setProperty(KEY_GRAPH_GRID, "true");
        properties.setProperty(KEY_DEFAULT_MATERIAL, "Steel");
        properties.setProperty(KEY_CALCULATION_PRECISION, "6");
    }
    
    /**
     * 설정 파일 저장
     */
    public void saveConfig() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "MEVA Configuration File");
            LOGGER.info("설정 파일 저장 완료: " + CONFIG_FILE);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "설정 파일 저장 실패", e);
        }
    }
    
    /**
     * 문자열 설정값 가져오기
     * 
     * @param key 설정 키
     * @return 설정값 (없으면 null)
     */
    public String getString(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * 문자열 설정값 가져오기 (기본값 포함)
     * 
     * @param key 설정 키
     * @param defaultValue 기본값
     * @return 설정값 (없으면 기본값)
     */
    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * 정수형 설정값 가져오기
     * 
     * @param key 설정 키
     * @param defaultValue 기본값
     * @return 설정값 (없거나 변환 실패시 기본값)
     */
    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "정수 변환 실패: " + key + "=" + value, e);
            return defaultValue;
        }
    }
    
    /**
     * 실수형 설정값 가져오기
     * 
     * @param key 설정 키
     * @param defaultValue 기본값
     * @return 설정값 (없거나 변환 실패시 기본값)
     */
    public double getDouble(String key, double defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "실수 변환 실패: " + key + "=" + value, e);
            return defaultValue;
        }
    }
    
    /**
     * 불린형 설정값 가져오기
     * 
     * @param key 설정 키
     * @param defaultValue 기본값
     * @return 설정값 (없으면 기본값)
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
    
    /**
     * 설정값 저장
     * 
     * @param key 설정 키
     * @param value 설정값
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }
    
    /**
     * 정수형 설정값 저장
     * 
     * @param key 설정 키
     * @param value 설정값
     */
    public void setProperty(String key, int value) {
        properties.setProperty(key, String.valueOf(value));
    }
    
    /**
     * 실수형 설정값 저장
     * 
     * @param key 설정 키
     * @param value 설정값
     */
    public void setProperty(String key, double value) {
        properties.setProperty(key, String.valueOf(value));
    }
    
    /**
     * 불린형 설정값 저장
     * 
     * @param key 설정 키
     * @param value 설정값
     */
    public void setProperty(String key, boolean value) {
        properties.setProperty(key, String.valueOf(value));
    }
    
    /**
     * 설정값 제거
     * 
     * @param key 설정 키
     */
    public void removeProperty(String key) {
        properties.remove(key);
    }
    
    /**
     * 모든 설정 초기화
     */
    public void resetConfig() {
        properties.clear();
        setDefaultConfig();
        saveConfig();
        LOGGER.info("설정 초기화 완료");
    }
}
