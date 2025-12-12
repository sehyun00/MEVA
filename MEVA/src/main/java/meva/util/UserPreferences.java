package meva.util;

import java.util.prefs.Preferences;

/**
 * 사용자 설정을 저장하고 불러오는 유틸리티 클래스
 * Java Preferences API를 사용하여 윈도우 레지스트리 또는 사용자 홈에 저장
 * 
 * @author MEVA 개발팀
 * @version 1.0
 */
public class UserPreferences {

    private static final Preferences prefs = Preferences.userNodeForPackage(UserPreferences.class);

    // 키 상수
    private static final String KEY_WINDOW_X = "window.x";
    private static final String KEY_WINDOW_Y = "window.y";
    private static final String KEY_WINDOW_WIDTH = "window.width";
    private static final String KEY_WINDOW_HEIGHT = "window.height";
    private static final String KEY_WINDOW_MAXIMIZED = "window.maximized";

    private static final String KEY_SPLIT_LEFT_RIGHT = "split.leftRight";
    private static final String KEY_SPLIT_GRAPH_RESULT = "split.graphResult";

    private static final String KEY_LAST_FILE_PATH = "file.lastPath";
    private static final String KEY_LAST_EXPORT_PATH = "file.lastExportPath";

    // ========== 윈도우 위치/크기 ==========

    public static void saveWindowBounds(int x, int y, int width, int height, boolean maximized) {
        prefs.putInt(KEY_WINDOW_X, x);
        prefs.putInt(KEY_WINDOW_Y, y);
        prefs.putInt(KEY_WINDOW_WIDTH, width);
        prefs.putInt(KEY_WINDOW_HEIGHT, height);
        prefs.putBoolean(KEY_WINDOW_MAXIMIZED, maximized);
    }

    public static int getWindowX(int defaultValue) {
        return prefs.getInt(KEY_WINDOW_X, defaultValue);
    }

    public static int getWindowY(int defaultValue) {
        return prefs.getInt(KEY_WINDOW_Y, defaultValue);
    }

    public static int getWindowWidth(int defaultValue) {
        return prefs.getInt(KEY_WINDOW_WIDTH, defaultValue);
    }

    public static int getWindowHeight(int defaultValue) {
        return prefs.getInt(KEY_WINDOW_HEIGHT, defaultValue);
    }

    public static boolean isWindowMaximized() {
        return prefs.getBoolean(KEY_WINDOW_MAXIMIZED, false);
    }

    // ========== 패널 분할 위치 ==========

    public static void setSplitLeftRight(int location) {
        prefs.putInt(KEY_SPLIT_LEFT_RIGHT, location);
    }

    public static int getSplitLeftRight(int defaultValue) {
        return prefs.getInt(KEY_SPLIT_LEFT_RIGHT, defaultValue);
    }

    public static void setSplitGraphResult(int location) {
        prefs.putInt(KEY_SPLIT_GRAPH_RESULT, location);
    }

    public static int getSplitGraphResult(int defaultValue) {
        return prefs.getInt(KEY_SPLIT_GRAPH_RESULT, defaultValue);
    }

    // ========== 파일 경로 ==========

    public static void setLastFilePath(String path) {
        if (path != null && !path.isEmpty()) {
            // 파일이면 부모 디렉토리 저장
            java.io.File file = new java.io.File(path);
            if (file.isFile()) {
                path = file.getParent();
            }
            prefs.put(KEY_LAST_FILE_PATH, path);
        }
    }

    public static String getLastFilePath() {
        return prefs.get(KEY_LAST_FILE_PATH, System.getProperty("user.home"));
    }

    public static void setLastExportPath(String path) {
        if (path != null && !path.isEmpty()) {
            java.io.File file = new java.io.File(path);
            if (file.isFile()) {
                path = file.getParent();
            }
            prefs.put(KEY_LAST_EXPORT_PATH, path);
        }
    }

    public static String getLastExportPath() {
        return prefs.get(KEY_LAST_EXPORT_PATH, System.getProperty("user.home"));
    }

    // ========== 초기화 ==========

    /**
     * 모든 설정 초기화
     */
    public static void clearAll() {
        try {
            prefs.clear();
        } catch (Exception e) {
            System.err.println("설정 초기화 실패: " + e.getMessage());
        }
    }
}
