package meva.models;

/**
 * 재료 정보를 담는 데이터 모델 클래스
 * SQLite DB의 materials 테이블과 매핑됨
 * 
 * @author 이태윤 (수정: DB 연동 추가)
 */
public class Material {
    private int id;                // DB Primary Key
    private String name;           // 재료명
    private String category;       // 카테고리 (Metal, Plastic 등)
    private double youngsModulus;  // 영률 (GPa)
    private double yieldStrength;  // 항복강도 (MPa)
    private double tensileStrength; // 인장강도 (MPa)
    private double density;        // 밀도 (g/cm³)
    private double poissonRatio;   // 포아송비
    
    // 시뮬레이션용 추가 필드 (Hollomon 식 및 파단 기준)
    private double strengthCoefficient; // 강도계수 K (MPa)
    private double hardeningExponent;   // 가공경화지수 n
    private double fractureStrain;      // 파단변형률 Ef
    
    // 기본 생성자
    public Material() {}
    
    // 전체 필드 생성자 (DB 조회 및 시뮬레이션용)
    public Material(String name, String category, double youngsModulus, double yieldStrength, 
                   double tensileStrength, double density, double poissonRatio,
                   double strengthCoefficient, double hardeningExponent, double fractureStrain) {
        this.name = name;
        this.category = category;
        this.youngsModulus = youngsModulus;
        this.yieldStrength = yieldStrength;
        this.tensileStrength = tensileStrength;
        this.density = density;
        this.poissonRatio = poissonRatio;
        this.strengthCoefficient = strengthCoefficient;
        this.hardeningExponent = hardeningExponent;
        this.fractureStrain = fractureStrain;
    }
    
    // Getter 및 Setter 메서드들
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
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
    
    public double getStrengthCoefficient() { return strengthCoefficient; }
    public void setStrengthCoefficient(double strengthCoefficient) { this.strengthCoefficient = strengthCoefficient; }
    
    public double getHardeningExponent() { return hardeningExponent; }
    public void setHardeningExponent(double hardeningExponent) { this.hardeningExponent = hardeningExponent; }
    
    public double getFractureStrain() { return fractureStrain; }
    public void setFractureStrain(double fractureStrain) { this.fractureStrain = fractureStrain; }
    
    @Override
    public String toString() {
        return String.format("Material{id=%d, name='%s', category='%s', E=%.2f GPa, σy=%.2f MPa, K=%.2f MPa, n=%.2f}",
                           id, name, category, youngsModulus, yieldStrength, strengthCoefficient, hardeningExponent);
    }
}
