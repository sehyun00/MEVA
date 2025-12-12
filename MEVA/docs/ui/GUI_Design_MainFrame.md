# MainFrame GUI 설계 문서

## 문서 정보
- **작성일**: 2025-11-11
- **프로젝트**: MEVA (Materials Engineering Visualization and Analysis)
- **대상 컴포넌트**: MainFrame.java
- **버전**: 1.0
- **작성자**: MEVA 개발팀

---

## 1. 개요

### 1.1 목적
MainFrame은 MEVA 애플리케이션의 메인 윈도우로, 재료 공학 데이터 시각화 및 분석을 위한 모든 UI 컴포넌트를 통합하는 최상위 컨테이너입니다.

### 1.2 주요 기능
- 재료 물성 및 시편 치수 입력 인터페이스
- 실시간 데이터 시각화 (그래프)
- 계산 결과 표시
- 파일 입출력 관리
- 프리셋 관리

### 1.3 요구사항 매핑
- **UI-GUI-001**: 메인 애플리케이션 윈도우 구현
- **UI-GUI-002**: 사용자 입력 폼 구현
- **UI-VIS-001**: 데이터 시각화 패널 구현

---

## 2. 전체 구조

### 2.1 레이아웃 개요
MainFrame은 BorderLayout을 기본 레이아웃으로 사용하며, 다음과 같이 5개 주요 영역으로 구분됩니다.

```
┌─────────────────────────────────────────────┐
│            MenuBar (NORTH)                  │
├─────────────────────────────────────────────┤
│            ToolBar (NORTH)                  │
├─────────────────────────────────────────────┤
│  InputPanel  │  VisualizationPanel  │ Results │
│   (WEST)     │      (CENTER)        │ (EAST)  │
│              │                      │         │
│              │                      │         │
│              │                      │         │
├─────────────────────────────────────────────┤
│           StatusBar (SOUTH)                 │
└─────────────────────────────────────────────┘
```

### 2.2 컴포넌트 계층 구조

```
MainFrame (JFrame)
├── JMenuBar
│   ├── File Menu
│   ├── Edit Menu  
│   ├── View Menu
│   ├── Tools Menu
│   └── Help Menu
├── JToolBar
│   ├── New Button
│   ├── Open Button
│   ├── Save Button
│   ├── Export Button
│   └── Settings Button
├── MainPanel (JPanel - BorderLayout)
│   ├── InputPanel (WEST)
│   │   ├── MaterialPropertiesPanel
│   │   ├── SpecimenDimensionsPanel
│   │   ├── ControlButtonsPanel
│   │   └── PresetManagementPanel
│   ├── VisualizationPanel (CENTER)
│   │   ├── ChartPanel (JFreeChart)
│   │   └── ChartControlPanel
│   └── ResultsPanel (EAST)
│       ├── JScrollPane
│       └── JTable
└── StatusBar (SOUTH)
    ├── StatusLabel
    ├── ProgressBar
    └── TimeLabel
```

---

## 3. 상세 컴포넌트 설계

### 3.1 MenuBar

#### 구성 요소
| 메뉴 | 메뉴 항목 | 단축키 | 기능 |
|------|----------|-------|------|
| File | New | Ctrl+N | 새 프로젝트 생성 |
| | Open... | Ctrl+O | 프로젝트 열기 |
| | Save | Ctrl+S | 프로젝트 저장 |
| | Save As... | Ctrl+Shift+S | 다른 이름으로 저장 |
| | --- | | 구분선 |
| | Export | | 데이터 내보내기 |
| | --- | | 구분선 |
| | Exit | Alt+F4 | 프로그램 종료 |
| Edit | Undo | Ctrl+Z | 실행 취소 |
| | Redo | Ctrl+Y | 다시 실행 |
| | --- | | 구분선 |
| | Preferences | Ctrl+, | 환경설정 |
| View | Zoom In | Ctrl++ | 확대 |
| | Zoom Out | Ctrl+- | 축소 |
| | Reset Zoom | Ctrl+0 | 기본 크기 |
| | --- | | 구분선 |
| | Show Grid | | 그리드 표시 |
| | Show Legend | | 범례 표시 |
| Tools | Calculate | F5 | 계산 실행 |
| | Clear Data | | 데이터 초기화 |
| | --- | | 구분선 |
| | Data Validator | | 데이터 검증 |
| Help | User Guide | F1 | 사용자 설명서 |
| | About | | 프로그램 정보 |

#### 기술 사항
- **클래스**: `JMenuBar`
- **폰트**: Dialog, 12pt
- **높이**: 25px

### 3.2 ToolBar

