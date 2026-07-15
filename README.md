# Local Web Service Infrastructure Practice

Docker Compose, Nginx, Spring Boot, Next.js, PostgreSQL, Redis를 사용해 로컬 웹서비스 배포 구조를 구성한 프로젝트입니다.

이 프로젝트는 여러 서비스를 컨테이너로 실행하고, Nginx를 단일 진입점으로 사용해 frontend, backend, database, cache를 함께 동작시키는 구조를 학습하고 검증하기 위한 프로젝트입니다.

## Overview

```text
Browser
  -> Nginx
      -> /      -> Next.js Frontend
      -> /api   -> Spring Boot Backend
                    -> PostgreSQL
                    -> Redis
```

## Tech Stack

| Area          | Stack                   |
| ------------- | ----------------------- |
| Frontend      | Next.js                 |
| Backend       | Spring Boot             |
| Database      | PostgreSQL              |
| Cache         | Redis                   |
| Reverse Proxy | Nginx                   |
| Container     | Docker, Docker Compose  |
| HTTPS Test    | Self-signed certificate |

## Features

- Docker Compose 기반 다중 컨테이너 실행
- PostgreSQL, Redis, Adminer 컨테이너 구성
- Docker volume을 이용한 PostgreSQL 데이터 유지
- Docker Compose network에서 서비스 이름 기반 통신
- Nginx reverse proxy 구성
- `/` 요청을 Next.js frontend로 전달
- `/api` 요청을 Spring Boot backend로 전달
- Spring Boot에서 PostgreSQL 연결
- Spring Boot에서 Redis 연결
- PostgreSQL `products` 테이블 조회 API
- Redis를 이용한 products API 캐시 테스트
- 자체 서명 인증서를 이용한 로컬 HTTPS 테스트
- 서버 배포 시 포트 노출 및 보안 주의사항 정리

## Project Structure

```text
.
├── frontend/
│   ├── Dockerfile
│   ├── package.json
│   └── app/
├── spring-backend/
│   ├── Dockerfile
│   ├── build.gradle
│   └── src/
├── nginx/
│   ├── default.conf
│   └── certs/
├── compose.yml
├── .gitignore
└── README.md
```

## Services

| Service  | Description              | Internal Port | External Access                             |
| -------- | ------------------------ | ------------: | ------------------------------------------- |
| nginx    | Reverse proxy            |       80, 443 | `http://localhost`, `https://localhost` |
| frontend | Next.js frontend         |          3000 | via Nginx                                   |
| backend  | Spring Boot backend      |          8080 | via Nginx`/api`                           |
| postgres | PostgreSQL database      |          5432 | Docker network                              |
| redis    | Redis cache              |          6379 | Docker network                              |
| adminer  | Database management tool |          8080 | development only                            |

## Running the Project

Start all services:

```bash
docker compose up -d --build
```

Check running containers:

```bash
docker compose ps
```

Check logs:

```bash
docker compose logs nginx
docker compose logs frontend
docker compose logs backend
docker compose logs postgres
docker compose logs redis
```

Stop services:

```bash
docker compose down
```

Stop services and remove volumes:

```bash
docker compose down -v
```

`docker compose down -v` removes Docker volumes, including PostgreSQL data.

## Local URLs

| URL                                    | Description                 |
| -------------------------------------- | --------------------------- |
| `http://localhost`                   | Redirects to HTTPS          |
| `https://localhost`                  | Frontend through Nginx      |
| `https://localhost/api/health`       | Backend health check        |
| `https://localhost/api/db/health`    | PostgreSQL connection check |
| `https://localhost/api/redis/health` | Redis connection check      |
| `https://localhost/api/products`     | Products API                |

Because this project uses a self-signed certificate, the browser shows a security warning on `https://localhost`. This is expected in local testing.

For `curl`, use the `-k` option:

```bash
curl -k https://localhost/api/health
```

## API Examples

### Backend Health Check

```bash
curl -k https://localhost/api/health
```

Example response:

```json
{
  "status": "ok",
  "service": "spring-backend"
}
```

### PostgreSQL Health Check

```bash
curl -k https://localhost/api/db/health
```

Example response:

```json
{
  "database": "ok",
  "result": 1
}
```

### Redis Health Check

```bash
curl -k https://localhost/api/redis/health
```

Example response:

```json
{
  "redis": "ok",
  "pong": "PONG"
}
```

### Products API

```bash
curl -k https://localhost/api/products
```

Example response:

```json
{
  "source": "postgres",
  "items": [
    {
      "id": 1,
      "name": "Keyboard",
      "price": 50000
    },
    {
      "id": 2,
      "name": "Mouse",
      "price": 25000
    }
  ]
}
```

If the Redis cache is already populated, the response source changes:

```json
{
  "source": "redis",
  "items": [
    {
      "id": 1,
      "name": "Keyboard",
      "price": 50000
    },
    {
      "id": 2,
      "name": "Mouse",
      "price": 25000
    }
  ]
}
```

