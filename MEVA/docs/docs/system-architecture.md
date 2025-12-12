# MEVA 시스템 아키텍처 문서

## 1. 시스템 개요

### 1.1 프로젝트 정보
- **프로젝트명**: MEVA (Materials Engineering Visualization and Analysis)
- **목적**: 재료공학 학부생을 위한 인장시험 데이터 분석 및 시각화 교육용 프로그램
- **개발 환경**: Java 17, Java Swing, SQLite
- **버전**: v1.0
- **작성일**: 2025-11-11

### 1.2 시스템 목표
- 인장시험 이론 시뮬레이션 및 실시간 데이터 분석
- 직관적인 GUI를 통한 사용자 친화적 인터페이스 제공
- 다양한 형식의 데이터 입출력 지원 (CSV, Excel, PDF)
- 응력-변형률 곡선 및 물성값 시각화
- 경량화된 교육용 솔루션 제공

---

## 2. 시스템 아키텍처

### 2.1 아키텍처 패턴
MEVA는 **계층형 아키텍처(Layered Architecture)** 패턴을 따릅니다.

```
┌─────────────────────────────────────────────────────┐
│           Presentation Layer (GUI)                  │
│   - Java Swing Components                           │
│   - Event Handlers                                  │
│   - User Input/Output                               │
└─────────────────────────────────────────────────────┘
                        ↕
┌─────────────────────────────────────────────────────┐
│           Business Logic Layer                      │
│   - Calculation Engine                              │
│   - Data Validation                                 │
│   - Analysis Algorithms                             │
└─────────────────────────────────────────────────────┘
                        ↕
┌─────────────────────────────────────────────────────┐
│           Data Access Layer                         │
│   - Database Manager (SQLite)                       │
│   - File I/O Handler                                │
│   - Data Export Manager                             │
└─────────────────────────────────────────────────────┘
                        ↕
┌─────────────────────────────────────────────────────┐
│           Data Storage Layer                        │
│   - SQLite Database                                 │
│   - File System (CSV, Excel, PDF)                  │
└─────────────────────────────────────────────────────┘
```

### 2.2 주요 모듈 구성

#### 2.2.1 GUI Module (Presentation Layer)
- **담당자**: 김종현
- **브랜치**: `feature/gui-design`, `feature/user-interface`
- **주요 컴포넌트**:
  - `MainFrame`: 메인 윈도우
  - `InputPanel`: 데이터 입력 패널
  - `GraphPanel`: 그래프 시각화 패널
  - `ResultPanel`: 결과 출력 패널
  - `MenuBar`: 메뉴 및 도구 모음

#### 2.2.2 Calculation Module (Business Logic Layer)
- **담당자**: 이태윤, 김세현
- **브랜치**: `feature/stress-strain-calculation`
- **주요 컴포넌트**:
  - `StressStrainCalculator`: 응력-변형률 계산
  - `MaterialPropertiesCalculator`: 물성값 계산 (영률, 항복강도, 인장강도 등)
  - `SimulationEngine`: 인장시험 시뮬레이션
  - `DataValidator`: 입력 데이터 검증

#### 2.2.3 Visualization Module (Business Logic Layer)
- **담당자**: 김종현
- **브랜치**: `feature/graph-visualization`
- **주요 컴포넌트**:
  - `ChartGenerator`: JFreeChart 기반 차트 생성
  - `GraphRenderer`: 그래프 렌더링
  - `DataPlotter`: 데이터 포인트 플로팅

#### 2.2.4 File Handler Module (Data Access Layer)
- **담당자**: 이태윤
- **브랜치**: `feature/file-handler`, `feature/export-functionality`
- **주요 컴포넌트**:
  - `CSVReader`: CSV 파일 읽기
  - `ExcelHandler`: Excel 파일 입출력 (Apache POI)
  - `PDFExporter`: PDF 보고서 생성
  - `ImageExporter`: 그래프 이미지 저장 (PNG, JPG, SVG)

