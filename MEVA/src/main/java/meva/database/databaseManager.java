package materialsengineering;


import java.io.InputStream;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;

// Optional: HikariCP 사용 권장 (성능/커넥션풀링)
// Maven dependency:
// <dependency>
//   <groupId>com.zaxxer</groupId>
//   <artifactId>HikariCP</artifactId>
//   <version>5.0.1</version>
// </dependency>

public class databaseManager {
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final boolean usePool;

    // Simple connection pool fallback (DriverManager) if HikariCP not available
    // If you add HikariCP, replace getConnection() implementation.

    public databaseManager(String jdbcUrl, String username, String password, boolean usePool) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.usePool = usePool;
    }

    /**
     * 초기화: 드라이버 로드나 풀 초기화 수행
     */
    public void init() throws SQLException {
        try {
            // 드라이버 자동 로드 (JDBC 4 이상에서는 대개 필요 없음)
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            // 드라이버가 없을 경우 로그남기기(또는 다른 RDBMS 드라이버 기재)
            System.err.println("PostgreSQL JDBC Driver not found; ensure dependency is added if using PostgreSQL.");
        }

        // 풀 사용 설정이 있으면 여기서 풀 초기화
        if (usePool) {
            // HikariCP 권장 - 프로젝트에 의존성 추가 후 풀 설정을 여기에 작성하세요.
            // 현재 샘플은 DriverManager 기반으로 동작합니다n
        }

        // 테이블이 없으면 생성
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            createTablesIfNotExist(conn);
            conn.commit();
        }
    }

    private Connection getConnection() throws SQLException {
        // 추후 HikariCP로 교체 권장
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    /**************************************************************************
     * DDL: 테이블 생성 (제약조건, 인덱스 포함)
     * 설계 목적: 재료 표준값, 인장 시험 메타데이터, 개별 측정 결과, 시뮬레이션 결과,
     *            계산 비교(테스트 결과 vs 표준값) 등을 담을 수 있게 함.
     **************************************************************************/
    private void createTablesIfNotExist(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            // 표준 물성값 테이블: materials_standard
            st.execute(
                    "CREATE TABLE IF NOT EXISTS materials_standard (" +
                    "  material_id SERIAL PRIMARY KEY," +
                    "  name VARCHAR(100) NOT NULL UNIQUE," +
                    "  category VARCHAR(50)," +
                    "  density DOUBLE PRECISION,
                    " +
                    "  youngs_modulus DOUBLE PRECISION,
                    "  yield_strength DOUBLE PRECISION,
                    "  tensile_strength DOUBLE PRECISION,
                    "  poisson_ratio DOUBLE PRECISION,
                    "  last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP" +
                    ");"
            );

            // 인장 시험 메타데이터: tensile_tests
            st.execute(
                    "CREATE TABLE IF NOT EXISTS tensile_tests (" +
                    "  test_id BIGSERIAL PRIMARY KEY," +
                    "  sample_id VARCHAR(100) NOT NULL,
                    " +
                    "  material_id INTEGER REFERENCES materials_standard(material_id) ON DELETE SET NULL,
                    "  operator VARCHAR(100)," +
                    "  machine VARCHAR(100)," +
                    "  gauge_length DOUBLE PRECISION,
                    "  cross_section_area DOUBLE PRECISION,
                    "  test_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    "  notes TEXT" +
                    ");"
            );

            // 인장 시험 결과(시간/하중/변위 등) — 대용량 저장: 분할 파티셔닝 권장
            st.execute(
                    "CREATE TABLE IF NOT EXISTS tensile_results (" +
                    "  result_id BIGSERIAL PRIMARY KEY," +
                    "  test_id BIGINT NOT NULL REFERENCES tensile_tests(test_id) ON DELETE CASCADE," +
                    "  seq INTEGER NOT NULL," +
                    "  time_ms DOUBLE PRECISION NOT NULL," +
                    "  load DOUBLE PRECISION NOT NULL," +
                    "  displacement DOUBLE PRECISION NOT NULL," +
                    "  strain DOUBLE PRECISION NULL," +
                    "  stress DOUBLE PRECISION NULL" +
                    ");"
            );

            // 시뮬레이션 결과 테이블
            st.execute(
                    "CREATE TABLE IF NOT EXISTS simulations (" +
                    "  sim_id BIGSERIAL PRIMARY KEY," +
                    "  test_id BIGINT REFERENCES tensile_tests(test_id) ON DELETE SET NULL," +
                    "  sim_name VARCHAR(200)," +
                    "  sim_params JSONB,
                    " +
                    "  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP" +
                    ");"
            );

            // 계산 비교 결과
            st.execute(
                    "CREATE TABLE IF NOT EXISTS comparisons (" +
                    "  comp_id BIGSERIAL PRIMARY KEY," +
                    "  test_id BIGINT REFERENCES tensile_tests(test_id) ON DELETE CASCADE," +
                    "  metric VARCHAR(100) NOT NULL," +
                    "  value DOUBLE PRECISION NOT NULL," +
                    "  standard_value DOUBLE PRECISION NULL," +
                    "  deviation DOUBLE PRECISION NULL," +
                    "  computed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP" +
                    ");"
            );

            // 인덱스: 자주 조회되는 컬럼에 인덱스 부여
            st.execute("CREATE INDEX IF NOT EXISTS idx_tensile_results_test_id_seq ON tensile_results(test_id, seq);");
            st.execute("CREATE INDEX IF NOT EXISTS idx_tensile_tests_sample_id ON tensile_tests(sample_id);");
            st.execute("CREATE INDEX IF NOT EXISTS idx_materials_name ON materials_standard(name);");

            // 파티셔닝 권장 주석: 대용량 테이블은 날짜/테스트 단위 파티셔닝 고려
            // PostgreSQL의 경우 time-based 또는 test_id-based partitioning 권장
        }
    }

    /**************************************************************************
     * CRUD API 설계: 주요 함수명/스펙 (간단 구현 포함)
     * - insertMaterialStandard
     * - upsertMaterialStandard
     * - insertTensileTest
     * - batchInsertTensileResults (대용량 배치)
     * - queryTestSummary
     * - streamTensileResults (메모리 최적화)
     **************************************************************************/

    public long insertMaterialStandard(String name, String category, Double density,
                                       Double youngsModulus, Double yieldStrength,
                                       Double tensileStrength, Double poissonRatio) throws SQLException {
        final String sql = "INSERT INTO materials_standard(name, category, density, youngs_modulus, yield_strength, tensile_strength, poisson_ratio) " +
                "VALUES(?,?,?,?,?,?,?) RETURNING material_id";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setObject(3, density);
            ps.setObject(4, youngsModulus);
            ps.setObject(5, yieldStrength);
            ps.setObject(6, tensileStrength);
            ps.setObject(7, poissonRatio);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1;
    }

    public void upsertMaterialStandard(String name, String category, Double density,
                                       Double youngsModulus, Double yieldStrength,
                                       Double tensileStrength, Double poissonRatio) throws SQLException {
        // PostgreSQL ON CONFLICT 사용 예
        final String sql = "INSERT INTO materials_standard(name, category, density, youngs_modulus, yield_strength, tensile_strength, poisson_ratio) " +
                "VALUES(?,?,?,?,?,?,?) " +
                "ON CONFLICT (name) DO UPDATE SET category=EXCLUDED.category, density=EXCLUDED.density, youngs_modulus=EXCLUDED.youngs_modulus, " +
                "yield_strength=EXCLUDED.yield_strength, tensile_strength=EXCLUDED.tensile_strength, poisson_ratio=EXCLUDED.poisson_ratio, last_updated=CURRENT_TIMESTAMP";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setObject(3, density);
            ps.setObject(4, youngsModulus);
            ps.setObject(5, yieldStrength);
            ps.setObject(6, tensileStrength);
            ps.setObject(7, poissonRatio);
            ps.executeUpdate();
        }
    }

    public long insertTensileTest(String sampleId, Long materialId, String operator, String machine,
                                  Double gaugeLength, Double crossSectionArea, String notes) throws SQLException {
        final String sql = "INSERT INTO tensile_tests(sample_id, material_id, operator, machine, gauge_length, cross_section_area, notes) " +
                "VALUES(?,?,?,?,?,?,?) RETURNING test_id";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sampleId);
            if (materialId != null) ps.setLong(2, materialId); else ps.setNull(2, Types.BIGINT);
            ps.setString(3, operator);
            ps.setString(4, machine);
            ps.setObject(5, gaugeLength);
            ps.setObject(6, crossSectionArea);
            ps.setString(7, notes);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1;
    }

    /**
     * 대용량 결과를 배치로 삽입: 메모리 사용 최소화
     * - PreparedStatement 배치 사용
     * - 트랜잭션 단위로 커밋 (예: 1000개씩)
     */
    public void batchInsertTensileResults(long testId, Iterator<TensilePoint> points, int batchSize) throws SQLException {
        final String sql = "INSERT INTO tensile_results(test_id, seq, time_ms, load, displacement, strain, stress) VALUES(?,?,?,?,?,?,?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            int count = 0;
            while (points.hasNext()) {
                TensilePoint p = points.next();
                ps.setLong(1, testId);
                ps.setInt(2, p.seq);
                ps.setDouble(3, p.timeMs);
                ps.setDouble(4, p.load);
                ps.setDouble(5, p.displacement);
                if (p.strain != null) ps.setObject(6, p.strain); else ps.setNull(6, Types.DOUBLE);
                if (p.stress != null) ps.setObject(7, p.stress); else ps.setNull(7, Types.DOUBLE);
                ps.addBatch();
                count++;
                if (count % batchSize == 0) {
                    ps.executeBatch();
                    conn.commit();
                }
            }
            if (count % batchSize != 0) {
                ps.executeBatch();
                conn.commit();
            }
            conn.setAutoCommit(true);
        }
    }

    /**
     * 메모리 최적화를 위해 ResultSet을 스트리밍 처리
     * consumer는 각 레코드를 받아서 처리(예: 파일로 쓰기, 통계 집계)
     */
    public void streamTensileResults(long testId, Consumer<TensilePoint> consumer) throws SQLException {
        final String sql = "SELECT seq, time_ms, load, displacement, strain, stress FROM tensile_results WHERE test_id = ? ORDER BY seq";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            ps.setLong(1, testId);
            // PostgreSQL streaming: set fetch size
            ps.setFetchSize(1000);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TensilePoint p = new TensilePoint();
                    p.seq = rs.getInt("seq");
                    p.timeMs = rs.getDouble("time_ms");
                    p.load = rs.getDouble("load");
                    p.displacement = rs.getDouble("displacement");
                    p.strain = (Double) rs.getObject("strain");
                    p.stress = (Double) rs.getObject("stress");
                    consumer.accept(p);
                }
            }
        }
    }

    /**
     * 간단 비교 쿼리: 테스트의 특정 metric과 표준값 비교
     */
    public void computeAndStoreComparison(long testId, String metric, double computedValue) throws SQLException {
        // 표준값 조회 (예: material의 standard value 혹은 전역 표준 테이블에서 조회)
        String lookupSql = "SELECT m." + metric + " FROM materials_standard m JOIN tensile_tests t ON t.material_id = m.material_id WHERE t.test_id = ?";
        Double standard = null;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(lookupSql)) {
            ps.setLong(1, testId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    standard = (Double) rs.getObject(1);
                }
            }
        }

        double deviation = Double.NaN;
        if (standard != null) {
            deviation = computedValue - standard;
        }

        final String insertSql = "INSERT INTO comparisons(test_id, metric, value, standard_value, deviation) VALUES(?,?,?,?,?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setLong(1, testId);
            ps.setString(2, metric);
            ps.setDouble(3, computedValue);
            if (standard != null) ps.setDouble(4, standard); else ps.setNull(4, Types.DOUBLE);
            if (!Double.isNaN(deviation)) ps.setDouble(5, deviation); else ps.setNull(5, Types.DOUBLE);
            ps.executeUpdate();
        }
    }

    // 기타 유틸: 대량 데이터 Import (CSV 스트림 -> 배치 삽입)
    public void importTensileCsv(long testId, InputStream csvStream, int batchSize, CsvParser parser) throws SQLException {
        Iterator<TensilePoint> it = parser.parse(csvStream);
        batchInsertTensileResults(testId, it, batchSize);
    }

    public void close() {
        // pool 사용 시 풀 종료
    }

    /**************************************************************************
     * 보조 클래스/인터페이스
     **************************************************************************/
    public static class TensilePoint {
        public int seq;
        public double timeMs;
        public double load;
        public double displacement;
        public Double strain;
        public Double stress;
    }

    public interface CsvParser {
        Iterator<TensilePoint> parse(InputStream csvStream);
    }

}
