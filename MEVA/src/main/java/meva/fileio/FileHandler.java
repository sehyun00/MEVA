package meva.fileio;

/**
 * 파일 처리 인터페이스
 * CSV, Excel 등 다양한 파일 형식을 처리하기 위한 공통 인터페이스
 */
public interface FileHandler {
    
    /**
     * 파일을 읽어서 데이터를 반환
     * @param filePath 파일 경로
     * @return 읽어온 데이터
     */
    Object readFile(String filePath);
    
    /**
     * 데이터를 파일로 저장
     * @param data 저장할 데이터
     * @param filePath 저장할 파일 경로
     * @return 저장 성공 여부
     */
    boolean writeFile(Object data, String filePath);
    
    /**
     * 지원하는 파일 형식인지 확인
     * @param filePath 파일 경로
     * @return 지원 여부
     */
    boolean isSupported(String filePath);
}