#### 2.2.5 Database Module (Data Access Layer)
- **담당자**: 이태윤
- **브랜치**: `feature/database-setup`
- **주요 컴포넌트**:
  - `DatabaseManager`: SQLite 연결 관리
  - `MaterialRepository`: 재료 데이터 CRUD
  - `TestResultRepository`: 시험 결과 데이터 CRUD
  - `StandardPropertiesRepository`: 표준 물성 데이터 관리

#### 2.2.6 Validation Module (Business Logic Layer)
- **담당자**: 박성빈
- **브랜치**: `feature/data-validation`
- **주요 컴포넌트**:
  - `InputValidator`: 사용자 입력 검증
  - `DataRangeValidator`: 데이터 범위 검증
  - `FileFormatValidator`: 파일 형식 검증

#### 2.2.7 Testing Module
- **담당자**: 박성빈
- **브랜치**: `feature/testing`
- **주요 컴포넌트**:
  - JUnit 5 기반 단위 테스트
  - 통합 테스트 시나리오
  - 품질 관리 및 버그 수정

---

## 3. 모듈 간 상호작용

### 3.1 데이터 플로우 다이어그램

```
사용자 입력
    ↓
[InputPanel] → [InputValidator]
    ↓
[StressStrainCalculator] ← [DatabaseManager]
    ↓
[MaterialPropertiesCalculator]
    ↓
[ChartGenerator] → [GraphPanel]
    ↓
[ResultPanel]
    ↓
[PDFExporter] / [ImageExporter]
```

### 3.2 제어 플로우

1. **사용자 입력 단계**:
   - 사용자가 GUI를 통해 재료 정보 및 시험 조건 입력
   - `InputValidator`가 입력값 유효성 검증

2. **데이터 처리 단계**:
   - `StressStrainCalculator`가 응력-변형률 계산
   - `MaterialPropertiesCalculator`가 물성값 산출
   - `DatabaseManager`를 통해 데이터 저장/조회

3. **시각화 단계**:
   - `ChartGenerator`가 그래프 생성
   - `GraphPanel`에 실시간 렌더링
   - `ResultPanel`에 수치 결과 표시

4. **출력 단계**:
   - `PDFExporter`를 통한 보고서 생성
   - `ImageExporter`를 통한 그래프 이미지 저장
   - `ExcelHandler`를 통한 데이터 내보내기

### 3.3 모듈 간 인터페이스

#### 3.3.1 GUI ↔ Business Logic
```java
interface CalculationService {
    StressStrainData calculate(MaterialInput input);
    MaterialProperties analyzeMaterial(StressStrainData data);
}
```

#### 3.3.2 Business Logic ↔ Data Access
```java
interface DataRepository {
    void save(MaterialData data);
    MaterialData load(String id);
    List<MaterialData> findAll();
}
```

#### 3.3.3 Visualization ↔ GUI
```java
interface ChartService {
    JFreeChart createChart(StressStrainData data);
    void updateChart(JFreeChart chart, StressStrainData data);
}
```

---

## 4. 기술 스택

### 4.1 개발 환경
- **언어**: Java 17
- **빌드 도구**: Gradle
- **IDE**: IntelliJ IDEA / Eclipse
- **버전 관리**: Git, GitHub

### 4.2 주요 라이브러리

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| Java Swing | Built-in | GUI 프레임워크 |
| JFreeChart | 1.5.4 | 그래프 시각화 |
| Apache POI | 5.2.3 | Excel 파일 처리 |
| SQLite JDBC | 3.42.0.0 | SQLite 데이터베이스 연결 |
| iText | 5.5.13 | PDF 생성 |
| JUnit 5 | 5.10.0 | 단위 테스트 |

### 4.3 데이터베이스 스키마

