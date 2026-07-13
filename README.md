# Finel Local Server Deployment Practice

이 프로젝트는 기존 `finel` 서비스를 로컬 서버에서 실행하기 위한 배포 구조를 학습하고 검증하는 프로젝트입니다.

최종 목표는 `finel` 프로젝트를 Docker Compose 기반 로컬 서버 환경에서 실행하는 것입니다.

현재는 바로 기존 `finel`을 올리기 전에, 작은 예제 서비스로 아래 구조를 먼저 완성하고 있습니다.

```text
Browser
  -> Nginx
      -> /      -> Next.js frontend
      -> /api   -> Spring Boot backend
                  -> PostgreSQL
                  -> Redis
```

이 구조를 먼저 안정적으로 만든 뒤, 연습용 frontend/backend를 기존 `finel` 서비스로 교체하는 것이 최종 목표입니다.

---

## 프로젝트 목적

최종적으로 하고 싶은 일은 단순히 코드를 실행하는 것이 아니라, 실제 서비스처럼 로컬 서버에서 여러 구성 요소를 함께 실행하는 것입니다.

목표 구조는 다음과 같습니다.

```text
Local Server
  -> Docker Compose
      -> Nginx reverse proxy
      -> finel frontend
      -> finel backend
      -> PostgreSQL
      -> Redis
      -> Adminer
```

현재 프로젝트에서는 `finel`을 바로 올리기 전에 아래 내용을 먼저 학습하고 검증합니다.

- Docker Compose로 여러 서비스 실행
- Nginx reverse proxy 구성
- frontend/backend 라우팅 분리
- Spring Boot backend Docker 실행
- PostgreSQL, Redis 컨테이너 구성
- frontend에서 `/api` 경로로 backend 호출
- 나중에 기존 `finel` 프로젝트로 교체할 수 있는 기준 구조 만들기

---

## 현재 아키텍처

현재 학습용 구조는 다음과 같습니다.

```text
Browser
  -> Nginx
      -> /      -> Next.js frontend
      -> /api   -> Spring Boot backend
                  -> PostgreSQL
                  -> Redis
```

현재 `backend`는 Spring Boot로 구성되어 있습니다.

처음에는 FastAPI를 임시 backend로 사용했지만, 현재는 Spring Boot backend로 교체했습니다.

---

## 최종 목표 아키텍처

최종적으로는 현재 연습용 서비스를 기존 `finel` 서비스로 교체합니다.

```text
Browser
  -> Nginx
      -> /      -> finel frontend
      -> /api   -> finel backend
                  -> PostgreSQL
                  -> Redis
```

즉, 현재 프로젝트의 핵심은 다음을 미리 검증하는 것입니다.

```text
Nginx
Docker Compose
frontend/backend 분리
DB/Redis 컨테이너
로컬 서버 배포 흐름
```

이 구조가 안정되면 `frontend`, `backend` 자리에 기존 `finel` 프로젝트를 연결합니다.

---

## 서비스 구성

| 서비스   | 역할                | 외부 포트 | 내부 주소     |
| -------- | ------------------- | --------- | ------------- |
| nginx    | reverse proxy       | 80        | nginx:80      |
| frontend | Next.js frontend    | 3000      | frontend:3000 |
| backend  | Spring Boot backend | 8082      | backend:8080  |
| postgres | PostgreSQL database | 5432      | postgres:5432 |
| redis    | Redis cache         | 6379      | redis:6379    |
| adminer  | DB admin UI         | 8080      | adminer:8080  |

주의:

- 외부 포트 `8082`는 Spring Boot backend 직접 확인용입니다.
- Nginx는 외부 포트가 아니라 Compose 내부 주소인 `backend:8080`으로 backend에 접근합니다.
- PostgreSQL과 Redis는 현재 Compose에 포함되어 있지만, Spring Boot 코드와의 실제 연결은 다음 단계에서 진행합니다.

---

## 실행 방법

프로젝트 루트에서 실행합니다.

```bash
docker compose up -d --build
```

만약 compose 파일명이 `compose.yml`이라면 다음 명령어를 사용합니다.

```bash
docker compose -f compose.yml up -d --build
```

컨테이너 상태 확인:

```bash
docker compose ps
```

또는:

```bash
docker compose -f compose.yml ps
```

로그 확인:

```bash
docker compose logs nginx
docker compose logs frontend
docker compose logs backend
```

또는:

```bash
docker compose -f compose.yml logs nginx
docker compose -f compose.yml logs frontend
docker compose -f compose.yml logs backend
```

전체 중지:

```bash
docker compose down
```

볼륨까지 삭제:

