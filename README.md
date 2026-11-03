# PlayWithKotlin

A sample Play Framework application combining Kotlin and Java controllers with
Scala Twirl templates. The project uses Maven and keeps Play's `app/`, `conf/`,
`public/`, and `test/` directory layout.

## Requirements

- JDK 8, with `JAVA_HOME` pointing to the JDK installation.
- Internet access on the first build to download Maven and dependencies.

The included Maven wrapper downloads Maven 3.9.16 automatically; a separate
Maven installation is not required. The application uses Play 2.5.14,
Scala 2.11.8, and Kotlin 1.1.1.

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

## Run locally

Start the development server:

```sh
./mvnw compile play2:run
```

Open http://localhost:9000. Stop the server with `Ctrl+C`.

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
- `test/`: JUnit tests, including browser integration coverage for `/` and `/Home`.
- `pom.xml`: Dependencies and Maven build configuration.
- `.mvn/`, `mvnw`, and `mvnw.cmd`: Maven wrapper configuration and launchers.

## Continuous integration and dependency updates

The [Maven CI workflow](.github/workflows/maven-publish.yml) runs
`./mvnw --batch-mode --no-transfer-progress clean verify` on JDK 8 for pushes
and pull requests. It also supports manual runs and uploads test reports as
the `test-reports` artifact when reports are available.

[Dependabot](.github/dependabot.yml) checks Maven dependencies and GitHub
Actions weekly and opens update pull requests.

## License

See [LICENSE](LICENSE).
