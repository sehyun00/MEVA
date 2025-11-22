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
                point.getTStrain()
            );
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
}
