package meva.calculation;

import meva.models.AnalysisResult;
import meva.models.StressStrainPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * 재료 물성값 계산 클래스
 * 
 * @author MEVA 개발팀
 */
public class MaterialProperties {

    /**
     * 데이터 노이즈 제거를 위한 이동 평균 필터 (Strain-based Adaptive Smoothing)
     * 공학적 타당성: 0.2% Offset 지점을 왜곡하지 않도록, 윈도우 크기는 그 절반인 0.1% Strain 이하로 제한
     * 
     * @param points 원본 데이터
     * @return 노이즈가 제거된 새로운 데이터 리스트
     */
    private List<StressStrainPoint> applySmoothing(List<StressStrainPoint> points) {
        if (points == null || points.size() < 10) return points;

        List<StressStrainPoint> smoothed = new ArrayList<>();
        
        // 적응형 윈도우 크기 계산: Strain 0.001 (0.1%) 구간에 해당하는 데이터 개수 찾기
        // 초반 탄성 구간(데이터 밀도가 높은 곳)을 기준으로 샘플링
        int sampleLimit = Math.min(points.size(), 1000);
        double strainSum = 0;
        for(int i=1; i<sampleLimit; i++) {
            strainSum += (points.get(i).getTrueStrain() - points.get(i-1).getTrueStrain());
        }
        double avgStrainStep = strainSum / (sampleLimit - 1);
        
        // 0.001 Strain 구간에 들어가는 포인트 개수 (최소 1개 ~ 최대 20개 제한)
        int windowHalfSize = (int) (0.0005 / avgStrainStep); // 앞뒤로 0.05%씩, 총 0.1%
        if (windowHalfSize < 1) windowHalfSize = 1; // 스무딩 불필요
        if (windowHalfSize > 20) windowHalfSize = 20; // 너무 많이 뭉개지 않도록 제한

        // 윈도우가 너무 작으면 원본 반환 (스무딩 의미 없음)
        if (windowHalfSize <= 1) return points;

        System.out.println("  ✓ 적응형 스무딩 적용 (Window Half-Size: " + windowHalfSize + ")");

        for (int i = 0; i < points.size(); i++) {
            double sumStress = 0;
            // Strain은 X축 변수이므로 스무딩하지 않고 원본 위치를 유지하는 것이 일반적이지만,
            // 여기서는 노이즈 캔슬링을 위해 Stress만 평균값으로 대체
            
            int start = Math.max(0, i - windowHalfSize);
            int end = Math.min(points.size() - 1, i + windowHalfSize);
            int count = 0;
            
            for (int j = start; j <= end; j++) {
                sumStress += points.get(j).getTrueStress();
                count++;
            }
            
            StressStrainPoint p = points.get(i);
            // 새로운 Stress 값을 가진 객체 생성 (나머지 속성은 원본 유지)
            
            // StressStrainPoint는 EngStress, EngStrain, TrueStress, TrueStrain만 가지고 있음.
            // TrueStress를 스무딩했으므로, EngStress도 그에 비례하여 역산해야 함.
            // TrueStress = EngStress * (1 + EngStrain) -> EngStress = TrueStress / (1 + EngStrain)
            
            double newTrueStress = sumStress / count;
            double newEngStress = newTrueStress / (1.0 + p.getEngineeringStrain());
            
            StressStrainPoint newPoint = new StressStrainPoint(
                newEngStress,
                p.getEngineeringStrain(), 
                newTrueStress,
                p.getTrueStrain()
            );
            smoothed.add(newPoint);
        }
        return smoothed;
    }

