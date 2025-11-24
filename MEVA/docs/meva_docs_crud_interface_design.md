# MEVA — CRUD Interface Design

> File: `MEVA/docs/crud-interface-design.md`

본 문서는 **MEVA(Material Evaluation & Verification Architecture)** 시스템의 CRUD (Create, Read, Update, Delete) 인터페이스 설계를 정의합니다. 각 데이터 엔티티(Material, Test, Simulation 등)에 대한 접근 로직, API 설계, 데이터 검증 절차, 트랜잭션 처리 방식을 포함합니다.

---

## 🧩 1. 설계 개요
CRUD 인터페이스는 **API 계층**과 **데이터 접근 계층(Repository)** 로 분리되어 있으며, 다음 구조로 동작합니다.

```
[Frontend/UI]
   ↓
[REST API / FastAPI]
   ↓
[CRUD Service Layer]
   ↓
[ORM / SQLAlchemy Repository]
   ↓
[PostgreSQL Database]
```

---

## 📘 2. 주요 엔티티별 CRUD API 설계

### (1) Material (재료 정보)
| 작업 | HTTP Method | Endpoint | 요청 파라미터 | 설명 |
|------|--------------|-----------|----------------|------|
| Create | POST | `/api/v1/materials` | name, type, density, supplier | 새 재료 등록 |
| Read (list) | GET | `/api/v1/materials` | optional: name/type filter | 전체 혹은 조건 검색 |
| Read (detail) | GET | `/api/v1/materials/{id}` | id | 특정 재료 조회 |
| Update | PUT | `/api/v1/materials/{id}` | density, supplier 등 | 재료 정보 수정 |
| Delete | DELETE | `/api/v1/materials/{id}` | id | 재료 삭제 |

#### ✅ Validation Rules
- `density > 0`
- `name` 중복 불가 (UNIQUE)
- 삭제 시 `CASCADE` → 관련 시험 데이터 자동 삭제 금지 (경고 후 수행)

---

### (2) Tensile Test (인장 시험)
| 작업 | HTTP Method | Endpoint | 요청 파라미터 | 설명 |
|------|--------------|-----------|----------------|------|
| Create | POST | `/api/v1/tensile-tests` | material_id, area, length, date_performed, temperature | 시험 생성 |
| Read | GET | `/api/v1/tensile-tests` | optional: material_id | 전체 혹은 특정 재료 시험 목록 조회 |
| Read (detail) | GET | `/api/v1/tensile-tests/{id}` | id | 시험 상세 조회 |
| Update | PUT | `/api/v1/tensile-tests/{id}` | temperature, remarks | 시험 메타데이터 수정 |
| Delete | DELETE | `/api/v1/tensile-tests/{id}` | id | 시험 삭제 |

#### ✅ Validation Rules
- `area`, `length` > 0
- `temperature` 합리적 범위 (-50 ~ 1000°C)

---

### (3) Tensile Data (하중-변위 데이터)
| 작업 | HTTP Method | Endpoint | 요청 파라미터 | 설명 |
|------|--------------|-----------|----------------|------|
| Bulk Insert | POST | `/api/v1/tensile-data/bulk` | CSV/JSON 업로드 | 대용량 실험 데이터 입력 |
| Read (by test) | GET | `/api/v1/tensile-data/{test_id}` | test_id | 특정 시험의 하중-변위 곡선 조회 |
| Delete | DELETE | `/api/v1/tensile-data/{test_id}` | test_id | 특정 시험 데이터 일괄 삭제 |

#### ✅ Validation Rules
- `load, displacement >= 0`
- 데이터 수 ≥ 2 (최소 2개 측정점 필요)
- 중복 타임스탬프 금지

---

### (4) Simulation Result (시뮬레이션 결과)
| 작업 | HTTP Method | Endpoint | 요청 파라미터 | 설명 |
|------|--------------|-----------|----------------|------|
| Create | POST | `/api/v1/simulations` | test_id, young_modulus_calc, yield_strength_calc, etc. | 계산 결과 저장 |
| Read | GET | `/api/v1/simulations` | optional: test_id | 전체/특정 시험 결과 조회 |
| Update | PUT | `/api/v1/simulations/{id}` | compare_to_std 등 | 계산값 수정 |
| Delete | DELETE | `/api/v1/simulations/{id}` | id | 결과 삭제 |

#### ✅ Validation Rules
- `compare_to_std`는 ±100% 범위 이내
- `test_id` 유효성 확인 필요 (`FK` 존재 검사)

---

## ⚙️ 3. 내부 CRUD Service 계층 설계 (Python 예시)
```python
class MaterialService:
    def __init__(self, repo):
        self.repo = repo

    def create_material(self, data):
        if data.density <= 0:
            raise ValueError('Density must be positive')
        if self.repo.exists(name=data.name):
            raise ValueError('Material already exists')
        self.repo.add(Material(**data))

    def update_material(self, id, data):
        mat = self.repo.get(id)
        if not mat:
            raise ValueError('Material not found')
        mat.supplier = data.supplier or mat.supplier
        mat.density = data.density or mat.density
        self.repo.commit()
```

---

## 🧠 4. 트랜잭션 처리 및 예외 관리
| 상황 | 처리 방식 |
|------|-------------|
| 데이터 입력 중 실패 | 전체 롤백 (`session.rollback()`) |
| FK 위반 발생 | `409 Conflict` 반환 |
| 중복 키 오류 | `422 Unprocessable Entity` |
| 논리적 제약 위반 (음수, 범위 초과) | ValidationError 처리 |

---

## 🔒 5. 권한 및 접근 제어
| 역할 | 권한 |
|-------|-------|
| admin | 전체 CRUD 가능 |
| researcher | Read, Create, Update 가능 / Delete 제한 |
| viewer | Read Only |

API 토큰 또는 JWT 기반 인증 후, 각 요청에 대해 `role` 기반 접근 제어 수행.

---

## 🧾 6. 응답 포맷 규격 (JSON)
```json
{
  "status": "success",
  "data": {
    "id": 1,
    "name": "철",
    "density": 7850,
    "created_at": "2025-11-07T12:00:00Z"
  },
  "message": "Material created successfully"
}
```

오류 응답 예시:
```json
{
  "status": "error",
  "code": 422,
  "message": "Density must be positive"
}
```

---

## 📡 7. 비동기 CRUD 처리 (옵션)
- 대용량 `tensile_data` 업로드 시 비동기 큐(RabbitMQ / Celery) 사용.
- 상태 조회 API 제공 (`/api/v1/tasks/{id}`)

---

## 🧩 8. 테스트 케이스 설계
| 테스트 항목 | 입력 | 기대 결과 |
|--------------|------|-------------|
| 재료 생성 | 철, 7850 | 생성 성공 (201) |
| 음수 밀도 입력 | -100 | 422 오류 |
| 없는 재료 조회 | id=999 | 404 Not Found |
| 시험 삭제 시 연관 데이터 | test_id 연결 존재 | 409 Conflict |

---

_작성일: 2025-11-07_