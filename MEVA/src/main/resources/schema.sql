-- =============================================
-- MEVA (Materials Engineering Visualization and Analysis)
-- 데이터베이스 스키마
-- =============================================
-- 데이터베이스: SQLite
-- 작성일: 2025-11-17
-- 담당자: 이태윤
-- 브랜치: feature/database-setup
-- =============================================

-- =============================================
-- 1. 재료(Material) 테이블
-- =============================================
-- 인장 시험에 사용되는 재료 정보를 저장
CREATE TABLE IF NOT EXISTS material (
    material_id INTEGER PRIMARY KEY AUTOINCREMENT,
    material_name VARCHAR(100) NOT NULL UNIQUE,       -- 재료명 (예: Steel, Aluminum, Copper)
    youngs_modulus REAL,                              -- 영률(Young's Modulus, GPa)
    yield_strength REAL,                              -- 항복 강도(Yield Strength, MPa)
    ultimate_strength REAL,                           -- 극한 인장 강도(Ultimate Tensile Strength, MPa)
    fracture_strain REAL,                             -- 파괴 변형률(Fracture Strain, %)
    poissons_ratio REAL,                              -- 푸아송 비(Poisson's Ratio)
    density REAL,                                     -- 밀도(Density, g/cm³)
    description TEXT,                                 -- 재료 설명
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,   -- 생성 일시
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP    -- 수정 일시
);

-- =============================================
-- 2. 시편(Specimen) 테이블
-- =============================================
-- 인장 시험에 사용되는 시편의 기하학적 정보를 저장
CREATE TABLE IF NOT EXISTS specimen (
    specimen_id INTEGER PRIMARY KEY AUTOINCREMENT,
    material_id INTEGER NOT NULL,                     -- 재료 ID (외래키)
    specimen_name VARCHAR(100) NOT NULL,              -- 시편명
    initial_length REAL NOT NULL,                     -- 초기 길이(mm)
    gauge_length REAL NOT NULL,                       -- 표점 거리(Gauge Length, mm)
    cross_sectional_area REAL NOT NULL,               -- 단면적(mm²)
    diameter REAL,                                    -- 직경(mm, 원형 단면의 경우)
    width REAL,                                       -- 폭(mm, 직사각형 단면의 경우)
    thickness REAL,                                   -- 두께(mm, 직사각형 단면의 경우)
    shape VARCHAR(20) CHECK(shape IN ('circular', 'rectangular')), -- 시편 형상
    description TEXT,                                 -- 시편 설명
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,   -- 생성 일시
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,   -- 수정 일시
    FOREIGN KEY (material_id) REFERENCES material(material_id) ON DELETE CASCADE
);

-- =============================================
-- 3. 시험(Test) 테이블
-- =============================================
-- 인장 시험 세션 정보를 저장
CREATE TABLE IF NOT EXISTS test (
    test_id INTEGER PRIMARY KEY AUTOINCREMENT,
    specimen_id INTEGER NOT NULL,                     -- 시편 ID (외래키)
    test_name VARCHAR(100) NOT NULL,                  -- 시험명
    test_date DATE DEFAULT (DATE('now')),             -- 시험 날짜
    test_type VARCHAR(50) DEFAULT 'Tensile Test',     -- 시험 유형
    strain_rate REAL,                                 -- 변형률 속도(Strain Rate, 1/s)
    temperature REAL DEFAULT 25.0,                    -- 시험 온도(°C)
    humidity REAL,                                    -- 습도(%)
    operator VARCHAR(100),                            -- 시험자
    notes TEXT,                                       -- 시험 노트
    status VARCHAR(20) DEFAULT 'In Progress' CHECK(status IN ('In Progress', 'Completed', 'Failed')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,   -- 생성 일시
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,   -- 수정 일시
    FOREIGN KEY (specimen_id) REFERENCES specimen(specimen_id) ON DELETE CASCADE
);

-- =============================================
-- 4. 시험 데이터(Test Data) 테이블
-- =============================================
-- 인장 시험 중 측정된 원시 데이터를 저장
CREATE TABLE IF NOT EXISTS test_data (
    data_id INTEGER PRIMARY KEY AUTOINCREMENT,
    test_id INTEGER NOT NULL,                         -- 시험 ID (외래키)
    sequence_number INTEGER NOT NULL,                 -- 측정 순서
    time_elapsed REAL NOT NULL,                       -- 경과 시간(s)
    load REAL NOT NULL,                               -- 하중(Load, N)
    extension REAL NOT NULL,                          -- 신장(Extension, mm)
    stress REAL,                                      -- 응력(Stress, MPa) - 계산값
    strain REAL,                                      -- 변형률(Strain, %) - 계산값
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,   -- 생성 일시
    FOREIGN KEY (test_id) REFERENCES test(test_id) ON DELETE CASCADE,
    UNIQUE(test_id, sequence_number)                  -- 각 시험 내에서 순서 번호는 고유
);

-- =============================================
-- 5. 분석 결과(Analysis Result) 테이블
-- =============================================
-- 시험 데이터 분석 결과를 저장 (영률, 항복강도, 극한강도 등)
CREATE TABLE IF NOT EXISTS analysis_result (
    result_id INTEGER PRIMARY KEY AUTOINCREMENT,
    test_id INTEGER NOT NULL UNIQUE,                  -- 시험 ID (외래키, 고유)
    calculated_youngs_modulus REAL,                   -- 계산된 영률(GPa)
    calculated_yield_strength REAL,                   -- 계산된 항복 강도(MPa)
    calculated_ultimate_strength REAL,                -- 계산된 극한 인장 강도(MPa)
    calculated_fracture_strain REAL,                  -- 계산된 파괴 변형률(%)
    elastic_limit REAL,                               -- 탄성 한계(Elastic Limit, MPa)
    proportional_limit REAL,                          -- 비례 한계(Proportional Limit, MPa)
    elongation_at_break REAL,                         -- 파단 신장률(%, Elongation at Break)
    reduction_of_area REAL,                           -- 단면 감소율(%, Reduction of Area)
    toughness REAL,                                   -- 인성(Toughness, MJ/m³)
    resilience REAL,                                  -- 탄성 에너지(Resilience, kJ/m³)
    analysis_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,-- 분석 일시
    notes TEXT,                                       -- 분석 노트
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,   -- 생성 일시
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,   -- 수정 일시
    FOREIGN KEY (test_id) REFERENCES test(test_id) ON DELETE CASCADE
);

-- =============================================
-- 6. 파일(File) 테이블
-- =============================================
-- 가져온 데이터 파일 및 내보낸 보고서 파일 정보를 저장
CREATE TABLE IF NOT EXISTS file (
    file_id INTEGER PRIMARY KEY AUTOINCREMENT,
    test_id INTEGER,                                  -- 시험 ID (외래키, NULL 가능)
    file_name VARCHAR(255) NOT NULL,                  -- 파일명
    file_path TEXT NOT NULL,                          -- 파일 경로
    file_type VARCHAR(50) NOT NULL CHECK(file_type IN ('import', 'export')), -- 파일 유형
    file_format VARCHAR(20) CHECK(file_format IN ('CSV', 'XLSX', 'TXT', 'PDF', 'PNG', 'JPEG')), -- 파일 형식
    file_size INTEGER,                                -- 파일 크기(bytes)
    description TEXT,                                 -- 파일 설명
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,   -- 생성 일시
    FOREIGN KEY (test_id) REFERENCES test(test_id) ON DELETE SET NULL
);

-- =============================================
-- 인덱스 생성
-- =============================================
-- 자주 사용되는 쿼리의 성능 향상을 위한 인덱스

CREATE INDEX IF NOT EXISTS idx_specimen_material ON specimen(material_id);
CREATE INDEX IF NOT EXISTS idx_test_specimen ON test(specimen_id);
CREATE INDEX IF NOT EXISTS idx_test_date ON test(test_date);
CREATE INDEX IF NOT EXISTS idx_test_data_test ON test_data(test_id);
CREATE INDEX IF NOT EXISTS idx_file_test ON file(test_id);

-- =============================================
-- 트리거 생성
-- =============================================
-- updated_at 필드 자동 업데이트를 위한 트리거

CREATE TRIGGER IF NOT EXISTS update_material_timestamp 
AFTER UPDATE ON material
FOR EACH ROW
BEGIN
    UPDATE material SET updated_at = CURRENT_TIMESTAMP WHERE material_id = NEW.material_id;
END;

CREATE TRIGGER IF NOT EXISTS update_specimen_timestamp 
AFTER UPDATE ON specimen
FOR EACH ROW
BEGIN
    UPDATE specimen SET updated_at = CURRENT_TIMESTAMP WHERE specimen_id = NEW.specimen_id;
END;

CREATE TRIGGER IF NOT EXISTS update_test_timestamp 
AFTER UPDATE ON test
FOR EACH ROW
BEGIN
    UPDATE test SET updated_at = CURRENT_TIMESTAMP WHERE test_id = NEW.test_id;
END;

CREATE TRIGGER IF NOT EXISTS update_analysis_result_timestamp 
AFTER UPDATE ON analysis_result
FOR EACH ROW
BEGIN
    UPDATE analysis_result SET updated_at = CURRENT_TIMESTAMP WHERE result_id = NEW.result_id;
END;

-- =============================================
-- 샘플 데이터 삽입 (선택적)
-- =============================================
-- 개발 및 테스트를 위한 기본 재료 데이터

INSERT OR IGNORE INTO material (material_name, youngs_modulus, yield_strength, ultimate_strength, fracture_strain, poissons_ratio, density, description)
VALUES 
    ('Steel (Mild Steel)', 200, 250, 400, 25, 0.30, 7.85, '일반 구조용 연강'),
    ('Aluminum (Al 6061)', 69, 276, 310, 12, 0.33, 2.70, '알루미늄 합금 6061'),
    ('Copper (Pure)', 120, 70, 220, 50, 0.34, 8.96, '순수 구리'),
    ('Titanium (Ti-6Al-4V)', 114, 880, 950, 14, 0.34, 4.43, '티타늄 합금 Ti-6Al-4V'),
    ('Brass (Cu-Zn)', 100, 200, 400, 30, 0.35, 8.50, '황동');

-- =============================================
-- 데이터베이스 스키마 정보 확인용 뷰
-- =============================================
-- 스키마 버전 및 생성 정보를 저장하는 메타데이터 테이블

CREATE TABLE IF NOT EXISTS schema_info (
    version VARCHAR(20) PRIMARY KEY,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT OR REPLACE INTO schema_info (version, description) 
VALUES ('1.0.0', 'MEVA 프로젝트 초기 데이터베이스 스키마');

-- =============================================
-- 스키마 생성 완료
-- =============================================