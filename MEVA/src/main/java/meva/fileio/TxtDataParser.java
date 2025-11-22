package meva.fileio;

import meva.models.DataPoint;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * TXT 형식의 실험 데이터 파일을 파싱하는 클래스
 * 탭으로 구분된 데이터를 읽어 DataPoint 리스트로 변환
 * 
 * @author MEVA 개발팀
 * @version 1.0
 */
public class TxtDataParser {
    
    /**
     * TXT 파일을 읽어서 DataPoint 리스트로 변환
     * 
     * @param filePath 파일 경로
     * @return DataPoint 리스트
     * @throws IOException 파일 읽기 실패 시
     */
    public List<DataPoint> parseFile(String filePath) throws IOException {
        List<DataPoint> dataPoints = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // 첫 번째 줄(헤더) 건너뛰기
            String line = br.readLine();
            
            // 데이터 행 읽기
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                try {
                    DataPoint point = parseLine(line);
                    dataPoints.add(point);
                } catch (Exception e) {
                    System.err.println("Failed to parse line: " + line);
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }
        
        return dataPoints;
    }
    
    /**
     * 한 줄의 데이터를 파싱하여 DataPoint 객체로 변환
     * 
     * @param line 탭으로 구분된 데이터 행
     * @return DataPoint 객체
     */
    private DataPoint parseLine(String line) {
        String[] values = line.split("\\s+"); // 공백 또는 탭으로 분리
        
        if (values.length < 9) {
            throw new IllegalArgumentException(
                "Invalid data format. Expected 9 columns, got " + values.length);
        }
        
        double time = Double.parseDouble(values[0]);
        double load = Double.parseDouble(values[1]);
        double displacement = Double.parseDouble(values[2]);
        double strainGage = Double.parseDouble(values[3]);
        double thetaL = Double.parseDouble(values[4]);
        double eStress = Double.parseDouble(values[5]);
        double eStrain = Double.parseDouble(values[6]);
        double tStress = Double.parseDouble(values[7]);
        double tStrain = Double.parseDouble(values[8]);
        
        return new DataPoint(time, load, displacement, strainGage, thetaL,
                           eStress, eStrain, tStress, tStrain);
    }
    
    /**
     * 파일의 유효성을 검사
     * 
     * @param filePath 파일 경로
     * @return 유효한 파일이면 true
     */
    public boolean validateFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String header = br.readLine();
            if (header == null) return false;
            
            // 헤더에 필수 컬럼명이 있는지 확인
            return header.contains("TIME") && header.contains("LOAD") && 
                   header.contains("T.STRESS") && header.contains("T.STRAIN");
        } catch (IOException e) {
            return false;
        }
    }
}
