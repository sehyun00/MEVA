package meva.models;

/**
 * 시뮬레이션 결과를 담는 모델 클래스
 * 
 * @author 이태윤
 */
public class SimulationResult {
    private double[] stress;       // 응력 배열
    private double[] strain;       // 변형률 배열
    private double youngsModulus;  // 계산된 영률
    private double yieldStrength;  // 계산된 항복강도
    private double tensileStrength; // 계산된 인장강도
    private double elongation;     // 연신율
    private double reductionOfArea; // 단면수축률
    private boolean isValid;       // 결과 유효성
    private String errorMessage;   // 오류 메시지
    
    // 생성자
    public SimulationResult() {}
    
    // Getter 및 Setter 메서드들
    public double[] getStress() { return stress; }
    public void setStress(double[] stress) { this.stress = stress; }
    
    public double[] getStrain() { return strain; }
    public void setStrain(double[] strain) { this.strain = strain; }
    
    public double getYoungsModulus() { return youngsModulus; }
    public void setYoungsModulus(double youngsModulus) { this.youngsModulus = youngsModulus; }
    
    public double getYieldStrength() { return yieldStrength; }
    public void setYieldStrength(double yieldStrength) { this.yieldStrength = yieldStrength; }
    
    public double getTensileStrength() { return tensileStrength; }
    public void setTensileStrength(double tensileStrength) { this.tensileStrength = tensileStrength; }
    
    public double getElongation() { return elongation; }
    public void setElongation(double elongation) { this.elongation = elongation; }
    
    public double getReductionOfArea() { return reductionOfArea; }
    public void setReductionOfArea(double reductionOfArea) { this.reductionOfArea = reductionOfArea; }
    
    public boolean isValid() { return isValid; }
    public void setValid(boolean valid) { isValid = valid; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}