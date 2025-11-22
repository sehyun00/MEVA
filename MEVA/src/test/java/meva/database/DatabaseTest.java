package meva.database;

import meva.models.Material;
import java.util.List;

/**
 * SQLite 데이터베이스 기능 테스트 클래스
 * main 메서드를 실행하여 DB 연동이 제대로 작동하는지 확인
 */
public class DatabaseTest {
    public static void main(String[] args) {
        System.out.println("====================================");
        System.out.println("   MEVA SQLite 데이터베이스 테스트");
        System.out.println("====================================\n");
        
        // 1. DB 초기화
        System.out.println("[Step 1] 데이터베이스 초기화 중...");
        DatabaseManager.initializeDatabase();
        
        // 2. DAO 객체 생성
        MaterialDAO dao = new MaterialDAO();
        
        // 3. 샘플 데이터 삽입
        System.out.println("\n[Step 2] 샘플 재료 데이터 추가 중...");
        
        Material aluminum = new Material(
            "Aluminum 6061", "Metal",
            68.9,    // 영률 (GPa)
            276,     // 항복강도 (MPa)
            310,     // 인장강도 (MPa)
            2.7,     // 밀도 (g/cm³)
            0.33     // 포아송비
        );
        
        Material steel = new Material(
            "Stainless Steel 304", "Metal",
            193,     // 영률 (GPa)
            215,     // 항복강도 (MPa)
            505,     // 인장강도 (MPa)
            8.0,     // 밀도 (g/cm³)
            0.29     // 포아송비
        );
        
        Material plastic = new Material(
            "Polycarbonate", "Plastic",
            2.4,     // 영률 (GPa)
            62,      // 항복강도 (MPa)
            72,      // 인장강도 (MPa)
            1.2,     // 밀도 (g/cm³)
            0.37     // 포아송비
        );
        
        dao.addMaterial(aluminum);
        dao.addMaterial(steel);
        dao.addMaterial(plastic);
        
        // 4. 전체 데이터 조회
        System.out.println("\n[Step 3] 저장된 재료 목록 조회:");
        System.out.println("─────────────────────────────────────────────────────────────");
        List<Material> list = dao.getAllMaterials();
        for (Material m : list) {
            System.out.printf("[ID:%d] %s (%s)%n", m.getId(), m.getName(), m.getCategory());
            System.out.printf("  ├ 영률: %.2f GPa%n", m.getYoungsModulus());
            System.out.printf("  ├ 항복강도: %.2f MPa%n", m.getYieldStrength());
            System.out.printf("  ├ 인장강도: %.2f MPa%n", m.getTensileStrength());
            System.out.printf("  └ 밀도: %.2f g/cm³%n", m.getDensity());
            System.out.println();
        }
        
        // 5. 특정 ID로 검색
        System.out.println("[Step 4] ID=1 재료 검색:");
        Material found = dao.getMaterialById(1);
        if (found != null) {
            System.out.println("  → " + found.toString());
        } else {
            System.out.println("  → 결과를 찾을 수 없습니다.");
        }
        
        // 6. 이름으로 검색
        System.out.println("\n[Step 5] 'Aluminum 6061' 재료 검색:");
        Material foundByName = dao.getMaterialByName("Aluminum 6061");
        if (foundByName != null) {
            System.out.println("  → " + foundByName.toString());
        }
        
        System.out.println("\n====================================");
        System.out.println("   테스트 완료!");
        System.out.println("   프로젝트 루트에 meva.db 파일이 생성되었습니다.");
        System.out.println("====================================\n");
    }
}
