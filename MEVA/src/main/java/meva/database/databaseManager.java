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

            // TensileTests 테이블 생성
            String sqlTests = "CREATE TABLE IF NOT EXISTS tensile_tests (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "material_id INTEGER NOT NULL, " +
                    "test_date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "specimen_area REAL, " +
                    "specimen_length REAL, " +
                    "temperature REAL, " +
                    "remarks TEXT, " +
                    "FOREIGN KEY(material_id) REFERENCES materials(id) ON DELETE CASCADE" +
                    ");";
            stmt.execute(sqlTests);

            // TensileData 테이블 생성 (시계열 데이터)
            String sqlData = "CREATE TABLE IF NOT EXISTS tensile_data (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "test_id INTEGER NOT NULL, " +
                    "load_value REAL, " +
                    "displacement REAL, " +
                    "timestamp REAL, " +
                    "FOREIGN KEY(test_id) REFERENCES tensile_tests(id) ON DELETE CASCADE" +
                    ");";
            stmt.execute(sqlData);

            // SimulationResults 테이블 생성
            String sqlResults = "CREATE TABLE IF NOT EXISTS simulation_results (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "test_id INTEGER NOT NULL, " +
                    "calculated_youngs_modulus REAL, " +
                    "calculated_yield_strength REAL, " +
                    "calculated_tensile_strength REAL, " +
                    "calculated_elongation REAL, " +
                    "comparison_score REAL, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY(test_id) REFERENCES tensile_tests(id) ON DELETE CASCADE" +
                    ");";
            stmt.execute(sqlResults);

            System.out.println("[DatabaseManager] 데이터베이스 연결 및 테이블 초기화 완료");

        } catch (SQLException e) {
            System.err.println("[DatabaseManager] DB 초기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
