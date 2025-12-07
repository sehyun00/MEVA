# MEVA Project Structure

## 📁 폴더 구조 설명

### `/src/main/java/meva/`
메인 소스코드 디렉토리

- **`Main.java`**: 애플리케이션 진입점
- **`gui/`**: 사용자 인터페이스 패키지 (김종현 담당)
- **`simulation/`**: 시뮬레이션 로직 패키지 (김종현, 이태윤 담당)
- **`calculation/`**: 계산 알고리즘 패키지 (이태윤 담당)
- **`visualization/`**: 그래프 시각화 패키지 (김종현 담당)
- **`fileio/`**: 파일 입출력 패키지 (이태윤 담당)
- **`database/`**: 데이터 관리 패키지 (이태윤 담당)
- **`validation/`**: 데이터 검증 패키지 (박성빈 담당)
- **`models/`**: 데이터 모델 패키지
- **`utils/`**: 유틸리티 패키지

### `/src/main/resources/`
애플리케이션 리소스 디렉토리

- **`config/`**: 설정 파일
- **`data/`**: 기본 데이터 파일
- **`images/`**: 이미지 리소스 (향후 추가)

### `/src/test/java/meva/`
테스트 코드 디렉토리 (박성빈 담당)

### `/docs/`
프로젝트 문서화 디렉토리

### `/lib/`
외부 라이브러리 디렉토리 (향후 추가)

### `/examples/`
예제 파일 디렉토리 (향후 추가)

## 🔧 브랜치별 작업 영역

각 기능별 브랜치에서 해당 패키지를 담당하여 개발합니다:

- `feature/gui-design` → `gui/` 패키지
- `feature/stress-strain-calculation` → `simulation/`, `calculation/` 패키지
- `feature/graph-visualization` → `visualization/` 패키지
- `feature/file-handler` → `fileio/` 패키지
- `feature/database-setup` → `database/` 패키지
- `feature/data-validation` → `validation/` 패키지
- `feature/testing` → `test/` 디렉토리

## 📝 개발 가이드라인

1. **패키지별 담당자는 해당 패키지 내에서만 작업**
2. **공통 모델 클래스(`models/`)는 팀장 승인 후 수정**
3. **상수 정의(`utils/Constants.java`)는 공통으로 관리**
4. **테스트 코드는 기능 구현과 함께 작성**
5. **커밋 메시지는 `feat:`, `fix:`, `docs:` 등의 prefix 사용**