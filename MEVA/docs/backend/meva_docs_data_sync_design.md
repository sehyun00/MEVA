# MEVA — Data Storage, Access, and Synchronization Design

> File: `MEVA/docs/data-sync-design.md`

본 문서는 **MEVA(Material Evaluation & Verification Architecture)** 시스템의 데이터 저장, 접근 계층, 동기화 설계를 정의합니다. 데이터의 무결성, 일관성, 성능, 및 확장성을 유지하면서 실험 데이터(인장 시험, 물성값, 시뮬레이션 결과 등)를 안전하게 관리하기 위한 구조를 다룹니다.

---

## 🧩 1. 시스템 아키텍처 개요
```
+--------------------+
|  Web UI / Client   |
+--------------------+
          |
          v
+--------------------+
|  API Gateway       |
+--------------------+
          |
          v
+--------------------+
|  Application Layer |
|  (Flask / FastAPI) |
+--------------------+
          |
          v
+--------------------+
|  ORM / Repository  |
|  (SQLAlchemy, etc) |
+--------------------+
          |
          v
+--------------------+
|  PostgreSQL DB     |
+--------------------+
```

---

## 🗂️ 2. 데이터 저장 계층 설계

### (1) 주요 저장 구조
| 데이터 유형 | 저장 위치 | 포맷 | 비고 |
|--------------|-------------|-------|------|
| 실험 메타데이터 | PostgreSQL | 정형(SQL) | `tensile_test`, `material` |
| 하중-변위 데이터 | PostgreSQL + TimescaleDB | 시계열형 | 대용량 처리 대비 |
| 시뮬레이션 결과 | PostgreSQL | JSONB + 수치형 | `simulation_result` |
| 표준 물성값 | PostgreSQL | 정형(SQL) | 기준 비교용 |
| 로그/이벤트 | File + Elastic Stack | 비정형(JSON) | 감사 및 추적 |

### (2) 파티셔닝 및 인덱싱 전략
- **파티셔닝:** `tensile_data` 테이블을 `test_id` 기반으로 파티셔닝.
- **인덱스:** `(test_id, timestamp)` 복합 인덱스.
- **Material Name**에 대한 Unique 인덱스.

### (3) 백업 및 복구 정책
- 매일 새벽 **전체 백업 (pg_dump)**
- 1시간 단위 **WAL Incremental Backup**
- 7일 단위 **보관 및 로그 회전(log rotation)**

---

## 🔍 3. 데이터 접근 계층 (Data Access Layer, DAL)

### (1) Repository 패턴
```python
class MaterialRepository:
    def __init__(self, db_session):
        self.db = db_session

    def get_by_name(self, name):
        return self.db.query(Material).filter(Material.name == name).first()

    def add(self, material):
        self.db.add(material)
        self.db.commit()

    def update_density(self, material_id, density):
        self.db.query(Material).filter(Material.id == material_id).update({Material.density: density})
        self.db.commit()
```

### (2) ORM 매핑 (SQLAlchemy)
- `Material`, `TensileTest`, `TensileData`, `SimulationResult`, `StandardProperty` 엔티티 정의.
- 관계형 매핑 (1:N, N:1) 설정.

### (3) 캐싱
- `Redis`를 활용하여 **표준 물성값**과 **최근 시뮬레이션 결과** 캐싱.
- TTL(Time To Live): 24시간.

---

## 🔄 4. 데이터 동기화 설계

### (1) 개요
- **목표:** 실험 장비 ↔ 서버 ↔ 클라우드 간의 실시간 데이터 동기화.
- **도구:** REST API + 메시지 큐 (RabbitMQ or Kafka)

### (2) 동기화 프로세스 흐름
```
[실험 장비] → [로컬 Collector] → [API Server] → [DB Insert] → [Cloud Sync]
```

### (3) 동기화 전략
| 항목 | 방법 | 주기 | 비고 |
|------|------|------|------|
| 실시간 하중 데이터 | WebSocket / MQTT | 실시간 | 장비 데이터 송신 |
| 결과 파일 업로드 | REST (POST /upload) | 수동/자동 | JSON or CSV |
| 표준 물성 업데이트 | Batch Sync | 1일 1회 | 서버 기준 |
| 캐시 동기화 | Redis Pub/Sub | 즉시 | API 서버 간 일관성 유지 |

### (4) 충돌 해결 정책
- `test_id` 기반 버전 관리 (`version INT DEFAULT 1`)
- 충돌 시:
  1. 최근 타임스탬프 우선
  2. 사용자 승인 필요시 Conflict 테이블(`data_conflict_log`) 기록

---

## ⚙️ 5. API 연동 및 데이터 검증

### (1) 주요 API 스펙
| 기능 | 메서드 | 엔드포인트 | 설명 |
|-------|---------|-------------|------|
| 실험 데이터 업로드 | POST | `/api/v1/tensile/upload` | 하중-변위 CSV 업로드 |
| 물성값 계산 요청 | POST | `/api/v1/simulation/run` | 계산 및 저장 |
| 표준값 비교 | GET | `/api/v1/compare/{test_id}` | 표준 vs 계산값 비교 |
| 캐시 초기화 | POST | `/api/v1/cache/refresh` | Redis 캐시 갱신 |

### (2) 데이터 검증 레이어
- DB 트리거를 통한 음수값 검증 (`CHECK(load >= 0)`)
- API 입력 유효성 검사 (Pydantic / Marshmallow)
- 검증 실패 시 로그 기록 및 응답 코드 `422 Unprocessable Entity`

---

## 🧠 6. 대용량 데이터 처리 및 최적화
- TimescaleDB Chunk Interval: 1일 단위.
- Batch Insert (COPY 명령어 사용) → 10x 성능 향상.
- Connection Pool 관리 (최대 20개 세션).
- 압축 정책 (TimescaleDB `compress_orderby = 'timestamp'`).

---

## 🧾 7. 장애 복구 및 일관성 보장
- 2-Phase Commit (2PC) 기반 트랜잭션.
- WAL 기반 복구 절차 (`pg_wal_replay`).
- 비정상 종료 시 자동 재처리 큐.

---

## 📦 8. 향후 확장 고려사항
- NoSQL (MongoDB) 기반 물성 그래프 확장 저장.
- Cloud Object Storage (S3 호환) 활용하여 장비별 로그 파일 보관.
- Federated Query (PostgreSQL FDW)로 실험소별 데이터 통합.

---

_작성일: 2025-11-07_