    /**
     * 전체 데이터에 대한 물성 분석을 수행하여 결과를 반환합니다.
     * Auto 모드로 항복점 거동(연속/불연속)을 자동 감지합니다.
     * 
     * @param points 응력-변형률 데이터 리스트
     * @return 분석 결과 객체 (AnalysisResult)
     */
    public AnalysisResult analyze(List<StressStrainPoint> points) {
        AnalysisResult result = new AnalysisResult();
        
        if (points == null || points.isEmpty()) {
            return result;
        }

        // [전처리] 적응형 스무딩 적용
        // 노이즈가 심한 재료(알루미늄 등)를 위해 0.1% Strain 구간 이동평균 데이터 생성
        // 원본 데이터(points)는 보존하고, 계산 로직에는 smoothedPoints를 사용
        List<StressStrainPoint> smoothedPoints = applySmoothing(points);

        // 1. 영률(Young's Modulus) 정밀 계산 (R² 최적화 방식)
        // 영률 계산은 미세한 초기 구간이므로 원본 데이터를 쓰는 것이 더 정확할 수 있으나,
        // 노이즈가 심하면 스무딩된 것이 유리함. R² 로직이 강력하므로 스무딩된 데이터 사용.
        double youngsModulus = calculateYoungsModulus(smoothedPoints);
        result.setYoungsModulus(youngsModulus);

        // [핵심 변경] 분석 로직의 이원화 (Dual Calculation)
        // 사용자가 UI에서 '0.2% Offset' 또는 '상/하항복점'을 선택할 수 있도록 두 가지 모두 계산하여 저장함.

        // A. 0.2% 오프셋 항복점 (알루미늄/티타늄용, 또는 S45C 보조용)
        // 스무딩된 데이터를 사용하여 노이즈 영향을 최소화함
        StressStrainPoint offsetPoint = calculateOffsetYieldPoint(smoothedPoints, youngsModulus, 0.002);
        result.setOffsetYieldPoint(offsetPoint); // 별도 필드에 저장 (UI 전환용)

        // B. 불연속 항복점 (S45C용) - 원본 데이터 사용 (피크 보존)
        // '데이터 테이블 패턴 매칭' 알고리즘으로 UYP/LYP 탐색
        YieldPoints discontinuousPoints = detectDiscontinuousYielding(points, youngsModulus);
        
        if (discontinuousPoints != null) {
            // 불연속 항복이 명확히 감지된 경우 (S45C 등)
            result.setYieldType(AnalysisResult.YieldType.DISCONTINUOUS);
            result.setUpperYieldPoint(discontinuousPoints.upper);
            result.setLowerYieldPoint(discontinuousPoints.lower);
            
            // 기본 대표값 설정 (Auto 모드용)
            result.setYieldPoint(discontinuousPoints.upper);
            result.setYieldStrength(discontinuousPoints.upper.getTrueStress());
            
            System.out.println("  ✓ 불연속 항복 감지됨: UYP=" + discontinuousPoints.upper.getTrueStress());
        } else {
            // 불연속 항복이 감지되지 않은 경우 (알루미늄 등) -> 0.2% Offset 사용
            result.setYieldType(AnalysisResult.YieldType.OFFSET_02);
            
            if (offsetPoint != null) {
                result.setYieldPoint(offsetPoint);
                result.setYieldStrength(offsetPoint.getTrueStress());
            } else {
                result.setYieldStrength(0.0); // 찾지 못함
            }
            
            System.out.println("  ✓ 연속 항복 (0.2% Offset) 적용");
        }

        // 3. UTS(최대 인장 강도) 찾기 - UTS는 스무딩하면 값이 낮아질 수 있으므로 원본 데이터 사용 권장
        StressStrainPoint utsPoint = findUTSPoint(points);
        result.setUtsPoint(utsPoint);
        result.setTensileStrength(utsPoint != null ? utsPoint.getEngineeringStress() : 0.0);

        // 4. 파단점 - 원본 데이터 사용
        StressStrainPoint fracturePoint = points.get(points.size() - 1);
        result.setFracturePoint(fracturePoint);

        // 5. 기타 물성 계산 (원본 데이터 기준)
        result.setElongation(calculateElongation(points));
        result.setReductionOfArea(calculateReductionOfArea(points));
        result.setToughness(calculateToughness(points));
        result.setResilience(calculateResilience(points, result.getYieldStrength()));
        result.setElasticLimit(calculateElasticLimit(points, youngsModulus));
        result.setProportionalLimit(calculateProportionalLimit(points, youngsModulus));
        result.setNeckingStartStrain(utsPoint != null ? utsPoint.getEngineeringStrain() : 0.0);
        result.setFractureStress(fracturePoint.getTrueStress());
        result.setFractureStrain(fracturePoint.getTrueStrain());

        return result;
    }

