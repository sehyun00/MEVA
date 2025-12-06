package meva.calculation;

import meva.models.StressStrainPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * 탄성 영역 분석 및 영률(Young's Modulus) 계산 클래스
 * 
 * @author MEVA 개발팀
 * @version 1.0
 */
public class ElasticAnalyzer {

    /**
     * 영률(Young's Modulus)과 Y절편(Intercept)을 함께 계산하여 반환 (True Stress 기준)
     */
    public double[] calculateYoungsModulusWithIntercept(List<StressStrainPoint> points) {
        return calculateYoungsModulusWithIntercept(points, false);
    }

    /**
     * 영률(Young's Modulus)과 Y절편(Intercept)을 함께 계산하여 반환 (모드 선택 가능)
     */
    public double[] calculateYoungsModulusWithIntercept(List<StressStrainPoint> points, boolean useEngineering) {
        if (points == null || points.size() < 20)
            return new double[] { 0.0, 0.0 };

        // 1. UTS(최대 강도) 찾기 (범위 설정을 위한 기준값)
        double maxStress = 0.0;
        for (StressStrainPoint p : points) {
            double val = useEngineering ? p.getEngineeringStress() : p.getTrueStress();
            if (val > maxStress)
                maxStress = val;
        }

        // 2. 탐색 범위 설정: 응력의 10% ~ 40% 구간
        double lowerBound = maxStress * 0.10;
        double upperBound = maxStress * 0.40;

        List<StressStrainPoint> candidateRegion = new ArrayList<>();
        for (StressStrainPoint p : points) {
            double stress = useEngineering ? p.getEngineeringStress() : p.getTrueStress();
            if (stress >= lowerBound && stress <= upperBound) {
                candidateRegion.add(p);
            }
            if (stress > upperBound * 1.5)
                break;
        }

        // 데이터 부족 시 재시도 (0~50%)
        if (candidateRegion.size() < 10) {
            candidateRegion.clear();
            lowerBound = 0.0;
            upperBound = maxStress * 0.50;
            for (StressStrainPoint p : points) {
                double stress = useEngineering ? p.getEngineeringStress() : p.getTrueStress();
                if (stress >= lowerBound && stress <= upperBound) {
                    candidateRegion.add(p);
                }
                if (stress > upperBound * 1.5)
                    break;
            }
        }

        if (candidateRegion.size() < 5)
            return new double[] { 0.0, 0.0 };

        // 3. 슬라이딩 윈도우 설정
        int windowSize = Math.min(20, candidateRegion.size() / 2);
        if (windowSize < 5)
            windowSize = 5;

        double maxSlope = 0.0;
        double bestIntercept = 0.0; // 최적 기울기일 때의 절편

        double fallbackSlope = 0.0;
        double fallbackIntercept = 0.0;
        double maxR2 = -1.0;

        for (int i = 0; i <= candidateRegion.size() - windowSize; i += 1) {
            List<StressStrainPoint> subset = candidateRegion.subList(i, i + windowSize);
            double[] reg = calculateLinearRegressionWithIntercept(subset, useEngineering); // [0]: slope, [1]:
                                                                                           // intercept, [2]: r2
            double slope = reg[0];
            double intercept = reg[1];
            double r2 = reg[2];

            if (slope <= 0)
                continue;

            // 거시적 판단 로직
            if (r2 > 0.980) {
                if (slope > maxSlope) {
                    maxSlope = slope;
                    bestIntercept = intercept;
                }
            }

            if (r2 > maxR2) {
                maxR2 = r2;
                fallbackSlope = slope;
                fallbackIntercept = intercept;
            }
        }

        // 4. 결과 반환 (MPa -> GPa)
        if (maxSlope > 0) {
            return new double[] { maxSlope / 1000.0, bestIntercept };
        }

        // 기준 완화 재탐색
        if (maxR2 > 0.98 && maxSlope == 0.0) {
            for (int i = 0; i <= candidateRegion.size() - windowSize; i += 1) {
                List<StressStrainPoint> subset = candidateRegion.subList(i, i + windowSize);
                double[] reg = calculateLinearRegressionWithIntercept(subset, useEngineering);
                if (reg[2] > 0.95 && reg[0] > maxSlope) {
                    maxSlope = reg[0];
                    bestIntercept = reg[1];
                }
            }
            if (maxSlope > 0)
                return new double[] { maxSlope / 1000.0, bestIntercept };
        }

        return new double[] { fallbackSlope / 1000.0, fallbackIntercept };
    }

    /**
     * 선형 회귀 계산 (Intercept 포함, 모드 선택 가능)
     * 
     * @return double[] {Slope, Intercept, R²}
     */
    public double[] calculateLinearRegressionWithIntercept(List<StressStrainPoint> points, boolean useEngineering) {
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = points.size();

        for (StressStrainPoint p : points) {
            double x = useEngineering ? p.getEngineeringStrain() : p.getTrueStrain();
            double y = useEngineering ? p.getEngineeringStress() : p.getTrueStress();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0)
            return new double[] { 0, 0, 0 };

        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;

        double ssTot = 0, ssRes = 0;
        double yMean = sumY / n;

        for (StressStrainPoint p : points) {
            double x = useEngineering ? p.getEngineeringStrain() : p.getTrueStrain();
            double y = useEngineering ? p.getEngineeringStress() : p.getTrueStress();
            double yPred = slope * x + intercept;
            ssTot += Math.pow(y - yMean, 2);
            ssRes += Math.pow(y - yPred, 2);
        }

        double r2 = (ssTot == 0) ? 0 : (1 - (ssRes / ssTot));
        return new double[] { slope, intercept, r2 };
    }
}
