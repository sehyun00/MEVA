package meva.calculation;

import meva.models.TestData;
import meva.models.StressStrainPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * 재료 물성값 계산 클래스
 * 
 * @author 이태윤
 */
public class MaterialProperties {

    /**
     * 영률(Young's Modulus) 계산
     * 초기 선형 구간(탄성 구간)의 기울기를 계산
     * 
     * @param points 응력-변형률 데이터 포인트 리스트
     * @return 영률값 (GPa)
     */
    public double calculateYoungsModulus(List<StressStrainPoint> points) {
        if (points == null || points.size() < 10) {
            return 0.0;
        }

        // 1. 탄성 구간 데이터 선택 (변형률 0~0.002 또는 0.2%)
        double maxStrainForElastic = 0.002;
        List<StressStrainPoint> elasticPoints = new ArrayList<>();

        for (StressStrainPoint point : points) {
            if (point.getTrueStrain() <= maxStrainForElastic &&
                    point.getTrueStrain() > 0 &&
                    point.getTrueStress() > 0) {
                elasticPoints.add(point);
            }
        }

        if (elasticPoints.size() < 5) {
            System.out.println("  ⚠️ 변형률 0~0.002 구간 데이터 부족 (" + elasticPoints.size() + "개)");
            // 탄성 구간 데이터가 부족하면 초기 데이터 사용
            elasticPoints.clear();
            int count = Math.min(50, points.size() / 10); // 전체의 10% 또는 최대 50개
            for (int i = 0; i < count && i < points.size(); i++) {
                if (points.get(i).getTrueStress() > 0 &&
                        points.get(i).getTrueStrain() > 0) {
                    elasticPoints.add(points.get(i));
                }
            }
            System.out.println("  → 초기 데이터 사용: " + elasticPoints.size() + "개 포인트");
        } else {
            System.out.println("  ✓ 변형률 0~0.002 구간 사용: " + elasticPoints.size() + "개 포인트");
        }

        if (elasticPoints.size() < 2) {
            System.err.println("  ✗ 영률 계산 불가: 데이터 포인트 부족");
            return 0.0;
        }

        // 2. 선형 회귀를 사용한 기울기 계산 (최소제곱법)
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = elasticPoints.size();

        for (StressStrainPoint point : elasticPoints) {
            double strain = point.getTrueStrain();
            double stress = point.getTrueStress();
            sumX += strain;
            sumY += stress;
            sumXY += strain * stress;
            sumX2 += strain * strain;
        }

        // 기울기 = (n*ΣXY - ΣX*ΣY) / (n*ΣX² - (ΣX)²)
        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < 1e-10) {
            System.err.println("  ✗ 영률 계산 불가: 분모가 0에 가까움");
            return 0.0;
        }

        double slope = (n * sumXY - sumX * sumY) / denominator;

        // MPa에서 GPa로 변환 (1 GPa = 1000 MPa)
        double youngsModulus = slope / 1000.0;

        // 디버그 정보 출력
        System.out.println("  - 변형률 범위: " + String.format("%.6f", elasticPoints.get(0).getTrueStrain()) +
                " ~ " + String.format("%.6f", elasticPoints.get(n - 1).getTrueStrain()));
        System.out.println("  - 응력 범위: " + String.format("%.2f", elasticPoints.get(0).getTrueStress()) +
                " ~ " + String.format("%.2f", elasticPoints.get(n - 1).getTrueStress()) + " MPa");
        System.out.println("  - 기울기: " + String.format("%.2f", slope) + " MPa");
        System.out.println("  - 계산된 영률: " + String.format("%.2f", youngsModulus) + " GPa");

