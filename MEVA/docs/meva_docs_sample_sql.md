# MEVA — Sample SQL & Schema Validation

> File: `MEVA/docs/sample-sql.md`

이 문서는 **MEVA(Material Evaluation & Verification Architecture)** 시스템의 데이터 입출력 예시 및 검증용 SQL 쿼리를 제공합니다. 스키마 초기화, 샘플 데이터 삽입, 조회, 검증, 비교를 위한 SQL 스크립트를 포함합니다.

---

## 📘 1. 스키마 초기화
```sql
-- 데이터베이스 생성 (PostgreSQL)
CREATE DATABASE meva_db;
\c meva_db;

-- 확장 기능 (시계열, JSON, 등)
CREATE EXTENSION IF NOT EXISTS timescaledb;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

---

## 📗 2. 테이블 생성 (요약)
> 전체 정의는 `MEVA/docs/table-definition.md` 참조

```sql
-- Material 테이블 생성
CREATE TABLE material (
  material_id SERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE,
  type VARCHAR(50),
  density REAL CHECK (density > 0),
  supplier VARCHAR(255),
  created_at TIMESTAMP DEFAULT now()
);

-- User 테이블 생성
CREATE TABLE "user" (
  user_id SERIAL PRIMARY KEY,
  username VARCHAR(80) NOT NULL UNIQUE,
  email VARCHAR(200) NOT NULL UNIQUE,
  role VARCHAR(20) DEFAULT 'researcher',
  created_at TIMESTAMP DEFAULT now()
);

-- Standard Property 테이블 생성
CREATE TABLE standard_property (
  std_id SERIAL PRIMARY KEY,
  material_id INT REFERENCES material(material_id) ON DELETE CASCADE,
  young_modulus REAL,
  yield_strength REAL,
  tensile_strength REAL,
  elongation REAL,
  reference TEXT
);

-- Tensile Test 테이블 생성
CREATE TABLE tensile_test (
  test_id SERIAL PRIMARY KEY,
  material_id INT REFERENCES material(material_id) ON DELETE CASCADE,
  user_id INT REFERENCES "user"(user_id) ON DELETE SET NULL,
  area REAL,
  length REAL,
  date_performed DATE,
  temperature REAL,
  remarks TEXT
);

-- Tensile Data 테이블 생성
CREATE TABLE tensile_data (
  data_id BIGSERIAL PRIMARY KEY,
  test_id INT REFERENCES tensile_test(test_id) ON DELETE CASCADE,
  load REAL,
  displacement REAL,
  timestamp REAL
);

-- Simulation Result 테이블 생성
CREATE TABLE simulation_result (
  sim_id SERIAL PRIMARY KEY,
  test_id INT REFERENCES tensile_test(test_id) ON DELETE CASCADE,
  young_modulus_calc REAL,
  yield_strength_calc REAL,
  tensile_strength_calc REAL,
  elongation_calc REAL,
  compare_to_std REAL,
  created_at TIMESTAMP DEFAULT now()
);
```

---

## 📘 3. 샘플 데이터 삽입
```sql
-- 1️⃣ 재료 정보
INSERT INTO material (name, type, density, supplier)
VALUES
  ('철', '금속', 7850, 'POSCO'),
  ('알루미늄', '금속', 2700, 'Alcoa'),
  ('구리', '금속', 8960, 'LS Cable');

-- 2️⃣ 표준 물성값
INSERT INTO standard_property (material_id, young_modulus, yield_strength, tensile_strength, elongation, reference)
VALUES
  (1, 210.0, 250.0, 400.0, 25.0, 'ASTM E8/E8M'),
  (2, 69.0, 95.0, 130.0, 12.0, 'ASTM E8/E8M'),
  (3, 110.0, 200.0, 250.0, 30.0, 'ASTM B557');

-- 3️⃣ 사용자 등록
INSERT INTO "user" (username, email, role)
VALUES
  ('admin', 'admin@meva.org', 'admin'),
  ('researcher1', 'r1@meva.org', 'researcher');

-- 4️⃣ 인장시험 메타데이터
INSERT INTO tensile_test (material_id, user_id, area, length, date_performed, temperature, remarks)
VALUES
  (1, 2, 100.5, 50.0, '2025-11-01', 25.0, 'Room temp test'),
  (2, 2, 98.2, 45.0, '2025-11-02', 27.0, 'Standard test');

-- 5️⃣ 하중-변위 데이터 (샘플 5행)
INSERT INTO tensile_data (test_id, load, displacement, timestamp)
VALUES
  (1, 0, 0, 0),
  (1, 100, 0.1, 0.5),
  (1, 200, 0.2, 1.0),
  (1, 300, 0.4, 1.5),
  (1, 350, 0.8, 2.0);

