# edu-spring-security

## Overview

> The goal of this tutorial is to provide a comprehensive, JUnit 5-based testing framework and workflow for building secure Spring Boot applications.
> In addition to configuring application services, mastering the specialized test base classes and annotations is essential for achieving robust, high-value test coverage across unit, smoke, integration, and regression suites.

## Plugin net.miwashi.test-suites

> The plugin offers test-suite support. Anything it does can be achieved with `Gradle DSL`, but as the purpose of this tutorial is not DSL, but [REST Assured](https://rest-assured.io). and [BDT](https://dannorth.net/blog/introducing-bdd/).
> When applied it alters `classpath` for the build, pushing tests into specialised folders, and making `./src/test/common` the `common testing folder`. It also creates the folders `java` and `resources` when needed.

### Modification from adding plugin with no configuration

```text
.
├── app
│   └── src
│       └── common [testCommon]
│           ├── java
│           └── resources
│       └── test [test]
│           ├── java
│           └── resources
```

## Starting Project Structure

```text
.
├── gradle.properties
├── LICENSE
├── README.md
├── settings.gradle # defines the project’s name and included modules, and is read before build.gradle
├── app # A module included from settings.gradle
│   ├── build.gradle
│   └── src
│       ├── main
│       │   ├── java
│       │   │   └── org
│       │   │       └── example
│       │   │           └── App.java
│       │   └── resources
│       │       ├── application.yaml # production settings
│       │       └── templates
│       │           ├── error
│       │           │   └── 404.html
│       │           ├── error.html
│       │           └── index.html
│       └── test
│           ├── java
│           └── resources
```

## Final Project Structure

```text
.
├── gradle.properties
├── LICENSE
├── README.md
├── settings.gradle # defines the project’s name and included modules, and is read before build.gradle
├── app # A module included from settings.gradle
│   ├── build.gradle
│   └── src
│       ├── main
│       │   ├── java
│       │   │   └── org
│       │   │       └── example
│       │   │           └── App.java
│       │   └── resources
│       │       ├── application-dev.yaml # increase testability by lessening security
│       │       ├── application-prod.yaml # most often not used
│       │       ├── application-stage.yaml # increase testability by lessening security
│       │       ├── application.yaml # production settings
│       │       └── templates
│       │           ├── error
│       │           │   └── 404.html
│       │           ├── error.html
│       │           └── index.html
│       └── test
│           ├── common # No tests, only common code for testing
│           │   ├── java
│           │   │   └── org
│           │   │       └── example
│           │   │           ├── IntegrationTest.java
│           │   │           ├── RegressionTest.java # Interface
│           │   │           ├── SmokeTest.java
│           │   │           └── UnitTest.java
│           │   └── resources # common test resources
│           ├── integration # Tests for purpose integration testing
│           │   ├── java
│           │   └── resources
│           ├── smoke # Tests for purpose smoke testing
│           │   ├── java
│           │   └── resources
│           └── unit # Tests for purpose unit testing
│               ├── java
│               └── resources
```

## Creating the source sets - not using plugin - (over course!)

> This file is included for learning, not for doing. Understand how source sets work and how to create them.
> Also understand classpath and how different source sets are compiled and run.

> [!Note]
> **Adding source sets**
> use `apply from: './test-sets.gradle'` just after `plugins` in `build.gradle`.


```
cd ws
cd [spring boot project]
cat > ./app/test-sets.gradle << 'EOF'
def hasCommon = file('src/test/common').exists()

if (hasCommon) {
    sourceSets.create('testCommon') {
        java.srcDirs = ['src/test/common/java']
        resources.srcDirs = ['src/test/common/resources']
    }
    configurations {
        testCommonImplementation.extendsFrom testImplementation
        testCommonRuntimeOnly.extendsFrom testRuntimeOnly
    }
}

def commonOutput = hasCommon ? sourceSets.testCommon.output : files()

// White-box: unit tests see main + common
sourceSets {
    test {
        java.srcDirs = ['src/test/unit/java']
        resources.srcDirs = ['src/test/unit/resources']
        compileClasspath += commonOutput
        runtimeClasspath += commonOutput
    }
}

tasks.named('test') {
    useJUnitPlatform()
}

def suites = [unit: sourceSets.test]

// Black-box: only common + test dependencies, no main
['smoke', 'integration'].each { name ->
    if (!file("src/test/${name}").exists()) return

    def ss = sourceSets.create("${name}Test") {
        java.srcDirs = ["src/test/${name}/java"]
        resources.srcDirs = ["src/test/${name}/resources"]
        compileClasspath = commonOutput + configurations["${name}TestCompileClasspath"]
        runtimeClasspath = output + commonOutput + configurations["${name}TestRuntimeClasspath"]
    }

    configurations["${name}TestImplementation"].extendsFrom configurations.testImplementation
    configurations["${name}TestRuntimeOnly"].extendsFrom configurations.testRuntimeOnly

    suites[name] = ss

    def task = tasks.register("${name}Test", Test) {
        description = "Runs ${name} tests."
        group = 'verification'
        testClassesDirs = ss.output.classesDirs
        classpath = ss.runtimeClasspath
        useJUnitPlatform()
        shouldRunAfter tasks.named('test')
    }

    tasks.named('check') { dependsOn task }
}

// Regression: @Regression-tagged tests from all suites, one task
tasks.register('regressionTest', Test) {
    description = 'Runs @Regression tests from all suites.'
    group = 'verification'
    testClassesDirs = files(suites.values().collect { it.output.classesDirs })
    classpath = files(suites.values().collect { it.runtimeClasspath })
    useJUnitPlatform { includeTags 'regression' }
}

// Shared settings for all test tasks
tasks.withType(Test).configureEach {
    ['smoke', 'integration'].each { n ->
        systemProperty "${n}.baseUrl", findProperty("${n}.baseUrl") ?: 'http://localhost:8080'
    }

    testLogging {
        events 'started', 'passed', 'skipped', 'failed', 'standardOut', 'standardError'
        exceptionFormat = 'full'
        showExceptions = true
        showCauses = true
        showStackTraces = true
        showStandardStreams = true
    }

    def taskName = name
    afterSuite { desc, result ->
        if (!desc.parent) {
            System.out.println("\n" + taskName + ": " + result.resultType +
                    " (" + result.testCount + " tests, " + result.successfulTestCount + " passed, " +
                    result.failedTestCount + " failed, " + result.skippedTestCount + " skipped)")
        }
    }
}
EOF
```
