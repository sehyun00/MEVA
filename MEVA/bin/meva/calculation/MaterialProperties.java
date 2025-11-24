package first;

import java.util.ArrayList;
import java.util.List;

// --------------------------------------------------------------
// MaterialProperties 클래스
// --------------------------------------------------------------
// 이 클래스는 재료의 인장시험 데이터를 기반으로
// ① 공학적(Engineering) 응력-변형률
// ② 진(True) 응력-변형률
// 을 계산하는 역할을 담당한다.
//
// - 입력: 변위(ΔL), 하중(F)로 구성된 RawDataPoint 리스트
// - 내부 계산:
//      ε_eng = ΔL / L0
//      σ_eng = F / A0
//
//      ε_true = ln(1 + ε_eng)
//      σ_true = σ_eng * (1 + ε_eng)
// --------------------------------------------------------------
public class MaterialProperties {

    // ----------------------------------------------------------
    // 계산된 응력/변형률 데이터를 저장하는 리스트
    // ----------------------------------------------------------

    // 공학적 응력 (σ_eng)
    public List<Double> engineeringStress = new ArrayList<>();

    // 공학적 변형률 (ε_eng)
    public List<Double> engineeringStrain = new ArrayList<>();

    // 진응력 (σ_true)
    public List<Double> trueStress = new ArrayList<>();

    // 진변형률 (ε_true)
    public List<Double> trueStrain = new ArrayList<>();

    // ----------------------------------------------------------
    // 재료의 기본 물성값
    // ----------------------------------------------------------

    // 초기 게이지 길이 (L0, mm)
    public double L0;

    // 초기 단면적 (A0, mm^2)
    public double A0;

    // ----------------------------------------------------------
    // 생성자
    // L0, A0 를 받아 MaterialProperties 객체를 초기화한다.
    // ----------------------------------------------------------
    public MaterialProperties(double L0, double A0) {
        this.L0 = L0;
        this.A0 = A0;
    }

    // ----------------------------------------------------------
    // (1) 공학적 응력/변형률 계산 메소드
    // raw: RawDataPoint 리스트 (변위, 하중 데이터)
    //
    // ε_eng = ΔL / L0
    // σ_eng = F / A0
    // ----------------------------------------------------------
    public void computeEngineering(List<RawDataPoint> raw) {
        for (RawDataPoint p : raw) {

            // 공학적 변형률 ε = ΔL / L0
            engineeringStrain.add(p.displacement / L0);

            // 공학적 응력 σ = F / A0
            engineeringStress.add(p.force / A0);
        }
    }

    // ----------------------------------------------------------
    // (2) 진응력·진변형률 변환 메소드
    //
    // ε_true = ln(1 + ε_eng)
    // σ_true = σ_eng * (1 + ε_eng)
    // ----------------------------------------------------------
    public void convertToTrue() {
        for (int i = 0; i < engineeringStress.size(); i++) {
            double e = engineeringStrain.get(i);  // 공학적 변형률
            double s = engineeringStress.get(i);  // 공학적 응력

            // 진변형률: ln(1 + ε_eng)
            trueStrain.add(Math.log(1 + e));

            // 진응력: σ_eng * (1 + ε_eng)
            trueStress.add(s * (1 + e));
        }
    }
}