-- 6️⃣ 계산 결과
INSERT INTO simulation_result (test_id, young_modulus_calc, yield_strength_calc, tensile_strength_calc, elongation_calc, compare_to_std)
VALUES
  (1, 208.0, 260.0, 395.0, 24.8, NULL);
```

---

## 📙 4. 데이터 검증 쿼리
### (1) 데이터 무결성 점검
```sql
-- FK 누락 검증
SELECT t.test_id
FROM tensile_test t
LEFT JOIN material m ON t.material_id = m.material_id
WHERE m.material_id IS NULL;

-- 음수값 존재 여부 검증
SELECT * FROM tensile_data WHERE load < 0 OR displacement < 0;
```

### (2) 표준값 대비 계산값 비교
```sql
SELECT m.name AS material, s.young_modulus AS std_E, r.young_modulus_calc AS calc_E,
       ROUND((r.young_modulus_calc - s.young_modulus) / s.young_modulus * 100, 2) AS pct_diff
FROM simulation_result r
JOIN tensile_test t ON r.test_id = t.test_id
JOIN material m ON t.material_id = m.material_id
JOIN standard_property s ON s.material_id = m.material_id
ORDER BY pct_diff DESC;
```

### (3) 실험별 요약 통계
```sql
SELECT t.test_id, m.name AS material,
       COUNT(d.data_id) AS num_points,
       MAX(d.load) AS max_load,
       MAX(d.displacement) AS max_disp
FROM tensile_test t
JOIN material m ON t.material_id = m.material_id
JOIN tensile_data d ON d.test_id = t.test_id
GROUP BY t.test_id, m.name;
```

---

## 📒 5. CRUD 검증 예제
### (1) Create / Insert
```sql
INSERT INTO material (name, type, density) VALUES ('Titanium', '금속', 4500);
```

### (2) Read / Select
```sql
SELECT * FROM material WHERE name = 'Titanium';
```

### (3) Update
```sql
UPDATE material SET supplier = 'Korea Metal Co.' WHERE name = 'Titanium';
```

### (4) Delete
```sql
DELETE FROM material WHERE name = 'Titanium';
```

---

## 📊 6. 데이터 검증 자동화 (예시)
```sql
-- 물성값 오차율 검증 프로시저 (단순 예)
CREATE OR REPLACE FUNCTION verify_property_accuracy(test_id INT)
RETURNS TABLE(property VARCHAR, std REAL, calc REAL, pct_diff REAL, status VARCHAR) AS $$
BEGIN
  RETURN QUERY
  SELECT 'Young Modulus', s.young_modulus, r.young_modulus_calc,
         ROUND((r.young_modulus_calc - s.young_modulus) / s.young_modulus * 100, 2),
         CASE WHEN ABS((r.young_modulus_calc - s.young_modulus) / s.young_modulus * 100) <= 5 THEN 'OK' ELSE 'OUT_OF_RANGE' END
  FROM simulation_result r
  JOIN tensile_test t ON r.test_id = t.test_id
  JOIN standard_property s ON s.material_id = t.material_id
  WHERE r.test_id = test_id;
END;
$$ LANGUAGE plpgsql;

-- 실행 예시
SELECT * FROM verify_property_accuracy(1);
```

---

## 🧪 7. 테스트 및 벤치마크용 쿼리
```sql
-- 대용량 삽입 시 성능 측정 (1만 행 샘플)
INSERT INTO tensile_data (test_id, load, displacement, timestamp)
SELECT 1, random()*500, random()*1.0, generate_series(1,10000)*0.001;

-- 통계 성능 테스트
EXPLAIN ANALYZE SELECT AVG(load), MAX(displacement) FROM tensile_data WHERE test_id = 1;
```

---

## 📄 8. 데이터 검증 보고서 자동 생성용 뷰
```sql
CREATE OR REPLACE VIEW v_test_comparison AS
SELECT t.test_id, m.name AS material,
       s.young_modulus AS std_E, r.young_modulus_calc AS calc_E,
       ROUND((r.young_modulus_calc - s.young_modulus)/s.young_modulus*100,2) AS pct_diff_E,
       CASE WHEN ABS((r.young_modulus_calc - s.young_modulus)/s.young_modulus*100) <= 5 THEN 'PASS' ELSE 'FAIL' END AS eval_status
FROM simulation_result r
JOIN tensile_test t ON r.test_id = t.test_id
JOIN material m ON t.material_id = m.material_id
JOIN standard_property s ON s.material_id = m.material_id;
```

---

_작성일: 2025-11-07_

