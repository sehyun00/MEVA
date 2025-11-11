package meva.fileio;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

/**
 * CSV 파일 처리 클래스
 * CSV 형식의 데이터 파일을 읽고 쓰는 기능을 제공
 */
public class CSVHandler implements FileHandler {
    
    @Override
    public Object readFile(String filePath) {
        List<String[]> data = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                data.add(line.split(","));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        
        return data;
    }
    
    @Override
    public boolean writeFile(Object data, String filePath) {
        if (!(data instanceof List)) {
            return false;
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            @SuppressWarnings("unchecked")
            List<String[]> csvData = (List<String[]>) data;
            
            for (String[] row : csvData) {
                writer.println(String.join(",", row));
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public boolean isSupported(String filePath) {
        return filePath != null && filePath.toLowerCase().endsWith(".csv");
    }
}
