package meva.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite 데이터베이스 연결 및 초기화를 담당하는 클래스
 */
public class DatabaseManager {
    // 프로젝트 루트 폴더에 meva.db 파일 생성됨
    private static final String DB_URL = "jdbc:sqlite:meva.db";

    /**
     * DB 연결 객체 반환
     * @return Connection 객체
     * @throws SQLException 연결 실패 시
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * 테이블 초기화 메서드
     * 애플리케이션 시작 시 호출하여 필요한 테이블 생성
     */
    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Materials 테이블 생성
            String sqlMaterials = "CREATE TABLE IF NOT EXISTS materials (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL UNIQUE, " +
                    "category TEXT, " +
                    "youngs_modulus REAL, " +
                    "yield_strength REAL, " +
                    "tensile_strength REAL, " +
                    "density REAL, " +
                    "poisson_ratio REAL, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");";
            stmt.execute(sqlMaterials);

            // Experiments 테이블 생성 (기존 TensileTests 대체)
            String sqlExperiments = "CREATE TABLE IF NOT EXISTS experiments (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "material_id INTEGER NOT NULL, " +
                    "specimen_diameter REAL, " +
                    "gauge_length REAL, " +
                    "cross_section_area REAL, " +
                    "test_date TEXT, " +
                    "test_temperature REAL, " +
                    "test_speed REAL, " +
                    "data_file_path TEXT, " +
                    "remarks TEXT, " +
                    "FOREIGN KEY(material_id) REFERENCES materials(id) ON DELETE CASCADE" +
                    ");";
            stmt.execute(sqlExperiments);

            // TensileData 테이블 생성 (시계열 데이터)
            String sqlData = "CREATE TABLE IF NOT EXISTS tensile_data (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "experiment_id INTEGER NOT NULL, " +
                    "load_value REAL, " +
                    "displacement REAL, " +
                    "timestamp REAL, " +
                    "FOREIGN KEY(experiment_id) REFERENCES experiments(id) ON DELETE CASCADE" +
                    ");";
            stmt.execute(sqlData);

            // CalculationResults 테이블 생성 (기존 SimulationResults 대체)
            String sqlResults = "CREATE TABLE IF NOT EXISTS calculation_results (" +
                    "experiment_id INTEGER PRIMARY KEY, " +
                    "max_stress REAL, " +
                    "strain_at_max_stress REAL, " +
                    "uts REAL, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY(experiment_id) REFERENCES experiments(id) ON DELETE CASCADE" +
                    ");";
            stmt.execute(sqlResults);

            // 기본 재료 데이터 삽입 (FK 제약조건 만족을 위해)
            String sqlInitMaterial = "INSERT OR IGNORE INTO materials (id, name, category, youngs_modulus, yield_strength, tensile_strength) " +
                    "VALUES (1, 'Default Steel', 'Steel', 200000, 250, 400);";
            stmt.execute(sqlInitMaterial);

            System.out.println("[DatabaseManager] 데이터베이스 연결 및 테이블 초기화 완료");

        } catch (SQLException e) {
            System.err.println("[DatabaseManager] DB 초기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
