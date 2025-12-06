package meva.calculation;

import meva.models.AnalysisResult;
import meva.models.StressStrainPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * 재료 물성 분석 메인 클래스 (Facade Pattern)
 * 실제 복잡한 계산은 각 Analyzer 클래스에게 위임하고,
 * 결과값(AnalysisResult DTO)을 조립하여 반환하는 역할만 수행함.
 * 
 * @author MEVA 개발팀
 * @version 2.0 (Refactored)
 */
public class MaterialProperties {

    // 계산 로직 위임 객체들
    private final DataSmoother dataSmoother;
    private final ElasticAnalyzer elasticAnalyzer;
    private final YieldAnalyzer yieldAnalyzer;
    private final EnergyAnalyzer energyAnalyzer;

    public MaterialProperties() {
        this.dataSmoother = new DataSmoother();
        this.elasticAnalyzer = new ElasticAnalyzer();
        this.yieldAnalyzer = new YieldAnalyzer();
        this.energyAnalyzer = new EnergyAnalyzer();
    }

    /**
     * 메인 분석 메서드
     * Raw Data를 받아 스무딩 -> 탄성 -> 항복 -> 파괴 -> 에너지 순으로 분석
     */
    public AnalysisResult analyze(List<StressStrainPoint> points) {
        AnalysisResult result = new AnalysisResult();

        if (points == null || points.isEmpty()) {
            return result;
        }

        // 1. [전처리] 적응형 스무딩 적용
        List<StressStrainPoint> smoothedPoints = dataSmoother.applySmoothing(points);

        // 2. 영률(Young's Modulus) 계산 (ElasticAnalyzer)
        // True Stress 기준
        double[] elasticProps = elasticAnalyzer.calculateYoungsModulusWithIntercept(smoothedPoints, false);
        double youngsModulus = elasticProps[0];
        double intercept = elasticProps[1];

        result.setYoungsModulus(youngsModulus);
        result.setElasticLineIntercept(intercept);

        // Engineering Stress 기준
        double[] elasticPropsEng = elasticAnalyzer.calculateYoungsModulusWithIntercept(smoothedPoints, true);
        result.setYoungsModulusEng(elasticPropsEng[0]);
        result.setElasticLineInterceptEng(elasticPropsEng[1]);

        // 3. UTS(최대 인장 강도) 찾기 - 원본 데이터 사용 권장 (피크 보존)
        StressStrainPoint utsPoint = findUTSPoint(points);
        result.setUtsPoint(utsPoint);
        result.setTensileStrength(utsPoint.getEngineeringStress()); // 보통 UTS는 공칭응력 기준

        // 4. 항복점(Yield Point) 분석 (YieldAnalyzer)
        // A. 0.2% 오프셋 항복점 (True & Eng)
        double utsStrainLimit = utsPoint.getTrueStrain();

        StressStrainPoint offsetPoint = yieldAnalyzer.calculateOffsetYieldPoint(
                smoothedPoints, youngsModulus, 0.002, false, utsStrainLimit);
        result.setOffsetYieldPoint(offsetPoint);

        StressStrainPoint offsetPointEng = yieldAnalyzer.calculateOffsetYieldPoint(
                smoothedPoints, result.getYoungsModulusEng(), 0.002, true, utsPoint.getEngineeringStrain());
        result.setOffsetYieldPointEng(offsetPointEng);

        // B. 불연속 항복점 (S45C 등) 확인
        int utsIndex = points.indexOf(utsPoint);
        if (utsIndex == -1)
            utsIndex = points.size();

        YieldAnalyzer.YieldPoints discontinuousPoints = yieldAnalyzer.detectDiscontinuousYielding(
                points, dataSmoother, utsIndex // 스무더를 헬퍼로 전달
        );

        if (discontinuousPoints != null) {
            result.setYieldType(AnalysisResult.YieldType.DISCONTINUOUS);
            result.setUpperYieldPoint(discontinuousPoints.upper);
            result.setLowerYieldPoint(discontinuousPoints.lower);

            // 대표 항복점: 상항복점
            result.setYieldPoint(discontinuousPoints.upper);
            result.setYieldStrength(discontinuousPoints.upper.getEngineeringStress()); // 관행상 공칭응력
        } else {
            result.setYieldType(AnalysisResult.YieldType.OFFSET_02);
            // 대표 항복점: 0.2% 오프셋
            result.setYieldPoint(offsetPointEng); // 엔지니어링 표기 우선
            if (offsetPointEng != null) {
                result.setYieldStrength(offsetPointEng.getEngineeringStress());
            }
        }

        // 5. 파단점 & 연신율/단면감소율
        StressStrainPoint fracturePoint = points.get(points.size() - 1);
        result.setFracturePoint(fracturePoint);
        result.setFractureStress(fracturePoint.getTrueStress());
        result.setFractureStrain(fracturePoint.getTrueStrain());

        result.setElongation(calculateElongation(fracturePoint));
        result.setReductionOfArea(calculateReductionOfArea(fracturePoint));

        // 6. 에너지(인성, 레질리언스) 계산 (EnergyAnalyzer)
        // 인성 (Toughness) - 전체 면적 (Simpson)
        List<Double> epsilons = new ArrayList<>();
        List<Double> sigmas = new ArrayList<>();
        for (StressStrainPoint p : points) { // 원본 데이터 사용 (스무딩된 것보다 원본 적분이 정확할 수 있음)
            epsilons.add(p.getTrueStrain());
            sigmas.add(p.getTrueStress());
        }
        double toughness = energyAnalyzer.calculateToughnessSimpson(epsilons, sigmas, fracturePoint.getTrueStrain());
        result.setToughness(toughness / 1000.0); // MJ/m^3 단위 변환

        // 레질리언스 (Resilience)
        double yieldStrengthVal = result.getYieldStrength(); // 대표 항복 강도

        // [Fix] 초기 적분 시 대표 항복점(공칭)의 TrueStrain이 0.0일 수 있는 문제 해결
        // 무조건 True Stress 기준의 Offset 항복점을 사용하여 적분 범위를 설정함
        double yieldStrain = 0.0;
        if (offsetPoint != null) {
            yieldStrain = offsetPoint.getTrueStrain();
        } else if (result.getYieldPoint() != null) {
            // 오프셋 점이 없으면(거의 없겠지만), 대표 항복점의 진변형률 사용
            // 단, 대표 항복점이 공칭 기반이면 변환 필요
            StressStrainPoint yp = result.getYieldPoint();
            if (yp.getTrueStrain() == 0.0 && yp.getEngineeringStrain() > 0) {
                yieldStrain = Math.log(1 + yp.getEngineeringStrain());
            } else {
                yieldStrain = yp.getTrueStrain();
            }
        }

        // 1) 삼각형 근사
        double resilience = energyAnalyzer.calculateResilience(points, yieldStrengthVal);
        result.setResilience(resilience);

        // 2) 실제 적분 (Yield Point까지)
        double resilienceInt = energyAnalyzer.calculateResilienceIntegral(points, yieldStrain);
        result.setResilienceIntegral(resilienceInt);

        // 3) 오프셋 기준 적분 (위와 동일할 가능성 높음)
        if (offsetPoint != null) {
            result.setResilienceIntegralOffset(resilienceInt);
        }

        // 7. 기타 (탄성 한계, 비례 한계 등) - 간단한 로직은 Helper로 분리하거나 여기에 유지
        result.setElasticLimit(calculateElasticLimit(smoothedPoints, youngsModulus, false));
        result.setProportionalLimit(calculateProportionalLimit(smoothedPoints, youngsModulus, false));

        return result;
    }