#### Materials 테이블
```sql
CREATE TABLE Materials (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50),
    youngs_modulus REAL,
    yield_strength REAL,
    tensile_strength REAL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### TestResults 테이블
```sql
CREATE TABLE TestResults (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    material_id INTEGER,
    test_date TIMESTAMP,
    data_file_path VARCHAR(255),
    calculated_properties TEXT,
    FOREIGN KEY (material_id) REFERENCES Materials(id)
);
```

#### StandardProperties 테이블
```sql
CREATE TABLE StandardProperties (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    material_name VARCHAR(100),
    property_name VARCHAR(100),
    property_value REAL,
    unit VARCHAR(20),
    source VARCHAR(255)
);
```

---

## 5. 시스템 제약사항 및 요구사항

### 5.1 성능 요구사항
- **응답 시간**: 사용자 입력 후 1초 이내 계산 완료 (요구사항: NF-PERF-001)
- **데이터 처리**: 최대 10,000개 데이터 포인트 실시간 처리
- **메모리 사용**: 최대 512MB 이하

### 5.2 플랫폼 요구사항
- **운영체제**: Windows 10 이상, macOS 10.15 이상, Linux (Ubuntu 20.04 이상)
- **Java 버전**: Java 17 이상 필수
- **화면 해상도**: 최소 1280x720

### 5.3 사용성 요구사항 (NF-USE-001)
- 재료공학 학부생이 10분 이내에 사용법 습득 가능
- 직관적인 메뉴 구조 및 툴팁 제공
- 단축키 지원

### 5.4 신뢰성 요구사항 (NF-QUAL-001)
- 계산 결과의 정확도: 이론값 대비 오차 1% 이내
- 데이터 무결성 보장
- 예외 처리 및 오류 복구 메커니즘

---

## 6. 배포 및 설치

### 6.1 배포 구조
```
MEVA/
├── bin/
│   └── MEVA.jar           # 실행 가능한 JAR 파일
├── lib/                   # 외부 라이브러리
├── data/
│   └── meva.db            # SQLite 데이터베이스
├── config/
│   └── application.properties
├── docs/                  # 사용자 매뉴얼
└── README.md
```

### 6.2 설치 요구사항
1. Java 17 이상 설치
2. MEVA.jar 다운로드
3. 더블클릭 실행 또는 `java -jar MEVA.jar` 명령어 실행

---

## 7. 프로젝트 통합 전략

### 7.1 브랜치 전략
- **main**: 최종 릴리스 브랜치 (김세현 관리)
- **develop**: 개발 통합 브랜치 (김세현 관리)
- **feature/**: 기능별 개발 브랜치
  - `feature/gui-design` (김종현)
  - `feature/stress-strain-calculation` (이태윤, 김세현)
  - `feature/graph-visualization` (김종현)
  - `feature/file-handler` (이태윤)
  - `feature/database-setup` (이태윤)
  - `feature/data-validation` (박성빈)
  - `feature/testing` (박성빈)

### 7.2 통합 절차
1. 각 팀원은 feature 브랜치에서 개발
2. Pull Request를 통해 develop 브랜치로 병합
3. 코드 리뷰 및 테스트 통과 후 승인
4. develop 브랜치에서 통합 테스트
5. 최종 릴리스 시 main 브랜치로 병합

### 7.3 코드 품질 관리
- **코드 리뷰**: 모든 PR은 최소 1명 이상의 리뷰 필요
- **테스트 커버리지**: 70% 이상 목표
- **코딩 컨벤션**: Java 표준 코딩 규칙 준수

---

## 8. 향후 확장 계획

### 8.1 단기 확장 (v1.1)
- 다국어 지원 (영어, 한국어)
- 더 많은 재료 유형 지원
- 그래프 커스터마이징 옵션 추가

### 8.2 장기 확장 (v2.0)
- 클라우드 기반 데이터 저장 및 공유
- 머신러닝 기반 재료 특성 예측
- 웹 버전 개발
- 모바일 앱 지원

---

## 9. 참고 문서

- [요구사항 정의서](../README.md)
- [데이터베이스 ERD](meva_docs_database_erd.md)
- [CRUD 인터페이스 설계](meva_docs_crud_interface_design.md)
- [데이터 동기화 설계](meva_docs_data_sync_design.md)
- [표준 물성 데이터베이스](meva_docs_standard_properties_db.md)

---

## 10. 연락처 및 팀 정보

- **팀장**: 김세현 (신소재공학과 4학년, 2019727029)
- **이메일**: sh000917@gmail.com
- **GitHub**: [https://github.com/sehyun00/MEVA](https://github.com/sehyun00/MEVA)

---

**문서 버전**: v1.0  
**최종 수정일**: 2025-11-11  
**작성자**: 김세현 (MEVA 프로젝트 팀)
