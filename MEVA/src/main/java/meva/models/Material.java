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
    
    // 기본 생성자
    public Material() {}
    
    // 전체 필드 생성자
    public Material(String name, String category, double youngsModulus, double yieldStrength, 
                   double tensileStrength, double density, double poissonRatio) {
        this.name = name;
        this.category = category;
        this.youngsModulus = youngsModulus;
        this.yieldStrength = yieldStrength;
        this.tensileStrength = tensileStrength;
        this.density = density;
        this.poissonRatio = poissonRatio;
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
    
    @Override
    public String toString() {
        return String.format("Material{id=%d, name='%s', category='%s', E=%.2f GPa, σy=%.2f MPa}",
                           id, name, category, youngsModulus, yieldStrength);
    }
}
