# Week02 Docker Compose Local Deployment

## 목표

이 문서는 Docker Compose로 로컬 개발 인프라를 구성한 내용을 정리한다.

이번 단계에서 만든 구조는 다음과 같다.

```text
Browser
  -> Nginx
      -> FastAPI backend
      -> PostgreSQL
      -> Redis
      -> Adminer
```

핵심 목표는 여러 컨테이너를 하나의 `docker-compose.yml`로 묶고, Nginx가 외부 요청을 받아 backend 서비스로 전달하는 구조를 이해하는 것이다.

## 전체 구성

```text
week02-compose/
  docker-compose.yml
  backend/
    main.py
    requirements.txt
    Dockerfile
  nginx/
    default.conf
  README.md
```

## 서비스 역할

### PostgreSQL

PostgreSQL은 관계형 데이터베이스다.

이번 실습에서는 `appdb` 데이터베이스를 만들고, Adminer를 통해 `products` 테이블과 더미 데이터를 생성했다.

나중에 실제 프로젝트에서는 다음과 같은 데이터를 저장할 수 있다.

- 사용자 요청
- 메일 첨부파일 정보
- 부품 모델명
- 검색 결과
- 답변 초안
- 작업 로그

### Redis

Redis는 빠른 key-value 저장소다.

PostgreSQL처럼 테이블을 만드는 DB가 아니라, 다음처럼 값을 저장하고 꺼내는 방식이다.

```redis
set app:status running
get app:status
```

나중에는 다음 용도로 사용할 수 있다.

- 캐시
- 임시 작업 상태
- 중복 처리 방지
- 작업 큐
- 자동화 실행 상태 저장

### Adminer

Adminer는 브라우저에서 PostgreSQL을 관리할 수 있게 해주는 웹 도구다.

Adminer 자체가 데이터베이스는 아니다. PostgreSQL에 접속해서 테이블을 만들고 데이터를 확인하는 관리 화면이다.

접속 주소:

```text
http://localhost:8081
```

로그인 정보:

```text
System: PostgreSQL
Server: postgres
Username: chan
Password: chan1234
Database: appdb
```

여기서 `Server`에 `postgres`를 쓰는 이유는 `docker-compose.yml`의 서비스 이름이 `postgres`이기 때문이다.

```yaml
services:
  postgres:
```

Compose 내부에서는 서비스 이름이 컨테이너끼리 통신할 때 사용하는 주소가 된다.

### FastAPI backend

FastAPI는 이번 실습에서 사용하는 연습용 백엔드 서버다.

최종적으로 Spring Boot를 사용할 예정이더라도, 지금 FastAPI로 배우는 구조는 그대로 사용할 수 있다.

중요한 것은 FastAPI 자체가 아니라 다음 개념이다.

- backend 서버를 Docker 이미지로 만드는 법
- backend 컨테이너를 Compose에 추가하는 법
- Nginx가 backend 서비스로 요청을 전달하는 법
- backend가 PostgreSQL/Redis와 같은 Compose 네트워크 안에서 실행되는 구조

나중에 Spring Boot로 바꿀 때는 backend 내부 구현과 포트만 바뀐다.

```text
FastAPI: backend:8000
Spring Boot: backend:8080
```

### Nginx

Nginx는 외부 요청을 받아 적절한 내부 서비스로 전달하는 역할을 한다.

이번 실습에서는 다음 요청 흐름을 만든다.

```text
http://localhost/api/health
  -> Nginx
  -> backend:8000
  -> FastAPI /health
```

이런 구조를 reverse proxy라고 한다.

Nginx를 쓰는 이유는 다음과 같다.

- 외부 진입점을 하나로 통일한다.
- `/api` 요청은 backend로 보낸다.
- `/` 요청은 frontend로 보낼 수 있다.
- 나중에 HTTPS 설정을 붙일 수 있다.
- 여러 서비스를 하나의 주소 뒤에 묶을 수 있다.

## docker-compose.yml 예시

```yaml
services:
  postgres:
    image: postgres:16
    container_name: week02-postgres
    environment:
      POSTGRES_USER: chan
      POSTGRES_PASSWORD: chan1234
      POSTGRES_DB: appdb
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

  redis:
    image: redis:7
    container_name: week02-redis
    ports:
      - "6379:6379"

  adminer:
    image: adminer
    container_name: week02-adminer
    ports:
      - "8081:8080"
    depends_on:
      - postgres

  backend:
    build:
      context: ./backend
    container_name: week02-backend
    ports:
      - "8000:8000"
    depends_on:
      - postgres
      - redis

  nginx:
    image: nginx
    container_name: week02-nginx
    ports:
      - "80:80"
    volumes:
      - ./nginx/default.conf:/etc/nginx/conf.d/default.conf:ro
    depends_on:
      - backend

volumes:
  postgres-data:
```

