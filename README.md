# Summer Project Local Web Service

Docker Compose 기반 로컬 웹서비스 실습 프로젝트입니다.

현재 목표는 frontend, backend, database, cache, reverse proxy를 하나의 Compose 환경에서 실행하고, 이후 backend를 Spring Boot로 전환하는 것입니다.

## 현재 아키텍처

```text
Browser
  -> Nginx
      -> /      -> Next.js frontend
      -> /api   -> FastAPI backend
                  -> PostgreSQL
                  -> Redis
```

현재 backend는 FastAPI로 구성되어 있지만, 최종 목표는 Spring Boot backend로 교체하는 것입니다.

FastAPI는 이번 프로젝트에서 최종 기술이라기보다 Docker Compose, Nginx reverse proxy, frontend-backend 연결 구조를 연습하기 위한 임시 backend입니다.

## 서비스 구성

| 서비스   | 역할                | 외부 포트 | 내부 주소     |
| -------- | ------------------- | --------- | ------------- |
| nginx    | reverse proxy       | 80        | nginx:80      |
| frontend | Next.js frontend    | 3000      | frontend:3000 |
| backend  | FastAPI backend     | 8000      | backend:8000  |
| postgres | PostgreSQL database | 5432      | postgres:5432 |
| redis    | Redis cache         | 6379      | redis:6379    |
| adminer  | DB admin UI         | 8080      | adminer:8080  |

## 실행 방법

```bash
docker compose up -d --build
```

컨테이너 상태 확인:

```bash
docker compose ps
```

로그 확인:

```bash
docker compose logs nginx
docker compose logs frontend
docker compose logs backend
```

전체 중지:

```bash
docker compose down
```

볼륨까지 삭제:

```bash
docker compose down -v
```

주의: `docker compose down -v`는 PostgreSQL volume 데이터도 삭제할 수 있습니다.

## 접속 주소

| 주소                         | 설명                            |
| ---------------------------- | ------------------------------- |
| http://localhost             | Nginx를 거친 Next.js frontend   |
| http://localhost/api/health  | Nginx를 거친 backend health API |
| http://localhost:3000        | Next.js frontend 직접 접속      |
| http://localhost:8000/health | FastAPI backend 직접 접속       |
| http://localhost:8080        | Adminer 접속                    |

## Nginx 라우팅 규칙

Nginx는 외부 요청을 path 기준으로 나눕니다.

```text
/      -> frontend:3000
/api/  -> backend:8000
```

예시:

```text
http://localhost
  -> Nginx
      -> frontend:3000

http://localhost/api/health
  -> Nginx
      -> backend:8000/health
```

## Nginx 설정 핵심

```nginx
location /api/ {
    proxy_pass http://backend:8000/;
}

location / {
    proxy_pass http://frontend:3000;
}
```

`/api/`의 `proxy_pass` 끝에는 `/`를 붙입니다.

```nginx
proxy_pass http://backend:8000/;
```

이렇게 하면 외부 요청 `/api/health`가 backend 내부에서는 `/health`로 전달됩니다.

frontend 쪽은 경로를 그대로 유지해야 하므로 끝에 `/`를 붙이지 않습니다.

```nginx
proxy_pass http://frontend:3000;
```

## Next.js HMR WebSocket 설정

Next.js 개발 서버는 코드 수정 시 자동 반영을 위해 HMR WebSocket을 사용합니다.

Nginx 뒤에서 Next.js dev server를 실행할 때는 WebSocket upgrade 헤더를 전달해야 합니다.

```nginx
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
```

이 설정이 없으면 브라우저 콘솔에 아래와 같은 오류가 주기적으로 발생할 수 있습니다.

```text
WebSocket connection to 'ws://localhost/_next/webpack-hmr?...' failed
```

## Frontend에서 Backend API 호출

Next.js frontend는 backend를 직접 `localhost:8000`으로 호출하지 않고, Nginx를 통해 `/api/health`를 호출합니다.

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
                  -> FastAPI backend
```

이 구조를 사용하면 frontend가 backend 내부 포트를 직접 알 필요가 없고, 배포 환경에서도 같은 API path 구조를 유지하기 쉽습니다.

## 확인 기준

아래 주소들이 정상 동작해야 합니다.

```text
http://localhost
http://localhost/api/health
http://localhost:3000
http://localhost:8000/health
http://localhost:8080
```

frontend 화면에서는 backend API 상태가 OK로 표시되어야 합니다.

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

- backend 또는 frontend 컨테이너가 꺼져 있음
- Nginx의 서비스 이름이 docker-compose.yml과 다름
- 포트 설정이 잘못됨

### 3. Backend API가 Checking에서 멈추는 경우

먼저 접속 주소를 확인합니다.

```text
http://localhost
```

에서 확인해야 합니다.

`http://localhost:3000`에서 보면 `fetch("/api/health")`가 Next.js dev server로 직접 요청될 수 있습니다.

직접 API 확인:

```bash
curl http://localhost/api/health
curl http://localhost:8000/health
```

### 4. WebSocket connection failed

Next.js 개발 서버의 HMR WebSocket이 Nginx를 통과하지 못한 경우입니다.

Nginx `location /`에 아래 설정을 추가합니다.

```nginx
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
```

## 현재까지 완료한 내용

- WSL2 / Ubuntu / VS Code 작업 흐름 구성
- Git 기본 설정 및 README 문서화 흐름 학습
- Docker 기본 명령어 학습
- Nginx 컨테이너 실행 및 내부 구조 확인
- Docker Compose로 PostgreSQL, Redis, Adminer 구성
- PostgreSQL dummy data 실습
- Redis CLI 테스트
- FastAPI backend 구성
- backend Dockerfile 작성
- Nginx reverse proxy로 `/api` 요청 backend 전달
- Next.js frontend 추가
- Nginx에서 `/` 요청 frontend 전달
- frontend에서 `/api/health` 호출

## 다음 단계

1. Spring Boot backend 프로젝트 생성
2. `/api/health` API 작성
3. Dockerfile 작성
4. Compose에서 FastAPI backend를 Spring Boot backend로 교체
5. Spring Boot에서 PostgreSQL 연결
6. Spring Boot에서 Redis 연결
