package first;

import java.sql.SQLException;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/**
 * StressStrainCalculationEngine.java
 * 
 * 설명(한국어):
 * - 응력-변형률 계산을 위한 Java 기반 계산 엔진 예제입니다.
 * - 탄성 (Elastic) -> 항복 (Yield) -> 소성 경화 (Isotropic hardening) -> 파단 (Fracture)
 *   단계별 알고리즘(단축형, 단축 적용)을 포함합니다.
 * - 영률, 항복강도, 인장강도 등 물성값 계산 로직 포함
 * - SQLite 데이터베이스 스키마 및 기본 CRUD 예제 포함
 * - CSV/Excel 입출력 (CSV는 표준 라이브러리, Excel은 Apache POI 사용 권장)
 * - 모듈 간 인터페이스 정의 (데이터 전달용 DTO 및 Java 인터페이스)
 * 
 * 사용법 요약:
 * 1) MaterialProperties 객체 생성
 * 2) StressStrainEngine.compute(...) 호출
 * 3) DatabaseManager를 통해 결과 저장
 * 4) IOManager를 통해 CSV/Excel로 내보내기
 * 
 * 주의: Excel 내보내기는 Apache POI 라이브러리를 빌드 의존성으로 추가해야 합니다.
 */

public class StressStrainCalculator {

    /* ----------------------------- DTO / 모델 ----------------------------- */
    /** 물성치 */
    public static class MaterialProperties {
        public final double youngsModulus; // 영률 E (Pa)
        public final double yieldStress;   // 항복강도 (Pa)
        public final double tensileStrength; // 인장강도 (ultimate) (Pa)
        public final double hardeningModulus; // 소성 경화 계수 H (Pa)
        public final double fractureStrain; // 파단 변형률 (engineering)

        public MaterialProperties(double E, double sigmaY, double sigmaU, double H, double epsFracture) {
            this.youngsModulus = E;
            this.yieldStress = sigmaY;
            this.tensileStrength = sigmaU;
            this.hardeningModulus = H;
            this.fractureStrain = epsFracture;
        }
    }

    /** 입력 스트레인 시퀀스 */
    public static class StrainPath {
        public final double[] strainSteps; // 누적 혹은 증가량 (assumed incremental engineering strains)
        public final double dt; // 시간 간격(선택적)

        public StrainPath(double[] steps, double dt) {
            this.strainSteps = steps;
            this.dt = dt;
        }
    }

    /** 계산 단계별 결과 */
    public static class StepResult {
        public final int stepIndex;
        public final double totalStrain;    // 누적 변형률 (engineering)
        public final double stress;         // 계산된 응력
        public final double plasticStrain;  // 누적 소성 변형률
        public final String stage;          // "Elastic", "Plastic", "Fracture"

