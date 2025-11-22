package meva.calculation;

import meva.models.DataPoint;
import meva.models.StressStrainPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * 응력-변형률 계산을 수행하는 클래스
 * 파일에서 읽은 데이터를 기반으로 추가 계산 수행 (필요시)
 * 
 * @author MEVA 개발팀
 * @version 1.0
 */
public class StressStrainCalculator {

    /**
     * DataPoint 리스트를 StressStrainPoint 리스트로 변환
     * (파일에 이미 True Stress, True Strain이 있으므로 단순 변환)
     * 
     * @param dataPoints 원시 데이터 포인트 리스트
     * @return 응력-변형률 포인트 리스트
     */
    public List<StressStrainPoint> convertToStressStrain(List<DataPoint> dataPoints) {
        List<StressStrainPoint> result = new ArrayList<>();

        for (DataPoint point : dataPoints) {
            StressStrainPoint ssPoint = new StressStrainPoint(
                    point.getEStress(),
                    point.getEStrain(),
                    point.getTStress(),
                    point.getTStrain());
            result.add(ssPoint);
        }

        return result;
    }

    /**
     * 최대 응력 찾기
     * 
     * @param points 응력-변형률 포인트 리스트
     * @return 최대 True Stress 값
     */
    public double findMaxStress(List<StressStrainPoint> points) {
        return points.stream()
                .mapToDouble(StressStrainPoint::getTrueStress)
                .max()
                .orElse(0.0);
    }

    /**
     * 최대 응력에서의 변형률 찾기
     * 
     * @param points 응력-변형률 포인트 리스트
     * @return 최대 응력에서의 True Strain 값
     */
    public double findStrainAtMaxStress(List<StressStrainPoint> points) {
        double maxStress = findMaxStress(points);
        return points.stream()
                .filter(p -> Math.abs(p.getTrueStress() - maxStress) < 0.01)
                .mapToDouble(StressStrainPoint::getTrueStrain)
                .findFirst()
                .orElse(0.0);
    }

    /**
     * 이동 평균 필터를 사용하여 데이터 스무딩
     * 
     * @param data       원본 데이터
     * @param windowSize 윈도우 크기 (홀수 권장, 예: 5, 11, 21)
     * @return 스무딩된 데이터
     */
    public List<StressStrainPoint> smoothData(List<StressStrainPoint> data, int windowSize) {
        if (data == null || data.isEmpty() || windowSize <= 1) {
            return data;
        }

        List<StressStrainPoint> smoothed = new ArrayList<>();
        int halfWindow = windowSize / 2;

        for (int i = 0; i < data.size(); i++) {
            double sumEStress = 0, sumEStrain = 0;
            double sumTStress = 0, sumTStrain = 0;
            int count = 0;

            // 윈도우 범위 설정
            int start = Math.max(0, i - halfWindow);
            int end = Math.min(data.size(), i + halfWindow + 1);

            // 평균 계산
            for (int j = start; j < end; j++) {
                StressStrainPoint p = data.get(j);
                sumEStress += p.getEngineeringStress();
                sumEStrain += p.getEngineeringStrain();
                sumTStress += p.getTrueStress();
                sumTStrain += p.getTrueStrain();
                count++;
            }

            // 평균값으로 새 포인트 생성
            smoothed.add(new StressStrainPoint(
                    sumEStress / count,
                    sumEStrain / count,
                    sumTStress / count,
                    sumTStrain / count));
        }

        return smoothed;
    }

    /**
     * 데이터 다운샘플링 (N개마다 1개 선택)
     * 
     * @param data   원본 데이터
     * @param factor 샘플링 비율 (예: 10이면 10개마다 1개)
     * @return 다운샘플링된 데이터
     */
    public List<StressStrainPoint> downsample(List<StressStrainPoint> data, int factor) {
        if (data == null || data.isEmpty() || factor <= 1) {
            return data;
        }

        List<StressStrainPoint> result = new ArrayList<>();

        for (int i = 0; i < data.size(); i += factor) {
            result.add(data.get(i));
        }

        return result;
    }

}
