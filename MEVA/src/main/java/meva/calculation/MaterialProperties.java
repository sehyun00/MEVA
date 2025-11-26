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
            // 탄성 구간 데이터가 부족하면 초기 데이터 사용
            elasticPoints.clear();
            int count = Math.min(20, points.size());
            for (int i = 0; i < count; i++) {
                if (points.get(i).getTrueStress() > 0 && 
                    points.get(i).getTrueStrain() > 0) {
                    elasticPoints.add(points.get(i));
                }
            }
        }
        
        if (elasticPoints.size() < 2) {
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
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        
        // MPa에서 GPa로 변환 (1 GPa = 1000 MPa)
        return slope / 1000.0;
    }
    
    /**
     * 항복 강도 계산 (0.2% Offset Method)
     * 
     * @param points 응력-변형률 데이터 포인트 리스트
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
     * 인장 강도 계산
     * 
     * @param testData 시험 데이터
     * @return 인장 강도
     */
    public double calculateTensileStrength(TestData testData) {
        // TODO: F-CALC-001 요구사항 구현
        return 0.0;
    }
    
    /**
     * 연신율 계산
     * 
     * @param testData 시험 데이터
     * @return 연신율
     */
    public double calculateElongation(TestData testData) {
        // TODO: F-CALC-002 요구사항 구현
        return 0.0;
    }
    
    /**
     * 단면수축률 계산
     * 
     * @param testData 시험 데이터
     * @return 단면수축률
     */
    public double calculateReductionOfArea(TestData testData) {
        // TODO: F-CALC-002 요구사항 구현
        return 0.0;
    }
}
