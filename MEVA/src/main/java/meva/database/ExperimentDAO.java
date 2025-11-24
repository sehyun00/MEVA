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
     * 실험 저장 (INSERT)
     * @param experiment 저장할 실험 객체
     * @return 생성된 실험 ID (실패 시 -1)
     */
    public int saveExperiment(Experiment experiment) {
        String sql = "INSERT INTO experiments (material_id, specimen_diameter, gauge_length, " +
                     "cross_section_area, test_date, test_temperature, test_speed, " +
                     "data_file_path, remarks) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, experiment.getMaterialId());
            pstmt.setDouble(2, experiment.getSpecimenDiameter());
            pstmt.setDouble(3, experiment.getGaugeLength());
            pstmt.setDouble(4, experiment.getCrossSectionArea());
            pstmt.setString(5, experiment.getTestDate());
            
            if (experiment.getTestTemperature() != null) {
                pstmt.setDouble(6, experiment.getTestTemperature());
            } else {
                pstmt.setNull(6, Types.DOUBLE);
            }
            
            if (experiment.getTestSpeed() != null) {
                pstmt.setDouble(7, experiment.getTestSpeed());
            } else {
                pstmt.setNull(7, Types.DOUBLE);
            }
            
            pstmt.setString(8, experiment.getDataFilePath());
            pstmt.setString(9, experiment.getRemarks());
            
            int rows = pstmt.executeUpdate();
            
            if (rows > 0) {
                // 생성된 ID 가져오기
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int experimentId = generatedKeys.getInt(1);
                        System.out.println("[정보] 실험 저장 완료 (ID: " + experimentId + ")");
                        return experimentId;
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("[오류] 실험 저장 실패: " + e.getMessage());
            e.printStackTrace();
        }
        
        return -1;
    }
    
    /**
     * 계산 결과 저장
     * @param experimentId 실험 ID
     * @param maxStress 최대 응력
     * @param strainAtMaxStress 최대 응력에서의 변형률
     * @param uts 극한인장강도
     * @return 성공 여부
     */
    public boolean saveCalculationResults(int experimentId, Double maxStress, 
                                          Double strainAtMaxStress, Double uts) {
        String sql = "INSERT INTO calculation_results (experiment_id, max_stress, " +
                     "strain_at_max_stress, uts) VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT(experiment_id) DO UPDATE SET " +
                     "max_stress = excluded.max_stress, " +
                     "strain_at_max_stress = excluded.strain_at_max_stress, " +
                     "uts = excluded.uts";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, experimentId);
            
            if (maxStress != null) {
                pstmt.setDouble(2, maxStress);
            } else {
                pstmt.setNull(2, Types.DOUBLE);
            }
            
            if (strainAtMaxStress != null) {
                pstmt.setDouble(3, strainAtMaxStress);
            } else {
                pstmt.setNull(3, Types.DOUBLE);
            }
            
            if (uts != null) {
                pstmt.setDouble(4, uts);
            } else {
                pstmt.setNull(4, Types.DOUBLE);
            }
            
            int rows = pstmt.executeUpdate();
            
            if (rows > 0) {
                System.out.println("[정보] 계산 결과 저장 완료 (실험 ID: " + experimentId + ")");
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("[오류] 계산 결과 저장 실패: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * 모든 실험 조회
     */
    public List<Experiment> getAllExperiments() {
        List<Experiment> experiments = new ArrayList<>();
        String sql = "SELECT e.id, e.material_id, e.test_date, e.specimen_diameter, e.gauge_length, " +
                     "m.name AS material_name, c.max_stress, c.uts " +
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
                exp.setSpecimenDiameter(rs.getDouble("specimen_diameter"));
                exp.setGaugeLength(rs.getDouble("gauge_length"));
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
            "SELECT e.id, e.material_id, e.test_date, e.specimen_diameter, e.gauge_length, " +
            "m.name AS material_name, c.max_stress, c.uts " +
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
                    exp.setSpecimenDiameter(rs.getDouble("specimen_diameter"));
                    exp.setGaugeLength(rs.getDouble("gauge_length"));
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
