# Test certificate (DEV ONLY)

Test fixture for certificate login (REQ-035, REQ-036). **Never use in production.**

| File | Who has it |
|---|---|
| `user.crt.pem` | Registered on the server (dev/stage) for `user@example.org` |
| `user.key.pem` | "Mailed" to the user; the tests sign login assertions with it |

Regenerate:

```shell
openssl req -x509 -newkey rsa:2048 -nodes -keyout user.key.pem -out user.crt.pem -days 3650 \
  -subj "/CN=user@example.org/O=edu-spring-security (DEV ONLY - TEST FIXTURE)"
```

Use another key with `TEST_CERT_KEY_FILE=/path/to/key.pem` (PKCS#8 PEM) and `TEST_CERT_EMAIL`.
