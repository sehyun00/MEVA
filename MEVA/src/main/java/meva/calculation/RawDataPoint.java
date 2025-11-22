package meva.calculation;

// --------------------------------------------------------------
// RawDataPoint 클래스
// --------------------------------------------------------------
// 이 클래스는 "인장시험에서 측정된 한 지점의 원시 데이터"를 저장하는 역할을 한다.
// 예: 변위(displacement) = 0.1mm, 하중(force) = 100N
// 이런 데이터가 여러 개 모여서 전체 인장곡선(raw data)을 구성하게 된다.
// --------------------------------------------------------------
public class RawDataPoint {

    // 변위(ΔL): 인장시험에서 늘어난 길이(mm)
    public double displacement;

    // 하중(F): 시험편에 실제로 걸린 힘(N)
    public double force;

    // ----------------------------------------------------------
    // 생성자(Constructor)
    // RawDataPoint 객체를 만들 때 변위와 하중 값을 입력받아 저장한다.
    // 예: new RawDataPoint(0.2, 150);
    // ----------------------------------------------------------
    public RawDataPoint(double displacement, double force) {
        this.displacement = displacement;  // 객체의 변위 값 저장
        this.force = force;                // 객체의 하중 값 저장
    }
}
