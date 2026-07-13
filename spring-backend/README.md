
## Spring Boot Docker 실행

Spring Boot backend를 Docker 이미지로 빌드하고 컨테이너로 실행했다.

### Docker 이미지 빌드

```bash
cd spring-backend
docker build -t spring-backend:dev .
```

### Docker 컨테이너 실행

```bash
docker run --name spring-backend-test -p 8080:8080 spring-backend:dev
```

8080 포트가 이미 사용 중이면:

```bash
docker run --name spring-backend-test -p 8081:8080 spring-backend:dev
```

### 확인

```bash
curl http://localhost:8080/health
```

또는:

```bash
curl http://localhost:8081/health
```

### 예상 응답

```json
{
  "status": "ok",
  "service": "spring-backend"
}
```

### 정리

- Spring Boot는 Gradle build를 통해 실행 가능한 jar로 패키징된다.
- Dockerfile은 multi-stage build를 사용한다.
- 첫 번째 stage에서 jar를 만들고, 두 번째 stage에는 실행에 필요한 JRE와 jar만 포함한다.
- 컨테이너 내부 포트는 8080이고, 외부 포트는 `docker run -p`로 연결한다.


## Spring Boot Docker 실행

Spring Boot backend를 Docker 이미지로 빌드하고 컨테이너로 실행했다.

### Docker 이미지 빌드

```bash
cd spring-backend
docker build -t spring-backend:dev .
```

### Docker 컨테이너 실행

```bash
docker run --name spring-backend-test -p 8080:8080 spring-backend:dev
```

8080 포트가 이미 사용 중이면:

```bash
docker run --name spring-backend-test -p 8081:8080 spring-backend:dev
```

### 확인

```bash
curl http://localhost:8080/health
```

또는:

```bash
curl http://localhost:8081/health
```

### 예상 응답

```json
{
  "status": "ok",
  "service": "spring-backend"
}
```

### 정리

- Spring Boot는 Gradle build를 통해 실행 가능한 jar로 패키징된다.
- Dockerfile은 multi-stage build를 사용한다.
- 첫 번째 stage에서 jar를 만들고, 두 번째 stage에는 실행에 필요한 JRE와 jar만 포함한다.
- 컨테이너 내부 포트는 8080이고, 외부 포트는 `docker run -p`로 연결한다.