## Compose 핵심 개념

### services

`services` 아래에는 실행할 컨테이너들을 적는다.

```yaml
services:
  postgres:
  redis:
  adminer:
  backend:
  nginx:
```

각 이름은 Compose 내부 네트워크에서 주소처럼 사용된다.

예를 들어 Nginx는 backend를 이렇게 찾는다.

```nginx
proxy_pass http://backend:8000/;
```

Adminer는 PostgreSQL을 이렇게 찾는다.

```text
Server: postgres
```

파일에서 위에 있어서 `postgres`가 되는 것이 아니다. `services` 아래의 키 이름이 `postgres`라서 서버 이름이 `postgres`가 된다.

### image

`image`는 Docker Hub 등에서 가져올 이미지를 의미한다.

```yaml
image: postgres:16
image: redis:7
image: adminer
image: nginx
```

이미 만들어진 공식 이미지를 사용할 때 쓴다.

### build

`build`는 직접 만든 코드를 Docker 이미지로 만들 때 사용한다.

```yaml
backend:
  build:
    context: ./backend
```

이 설정은 `./backend` 폴더의 `Dockerfile`을 읽어서 backend 이미지를 직접 빌드하라는 뜻이다.

### ports

`ports`는 내 컴퓨터 포트와 컨테이너 내부 포트를 연결한다.

형식:

```text
"내 컴퓨터 포트:컨테이너 내부 포트"
```

예시:

```yaml
ports:
  - "8081:8080"
```

의미:

```text
내 컴퓨터 8081번 포트
  -> 컨테이너 내부 8080번 포트
```

그래서 Adminer는 브라우저에서 다음 주소로 접속한다.

```text
http://localhost:8081
```

### volumes

`volumes`는 컨테이너가 삭제되어도 데이터를 유지하기 위해 사용한다.

PostgreSQL 설정:

```yaml
volumes:
  - postgres-data:/var/lib/postgresql/data
```

의미:

```text
PostgreSQL 컨테이너 내부 /var/lib/postgresql/data
  -> Docker volume postgres-data
```

PostgreSQL은 실제 DB 데이터를 `/var/lib/postgresql/data`에 저장한다.

컨테이너를 내렸다가 다시 올려도 데이터가 남는 이유는 이 경로가 Docker volume에 연결되어 있기 때문이다.

주의:

```bash
docker compose down
```

이 명령어는 컨테이너와 네트워크를 내리지만 volume은 유지한다.

```bash
docker compose down -v
```

이 명령어는 volume까지 삭제한다. PostgreSQL 데이터가 사라질 수 있으므로 조심해야 한다.

### depends_on

`depends_on`은 컨테이너 생성 순서를 지정한다.

```yaml
adminer:
  depends_on:
    - postgres
```

의미:

```text
adminer는 postgres 컨테이너가 먼저 만들어진 다음 실행한다.
```

단, `depends_on`은 PostgreSQL이 완전히 접속 가능한 상태가 될 때까지 기다린다는 뜻은 아니다.

## FastAPI 코드

파일 위치:

```text
backend/main.py
```

내용:

```python
from fastapi import FastAPI

app = FastAPI()

@app.get("/health")
def health_check():
    return {
        "status": "ok",
        "service": "backend"
    }

@app.get("/")
def root():
    return {
        "message": "FastAPI backend is running"
    }
```

### `/health`

서버 상태를 확인하기 위한 API다.

접속:

```text
http://localhost:8000/health
```

응답:

```json
{
  "status": "ok",
  "service": "backend"
}
```

## requirements.txt

파일 위치:

```text
backend/requirements.txt
```

내용:

```txt
fastapi
uvicorn[standard]
```

의미:

- `fastapi`: API 서버를 만드는 프레임워크
- `uvicorn`: FastAPI 앱을 실제 HTTP 서버로 실행하는 프로그램

## FastAPI Dockerfile

파일 위치:

```text
backend/Dockerfile
```

내용:

