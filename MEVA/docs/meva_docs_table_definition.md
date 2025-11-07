# MEVA — Table Definition Document

> File: `MEVA/docs/table-definition.md`

이 문서는 **MEVA(Material Evaluation & Verification Architecture)** 시스템에서 사용되는 데이터베이스 테이블의 구조, 컬럼 정의, 데이터 타입, 제약조건, 인덱스 및 설명을 상세히 정의합니다.

---

## 📘 1. MATERIAL — 재료 정보 테이블
| 항목 | 데이터 타입 | NULL 허용 | 제약조건 | 설명 |
|------|--------------|------------|-----------|------|
| material_id | SERIAL | NO | **PK** | 재료 고유 ID |
| name | VARCHAR(100) | NO | **UNIQUE** | 재료명 (예: 철, 알루미늄 등) |
| type | VARCHAR(50) | YES |  | 재료 분류 (금속, 합금 등) |
| density | REAL | YES | CHECK (density > 0) | 밀도 (kg/m³) |
| supplier | VARCHAR(255) | YES |  | 제조사 이름 |
| created_at | TIMESTAMP WITH TIME ZONE | NO | DEFAULT now() | 등록 일시 |

**인덱스:** `material_id (PK)`, `name (UNIQUE)`

---

## 📘 2. STANDARD_PROPERTY — 표준 물성값 테이블
| 항목 | 데이터 타입 | NULL 허용 | 제약조건 | 설명 |
|------|--------------|------------|-----------|------|
| std_id | SERIAL | NO | **PK** | 표준값 고유 ID |
| material_id | INT | NO | **FK → material(material_id)** | 참조 재료 |
| young_modulus | REAL | YES | CHECK (young_modulus > 0) | 영률 (GPa) |
| yield_strength | REAL | YES | CHECK (yield_strength > 0) | 항복강도 (MPa) |
| tensile_strength | REAL | YES | CHECK (tensile_strength > 0) | 인장강도 (MPa) |
| elongation | REAL | YES | CHECK (elongation >= 0) | 연신율 (%) |
| reference | TEXT | YES |  | 표준 참조 (예: ASTM E8/E8M) |

**인덱스:** `std_id (PK)`, `material_id (FK)`

---

## 📘 3. TENSILE_TEST — 인장 시험 메타데이터
| 항목 | 데이터 타입 | NULL 허용 | 제약조건 | 설명 |
|------|--------------|------------|-----------|------|
| test_id | SERIAL | NO | **PK** | 시험 고유 ID |
| material_id | INT | NO | **FK → material(material_id)** | 재료 참조 |
| user_id | INT | YES | **FK → user(user_id)** | 실험자 ID |
| area | REAL | YES | CHECK (area > 0) | 단면적 (mm²) |
| length | REAL | YES | CHECK (length > 0) | 초기 길이 (mm) |
| date_performed | DATE | YES |  | 시험 수행 일자 |
| temperature | REAL | YES |  | 시험 온도 (°C) |
| remarks | TEXT | YES |  | 비고 |

**인덱스:** `test_id (PK)`, `material_id (FK)`, `user_id (FK)`

---

## 📘 4. TENSILE_DATA — 하중-변위 시계열 데이터
| 항목 | 데이터 타입 | NULL 허용 | 제약조건 | 설명 |
|------|--------------|------------|-----------|------|
| data_id | BIGSERIAL | NO | **PK** | 데이터 고유 ID |
| test_id | INT | NO | **FK → tensile_test(test_id)** | 시험 참조 |
| load | REAL | YES |  | 하중 (N) |
| displacement | REAL | YES |  | 변위 (mm) |
| timestamp | REAL | YES |  | 시간 (s) |

**인덱스:** `(test_id, timestamp)` 복합 인덱스  
**특이사항:** 대용량 저장소 고려 (파티셔닝 / 외부 시계열 DB 연동 권장)

---

## 📘 5. SIMULATION_RESULT — 시뮬레이션 및 계산 결과
| 항목 | 데이터 타입 | NULL 허용 | 제약조건 | 설명 |
|------|--------------|------------|-----------|------|
| sim_id | SERIAL | NO | **PK** | 시뮬레이션 고유 ID |
| test_id | INT | NO | **FK → tensile_test(test_id)** | 시험 참조 |
| young_modulus_calc | REAL | YES |  | 계산된 영률 |
| yield_strength_calc | REAL | YES |  | 계산된 항복강도 |
| tensile_strength_calc | REAL | YES |  | 계산된 인장강도 |
| elongation_calc | REAL | YES |  | 계산된 연신율 |
| compare_to_std | REAL | YES |  | 표준 대비 오차율 (%) |
| created_at | TIMESTAMP WITH TIME ZONE | NO | DEFAULT now() | 생성 일시 |

**인덱스:** `sim_id (PK)`, `test_id (FK)`

---

## 📘 6. USER — 사용자 정보
| 항목 | 데이터 타입 | NULL 허용 | 제약조건 | 설명 |
|------|--------------|------------|-----------|------|
| user_id | SERIAL | NO | **PK** | 사용자 고유 ID |
| username | VARCHAR(80) | NO | **UNIQUE** | 사용자명 |
| email | VARCHAR(200) | NO | **UNIQUE** | 이메일 |
| role | VARCHAR(20) | NO | DEFAULT 'researcher' | 권한 (admin / researcher / viewer) |
| created_at | TIMESTAMP WITH TIME ZONE | NO | DEFAULT now() | 등록 일시 |

**인덱스:** `user_id (PK)`, `username (UNIQUE)`, `email (UNIQUE)`

---

## ⚙️ 7. 공통 제약조건 요약
| 제약조건 | 설명 |
|-----------|------|
| **PK (Primary Key)** | 각 테이블의 기본 키 (ID) |
| **FK (Foreign Key)** | 참조 무결성 보장 (삭제 시 CASCADE 또는 RESTRICT) |
| **UNIQUE** | 중복 방지 (재료명, 이메일 등) |
| **CHECK** | 값의 유효 범위 검사 (음수 방지 등) |
| **DEFAULT** | 등록 시 자동 값 (now()) |

---

## 📊 8. 향후 확장 고려사항
- **MULTI-TENANCY**: 조직별 데이터 분리 및 접근 제어 (organization_id 추가 가능)
- **AUDIT LOG**: 데이터 변경 이력 추적용 로그 테이블
- **MODEL_VERSION**: 시뮬레이션 알고리즘 버전 관리
- **PARTITIONING**: 대용량 시계열 데이터 분할 저장 구조 (RANGE / HASH 기반)

---

_작성일: 2025-11-07_
