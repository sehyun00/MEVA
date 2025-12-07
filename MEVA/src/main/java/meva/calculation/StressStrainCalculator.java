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
 * @version 1.1 (Enhanced)
 */
public class StressStrainCalculator {

    /**
     * DataPoint 리스트를 StressStrainPoint 리스트로 변환
     * (파일에 이미 True Stress, True Strain이 있는 경우 단순 변환)
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
     * [New] 원본 데이터(하중, 변위)와 수동 입력 치수(단면적, 표점거리)를 사용하여 스트레스/변형률 재계산
     * (보정 계수 1.0 사용)
     */
    public List<StressStrainPoint> calculateFromRawData(List<DataPoint> dataPoints, double initialArea,
            double gaugeLength) {
        return calculateFromRawData(dataPoints, initialArea, gaugeLength, 1.0);
    }

    /**
     * [New] 원본 데이터, 수동 입력 치수, 그리고 보정 계수(Load Factor)를 사용하여 재계산
     * 
     * @param loadCorrectionFactor 하중 단위 보정 계수 (예: kN -> N 변환 등 추정치)
     */
    public List<StressStrainPoint> calculateFromRawData(List<DataPoint> dataPoints, double initialArea,
            double gaugeLength, double loadCorrectionFactor) {
        List<StressStrainPoint> result = new ArrayList<>();

        if (dataPoints == null || initialArea <= 0 || gaugeLength <= 0) {
            return result;
        }

        for (DataPoint point : dataPoints) {
            // Load 보정 적용
            double correctedLoad = point.getLoad() * loadCorrectionFactor;

            // 1. 공칭 응력/변형률 계산
            double engStress = correctedLoad / initialArea;
            double engStrain = point.getDisplacement() / gaugeLength;

            // 2. 진응력/진변형률 계산
            double trueStrain = 0.0;
            double trueStress = 0.0;

            if (1 + engStrain > 0) {
                trueStrain = Math.log(1 + engStrain);
                trueStress = engStress * (1 + engStrain);
            }

            result.add(new StressStrainPoint(engStress, engStrain, trueStress, trueStrain));
        }

        return result;
    }

    /**
     * [New] 파일 원본 Stress 값과 (Load / UserArea) 값을 비교하여 보정 계수를 역산함.
     * 이를 통해 Load Unit이 N이 아니더라도 원본 Stress 스케일에 맞춰줌.
     */
    public double calculateCorrectionFactor(List<DataPoint> rawData, List<StressStrainPoint> filePoints,
            double userArea) {
        if (rawData == null || filePoints == null || rawData.size() != filePoints.size() || userArea <= 0) {
            return 1.0;
        }

        double sumRatio = 0.0;
        int count = 0;

        // 전체 데이터를 다 돌면 노이즈나 0 부근에서 오차가 커지므로,
        // 응력이 어느 정도 발생하는 구간(중간 50%)만 샘플링
        int start = rawData.size() / 4;
        int end = rawData.size() * 3 / 4;

        for (int i = start; i < end; i++) {
            double load = rawData.get(i).getLoad();
            // 파일 내 Engineering Stress (정답 데이터)
            double targetStress = filePoints.get(i).getEngineeringStress();

            if (Math.abs(load) > 1.0 && Math.abs(targetStress) > 1.0) {
                // targetStress = (load * Factor) / userArea
                // Factor = (targetStress * userArea) / load
                double factor = (targetStress * userArea) / load;
                sumRatio += factor;
                count++;
            }
        }

        if (count == 0)
            return 1.0;
        return sumRatio / count;
    }

    /**
     * 최대 응력 찾기
     */
    public double findMaxStress(List<StressStrainPoint> points) {
        return points.stream()
                .mapToDouble(StressStrainPoint::getTrueStress)
                .max()
                .orElse(0.0);
    }