```dockerfile
FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

COPY main.py .

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

### Dockerfile 설명

```dockerfile
FROM python:3.12-slim
```

Python 3.12가 설치된 가벼운 Linux 이미지를 기반으로 사용한다.

```dockerfile
WORKDIR /app
```

컨테이너 안의 작업 폴더를 `/app`으로 설정한다.

```dockerfile
COPY requirements.txt .
```

내 컴퓨터의 `requirements.txt`를 컨테이너 안 `/app` 폴더로 복사한다.

```dockerfile
RUN pip install --no-cache-dir -r requirements.txt
```

필요한 Python 패키지를 설치한다.

```dockerfile
COPY main.py .
```

FastAPI 코드 파일을 컨테이너 안으로 복사한다.

```dockerfile
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

컨테이너가 실행될 때 FastAPI 서버를 8000번 포트에서 실행한다.

`main:app`은 `main.py` 파일 안의 `app` 객체를 의미한다.

`--host 0.0.0.0`은 컨테이너 외부에서도 접근 가능하게 서버를 여는 설정이다.

## Nginx 설정

파일 위치:

```text
nginx/default.conf
```

내용:

```nginx
server {
    listen 80;

    location /api/ {
        proxy_pass http://backend:8000/;
    }

    location / {
        return 200 "Nginx is running\n";
        add_header Content-Type text/plain;
    }
}
```

### 설정 설명

```nginx
listen 80;
```

Nginx가 컨테이너 내부 80번 포트에서 요청을 받는다.

```nginx
location /api/ {
    proxy_pass http://backend:8000/;
}
```

`/api/`로 시작하는 요청을 backend 서비스의 8000번 포트로 전달한다.

예시:

```text
브라우저 요청:
http://localhost/api/health

Nginx가 전달:
http://backend:8000/health
```

여기서 `backend`는 Compose 서비스 이름이다.

```nginx
location / {
    return 200 "Nginx is running\n";
    add_header Content-Type text/plain;
}
```

`/`로 들어오는 요청은 Nginx가 직접 응답한다.

확인 주소:

```text
http://localhost
```

응답:

```text
Nginx is running
```

## 실행 방법

`docker-compose.yml`이 있는 폴더에서 실행한다.

```bash
docker compose up -d --build
```

의미:

```text
docker-compose.yml을 읽고
필요한 이미지를 빌드하거나 다운로드하고
컨테이너들을 백그라운드에서 실행한다.
```

`--build`는 Dockerfile 변경사항을 반영해서 이미지를 다시 빌드하라는 뜻이다.

## 상태 확인

```bash
docker compose ps
```

확인할 컨테이너:

```text
week02-postgres
week02-redis
week02-adminer
week02-backend
week02-nginx
```

## 접속 확인

### Nginx 직접 확인

```text
http://localhost
```

정상 응답:

```text
Nginx is running
```

### FastAPI 직접 확인

```text
http://localhost:8000/health
```

정상 응답:

```json
{
  "status": "ok",
  "service": "backend"
}
```

### Nginx를 통한 FastAPI 확인

```text
http://localhost/api/health
```

정상 응답:

```json
{
  "status": "ok",
  "service": "backend"
}
```

이 요청 흐름은 다음과 같다.

```text
Browser
  -> http://localhost/api/health
  -> Nginx
  -> backend:8000
  -> FastAPI /health
```

### Adminer 확인

```text
http://localhost:8081
```

로그인:

```text
System: PostgreSQL
Server: postgres
Username: chan
Password: chan1234
Database: appdb
```

## 로그 확인

Backend 로그:

```bash
docker logs week02-backend
```

Nginx 로그:

```bash
docker logs week02-nginx
```

PostgreSQL 로그:

```bash
docker logs week02-postgres
```

Redis 로그:

```bash
docker logs week02-redis
```

## 자주 생기는 문제

### 80번 포트 충돌

증상:

```text
Bind for 0.0.0.0:80 failed
```

원인:

```text
이미 다른 프로그램이 내 컴퓨터의 80번 포트를 사용 중이다.
```

해결:

```yaml
nginx:
  ports:
    - "8082:80"
```

그 후 접속:

```text
http://localhost:8082
```

### 502 Bad Gateway

증상:

```text
502 Bad Gateway
```

의미:

```text
Nginx가 backend 서비스로 요청을 넘기지 못했다.
```

확인할 것:

```bash
docker compose ps
docker logs week02-backend
docker logs week02-nginx
```

주요 원인:

- backend 컨테이너가 실행 중이 아님
- Nginx 설정의 서비스 이름이 틀림
- backend 포트가 틀림
- FastAPI 서버가 0.0.0.0이 아니라 127.0.0.1로 실행됨

