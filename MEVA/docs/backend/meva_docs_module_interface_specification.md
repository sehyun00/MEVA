# MEVA 모듈 간 인터페이스 정의서

## 문서 정보
- **프로젝트**: MEVA (Materials Engineering Visualization and Analysis)
- **버전**: v1.0
- **작성일**: 2025-11-11
- **작성자**: MEVA 프로젝트 팀
- **목적**: MEVA 시스템의 모듈 간 통신 및 데이터 교환을 위한 인터페이스 명세

---

## 목차
1. [개요](#1-개요)
2. [인터페이스 설계 원칙](#2-인터페이스-설계-원칙)
3. [모듈 간 인터페이스 정의](#3-모듈-간-인터페이스-정의)
4. [데이터 전송 객체(DTO)](#4-데이터-전송-객체dto)
5. [예외 처리 규약](#5-예외-처리-규약)
6. [인터페이스 버전 관리](#6-인터페이스-버전-관리)

---

## 1. 개요

### 1.1 목적
본 문서는 MEVA 시스템의 각 모듈 간 상호작용을 위한 인터페이스를 정의합니다. 각 모듈은 정의된 인터페이스를 통해서만 통신하며, 이를 통해 모듈 간 결합도를 낮추고 유지보수성을 향상시킵니다.

### 1.2 적용 범위
- GUI Module ↔ Business Logic Module
- Business Logic Module ↔ Data Access Module
- Visualization Module ↔ GUI Module
- File Handler Module ↔ Business Logic Module

### 1.3 모듈 구조 개요
```
┌─────────────────────────────────────────────────────┐
│                  GUI Module                          │
│              (Presentation Layer)                    │
└─────────────────────────────────────────────────────┘
         ↕ (Interface A, D)                    
┌──────────────────────┬──────────────────────────────┐
│   Calculation Module │   Visualization Module       │
│  (Business Logic)    │   (Business Logic)          │
└──────────────────────┴──────────────────────────────┘
         ↕ (Interface B, C)                    
┌──────────────────────┬──────────────────────────────┐
│   Database Module    │   File Handler Module        │
│   (Data Access)      │   (Data Access)              │
└──────────────────────┴──────────────────────────────┘
```

---

## 2. 인터페이스 설계 원칙

### 2.1 기본 원칙
1. **추상화**: 구현 세부사항을 숨기고 필수 기능만 노출
2. **느슨한 결합**: 인터페이스를 통한 통신으로 모듈 독립성 보장
3. **단일 책임**: 각 인터페이스는 하나의 명확한 목적을 가짐
4. **확장 가능성**: 새로운 기능 추가 시 기존 인터페이스에 영향 최소화

### 2.2 명명 규칙
- **인터페이스명**: `I[기능명]Service` 형식 (예: `ICalculationService`)
- **메서드명**: 동사로 시작 (예: `calculate()`, `save()`, `load()`)
- **매개변수**: camelCase 사용
- **반환 타입**: 명확한 DTO 객체 또는 기본 타입

---

## 3. 모듈 간 인터페이스 정의

### 3.1 Interface A: GUI ↔ Calculation Module

#### 3.1.1 ICalculationService
**목적**: GUI에서 계산 모듈로 인장시험 데이터를 전달하고 계산 결과를 받음

```java
package com.meva.service;

import com.meva.dto.*;
import java.util.List;

public interface ICalculationService {
    
    /**
     * 응력-변형률 데이터 계산
     * @param input 재료 정보 및 시험 조건
     * @return 계산된 응력-변형률 데이터
     * @throws ValidationException 입력 데이터 유효성 검증 실패 시
     * @throws CalculationException 계산 중 오류 발생 시
     */
    StressStrainData calculateStressStrain(MaterialInput input) 
        throws ValidationException, CalculationException;
    
    /**
     * 재료 물성값 분석 및 산출
     * @param data 응력-변형률 데이터
     * @return 계산된 물성값 (영률, 항복강도, 인장강도 등)
     * @throws CalculationException 계산 중 오류 발생 시
     */
    MaterialProperties analyzeMaterial(StressStrainData data) 
        throws CalculationException;
    
    /**
     * 인장시험 시뮬레이션 실행
     * @param config 시뮬레이션 설정
     * @return 시뮬레이션 결과 데이터
     * @throws SimulationException 시뮬레이션 실행 중 오류 발생 시
     */
    SimulationResult runSimulation(SimulationConfig config) 
        throws SimulationException;
    
    /**
     * 계산 진행 상태 조회
     * @return 진행률 (0-100)
     */
    int getCalculationProgress();
    
    /**
     * 계산 취소
     */
    void cancelCalculation();
}
```

#### 3.1.2 IValidationService
**목적**: 사용자 입력 데이터 검증

```java
package com.meva.service;

import com.meva.dto.ValidationResult;

public interface IValidationService {
    
    /**
     * 재료 입력 데이터 검증
     * @param input 검증할 입력 데이터
     * @return 검증 결과 (성공 여부 및 오류 메시지)
     */
    ValidationResult validateMaterialInput(MaterialInput input);
    
    /**
     * 파일 데이터 검증
     * @param filePath 파일 경로
     * @return 검증 결과
     */
    ValidationResult validateFileData(String filePath);
    
    /**
     * 데이터 범위 검증
     * @param value 검증할 값
     * @param min 최소값
     * @param max 최대값
     * @return 검증 성공 여부
     */
    boolean validateRange(double value, double min, double max);
}
```

---

### 3.2 Interface B: Calculation ↔ Database Module

#### 3.2.1 IMaterialRepository
**목적**: 재료 데이터 CRUD 작업

```java
package com.meva.repository;

import com.meva.model.Material;
import java.util.List;
import java.util.Optional;

public interface IMaterialRepository {
    
    /**
     * 재료 데이터 저장
     * @param material 저장할 재료 객체
     * @return 저장된 재료 ID
     * @throws DatabaseException 데이터베이스 작업 실패 시
     */
    long save(Material material) throws DatabaseException;
    
    /**
     * ID로 재료 조회
     * @param id 재료 ID
     * @return 재료 객체 (Optional)
     */
    Optional<Material> findById(long id);
    
    /**
     * 이름으로 재료 조회
     * @param name 재료 이름
     * @return 재료 객체 (Optional)
     */
    Optional<Material> findByName(String name);
    
    /**
     * 모든 재료 조회
     * @return 재료 리스트
     */
    List<Material> findAll();
    
    /**
     * 재료 정보 업데이트
     * @param material 업데이트할 재료 객체
     * @return 업데이트 성공 여부
     */
    boolean update(Material material) throws DatabaseException;
    
    /**
     * 재료 삭제
     * @param id 삭제할 재료 ID
     * @return 삭제 성공 여부
     */
    boolean delete(long id) throws DatabaseException;
    
    /**
     * 재료 존재 여부 확인
     * @param name 재료 이름
     * @return 존재 여부
     */
    boolean exists(String name);
}
```

#### 3.2.2 ITestResultRepository
**목적**: 시험 결과 데이터 관리

```java
package com.meva.repository;

import com.meva.model.TestResult;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface ITestResultRepository {
    
    /**
     * 시험 결과 저장
     * @param result 시험 결과 객체
     * @return 저장된 결과 ID
     */
    long save(TestResult result) throws DatabaseException;
    
    /**
     * 시험 결과 조회
     * @param id 결과 ID
     * @return 시험 결과 객체
     */
    Optional<TestResult> findById(long id);
    
    /**
     * 특정 재료의 모든 시험 결과 조회
     * @param materialId 재료 ID
     * @return 시험 결과 리스트
     */
    List<TestResult> findByMaterialId(long materialId);
    
    /**
     * 날짜 범위로 시험 결과 조회
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 시험 결과 리스트
     */
    List<TestResult> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 시험 결과 삭제
     * @param id 결과 ID
     * @return 삭제 성공 여부
     */
    boolean delete(long id) throws DatabaseException;
}
```

#### 3.2.3 IStandardPropertiesRepository
**목적**: 표준 물성 데이터 조회

```java
package com.meva.repository;

import com.meva.model.StandardProperty;
import java.util.List;
import java.util.Optional;

public interface IStandardPropertiesRepository {
    
    /**
     * 재료명으로 표준 물성 조회
     * @param materialName 재료 이름
     * @return 표준 물성 리스트
     */
    List<StandardProperty> findByMaterialName(String materialName);
    
    /**
     * 특정 물성값 조회
     * @param materialName 재료 이름
     * @param propertyName 물성 이름 (예: "Young's Modulus")
     * @return 표준 물성 객체
     */
    Optional<StandardProperty> findByMaterialAndProperty(
        String materialName, String propertyName);
    
    /**
     * 모든 표준 물성 조회
     * @return 표준 물성 리스트
     */
    List<StandardProperty> findAll();
}
```

---

### 3.3 Interface C: Calculation ↔ File Handler Module

#### 3.3.1 IFileImportService
**목적**: 외부 파일에서 데이터 가져오기

```java
package com.meva.service;

import com.meva.dto.ImportedData;
import java.io.File;

public interface IFileImportService {
    
    /**
     * CSV 파일에서 데이터 읽기
     * @param file CSV 파일
     * @return 파싱된 데이터
     * @throws FileFormatException 파일 형식 오류 시
     */
    ImportedData readCSV(File file) throws FileFormatException;
    
    /**
     * Excel 파일에서 데이터 읽기
     * @param file Excel 파일
     * @return 파싱된 데이터
     * @throws FileFormatException 파일 형식 오류 시
     */
    ImportedData readExcel(File file) throws FileFormatException;
    
    /**
     * 지원되는 파일 형식 확인
     * @param fileName 파일 이름
     * @return 지원 여부
     */
    boolean isSupportedFormat(String fileName);
    
    /**
     * 파일 미리보기 (첫 10행)
     * @param file 파일
     * @return 미리보기 데이터
     */
    List<String[]> previewFile(File file) throws FileFormatException;
}
```

#### 3.3.2 IFileExportService
**목적**: 계산 결과를 파일로 내보내기

```java
package com.meva.service;

import com.meva.dto.*;
import java.io.File;

public interface IFileExportService {
    
    /**
     * 데이터를 CSV 파일로 내보내기
     * @param data 내보낼 데이터
     * @param outputPath 출력 파일 경로
     * @throws ExportException 내보내기 실패 시
     */
    void exportToCSV(StressStrainData data, String outputPath) 
        throws ExportException;
    
    /**
     * 데이터를 Excel 파일로 내보내기
     * @param data 내보낼 데이터
     * @param outputPath 출력 파일 경로
     * @throws ExportException 내보내기 실패 시
     */
    void exportToExcel(StressStrainData data, String outputPath) 
        throws ExportException;
    
    /**
     * 보고서를 PDF로 생성
     * @param report 보고서 데이터
     * @param outputPath 출력 파일 경로
     * @throws ExportException 생성 실패 시
     */
    void generatePDFReport(ReportData report, String outputPath) 
        throws ExportException;
    
    /**
     * 그래프를 이미지 파일로 저장
     * @param chart 그래프 객체
     * @param outputPath 출력 파일 경로
     * @param format 이미지 형식 (PNG, JPG, SVG)
     * @throws ExportException 저장 실패 시
     */
    void exportChartAsImage(Object chart, String outputPath, String format) 
        throws ExportException;
}
```

---

### 3.4 Interface D: Visualization ↔ GUI Module

#### 3.4.1 IChartService
**목적**: 그래프 생성 및 렌더링

```java
package com.meva.service;

import org.jfree.chart.JFreeChart;
import com.meva.dto.*;

public interface IChartService {
    
    /**
     * 응력-변형률 곡선 그래프 생성
     * @param data 응력-변형률 데이터
     * @return JFreeChart 객체
     */
    JFreeChart createStressStrainChart(StressStrainData data);
    
    /**
     * 그래프 업데이트 (실시간 데이터 추가)
     * @param chart 기존 차트 객체
     * @param newData 추가할 데이터
     */
    void updateChart(JFreeChart chart, StressStrainData newData);
    
    /**
     * 그래프 스타일 설정
     * @param chart 차트 객체
     * @param style 스타일 설정
     */
    void applyChartStyle(JFreeChart chart, ChartStyle style);
    
    /**
     * 여러 데이터셋을 하나의 그래프에 표시
     * @param datasets 데이터셋 리스트
     * @return JFreeChart 객체
     */
    JFreeChart createComparisonChart(List<StressStrainData> datasets);
    
    /**
     * 그래프 범례 추가/수정
     * @param chart 차트 객체
     * @param legends 범례 정보
     */
    void setLegends(JFreeChart chart, List<String> legends);
}
```

---

## 4. 데이터 전송 객체(DTO)

### 4.1 MaterialInput
```java
package com.meva.dto;

public class MaterialInput {
    private String materialName;        // 재료 이름
    private String materialType;        // 재료 유형 (철, 알루미늄 등)
    private double originalArea;        // 원래 단면적 (mm²)
    private double originalLength;      // 원래 길이 (mm)
    private double testTemperature;     // 시험 온도 (°C)
    private String testStandard;        // 시험 표준 (ASTM, KS 등)
    
    // Getters and Setters
    // ...
}
```

### 4.2 StressStrainData
```java
package com.meva.dto;

import java.util.List;

public class StressStrainData {
    private List<Double> strainValues;      // 변형률 데이터
    private List<Double> stressValues;      // 응력 데이터 (MPa)
    private List<Double> loadValues;        // 하중 데이터 (N)
    private List<Double> displacementValues; // 변위 데이터 (mm)
    private int dataPointCount;             // 데이터 포인트 수
    
    // Getters and Setters
    // ...
}
```

### 4.3 MaterialProperties
```java
package com.meva.dto;

public class MaterialProperties {
    private double youngsModulus;           // 영률 (GPa)
    private double yieldStrength;           // 항복강도 (MPa)
    private double tensileStrength;         // 인장강도 (MPa)
    private double elongation;              // 연신율 (%)
    private double reductionOfArea;         // 단면수축률 (%)
    private double poissonRatio;            // 푸아송 비
    private String materialGrade;           // 재료 등급
    
    // Getters and Setters
    // ...
}
```

### 4.4 ValidationResult
```java
package com.meva.dto;

import java.util.List;

public class ValidationResult {
    private boolean isValid;                // 검증 성공 여부
    private List<String> errorMessages;     // 오류 메시지 목록
    private List<String> warningMessages;   // 경고 메시지 목록
    
    // Getters and Setters
    // ...
}
```

### 4.5 SimulationConfig
```java
package com.meva.dto;

public class SimulationConfig {
    private double loadRate;                // 하중 속도 (N/s)
    private double maxLoad;                 // 최대 하중 (N)
    private int stepCount;                  // 시뮬레이션 스텝 수
    private boolean includeElasticRegion;   // 탄성 영역 포함 여부
    private boolean includePlasticRegion;   // 소성 영역 포함 여부
    
    // Getters and Setters
    // ...
}
```

### 4.6 ReportData
```java
package com.meva.dto;

import java.time.LocalDateTime;

public class ReportData {
    private String reportTitle;             // 보고서 제목
    private LocalDateTime generatedDate;    // 생성 날짜
    private MaterialInput inputData;        // 입력 데이터
    private MaterialProperties properties;  // 계산된 물성
    private StressStrainData chartData;     // 그래프 데이터
    private String remarks;                 // 비고
    
    // Getters and Setters
    // ...
}
```

---

## 5. 예외 처리 규약

### 5.1 예외 계층 구조
```
MEVAException (최상위)
├── ValidationException (입력 검증 오류)
├── CalculationException (계산 오류)
├── DatabaseException (데이터베이스 오류)
├── FileFormatException (파일 형식 오류)
├── ExportException (내보내기 오류)
└── SimulationException (시뮬레이션 오류)
```

### 5.2 예외 처리 원칙
1. **체크 예외 사용**: 모든 모듈 인터페이스는 체크 예외를 명시적으로 선언
2. **의미 있는 메시지**: 예외 발생 시 사용자가 이해할 수 있는 메시지 제공
3. **로깅**: 모든 예외는 로그에 기록
4. **복구 가능성**: 가능한 경우 예외 복구 메커니즘 제공

### 5.3 예외 클래스 예시
```java
package com.meva.exception;

public class ValidationException extends MEVAException {
    private List<String> validationErrors;
    
    public ValidationException(String message, List<String> errors) {
        super(message);
        this.validationErrors = errors;
    }
    
    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
```

---

## 6. 인터페이스 버전 관리

### 6.1 버전 정책
- **Major 버전**: 호환성이 깨지는 변경 (메서드 시그니처 변경, 삭제)
- **Minor 버전**: 하위 호환 가능한 기능 추가
- **Patch 버전**: 버그 수정 및 내부 구현 변경

### 6.2 버전 표기
```java
/**
 * @version 1.0.0
 * @since 2025-11-11
 */
public interface ICalculationService {
    // ...
}
```

### 6.3 Deprecated 처리
```java
/**
 * @deprecated Use {@link #calculateStressStrain(MaterialInput)} instead
 */
@Deprecated
StressStrainData calculate(double area, double length);
```

---

## 7. 인터페이스 구현 가이드라인

### 7.1 구현 클래스 명명 규칙
- 인터페이스 이름에서 'I' 제거 후 'Impl' 추가
- 예: `ICalculationService` → `CalculationServiceImpl`

### 7.2 구현 시 주의사항
1. **스레드 안전성**: 멀티스레드 환경 고려
2. **리소스 관리**: 파일, DB 연결 등 적절히 해제
3. **성능 최적화**: 대용량 데이터 처리 시 메모리 효율 고려
4. **로깅**: 주요 작업 수행 시 로그 기록

### 7.3 단위 테스트 작성
```java
@Test
public void testCalculateStressStrain() {
    // Given
    MaterialInput input = new MaterialInput();
    input.setOriginalArea(100.0);
    input.setOriginalLength(50.0);
    
    // When
    StressStrainData result = service.calculateStressStrain(input);
    
    // Then
    assertNotNull(result);
    assertTrue(result.getDataPointCount() > 0);
}
```

---

## 8. 통합 테스트 시나리오

### 8.1 시나리오 1: 전체 플로우 테스트
```
1. GUI에서 재료 정보 입력
   ↓ (Interface A: ICalculationService)
2. Calculation Module에서 계산 수행
   ↓ (Interface B: IMaterialRepository)
3. Database에 결과 저장
   ↓ (Interface D: IChartService)
4. Visualization Module에서 그래프 생성
   ↓
5. GUI에 결과 표시
```

### 8.2 시나리오 2: 파일 가져오기 및 분석
```
1. 사용자가 CSV 파일 선택
   ↓ (Interface C: IFileImportService)
2. File Handler에서 데이터 파싱
   ↓ (Interface A: IValidationService)
3. 데이터 검증
   ↓ (Interface A: ICalculationService)
4. 계산 수행 및 결과 저장
   ↓ (Interface C: IFileExportService)
5. PDF 보고서 생성
```

---

## 9. 변경 이력

| 버전 | 날짜 | 변경 내용 | 작성자 |
|------|------|-----------|--------|
| 1.0 | 2025-11-11 | 초안 작성 | MEVA 팀 |

---

## 10. 참고 문서

- [시스템 아키텍처 문서](system-architecture.md)
- [CRUD 인터페이스 설계](meva_docs_crud_interface_design.md)
- [데이터베이스 ERD](meva_docs_database_erd.md)
- [데이터 동기화 설계](meva_docs_data_sync_design.md)

---

**문서 버전**: v1.0  
**최종 수정일**: 2025-11-11  
**작성자**: MEVA 프로젝트 팀  
**GitHub**: [https://github.com/sehyun00/MEVA](https://github.com/sehyun00/MEVA)