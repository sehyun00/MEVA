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
        // True Stress 기준 (Primary)
        double[] modulusData = calculateYoungsModulusWithIntercept(smoothedPoints, false);
        double youngsModulus = modulusData[0];
        result.setYoungsModulus(youngsModulus);
        result.setElasticLineIntercept(modulusData[1]);

        // Engineering Stress 기준 (For Visualization)
        double[] modulusDataEng = calculateYoungsModulusWithIntercept(smoothedPoints, true);
        result.setYoungsModulusEng(modulusDataEng[0]);
        result.setElasticLineInterceptEng(modulusDataEng[1]);

        // [핵심 변경] 분석 로직의 이원화 (Dual Calculation)
        // 사용자가 UI에서 '0.2% Offset' 또는 '상/하항복점'을 선택할 수 있도록 두 가지 모두 계산하여 저장함.

        // A. 0.2% 오프셋 항복점
        // True Stress 기준
        StressStrainPoint offsetPoint = calculateOffsetYieldPoint(smoothedPoints, youngsModulus, 0.002, false);
        result.setOffsetYieldPoint(offsetPoint);

        // Engineering Stress 기준 (For Visualization)
        StressStrainPoint offsetPointEng = calculateOffsetYieldPoint(smoothedPoints, result.getYoungsModulusEng(), 0.002, true);
        result.setOffsetYieldPointEng(offsetPointEng);

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
     * 0.2% 오프셋 항복점 계산 및 반환 (Legacy 호환용 - True Stress 기준)
     */
    private StressStrainPoint calculateOffsetYieldPoint(List<StressStrainPoint> points, double youngsModulus, double offset) {
        return calculateOffsetYieldPoint(points, youngsModulus, offset, false);
    }

    /**
     * 0.2% 오프셋 항복점 계산 및 반환 (공칭/진응력 선택 가능, 보간법 적용)
     */
    private StressStrainPoint calculateOffsetYieldPoint(List<StressStrainPoint> points, double youngsModulus, double offset, boolean useEngineering) {
        if (points == null || points.isEmpty()) return null;
        
        double E_MPa = youngsModulus * 1000.0;
        
        // [안전장치] UTS 포인트 찾기
        StressStrainPoint utsPoint = findUTSPoint(points);
        double utsStrain = (utsPoint != null) ? 
            (useEngineering ? utsPoint.getEngineeringStrain() : utsPoint.getTrueStrain()) : 
            Double.MAX_VALUE;

        // 교차점 찾기 (선형 보간)
        for (int i = 1; i < points.size(); i++) {
            StressStrainPoint p1 = points.get(i - 1);
            StressStrainPoint p2 = points.get(i);

            double strain1 = useEngineering ? p1.getEngineeringStrain() : p1.getTrueStrain();
            double strain2 = useEngineering ? p2.getEngineeringStrain() : p2.getTrueStrain();
            
            // 너무 초반 데이터는 스킵 (오프셋 이전)
            if (strain1 < offset * 0.5) continue; 

            // [중요] UTS 지점을 넘어가면 탐색 중단
            if (strain1 > utsStrain) break;

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
                // x축 기준 보간 비율 (t) 계산: diff1에서 0이 되는 지점까지의 비율
                // diff(x) = stress(x) - lineY(x)
                // diff는 선형이라고 가정 (구간이 짧으므로)
                double t = Math.abs(diff1) / (Math.abs(diff1) + Math.abs(diff2));
                
                double intersectStrain = strain1 + t * (strain2 - strain1);
                double intersectStress = stress1 + t * (stress2 - stress1);
                
                // 새로운 포인트 생성 (나머지 값은 0 또는 근사치로 채움 - 시각화용이므로 해당 모드 값만 중요)
                if (useEngineering) {
                    return new StressStrainPoint(intersectStress, intersectStrain, 0.0, 0.0);
                } else {
                    return new StressStrainPoint(0.0, 0.0, intersectStress, intersectStrain);
                }
            }
        }
        
        return null; // 교차점 없음
    }

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
        if (points == null || points.size() < 20) return new double[]{0.0, 0.0};

        // 1. UTS(최대 강도) 찾기
        double maxStress = 0.0;
        for (StressStrainPoint p : points) {
            double val = useEngineering ? p.getEngineeringStress() : p.getTrueStress();
            if (val > maxStress) maxStress = val;
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
            if (stress > upperBound * 1.5) break; 
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
                if (stress > upperBound * 1.5) break;
            }
        }

        if (candidateRegion.size() < 5) return new double[]{0.0, 0.0};

        // 3. 슬라이딩 윈도우 설정
        int windowSize = Math.min(20, candidateRegion.size() / 2);
        if (windowSize < 5) windowSize = 5;

        double maxSlope = 0.0;
        double bestIntercept = 0.0; // 최적 기울기일 때의 절편
        
        double fallbackSlope = 0.0;
        double fallbackIntercept = 0.0;
        double maxR2 = -1.0;

        for (int i = 0; i <= candidateRegion.size() - windowSize; i += 1) {
            List<StressStrainPoint> subset = candidateRegion.subList(i, i + windowSize);
            double[] reg = calculateLinearRegressionWithIntercept(subset, useEngineering); // [0]: slope, [1]: intercept, [2]: r2
            double slope = reg[0];
            double intercept = reg[1];
            double r2 = reg[2];

            if (slope <= 0) continue;

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
            return new double[]{maxSlope / 1000.0, bestIntercept};
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
            if(maxSlope > 0) return new double[]{maxSlope / 1000.0, bestIntercept};
        }

        return new double[]{fallbackSlope / 1000.0, fallbackIntercept};
    }

    /**
     * 수동으로 지정된 변형률(Strain) 구간을 사용하여 물성치를 재계산합니다.
     * (Legacy 호환용 - True Strain 기준)
     */
    public AnalysisResult recalculateFromManualSlope(List<StressStrainPoint> points, AnalysisResult currentResult, double startStrain, double endStrain) {
        return recalculateFromManualSlope(points, currentResult, startStrain, endStrain, false);
    }

    /**
     * 수동으로 지정된 변형률(Strain) 구간을 사용하여 물성치를 재계산합니다.
     * 
     * @param points 원본 데이터
     * @param startStrain 구간 시작 변형률
     * @param endStrain 구간 끝 변형률
     * @param useEngineering 공칭 응력/변형률 사용 여부
     * @return 업데이트된 AnalysisResult
     */
    public AnalysisResult recalculateFromManualSlope(List<StressStrainPoint> points, AnalysisResult currentResult, double startStrain, double endStrain, boolean useEngineering) {
        if (points == null || points.size() < 10 || currentResult == null) return currentResult;
        
        List<StressStrainPoint> smoothedPoints = applySmoothing(points);
        List<StressStrainPoint> manualRegion = new ArrayList<>();
        
        // 1. 선택된 구간 데이터 추출
        for(StressStrainPoint p : smoothedPoints) {
            double strain = useEngineering ? p.getEngineeringStrain() : p.getTrueStrain();
            if(strain >= startStrain && strain <= endStrain) {
                manualRegion.add(p);
            }
        }
        
        if(manualRegion.size() < 2) return currentResult; // 데이터 너무 적음
        
        // 2. 해당 구간에 대한 선형 회귀
        double[] reg = calculateLinearRegressionWithIntercept(manualRegion, useEngineering);
        double slope = reg[0];
        double intercept = reg[1];
        double r2 = reg[2];
        
        // 3. 결과 업데이트
        if (useEngineering) {
            currentResult.setYoungsModulusEng(slope / 1000.0);
            currentResult.setElasticLineInterceptEng(intercept);
        } else {
            currentResult.setYoungsModulus(slope / 1000.0); // MPa -> GPa
            currentResult.setElasticLineIntercept(intercept);
        }
        
        // 4. Offset 항복점 재계산
        if (useEngineering) {
            StressStrainPoint offsetPointEng = calculateOffsetYieldPoint(smoothedPoints, currentResult.getYoungsModulusEng(), 0.002, true);
            currentResult.setOffsetYieldPointEng(offsetPointEng);
        } else {
            StressStrainPoint offsetPoint = calculateOffsetYieldPoint(smoothedPoints, currentResult.getYoungsModulus(), 0.002, false);
            currentResult.setOffsetYieldPoint(offsetPoint);
        }
        
        // Auto 모드가 Offset이었거나, 사용자가 Offset 모드를 보고 있었다면 YieldPoint도 갱신
        if (currentResult.getYieldType() == AnalysisResult.YieldType.OFFSET_02) {
            if (useEngineering) {
                if (currentResult.getOffsetYieldPointEng() != null) {
                    currentResult.setYieldPoint(currentResult.getOffsetYieldPointEng());
                    currentResult.setYieldStrength(currentResult.getOffsetYieldPointEng().getEngineeringStress());
                }
            } else {
                if (currentResult.getOffsetYieldPoint() != null) {
                    currentResult.setYieldPoint(currentResult.getOffsetYieldPoint());
                    currentResult.setYieldStrength(currentResult.getOffsetYieldPoint().getTrueStress());
                }
            }
        }
        
        System.out.println(String.format("  ✓ Manual Recalc(Eng=%b): Range[%.4f ~ %.4f], E=%.1f GPa, R²=%.4f", 
                useEngineering, startStrain, endStrain, currentResult.getYoungsModulus(), r2));
        
        return currentResult;
    }

    /**
     * 기존 메서드 호환성 유지 (삭제 예정이나 안전을 위해 남겨둠)
     */
    public double calculateYoungsModulus(List<StressStrainPoint> points) {
        return calculateYoungsModulusWithIntercept(points)[0];
    }

    /**
     * 선형 회귀 계산 (Intercept 포함, True Stress 기준)
     */
    private double[] calculateLinearRegressionWithIntercept(List<StressStrainPoint> points) {
        return calculateLinearRegressionWithIntercept(points, false);
    }

    /**
     * 선형 회귀 계산 (Intercept 포함, 모드 선택 가능)
     * @return double[] {Slope, Intercept, R²}
     */
    private double[] calculateLinearRegressionWithIntercept(List<StressStrainPoint> points, boolean useEngineering) {
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
        if (denominator == 0) return new double[]{0, 0, 0};

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
        return new double[]{slope, intercept, r2};
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

    /**
     * 재료의 인성(Toughness) 계산
     * 파단점까지의 응력-변형률 곡선 아래 면적을 적분
     * 
     * @param points 응력-변형률 데이터 리스트
     * @return 인성 (MJ/m³)
     */
    public double calculateToughness(List<StressStrainPoint> points) {
        if (points == null || points.size() < 2) return 0.0;
        
        // 파단점 찾기 (마지막 포인트)
        StressStrainPoint fracturePoint = points.get(points.size() - 1);
        double epsilonFracture = fracturePoint.getTrueStrain();
        
        // 데이터 준비 (True Strain, True Stress)
        List<Double> epsilons = new ArrayList<>();
        List<Double> sigmas = new ArrayList<>();
        for (StressStrainPoint p : points) {
            epsilons.add(p.getTrueStrain());
            sigmas.add(p.getTrueStress());
        }
        
        // 사다리꼴 적분 수행
        return calculateToughnessTrapezoidal(epsilons, sigmas, epsilonFracture);
    }

    /**
     * 사다리꼴 적분법(Trapezoidal Rule)을 이용한 인성 계산 (내부 로직)
     * 불규칙한 데이터 간격을 고려하여 면적을 계산함
     * 
     * @param epsilons 변형률 리스트
     * @param sigmas 응력 리스트
     * @param epsilonFracture 파단 변형률 (적분 상한)
     * @return 적분된 면적 (MJ/m³)
     */
    public double calculateToughnessTrapezoidal(List<Double> epsilons, List<Double> sigmas, double epsilonFracture) {
        if (epsilons == null || sigmas == null) return 0.0;
        int n = Math.min(epsilons.size(), sigmas.size());
        if (n < 2) return 0.0;

        double area = 0.0;

        for (int i = 0; i < n - 1; i++) {
            double e0 = epsilons.get(i);
            double e1 = epsilons.get(i + 1);
            double s0 = sigmas.get(i);
            double s1 = sigmas.get(i + 1);

            // 파단점 이후 구간은 무시
            if (e0 >= epsilonFracture) break;

            // 마지막 구간이 파단점을 가로지르는 경우 (Linear Interpolation)
            if (e1 > epsilonFracture) {
                // 파단점에서의 응력 추정 (선형 보간)
                double sF = s0 + (s1 - s0) * (epsilonFracture - e0) / (e1 - e0);

                // 잘린 사다리꼴 면적 추가: (e0 ~ epsilonFracture)
                double base = epsilonFracture - e0;
                double avgHeight = 0.5 * (s0 + sF);
                area += avgHeight * base;
                break;
            } else {
                // 일반 구간: 전체 사다리꼴 면적 추가
                double base = e1 - e0;
                double avgHeight = 0.5 * (s0 + s1);
                area += avgHeight * base;
            }
        }

        return area;
    }

    /**
     * 탄성 에너지(Resilience) 계산 (삼각형 근사 - Primary)
     * Hooke's Law 가정: 0.5 * YieldStrength * YieldStrain
     * 
     * @param points 응력-변형률 데이터 (사용하지 않음, 호환성 위해 유지)
     * @param yieldStrength 항복 강도 (MPa)
     * @return 탄성 에너지 (MJ/m³)
     */
    public double calculateResilience(List<StressStrainPoint> points, double yieldStrength) {
        // 항복점이 없으면 계산 불가
        if (yieldStrength <= 0 || points == null || points.isEmpty()) return 0.0;

        // 항복 변형률 찾기 (데이터에서 yieldStrength에 가장 가까운 점 탐색)
        // 정확도를 위해 보간법을 쓸 수도 있지만, 여기서는 삼각형 근사를 위한 대표값만 찾음
        double yieldStrain = 0.0;
        
        // 1. 단순 탐색
        for(StressStrainPoint p : points) {
            if (Math.abs(p.getTrueStress() - yieldStrength) < 1.0) { // 오차 1MPa 이내
                yieldStrain = p.getTrueStrain();
                break;
            }
        }
        
        // 2. 못 찾았으면(오프셋 등으로 계산된 값이 데이터에 딱 없을 때)
        // E(영률)을 역산해서 추정하거나, 가장 가까운 점 사용
        if (yieldStrain == 0.0) {
             // 근사적으로 Young's Modulus를 이용해 역산: ε = σ / E
             // 하지만 여기선 points가 있으니 가장 가까운 놈으로
             double minDiff = Double.MAX_VALUE;
             for(StressStrainPoint p : points) {
                 double diff = Math.abs(p.getTrueStress() - yieldStrength);
                 if(diff < minDiff) {
                     minDiff = diff;
                     yieldStrain = p.getTrueStrain();
                 }
             }
        }
        
        // 삼각형 면적 공식: 0.5 * σ_y * ε_y
        return 0.5 * yieldStrength * yieldStrain;
    }
    
    /**
     * 탄성 에너지(Resilience) 계산 (정밀 적분 - Secondary)
     * 0 ~ 항복점까지의 실제 곡선 적분
     */
    public double calculateResilienceIntegral(List<StressStrainPoint> points, double yieldStrain) {
        if (points == null || points.size() < 2) return 0.0;
        
        List<Double> epsilons = new ArrayList<>();
        List<Double> sigmas = new ArrayList<>();
        for(StressStrainPoint p : points) {
            epsilons.add(p.getTrueStrain());
            sigmas.add(p.getTrueStress());
        }
        
        return calculateToughnessTrapezoidal(epsilons, sigmas, yieldStrain);
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