#### 구성 요소
| 버튼 | 아이콘 | 툴팁 | 기능 |
|------|------|------|------|
| New | 파일 아이콘 | New Project | 새 프로젝트 |
| Open | 폴더 아이콘 | Open Project | 프로젝트 열기 |
| Save | 디스크 아이콘 | Save Project | 프로젝트 저장 |
| Export | 내보내기 아이콘 | Export Data | 데이터 내보내기 |
| Settings | 톱니바퀴 아이콘 | Settings | 설정 |

#### 기술 사항
- **클래스**: `JToolBar`
- **배치**: NORTH (MenuBar 아래)
- **버튼 크기**: 32x32px
- **아이콘 크기**: 24x24px
- **간격**: 5px
- **Floatable**: false

---

### 3.3 InputPanel (WEST)

#### 3.3.1 MaterialPropertiesPanel

**목적**: 재료 물성 입력

**구성 요소**:
| 레이블 | 컴포넌트 | 기본값 | 단위 | 설명 |
|--------|----------|--------|------|------|
| 영계수율 (E) | JTextField | 200.0 | GPa | 재료의 영계수율 |
| 항복강도 (σy) | JTextField | 250.0 | MPa | 항복 응력 |
| 강도계수 (K) | JTextField | 500.0 | MPa | 강화 계수 |
| 경화지수 (n) | JTextField | 0.2 | - | 응력-변형률 경화지수 |

**레이아웃**: GridBagLayout (2열 구조)

**입력 검증**:
- 숫자만 허용
- 양수만 허용
- 유효 범위 검사

#### 3.3.2 SpecimenDimensionsPanel

**목적**: 시편 치수 입력

**구성 요소**:
| 레이블 | 컴포넌트 | 기본값 | 단위 | 설명 |
|--------|----------|--------|------|------|
| 넓이 (W) | JTextField | 10.0 | mm | 시편 넓이 |
| 두께 (t) | JTextField | 5.0 | mm | 시편 두께 |
| 길이 (L) | JTextField | 50.0 | mm | 시편 길이 |
| 게이지길이 | JTextField | 25.0 | mm | 스트레인 게이지 길이 |

**레이아웃**: GridBagLayout (2열 구조)

#### 3.3.3 ControlButtonsPanel

**구성 요소**:
- **Calculate** 버튼: 계산 실행
- **Reset** 버튼: 입력값 초기화
- **Clear Graph** 버튼: 그래프 초기화

**레이아웃**: FlowLayout

