# CUBRID Migration Toolkit (CMT) E2E Smoke Test 자동화 코드
이 프로젝트는 CUBRID Migration Toolkit (CMT) 콘솔 애플리케이션의 End-to-End(E2E) Smoke Test를 자동화하는 것이 목적입니다.

## 1. 프로젝트 개요
`cmt_e2e_smoke_poc`는 CMT의 주요 기능들이 예상대로 동작하는지 확인하는 자동화된 테스트 스위트입니다.
이 프로젝트는 CMT의 콘솔 명령어를 실행하고, 그 결과를 예상된 답변 파일과 비교하여 성공 여부를 판단합니다. 
Docker를 활용하여 격리된 테스트 환경을 구축하고, Testcontainers 라이브러리를 통해 CUBRID 데이터베이스 인스턴스를 동적으로 생성하여 테스트를 수행합니다. 
이를 통해 CMT의 안정성과 신뢰성을 보장하는 것을 목표로 합니다.

**주요 기능:**
 - 명령어 기반 테스트: `help`, `script`, `log`, `report` 등 CMT의 다양한 콘솔 명령어를 테스트합니다.
 - 자동 검증: 각 테스트 케이스의 실행 결과를 미리 정의된 `.answer` 파일과 비교하여 일치 여부를 자동으로 검증합니다.
 - 격리된 테스트 환경: Docker와 Testcontiners를 사용하여 각 테스트가 독립적인 데이터베이스 환경에서 실행되도록 보장합니다.
 - Jenkins 연동: Jenkinsfile을 포함하여 CI/CD 파이프라인에 쉽게 통합할 수 있습니다.

 ## 2. 사전 요구 사항
  - Java Development Kit(JDK) 21 이상: 프로젝트 빌드 및 실행을 위해 필요합니다.
  - Maven: 의존성 관리 및 프로젝트 빌드를 위해 사용됩니다.
  - Docker: 격리된 테스트 환경 구성을 위해 필수적입니다. Docker가 설치되어 있고, 실행 중인지 확인해주세요.

## 3. 설치 및 설정
 1. 프로젝트 클론:
```bash
git clone https://github.com/CUBRID/cmt_e2e_smoke_poc.git
cd cmt_e2e_smoke_poc
```
 2. Maven 의존성 설정:
프로젝트 루트 디렉터리에서 다음 명령어를 실행하여 필요한 라이브러리를 다운로드하고 프로젝트를 빌드합니다.
```bash
mvn clean install
```

## 4. 테스트 실행
1. 전체 테스트 실행
프로젝트의 모든 테스트를 실행하려면 다음 Maven 명령어를 사용합니다.
```bash
mvn test
```

2. 특정 테스트 실행
특정 테스트 클래스나 메소드만 실행할 수도 있습니다. 예를 들어 `HelpTest` 클래스만 실행하려면 다음과 같이 실행합니다.
```bash
mvn -Dtest=HelpTest test
```

## 5. 프로젝트 구조
```
cmt_e2e_smoke_poc/
├── pom.xml                      # Maven 프로젝트 설정 파일
├── Jenkinsfile                  # Jenkins CI/CD 파이프라인 설정
├── e2e-test.properties          # E2E 테스트를 위한 주요 설정 파일
└── src/
    ├── test/
        ├── java/com/cmt/e2e/
        │   ├── framework/       # 테스트 프레임워크 핵심 로직
        │   │   ├── assertion/   # 결과 검증 관련 클래스
        │   │   ├── command/     # CMT 명령어 실행 관련 클래스
        │   │   ├── core/        # 테스트 기본 클래스 및 유틸리티
        │   │   ├── db/          # 데이터베이스 및 Testcontainers 관련 클래스
        │   │   └── junit/       # JUnit 확장 기능
        │   └── tests/           # 실제 테스트 케이스
        │       
        └── resources/           # 테스트에 사용되는 리소스 파일
            ├── driver/          # JDBC 드라이버
            ├── logback-test.xml # 테스트용 로깅 설정
            └── tests/           # 테스트 케이스별 예상 결과(.answer) 파일
```
