# edu-spring-security

## Instructions

### Start server
```bash
cd ~
cd ws
git clone https://github.com/miwashi-edu/edu-spring-security.git
cd edu-spring-security
gradle bootDev #An added task to run server in dev environment.
```

### Run Tests

> Requires another shell  
> Note! We don't want the server to reboot every time we run tests,
> so the thask integrationTest is independent of the main source set.

```
cd ~
cd ws
cd edu-spring-security
gradle integrationTest
```

## Project Structure

```text
├── app
│   └── src
│       ├── main
│       │   ├── java
│       │   └── resources
│       │       └── templates
│       │           └── error
│       └── test # We have 3 test sets, replacing the normal "test"
│           ├── common
│           │   ├── java
│           │   └── resources
│           ├── integration
│           │   ├── java
│           │   └── resources
│           └── unit
│               ├── java
│               └── resources
```