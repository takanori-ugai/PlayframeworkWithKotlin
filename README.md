# PlayWithKotlin

A sample Play Framework application combining Kotlin and Java controllers with
Scala Twirl templates. The project uses Gradle with the standard `src/main/`
and `src/test/` directory layout.

## Requirements

- JDK 17 or newer, with `JAVA_HOME` pointing to the JDK installation (CI uses JDK 21).
- Internet access on the first build to download Gradle and dependencies.

The included Gradle wrapper downloads Gradle 9.8.2 automatically; a separate
Gradle installation is not required. The application uses Play 3.0.12,
Scala 3.10.0, and Kotlin 2.4.21.

## Build and test

Compile the application, generate routes and templates, run all tests, and
package the application:

```sh
./gradlew clean build
```

The application JAR is written to `build/libs/play-java-1.0-SNAPSHOT.jar`.
Test reports are available in `build/reports/tests/test/`.

To run only the tests and their required compilation steps:

```sh
./gradlew test
```

## Fat JAR

Build a runnable JAR containing the application and all runtime dependencies:

```sh
./gradlew shadowJar
java -jar build/libs/play-java-1.0-SNAPSHOT-all.jar
```

`./gradlew build` also creates this Fat JAR. It includes templates, configuration,
static assets, merged service descriptors, and Play/Pekko configuration fragments.
The launcher defaults to DEV mode, like `./gradlew run`, and listens on
`127.0.0.1:9000` (loopback only).
Override the port with a JVM property:

```sh
java -Dhttp.port=9001 -jar build/libs/play-java-1.0-SNAPSHOT-all.jar
```

To accept connections from other PCs, explicitly override the bind address:

```sh
java -Dhttp.address=0.0.0.0 -jar build/libs/play-java-1.0-SNAPSHOT-all.jar
```

Other PCs can then connect using `http://<server-LAN-IP>:9000`; allow TCP port
9000 through the server firewall. DEV diagnostic pages are visible to those clients.

The JAR can be launched from any working directory with JDK 17 or newer.
The test suite verifies it in a separate JVM outside the project directory.

## Selecting DEV or PROD at startup

The same Fat JAR supports `-Dplay.mode=DEV` (default) and `-Dplay.mode=PROD`.
Values are case insensitive; invalid values fail startup. Restart the process to
switch modes; recompilation is not required after building this version.

For production, create an external configuration file such as
`/etc/play-java/application-prod.conf`:

```hocon
include "application.conf"
play.http.secret.key = ${APPLICATION_SECRET}
```

Provide a strong, random `APPLICATION_SECRET` through your deployment environment
or secret manager, then start the existing JAR:

```sh
java -Dplay.mode=PROD \
  -Dconfig.file=/etc/play-java/application-prod.conf \
  -Dhttp.address=127.0.0.1 -Dhttp.port=9000 \
  -jar build/libs/play-java-1.0-SNAPSHOT-all.jar
```

PROD requires a valid secret; the bundled `changeme` value is for development.
`-Dconfig.file` also lets you change application settings without rebuilding the
JAR. Mode selection uses the JVM property `play.mode`, independently of that file.
The test suite starts the same JAR in DEV and PROD and checks HTTP responses,
assets, and mode-dependent filter behavior.

Production mode selection alone does not complete production hardening: the
current custom `Filters` class still returns no filters in PROD. Configure the
required security filters, HTTPS/proxy, and process supervision before deployment.

## Formatting

Spotless checks Java sources using Google Java Format and Kotlin sources using
ktlint (`ktlint_official` style) in `src/` and `build-support/`.
Both formatter versions are pinned for consistent results on JDK 17 and newer.
Generated files under `build/` are excluded.

Apply formatting:

```sh
./gradlew spotlessApply
```

Check formatting without changing files:

```sh
./gradlew spotlessCheck
```

`./gradlew clean build` also runs the formatting check, including in CI.

## Scala 3 build

Gradle generates Scala sources from `src/main/twirl/views/*.scala.html` using the official
`org.playframework.twirl` Gradle plugin (`2.1.0-M9`, with the Twirl compiler pinned
to `2.0.9` to match the existing Play runtime) and from `src/main/resources/routes` using Play's routes compiler.
The Kotlin plugin compiles Kotlin first, then the Scala plugin jointly compiles
Java from `src/main/java/` and generated Scala sources. Java sources are also
included in the Scala source set to resolve their mutual references with templates
and routes. Generated files stay under
`build/generated/sources/`.

Play 2.5 and its Scala 2.11 artifacts cannot be used with Scala 3.10.0.
The application therefore uses Play 3's `_3` artifacts and Pekko APIs.
Both `scala-library` and `scala3-library_3` are pinned to 3.10.0 to prevent
Gradle from selecting different transitive Scala libraries. [Play officially supports
Scala LTS releases](https://www.playframework.com/documentation/3.0.x/Scala3Migration); this project's non-LTS Scala 3.10.0 combination is covered
by its compilation and HTTP integration tests.

## Run locally

Start the compiled application in development mode:

```sh
./gradlew run
```

Open http://localhost:9000 locally. Remote access requires the explicit
`http.address` override described above.
Stop the server with `Ctrl+C`.
After source changes, stop the server and run the command again; this Gradle
launcher does not provide hot reloading.

| Route | Example |
| --- | --- |
| `/` | Welcome page served by the Java controller |
| `/Home` | Welcome page served through the Kotlin controller |
| `/count` | Counter using dependency injection |
| `/message` | Asynchronous response after a one-second delay |
| `/assets/*file` | Static resources from `src/main/resources/public/` |

On Windows, use `gradlew.bat` in place of `./gradlew`, for example
`gradlew.bat clean build`.

## Project layout

- `src/main/java/`: Java controllers, services, filters, and the development launcher.
- `src/main/kotlin/`: Kotlin controllers.
- `src/main/twirl/views/`: Twirl templates, using the Twirl plugin's standard source directory.
- `src/main/resources/`: Application configuration, routes, and logging configuration.
- `src/main/resources/public/`: Stylesheets, JavaScript, and images.
- `src/test/java/`: JUnit tests, including HTTP integration coverage for all application routes.
- `build.gradle.kts`, `settings.gradle.kts`: Gradle build configuration.
- `build-support/GenerateRoutes.java`: Adapter invoking Play’s routes compiler during Gradle source generation.
- `gradle/wrapper/`, `gradlew`, and `gradlew.bat`: Gradle wrapper configuration and launchers.

## Continuous integration and dependency updates

The [Gradle CI workflow](.github/workflows/gradle-build.yml) runs
`./gradlew --no-daemon clean build` on JDK 21 for pushes
and pull requests. It also supports manual runs and uploads test reports as
the `test-reports` artifact when reports are available.

[Dependabot](.github/dependabot.yml) checks Gradle dependencies and GitHub
Actions weekly and opens update pull requests.

## License

See [LICENSE](LICENSE).