        return youngsModulus;
    }

    /**
     * 항복 강도 계산 (0.2% Offset Method)
     * 
     * @param points        응력-변형률 데이터 포인트 리스트
     * @param youngsModulus 영률 (GPa)
     * @return 항복 강도 (MPa)
     */
    public double calculateYieldStrength(List<StressStrainPoint> points, double youngsModulus) {
        if (points == null || points.isEmpty() || youngsModulus <= 0) {
            return 0.0;
        }

        // 0.2% offset = 0.002
        double offset = 0.002;

        // 영률을 MPa로 변환
        double E_MPa = youngsModulus * 1000.0;

        // 0.2% offset 선: σ = E × (ε - 0.002)
        // 실제 응력-변형률 곡선과 이 선이 만나는 점을 찾기

        double minDifference = Double.MAX_VALUE;
        double yieldStrength = 0.0;

        for (StressStrainPoint point : points) {
            double strain = point.getTrueStrain();
            double stress = point.getTrueStress();

            // offset 이후 구간만 검사
            if (strain > offset) {
                // offset 선의 응력값
                double offsetStress = E_MPa * (strain - offset);

                // 실제 응력과 offset 선 응력의 차이
                double difference = Math.abs(stress - offsetStress);

                // 가장 가까운 점 찾기
                if (difference < minDifference) {
                    minDifference = difference;
                    yieldStrength = stress;
                }
            }
        }

        return yieldStrength;
    }

    /**
     * 연신율(Elongation) 계산
     * 파단 시점의 변형률을 백분율로 변환
     * 
     * @param points 응력-변형률 데이터 포인트 리스트
     * @return 연신율 (%)
     */
    public double calculateElongation(List<StressStrainPoint> points) {
        if (points == null || points.isEmpty()) {
            return 0.0;
        }

        // 마지막 데이터 포인트의 변형률 (파단 시점)
        StressStrainPoint lastPoint = points.get(points.size() - 1);
        double fractureTrueStrain = lastPoint.getTrueStrain();

        // True Strain을 Engineering Strain으로 변환
        // Engineering Strain = e^(True Strain) - 1
        double fractureEngStrain = Math.exp(fractureTrueStrain) - 1;

        // 백분율로 변환
        return fractureEngStrain * 100.0;
    }

    /**
     * 단면 감소율(Reduction of Area) 계산
     * True Strain을 이용한 단면적 변화 계산
     * 
     * @param points 응력-변형률 데이터 포인트 리스트
     * @return 단면 감소율 (%)
     */
    public double calculateReductionOfArea(List<StressStrainPoint> points) {
        if (points == null || points.isEmpty()) {
            return 0.0;
        }

        // 마지막 데이터 포인트 (파단 시점)
        StressStrainPoint lastPoint = points.get(points.size() - 1);
        double fractureTrueStrain = lastPoint.getTrueStrain();

        // True Strain과 단면적 관계식:
        // True Strain = ln(A₀/Af)
        // 따라서 Af/A₀ = e^(-True Strain)
        double areaRatio = Math.exp(-fractureTrueStrain);

        // Reduction of Area = (A₀ - Af)/A₀ × 100
        // = (1 - Af/A₀) × 100
        double reductionOfArea = (1 - areaRatio) * 100.0;

        return reductionOfArea;
    }

    /**
     * 인성(Toughness) 계산
     * 응력-변형률 곡선 아래 전체 면적 (0부터 파단까지)
     * 
     * @param points 응력-변형률 데이터 포인트 리스트
     * @return 인성 (MJ/m³)
     */
    public double calculateToughness(List<StressStrainPoint> points) {
        if (points == null || points.size() < 2) {
            return 0.0;
        }

        // 사다리꼴 공식을 이용한 수치 적분
        double totalArea = 0.0;

        for (int i = 1; i < points.size(); i++) {
            StressStrainPoint p1 = points.get(i - 1);
            StressStrainPoint p2 = points.get(i);

            double strain1 = p1.getTrueStrain();
            double strain2 = p2.getTrueStrain();
            double stress1 = p1.getTrueStress();
            double stress2 = p2.getTrueStress();

            // 사다리꼴 면적: (y1 + y2) / 2 * (x2 - x1)
            double trapezoidArea = (stress1 + stress2) / 2.0 * (strain2 - strain1);
            totalArea += trapezoidArea;
        }

        // MPa는 N/mm² = MN/m²이므로
        // 1 MPa = 1 MJ/m³
        // 따라서 단위 변환 불필요, 그대로 MJ/m³
        return totalArea;
    }

    /**
     * 탄성 에너지(Resilience) 계산
     * 항복점까지의 응력-변형률 곡선 아래 면적
     * 
     * @param points        응력-변형률 데이터 포인트 리스트
     * @param yieldStrength 항복 강도 (MPa)
     * @return 탄성 에너지 (MJ/m³)
     */
    public double calculateResilience(List<StressStrainPoint> points, double yieldStrength) {
        if (points == null || points.size() < 2 || yieldStrength <= 0) {
            return 0.0;
        }

        // 항복점까지의 데이터만 선택
        List<StressStrainPoint> elasticPoints = new ArrayList<>();

        for (StressStrainPoint point : points) {
            if (point.getTrueStress() <= yieldStrength) {
                elasticPoints.add(point);
            } else {
                // 항복 강도를 초과하면 중단
                break;
            }
        }

        if (elasticPoints.size() < 2) {
            return 0.0;
        }

        // 사다리꼴 공식을 이용한 수치 적분
        double totalArea = 0.0;

        for (int i = 1; i < elasticPoints.size(); i++) {
            StressStrainPoint p1 = elasticPoints.get(i - 1);
            StressStrainPoint p2 = elasticPoints.get(i);

            double strain1 = p1.getTrueStrain();
            double strain2 = p2.getTrueStrain();
            double stress1 = p1.getTrueStress();
            double stress2 = p2.getTrueStress();

            double trapezoidArea = (stress1 + stress2) / 2.0 * (strain2 - strain1);
            totalArea += trapezoidArea;
        }

        return totalArea;
    }

    /**
     * 탄성 한계(Elastic Limit) 계산
     * 응력-변형률 곡선이 선형성에서 벗어나기 시작하는 지점
     * 
     * @param points        응력-변형률 데이터 포인트 리스트
     * @param youngsModulus 영률 (GPa)
     * @return 탄성 한계 (MPa)
     */
    public double calculateElasticLimit(List<StressStrainPoint> points, double youngsModulus) {
        if (points == null || points.isEmpty() || youngsModulus <= 0) {
            return 0.0;
        }

        // 영률을 MPa로 변환
        double E_MPa = youngsModulus * 1000.0;

        // 초기 선형 구간 (변형률 0.001 이하)
        double maxStrainForElastic = 0.001;

        // 선형성 허용 오차 (2%)
        double tolerance = 0.02;

        double elasticLimit = 0.0;

        for (StressStrainPoint point : points) {
            double strain = point.getTrueStrain();
            double stress = point.getTrueStress();

            if (strain <= 0 || strain > maxStrainForElastic)
                continue;

            // 예상 응력 (선형 관계)
            double expectedStress = E_MPa * strain;

            // 실제 응력과 예상 응력의 차이
            double deviation = Math.abs(stress - expectedStress) / expectedStress;

            // 허용 오차를 초과하면 탄성 한계
            if (deviation > tolerance) {
                elasticLimit = stress;
                break;
            }
        }

        return elasticLimit;
    }

    /**
     * 비례 한계(Proportional Limit) 계산
     * 응력과 변형률이 비례 관계를 유지하는 최대 응력
     * (일반적으로 Elastic Limit와 유사하거나 약간 낮음)
     * 
     * @param points        응력-변형률 데이터 포인트 리스트
     * @param youngsModulus 영률 (GPa)
     * @return 비례 한계 (MPa)
     */
    public double calculateProportionalLimit(List<StressStrainPoint> points, double youngsModulus) {
        if (points == null || points.isEmpty() || youngsModulus <= 0) {
            return 0.0;
        }

        // 영률을 MPa로 변환
        double E_MPa = youngsModulus * 1000.0;

        // 더 엄격한 선형성 허용 오차 (1%)
        double tolerance = 0.01;

        double proportionalLimit = 0.0;

        for (StressStrainPoint point : points) {
            double strain = point.getTrueStrain();
            double stress = point.getTrueStress();

            if (strain <= 0 || strain > 0.002)
                continue;

            // 예상 응력 (선형 관계)
            double expectedStress = E_MPa * strain;

            // 실제 응력과 예상 응력의 차이
            double deviation = Math.abs(stress - expectedStress) / expectedStress;

            // 허용 오차를 초과하면 비례 한계
            if (deviation > tolerance) {
                proportionalLimit = stress;
                break;
            }
        }

        return proportionalLimit;
    }

    /**
     * 네킹 시작 변형률(Necking Start Strain) 계산
     * 최대 응력(UTS)에 도달한 시점의 변형률
     * 
     * @param points    응력-변형률 데이터 포인트 리스트
     * @param maxStress 최대 응력 (MPa)
     * @return 네킹 시작 변형률
     */
    public double calculateNeckingStartStrain(List<StressStrainPoint> points, double maxStress) {
        if (points == null || points.isEmpty() || maxStress <= 0) {
            return 0.0;
        }

        // 최대 응력 지점의 변형률 찾기
        double neckingStrain = 0.0;
        double minDifference = Double.MAX_VALUE;

        for (StressStrainPoint point : points) {
            double stress = point.getTrueStress();
            double difference = Math.abs(stress - maxStress);

            if (difference < minDifference) {
                minDifference = difference;
                neckingStrain = point.getTrueStrain();
            }
        }

        return neckingStrain;
    }

    /**
     * 파괴 응력(Fracture Stress) 계산
     * 마지막 데이터 포인트의 응력
     * 
     * @param points 응력-변형률 데이터 포인트 리스트
     * @return 파괴 응력 (MPa)
     */
    public double calculateFractureStress(List<StressStrainPoint> points) {
        if (points == null || points.isEmpty()) {
            return 0.0;
        }

        // 마지막 포인트의 응력
        StressStrainPoint lastPoint = points.get(points.size() - 1);
        return lastPoint.getTrueStress();
    }

    /**
     * 파괴 변형률(Fracture Strain) 계산
     * 마지막 데이터 포인트의 변형률
     * 
     * @param points 응력-변형률 데이터 포인트 리스트
     * @return 파괴 변형률
     */
    public double calculateFractureStrain(List<StressStrainPoint> points) {
        if (points == null || points.isEmpty()) {
            return 0.0;
        }

        // 마지막 포인트의 변형률
        StressStrainPoint lastPoint = points.get(points.size() - 1);
        return lastPoint.getTrueStrain();
    }

}