```bash
docker compose down -v
```

주의:

```text
docker compose down -v
```

는 PostgreSQL volume 데이터도 삭제할 수 있습니다.

---

## 접속 주소

| 주소                         | 설명                                        |
| ---------------------------- | ------------------------------------------- |
| http://localhost             | Nginx를 거친 frontend                       |
| http://localhost/api/health  | Nginx를 거친 Spring Boot backend health API |
| http://localhost:3000        | Next.js frontend 직접 접속                  |
| http://localhost:8082/health | Spring Boot backend 직접 접속               |
| http://localhost:8080        | Adminer 접속                                |

---

## 요청 흐름

### Frontend 요청

```text
http://localhost
  -> Nginx
      -> frontend:3000
```

### Backend API 요청

```text
http://localhost/api/health
  -> Nginx
      -> backend:8080/health
```

### Frontend에서 backend 호출

Next.js frontend는 backend를 직접 `localhost:8082`로 호출하지 않습니다.

대신 Nginx를 통해 상대 경로로 호출합니다.

```ts
const response = await fetch("/api/health");
```

요청 흐름:

```text
Browser
  -> http://localhost
      -> Next.js frontend
          -> fetch("/api/health")
              -> Nginx
                  -> Spring Boot backend
```

이 구조를 사용하면 frontend가 backend 내부 포트를 직접 알 필요가 없습니다.

나중에 `finel` 프로젝트로 교체할 때도 같은 방식으로 `/api` 경로를 유지할 수 있습니다.

---

## Nginx 라우팅 규칙

Nginx는 요청 path를 기준으로 frontend와 backend를 나눕니다.

```text
/      -> frontend:3000
/api/  -> backend:8080
```

핵심 설정:

```nginx
location /api/ {
    proxy_pass http://backend:8080/;
}

location / {
    proxy_pass http://frontend:3000;
}
```

---

## proxy_pass 슬래시 규칙

### Backend

```nginx
location /api/ {
    proxy_pass http://backend:8080/;
}
```

이 설정에서는 외부 요청:

```text
/api/health
```

가 backend 내부에서는:

```text
/health
```

로 전달됩니다.

Spring Boot controller에는 다음과 같은 API가 있습니다.

```java
@GetMapping("/health")
```

그래서 `/api/health` 요청이 정상적으로 Spring Boot의 `/health` API로 연결됩니다.

### Frontend

```nginx
location / {
    proxy_pass http://frontend:3000;
}
```

frontend는 `/`, `/dashboard`, `/products` 같은 경로를 그대로 받아야 하므로 `proxy_pass` 끝에 `/`를 붙이지 않습니다.

---

## Next.js HMR WebSocket 설정

Next.js 개발 서버는 코드 수정 시 자동 반영을 위해 HMR WebSocket을 사용합니다.

Nginx 뒤에서 Next.js dev server를 실행할 때는 WebSocket upgrade header를 전달해야 합니다.

```nginx
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
```

이 설정이 없으면 브라우저 콘솔에 다음과 같은 오류가 반복될 수 있습니다.

```text
WebSocket connection to 'ws://localhost/_next/webpack-hmr?...' failed
```

이 오류는 backend API 문제가 아니라 Next.js 개발 서버의 HMR WebSocket 연결 문제입니다.

---

## Spring Boot Backend

현재 backend는 Spring Boot로 구성되어 있습니다.

확인 API:

```text
GET /health
```

응답 예시:

```json
{
  "status": "ok",
  "service": "spring-backend"
}
```

Nginx를 거친 확인:

```bash
curl http://localhost/api/health
```

backend 직접 확인:

```bash
curl http://localhost:8082/health
```

---

## Spring Boot Dockerfile

Spring Boot backend는 Docker multi-stage build로 실행합니다.

예시:

