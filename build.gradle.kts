plugins {
    application
    id("com.gradleup.shadow") version "9.6.1"
    scala
    id("org.playframework.twirl") version "2.1.0-M9"
    kotlin("jvm") version "2.4.21"
    id("com.diffplug.spotless") version "8.10.4"
}

group = "com.example"
version = "1.0-SNAPSHOT"

val playVersion = "3.0.12"
val scalaVersion = "3.10.0"
val twirlVersion = "2.0.9"

repositories { mavenCentral() }

val generators = configurations.create("generators")
configurations.configureEach {
    // Zinc runs on its own Scala 2.13 runtime, independently of the application.
    if (name != "zinc") {
        resolutionStrategy.force(
            "org.scala-lang:scala-library:$scalaVersion",
            "org.scala-lang:scala3-library_3:$scalaVersion",
        )
    }
}

dependencies {
    for (module in listOf("play", "play-java", "play-guice", "play-netty-server", "play-logback")) {
        implementation("org.playframework:${module}_3:$playVersion")
    }
    implementation("org.scala-lang:scala-library:$scalaVersion")
    implementation("org.scala-lang:scala3-library_3:$scalaVersion")
    add(generators.name, "org.playframework:play-routes-compiler_3:$playVersion")
    add("twirl", "org.playframework.twirl:twirl-compiler_3:$twirlVersion")
    testImplementation("org.playframework:play-test_3:$playVersion")
    testImplementation("junit:junit:4.13.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

val routesOutput = layout.buildDirectory.dir("generated/sources/routes")
val generateRoutes = tasks.register<JavaExec>("generateRoutes") {
    classpath = generators
    mainClass.set("build-support/GenerateRoutes.java")
    inputs.file("src/main/resources/routes")
    inputs.file("build-support/GenerateRoutes.java")
    outputs.dir(routesOutput)
    doFirst {
        delete(routesOutput)
        args = listOf(file("src/main/resources/routes").absolutePath, routesOutput.get().asFile.absolutePath)
    }
}
// The plugin version and compiler version are independent; retain Play's Twirl runtime.
twirl { scalaVersion.set("3") }

sourceSets {
    main {
        // Joint compilation resolves references between Java controllers and generated Scala.
        scala.srcDirs("src/main/java", routesOutput)
        scala.include("**/*.scala", "**/*.java")
        twirl {
            templateImports.addAll("controllers._", "play.twirl.api._")
        }
    }
}
// Java is compiled jointly by Scala after Kotlin has resolved the Java sources.
tasks.compileJava { enabled = false }
tasks.compileKotlin { dependsOn(generateRoutes) }
tasks.compileScala {
    dependsOn(tasks.compileKotlin, generateRoutes)
    classpath += files(tasks.compileKotlin.flatMap { it.destinationDirectory })
    scalaCompileOptions.additionalParameters = listOf("-release:17", "-color:never")
}
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}
tasks.shadowJar {
    archiveClassifier.set("all")
    // Keep all configuration fragments and service providers required by Play and Pekko.
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    filesMatching(listOf("reference.conf", "version.conf", "META-INF/services/**", "META-INF/*.kotlin_module")) {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    mergeServiceFiles()
    append("reference.conf")
    append("version.conf")
    exclude("META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA")
}
tasks.assemble { dependsOn(tasks.shadowJar) }
tasks.test {
    dependsOn(tasks.shadowJar)
    inputs.file(tasks.shadowJar.flatMap { it.archiveFile })
    doFirst {
        systemProperty("fatJar.path", tasks.shadowJar.get().archiveFile.get().asFile.absolutePath)
    }
    useJUnit()
    systemProperty("expected.scala.version", scalaVersion)
}
application { mainClass.set("DevServer") }

spotless {
    java {
        target("src/**/*.java", "build-support/**/*.java")
        googleJavaFormat("1.28.0")
    }
    kotlin {
        target("src/**/*.kt", "build-support/**/*.kt")
        ktlint("1.8.0").editorConfigOverride(mapOf("ktlint_code_style" to "ktlint_official"))
    }
}
tasks.wrapper {
    gradleVersion = "9.8.2"
    distributionType = Wrapper.DistributionType.BIN
    distributionSha256Sum = "21bd5da2de49b642904077f38c0efdc7778e92b3ed0889392dca61b97e389607"
}
