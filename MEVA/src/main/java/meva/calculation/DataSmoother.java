package meva.calculation;

import meva.models.StressStrainPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * 데이터 노이즈 제거 및 스무딩 로직 담당 클래스
 * 
 * @author MEVA 개발팀
 * @version 1.0
 */
public class DataSmoother {

    /**
     * 데이터 노이즈 제거를 위한 이동 평균 필터 (Strain-based Adaptive Smoothing)
     * 공학적 타당성: 0.2% Offset 지점을 왜곡하지 않도록, 윈도우 크기는 그 절반인 0.1% Strain 이하로 제한
     * 
     * @param points 원본 데이터
     * @return 노이즈가 제거된 새로운 데이터 리스트
     */
    public List<StressStrainPoint> applySmoothing(List<StressStrainPoint> points) {
        if (points == null || points.size() < 10)
            return points;

        List<StressStrainPoint> smoothed = new ArrayList<>();

        // 적응형 윈도우 크기 계산: Strain 0.001 (0.1%) 구간에 해당하는 데이터 개수 찾기
        // 초반 탄성 구간(데이터 밀도가 높은 곳)을 기준으로 샘플링
        int sampleLimit = Math.min(points.size(), 1000);
        double strainSum = 0;
        for (int i = 1; i < sampleLimit; i++) {
            strainSum += (points.get(i).getTrueStrain() - points.get(i - 1).getTrueStrain());
        }
        double avgStrainStep = strainSum / (sampleLimit - 1);

        // 0.001 Strain 구간에 들어가는 포인트 개수 (최소 1개 ~ 최대 20개 제한)
        int windowHalfSize = (int) (0.0005 / avgStrainStep); // 앞뒤로 0.05%씩, 총 0.1%
        if (windowHalfSize < 1)
            windowHalfSize = 1; // 스무딩 불필요
        if (windowHalfSize > 20)
            windowHalfSize = 20; // 너무 많이 뭉개지 않도록 제한

        // 윈도우가 너무 작으면 원본 반환 (스무딩 의미 없음)
        if (windowHalfSize <= 1)
            return points;

        System.out.println("  ✓ 적응형 스무딩 적용 (Window Half-Size: " + windowHalfSize + ")");

        for (int i = 0; i < points.size(); i++) {
            double sumTrueStress = 0;
            double sumEngStress = 0;

            int start = Math.max(0, i - windowHalfSize);
            int end = Math.min(points.size() - 1, i + windowHalfSize);
            int count = 0;

            for (int j = start; j <= end; j++) {
                sumTrueStress += points.get(j).getTrueStress();
                sumEngStress += points.get(j).getEngineeringStress();
                count++;
            }

            StressStrainPoint p = points.get(i);

            // 공칭 응력과 진응력을 각각 독립적으로 스무딩
            // (원본 데이터의 경향성을 유지하기 위함)
            double newTrueStress = sumTrueStress / count;
            double newEngStress = sumEngStress / count;

            StressStrainPoint newPoint = new StressStrainPoint(
                    newEngStress,
                    p.getEngineeringStrain(),
                    newTrueStress,
                    p.getTrueStrain());
            smoothed.add(newPoint);
        }
        return smoothed;
    }

    /**
     * 주변 3개 점의 평균 응력 반환 (간이 스무딩 - Helper)
     */
    public double getAverageStress(List<StressStrainPoint> points, int index) {
        if (index < 1 || index >= points.size() - 1)
            return points.get(index).getTrueStress();

        double sum = 0;
        int count = 0;
        for (int i = index - 1; i <= index + 1; i++) {
            sum += points.get(i).getTrueStress();
            count++;
        }
        return sum / count;
    }
}
