package meva.models;

/**
 * 재료 물성 분석 결과를 담는 컨테이너 클래스 (DTO)
 * GraphPanel(시각화) 및 ResultPanel(수치 표시)에 데이터를 전달하는 역할
 * 
 * @author MEVA 개발팀
 */
public class AnalysisResult {
    // 주요 데이터 포인트 (그래프 마킹용)
    private StressStrainPoint utsPoint; // 최대 인장 강도 지점 (Engineering 기준)
    private StressStrainPoint yieldPoint; // 대표 항복점 (0.2% Offset 또는 상항복점)
    private StressStrainPoint upperYieldPoint; // 상항복점 (불연속 항복 시)
    private StressStrainPoint lowerYieldPoint; // 하항복점 (불연속 항복 시)
    private StressStrainPoint offsetYieldPoint; // 0.2% 오프셋 항복점 (True 기준)
    private StressStrainPoint offsetYieldPointEng; // 0.2% 오프셋 항복점 (Engineering 기준)
    private StressStrainPoint fracturePoint; // 파단점

    // 항복 거동 타입
    public enum YieldType {
        OFFSET_02, // 0.2% 오프셋 (연속 항복)
        DISCONTINUOUS // 불연속 항복 (상/하항복점 존재)
    }

    private YieldType yieldType = YieldType.OFFSET_02; // 기본값

    // 계산된 물성치 (ResultPanel 표시용)
    private double youngsModulus; // 영률 (GPa) (True Stress 기준)
    private double elasticLineIntercept; // 탄성 기울기 선의 Y절편 (True Stress 기준)
    private double youngsModulusEng; // 영률 (GPa) (Engineering Stress 기준)
    private double elasticLineInterceptEng; // 탄성 기울기 선의 Y절편 (Engineering Stress 기준)
    private double yieldStrength; // 항복 강도 (MPa)
    private double tensileStrength; // UTS (MPa)
    private double elongation; // 연신율 (%)
    private double reductionOfArea; // 단면 감소율 (%)
    private double toughness; // 인성 (MJ/m³)
    private double resilience; // 탄성 에너지 (MJ/m³) (Triangle 근사값)
    private double resilienceIntegral; // 탄성 에너지 (MJ/m³) (Integral 실제 적분값 - Auto Yield 기준)
    private double resilienceIntegralOffset; // 탄성 에너지 (MJ/m³) (Integral 실제 적분값 - Offset 0.2% 기준)
    private double proportionalLimit; // 비례 한계 (MPa)
    private double uniformElongation; // 균일 연신율 (Uniform Elongation)
    private double fractureStress; // 파괴 응력 (MPa)
    private double fractureStrain; // 파괴 변형률

    // 생성자
    public AnalysisResult() {
    }

    // Getters & Setters
    public StressStrainPoint getUtsPoint() {
        return utsPoint;
    }

    public void setUtsPoint(StressStrainPoint utsPoint) {
        this.utsPoint = utsPoint;
    }

    public StressStrainPoint getYieldPoint() {
        return yieldPoint;
    }

    public void setYieldPoint(StressStrainPoint yieldPoint) {
        this.yieldPoint = yieldPoint;
    }

    public StressStrainPoint getFracturePoint() {
        return fracturePoint;
    }

    public void setFracturePoint(StressStrainPoint fracturePoint) {
        this.fracturePoint = fracturePoint;
    }

    public StressStrainPoint getUpperYieldPoint() {
        return upperYieldPoint;
    }

    public void setUpperYieldPoint(StressStrainPoint upperYieldPoint) {
        this.upperYieldPoint = upperYieldPoint;
    }

    public StressStrainPoint getLowerYieldPoint() {
        return lowerYieldPoint;
    }

    public void setLowerYieldPoint(StressStrainPoint lowerYieldPoint) {
        this.lowerYieldPoint = lowerYieldPoint;
    }

    public StressStrainPoint getOffsetYieldPoint() {
        return offsetYieldPoint;
    }

    public void setOffsetYieldPoint(StressStrainPoint offsetYieldPoint) {
        this.offsetYieldPoint = offsetYieldPoint;
    }

    public StressStrainPoint getOffsetYieldPointEng() {
        return offsetYieldPointEng;
    }

    public void setOffsetYieldPointEng(StressStrainPoint offsetYieldPointEng) {
        this.offsetYieldPointEng = offsetYieldPointEng;
    }

    public YieldType getYieldType() {
        return yieldType;
    }

    public void setYieldType(YieldType yieldType) {
        this.yieldType = yieldType;
    }

    public double getYoungsModulus() {
        return youngsModulus;
    }

    public void setYoungsModulus(double youngsModulus) {
        this.youngsModulus = youngsModulus;
    }

    public double getElasticLineIntercept() {
        return elasticLineIntercept;
    }

    public void setElasticLineIntercept(double elasticLineIntercept) {
        this.elasticLineIntercept = elasticLineIntercept;
    }

    public double getYoungsModulusEng() {
        return youngsModulusEng;
    }

    public void setYoungsModulusEng(double youngsModulusEng) {
        this.youngsModulusEng = youngsModulusEng;
    }

    public double getElasticLineInterceptEng() {
        return elasticLineInterceptEng;
    }

    public void setElasticLineInterceptEng(double elasticLineInterceptEng) {
        this.elasticLineInterceptEng = elasticLineInterceptEng;
    }

    public double getYieldStrength() {
        return yieldStrength;
    }

    public void setYieldStrength(double yieldStrength) {
        this.yieldStrength = yieldStrength;
    }

    public double getTensileStrength() {
        return tensileStrength;
    }

    public void setTensileStrength(double tensileStrength) {
        this.tensileStrength = tensileStrength;
    }

    public double getElongation() {
        return elongation;
    }

    public void setElongation(double elongation) {
        this.elongation = elongation;
    }

    public double getReductionOfArea() {
        return reductionOfArea;
    }

    public void setReductionOfArea(double reductionOfArea) {
        this.reductionOfArea = reductionOfArea;
    }

    public double getToughness() {
        return toughness;
    }

    public void setToughness(double toughness) {
        this.toughness = toughness;
    }

    public double getResilience() {
        return resilience;
    }

    public void setResilience(double resilience) {
        this.resilience = resilience;
    }

    public double getResilienceIntegral() {
        return resilienceIntegral;
    }

    public void setResilienceIntegral(double resilienceIntegral) {
        this.resilienceIntegral = resilienceIntegral;
    }

    public double getResilienceIntegralOffset() {
        return resilienceIntegralOffset;
    }

    public void setResilienceIntegralOffset(double resilienceIntegralOffset) {
        this.resilienceIntegralOffset = resilienceIntegralOffset;
    }

    public double getProportionalLimit() {
        return proportionalLimit;
    }

    public void setProportionalLimit(double proportionalLimit) {
        this.proportionalLimit = proportionalLimit;
    }

    public double getFractureStress() {
        return fractureStress;
    }

    public void setFractureStress(double fractureStress) {
        this.fractureStress = fractureStress;
    }

    public double getFractureStrain() {
        return fractureStrain;
    }

    public void setFractureStrain(double fractureStrain) {
        this.fractureStrain = fractureStrain;
    }

    public double getUniformElongation() {
        return uniformElongation;
    }

    public void setUniformElongation(double uniformElongation) {
        this.uniformElongation = uniformElongation;
    }
}
