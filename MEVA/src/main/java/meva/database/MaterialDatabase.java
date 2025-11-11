package meva.database;

import java.util.*;
import meva.fileio.CSVHandler;

/**
 * 재료 데이터베이스 클래스
 * 표준 재료의 물성값을 관리
 */
public class MaterialDatabase {
    
    private Map<String, Map<String, Double>> materials;
    private CSVHandler csvHandler;
    
    public MaterialDatabase() {
        this.materials = new HashMap<>();
        this.csvHandler = new CSVHandler();
    }
    
    /**
     * 표준 재료 데이터 로드
     */
    public void loadStandardMaterials() {
        // TODO: CSV 파일에서 표준 재료 데이터 로드
        addSampleMaterials();
    }
    
    /**
     * 샘플 재료 데이터 추가 (테스트용)
     */
    private void addSampleMaterials() {
        // 알루미늄
        Map<String, Double> alProperties = new HashMap<>();
        alProperties.put("youngsModulus", 70000.0);  // MPa
        alProperties.put("yieldStrength", 276.0);    // MPa
        alProperties.put("tensileStrength", 310.0);  // MPa
        alProperties.put("density", 2700.0);         // kg/m³
        materials.put("Aluminum", alProperties);
        
        // 강철
        Map<String, Double> steelProperties = new HashMap<>();
        steelProperties.put("youngsModulus", 200000.0); // MPa
        steelProperties.put("yieldStrength", 250.0);    // MPa
        steelProperties.put("tensileStrength", 400.0);  // MPa
        steelProperties.put("density", 7850.0);         // kg/m³
        materials.put("Steel", steelProperties);
    }
    
    /**
     * 재료 물성값 조회
     * @param materialName 재료명
     * @return 물성값 맵
     */
    public Map<String, Double> getMaterialProperties(String materialName) {
        return materials.get(materialName);
    }
    
    /**
     * 표준 재료명 목록 반환
     * @return 재료명 목록
     */
    public List<String> getStandardMaterialNames() {
        return new ArrayList<>(materials.keySet());
    }
    
    /**
     * 새 재료 추가
     * @param materialName 재료명
     * @param properties 물성값
     * @return 추가 성공 여부
     */
    public boolean addMaterial(String materialName, Map<String, Double> properties) {
        if (materialName == null || properties == null) {
            return false;
        }
        materials.put(materialName, new HashMap<>(properties));
        return true;
    }
    
    /**
     * 재료 삭제
     * @param materialName 재료명
     * @return 삭제 성공 여부
     */
    public boolean removeMaterial(String materialName) {
        return materials.remove(materialName) != null;
    }
}
