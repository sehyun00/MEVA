package meva.database;

import meva.models.Material;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Material 테이블에 대한 데이터 접근 객체 (DAO)
 * CRUD 기능 제공
 */
public class MaterialDAO {

    /**
     * 새로운 재료를 데이터베이스에 추가
     * @param material 추가할 재료 객체
     * @return 성공 여부
     */
    public boolean addMaterial(Material material) {
        String sql = "INSERT INTO materials(name, category, youngs_modulus, yield_strength, " +
                     "tensile_strength, density, poisson_ratio) VALUES(?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, material.getName());
            pstmt.setString(2, material.getCategory());
            pstmt.setDouble(3, material.getYoungsModulus());
            pstmt.setDouble(4, material.getYieldStrength());
            pstmt.setDouble(5, material.getTensileStrength());
            pstmt.setDouble(6, material.getDensity());
            pstmt.setDouble(7, material.getPoissonRatio());
            
            pstmt.executeUpdate();
            System.out.println("[MaterialDAO] 재료 추가 완료: " + material.getName());
            return true;
            
        } catch (SQLException e) {
            System.err.println("[MaterialDAO] 재료 추가 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 모든 재료 목록 조회
     * @return 재료 리스트
     */
    public List<Material> getAllMaterials() {
        List<Material> materials = new ArrayList<>();
        String sql = "SELECT * FROM materials ORDER BY name";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Material m = new Material();
                m.setId(rs.getInt("id"));
                m.setName(rs.getString("name"));
                m.setCategory(rs.getString("category"));
                m.setYoungsModulus(rs.getDouble("youngs_modulus"));
                m.setYieldStrength(rs.getDouble("yield_strength"));
                m.setTensileStrength(rs.getDouble("tensile_strength"));
                m.setDensity(rs.getDouble("density"));
                m.setPoissonRatio(rs.getDouble("poisson_ratio"));
                materials.add(m);
            }

        } catch (SQLException e) {
            System.err.println("[MaterialDAO] 목록 조회 실패: " + e.getMessage());
        }
        return materials;
    }
    
    /**
     * ID로 특정 재료 조회
     * @param id 재료 ID
     * @return Material 객체 또는 null
     */
    public Material getMaterialById(int id) {
        String sql = "SELECT * FROM materials WHERE id = ?";
        Material m = null;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    m = new Material();
                    m.setId(rs.getInt("id"));
                    m.setName(rs.getString("name"));
                    m.setCategory(rs.getString("category"));
                    m.setYoungsModulus(rs.getDouble("youngs_modulus"));
                    m.setYieldStrength(rs.getDouble("yield_strength"));
                    m.setTensileStrength(rs.getDouble("tensile_strength"));
                    m.setDensity(rs.getDouble("density"));
                    m.setPoissonRatio(rs.getDouble("poisson_ratio"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[MaterialDAO] ID 검색 실패: " + e.getMessage());
        }
        return m;
    }

    /**
     * 이름으로 재료 조회
     * @param name 재료명
     * @return Material 객체 또는 null
     */
    public Material getMaterialByName(String name) {
        String sql = "SELECT * FROM materials WHERE name = ?";
        Material m = null;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    m = new Material();
                    m.setId(rs.getInt("id"));
                    m.setName(rs.getString("name"));
                    m.setCategory(rs.getString("category"));
                    m.setYoungsModulus(rs.getDouble("youngs_modulus"));
                    m.setYieldStrength(rs.getDouble("yield_strength"));
                    m.setTensileStrength(rs.getDouble("tensile_strength"));
                    m.setDensity(rs.getDouble("density"));
                    m.setPoissonRatio(rs.getDouble("poisson_ratio"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[MaterialDAO] 이름 검색 실패: " + e.getMessage());
        }
        return m;
    }

    /**
     * 재료 정보 업데이트
     * @param material 수정할 재료 객체 (id 포함)
     * @return 성공 여부
     */
    public boolean updateMaterial(Material material) {
        String sql = "UPDATE materials SET name=?, category=?, youngs_modulus=?, " +
                     "yield_strength=?, tensile_strength=?, density=?, poisson_ratio=? WHERE id=?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, material.getName());
            pstmt.setString(2, material.getCategory());
            pstmt.setDouble(3, material.getYoungsModulus());
            pstmt.setDouble(4, material.getYieldStrength());
            pstmt.setDouble(5, material.getTensileStrength());
            pstmt.setDouble(6, material.getDensity());
            pstmt.setDouble(7, material.getPoissonRatio());
            pstmt.setInt(8, material.getId());
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("[MaterialDAO] 업데이트 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 재료 삭제
     * @param id 삭제할 재료의 ID
     * @return 성공 여부
     */
    public boolean deleteMaterial(int id) {
        String sql = "DELETE FROM materials WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("[MaterialDAO] 삭제 실패: " + e.getMessage());
            return false;
        }
    }
}