### Adminer에서 PostgreSQL 접속 실패

확인할 것:

```text
System: PostgreSQL
Server: postgres
Username: chan
Password: chan1234
Database: appdb
```

주의:

```text
Server에 localhost를 쓰면 안 된다.
Adminer 컨테이너 입장에서 localhost는 자기 자신이다.
PostgreSQL은 다른 컨테이너이므로 서비스 이름인 postgres를 써야 한다.
```

### 데이터가 사라진 경우

확인할 명령어:

```bash
docker volume ls
```

주의:

```bash
docker compose down -v
```

이 명령어를 실행하면 PostgreSQL 데이터가 저장된 volume까지 삭제될 수 있다.

## 종료 방법

컨테이너 중지 및 삭제:

```bash
docker compose down
```

데이터까지 삭제:

```bash
docker compose down -v
```

`-v` 옵션은 volume을 삭제하므로 신중하게 사용한다.

## 오늘 배운 핵심

- Docker Compose는 여러 컨테이너를 하나의 설정 파일로 실행한다.
- Compose 서비스 이름은 컨테이너끼리 통신할 때 주소가 된다.
- 브라우저에서 접속할 때는 `localhost`를 사용한다.
- 컨테이너끼리 통신할 때는 `postgres`, `redis`, `backend` 같은 서비스 이름을 사용한다.
- Nginx는 외부 요청을 받아 backend로 전달하는 reverse proxy 역할을 한다.
- FastAPI는 지금은 연습용 backend지만, 나중에 Spring Boot로 교체할 수 있다.
- PostgreSQL 데이터는 Docker volume에 저장해야 컨테이너를 다시 만들어도 유지된다.

## Frontend 추가

Next.js 기반 frontend 서비스를 추가했다.

### 현재 서비스 구조

```text
Browser
  -> localhost:3000        -> Next.js frontend
  -> localhost:8000/health -> FastAPI backend
  -> localhost/api/health  -> Nginx reverse proxy -> backend
```

### frontend 실행 방식

frontend는 Docker Compose의 `frontend` 서비스로 실행된다.

```yaml
frontend:
  build: ./frontend
  ports:
    - "3000:3000"
```

### 실행 명령어

```bash
docker compose up -d --build
docker compose ps
docker compose logs frontend
```

### 확인 주소

- Frontend: http://localhost:3000
- Backend direct: http://localhost:8000/health
- Backend through Nginx: http://localhost/api/health

## Nginx 라우팅 분리

Nginx reverse proxy를 사용하여 frontend와 backend 요청을 분리했다.

### 목표 구조

```text
Browser
  -> Nginx
      -> /      -> Next.js frontend
      -> /api   -> FastAPI backend
```

### Nginx 설정

```nginx
server {
    listen 80;

    location /api/ {
        proxy_pass http://backend:8000/;

        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        proxy_pass http://frontend:3000;

        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 확인 주소

- Frontend via Nginx: http://localhost
- Backend via Nginx: http://localhost/api/health
- Frontend direct: http://localhost:3000
- Backend direct: http://localhost:8000/health

### 실행 명령어

```bash
docker compose up -d --build
docker compose ps
docker compose logs nginx
```

### 정리

- `/` 요청은 frontend 서비스로 전달된다.
- `/api/` 요청은 backend 서비스로 전달된다.
- Docker Compose 내부에서는 서비스 이름이 네트워크 주소처럼 사용된다.
- Nginx 설정에서 `localhost`가 아니라 `frontend`, `backend` 서비스 이름을 사용해야 한다.

## Frontend에서 Backend API 호출

Next.js frontend 화면에서 Nginx reverse proxy를 통해 backend health API를 호출하도록 구성했다.

### 요청 흐름

```text
Browser
  -> http://localhost
      -> Next.js frontend
          -> fetch("/api/health")
              -> Nginx
                  -> FastAPI backend
```

### 핵심 코드

```ts
const response = await fetch("/api/health");
const data = await response.json();
```

### 확인 기준

- `http://localhost` 접속 시 Dashboard 화면 표시
- 화면에서 `Backend API: OK` 표시
- `http://localhost/api/health` 직접 접속 시 backend health 응답 확인

### 정리

- frontend에서 backend를 직접 `localhost:8000`으로 호출하지 않는다.
- `/api/health` 같은 상대 경로를 사용하면 Nginx를 통해 backend로 요청이 전달된다.
- 이 구조는 나중에 배포할 때도 frontend 코드 변경을 줄여준다.