    // --- 수동 재계산 메서드 ---

    public AnalysisResult recalculateFromManualSlope(List<StressStrainPoint> points, AnalysisResult currentResult,
            double startStrain, double endStrain, boolean useEngineering) {

        // [Dual Regression] 공칭/진응력 동시 회귀 분석
        // 사용자가 선택한 범위(Range)를 공통으로 사용하여 두 가지 물성치를 모두 갱신함
        // 이를 통해 View 모드 전환 시 데이터 불일치 방지

        List<StressStrainPoint> range = new ArrayList<>();

        // 범위 필터링 시 기준은 현재 View 모드(useEngineering)를 따름
        // (사용자가 화면에서 보고 지정한 영역이므로)
        for (StressStrainPoint p : points) {
            double s = useEngineering ? p.getEngineeringStrain() : p.getTrueStrain();
            if (s >= startStrain && s <= endStrain) {
                range.add(p);
            }
        }

        // 1. Engineering Properties Update
        double[] engProps = elasticAnalyzer.calculateLinearRegressionWithIntercept(range, true);
        double newYoungsEng = engProps[0] / 1000.0; // MPa -> GPa
        double newInterceptEng = engProps[1];

        currentResult.setYoungsModulusEng(newYoungsEng);
        currentResult.setElasticLineInterceptEng(newInterceptEng);

        // Recalculate Offset Yield (Eng) with new modulus
        double limitEng = (currentResult.getUtsPoint() != null) ? currentResult.getUtsPoint().getEngineeringStrain()
                : 1.0;
        StressStrainPoint newOffsetEng = yieldAnalyzer.calculateOffsetYieldPoint(
                points, newYoungsEng, 0.002, true, limitEng);
        currentResult.setOffsetYieldPointEng(newOffsetEng);

        // 2. True Stress Properties Update
        // 동일한 데이터 포인트(points within range)를 사용하여 True Stress 기준 회귀 수행
        double[] trueProps = elasticAnalyzer.calculateLinearRegressionWithIntercept(range, false);
        double newYoungsTrue = trueProps[0] / 1000.0; // MPa -> GPa
        double newInterceptTrue = trueProps[1];

        currentResult.setYoungsModulus(newYoungsTrue);
        currentResult.setElasticLineIntercept(newInterceptTrue);

        // Recalculate Offset Yield (True) with new modulus
        double limitTrue = (currentResult.getUtsPoint() != null) ? currentResult.getUtsPoint().getTrueStrain() : 1.0;
        StressStrainPoint newOffsetTrue = yieldAnalyzer.calculateOffsetYieldPoint(
                points, newYoungsTrue, 0.002, false, limitTrue);
        currentResult.setOffsetYieldPoint(newOffsetTrue);

        // --- 재계산된 항복점 및 에너지, 기타 물성치 갱신 (리팩토링: 공통 메서드 호출) ---
        return recalculatePropertiesBasedOnMode(currentResult, points, useEngineering);
    }

