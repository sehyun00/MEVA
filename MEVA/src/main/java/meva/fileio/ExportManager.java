package meva.fileio;

import java.util.HashMap;
import java.util.Map;

/**
 * 내보내기 관리 클래스
 * 다양한 형식으로 데이터를 내보내는 기능을 통합 관리
 */
public class ExportManager {
    
    private Map<String, FileHandler> handlers;
    
    public ExportManager() {
        handlers = new HashMap<>();
        registerHandlers();
    }
    
    /**
     * 파일 핸들러들을 등록
     */
    private void registerHandlers() {
        handlers.put("csv", new CSVHandler());
        handlers.put("excel", new ExcelHandler());
    }
    
    /**
     * 데이터를 지정된 형식으로 내보내기
     * @param data 내보낼 데이터
     * @param filePath 저장할 파일 경로
     * @param format 파일 형식 (csv, excel)
     * @return 내보내기 성공 여부
     */
    public boolean exportData(Object data, String filePath, String format) {
        FileHandler handler = handlers.get(format.toLowerCase());
        if (handler == null) {
            System.err.println("지원하지 않는 파일 형식: " + format);
            return false;
        }
        
        return handler.writeFile(data, filePath);
    }
    
    /**
     * 파일 경로에서 자동으로 형식을 감지하여 내보내기
     * @param data 내보낼 데이터
     * @param filePath 저장할 파일 경로
     * @return 내보내기 성공 여부
     */
    public boolean exportDataAuto(Object data, String filePath) {
        for (FileHandler handler : handlers.values()) {
            if (handler.isSupported(filePath)) {
                return handler.writeFile(data, filePath);
            }
        }
        
        System.err.println("지원하지 않는 파일 형식: " + filePath);
        return false;
    }
}
