package meva.calculation;

import meva.models.StressStrainPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * 항복점(Yield Point) 감지 및 분석 클래스
 * 
 * @author MEVA 개발팀
 * @version 1.0
 */
public class YieldAnalyzer {

    // 상/하항복점을 담기 위한 내부 클래스 (public으로 공개하여 외부 사용 가능하게 함)
    public static class YieldPoints {
        public StressStrainPoint upper;
        public StressStrainPoint lower;

        public YieldPoints(StressStrainPoint upper, StressStrainPoint lower) {
            this.upper = upper;
            this.lower = lower;
        }
    }

    /**
     * [S45C 전용] 불연속 항복점(상/하항복점) 정밀 탐색 (데이터 테이블 패턴 매칭)
     * DataSmoother 참조 필요 (Helper usage)
     */
    public YieldPoints detectDiscontinuousYielding(List<StressStrainPoint> points, DataSmoother smoother,
            int utsIndex) {
        if (points == null || points.size() < 100)
            return null;

        // 2. 주요 변곡점(Key Points) 추출
        List<Integer> peaks = new ArrayList<>();
        List<Integer> valleys = new ArrayList<>();

        // 탐색 범위: 초기 탄성 구간(0.2% Strain) 이후 ~ UTS 이전
        int startIndex = 0;
        for (int i = 0; i < utsIndex; i++) {
            if (points.get(i).getTrueStrain() > 0.002) {
                startIndex = i;
                break;
            }
        }

        // 스무딩된 값은 아니지만, 주변 평균값을 통해 변곡점 찾기
        for (int i = startIndex + 5; i < utsIndex - 5; i++) {
            double prev = smoother.getAverageStress(points, i - 5);
            double curr = smoother.getAverageStress(points, i);
            double next = smoother.getAverageStress(points, i + 5);

            // 산봉우리 (Local Maxima)
            if (curr > prev && curr > next) {
                peaks.add(i);
            }
            // 골짜기 (Local Minima)
            if (curr < prev && curr < next) {
                valleys.add(i);
            }
        }

        // 3. S45C 패턴 매칭 (유의미한 피크-골짜기 쌍 찾기)
        for (int peakIdx : peaks) {
            double peakStress = points.get(peakIdx).getTrueStress();

            int bestValleyIdx = -1;
            double minValleyStress = peakStress;

            for (int valleyIdx : valleys) {
                if (valleyIdx <= peakIdx)
                    continue; // 피크 뒤여야 함

                double vStress = points.get(valleyIdx).getTrueStress();

                if (vStress < minValleyStress) {
                    minValleyStress = vStress;
                    bestValleyIdx = valleyIdx;
                }
            }

            if (bestValleyIdx != -1) {
                double dropRatio = (peakStress - minValleyStress) / peakStress;

                if (dropRatio > 0.005) { // 0.5% 이상 하락
                    return new YieldPoints(points.get(peakIdx), points.get(bestValleyIdx));
                }
            }
        }

        return null;
    }

    /**
     * 0.2% 오프셋 항복점 계산 및 반환 (공칭/진응력 선택 가능, 보간법 적용)
     */
    public StressStrainPoint calculateOffsetYieldPoint(List<StressStrainPoint> points, double youngsModulus,
            double offset, boolean useEngineering, double utsStrainLimit) {
        if (points == null || points.isEmpty())
            return null;

        double E_MPa = youngsModulus * 1000.0;

        // 교차점 찾기 (선형 보간)
        for (int i = 1; i < points.size(); i++) {
            StressStrainPoint p1 = points.get(i - 1);
            StressStrainPoint p2 = points.get(i);

            double strain1 = useEngineering ? p1.getEngineeringStrain() : p1.getTrueStrain();
            double strain2 = useEngineering ? p2.getEngineeringStrain() : p2.getTrueStrain();

            // 너무 초반 데이터는 스킵 (오프셋 이전)
            if (strain1 < offset * 0.5)
                continue;

            // UTS 지점을 넘어가면 탐색 중단
            if (strain1 > utsStrainLimit && utsStrainLimit > 0)
                break;

            double stress1 = useEngineering ? p1.getEngineeringStress() : p1.getTrueStress();
            double stress2 = useEngineering ? p2.getEngineeringStress() : p2.getTrueStress();

            // 오프셋 라인 식: y = E * (x - offset)
            double lineY1 = E_MPa * (strain1 - offset);
            double lineY2 = E_MPa * (strain2 - offset);

            // 곡선과 직선의 높이 차이
            double diff1 = stress1 - lineY1;
            double diff2 = stress2 - lineY2;

            // 부호가 반대면 교차함 -> 보간하여 정확한 교차점 생성
            if (diff1 * diff2 <= 0) {
                double t = Math.abs(diff1) / (Math.abs(diff1) + Math.abs(diff2));

                double intersectStrain = strain1 + t * (strain2 - strain1);
                double intersectStress = stress1 + t * (stress2 - stress1);

                if (useEngineering) {
                    return new StressStrainPoint(intersectStress, intersectStrain, 0.0, 0.0);
                } else {
                    return new StressStrainPoint(0.0, 0.0, intersectStress, intersectStrain);
                }
            }
        }

        return null; // 교차점 없음
    }
}