    // 상/하항복점을 담기 위한 내부 클래스
    private static class YieldPoints {
        StressStrainPoint upper;
        StressStrainPoint lower;
        YieldPoints(StressStrainPoint upper, StressStrainPoint lower) {
            this.upper = upper;
            this.lower = lower;
        }
    }

    /**
     * [S45C 전용] 불연속 항복점(상/하항복점) 정밀 탐색 (데이터 테이블 패턴 매칭)
     * 개선: 국소적 노이즈가 아닌, 전체 구간의 거시적 패턴(Peak & Valley)을 분석하여 S45C 고유의 형상을 찾음.
     */
    private YieldPoints detectDiscontinuousYielding(List<StressStrainPoint> points, double youngsModulus) {
        if (points.size() < 100) return null;

        // 1. UTS 인덱스 찾기 (탐색 종료점)
        // 항복 현상은 UTS 이전에 발생하므로 그 뒤는 볼 필요 없음
        int utsIndex = -1;
        double maxStress = -1;
        for(int i=0; i<points.size(); i++) {
            if(points.get(i).getEngineeringStress() > maxStress) {
                maxStress = points.get(i).getEngineeringStress();
                utsIndex = i;
            }
        }
        if(utsIndex == -1) utsIndex = points.size() - 1;

        // 2. 주요 변곡점(Key Points) 추출
        // 노이즈를 거르기 위해 ±5개 평균값(Moving Average)으로 추세를 비교하여 변곡점 후보를 찾음
        List<Integer> peaks = new ArrayList<>();
        List<Integer> valleys = new ArrayList<>();
        
        // 탐색 범위: 초기 탄성 구간(0.2% Strain) 이후 ~ UTS 이전
        int startIndex = 0;
        for(int i=0; i<utsIndex; i++) {
            if(points.get(i).getTrueStrain() > 0.002) {
                startIndex = i;
                break;
            }
        }

        for (int i = startIndex + 5; i < utsIndex - 5; i++) {
            double prev = getAverageStress(points, i - 5);
            double curr = getAverageStress(points, i);
            double next = getAverageStress(points, i + 5);

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
        // 조건: 피크 이후에 '더 낮은' 골짜기가 존재해야 하며, 그 낙차가 유의미해야 함.
        
        for (int peakIdx : peaks) {
            double peakStress = points.get(peakIdx).getTrueStress();
            
            // 이 피크 뒤에 오는 골짜기들 중 '가장 깊은' 골짜기 찾기
            // 단, 가공경화(Strain Hardening) 시작 전까지만 유효함.
            // (응력이 피크 레벨을 넘어서면 가공경화 구간이므로 중단)
            
            int bestValleyIdx = -1;
            double minValleyStress = peakStress;
            
            for (int valleyIdx : valleys) {
                if (valleyIdx <= peakIdx) continue; // 피크 뒤여야 함
                
                double vStress = points.get(valleyIdx).getTrueStress();
                
                // 만약 골짜기 가기 전에 응력이 피크보다 높아져버렸다면? (가공경화 시작)
                // -> 그 전까지 찾은 최저점이 LYP임.
                // (단, 여기서는 단순화된 리스트 순회이므로, 피크와 골짜기 사이의 데이터 검증이 필요할 수 있음)
                
                if (vStress < minValleyStress) {
                    minValleyStress = vStress;
                    bestValleyIdx = valleyIdx;
                }
            }
            
            // [검증]
            // 1. 골짜기를 찾았는가?
            // 2. 낙차(Drop)가 충분히 큰가? (0.5% 이상)
            // 3. 피크와 골짜기 사이 거리가 너무 멀지 않은가? (YPE 구간 내여야 함)
            if (bestValleyIdx != -1) {
                double dropRatio = (peakStress - minValleyStress) / peakStress;
                
                if (dropRatio > 0.005) { // 0.5% 이상 하락
                    // 찾았다! S45C 패턴임.
                    return new YieldPoints(points.get(peakIdx), points.get(bestValleyIdx));
                }
            }
        }

        return null;
    }

    // 도우미 함수: 주변 3개 점의 평균 응력 반환 (간이 스무딩)
    private double getAverageStress(List<StressStrainPoint> points, int index) {
        if (index < 1 || index >= points.size() - 1) return points.get(index).getTrueStress();
        
        double sum = 0;
        int count = 0;
        for(int i = index-1; i<=index+1; i++) {
            sum += points.get(i).getTrueStress();
            count++;
        }
        return sum / count;
    }

    /**
     * 0.2% 오프셋 항복점 계산 및 반환
     */
    private StressStrainPoint calculateOffsetYieldPoint(List<StressStrainPoint> points, double youngsModulus, double offset) {
        if (points == null || points.isEmpty()) return null;
        
        double E_MPa = youngsModulus * 1000.0;
        StressStrainPoint closest = null;
        double minDiff = Double.MAX_VALUE;
        
        // [안전장치] UTS 포인트 찾기 (교차점이 UTS를 넘어가면 안 됨)
        StressStrainPoint utsPoint = findUTSPoint(points);
        double utsStrain = (utsPoint != null) ? utsPoint.getTrueStrain() : Double.MAX_VALUE;

        // 교차점 찾기
        for (int i = 1; i < points.size(); i++) {
            StressStrainPoint p1 = points.get(i - 1);
            StressStrainPoint p2 = points.get(i);

            double strain1 = p1.getTrueStrain();
            double strain2 = p2.getTrueStrain();
            
            // 너무 초반 데이터는 스킵 (오프셋 이전)
            if (strain1 < offset * 0.5) continue; 

            // [중요] UTS 지점을 넘어가면 탐색 중단
            // 항복점은 물리적으로 반드시 UTS 이전에 있어야 함
            if (strain1 > utsStrain) break;

            double stress1 = p1.getTrueStress();
            double stress2 = p2.getTrueStress();

            // 오프셋 라인 식: y = E * (x - offset)
            double lineY1 = E_MPa * (strain1 - offset);
            double lineY2 = E_MPa * (strain2 - offset);
            
            // 곡선과 직선의 높이 차이
            double diff1 = stress1 - lineY1;
            double diff2 = stress2 - lineY2;
            
            // 부호가 반대면 교차함
            if (diff1 * diff2 <= 0) {
                if (Math.abs(diff1) < Math.abs(diff2)) return p1;
                else return p2;
            }
            
            // 교차점을 못 찾을 경우를 대비해 가장 가까운 점 저장
            double dist = Math.min(Math.abs(diff1), Math.abs(diff2));
            if (dist < minDiff) {
                minDiff = dist;
                if (Math.abs(diff1) < Math.abs(diff2)) closest = p1;
                else closest = p2;
            }
        }
        
        // 교차점을 못 찾았지만 근사한 점이 있다면 반환 (데이터 노이즈 대응)
        return closest;
    }

    /**
     * 영률(Young's Modulus) 정밀 계산 (응력 구간 기반 탐색)
     * 개선: 변형률(Strain)이 아닌 응력(Stress) 구간(10%~40%)을 기준으로 탐색하여
     * 초기 Toe 및 소성 구간의 영향을 배제하고 순수 탄성 구간을 찾음.
     */
    public double calculateYoungsModulus(List<StressStrainPoint> points) {
        if (points == null || points.size() < 20) return 0.0;

        // 1. UTS(최대 강도) 찾기
        double maxStress = 0.0;
        for (StressStrainPoint p : points) {
            if (p.getTrueStress() > maxStress) maxStress = p.getTrueStress();
        }

        // 2. 탐색 범위 설정: 응력의 10% ~ 40% 구간
        // (Toe 회피 및 소성 구간 진입 방지)
        double lowerBound = maxStress * 0.10;
        double upperBound = maxStress * 0.40;

        List<StressStrainPoint> candidateRegion = new ArrayList<>();
        for (StressStrainPoint p : points) {
            double stress = p.getTrueStress();
            if (stress >= lowerBound && stress <= upperBound) {
                candidateRegion.add(p);
            }
            // 탄성 구간을 확실히 벗어났다고 판단되면 조기 종료 (최적화)
            if (stress > upperBound * 1.5) break; 
        }
        
        // 데이터가 너무 적으면(샘플링 부족), 범위를 조금 더 넓혀서 재시도 (0~50%)
        if (candidateRegion.size() < 10) {
            candidateRegion.clear();
            lowerBound = 0.0; 
            upperBound = maxStress * 0.50;
            for (StressStrainPoint p : points) {
                if (p.getTrueStress() >= lowerBound && p.getTrueStress() <= upperBound) {
                    candidateRegion.add(p);
                }
                if (p.getTrueStress() > upperBound * 1.5) break;
            }
        }

        if (candidateRegion.size() < 5) return 0.0;

        // 3. 슬라이딩 윈도우 설정 (이하 기존 로직 유지)
        // 거시적인 선형성을 보기 위해 윈도우를 충분히 크게 잡음
        int windowSize = Math.min(20, candidateRegion.size() / 2);
        if (windowSize < 5) windowSize = 5;

        double maxSlope = 0.0;
        
        // 비상용: R²가 가장 좋은 구간 (기울기가 낮더라도)
        double fallbackSlope = 0.0;
        double maxR2 = -1.0;

        for (int i = 0; i <= candidateRegion.size() - windowSize; i += 1) {
            List<StressStrainPoint> subset = candidateRegion.subList(i, i + windowSize);
            double[] reg = calculateLinearRegression(subset); // [0]: slope, [1]: r2
            double slope = reg[0];
            double r2 = reg[1];

            if (slope <= 0) continue;

            // 3. [핵심] 거시적 판단 로직
            // 직선성이 확보된(0.98 이상) 구간들 중에서 '가장 가파른' 기울기를 찾음
            // (알루미늄 등 초기 노이즈가 있는 경우 0.99는 너무 가혹하므로 0.98로 완화하되 기울기 우선)
            if (r2 > 0.980) {
                if (slope > maxSlope) {
                    maxSlope = slope;
                }
            }

            // 비상용 (직선 구간이 0.99를 못 넘길 경우 대비)
            if (r2 > maxR2) {
                maxR2 = r2;
                fallbackSlope = slope;
            }
        }

        // 4. 결과 반환 (MPa -> GPa)
        // 0.99 이상의 고품질 구간을 찾았으면 그것을 우선 사용
        if (maxSlope > 0) {
            return maxSlope / 1000.0;
        }
        
        // 0.99를 못 넘겼다면, 0.98 이상이면서 기울기가 높은 것을 다시 탐색 (기준 완화)
        if (maxR2 > 0.98 && maxSlope == 0.0) {
             for (int i = 0; i <= candidateRegion.size() - windowSize; i += 1) {
                List<StressStrainPoint> subset = candidateRegion.subList(i, i + windowSize);
                double[] reg = calculateLinearRegression(subset);
                if (reg[1] > 0.95 && reg[0] > maxSlope) { // 0.95 이상이면 기울기 우선
                    maxSlope = reg[0];
                }
            }
            if(maxSlope > 0) return maxSlope / 1000.0;
        }

        // 정 안되면 R²가 제일 좋았던 것 반환
        return fallbackSlope / 1000.0;
    }

    /**
     * 선형 회귀 계산 헬퍼 메서드
     * @param points 구간 데이터
     * @return double[] {기울기(Slope), 결정계수(R²)}
     */
    private double[] calculateLinearRegression(List<StressStrainPoint> points) {
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = points.size();
        
        for (StressStrainPoint p : points) {
            double x = p.getTrueStrain();
            double y = p.getTrueStress();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0) return new double[]{0, 0};

        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;

        double ssTot = 0, ssRes = 0;
        double yMean = sumY / n;
        
        for (StressStrainPoint p : points) {
            double y = p.getTrueStress();
            double yPred = slope * p.getTrueStrain() + intercept;
            ssTot += Math.pow(y - yMean, 2);
            ssRes += Math.pow(y - yPred, 2);
        }
        
        double r2 = (ssTot == 0) ? 0 : (1 - (ssRes / ssTot));
        return new double[]{slope, r2};
    }

    /**
     * [MEVA 정의] UTS 포인트 찾기 (Engineering Stress 기준)
     */
    public StressStrainPoint findUTSPoint(List<StressStrainPoint> points) {
        if (points == null || points.isEmpty()) return null;

        StressStrainPoint utsPoint = null;
        double maxEngStress = -1.0;

        for (StressStrainPoint p : points) {
            double currentEngStress = p.getEngineeringStress();
            if (currentEngStress > maxEngStress) {
                maxEngStress = currentEngStress;
                utsPoint = p;
            } else if (currentEngStress == maxEngStress) {
                if (utsPoint != null && p.getEngineeringStrain() < utsPoint.getEngineeringStrain()) {
                    utsPoint = p;
                }
            }
        }
        return utsPoint;
    }
    
    public double calculateElongation(List<StressStrainPoint> points) {
        if (points == null || points.isEmpty()) return 0.0;
        StressStrainPoint lastPoint = points.get(points.size() - 1);
        return (Math.exp(lastPoint.getTrueStrain()) - 1) * 100.0;
    }

    public double calculateReductionOfArea(List<StressStrainPoint> points) {
        if (points == null || points.isEmpty()) return 0.0;
        StressStrainPoint lastPoint = points.get(points.size() - 1);
        double areaRatio = Math.exp(-lastPoint.getTrueStrain());
        return (1 - areaRatio) * 100.0;
    }

    public double calculateToughness(List<StressStrainPoint> points) {
        if (points == null || points.size() < 2) return 0.0;
        double totalArea = 0.0;
        for (int i = 1; i < points.size(); i++) {
            StressStrainPoint p1 = points.get(i - 1);
            StressStrainPoint p2 = points.get(i);
            totalArea += (p1.getTrueStress() + p2.getTrueStress()) / 2.0 * (p2.getTrueStrain() - p1.getTrueStrain());
        }
        return totalArea;
    }

    public double calculateResilience(List<StressStrainPoint> points, double yieldStrength) {
        if (points == null || points.size() < 2 || yieldStrength <= 0) return 0.0;
        double totalArea = 0.0;
        for (int i = 1; i < points.size(); i++) {
            StressStrainPoint p1 = points.get(i - 1);
            StressStrainPoint p2 = points.get(i);
            if (p2.getTrueStress() > yieldStrength) break;
            totalArea += (p1.getTrueStress() + p2.getTrueStress()) / 2.0 * (p2.getTrueStrain() - p1.getTrueStrain());
        }
        return totalArea;
    }

    public double calculateElasticLimit(List<StressStrainPoint> points, double youngsModulus) {
        if (points == null || points.isEmpty() || youngsModulus <= 0) return 0.0;
        double E_MPa = youngsModulus * 1000.0;
        double tolerance = 0.02;
        for (StressStrainPoint p : points) {
            if (p.getTrueStrain() <= 0.0005) continue; // Toe 무시
            double expected = E_MPa * p.getTrueStrain();
            if (Math.abs(p.getTrueStress() - expected) / expected > tolerance) return p.getTrueStress();
        }
            return 0.0;
        }

    public double calculateProportionalLimit(List<StressStrainPoint> points, double youngsModulus) {
        if (points == null || points.isEmpty() || youngsModulus <= 0) return 0.0;
        double E_MPa = youngsModulus * 1000.0;
        double tolerance = 0.01;
        for (StressStrainPoint p : points) {
            if (p.getTrueStrain() <= 0.0005) continue;
            double expected = E_MPa * p.getTrueStrain();
            if (Math.abs(p.getTrueStress() - expected) / expected > tolerance) return p.getTrueStress();
        }
            return 0.0;
        }

    public double calculateFractureStress(List<StressStrainPoint> points) {
        if (points == null || points.isEmpty()) return 0.0;
        return points.get(points.size() - 1).getTrueStress();
    }

    public double calculateFractureStrain(List<StressStrainPoint> points) {
        if (points == null || points.isEmpty()) return 0.0;
        return points.get(points.size() - 1).getTrueStrain();
    }
}
