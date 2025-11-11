package meva.fileio;

/**
 * Excel 파일 처리 클래스
 * Excel 형식의 데이터 파일을 읽고 쓰는 기능을 제공
 * Apache POI 라이브러리 사용
 */
public class ExcelHandler implements FileHandler {
    
    @Override
    public Object readFile(String filePath) {
        // TODO: Apache POI를 사용한 Excel 파일 읽기 구현
        System.out.println("Excel 파일 읽기: " + filePath);
        return null;
    }
    
    @Override
    public boolean writeFile(Object data, String filePath) {
        // TODO: Apache POI를 사용한 Excel 파일 쓰기 구현
        System.out.println("Excel 파일 쓰기: " + filePath);
        return false;
    }
    
    @Override
    public boolean isSupported(String filePath) {
        if (filePath == null) return false;
        String lower = filePath.toLowerCase();
        return lower.endsWith(".xlsx") || lower.endsWith(".xls");
    }
}