```dockerfile
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

이 방식은 프로젝트에 포함된 Gradle Wrapper를 사용합니다.

장점:

- Docker 이미지의 Gradle 버전에 덜 의존함
- 프로젝트가 지정한 Gradle 버전으로 빌드함
- Spring Boot 버전과 Gradle 버전 충돌 가능성을 줄임

---

## 확인 기준

아래 주소들이 정상 동작해야 합니다.

```text
http://localhost
http://localhost/api/health
http://localhost:3000
http://localhost:8082/health
http://localhost:8080
```

frontend 화면에서는 backend API 상태가 OK로 표시되어야 합니다.

예상 메시지:

```text
Backend 응답 성공: {"status":"ok","service":"spring-backend"}
```

---

## 트러블슈팅

### 1. http://localhost에서 Nginx 기본 화면이 보이는 경우

원인 후보:

- `nginx/default.conf`가 수정되지 않음
- Nginx 컨테이너에 설정 파일이 mount되지 않음
- 기존 Nginx 컨테이너에 접속 중

확인:

```bash
cat nginx/default.conf
docker compose exec nginx cat /etc/nginx/conf.d/default.conf
docker ps
```

---

### 2. 502 Bad Gateway

Nginx가 뒤쪽 서비스에 연결하지 못한 상태입니다.

확인:

```bash
docker compose ps
docker compose logs nginx
docker compose logs backend
docker compose logs frontend
```

주요 원인:

- backend 컨테이너가 꺼져 있음
- frontend 컨테이너가 꺼져 있음
- Nginx의 서비스 이름이 compose 파일과 다름
- Nginx가 잘못된 포트로 요청을 전달함
- Spring Boot는 내부 포트 `8080`인데 Nginx가 `8000`으로 보고 있음

Spring Boot backend 사용 시 Nginx 설정은 다음과 같아야 합니다.

```nginx
proxy_pass http://backend:8080/;
```

---

### 3. Backend API가 Checking에서 멈추는 경우

먼저 접속 주소를 확인합니다.

```text
http://localhost
```

에서 확인해야 합니다.

`http://localhost:3000`에서 보면 `fetch("/api/health")`가 Nginx가 아니라 Next.js dev server로 직접 요청될 수 있습니다.

직접 API 확인:

```bash
curl http://localhost/api/health
curl http://localhost:8082/health
```

---

### 4. WebSocket connection failed

Next.js 개발 서버의 HMR WebSocket이 Nginx를 통과하지 못한 경우입니다.

Nginx `location /`에 아래 설정을 추가합니다.

```nginx
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
```

---

### 5. Spring Boot Docker build 실패

Spring Boot와 Gradle 버전이 맞지 않으면 Docker build가 실패할 수 있습니다.

권장 조합:

```text
Java 17
Spring Boot 3.x
Gradle Wrapper 사용
```

Dockerfile에서는 가능하면 Docker 이미지의 `gradle` 명령이 아니라 프로젝트의 Gradle Wrapper를 사용합니다.

```dockerfile
RUN ./gradlew clean bootJar --no-daemon
```

---

### 6. Whitelabel Error Page 404

Spring Boot에서 다음 화면이 보일 수 있습니다.

```text
Whitelabel Error Page
status=404
```

이 경우는 보통 요청한 경로에 Controller가 없다는 뜻입니다.

확인해야 할 주소:

```text
http://localhost:8082/health
```

`http://localhost:8082`는 `/` mapping이 없으면 404가 날 수 있습니다.

---

## 현재까지 완료한 내용

- WSL2 / Ubuntu / VS Code 작업 흐름 구성
- Git 기본 설정
- GitHub 업로드
- Docker 기본 명령어 학습
- Nginx 컨테이너 실행 및 내부 구조 확인
- Docker Compose로 PostgreSQL, Redis, Adminer 구성
- PostgreSQL dummy data 실습
- Redis CLI 테스트
- FastAPI 임시 backend 구성
- Nginx reverse proxy로 `/api` 요청 backend 전달
- Next.js frontend 추가
- Nginx에서 `/` 요청 frontend 전달
- frontend에서 `/api/health` 호출
- Next.js HMR WebSocket 문제 해결
- Spring Boot backend 생성
- Spring Boot `/health` API 작성
- Spring Boot Dockerfile 작성
- Spring Boot Docker build 성공
- Docker Compose backend를 Spring Boot로 교체
- Nginx `/api/health` 요청을 Spring Boot backend로 전달

---

## 다음 단계

1. README 최신화 후 GitHub push
2. Spring Boot에서 PostgreSQL 연결
3. Spring Boot에서 Redis 연결
4. 간단한 실제 API 작성
   - 예: `/products`
   - 예: `/inquiries`
5. HTTPS 자체 서명 테스트
6. n8n 자동화 추가
7. Ollama 로컬 LLM 연동
8. Qdrant/RAG 구성
9. 기존 `finel` 프로젝트를 현재 구조에 맞게 교체
10. 미니PC 또는 로컬 서버에 배포

---

## 최종 목표

이 프로젝트의 최종 목표는 연습용 frontend/backend를 계속 유지하는 것이 아닙니다.

최종 목표는 다음과 같습니다.

```text
기존 finel 프로젝트를
Docker Compose + Nginx 기반 로컬 서버에서 실행한다.
```

현재 프로젝트는 그 목표를 위해 필요한 배포 구조를 단계별로 검증하는 학습용 인프라입니다.
