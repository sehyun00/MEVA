package meva.models;

/**
 * 실험 데이터 파일에서 읽은 원시 데이터를 저장하는 클래스
 * 
 * @author MEVA 개발팀
 * @version 1.0
 */
public class DataPoint {
    private double time;           // TIME
    private double load;           // LOAD
    private double displacement;   // DISPLACEMENT
    private double strainGage;     // STRAIN GAGE
    private double thetaL;         // θL
    private double eStress;        // E.STRESS (Engineering Stress)
    private double eStrain;        // E.STRAIN (Engineering Strain)
    private double tStress;        // T.STRESS (True Stress)
    private double tStrain;        // T.STRAIN (True Strain)

    // 생성자
    public DataPoint(double time, double load, double displacement, 
                     double strainGage, double thetaL,
                     double eStress, double eStrain, 
                     double tStress, double tStrain) {
        this.time = time;
        this.load = load;
        this.displacement = displacement;
        this.strainGage = strainGage;
        this.thetaL = thetaL;
        this.eStress = eStress;
        this.eStrain = eStrain;
        this.tStress = tStress;
        this.tStrain = tStrain;
    }

    // Getters
    public double getTime() { return time; }
    public double getLoad() { return load; }
    public double getDisplacement() { return displacement; }
    public double getStrainGage() { return strainGage; }
    public double getThetaL() { return thetaL; }
    public double getEStress() { return eStress; }
    public double getEStrain() { return eStrain; }
    public double getTStress() { return tStress; }
    public double getTStrain() { return tStrain; }

    @Override
    public String toString() {
        return String.format("DataPoint[time=%.3f, load=%.3f, tStress=%.3f, tStrain=%.3f]",
                time, load, tStress, tStrain);
    }
}
