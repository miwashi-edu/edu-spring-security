# edu-spring-security

## Overview

## Project Structure

```text
.
├── build.gradle
└── src
    ├── main
    │   ├── java
    │   │   └── org
    │   │       └── example
    │   │           └── App.java
    │   └── resources
    │       ├── application-dev.yaml
    │       ├── application-prod.yaml
    │       ├── application-stage.yaml
    │       ├── application.yaml
    │       └── templates
    │           ├── error
    │           │   └── 404.html
    │           ├── error.html
    │           └── index.html
    └── test
        ├── common
        │   ├── java
        │   │   └── org
        │   │       └── example
        │   │           ├── IntegrationTest.java
        │   │           ├── RegressionTest.java
        │   │           ├── SmokeTest.java
        │   │           └── UnitTest.java
        │   └── resources
        ├── integration
        │   ├── java
        │   │   └── org
        │   │       └── example
        │   └── resources
        ├── smoke
        │   ├── java
        │   │   └── org
        │   │       └── example
        │   └── resources
        └── unit
            ├── java
            │   └── org
            │       └── example
            └── resources
```

> [!Note]
> **Unit tests**
> `Unit tests`: Whitebox tests that check one small piece of code on its own, like testing a single gear outside the machine. Whitebox means the test knows how the code is built inside. They are fast, need no running application, and tell you exactly which piece is broken.

> [!Note]
> **Smoke tests**
> `Smoke tests`: Blackbox tests that quickly check that the running application starts and its most basic functions respond, like turning on a new appliance to see that it powers up. Blackbox means the test only sees the application from the outside. If these fail, there's no point running the rest.

> [!Note]
> **Integration tests**
> `Integration tests`: Blackbox tests that check the application across an integration point, the place where it meets something else, like a database, another service or an external API. The test itself doesn't decide what is behind that point; the environment does. The same test can run against mocked internals or against the real, integrated ones.
> [!Note]
> **Regression tests**
> `Regression tests`: A selected set of tests that guard against old bugs coming back and already-working features breaking after a change. They are picked from the other test types, so they can be both whitebox and blackbox, and are marked with `@Regression`.

[^1]: bootRunDev and bootRunStage tasks are added to help choosing runtime environment.