# MEVA — 대용량 데이터 최적화 방안

> File: `MEVA/docs/data-optimization.md`

이 문서는 **MEVA(Material Evaluation & Verification Architecture)** 시스템에서 인장시험 시계열(하중-변위) 및 대용량 실험 데이터를 효율적으로 저장, 조회, 처리하기 위한 최적화 전략과 구체적 실행 방안을 제시합니다. 목표는 성능(처리속도, 응답시간), 비용(스토리지, I/O), 그리고 메모리 사용량을 균형있게 최적화하는 것입니다.

---

## 목차
1. 설계 목표 및 요구조건
2. 아키텍처 선택 가이드
3. 저장소 레벨 최적화
4. 인제스션(ingestion) 전략
5. 조회 및 집계 최적화
6. 데이터 보관(휴지/요약) 전략
7. 메모리 사용량 최적화 (애플리케이션/DB)
8. 운영·모니터링·벤치마크
9. 예제 스크립트 및 설정
10. 권장 스택 요약

---

## 1. 설계 목표 및 요구조건
- **데이터 특성:** 초당 샘플 빈도 100Hz 이상의 시계열(하중, 변위) — 한 시험당 수만 ~ 수백만 행 가능
- **필요 기능:** 빠른 업로드(배치), 실시간/준실시간 시각화(청크 스트리밍), 대규모 집계(요약 통계), 표준값 비교, 장기 보존
- **제약:** 서버 메모리 제약(예: 32~64GB), 디스크 I/O 비용, 네트워크 대역폭

목표: 대용량 데이터에 대해 안정적인 쓰기 처리량과 낮은 읽기 지연을 보장하면서 메모리 소비를 최소화한다.

---

## 2. 아키텍처 선택 가이드
- **시계열 전용 DB 권장:** TimescaleDB (PostgreSQL 확장), ClickHouse, InfluxDB 등.
  - TimescaleDB: PostgreSQL 에코시스템 장점(복잡한 JOIN, ACID). 대량 시계열 데이터의 chunking & compression 지원.
  - ClickHouse: 대규모 집계 및 분석 쿼리에 유리. 컬럼 기반으로 I/O 절감.
- **하이브리드 전략:** 메타데이터(시험, 재료, 표준값) → PostgreSQL, 원시 시계열 → TimescaleDB / ClickHouse
- **객체 스토리지 활용:** 원시 업로드 파일(Parquet/CSV)은 S3에 보관하고, 필요시 재적재.

---

## 3. 저장소 레벨 최적화
### 3.1 파티셔닝(Partitioning)
- PostgreSQL: `tensile_data`를 `test_id` 또는 날짜 범위(date_trunc('day', timestamp)) 기준으로 파티셔닝.
- TimescaleDB: hypertable로 전환하여 자동 chunking (recommend: chunk_time_interval = 1 day 또는 chunk_target_size)

장점: 쿼리 스캔 범위 축소, VACUUM/maintenance 단위 축소.

### 3.2 압축(Compression)
- TimescaleDB: `compress_chunk` 사용 — 컬럼형 압축으로 디스크 사용량 대폭 절감.
- ClickHouse: 기본적으로 컬럼 압축을 사용.
- PostgreSQL: 장기 보관용 테이블을 Parquet로 추출하여 S3에 저장.

### 3.3 적절한 데이터 타입
- 시간을 `DOUBLE PRECISION` 대신 `TIMESTAMP` 또는 `DOUBLE`로 필요에 따라 선택.
- 부동소수점은 `REAL`보다 `DOUBLE PRECISION`이 정밀도/성능 균형에 유리하지만 저장공간을 고려.

### 3.4 인덱스 전략
- 핵심 인덱스: `(test_id, timestamp)` 복합 인덱스
- 시계열 DB에서는 timestamp 기반 파티셔닝 + 적은 수의 보조 인덱스 권장
- ClickHouse: primary key는 정렬 키 역할 — `ORDER BY (test_id, timestamp)` 권장

---

## 4. 인제스션(ingestion) 전략
### 4.1 배치 업로드 (Batching)
- 대량 데이터는 **COPY** (PostgreSQL) 또는 bulk insert (ClickHouse) 사용
- 서버에서 행 단위 INSERT를 피하고, 1k~100k 행 단위 청크로 전송

### 4.2 스트리밍 및 실시간 업로드
- 장비 → 에지(Collector) → 메시지 큐(Kafka) → Consumer(Worker) → DB
- 백프레셔(backpressure): 메시지 큐 길이/consumer 처리율 모니터링
- WebSocket/MQTT 연동 시 청크 전송(예: 1초 단위)로 메모리 및 네트워크 절약

### 4.3 데이터 검증(서버측)
- 청크 단위로 유효성 검사(음수 필드, 중복 timestamp 등) 수행 후 DB 적재
- 잘못된 행은 별도 파일/테이블에 기록하여 재처리

### 4.4 병렬화 및 커넥션 풀링
- 병렬 worker (ex. 4~8 workers)로 청크 병렬 삽입
- DB connection pool 크기와 worker 수 조율 (pool_size ≈ workers + spare)

---

## 5. 조회 및 집계 최적화
### 5.1 다운샘플링 및 요약 테이블 (Materialized View)
- 실시간 시각화: 원시 데이터를 그대로 읽지 말고, 주기적 다운샘플링(1ms→10ms/100ms/1s) 테이블 제공
- 요약 테이블: `daily_summary(test_id, min_load, max_load, avg_load)` 같은 집계 테이블 유지
- Materialized view + 트리거 또는 주기적 배치로 갱신

