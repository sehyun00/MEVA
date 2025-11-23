package meva.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import meva.models.Material;

/**
 * 재료 데이터 접근 객체 (DAO)
 * DB의 materials 테이블에 대한 CRUD 작업을 수행
 */
public class MaterialDAO {
    
    /**
     * 모든 재료 목록 조회
     * @return 재료 리스트
     */
    public List<Material> getAllMaterials() {
        List<Material> materials = new ArrayList<>();
        String sql = "SELECT * FROM materials";
        
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Material material = mapResultSetToMaterial(rs);
                materials.add(material);
            }
        } catch (SQLException e) {
            System.err.println("[MaterialDAO] 재료 목록 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        
        return materials;
    }
    
    /**
     * 이름으로 재료 조회
     * @param name 재료명
     * @return Material 객체 (없으면 null)
     */
    public Material getMaterialByName(String name) {
        String sql = "SELECT * FROM materials WHERE name = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMaterial(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[MaterialDAO] 재료 조회 실패 (" + name + "): " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 새로운 재료 추가
     * @param material 추가할 재료 객체
     * @return 성공 여부
     */
    public boolean addMaterial(Material material) {
        // 시뮬레이션 필드(K, n, Ef)는 아직 테이블에 없으므로 일단 기본 필드만 저장
        // TODO: DB 스키마 업데이트 후 추가 필드 반영 필요
        String sql = "INSERT INTO materials(name, category, youngs_modulus, yield_strength, tensile_strength, density, poisson_ratio) " +
                     "VALUES(?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, material.getName());
            pstmt.setString(2, material.getCategory());
            pstmt.setDouble(3, material.getYoungsModulus());
            pstmt.setDouble(4, material.getYieldStrength());
            pstmt.setDouble(5, material.getTensileStrength());
            pstmt.setDouble(6, material.getDensity());
            pstmt.setDouble(7, material.getPoissonRatio());
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            System.err.println("[MaterialDAO] 재료 추가 실패: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * ResultSet을 Material 객체로 매핑
     * (시뮬레이션 필드는 DB에 없을 경우 기본값 0.0 또는 추정값 사용)
     */
    private Material mapResultSetToMaterial(ResultSet rs) throws SQLException {
        Material material = new Material();
        material.setId(rs.getInt("id"));
        material.setName(rs.getString("name"));
        material.setCategory(rs.getString("category"));
        material.setYoungsModulus(rs.getDouble("youngs_modulus"));
        material.setYieldStrength(rs.getDouble("yield_strength"));
        material.setTensileStrength(rs.getDouble("tensile_strength"));
        material.setDensity(rs.getDouble("density"));
        material.setPoissonRatio(rs.getDouble("poisson_ratio"));
        
        // TODO: DB에 K, n 컬럼이 추가되기 전까지는 임의의 추정값 사용
        // (실제로는 Hollomon 식 역산 등으로 추정 가능하나 여기서는 단순 처리)
        material.setStrengthCoefficient(material.getTensileStrength() * 1.2); // 임시 추정
        material.setHardeningExponent(0.15); // 임시 기본값
        material.setFractureStrain(0.25);    // 임시 기본값
        
        return material;
    }
}
