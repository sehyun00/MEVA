package first;

import java.util.*;
import java.text.DecimalFormat;

public class materialsengineering {

    static class MaterialData {
        double[] load;
        double[] displacement;
        double area;
        double length;
    }

    static class MaterialProperties {
        double[] stress;
        double[] strain;
        double youngsModulus;
        double yieldStrength;
        double tensileStrength;
    }

    public static MaterialProperties calculate(MaterialData data) {
        int n = data.load.length;
        double[] stress = new double[n];
        double[] strain = new double[n];

        for (int i = 0; i < n; i++) {
            stress[i] = data.load[i] / data.area;
            strain[i] = data.displacement[i] / data.length;
        }

        MaterialProperties result = new MaterialProperties();
        result.stress = stress;
        result.strain = strain;

        // ① 영률 (E)
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        int count = 0;
        for (int i = 0; i < n; i++) {
            if (strain[i] <= 0.002) {
                sumX += strain[i];
                sumY += stress[i];
                sumXY += strain[i] * stress[i];
                sumXX += strain[i] * strain[i];
                count++;
            }
        }
        result.youngsModulus = (count > 1)
                ? (count * sumXY - sumX * sumY) / (count * sumXX - sumX * sumX)
                : 0;

        // ② 인장강도 (UTS)
        result.tensileStrength = Arrays.stream(stress).max().orElse(0);

        // ③ 항복강도 (0.2% offset)
        double offset = 0.002;
        double yield = 0;
        for (int i = 0; i < n; i++) {
            double offsetStress = result.youngsModulus * (strain[i] - offset);
            if (offsetStress <= stress[i]) {
                yield = stress[i];
                break;
            }
        }
        result.yieldStrength = yield;

        return result;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        DecimalFormat df = new DecimalFormat("0.000");

        MaterialData data = new MaterialData();

        System.out.print("단면적(mm^2): ");
        data.area = sc.nextDouble();
        System.out.print("초기 길이(mm): ");
        data.length = sc.nextDouble();
        sc.nextLine();

        System.out.print("하중 데이터 입력 (쉼표로 구분): ");
        String loadInput = sc.nextLine();
        data.load = Arrays.stream(loadInput.split(","))
                .map(String::trim)
                .mapToDouble(Double::parseDouble)
                .toArray();

        System.out.print("변위 데이터 입력 (쉼표로 구분): ");
        String dispInput = sc.nextLine();
        data.displacement = Arrays.stream(dispInput.split(","))
                .map(String::trim)
                .mapToDouble(Double::parseDouble)
                .toArray();

        MaterialProperties result = calculate(data);

        // 🧾 출력 시작
        System.out.println("\n====================================");
        System.out.println("[재료 시험 결과 보고서]\n");
        System.out.println("입력 데이터 요약:");
        System.out.printf("- 단면적 (A): %.2f mm²%n", data.area);
        System.out.printf("- 초기 길이 (L0): %.2f mm%n", data.length);
        System.out.printf("- 데이터 점 개수: %d%n", data.load.length);

        System.out.println("\n------------------------------------");
        System.out.println("[자동 계산된 주요 물성값]");

        System.out.println("\n1. 영률 (Young's Modulus, E)");
        System.out.println("   → 응력–변형률 곡선의 초기 선형 구간(ε ≤ 0.002)에서 계산");
        System.out.println("   → E = Δσ / Δε");
        System.out.printf("   → 결과: %.3f MPa%n", result.youngsModulus);

        System.out.println("\n2. 항복강도 (Yield Strength, σy)");
        System.out.println("   → 0.2% Offset Method 적용: E(ε - 0.002) = σ 조건 만족점");
        System.out.printf("   → 결과: %.3f MPa%n", result.yieldStrength);

        System.out.println("\n3. 인장강도 (Ultimate Tensile Strength, σuts)");
        System.out.println("   → 전체 응력 데이터 중 최대값");
        System.out.printf("   → 결과: %.3f MPa%n", result.tensileStrength);

        System.out.println("\n------------------------------------");
        System.out.println("[응력–변형률 주요 데이터]");
        System.out.println("  ε(%)       σ(MPa)");
        for (int i = 0; i < result.stress.length; i++) {
            System.out.printf("  %-10s %-10s%n",
                    df.format(result.strain[i]),
                    df.format(result.stress[i]));
        }

        System.out.println("\n------------------------------------");
        System.out.println("계산 완료: 모든 주요 기계적 물성이 자동 계산되었습니다.");
        System.out.println("====================================");
    }
}
