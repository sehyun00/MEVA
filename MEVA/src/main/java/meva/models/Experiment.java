package meva.models;

import java.time.LocalDateTime;

/**
 * 실험 데이터 모델 클래스
 * 인장 실험의 메타데이터를 저장
 */
public class Experiment {
    private int id;
    private int materialId;
    private String materialName;
    private double specimenDiameter; // 시편 직경 (mm)
    private double gaugeLength; // 게이지 길이 (mm)
    private double crossSectionArea; // 단면적 (mm²)
    private String testDate; // 실험 날짜
    private Double testTemperature; // 실험 온도 (℃)
    private Double testSpeed; // 실험 속도 (mm/min)
    private String dataFilePath; // 데이터 파일 경로
    private String remarks; // 비고
    private String testerName; // 실험자 이름
    private String testMethod; // 실험 방법 (예: Position Control)
    private Double finalCrossSectionArea; // 최종 단면적 (mm²) - 단면감소율 계산용
    private LocalDateTime createdAt; // 생성 시간

    // 계산 결과 (조인 시 포함)
    private Double maxStress; // 최대 응력 (MPa)
    private Double strainAtMaxStress; // 최대 응력에서의 변형률
    private Double uts; // 극한인장강도 (MPa)

    // 기본 생성자
    public Experiment() {
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMaterialId() {
        return materialId;
    }

    public void setMaterialId(int materialId) {
        this.materialId = materialId;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public double getSpecimenDiameter() {
        return specimenDiameter;
    }

    public void setSpecimenDiameter(double specimenDiameter) {
        this.specimenDiameter = specimenDiameter;
    }

    public double getGaugeLength() {
        return gaugeLength;
    }

    public void setGaugeLength(double gaugeLength) {
        this.gaugeLength = gaugeLength;
    }

    public double getCrossSectionArea() {
        return crossSectionArea;
    }

    public void setCrossSectionArea(double crossSectionArea) {
        this.crossSectionArea = crossSectionArea;
    }

    public String getTestDate() {
        return testDate;
    }

    public void setTestDate(String testDate) {
        this.testDate = testDate;
    }

    public Double getTestTemperature() {
        return testTemperature;
    }

    public void setTestTemperature(Double testTemperature) {
        this.testTemperature = testTemperature;
    }

    public Double getTestSpeed() {
        return testSpeed;
    }

    public void setTestSpeed(Double testSpeed) {
        this.testSpeed = testSpeed;
    }

    public String getDataFilePath() {
        return dataFilePath;
    }

    public void setDataFilePath(String dataFilePath) {
        this.dataFilePath = dataFilePath;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getTesterName() {
        return testerName;
    }

    public void setTesterName(String testerName) {
        this.testerName = testerName;
    }

    public String getTestMethod() {
        return testMethod;
    }

    public void setTestMethod(String testMethod) {
        this.testMethod = testMethod;
    }

    public Double getFinalCrossSectionArea() {
        return finalCrossSectionArea;
    }

    public void setFinalCrossSectionArea(Double finalCrossSectionArea) {
        this.finalCrossSectionArea = finalCrossSectionArea;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Double getMaxStress() {
        return maxStress;
    }

    public void setMaxStress(Double maxStress) {
        this.maxStress = maxStress;
    }

    public Double getStrainAtMaxStress() {
        return strainAtMaxStress;
    }

    public void setStrainAtMaxStress(Double strainAtMaxStress) {
        this.strainAtMaxStress = strainAtMaxStress;
    }

    public Double getUts() {
        return uts;
    }

    public void setUts(Double uts) {
        this.uts = uts;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - Max Stress: %.2f MPa",
                materialName != null ? materialName : "Unknown",
                testDate != null ? testDate : "N/A",
                maxStress != null ? maxStress : 0.0);
    }
}
