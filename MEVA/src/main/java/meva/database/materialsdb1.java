package materialsengineering;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MaterialDBValidator
 *
 * - standards.csv (표준 DB) 를 로드
 * - experiments.csv (대용량, 정렬된: experiment_id 순서) 를 스트리밍 처리
 * - 각 experiment에서 Young's modulus, yield, tensile 계산
 * - 표준값과 비교하여 오차 계산 및 PASS/FAIL 판정
 *
 * CSV 포맷 예시:
 *
 * standards.csv:
 * material_id,material_name,young_modulus_gpa,yield_strength_mpa,tensile_strength_mpa,density_kgm3,poisson
 * M001,AISI304,193,215,505,8000,0.29
 *
 * experiments.csv:
 * experiment_id,material_name,load_n,delta_mm,gage_length_mm,area_mm2
 * EXP001,AISI304,1000,0.05,50,19.6
 * EXP001,AISI304,2000,0.10,50,19.6
 * ...
 * (동일 experiment_id의 여러 row는 연속으로 정렬되어 있어야 함)
 */
public class materialsdb1 {

    // ========== 설정 ==========
    private static final double DEFAULT_TOLERANCE_PERCENT = 5.0; // 허용 오차(%)
    private static final int MIN_POINTS_FOR_REGRESSION = 5; // 최소 선형회귀에 사용할 점 수
    private static final double MAX_LINEAR_STRAIN = 0.02; // 선형 구간 최대 strain (예: 2%)

    // ========== 데이터 구조 ==========
    static class MaterialStandard {
        String id;
        String name;
        double youngGPa;
        double yieldMPa;
        double tensileMPa;
        double density;
        double poisson;

        static MaterialStandard fromCsv(String[] cols) {
            MaterialStandard m = new MaterialStandard();
            m.id = cols[0];
            m.name = cols[1];
            m.youngGPa = Double.parseDouble(cols[2]);
            m.yieldMPa = Double.parseDouble(cols[3]);
            m.tensileMPa = Double.parseDouble(cols[4]);
            m.density = Double.parseDouble(cols[5]);
            m.poisson = Double.parseDouble(cols[6]);
            return m;
        }
    }

    static class ComputedProperties {
        double youngGPa;
        double yieldMPa;
        double tensileMPa;
    }

    // ========== 메모리 모니터 ==========
    static class MemoryMonitor {
        static long usedMB() {
            Runtime r = Runtime.getRuntime();
            return (r.totalMemory() - r.freeMemory()) / (1024 * 1024);
        }
        static void print(String tag) {
            System.out.printf("[%s] Used Memory: %d MB%n", tag, usedMB());
        }
    }