    // --- Local Helper Methods (Simple Calculations) ---

    private StressStrainPoint findUTSPoint(List<StressStrainPoint> points) {
        StressStrainPoint uts = points.get(0);
        for (StressStrainPoint p : points) {
            if (p.getEngineeringStress() > uts.getEngineeringStress()) {
                uts = p;
            }
        }
        return uts;
    }

    private double calculateElongation(StressStrainPoint fracturePoint) {
        // 연신율 = 파단 변형률 * 100 (%)
        // 보통 공학적 변형률을 사용함
        return fracturePoint.getEngineeringStrain() * 100.0;
    }

    private double calculateReductionOfArea(StressStrainPoint fracturePoint) {
        // 단면 감소율 (RA) = (1 - A_f/A_0) * 100
        // 현재로서는 단면적 정보가 없으므로 0.0 반환 (추후 업데이트)
        return 0.0;
    }

    /**
     * [신규 추가] 현재 설정된 Yield Mode 및 View Mode에 따라 에너지(Resilience)를 재계산합니다.
     * 핸들 조작 없이 콤보박스 옵션 변경만 있을 때도 호출됩니다.
     */
    /**
     * [신규 추가/확장] 현재 설정된 Yield Mode 및 View Mode에 따라
     * 에너지(Resilience, Toughness)와 모드 의존적 물성치(Elastic/Prop Limit)를 재계산합니다.
     */
    public AnalysisResult recalculatePropertiesBasedOnMode(AnalysisResult result, List<StressStrainPoint> points,
            boolean useEngineering) {

        // 1. 현재 선택된 Yield Type에 따라 대표 항복점 결정
        AnalysisResult.YieldType type = result.getYieldType();
        StressStrainPoint yieldPoint = null;

        if (type == AnalysisResult.YieldType.DISCONTINUOUS) {
            yieldPoint = result.getUpperYieldPoint(); // 상항복점 우선
        } else {
            // OFFSET_02 or Others -> 사용자가 보고 있는 모드(View Mode)에 맞는 Offset Point 선택
            yieldPoint = useEngineering ? result.getOffsetYieldPointEng() : result.getOffsetYieldPoint();
        }

        // 대표 항복점 업데이트
        result.setYieldPoint(yieldPoint);
        if (yieldPoint != null) {
            double strength = useEngineering ? yieldPoint.getEngineeringStress() : yieldPoint.getTrueStress();
            result.setYieldStrength(strength);
        }

        // 2. Resilience (Triangle) 재계산 - Hooke's Law 근사
        double currentYieldStrength = result.getYieldStrength();
        double newResilience = energyAnalyzer.calculateResilience(points, currentYieldStrength);
        result.setResilience(newResilience);

        // 3. Resilience Integral (Actual) 재계산 - 실제 적분 (View Mode 반영)
        // Eng 모드일 때는 Eng Strain 기준으로 적분 범위를 잡는 것이 자연스러움
        double limitStrain = 0.0;
        if (yieldPoint != null) {
            // View Mode에 맞는 Strain 사용
            limitStrain = useEngineering ? yieldPoint.getEngineeringStrain() : yieldPoint.getTrueStrain();
        } else {
            // Fallback: Offset Point
            StressStrainPoint offPt = useEngineering ? result.getOffsetYieldPointEng() : result.getOffsetYieldPoint();
            if (offPt != null) {
                limitStrain = useEngineering ? offPt.getEngineeringStrain() : offPt.getTrueStrain();
            }
        }

        // Integral 계산 시에도 View Mode에 맞는 데이터(Strain, Stress)를 내부적으로 사용해야 하나,
        // energyAnalyzer.calculateResilienceIntegral은 현재 True Strain/Stress를 가정하고 작성되었을
        // 가능성이 큼.
        // -> 확인 필요. 만약 True 고정이라면 여기서 Eng 데이터를 추출해서 넘겨야 함.
        // 현재 EnergyAnalyzer는 List<StressStrainPoint>를 받아서 내부적으로 p.getTrueStrain() 등을
        // 호출함.
        // 따라서 Eng 모드 지원을 위해 EnergyAnalyzer 수정 없이 여기서 데이터를 변환하거나,
        // 별도의 Helper 로직을 사용해야 함.
        // -> 여기서는 간단히 로직 내에서 직접 적분 수행 (Simpson's Rule) 또는 Helper 확장.
        // 시간 관계상 직접 구현:

        double newResilienceInt = calculateIntegral(points, limitStrain, useEngineering);
        result.setResilienceIntegral(newResilienceInt);

        // Offset Integral 업데이트 (참고용) - View Mode에 맞춰 재계산
        StressStrainPoint offPt = useEngineering ? result.getOffsetYieldPointEng() : result.getOffsetYieldPoint();
        if (offPt != null) {
            double offsetLimit = useEngineering ? offPt.getEngineeringStrain() : offPt.getTrueStrain();
            if (Math.abs(offsetLimit - limitStrain) < 1e-6) {
                result.setResilienceIntegralOffset(newResilienceInt);
            } else {
                result.setResilienceIntegralOffset(calculateIntegral(points, offsetLimit, useEngineering));
            }
        }

        // 4. Elastic Limit & Proportional Limit 재계산 (View Mode 의존)
        double modulus = useEngineering ? result.getYoungsModulusEng() : result.getYoungsModulus();
        if (modulus == 0 && result.getYoungsModulus() > 0)
            modulus = result.getYoungsModulus(); // Safety

        result.setElasticLimit(calculateElasticLimit(points, modulus, useEngineering));
        result.setProportionalLimit(calculateProportionalLimit(points, modulus, useEngineering));

        // 5. Toughness (인성) 재계산 (View Mode 의존)
        // 파단점까지의 적분
        double fractureStrain = 0.0;
        if (!points.isEmpty()) {
            StressStrainPoint last = points.get(points.size() - 1);
            fractureStrain = useEngineering ? last.getEngineeringStrain() : last.getTrueStrain();
        }
        double newToughness = calculateIntegral(points, fractureStrain, useEngineering);
        result.setToughness(newToughness / 1000.0); // MJ/m^3 (MPa * strain)

        return result;
    }

