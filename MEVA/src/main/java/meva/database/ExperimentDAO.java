package meva.database;

import meva.models.Experiment;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 실험 데이터 접근 객체 (DAO)
 * 데이터베이스에서 실험 데이터 CRUD 작업 수행
 */
public class ExperimentDAO {
    
    /**
     * 모든 실험 조회
     */
    public List<Experiment> getAllExperiments() {
        List<Experiment> experiments = new ArrayList<>();
        String sql = "SELECT e.id, e.material_id, e.test_date, m.name AS material_name, " +
                     "c.max_stress, c.uts " +
                     "FROM experiments e " +
                     "LEFT JOIN materials m ON e.material_id = m.id " +
                     "LEFT JOIN calculation_results c ON e.id = c.experiment_id " +
                     "ORDER BY e.test_date DESC";
        
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Experiment exp = new Experiment();
                exp.setId(rs.getInt("id"));
                exp.setMaterialId(rs.getInt("material_id"));
                exp.setTestDate(rs.getString("test_date"));
                exp.setMaterialName(rs.getString("material_name"));
                
                // 계산 결과 (선택적)
                double maxStress = rs.getDouble("max_stress");
                if (!rs.wasNull()) {
                    exp.setMaxStress(maxStress);
                }
                
                double uts = rs.getDouble("uts");
                if (!rs.wasNull()) {
                    exp.setUts(uts);
                }
                
                experiments.add(exp);
            }
        } catch (SQLException e) {
            System.err.println("[오류] 실험 목록 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        
        return experiments;
    }
    
    /**
     * ID로 실험 조회
     */
    public Experiment getExperimentById(int experimentId) {
        String sql = "SELECT e.*, m.name AS material_name, " +
                     "c.max_stress, c.strain_at_max_stress, c.uts " +
                     "FROM experiments e " +
                     "LEFT JOIN materials m ON e.material_id = m.id " +
                     "LEFT JOIN calculation_results c ON e.id = c.experiment_id " +
                     "WHERE e.id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, experimentId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Experiment exp = new Experiment();
                    exp.setId(rs.getInt("id"));
                    exp.setMaterialId(rs.getInt("material_id"));
                    exp.setMaterialName(rs.getString("material_name"));
                    exp.setSpecimenDiameter(rs.getDouble("specimen_diameter"));
                    exp.setGaugeLength(rs.getDouble("gauge_length"));
                    exp.setCrossSectionArea(rs.getDouble("cross_section_area"));
                    exp.setTestDate(rs.getString("test_date"));
                    exp.setDataFilePath(rs.getString("data_file_path"));
                    exp.setRemarks(rs.getString("remarks"));
                    
                    // 계산 결과
                    double maxStress = rs.getDouble("max_stress");
                    if (!rs.wasNull()) exp.setMaxStress(maxStress);
                    
                    double strainAtMax = rs.getDouble("strain_at_max_stress");
                    if (!rs.wasNull()) exp.setStrainAtMaxStress(strainAtMax);
                    
                    double uts = rs.getDouble("uts");
                    if (!rs.wasNull()) exp.setUts(uts);
                    
                    return exp;
                }
            }
        } catch (SQLException e) {
            System.err.println("[오류] 실험 조회 실패 (ID: " + experimentId + "): " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * 검색 및 필터링
     */
    public List<Experiment> searchExperiments(String searchText, String materialCategory) {
        List<Experiment> experiments = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT e.id, e.material_id, e.test_date, m.name AS material_name, " +
            "c.max_stress, c.uts " +
            "FROM experiments e " +
            "LEFT JOIN materials m ON e.material_id = m.id " +
            "LEFT JOIN calculation_results c ON e.id = c.experiment_id " +
            "WHERE 1=1 ");
        
        List<String> parameters = new ArrayList<>();
        
        if (searchText != null && !searchText.isEmpty()) {
            sql.append("AND m.name LIKE ? ");
            parameters.add("%" + searchText + "%");
        }
        
        if (materialCategory != null && !materialCategory.equals("전체")) {
            sql.append("AND m.category = ? ");
            parameters.add(materialCategory);
        }
        
        sql.append("ORDER BY e.test_date DESC");
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setString(i + 1, parameters.get(i));
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Experiment exp = new Experiment();
                    exp.setId(rs.getInt("id"));
                    exp.setMaterialId(rs.getInt("material_id"));
                    exp.setTestDate(rs.getString("test_date"));
                    exp.setMaterialName(rs.getString("material_name"));
                    
                    double maxStress = rs.getDouble("max_stress");
                    if (!rs.wasNull()) exp.setMaxStress(maxStress);
                    
                    double uts = rs.getDouble("uts");
                    if (!rs.wasNull()) exp.setUts(uts);
                    
                    experiments.add(exp);
                }
            }
        } catch (SQLException e) {
            System.err.println("[오류] 실험 검색 실패: " + e.getMessage());
            e.printStackTrace();
        }
        
        return experiments;
    }
    
    /**
     * 실험 삭제
     */
    public boolean deleteExperiment(int experimentId) {
        String sql = "DELETE FROM experiments WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, experimentId);
            int rows = pstmt.executeUpdate();
            
            System.out.println("[정보] 실험 삭제 완료 (ID: " + experimentId + ")");
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("[오류] 실험 삭제 실패 (ID: " + experimentId + "): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