## PostgreSQL Sample Data

Connect to PostgreSQL:

```bash
docker compose exec postgres psql -U app -d app
```

Create a sample table:

```sql
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price INTEGER NOT NULL
);
```

Insert sample data:

```sql
INSERT INTO products (name, price) VALUES
('Keyboard', 50000),
('Mouse', 25000),
('Monitor', 230000);
```

Check data:

```sql
SELECT * FROM products;
```

Exit:

```sql
\q
```

## Redis Cache Test

The products API uses Redis as a simple cache layer.

Flow:

```text
First request:
Spring Boot -> Redis cache miss -> PostgreSQL query -> Redis save -> response

Next request:
Spring Boot -> Redis cache hit -> response
```

Clear products cache:

```bash
docker compose exec redis redis-cli DEL products:all
```

Check cached value:

```bash
docker compose exec redis redis-cli GET products:all
```

## HTTPS Test

This project uses a self-signed certificate for local HTTPS testing.

Generate certificate files:

```bash
mkdir -p nginx/certs

openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/certs/localhost.key \
  -out nginx/certs/localhost.crt \
  -subj "/CN=localhost"
```

Check HTTP to HTTPS redirect:

```bash
curl -I http://localhost
```

Expected result:

```text
HTTP/1.1 301 Moved Permanently
Location: https://localhost/
```

Check HTTPS:

```bash
curl -k https://localhost
curl -k https://localhost/api/health
```

## Nginx Routing

Nginx is the only external entry point.

```text
Browser
  -> Nginx
      -> Frontend
      -> Backend
```

Basic routing:

```nginx
location /api/ {
    proxy_pass http://backend:8080/;
}

location / {
    proxy_pass http://frontend:3000;
}
```

With this setting:

```text
External request:
GET /api/products

Backend receives:
GET /products
```

The trailing slash in `proxy_pass http://backend:8080/;` removes the `/api/` prefix before forwarding the request to the backend.

## Docker Network Notes

Docker Compose service names are used as internal hostnames.

Examples:

```text
nginx -> frontend:3000
nginx -> backend:8080
backend -> postgres:5432
backend -> redis:6379
frontend server component -> backend:8080
```

Inside Docker containers, `localhost` means the current container itself. Use Compose service names for container-to-container communication.

## Security Notes

For server deployment, only the reverse proxy should be exposed publicly.

Recommended public ports:

```text
80   HTTP
443  HTTPS
22   SSH, restricted when possible
```

Services that should not be exposed directly:

```text
3000  Next.js frontend
8080  Spring Boot backend
5432  PostgreSQL
6379  Redis
8080  Adminer
```

For internal services, prefer `expose` instead of `ports`.

Example:

```yaml
backend:
  expose:
    - "8080"

postgres:
  expose:
    - "5432"

redis:
  expose:
    - "6379"
```

Do not commit secrets or generated certificates.

Recommended `.gitignore` entries:

```gitignore
.env
.env.local
.env.production
nginx/certs/*.key
nginx/certs/*.crt
```

Use `.env.example` to document required environment variables without exposing real secrets.

## Troubleshooting

### Nginx 502 Bad Gateway

Check whether the target service is running:

```bash
docker compose ps
docker compose logs nginx
docker compose logs backend
docker compose logs frontend
```

Common causes:

- backend container is not running
- frontend container is not running
- wrong service name in `proxy_pass`
- wrong internal port

### Browser Shows a Certificate Warning

This is expected with a self-signed certificate.

The certificate is only for local HTTPS testing. A public deployment should use a trusted certificate such as Let's Encrypt.

### Next.js Server Component Shows `self-signed certificate`

This can happen when server-side code calls Nginx over HTTPS with a self-signed certificate.

Use internal HTTP communication between containers:

```tsx
fetch("http://backend:8080/products", {
  cache: "no-store",
});
```

External browser traffic still uses HTTPS through Nginx.

### `products.map is not a function`

The products API returns an object with an `items` array.

Use:

```tsx
const data = await getProducts();
const products = Array.isArray(data.items) ? data.items : [];
```

### PostgreSQL Connection Failed

Check datasource settings:

```properties
spring.datasource.url=jdbc:postgresql://postgres:5432/app
spring.datasource.username=app
spring.datasource.password=app
```

Check logs:

```bash
docker compose logs postgres
docker compose logs backend
```

### Redis Connection Failed

Check Redis settings:

```properties
spring.data.redis.host=redis
spring.data.redis.port=6379
```

Check Redis:

```bash
docker compose exec redis redis-cli ping
docker compose logs redis
docker compose logs backend
```

## Purpose

This repository demonstrates a local web service infrastructure pattern using Docker Compose and Nginx. It can be used as a reference for running frontend, backend, database, cache, and reverse proxy services together in a local server environment.
