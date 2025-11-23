package meva.database;

import java.util.List;
import java.util.ArrayList;
import meva.models.Material;

/**
 * 데이터 관리 클래스 (통합 관리자)
 * - 기존 MaterialDatabase(HashMap)와 MaterialDAO(SQLite)를 통합하여 관리
 * - UI 계층은 이 클래스를 통해 데이터에 접근함
 */
public class DataManager {
    
    private MaterialDAO materialDAO;
    
    public DataManager() {
        this.materialDAO = new MaterialDAO();
    }
    
    /**
     * 데이터베이스 초기화
     * - 테이블 생성 및 초기 데이터 로드 확인
     */
    public void initializeDatabase() {
        // 1. 테이블 생성
        DatabaseManager.initializeDatabase();
        
        // 2. 데이터 확인 및 초기화 (비어있으면 샘플 데이터 추가)
        if (getAllMaterials().isEmpty()) {
            addSampleDataToDB();
        }
    }
    
    /**
     * 모든 재료 목록 반환 (콤보박스용)
     */
    public List<String> getMaterialNames() {
        List<Material> materials = materialDAO.getAllMaterials();
        List<String> names = new ArrayList<>();
        for (Material m : materials) {
            names.add(m.getName());
        }
        return names;
    }
    
    /**
     * 이름으로 재료 객체 반환
     */
    public Material getMaterialByName(String name) {
        return materialDAO.getMaterialByName(name);
    }
    
    /**
     * 모든 재료 객체 리스트 반환
     */
    public List<Material> getAllMaterials() {
        return materialDAO.getAllMaterials();
    }
    
    /**
     * 샘플 데이터 DB 추가 (초기 실행 시)
     */
    private void addSampleDataToDB() {
        System.out.println("[DataManager] DB가 비어있어 샘플 데이터를 추가합니다.");
        
        // 표준 재료 데이터 (CSV 내용 기반)
        addMaterialSafely(new Material("Steel_AISI1020", "Metal", 200, 250, 400, 7.85, 0.29, 530, 0.26, 0.25));
        addMaterialSafely(new Material("Aluminum_6061T6", "Metal", 69, 276, 310, 2.70, 0.33, 410, 0.05, 0.12));
        addMaterialSafely(new Material("Titanium_Ti6Al4V", "Metal", 114, 880, 950, 4.43, 0.32, 1200, 0.10, 0.10));
    }
    
    private void addMaterialSafely(Material material) {
        if (materialDAO.getMaterialByName(material.getName()) == null) {
            materialDAO.addMaterial(material);
        }
    }
}
