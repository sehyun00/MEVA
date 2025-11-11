package meva.models;

/**
 * 재료 정보를 담는 데이터 모델 클래스
 * 
 * @author 이태윤
 */
public class Material {
    private String name;           // 재료명
    private double youngsModulus;  // 영률
    private double yieldStrength;  // 항복강도
    private double tensileStrength; // 인장강도
    private double density;        // 밀도
    private double poissonRatio;   // 포아송비
    
    // 생성자
    public Material() {}
    
    public Material(String name, double youngsModulus, double yieldStrength, 
                   double tensileStrength, double density, double poissonRatio) {
        this.name = name;
        this.youngsModulus = youngsModulus;
        this.yieldStrength = yieldStrength;
        this.tensileStrength = tensileStrength;
        this.density = density;
        this.poissonRatio = poissonRatio;
    }
    
    // Getter 및 Setter 메서드들
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public double getYoungsModulus() { return youngsModulus; }
    public void setYoungsModulus(double youngsModulus) { this.youngsModulus = youngsModulus; }
    
    public double getYieldStrength() { return yieldStrength; }
    public void setYieldStrength(double yieldStrength) { this.yieldStrength = yieldStrength; }
    
    public double getTensileStrength() { return tensileStrength; }
    public void setTensileStrength(double tensileStrength) { this.tensileStrength = tensileStrength; }
    
    public double getDensity() { return density; }
    public void setDensity(double density) { this.density = density; }
    
    public double getPoissonRatio() { return poissonRatio; }
    public void setPoissonRatio(double poissonRatio) { this.poissonRatio = poissonRatio; }
}