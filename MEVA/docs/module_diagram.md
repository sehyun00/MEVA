# 검증 모듈 클래스 다이어그램

```text
+---------------------+
|   ValidationManager |
+---------------------+
| - validators: List  |
+---------------------+
| +validate(data)     |
+---------------------+
           |
           |
    +----------------+
    | Validator      |
    +----------------+
    | +validate()    |
    +----------------+
     /       |       \
+----------------+ +---------------------+ +-------------+
| InputValidator | | CalculationValidator | | FileValidator |
+----------------+ +---------------------+ +-------------+
| +checkRange()  | | +checkLimits()      | | +readFile() |
| +checkFormat() | | +compareSS400()     | | +writeFile()|
| +checkLogic()  | +---------------------+ +-------------+