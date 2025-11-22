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

    /**
     * 음수 응력 데이터 제거
     * 시험 초기 압축 구간 제거
     * 
     * @param data 원본 데이터
     * @return 음수 응력이 제거된 데이터
     */
    public List<StressStrainPoint> removeNegativeStress(List<StressStrainPoint> data) {
        if (data == null || data.isEmpty()) {
            return data;
        }

        List<StressStrainPoint> filtered = new ArrayList<>();

        for (StressStrainPoint point : data) {
            // True Stress와 Engineering Stress 둘 다 양수인 것만
            if (point.getTrueStress() > 0 && point.getEngineeringStress() > 0) {
                filtered.add(point);
            }
        }

        System.out.println("음수 응력 제거: " + (data.size() - filtered.size()) + "개 포인트 제거됨");

        return filtered;
    }

    /**
     * 파단 후 데이터 제거
     * 최대 응력 이후 급격히 감소하는 구간 제거
     * 
     * @param data          원본 데이터
     * @param dropThreshold 응력 감소 임계값 (예: 0.5 = 50% 감소)
     * @return 파단 후 데이터가 제거된 데이터
     */
    public List<StressStrainPoint> removePostFractureData(
            List<StressStrainPoint> data, double dropThreshold) {

        if (data == null || data.isEmpty()) {
            return data;
        }

        // 1. 최대 응력 찾기
        double maxStress = findMaxStress(data);
        int maxStressIndex = -1;

        for (int i = 0; i < data.size(); i++) {
            if (Math.abs(data.get(i).getTrueStress() - maxStress) < 0.01) {
                maxStressIndex = i;
                break;
            }
        }

        if (maxStressIndex == -1) {
            return data;
        }

        // 2. 최대 응력 이후 급격한 감소 지점 찾기
        List<StressStrainPoint> filtered = new ArrayList<>();
        double thresholdStress = maxStress * dropThreshold;

        for (int i = 0; i <= maxStressIndex; i++) {
            filtered.add(data.get(i));
        }

        // 최대 응력 이후는 임계값 이상인 것만 추가
        for (int i = maxStressIndex + 1; i < data.size(); i++) {
            double currentStress = data.get(i).getTrueStress();

            // 임계값 이상이면 추가
            if (currentStress >= thresholdStress) {
                filtered.add(data.get(i));
            } else {
                // 한 번 임계값 아래로 떨어지면 종료
                System.out.println("파단 후 데이터 제거: " + (data.size() - i) + "개 포인트 제거됨");
                break;
            }
        }

        return filtered;
    }

    /**
     * 포괄적인 데이터 클리닝
     * 음수 응력 제거 + 파단 후 데이터 제거
     * 
     * @param data 원본 데이터
     * @return 클리닝된 데이터
     */
    public List<StressStrainPoint> cleanData(List<StressStrainPoint> data) {
        if (data == null || data.isEmpty()) {
            return data;
        }

        System.out.println("=== 데이터 클리닝 시작 ===");
        System.out.println("원본 데이터 포인트: " + data.size());

        // 1. 음수 응력 제거
        List<StressStrainPoint> cleaned = removeNegativeStress(data);
        System.out.println("음수 제거 후: " + cleaned.size() + "개");

        // 2. 파단 후 데이터 제거 (최대 응력의 50% 이하로 떨어지면 제거)
        cleaned = removePostFractureData(cleaned, 0.5);
        System.out.println("파단 후 제거 후: " + cleaned.size() + "개");

        System.out.println("=== 데이터 클리닝 완료 ===");
        System.out.println("총 " + (data.size() - cleaned.size()) + "개 포인트 제거됨\n");

        return cleaned;
    }

    /**
     * 더 정교한 파단 검출 (선택사항)
     * 연속된 감소를 기준으로 파단 지점 탐지
     * 
     * @param data             원본 데이터
     * @param consecutiveDrops 연속 감소 횟수 (예: 10)
     * @return 파단 전 데이터
     */
    public List<StressStrainPoint> removePostFractureAdvanced(
            List<StressStrainPoint> data, int consecutiveDrops) {

        if (data == null || data.isEmpty() || data.size() < consecutiveDrops) {
            return data;
        }

        // 최대 응력 찾기
        double maxStress = findMaxStress(data);
        int maxStressIndex = -1;

        for (int i = 0; i < data.size(); i++) {
            if (Math.abs(data.get(i).getTrueStress() - maxStress) < 0.01) {
                maxStressIndex = i;
                break;
            }
        }

        if (maxStressIndex == -1 || maxStressIndex >= data.size() - consecutiveDrops) {
            return data;
        }

        // 최대 응력 이후 연속 감소 감지
        int dropCount = 0;
        int fractureIndex = data.size();

        for (int i = maxStressIndex + 1; i < data.size(); i++) {
            double prevStress = data.get(i - 1).getTrueStress();
            double currStress = data.get(i).getTrueStress();

            if (currStress < prevStress) {
                dropCount++;
                if (dropCount >= consecutiveDrops) {
                    fractureIndex = i - consecutiveDrops + 1;
                    System.out.println("파단 지점 감지 (index: " + fractureIndex + ")");
                    break;
                }
            } else {
                dropCount = 0; // 리셋
            }
        }

        List<StressStrainPoint> filtered = new ArrayList<>();
        for (int i = 0; i < fractureIndex; i++) {
            filtered.add(data.get(i));
        }

        System.out.println("고급 파단 제거: " + (data.size() - filtered.size()) + "개 포인트 제거됨");

        return filtered;
    }

}