### 5.2 지연 쿼리(Chunked Query) 응답
- UI는 `/data?start=&end=&limit=1000&offset=` 방식 대신 **cursor/streaming** 또는 `next_token` 기반 페이지네이션 사용
- 쿼리에서 시간 범위를 명시적으로 좁히는 것이 중요 (test_id + time range)

### 5.3 집계 전용 엔진 활용
- 대규모 집계(수백만 행)의 경우 ClickHouse나 Presto를 사용하여 응답시간 단축

---

## 6. 데이터 보관 정책(라이프사이클)
- **원시 데이터 TTL:** 1년 (정책에 따름) — 이후 압축 혹은 Parquet로 옮겨 S3에 보관
- **요약/상태 데이터:** 영구 보관(요약본만 남김)
- **자동 아카이브:** 오래된 chunk를 자동으로 압축 또는 export

---

## 7. 메모리 사용량 최적화 (앱/DB)
### 7.1 애플리케이션 레벨
- **스트리밍 파서:** 파일을 메모리에 전부 로드하지 않고 스트리밍으로 파싱
- **제한된 청크 크기:** 한 번에 처리할 청크 크기를 1k~100k 행 범위로 제한
- **객체 재사용:** 데이터 파싱 시 객체 할당 최소화

### 7.2 DB 레벨
- **work_mem / maintenance_work_mem 조정:** 복잡한 정렬, 집계에 필요한 메모리 설정
- **shared_buffers:** PostgreSQL에서는 시스템 메모리의 25% 권장(환경 의존)
- **max_parallel_workers_per_gather** 조절로 과도한 병렬화로 인한 메모리 폭발 방지
- **TimescaleDB compression**을 이용해 메모리·디스크 I/O 절감

### 7.3 예시: 메모리 계산
- 한 행 평균 크기 ≈ (timestamp 8B + load 8B + displacement 8B) + overhead ≈ 40B
- 1M 행 ≈ 40MB (원시 데이터만) → 압축 후 훨씬 작아짐
- 네트워크/파싱/버퍼 등으로 여유분을 포함하여 설계

---

## 8. 운영·모니터링·벤치마크
### 8.1 모니터링 지표
- 쓰기 처리량(rows/sec), 읽기 처리량, 디스크 IOPS, DB CPU, memory usage
- 긴 쿼리 (slow queries), WAL 생성량, replication lag
- 메시지 큐 지연, worker 실패율

### 8.2 알림/오토스케일링
- 메시지 backlog가 임계치 초과 시 worker autoscale
- 디스크 사용량 80% 이상에서 경고

### 8.3 벤치마크 지표
- 데이터 인제스션: COPY vs INSERT (성능 차이 측정)
- 조회 레이턴시: 원시 vs 다운샘플 vs summary view
- 압축률 측정 (before/after)

---

## 9. 예제 스크립트 및 설정
### 9.1 PostgreSQL 파티셔닝 (예)
```sql
-- RANGE partition 예시 (Postgres 12+)
CREATE TABLE tensile_data (
  data_id BIGSERIAL PRIMARY KEY,
  test_id INT NOT NULL,
  load DOUBLE PRECISION,
  displacement DOUBLE PRECISION,
  ts TIMESTAMP WITH TIME ZONE NOT NULL
) PARTITION BY RANGE (ts);

CREATE TABLE tensile_data_202511 PARTITION OF tensile_data
  FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');

-- 인덱스
CREATE INDEX idx_td_test_ts ON tensile_data (test_id, ts);
```

### 9.2 TimescaleDB hypertable 변환
```sql
-- 기존 tensile_data 테이블이 있다면
SELECT create_hypertable('tensile_data', 'ts', chunk_time_interval => interval '1 day');
-- 압축 설정
SELECT add_compression_policy('tensile_data', compress_after => INTERVAL '7 days');
```

### 9.3 대량 업로드 예시 (COPY)
```sql
COPY tensile_data(test_id, load, displacement, ts)
FROM '/path/to/file.csv' CSV HEADER;
```

### 9.4 ClickHouse 샘플 (bulk insert 권장)
```sql
CREATE TABLE tensile_data (
  test_id UInt32,
  ts DateTime,
  load Float32,
  displacement Float32
) ENGINE = MergeTree()
ORDER BY (test_id, ts);

-- bulk load (client-side)
INSERT INTO tensile_data FORMAT CSV
< large csv stream >
```

---

## 10. 권장 스택 요약
- **권장 DB 구성:** PostgreSQL (메타데이터) + TimescaleDB (시계열 저장 및 압축)
- **대체/보완:** ClickHouse (대규모 분석에 유리)
- **메시징:** Kafka (높은 처리량), RabbitMQ (단순 큐)
- **캐시:** Redis (표준물성 캐싱, 최근 결과)
- **스토리지:** S3 호환 오브젝트 스토리지 (원시 파일 보관)

---

## 부록: 체크리스트 (배포 전)
- [ ] chunk_time_interval 및 compression 정책 설정 완료
- [ ] COPY 기반 배치 파이프라인 테스트 (throughput 측정)
- [ ] 백업 및 복구 정책(전체 + WAL) 검증
- [ ] 모니터링 대시보드 (Disk/CPU/rows/sec/slow queries) 구성
- [ ] 장애 시 재처리(Dead-letter queue) 정책 수립

---

_작성일: 2025-11-07_