    // --- Helper Methods ---

    /**
     * 간단한 적분 (Trapezoidal Rule) - View Mode 지원
     */
    private double calculateIntegral(List<StressStrainPoint> points, double limitStrain, boolean useEngineering) {
        double area = 0.0;
        for (int i = 0; i < points.size() - 1; i++) {
            StressStrainPoint p1 = points.get(i);
            StressStrainPoint p2 = points.get(i + 1);

            double x1 = useEngineering ? p1.getEngineeringStrain() : p1.getTrueStrain();
            double y1 = useEngineering ? p1.getEngineeringStress() : p1.getTrueStress();
            double x2 = useEngineering ? p2.getEngineeringStrain() : p2.getTrueStrain();
            double y2 = useEngineering ? p2.getEngineeringStress() : p2.getTrueStress();

            if (x1 > limitStrain)
                break; // 범위 초과

            // x2가 limit을 초과하는 경우 보간 (Interpolation)
            if (x2 > limitStrain) {
                double fraction = (limitStrain - x1) / (x2 - x1);
                x2 = limitStrain;
                y2 = y1 + (y2 - y1) * fraction;
            }

            area += 0.5 * (y1 + y2) * (x2 - x1);

            if (x2 >= limitStrain)
                break;
        }
        return area;
    }

