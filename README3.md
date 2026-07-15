## Local HTTPS Test

Nginx에 자체 서명 인증서를 적용하여 로컬 HTTPS 접속을 테스트했다.

### 인증서 생성

```bash
mkdir -p nginx/certs

openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/certs/localhost.key \
  -out nginx/certs/localhost.crt \
  -subj "/CN=localhost"

근데 중간에 오류 발생. 내부 통신까지 https로 바꾼것.
그래서 내부 통신은 http로 남겨둚. 