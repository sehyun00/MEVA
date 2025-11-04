package meva.models;

/**
 * 시험 데이터를 담는 모델 클래스
 * 
 * @author 이태윤
 */
public class TestData {
    private double[] force;        // 하중 데이터
    private double[] displacement; // 변위 데이터
    private double[] time;         // 시간 데이터
    private double initialLength;  // 초기 길이
    private double crossSectionArea; // 단면적
    private String testDate;       // 시험 일자
    private String operator;       // 시험자
    
    // 생성자
    public TestData() {}
    
    // Getter 및 Setter 메서드들
    public double[] getForce() { return force; }
    public void setForce(double[] force) { this.force = force; }
    
    public double[] getDisplacement() { return displacement; }
    public void setDisplacement(double[] displacement) { this.displacement = displacement; }
    
    public double[] getTime() { return time; }
    public void setTime(double[] time) { this.time = time; }
    
    public double getInitialLength() { return initialLength; }
    public void setInitialLength(double initialLength) { this.initialLength = initialLength; }
    
    public double getCrossSectionArea() { return crossSectionArea; }
    public void setCrossSectionArea(double crossSectionArea) { this.crossSectionArea = crossSectionArea; }
    
    public String getTestDate() { return testDate; }
    public void setTestDate(String testDate) { this.testDate = testDate; }
    
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}