    private double calculateElasticLimit(List<StressStrainPoint> points, double youngsModulus, boolean useEngineering) {
        if (points == null || points.isEmpty() || youngsModulus <= 0)
            return 0.0;

        double E_MPa = youngsModulus * 1000.0;
        double tolerance = 0.02; // 2% 편차 허용
        for (StressStrainPoint p : points) {
            double strain = useEngineering ? p.getEngineeringStrain() : p.getTrueStrain();
            double stress = useEngineering ? p.getEngineeringStress() : p.getTrueStress();

            if (strain <= 0.0005)
                continue; // Toe 무시

            double expected = E_MPa * strain;
            if (Math.abs(stress - expected) / expected > tolerance)
                return stress;
        }
        return 0.0;
    }

    private double calculateProportionalLimit(List<StressStrainPoint> points, double youngsModulus,
            boolean useEngineering) {
        if (points == null || points.isEmpty() || youngsModulus <= 0)
            return 0.0;

        double E_MPa = youngsModulus * 1000.0;
        double tolerance = 0.01; // 1% 편차 허용 (더 엄격)
        for (StressStrainPoint p : points) {
            double strain = useEngineering ? p.getEngineeringStrain() : p.getTrueStrain();
            double stress = useEngineering ? p.getEngineeringStress() : p.getTrueStress();

            if (strain <= 0.0005)
                continue;

            double expected = E_MPa * strain;
            if (Math.abs(stress - expected) / expected > tolerance)
                return stress;
        }
        return 0.0;
    }
}
