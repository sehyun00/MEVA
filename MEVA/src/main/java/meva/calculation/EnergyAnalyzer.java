package meva.calculation;

import meva.models.StressStrainPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * 에너지 관련 물성치(인성, 레질리언스) 계산 클래스
 * 적분 및 면적 계산 로직 담당
 * 
 * @author MEVA 개발팀
 * @version 1.0
 */
public class EnergyAnalyzer {

    /**
     * 인성(Toughness) 계산 - Simpson's Rule (정밀 적분)
     * 곡선 아래의 전체 면적을 계산
     */
    public double calculateToughnessSimpson(List<Double> strains, List<Double> stresses, double limitStrain) {
        if (strains == null || stresses == null || strains.size() < 3)
            return 0.0;

        double area = 0.0;
        int n = strains.size();

        for (int i = 0; i < n - 2; i += 2) {
            double h = (strains.get(i + 2) - strains.get(i)) / 2.0;

            // Simpson's 1/3 Rule 조건: 등간격이어야 함
            // 비등간격 데이터의 경우, 구간별 사다리꼴 적분과 Simpson을 혼용하거나 보간법 사용
            // 여기서는 원본 로직을 유지하여 구간별 Simpson 근사 적용

            double s0 = stresses.get(i);
            double s1 = stresses.get(i + 1);
            double s2 = stresses.get(i + 2);

            double e0 = strains.get(i);
            double e2 = strains.get(i + 2);

            if (e0 >= limitStrain)
                break;

            // limitStrain이 현재 구간 내에 있는 경우 처리
            if (e2 > limitStrain) {
                // 남은 구간은 사다리꼴로 처리 (간소화)
                if (strains.get(i + 1) <= limitStrain) {
                    area += calculateTrapezoidalSegment(strains.get(i), s0, strains.get(i + 1), s1, limitStrain);
                    area += calculateTrapezoidalSegment(strains.get(i + 1), s1, e2, s2, limitStrain);
                } else {
                    area += calculateTrapezoidalSegment(strains.get(i), s0, strains.get(i + 1), s1, limitStrain);
                }
                break;
            } else {
                // Simpson's Rule: (h/3) * (y0 + 4y1 + y2)
                area += (h / 3.0) * (s0 + 4 * s1 + s2);
            }
        }

        // 짝수 개라서 마지막 하나 남은 경우 사다리꼴 더하기
        if ((n - 1) % 2 != 0) {
            int last = n - 1;
            double ePrev = strains.get(last - 1);
            double eLast = strains.get(last);
            if (ePrev < limitStrain) {
                area += calculateTrapezoidalSegment(
                        ePrev, stresses.get(last - 1),
                        eLast, stresses.get(last),
                        limitStrain);
            }
        }

        return area;
    }

    /**
     * 단일 사다리꼴 구간 면적 계산 (limit 적용)
     */
    private double calculateTrapezoidalSegment(double e0, double s0, double e1, double s1, double limit) {
        if (e0 >= limit)
            return 0.0;

        double effectiveE1 = Math.min(e1, limit);
        double effectiveS1 = s1;

        if (e1 > limit) {
            // 선형 보간으로 limit 지점의 응력 추정
            effectiveS1 = s0 + (s1 - s0) * (limit - e0) / (e1 - e0);
        }

        return 0.5 * (s0 + effectiveS1) * (effectiveE1 - e0);
    }

    /**
     * 탄성 에너지(Resilience) 계산 (삼각형 근사 - Primary)
     * Hooke's Law 가정: 0.5 * YieldStrength * YieldStrain
     */
    public double calculateResilience(List<StressStrainPoint> points, double yieldStrength) {
        if (yieldStrength <= 0 || points == null || points.isEmpty())
            return 0.0;

        // 항복 변형률 찾기 (데이터에서 yieldStrength에 가장 가까운 점 탐색)
        double yieldStrain = 0.0;

        // 1. 단순 탐색
        for (StressStrainPoint p : points) {
            if (Math.abs(p.getTrueStress() - yieldStrength) < 1.0) { // 오차 1MPa 이내
                yieldStrain = p.getTrueStrain();
                break;
            }
        }

        // 2. 못 찾았으면 최단 거리 탐색
        if (yieldStrain == 0.0) {
            double minDiff = Double.MAX_VALUE;
            for (StressStrainPoint p : points) {
                double diff = Math.abs(p.getTrueStress() - yieldStrength);
                if (diff < minDiff) {
                    minDiff = diff;
                    yieldStrain = p.getTrueStrain();
                }
            }
        }

        return 0.5 * yieldStrength * yieldStrain;
    }

    /**
     * 탄성 에너지(Resilience) 계산 (정밀 적분 - Secondary)
     * 0 ~ 항복점까지의 실제 곡선 적분
     */
    public double calculateResilienceIntegral(List<StressStrainPoint> points, double yieldStrain) {
        if (points == null || points.size() < 2)
            return 0.0;

        double area = 0.0;

        // [Toe Compensation] 초기 미소 구간 보정 (0,0 ~ First Point)
        // 노이즈 필터링 등으로 인해 0부터 시작하지 않는 경우, 원점부터의 탄성 에너지를 보존함
        StressStrainPoint p0 = points.get(0);
        double e0 = p0.getTrueStrain();
        double s0 = p0.getTrueStress();

        if (e0 > 0.0001) { // 0이 아닌 경우에만 보정
            area += 0.5 * e0 * s0;
        }

        List<Double> epsilons = new ArrayList<>();
        List<Double> sigmas = new ArrayList<>();
        for (StressStrainPoint p : points) {
            epsilons.add(p.getTrueStrain());
            sigmas.add(p.getTrueStress());
        }

        area += calculateToughnessSimpson(epsilons, sigmas, yieldStrain);

        return area;
    }
}
