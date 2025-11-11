# MEVA 개발 환경 설정 가이드

## 1. 개요

이 문서는 MEVA (Materials Engineering Visualization and Analysis) 프로젝트의 개발 환경을 설정하는 방법을 설명합니다. 팀 구성원이 로컬 개발 환경을 구축하고 프로젝트를 실행하는 데 필요한 모든 단계를 포함합니다.

## 2. 시스템 요구사항

### 2.1 하드웨어 요구사항

**최소 사양**:
- **CPU**: Intel Core i5 또는 동급 이상
- **RAM**: 8GB 이상
- **디스크 공간**: 5GB 이상 여유 공간
- **화면 해상도**: 1280 x 720 이상

**권장 사양**:
- **CPU**: Intel Core i7 또는 동급 이상
- **RAM**: 16GB 이상
- **디스크 공간**: 10GB 이상 여유 공간
- **화면 해상도**: 1920 x 1080 이상

### 2.2 운영체제

MEVA는 다음 운영체제를 지원합니다:

- **Windows**: Windows 10 이상 (64-bit)
- **macOS**: macOS 10.15 (Catalina) 이상
- **Linux**: Ubuntu 20.04 LTS 이상 또는 동급 배포판

## 3. 필수 소프트웨어 설치

### 3.1 Java Development Kit (JDK) 17

MEVA는 Java 17을 필수로 요구합니다.

#### Windows

