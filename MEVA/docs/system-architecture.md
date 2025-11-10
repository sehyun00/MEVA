# MEVA 시스템 아키텍처 설계서

## 1. 개요

**프로젝트명**: MEVA (Materials Engineering Visualization and Analysis)
**작성일**: 2025년 11월 10일
**작성자**: 김세현
**버전**: v1.0
**요구사항 ID**: SYS-ENV-001

## 2. 시스템 구조

### 2.1 전체 아키텍처

MEVA는 3계층 아키텍처(Presentation - Business - Data)를 기반으로 설계되었습니다.

```
[Presentation Layer]
    |
    v
[Business Logic Layer]
    |
    v
[Data Access Layer]
    |
    v
[Database (SQLite)]
```

### 2.2 주요 모듈

#### 2.2.1 GUI 모듈 (Presentation Layer)
- **패키지**: `meva.gui`
- **기술**: Java Swing
- **주요 클래스**:
  - `MainFrame`: 메인 윈도우
  - `InputPanel`: 입력 폼 패널
  - `GraphPanel`: 그래프 시각화 패널
  - `ResultPanel`: 결과 표시 패널

#### 2.2.2 시뮬레이션 모듈 (Business Logic Layer)
- **패키지**: `meva.simulation`
- **주요 클래스**:
  - `StressStrainSimulator`: 응력-변형률 시뮬레이션
  - `MaterialBehaviorModel`: 재료 거동 모델

#### 2.2.3 계산 모듈 (Business Logic Layer)
- **패키지**: `meva.calculation`
- **주요 클래스**:
  - `PropertyCalculator`: 물성값 계산기
  - `YoungsModulusCalculator`: 영률 계산
  - `YieldStrengthCalculator`: 항복강도 계산
  - `TensileStrengthCalculator`: 인장강도 계산

#### 2.2.4 시각화 모듈 (Presentation Layer)
- **패키지**: `meva.visualization`
- **라이브러리**: JFreeChart
- **주요 클래스**:
  - `GraphRenderer`: 그래프 렌더러
  - `DataPlotter`: 데이터 플로팅

#### 2.2.5 데이터베이스 모듈 (Data Access Layer)
- **패키지**: `meva.database`
- **기술**: SQLite, JDBC
- **주요 클래스**:
  - `DatabaseManager`: DB 연결 관리
  - `MaterialDataDAO`: 재료 데이터 DAO
  - `ExperimentDAO`: 실험 데이터 DAO

#### 2.2.6 파일 처리 모듈 (Business Logic Layer)
- **패키지**: `meva.fileio`
- **주요 클래스**:
  - `CSVReader`: CSV 파일 읽기
  - `ExcelReader`: Excel 파일 읽기
  - `ReportGenerator`: 보고서 생성

#### 2.2.7 데이터 검증 모듈 (Business Logic Layer)
- **패키지**: `meva.validation`
- **주요 클래스**:
  - `InputValidator`: 입력값 검증
  - `DataValidator`: 데이터 유효성 검증

#### 2.2.8 유틸리티 모듈 (Common)
- **패키지**: `meva.utils`
- **주요 클래스**:
  - `Constants`: 시스템 상수
  - `ConfigManager`: 설정 관리
  - `Logger`: 로깅 유틸리티

## 3. 모듈 간 상호작용

### 3.1 데이터 플로우

```
사용자 입력 (GUI)
    ↓
입력 검증 (Validation)
    ↓
시뮬레이션 실행 (Simulation)
    ↓
물성값 계산 (Calculation)
    ↓
결과 저장 (Database)
    ↓
그래프 시각화 (Visualization)
    ↓
결과 표시/내보내기 (GUI/FileIO)
```

### 3.2 제어 플로우

1. **초기화 단계**:
   - `Main.java` → `ConfigManager` → `DatabaseManager`
   - 설정 로드 및 DB 연결 초기화

2. **입력 단계**:
   - `MainFrame` → `InputPanel` → `InputValidator`
   - 사용자 입력 수집 및 검증