**버튼 스타일**:
- 크기: 100x30px
- 간격: 10px
- Calculate 버튼: Primary 색상 (#2196F3)

#### 3.3.4 PresetManagementPanel

**구성 요소**:
- **프리셋 선택**: JComboBox
- **저장** 버튼: 현재 설정 저장
- **삭제** 버튼: 선택한 프리셋 삭제

**기본 프리셋**:
- Steel (Default)
- Aluminum
- Copper
- Custom

---

### 3.4 VisualizationPanel (CENTER)

#### 3.4.1 ChartPanel

**목적**: 응력-변형률 곡선 표시

**기술 사항**:
- **라이브러리**: JFreeChart
- **차트 타입**: XYLineChart
- **X축**: 변형률 (Strain, ε)
- **Y축**: 응력 (Stress, σ) [MPa]
- **배경색**: 흰색
- **그리드**: 표시, 회색 점선

**기능**:
- 실시간 데이터 업데이트
- 줌/팬 기능
- 마우스 드래그로 영역 선택 확대
- 마우스 호버로 데이터 포인트 값 표시

#### 3.4.2 ChartControlPanel

**구성 요소**:
- **Zoom In** 버튼
- **Zoom Out** 버튼
- **Reset Zoom** 버튼
- **Export Chart** 버튼 (PNG, SVG 형식 지원)

**배치**: ChartPanel 하단

---

### 3.5 ResultsPanel (EAST)

**목적**: 계산된 물성값 표시

**구성 요소**:
- **JTable**: 결과 데이터 표시
- **JScrollPane**: 스크롤 기능

**테이블 열 구성**:
| 열 이름 | 폭 | 설명 |
|----------|-----|------|
| Property | 120px | 물성 이름 |
| Value | 100px | 계산 값 |
| Unit | 60px | 단위 |

**표시 항목**:
- Maximum Stress (σmax)
- Strain at Max Stress (εmax)
- Ultimate Tensile Strength (UTS)
- Young's Modulus (E)
- Yield Strength (σy)
- Elongation (%)
- Reduction of Area (%)

**테이블 스타일**:
- 헤더: 굵은 글꼴, 회색 배경
- 교차 행 색상: 흰색/연한 회색
- 폰트: Monospaced, 11pt

---

### 3.6 StatusBar (SOUTH)

**구성 요소**:
- **StatusLabel**: 현재 상태 메시지
- **ProgressBar**: 작업 진행 표시
- **TimeLabel**: 현재 시간 표시

**레이아웃**: BorderLayout
- WEST: StatusLabel
- CENTER: ProgressBar
- EAST: TimeLabel

**높이**: 25px

**상태 메시지 예시**:
- "Ready"
- "Calculating..."
- "Calculation completed"
- "Error: Invalid input"
- "File saved successfully"

---

## 4. 이벤트 처리

### 4.1 주요 이벤트 플로우

#### 4.1.1 Calculate 버튼 클릭
```
사용자 -> Calculate 버튼 클릭
    ↓
입력값 검증
    ↓ (유효)
StatusBar: "Calculating..."
    ↓
SimulationController.calculate()
    ↓
결과 데이터 수신
    ↓
ResultsPanel 업데이트
    ↓
VisualizationPanel 업데이트
    ↓
StatusBar: "Calculation completed"
```

#### 4.1.2 파일 불러오기 (Open)
```
사용자 -> File > Open 메뉴 클릭
    ↓
JFileChooser 표시
    ↓ (파일 선택)
FileHandler.loadProject(file)
    ↓
데이터 파싱
    ↓
InputPanel 업데이트
    ↓
ResultsPanel 업데이트
    ↓
VisualizationPanel 업데이트
    ↓
StatusBar: "File loaded: [filename]"
```

### 4.2 이벤트 리스너 매핑

| 컴포넌트 | 이벤트 | 핸들러 메서드 | 설명 |
|----------|------|---------------|------|
| Calculate 버튼 | ActionEvent | onCalculateClicked() | 계산 실행 |
| Reset 버튼 | ActionEvent | onResetClicked() | 입력 초기화 |
| Clear Graph 버튼 | ActionEvent | onClearGraphClicked() | 그래프 초기화 |
| File > New | ActionEvent | onNewProject() | 새 프로젝트 |
| File > Open | ActionEvent | onOpenProject() | 프로젝트 열기 |
| File > Save | ActionEvent | onSaveProject() | 프로젝트 저장 |
| File > Export | ActionEvent | onExportData() | 데이터 내보내기 |
| Preset 선택 | ItemEvent | onPresetChanged() | 프리셋 변경 |
| 입력 필드 | FocusEvent | onInputFocusLost() | 입력 검증 |

---

## 5. 데이터 흐름

### 5.1 데이터 흐름도

```
InputPanel
    ↓ (사용자 입력)
InputValidator
    ↓ (검증 통과)
DataModel
    ↓
SimulationController
    ↓
CalculationEngine
    ↓ (결과)
ResultsModel
    ↓
    ├──> ResultsPanel (테이블 업데이트)
    └──> VisualizationPanel (차트 업데이트)
```

### 5.2 클래스 간 상호작용

**MainFrame**과 다른 클래스 간의 관계:

```
MainFrame
  ├── owns ──> InputPanel
  ├── owns ──> VisualizationPanel
  ├── owns ──> ResultsPanel
  ├── delegates to ──> SimulationController
  ├── delegates to ──> FileHandler
  └── observes ──> DataModel (Observer Pattern)
```

---

## 6. 스타일 가이드

### 6.1 색상 팔레트

| 용도 | 색상 코드 | RGB | 설명 |
|------|----------|-----|------|
| Primary | #2196F3 | (33, 150, 243) | 메인 버튼, 강조 |
| Secondary | #757575 | (117, 117, 117) | 보조 버튼, 비활성 |
| Success | #4CAF50 | (76, 175, 80) | 성공 메시지 |
| Warning | #FF9800 | (255, 152, 0) | 경고 메시지 |
| Error | #F44336 | (244, 67, 54) | 오류 메시지 |
| Background | #FFFFFF | (255, 255, 255) | 기본 배경 |
| Border | #E0E0E0 | (224, 224, 224) | 테두리 |
| Text | #212121 | (33, 33, 33) | 기본 텍스트 |

### 6.2 폰트

| 용도 | 폰트 | 크기 | 스타일 |
|------|------|------|--------|
| 제목 | Dialog | 16pt | Bold |
| 서브 제목 | Dialog | 14pt | Bold |
| 본문 | Dialog | 12pt | Plain |
| 버튼 | Dialog | 12pt | Bold |
| 입력 필드 | Monospaced | 12pt | Plain |
| 상태바 | Dialog | 11pt | Plain |
| 결과 테이블 | Monospaced | 11pt | Plain |

### 6.3 여백 및 간격

| 요소 | 값 | 설명 |
|------|-----|------|
| 컴포넌트 간 기본 간격 | 10px | 표준 간격 |
| 패널 내부 여백 | 15px | Padding |
| 섹션 간 간격 | 20px | 큰 간격 |
| 버튼 높이 | 30px | 표준 버튼 |
| 입력 필드 높이 | 25px | 텍스트 필드 |
| 테두리 두께 | 1px | 표준 테두리 |

### 6.4 Look and Feel

**권장 설정**: 
- **Windows**: Windows Look and Feel
- **macOS**: Aqua Look and Feel  
- **Linux**: GTK+ Look and Feel
- **대체**: Nimbus Look and Feel (크로스 플랫폼)

**구현 예시**:
```java
try {
    UIManager.setLookAndFeel(
        UIManager.getSystemLookAndFeelClassName()
    );
} catch (Exception e) {
    // Fallback to Nimbus
    UIManager.setLookAndFeel(
        "javax.swing.plaf.nimbus.NimbusLookAndFeel"
    );
}
```

---

## 7. 구현 우선순위

### Phase 1: 기본 구조 (Week 1)
- [x] MainFrame 기본 구조
- [ ] MenuBar 구현
- [ ] ToolBar 구현
- [ ] StatusBar 구현
- [ ] 기본 레이아웃 설정

### Phase 2: 입력 인터페이스 (Week 2)
- [ ] MaterialPropertiesPanel 구현
- [ ] SpecimenDimensionsPanel 구현
- [ ] ControlButtonsPanel 구현
- [ ] 입력 검증 로직 구현
- [ ] PresetManagementPanel 구현

### Phase 3: 시각화 및 결과 (Week 3)
- [ ] VisualizationPanel 구현
- [ ] JFreeChart 통합
- [ ] ResultsPanel 구현
- [ ] 데이터 바인딩 구현

### Phase 4: 파일 처리 및 완성 (Week 4)
- [ ] 파일 입출력 기능
- [ ] 데이터 내보내기 기능
- [ ] 이벤트 핸들러 완성
- [ ] 통합 테스트
- [ ] UI/UX 개선

---

## 8. 테스트 체크리스트

### 8.1 기능 테스트
- [ ] 모든 메뉴 항목 동작 확인
- [ ] 모든 버튼 동작 확인
- [ ] 입력 검증 동작 확인
- [ ] 계산 기능 동작 확인
- [ ] 파일 저장/불러오기 동작 확인
- [ ] 데이터 내보내기 동작 확인
- [ ] 프리셋 저장/불러오기 동작 확인

### 8.2 UI/UX 테스트
- [ ] 반응형 레이아웃 확인
- [ ] 창 크기 조절 시 동작 확인
- [ ] 키보드 단축키 동작 확인
- [ ] 툴팁 표시 확인
- [ ] 상태바 메시지 표시 확인
- [ ] 그래프 줌/팬 동작 확인

### 8.3 성능 테스트
- [ ] 대용량 데이터 처리 확인
- [ ] 메모리 누수 확인
- [ ] 반응 속도 확인

### 8.4 호환성 테스트
- [ ] Windows 10/11에서 동작 확인
- [ ] macOS에서 동작 확인
- [ ] Linux에서 동작 확인
- [ ] 다양한 해상도에서 확인

---

## 9. 참고사항

### 9.1 주의사항
1. **Thread Safety**: Swing 컴포넌트는 EDT(Event Dispatch Thread)에서만 조작해야 함
2. **Memory Management**: 대용량 데이터 처리 시 메모리 관리 필요
3. **Exception Handling**: 모든 사용자 입력에 대한 예외 처리 필요
4. **Accessibility**: 키보드 네비게이션 지원 필수

### 9.2 확장 가능성
- 플러그인 시스템 추가 고려
- 다국어 지원 (i18n) 고려
- 테마 커스터마이징 기능 고려
- 클라우드 동기화 기능 고려

### 9.3 관련 문서
- [MEVA 프로젝트 요구사항 정의서](./README.md)
- [Java Swing 공식 문서](https://docs.oracle.com/javase/tutorial/uiswing/)
- [JFreeChart 문서](https://www.jfree.org/jfreechart/)

---

## 10. 변경 이력

| 버전 | 날짜 | 변경 내용 | 작성자 |
|------|------|-----------|--------|
| 1.0 | 2025-11-11 | 초기 문서 작성 | MEVA 개발팀 |

---

## 부록 A: 컴포넌트 크기 참조표

| 컴포넌트 | 최소 크기 | 권장 크기 | 최대 크기 |
|----------|-----------|-----------|----------|
| MainFrame | 1024x768 | 1280x800 | - |
| InputPanel | 250px | 300px | 400px |
| ResultsPanel | 200px | 250px | 350px |
| StatusBar | - | 25px | 25px |
| MenuBar | - | 25px | 25px |
| ToolBar | - | 40px | 40px |

---

**문서 끝**