        public StepResult(int idx, double totalStrain, double stress, double plasticStrain, String stage) {
            this.stepIndex = idx;
            this.totalStrain = totalStrain;
            this.stress = stress;
            this.plasticStrain = plasticStrain;
            this.stage = stage;
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "%d,%.6e,%.6e,%.6e,%s",
                    stepIndex, totalStrain, stress, plasticStrain, stage);
        }
    }

    /* ----------------------------- 인터페이스 / 모듈 통신 ----------------------------- */
    /** 계산 엔진 인터페이스 */
    public interface ICalculationEngine {
        List<StepResult> compute(MaterialProperties mat, StrainPath path);
    }

    /** 데이터 저장소 인터페이스 */
    public interface IDataStore {
        void saveResults(String experimentId, List<StepResult> results) throws Exception;
        List<StepResult> loadResults(String experimentId) throws Exception;
    }

    /** 파일 입출력 인터페이스 */
    public interface IIOManager {
        void exportCsv(String filepath, List<StepResult> results) throws IOException;
        void importCsv(String filepath) throws IOException; // 단순 예시
        void exportExcel(String filepath, List<StepResult> results) throws Exception; // Apache POI 권장
    }

    /* ----------------------------- 계산 엔진 구현 ----------------------------- */
    /** 단순 등방성 소성(등방성 경화, 1차원 단축) 계산 엔진 구현 */
    public static class SimpleIsotropicHardeningEngine implements ICalculationEngine {

        /**
         * 알고리즘 설명:
         * - 입력으로 incremental engineering strain steps 받음.
         * - 각 스텝에서 trial stress = E * (totalStrain - plasticStrain)
         * - yield 조건: |trialStress| > yield + H * plasticEquivalent (단순화)
         * - 소성일 경우 반환하는 탄성 복원 응력과 소성 변형률 갱신 (일축 간단 모델)
         * - 파단은 총 누적 변형률(totalStrain) >= fractureStrain로 판단
         *
         * 참고: 실제 재료 모델(예: J2 von Mises, kinematic hardening 등)은 더 복잡하며
         * 이 구현은 교육/프로토타입 목적의 단순화 모델입니다.
         */
        @Override
        public List<StepResult> compute(MaterialProperties mat, StrainPath path) {
            List<StepResult> results = new ArrayList<>();
            double plasticStrain = 0.0; // 누적 소성 변형률
            double totalStrain = 0.0;

            for (int i = 0; i < path.strainSteps.length; i++) {
                double de = path.strainSteps[i];
                totalStrain += de;

                // Trial stress (elastic predictor)
                double trialStress = mat.youngsModulus * (totalStrain - plasticStrain);

                // Simple yield surface (1D) with isotropic hardening
                double currentYield = mat.yieldStress + mat.hardeningModulus * Math.abs(plasticStrain);

                double stress;
                String stageStr;


                if (Math.abs(trialStress) <= currentYield) {
                    // Elastic step
                    stress = trialStress;
                    stageStr = "Elastic";
                } else {
                    // Plastic correction: return mapping (radial return in 1D -> clamp to yield)
                    double sign = Math.signum(trialStress);
                    // Solve for plastic increment dp such that stress = sign*(yield + H*|plastic+dp|)
                    // stress = E*(totalStrain - (plasticStrain+dp))
                    // Equate: E*(totalStrain - plasticStrain - dp) = sign*(yield + H*(|plasticStrain+dp|))
                    // For simplicity assume plasticStrain and dp same sign -> drop abs and sign
                    // => E*(epsilon - ep - dp) = sign*(sigmaY + H*(ep + dp))
                    // rearrange for dp -> (E + H) * dp = E*(epsilon - ep) - sign*sigmaY - H*ep
                    double rhs = mat.youngsModulus * (totalStrain - plasticStrain) - sign * mat.yieldStress - mat.hardeningModulus * plasticStrain;
                    double denom = mat.youngsModulus + mat.hardeningModulus;
                    double dp = rhs / denom;
                    if (Double.isNaN(dp) || Math.abs(dp) < 1e-15) dp = Math.signum(rhs) * 0.0;

                    plasticStrain += dp;
                    stress = mat.youngsModulus * (totalStrain - plasticStrain);
                    stageStr = "Plastic";
                }

                // Check fracture
                if (Math.abs(totalStrain) >= mat.fractureStrain || Math.abs(stress) >= mat.tensileStrength) {
                    stageStr = "Fracture";
                    // Once fractured, we keep the last valid stress but could set to 0 or NaN
                    results.add(new StepResult(i, totalStrain, stress, plasticStrain, stageStr));
                    break; // 중단
                }

                results.add(new StepResult(i, totalStrain, stress, plasticStrain, stageStr));
            }

            return results;
        }
    }

    /* ----------------------------- 데이터베이스 (SQLite) ----------------------------- */
    /** 간단한 SQLite 데이터베이스 구현 - JDBC 사용 */
    public static class DatabaseManager implements IDataStore, AutoCloseable {
        private final Connection conn;

        public static final String CREATE_TABLE_SQL = """
                CREATE TABLE IF NOT EXISTS experiments (
                  id TEXT PRIMARY KEY,
                  description TEXT,
                  created_at TEXT
                );
                """;

        public static final String CREATE_STEP_TABLE_SQL = """
                CREATE TABLE IF NOT EXISTS steps (
                  experiment_id TEXT,
                  step_index INTEGER,
                  total_strain REAL,
                  stress REAL,
                  plastic_strain REAL,
                  stage TEXT,
                  PRIMARY KEY (experiment_id, step_index),
                  FOREIGN KEY (experiment_id) REFERENCES experiments(id)
                );
                """;

        public DatabaseManager(String dbFilePath) throws SQLException {
            String url = "jdbc:sqlite:" + dbFilePath;
            this.conn = DriverManager.getConnection(url);
            try (Statement st = conn.createStatement()) {
                st.execute(CREATE_TABLE_SQL);
                st.execute(CREATE_STEP_TABLE_SQL);
            }
        }

        @Override
        public void saveResults(String experimentId, List<StepResult> results) throws SQLException {
            String insertExp = "INSERT OR REPLACE INTO experiments(id, description, created_at) VALUES(?,?,?)";
            String deleteSteps = "DELETE FROM steps WHERE experiment_id = ?";
            String insertStep = "INSERT INTO steps(experiment_id, step_index, total_strain, stress, plastic_strain, stage) VALUES(?,?,?,?,?,?)";

            try (PreparedStatement pExp = conn.prepareStatement(insertExp);
                 PreparedStatement pDel = conn.prepareStatement(deleteSteps);
                 PreparedStatement pStep = conn.prepareStatement(insertStep)) {

                conn.setAutoCommit(false);

                pExp.setString(1, experimentId);
                pExp.setString(2, "Stress-strain run");
                pExp.setString(3, Instant.now().toString());
                pExp.executeUpdate();

                pDel.setString(1, experimentId);
                pDel.executeUpdate();

                for (StepResult r : results) {
                    pStep.setString(1, experimentId);
                    pStep.setInt(2, r.stepIndex);
                    pStep.setDouble(3, r.totalStrain);
                    pStep.setDouble(4, r.stress);
                    pStep.setDouble(5, r.plasticStrain);
                    pStep.setString(6, r.stage);
                    pStep.addBatch();
                }
                pStep.executeBatch();

                conn.commit();
                conn.setAutoCommit(true);
            }
        }

        @Override
        public List<StepResult> loadResults(String experimentId) throws SQLException {
            String q = "SELECT step_index, total_strain, stress, plastic_strain, stage FROM steps WHERE experiment_id = ? ORDER BY step_index";
            List<StepResult> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setString(1, experimentId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int idx = rs.getInt(1);
                        double eps = rs.getDouble(2);
                        double s = rs.getDouble(3);
                        double ep = rs.getDouble(4);
                        String stage = rs.getString(5);
                        list.add(new StepResult(idx, eps, s, ep, stage));
                    }
                }
            }
            return list;
        }

        @Override
        public void close() throws Exception {
            if (conn != null && !conn.isClosed()) conn.close();
        }
    }

    /* ----------------------------- 파일 입출력 (CSV / Excel) ----------------------------- */
    public static class IOManager implements IIOManager {

        @Override
        public void exportCsv(String filepath, List<StepResult> results) throws IOException {
            Path p = Paths.get(filepath);
            try (BufferedWriter bw = Files.newBufferedWriter(p)) {
                bw.write("step,total_strain,stress,plastic_strain,stage\n");
                for (StepResult r : results) {
                    bw.write(String.format(Locale.US, "%d,%.9e,%.9e,%.9e,%s\n",
                            r.stepIndex, r.totalStrain, r.stress, r.plasticStrain, r.stage));
                }
                bw.flush();
            }
        }

        @Override
        public void importCsv(String filepath) throws IOException {
            // 간단 예시: 파일을 읽고 로그 출력
            Path p = Paths.get(filepath);
            try (BufferedReader br = Files.newBufferedReader(p)) {
                String line;
                boolean first = true;
                while ((line = br.readLine()) != null) {
                    if (first) { first = false; continue; }
                    System.out.println(line);
                }
            }
        }

        @Override
        public void exportExcel(String filepath, List<StepResult> results) throws Exception {
            // Excel 내보내기는 Apache POI 라이브러리를 사용합니다.
            // Maven dependencies (pom.xml):
            //  <dependency>
            //    <groupId>org.apache.poi</groupId>
            //    <artifactId>poi</artifactId>
            //    <version>5.2.3</version>
            //  </dependency>
            //  <dependency>
            //    <groupId>org.apache.poi</groupId>
            //    <artifactId>poi-ooxml</artifactId>
            //    <version>5.2.3</version>
            //  </dependency>
            
            // 구현 예시는 생략하되, 아래와 같은 구조로 작성합니다:
            // Workbook wb = new XSSFWorkbook();
            // Sheet sh = wb.createSheet("results");
            // Row header = sh.createRow(0); header.createCell(0).setCellValue("step"); ...
            // for each result -> createRow and setCellValues
            // try (FileOutputStream fos = new FileOutputStream(filepath)) { wb.write(fos); }

            throw new UnsupportedOperationException("Excel export requires Apache POI dependency. See method comments.");
        }
    }

    /* ----------------------------- 계산 파이프라인 예시 ----------------------------- */
    public static void main(String[] args) throws Exception {
        // 샘플 물성치: E=210 GPa, yield=250 MPa, tensile=450 MPa, H=1 GPa, fracture strain=0.25
        MaterialProperties steel = new MaterialProperties(
                210e9,    // E (Pa)
                250e6,    // yield (Pa)
                450e6,    // tensile (Pa)
                1e9,      // hardening modulus (Pa)
                0.25      // fracture strain (engineering)
        );

        // 샘플 변형 경로: 100 steps, each 0.003 strain increment
        double[] steps = new double[100];
        Arrays.fill(steps, 0.003);
        StrainPath path = new StrainPath(steps, 0.0);

        ICalculationEngine engine = new SimpleIsotropicHardeningEngine();
        List<StepResult> results = engine.compute(steel, path);

        // 저장
        String dbFile = "stress_results.db";
        try (DatabaseManager db = new DatabaseManager(dbFile)) {
            db.saveResults("exp01", results);
            System.out.println("Saved " + results.size() + " results to DB: " + dbFile);
        }

        // CSV export
        IOManager io = new IOManager();
        io.exportCsv("results_exp01.csv", results);
        System.out.println("Exported CSV: results_exp01.csv");

        // Load from DB and print
        try (DatabaseManager db = new DatabaseManager(dbFile)) {
            List<StepResult> loaded = db.loadResults("exp01");
            System.out.println("Loaded from DB: "+loaded.size()+" rows");
        }
    }

}