3. **처리 단계**:
   - `InputValidator` → `StressStrainSimulator` → `PropertyCalculator`
   - 시뮬레이션 실행 및 물성값 계산

4. **저장 단계**:
   - `PropertyCalculator` → `ExperimentDAO` → SQLite DB
   - 결과 데이터베이스 저장

5. **출력 단계**:
   - `GraphRenderer` → `GraphPanel` → `ResultPanel`
   - 시각화 및 결과 표시

## 4. 개발 환경 요구사항

### 4.1 필수 요구사항
- **JDK**: Java 17 이상
- **IDE**: Eclipse 또는 IntelliJ IDEA
- **빌드 도구**: Maven (선택사항)
- **데이터베이스**: SQLite 3.x
- **버전 관리**: Git

### 4.2 의존성 라이브러리
```xml
<!-- JFreeChart -->
<dependency>
    <groupId>org.jfree</groupId>
    <artifactId>jfreechart</artifactId>
    <version>1.5.4</version>
</dependency>

<!-- SQLite JDBC -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.43.0.0</version>
</dependency>

<!-- Apache POI (Excel 처리) -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.3</version>
</dependency>
```

## 5. 디렉토리 구조

```
MEVA/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── meva/
│   │   │       ├── Main.java
│   │   │       ├── gui/
│   │   │       ├── simulation/
│   │   │       ├── calculation/
│   │   │       ├── visualization/
│   │   │       ├── database/
│   │   │       ├── fileio/
│   │   │       ├── validation/
│   │   │       ├── models/
│   │   │       └── utils/
│   │   └── resources/
│   │       ├── config/
│   │       ├── data/
│   │       └── images/
│   └── test/
│       └── java/
│           └── meva/
├── docs/
├── lib/
└── bin/
```

## 6. 성능 요구사항

- **응답 시간**: 사용자 입력 후 1초 이내 결과 표시
- **그래프 렌더링**: 500ms 이내
- **데이터 로드**: 대용량 파일(10MB) 3초 이내 처리
- **메모리 사용**: 최대 512MB
- **동시 사용자**: 단일 사용자 (로컬 애플리케이션)

## 7. 시스템 제약사항

- **플랫폼**: Windows, macOS, Linux (Java 17 지원 플랫폼)
- **화면 해상도**: 최소 1280x720
- **디스크 공간**: 최소 100MB
- **네트워크**: 오프라인 사용 가능 (네트워크 불필요)

## 8. 보안 고려사항

- 로컬 데이터베이스 사용으로 네트워크 보안 위험 최소화
- 사용자 입력 검증을 통한 SQL Injection 방지
- 파일 경로 검증을 통한 Path Traversal 방지

## 9. 확장성

### 9.1 수평 확장
- 플러그인 아키텍처를 통한 새로운 시뮬레이션 모델 추가
- 인터페이스 기반 설계로 모듈 교체 용이

### 9.2 수직 확장
- 다양한 재료 모델 추가 가능
- 새로운 파일 형식 지원 추가 가능
- 추가 분석 기능 통합 가능

## 10. 통합 및 배포

### 10.1 브랜치 전략
- **main**: 최종 릴리스 버전
- **develop**: 개발 통합 브랜치
- **feature/***: 기능별 개발 브랜치

### 10.2 통합 절차
1. 기능 개발 완료 후 feature 브랜치에서 develop으로 PR
2. 코드 리뷰 및 테스트 통과
3. develop에 병합
4. 통합 테스트 완료 후 main으로 병합

### 10.3 배포 방법
- JAR 파일로 패키징
- 독립 실행 가능한 실행 파일 생성 (선택사항)
- 설치 가이드 및 사용자 매뉴얼 제공

## 11. 유지보수 및 모니터링

- 로그 파일 생성 (`logs/meva.log`)
- 에러 추적 및 스택 트레이스 기록
- 성능 메트릭 수집 (선택사항)

## 12. 참고 문서

- 요구사항 정의서
- 모듈 간 인터페이스 정의서
- 데이터베이스 스키마 설계서
- API 문서
