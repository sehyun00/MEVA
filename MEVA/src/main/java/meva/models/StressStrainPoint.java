package meva.models;

/**
 * 계산된 응력-변형률 데이터를 저장하는 클래스
 * 그래프 시각화에 사용됨
 * 
 * @author MEVA 개발팀
 * @version 1.0
 */
public class StressStrainPoint {
    private double engineeringStress;  // 공칭 응력 (MPa)
    private double engineeringStrain;  // 공칭 변형률
    private double trueStress;         // 진응력 (MPa)
    private double trueStrain;         // 진변형률

    public StressStrainPoint(double engineeringStress, double engineeringStrain,
                             double trueStress, double trueStrain) {
        this.engineeringStress = engineeringStress;
        this.engineeringStrain = engineeringStrain;
        this.trueStress = trueStress;
        this.trueStrain = trueStrain;
    }

    // Getters
    public double getEngineeringStress() { return engineeringStress; }
    public double getEngineeringStrain() { return engineeringStrain; }
    public double getTrueStress() { return trueStress; }
    public double getTrueStrain() { return trueStrain; }

    @Override
    public String toString() {
        return String.format("StressStrainPoint[eStress=%.3f, eStrain=%.4f, tStress=%.3f, tStrain=%.4f]",
                engineeringStress, engineeringStrain, trueStress, trueStrain);
    }
}