    // ========== CSV 로드 (standards) ==========
    static Map<String, MaterialStandard> loadStandards(File csvFile) throws IOException {
        Map<String, MaterialStandard> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String header = br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] cols = line.split(",", -1);
                // 기대: id,name,youngGPa,yieldMPa,tensileMPa,density,poisson
                if (cols.length < 7) {
                    System.err.println("Invalid standards line: " + line);
                    continue;
                }
                MaterialStandard m = MaterialStandard.fromCsv(cols);
                map.put(m.name.trim().toLowerCase(), m); // name 기반 매핑(소문자)
            }
        }
        return map;
    }

    // ========== 선형회귀 (slope) ==========
    // inputs: stress[], strain[], n
    static double linearRegressionSlope(double[] x, double[] y, int n) {
        // slope = covariance(x,y) / variance(x)
        double sumX = 0.0, sumY = 0.0;
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
        }
        double meanX = sumX / n;
        double meanY = sumY / n;
        double num = 0.0, den = 0.0;
        for (int i = 0; i < n; i++) {
            double dx = x[i] - meanX;
            num += dx * (y[i] - meanY);
            den += dx * dx;
        }
        if (den == 0.0) return 0.0;
        return num / den;
    }

    // ========== 실험 그룹 처리 ==========
    static ComputedProperties computeFromGroup(List<Double> loadsN,
                                               List<Double> deltaMm,
                                               List<Double> gageLengthMm,
                                               List<Double> areaMm2) {
        int n = loadsN.size();
        if (n == 0) return null;

        // 우리는 각 row에서 stress(MPa) = (load [N]) / (area [mm^2])  => N/mm^2 == MPa
        // strain = delta (mm) / gage_length(mm) (무단위)
        // collect stress/strain arrays for this experiment
        double[] stress = new double[n];
        double[] strain = new double[n];
        double tensileMax = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < n; i++) {
            double a = areaMm2.get(i);
            if (a <= 0) a = 1e-9; // 안전장치
            stress[i] = loadsN.get(i) / a; // MPa
            strain[i] = gageLengthMm.get(i) == 0 ? 0.0 : (deltaMm.get(i) / gageLengthMm.get(i));
            if (stress[i] > tensileMax) tensileMax = stress[i];
        }

        // 선형 회귀에 사용할 점들 선택:
        // - strain이 MAX_LINEAR_STRAIN보다 작은 점들 또는 처음 MIN_POINTS_FOR_REGRESSION 점들
        int useCount = 0;
        for (int i = 0; i < n && strain[i] <= MAX_LINEAR_STRAIN; i++) useCount++;
        if (useCount < MIN_POINTS_FOR_REGRESSION) {
            useCount = Math.min(n, MIN_POINTS_FOR_REGRESSION);
        }

        // prepare arrays for regression: x = strain[0..useCount-1], y = stress[...]
        double[] x = new double[useCount];
        double[] y = new double[useCount];
        for (int i = 0; i < useCount; i++) {
            x[i] = strain[i];
            y[i] = stress[i];
        }

        double slope = linearRegressionSlope(x, y, useCount); // slope in MPa per unit strain
        // Young's modulus in GPa
        double youngGPa = slope / 1000.0; // MPa / 1000 = GPa

        // Yield strength: approximate 0.2% offset -> stress = slope * 0.002
        double yieldApprox = slope * 0.002; // MPa

        ComputedProperties cp = new ComputedProperties();
        cp.youngGPa = youngGPa;
        cp.yieldMPa = yieldApprox;
        cp.tensileMPa = tensileMax;

        return cp;
    }

    // ========== 검증 비교 ==========
    static double percentError(double calc, double standard) {
        if (Double.isNaN(calc) || Double.isNaN(standard) || standard == 0.0) return Double.NaN;
        return Math.abs((calc - standard) / standard) * 100.0;
    }

    static void validateAndPrint(String experimentId, String materialName,
                                 ComputedProperties cp, MaterialStandard std,
                                 double tolerancePercent, BufferedWriter reportWriter) throws IOException {

        if (std == null) {
            String msg = String.format("%s | %s : STANDARD NOT FOUND%n", experimentId, materialName);
            System.out.print(msg);
            if (reportWriter != null) reportWriter.write(msg);
            return;
        }

        // compare
        double errYoung = percentError(cp.youngGPa, std.youngGPa);
        double errYield = percentError(cp.yieldMPa, std.yieldMPa);
        double errTensile = percentError(cp.tensileMPa, std.tensileMPa);

        String resYoung = (errYoung <= tolerancePercent) ? "PASS" : "FAIL";
        String resYield = (errYield <= tolerancePercent) ? "PASS" : "FAIL";
        String resTensile = (errTensile <= tolerancePercent) ? "PASS" : "FAIL";

        String out = String.format(Locale.US,
                "%s | %s | Young: calc=%.3f GPa ref=%.3f GPa err=%.2f%% [%s] | " +
                        "Yield: calc=%.3f MPa ref=%.3f MPa err=%.2f%% [%s] | " +
                        "Tensile: calc=%.3f MPa ref=%.3f MPa err=%.2f%% [%s]%n",
                experimentId, materialName,
                cp.youngGPa, std.youngGPa, errYoung, resYoung,
                cp.yieldMPa, std.yieldMPa, errYield, resYield,
                cp.tensileMPa, std.tensileMPa, errTensile, resTensile
        );

        System.out.print(out);
        if (reportWriter != null) {
            reportWriter.write(out);
        }
    }

    // ========== 주 처리기: experiments.csv 스트리밍 ==========
    static void processExperiments(File experimentsCsv,
                                   Map<String, MaterialStandard> standards,
                                   double tolerancePercent,
                                   File reportFile) throws IOException {

        try (BufferedReader br = new BufferedReader(new FileReader(experimentsCsv));
             BufferedWriter reportWriter = reportFile == null ? null : new BufferedWriter(new FileWriter(reportFile))) {

            String header = br.readLine(); // skip header
            String line;
            String currentExpId = null;
            String currentMaterialName = null;

            // temporary per-experiment buffers (역시 메모리 최소화: 한 experiment 당 데이터만 보유)
            List<Double> loadsN = new ArrayList<>();
            List<Double> deltaMm = new ArrayList<>();
            List<Double> gageLenMm = new ArrayList<>();
            List<Double> areaMm2 = new ArrayList<>();

            long processedExperiments = 0;
            AtomicLong processedRows = new AtomicLong(0);

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                processedRows.incrementAndGet();
                String[] cols = line.split(",", -1);
                // 예상: experiment_id,material_name,load_n,delta_mm,gage_length_mm,area_mm2
                if (cols.length < 6) {
                    System.err.println("Invalid experiment line: " + line);
                    continue;
                }
                String expId = cols[0].trim();
                String matName = cols[1].trim();
                double load = Double.parseDouble(cols[2].trim());
                double delta = Double.parseDouble(cols[3].trim());
                double gage = Double.parseDouble(cols[4].trim());
                double area = Double.parseDouble(cols[5].trim());

                // 새로운 experiment 시작: 이전 것을 처리
                if (!expId.equals(currentExpId) && currentExpId != null) {
                    // process previous
                    ComputedProperties cp = computeFromGroup(loadsN, deltaMm, gageLenMm, areaMm2);

                    MemoryMonitor.print("Before Validate");
                    MaterialStandard std = standards.get(currentMaterialName.toLowerCase());
                    validateAndPrint(currentExpId, currentMaterialName, cp, std, tolerancePercent, reportWriter);
                    MemoryMonitor.print("After Validate");

                    // clear buffers
                    loadsN.clear();
                    deltaMm.clear();
                    gageLenMm.clear();
                    areaMm2.clear();

                    processedExperiments++;
                    if (processedExperiments % 100 == 0) {
                        System.out.printf("Processed experiments: %d, processed rows: %d%n", processedExperiments, processedRows.get());
                        MemoryMonitor.print("Progress");
                    }
                }

                // append current row
                currentExpId = expId;
                currentMaterialName = matName;
                loadsN.add(load);
                deltaMm.add(delta);
                gageLenMm.add(gage);
                areaMm2.add(area);
            }

            // 마지막 그룹 처리
            if (currentExpId != null && !loadsN.isEmpty()) {
                ComputedProperties cp = computeFromGroup(loadsN, deltaMm, gageLenMm, areaMm2);
                MaterialStandard std = standards.get(currentMaterialName.toLowerCase());
                validateAndPrint(currentExpId, currentMaterialName, cp, std, tolerancePercent, reportWriter);
                processedExperiments++;
            }

            System.out.printf("=== DONE: total experiments processed: %d, total rows: %d ===%n",
                    processedExperiments, processedRows.get());
            if (reportWriter != null) {
                reportWriter.flush();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Working directory: " + new java.io.File(".").getAbsolutePath());

        if (args.length < 2) {
            System.err.println("Usage: java MaterialDBValidator <standards.csv> <experiments.csv> [report.txt]");
            System.err.println("Example: java MaterialDBValidator standards.csv experiments.csv report.txt");
            System.exit(1);
        }

        File standardsFile = new File(args[0]);
        File experimentsFile = new File(args[1]);
        File reportFile = args.length >= 3 ? new File(args[2]) : null;

        System.out.println("Loading standards...");
        MemoryMonitor.print("Start");
        Map<String, MaterialStandard> standards = loadStandards(standardsFile);
        System.out.printf("Standards loaded: %d materials%n", standards.size());
        MemoryMonitor.print("After loadStandards");

        System.out.println("Processing experiments...");
        processExperiments(experimentsFile, standards, DEFAULT_TOLERANCE_PERCENT, reportFile);

        MemoryMonitor.print("End");
        System.out.println("Report file: " + (reportFile == null ? "none" : reportFile.getAbsolutePath()));
    }
}
