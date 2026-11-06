# PlayWithKotlin

A sample Play Framework application combining Kotlin and Java controllers with
Scala Twirl templates. The project uses Maven and keeps Play's `app/`, `conf/`,
`public/`, and `test/` directory layout.

## Requirements

- JDK 17 or newer, with `JAVA_HOME` pointing to the JDK installation (CI uses JDK 21).
- Internet access on the first build to download Maven and dependencies.

The included Maven wrapper downloads Maven 3.10.0 automatically; a separate
Maven installation is not required. The application uses Play 3.0.12,
Scala 3.10.0, and Kotlin 2.4.21.

## Build and test

Compile the application, generate routes and templates, run all tests, and
package the application:

```sh
./mvnw clean verify
```

The application JAR is written to `target/play-java-1.0-SNAPSHOT.jar`.
Test reports are available in `target/surefire-reports/`.

To run only the tests and their required compilation steps:

```sh
./mvnw test
```

## Formatting

Spotless checks Java sources in `app/`, `test/`, and `build-support/` using
Google Java Format. The formatter version is pinned for consistent results on
JDK 17 and newer. Generated files under `target/` are excluded.

Apply formatting:

```sh
./mvnw spotless:apply
```

Check formatting without changing files:

```sh
./mvnw spotless:check
```

`./mvnw clean verify` also runs the formatting check, including in CI.

## Scala 3 build

Maven generates Scala sources from `app/views/*.scala.html` using the official
Twirl Maven plugin and from `conf/routes` using Play's routes compiler.
The Kotlin Maven plugin compiles Kotlin first, then `scala-maven-plugin` compiles
the generated Scala sources with Scala 3.10.0, and `javac` compiles Java.
Generated files stay under `target/generated-sources/`.

Play 2.5 and its Scala 2.11 artifacts cannot be used with Scala 3.10.0.
The application therefore uses Play 3's `_3` artifacts and Pekko APIs.
Both `scala-library` and `scala3-library_3` are pinned to 3.10.0 to prevent
Maven from selecting older transitive Scala libraries. [Play officially supports
Scala LTS releases](https://www.playframework.com/documentation/3.0.x/Scala3Migration); this project's non-LTS Scala 3.10.0 combination is covered
by its compilation and HTTP integration tests.

## Run locally

Start the compiled application in development mode:

```sh
./mvnw compile exec:exec@run
```

Open http://localhost:9000. Stop the server with `Ctrl+C`.
After source changes, stop the server and run the command again; this Maven
launcher does not provide hot reloading.

| Route | Example |
| --- | --- |
| `/` | Welcome page served by the Java controller |
| `/Home` | Welcome page served through the Kotlin controller |
| `/count` | Counter using dependency injection |
| `/message` | Asynchronous response after a one-second delay |
| `/assets/*file` | Static resources from `public/` |

On Windows, use `mvnw.cmd` in place of `./mvnw`, for example
`mvnw.cmd clean verify`.

## Project layout

- `app/`: Java and Kotlin source files, services, filters, and Twirl templates.
- `conf/`: Application configuration, routes, and logging configuration.
- `public/`: Stylesheets, JavaScript, and images.
- `test/`: JUnit tests, including HTTP integration coverage for all application routes.
- `pom.xml`: Dependencies and Maven build configuration.
- `build-support/GenerateRoutes.java`: Adapter invoking Play’s routes compiler during Maven source generation.
- `.mvn/`, `mvnw`, and `mvnw.cmd`: Maven wrapper configuration and launchers.

## Continuous integration and dependency updates

The [Maven CI workflow](.github/workflows/maven-publish.yml) runs
`./mvnw --batch-mode --no-transfer-progress clean verify` on JDK 21 for pushes
and pull requests. It also supports manual runs and uploads test reports as
the `test-reports` artifact when reports are available.

[Dependabot](.github/dependabot.yml) checks Maven dependencies and GitHub
Actions weekly and opens update pull requests.

## License

See [LICENSE](LICENSE).