1. **Oracle JDK 또는 OpenJDK 다운로드**
   - Oracle JDK: [https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
   - OpenJDK: [https://adoptium.net/](https://adoptium.net/)

2. **설치 파일 실행**
   - 다운로드한 `.msi` 또는 `.exe` 파일 실행
   - 설치 마법사 지시에 따라 진행

3. **환경 변수 설정**
   ```cmd
   # 시스템 환경 변수 편집
   제어판 > 시스템 > 고급 시스템 설정 > 환경 변수
   
   # JAVA_HOME 변수 추가
   변수 이름: JAVA_HOME
   변수 값: C:\Program Files\Java\jdk-17
   
   # Path에 Java bin 추가
   Path 변수에 추가: %JAVA_HOME%\bin
   ```

4. **설치 확인**
   ```cmd
   java -version
   # 출력: java version "17.0.x" ...
   ```

#### macOS

1. **Homebrew를 통한 설치 (권장)**
   ```bash
   # Homebrew 설치 (설치되지 않은 경우)
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   
   # OpenJDK 17 설치
   brew install openjdk@17
   
   # 심볼릭 링크 생성
   sudo ln -sfn /usr/local/opt/openjdk@17/libexec/openjdk.jdk \
     /Library/Java/JavaVirtualMachines/openjdk-17.jdk
   ```

2. **환경 변수 설정**
   ```bash
   # ~/.zshrc 또는 ~/.bash_profile에 추가
   echo 'export JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home' >> ~/.zshrc
   echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
   
   # 변경 사항 적용
   source ~/.zshrc
   ```

3. **설치 확인**
   ```bash
   java -version
   # 출력: openjdk version "17.0.x" ...
   ```

#### Linux (Ubuntu)

1. **OpenJDK 설치**
   ```bash
   # 패키지 목록 업데이트
   sudo apt update
   
   # OpenJDK 17 설치
   sudo apt install openjdk-17-jdk -y
   ```

2. **환경 변수 설정**
   ```bash
   # ~/.bashrc에 추가
   echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
   echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.bashrc
   
   # 변경 사항 적용
   source ~/.bashrc
   ```

3. **설치 확인**
   ```bash
   java -version
   # 출력: openjdk version "17.0.x" ...
   ```

### 3.2 Git

버전 관리를 위해 Git이 필요합니다.

#### Windows

```cmd
# Git for Windows 다운로드 및 설치
https://git-scm.com/download/win

# 설치 확인
git --version
```

#### macOS

```bash
# Xcode Command Line Tools 설치 (기본 Git 포함)
xcode-select --install

# 또는 Homebrew로 설치
brew install git

# 설치 확인
git --version
```

#### Linux

```bash
# Ubuntu/Debian
sudo apt install git -y

# 설치 확인
git --version
```

### 3.3 통합 개발 환경 (IDE)

#### IntelliJ IDEA (권장)

1. **다운로드**
   - Community Edition: [https://www.jetbrains.com/idea/download/](https://www.jetbrains.com/idea/download/)
   - 또는 Ultimate Edition (학생 라이선스 가능)

2. **설치 및 초기 설정**
   - 설치 파일 실행
   - JDK 17 설정 확인
   - Gradle 플러그인 활성화 확인

#### Eclipse

1. **다운로드**
   - Eclipse IDE for Java Developers: [https://www.eclipse.org/downloads/](https://www.eclipse.org/downloads/)

2. **설치**
   - Eclipse Installer 실행
   - "Eclipse IDE for Java Developers" 선택
   - JDK 17 경로 설정

#### Visual Studio Code

1. **다운로드 및 설치**
   - [https://code.visualstudio.com/](https://code.visualstudio.com/)

2. **필수 확장 프로그램 설치**
   - Extension Pack for Java
   - Gradle for Java
   - Git Graph

### 3.4 Gradle

MEVA는 Gradle을 빌드 도구로 사용합니다. Gradle Wrapper가 포함되어 있어 별도 설치가 필요하지 않습니다.

```bash
# 프로젝트 디렉토리에서 실행
./gradlew --version  # Linux/macOS
gradlew.bat --version  # Windows
```

## 4. 프로젝트 클론 및 설정

### 4.1 저장소 클론

```bash
# GitHub에서 프로젝트 클론
git clone https://github.com/sehyun00/MEVA.git

# 프로젝트 디렉토리로 이동
cd MEVA
```

### 4.2 Git 설정

```bash
# 사용자 정보 설정
git config --global user.name "당신의 이름"
git config --global user.email "your.email@example.com"

# 기본 브랜치 이름 설정
git config --global init.defaultBranch main

# 한글 파일명 깨짐 방지 (Windows)
git config --global core.quotepath false
```

### 4.3 브랜치 전략

```bash
# 원격 저장소 확인
git remote -v

# 모든 브랜치 확인
git branch -a

# 자신의 기능 브랜치 체크아웃 (예시)
git checkout feature/your-feature-name
```

## 5. 프로젝트 빌드

### 5.1 의존성 다운로드

```bash
# Gradle을 사용하여 의존성 다운로드
./gradlew build --refresh-dependencies
```

### 5.2 프로젝트 컴파일

```bash
# 전체 프로젝트 컴파일
./gradlew compileJava

# 테스트 포함 컴파일
./gradlew build
```

### 5.3 빌드 오류 해결

#### 오류: "JAVA_HOME is not set"

```bash
# 환경 변수 확인
echo $JAVA_HOME  # Linux/macOS
echo %JAVA_HOME%  # Windows

# 설정되지 않은 경우 3.1 절 참조
```

#### 오류: "Could not resolve dependencies"

```bash
# Gradle 캠시 클리어
./gradlew clean

# 의존성 재다운로드
./gradlew build --refresh-dependencies
```

## 6. 프로젝트 실행

### 6.1 IDE에서 실행

#### IntelliJ IDEA

1. **프로젝트 열기**
   - File > Open > MEVA 폴더 선택
   - Gradle 프로젝트로 자동 인식

2. **Main 클래스 실행**
   - `src/main/java/meva/Main.java` 파일 열기
   - 우클릭 > Run 'Main.main()'
   - 또는 Shift + F10

3. **실행 구성**
   - Run > Edit Configurations
   - Main class: `meva.Main`
   - VM options: `-Xmx512m` (메모리 할당)

#### Eclipse

1. **프로젝트 가져오기**
   - File > Import > Existing Gradle Project
   - MEVA 폴더 선택

2. **실행**
   - Main.java 우클릭 > Run As > Java Application

#### Visual Studio Code

1. **프로젝트 열기**
   - File > Open Folder > MEVA 선택

2. **실행**
   - Main.java 열기
   - F5 누르기 또는 Run 버튼 클릭

### 6.2 컴맨드라인에서 실행

```bash
# Gradle을 사용하여 실행
./gradlew run

# JAR 파일 생성 및 실행
./gradlew jar
java -jar build/libs/MEVA-1.0.jar
```

### 6.3 실행 확인

프로그램이 정상적으로 실행되면 다음과 같은 GUI 창이 나타나야 합니다:

- 메인 윈도우 타이틀: "MEVA - Materials Engineering Visualization and Analysis"
- 메뉴바: 파일, 편집, 보기, 도움말
- 입력 패널, 그래프 패널, 결과 패널

## 7. 데이터베이스 설정

### 7.1 SQLite 데이터베이스

MEVA는 SQLite를 사용하며, 별도의 설치가 필요하지 않습니다. 데이터베이스 파일은 자동으로 생성됩니다.

```bash
# 데이터베이스 파일 위치
MEVA/data/meva.db
```

### 7.2 초기 데이터 로드

최초 실행 시 다음 데이터가 자동으로 로드됩니다:

- 표준 재료 물성값 (StandardProperties)
- 샘플 재료 데이터 (Materials)

### 7.3 데이터베이스 초기화

```bash
# 데이터베이스 파일 삭제 (재생성됨)
rm data/meva.db  # Linux/macOS
del data\meva.db  # Windows

# 프로그램 재실행
./gradlew run
```

## 8. 테스트 실행

### 8.1 단위 테스트

```bash
# 모든 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "meva.calculator.StressStrainCalculatorTest"

# 테스트 보고서 확인
# build/reports/tests/test/index.html
```

### 8.2 테스트 커버리지

```bash
# JaCoCo 테스트 커버리지 생성
./gradlew jacocoTestReport

# 보고서 확인
# build/reports/jacoco/test/html/index.html
```

### 8.3 통합 테스트

```bash
# 통합 테스트 실행
./gradlew integrationTest
```

## 9. 문제 해결 (Troubleshooting)

### 9.1 일반적인 문제

#### 문제: "프로그램이 실행되지 않음"

```bash
# Java 버전 확인
java -version  # 17.0.x 여야 함

# JAVA_HOME 확인
echo $JAVA_HOME  # 경로가 올바른지 확인

# Gradle 래퍼 권한 확인 (Linux/macOS)
chmod +x gradlew
```

#### 문제: "GUI가 표시되지 않음"

```bash
# 헤드리스 모드 확인 (Linux 서버)
echo $DISPLAY  # 비어있으면 GUI 지원 안 됨

# macOS에서 XQuartz 설치 필요 여부 확인
```

#### 문제: "메모리 부족 오류"

```bash
# 힙 메모리 크기 증가
./gradlew run -Dorg.gradle.jvmargs="-Xmx1024m"

# 또는 gradle.properties 파일에 추가
org.gradle.jvmargs=-Xmx1024m -XX:MaxPermSize=512m
```

### 9.2 플랫폼별 문제

#### Windows

```cmd
# 한글 경로 문제
# 프로젝트를 영문 경로에 위치
# 예: C:\Dev\MEVA

# 권한 문제
# 관리자 권한으로 CMD 실행
```

#### macOS

```bash
# Gatekeeper 문제
# 시스템 환경설정 > 보안 및 개인정보 보호
# "확인되지 않은 개발자의 앱 허용"

# Rosetta 2 필요 (Apple Silicon)
arch -x86_64 ./gradlew run
```

#### Linux

```bash
# 폰트 문제
sudo apt install fontconfig -y

# 디스플레이 문제
export DISPLAY=:0
```

### 9.3 의존성 문제

```bash
# Gradle 캠시 전체 클리어
rm -rf ~/.gradle/caches/  # Linux/macOS
rmdir /s ~/.gradle\caches  # Windows

# 프로젝트 재빌드
./gradlew clean build --refresh-dependencies
```

## 10. 추가 리소스

### 10.1 공식 문서

- **프로젝트 README**: [../README.md](../README.md)
- **시스템 아키텍처**: [system-architecture.md](./system-architecture.md)
- **데이터 플로우 다이어그램**: [data-flow-diagram.md](./data-flow-diagram.md)
- **데이터베이스 ERD**: [meva_docs_database_erd.md](./meva_docs_database_erd.md)

### 10.2 외부 링크

- **Java 17 공식 문서**: [https://docs.oracle.com/en/java/javase/17/](https://docs.oracle.com/en/java/javase/17/)
- **Gradle 가이드**: [https://docs.gradle.org/current/userguide/userguide.html](https://docs.gradle.org/current/userguide/userguide.html)
- **Java Swing 튜토리얼**: [https://docs.oracle.com/javase/tutorial/uiswing/](https://docs.oracle.com/javase/tutorial/uiswing/)
- **JFreeChart 문서**: [https://www.jfree.org/jfreechart/](https://www.jfree.org/jfreechart/)

### 10.3 커뮤니티

- **GitHub 저장소**: [https://github.com/sehyun00/MEVA](https://github.com/sehyun00/MEVA)
- **이슈 트래커**: [https://github.com/sehyun00/MEVA/issues](https://github.com/sehyun00/MEVA/issues)
- **품의사항**: 팀장 김세현 (sh000917@gmail.com)

## 11. 개발 규칙

### 11.1 코드 스타일

- **Java 코딩 규칙** 준수
- **들여쓰기**: 스페이스 2칸 또는 4칸
- **명명 규칙**:
  - 클래스: PascalCase (예: `StressStrainCalculator`)
  - 메서드: camelCase (예: `calculateStress`)
  - 상수: UPPER_SNAKE_CASE (예: `MAX_STRESS`)

### 11.2 커밋 메시지

```
[타입] 간략한 설명

상세 설명 (선택사항)

타입:
- feat: 새로운 기능
- fix: 버그 수정
- docs: 문서 수정
- style: 코드 포맷팅
- refactor: 코드 리팩토링
- test: 테스트 코드
- chore: 빌드 설정
```

### 11.3 브랜치 명명 규칙

```
feature/기능명         # 새로운 기능
bugfix/버그명          # 버그 수정
hotfix/긴급수정       # 긴급 수정
refactor/리팩토링명   # 코드 리팩토링

예시:
feature/stress-strain-calculation
bugfix/chart-display-error
```

## 12. 버전 정보

**문서 버전**: v1.0  
**최종 수정일**: 2025-11-11  
**작성자**: 김세현 (MEVA 프로젝트 팀)  
**프로젝트**: MEVA (Materials Engineering Visualization and Analysis)  
**라이선스**: MIT License

---

해피 코딩! 프로젝트 개발에 문제가 있으면 언제든지 이슈를 등록하거나 팀장에게 문의하세요. 🚀
