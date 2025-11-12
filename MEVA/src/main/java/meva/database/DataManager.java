package meva.database;

import java.util.List;
import java.util.Map;

/**
 * 데이터 관리 클래스
 * 재료 데이터베이스와 시험 데이터를 관리
 */
public class DataManager {
    
    private MaterialDatabase materialDB;
    
    public DataManager() {
        this.materialDB = new MaterialDatabase();
    }
    
    /**
     * 재료 데이터베이스 초기화
     */
    public void initializeDatabase() {
        materialDB.loadStandardMaterials();
    }
    
    /**
     * 재료명으로 물성값 조회
     * @param materialName 재료명
     * @return 물성값 맵
     */
    public Map<String, Double> getMaterialProperties(String materialName) {
        return materialDB.getMaterialProperties(materialName);
    }
    
    /**
     * 표준 재료 목록 조회
     * @return 표준 재료 목록
     */
    public List<String> getStandardMaterialNames() {
        return materialDB.getStandardMaterialNames();
    }
    
    /**
     * 새로운 재료 추가
     * @param materialName 재료명
     * @param properties 물성값
     * @return 추가 성공 여부
     */
    public boolean addMaterial(String materialName, Map<String, Double> properties) {
        return materialDB.addMaterial(materialName, properties);
    }
    
    /**
     * 재료 삭제
     * @param materialName 삭제할 재료명
     * @return 삭제 성공 여부
     */
    public boolean removeMaterial(String materialName) {
        return materialDB.removeMaterial(materialName);
    }
}