    /**
     * 최대 응력에서의 변형률 찾기
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

            int start = Math.max(0, i - halfWindow);
            int end = Math.min(data.size(), i + halfWindow + 1);

            for (int j = start; j < end; j++) {
                StressStrainPoint p = data.get(j);
                sumEStress += p.getEngineeringStress();
                sumEStrain += p.getEngineeringStrain();
                sumTStress += p.getTrueStress();
                sumTStrain += p.getTrueStrain();
                count++;
            }

            smoothed.add(new StressStrainPoint(
                    sumEStress / count,
                    sumEStrain / count,
                    sumTStress / count,
                    sumTStrain / count));
        }

        return smoothed;
    }

    /**
     * 데이터 다운샘플링
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
     */
    public List<StressStrainPoint> removeNegativeStress(List<StressStrainPoint> data) {
        if (data == null || data.isEmpty())
            return data;

        List<StressStrainPoint> filtered = new ArrayList<>();
        for (StressStrainPoint point : data) {
            if (point.getTrueStress() > 0 && point.getEngineeringStress() > 0) {
                filtered.add(point);
            }
        }
        return filtered;
    }

    /**
     * 파단 후 데이터 제거
     */
    public List<StressStrainPoint> removePostFractureData(List<StressStrainPoint> data, double dropThreshold) {
        if (data == null || data.isEmpty())
            return data;

        double maxStress = findMaxStress(data);
        int maxStressIndex = -1;

        for (int i = 0; i < data.size(); i++) {
            if (Math.abs(data.get(i).getTrueStress() - maxStress) < 0.01) {
                maxStressIndex = i;
                break;
            }
        }

        if (maxStressIndex == -1)
            return data;

        List<StressStrainPoint> filtered = new ArrayList<>();
        double thresholdStress = maxStress * dropThreshold;

        for (int i = 0; i <= maxStressIndex; i++) {
            filtered.add(data.get(i));
        }

        for (int i = maxStressIndex + 1; i < data.size(); i++) {
            if (data.get(i).getTrueStress() >= thresholdStress) {
                filtered.add(data.get(i));
            } else {
                break;
            }
        }
        return filtered;
    }

    /**
     * 포괄적인 데이터 클리닝
     */
    public List<StressStrainPoint> cleanData(List<StressStrainPoint> data) {
        if (data == null || data.isEmpty())
            return data;
        List<StressStrainPoint> cleaned = removeNegativeStress(data);
        cleaned = removePostFractureData(cleaned, 0.5);
        return cleaned;
    }

    /**
     * 데이터의 시작점을 (0,0)으로 강제 이동
     */
    public List<StressStrainPoint> applyZeroOffset(List<StressStrainPoint> points) {
        if (points == null || points.isEmpty())
            return points;

        double startStrain = 0.0;
        double startStress = 0.0;
        boolean foundStart = false;

        for (StressStrainPoint p : points) {
            if (p.getTrueStress() > 5.0) {
                startStrain = p.getTrueStrain();
                startStress = p.getTrueStress();
                foundStart = true;
                break;
            }
        }

        if (!foundStart) {
            startStrain = points.get(0).getTrueStrain();
            startStress = points.get(0).getTrueStress();
        }

        List<StressStrainPoint> correctedPoints = new ArrayList<>();
        double startEngStrain = 0.0;

        for (StressStrainPoint p : points) {
            if (Math.abs(p.getTrueStrain() - startStrain) < 0.00001) {
                startEngStrain = p.getEngineeringStrain();
                break;
            }
        }

        for (StressStrainPoint p : points) {
            if (p.getTrueStrain() >= startStrain) {
                double newTrueStrain = p.getTrueStrain() - startStrain;
                double newTrueStress = p.getTrueStress(); // Stress는 보통 0으로 안 맞춤 (초기 하중 있으므로) -> 요구사항 따라 확인 필요
                // 여기서는 Stress Shift는 안 함 (Load Zeroing은 실험기가 했을 거라 가정)
                // 만약 Stress도 0으로 맞추려면: newTrueStress = p.getTrueStress() - startStress;

                double newEngStrain = p.getEngineeringStrain() - startEngStrain;
                double newEngStress = p.getEngineeringStress();

                correctedPoints.add(new StressStrainPoint(
                        newEngStress,
                        newEngStrain,
                        newTrueStress,
                        newTrueStrain));
            }
        }
        return correctedPoints;
    }
}
