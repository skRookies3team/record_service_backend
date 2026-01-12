# 📔 Record Service Backend

**PetLog MSA 기록 서비스** - AI 기반 반려동물 일기 생성, 산책 경로 기록 및 월간 리캡 분석 서비스

## 📋 프로젝트 개요

Record Service는 PetLog MSA 프로젝트의 핵심 마이크로서비스 중 하나로, 반려동물의 일상을 다각도로 기록합니다. 사용자가 업로드한 사진을 AI가 분석하여 일기 초안을 작성해주고, 산책 경로를 공간 데이터(PostGIS)로 저장하며, 한 달간의 데이터를 종합하여 감동적인 월간 리캡을 생성합니다.

<img width="969" height="430" alt="스크린샷 2026-01-09 오후 12 53 57" src="https://github.com/user-attachments/assets/0e81aca3-eec4-496b-80f0-ed3f40dca864" />

### 핵심 가치

- **AI 기반 일기 어시스턴트**: 사진 분석을 통해 일기 내용, 날씨, 기분 등을 자동 추천 및 생성
- **정밀한 활동 추적**: PostGIS를 활용한 산책 경로(LineString) 저장 및 위치 기반 주소 복원
- **데이터 기반 리캡**: 한 달간의 기록을 AI로 분석하여 하이라이트와 요약 리포트 제공
- **이벤트 기반 아키텍처**: 일기 생성/수정 시 Kafka 이벤트를 발행하여 헬스케어 등 타 서비스와 연동
- **고성능 검색**: Milvus Vector DB를 연동하여 일기 내용의 시맨틱 검색 및 RAG 기초 데이터 제공

---

## 🚀 주요 기능

| 기능 | 설명 |
| --- | --- |
| **AI Diary Generation** | 사진 메타데이터와 AI 분석을 결합한 일기 초안 생성 및 커스텀 스타일 적용 |
| **Spatial Tracking** | 산책 경로(WalkRoute) 저장 및 특정 날짜의 대표 활동 위치 산출 (PostGIS) |
| **Monthly Recap** | 월별 일기/활동 데이터를 분석하여 자동 생성되는 카드 뉴스형 리포트 |
| **Weather & Address** | 기상청 ASOS 데이터 연동 및 Kakao 로컬 API를 통한 좌표-주소 변환 |
| **Event Streaming** | `diary-events` 토픽을 통한 일기 변경 내역 실시간 브로드캐스팅 |
| **Vector Storage** | 일기 본문의 벡터화를 통한 Milvus 인덱싱 및 지식 베이스 구축 |

---

## 🛠️ 기술 스택

### Backend Framework
- **Spring Boot 3.5.7** (Java 17)
- **Spring AI** (OpenAI 연동 및 Milvus Vector Store 통합)
- **Spring Data JPA & Querydsl** (관계형 데이터 처리)
- **Spring Data MongoDB** (사진 메타데이터 및 비정형 데이터 관리)
- **Spring Kafka** (이벤트 프로듀서 구현)
- **Spring Cloud OpenFeign** (User, Pet, Image 서비스 간 통신)

### AI & Database
- **OpenAI API (GPT-4o)** - 일기 생성 및 리캡 분석
- **PostgreSQL + PostGIS** - 산책 경로 및 공간 데이터 관리
- **Milvus** - 시맨틱 검색을 위한 Vector Database
- **MongoDB** - 이미지 소스 및 사진 메타데이터 저장

### Infrastructure & Tools
- **Apache Kafka** - 마이크로서비스 간 비동기 이벤트 통신
- **Docker & Docker Compose** - 로컬 개발 환경 컨테이너화
- **GitHub Actions** - CI/CD 파이프라인 자동화 (CI: Gradle build)
- **Swagger (SpringDoc)** - API 문서화 및 테스트

---

## 📁 프로젝트 구조

```text
src/main/java/com/petlog/record/
├── PetlogApplication.java        # 메인 실행 클래스
├── controller/                   # API 엔드포인트 (Diary, Location, Recap, Weather)
├── service/                      # 비즈니스 로직 인터페이스
│   └── impl/                     # 상세 구현체 (AI 연동, 공간 데이터 처리 등)
├── repository/
│   ├── jpa/                      # PostgreSQL/PostGIS 레포지토리
│   └── mongo/                    # MongoDB 레포지토리
├── entity/
│   ├── mongo/                    # MongoDB Document 엔티티
│   └── ...                       # JPA 기반 엔티티 (Geometry 포함)
├── infrastructure/
│   └── kafka/                    # Kafka Message Producer
├── config/                       # Milvus, Security, Swagger, RestTemplate 설정
└── util/                         # 날씨 격자 변환 등 공통 유틸리티
```

---

## ⚙️ 환경 설정

### 1. 사전 요구사항
* JDK 17 이상
* Docker 및 Docker Compose
* PostgreSQL (PostGIS 확장 기능 포함)
* MongoDB
* Milvus

### 2. 주요 환경 변수 (.yaml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/recorddb
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  data:
    mongodb:
      uri: mongodb://localhost:27017/record_photo
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
    vectorstore:
      milvus:
        host: localhost
        port: 19530

kakao:
  rest-api-key: ${KAKAO_API_KEY}

external:
  weather:
    api-key: ${WEATHER_SERVICE_KEY}
```

### 3. 로컬 실행

```bash
# 인프라 환경 구축 (DB, Kafka 등)
docker-compose up -d

# 애플리케이션 빌드 및 실행
./gradlew clean bootRun
```

---

## 📡 Kafka 통합 (Event Flow)

기록 서비스는 일기의 상태 변화를 감지하여 이벤트를 발행합니다.

```text
[Record Service] 
    (일기 작성/수정/삭제)
         ↓
    [Kafka Topic: diary-events] 
         ↓
    - Healthcare Service (RAG 데이터 업데이트)
    - Social Service (활동 피드 갱신)
    - User Service (활동 포인트/뱃지 부여)
```

### Kafka 구성 내역

| 항목 | 설정값 |
| --- | --- |
| **Topic** | `diary-events` |
| **Partition** | 3 |
| **Event Message** | `DiaryEventMessage` (ID, UserID, PetID, Type 등) |
| **Producer** | `DiaryEventProducer` |

---

## 📚 API 명세 (핵심 기능)

| Method | Endpoint | Description |
| --- | --- | --- |
| **POST** | `/api/diaries/preview` | AI 일기 초안 생성 (이미지 분석 포함) |
| **POST** | `/api/diaries` | 일기 저장 및 Kafka 이벤트 발행 |
| **POST** | `/api/locations` | 산책 위치(좌표) 실시간 저장 |
| **GET** | `/api/locations/representative` | 특정 날짜의 산책 대표 위치 조회 |
| **POST** | `/api/recaps/generate` | 월간 데이터 분석 및 리캡 생성 트리거 |
| **GET** | `/api/recaps/pet/{petId}` | 특정 반려동물의 리캡 기록 조회 |

---

## 🧑‍💻 개발 가이드

### Commit Convention
- `feat`: 새로운 기능 추가
- `fix`: 버그 수정
- `refactor`: 코드 리팩토링 (기능 변경 없음)
- `docs`: 문서 수정
- `test`: 테스트 코드 추가/수정

### Git Workflow
1. `main` 브랜치로부터 `feat/feature-name` 브랜치 생성
2. 기능 구현 후 `develop` 브랜치로 Pull Request 생성
3. 코드 리뷰 완료 후 Merge

---

## 👥 팀 정보

**Team 이음 (PetLog MSA Project)**

- **Organization**: skRookies3team
- **Repository**: record_service_backend

---

## 📄 License

MIT License
