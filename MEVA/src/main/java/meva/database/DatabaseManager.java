package meva.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:meva.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
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

            String sqlData = "CREATE TABLE IF NOT EXISTS tensile_data (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "test_id INTEGER NOT NULL, " +
                    "load_value REAL, " +
                    "displacement REAL, " +
                    "timestamp REAL, " +
                    "FOREIGN KEY(test_id) REFERENCES tensile_tests(id) ON DELETE CASCADE" +
                    ");";
            stmt.execute(sqlData);

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

            System.out.println("[DatabaseManager] DB Initialized.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

