import static org.junit.Assert.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Verifies the packaged application without relying on the project's runtime classpath or files.
 */
public class FatJarTest {
  @Rule public TemporaryFolder temporaryFolder = TemporaryFolder.builder().assureDeletion().build();

  @Test(timeout = 90000)
  public void runsAsStandaloneJar() throws Exception {
    verifyStandaloneJar(false);
  }

  @Test(timeout = 90000)
  public void runsSameJarInProductionWithExternalConfiguration() throws Exception {
    verifyStandaloneJar(true);
  }

  /** Starts an isolated child JVM, verifies its HTTP endpoints, and stops it before cleanup. */
  private void verifyStandaloneJar(boolean production) throws Exception {
    Path workingDirectory = temporaryFolder.newFolder("play-fat-jar").toPath();
    Path log = workingDirectory.resolve("server.log");
    var command =
        new ArrayList<>(
            List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Dhttp.port=0"));
    if (production) {
      Path config = workingDirectory.resolve("application-prod.conf");
      Files.writeString(
          config, "include \"application.conf\"\nplay.http.secret.key = ${APPLICATION_SECRET}\n");
      command.add("-Dplay.mode=prod");
      command.add("-Dconfig.file=" + config);
    }
    command.addAll(List.of("-jar", System.getProperty("fatJar.path")));
    var processBuilder =
        new ProcessBuilder(command)
            .directory(workingDirectory.toFile())
            .redirectErrorStream(true)
            .redirectOutput(log.toFile());
    processBuilder.environment().remove("JAVA_TOOL_OPTIONS");
    processBuilder.environment().remove("JDK_JAVA_OPTIONS");
    if (production) {
      processBuilder
          .environment()
          .put("APPLICATION_SECRET", UUID.randomUUID().toString() + UUID.randomUUID());
    }
    Process server = processBuilder.start();
    try {
      var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
      String baseUrl = null;
      var assignedPort = Pattern.compile("(?m)^PLAY_HTTP_PORT=(\\d+)\\r?$");
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
      boolean ready = false;
      while (server.isAlive() && System.nanoTime() < deadline) {
        try {
          var port = assignedPort.matcher(Files.readString(log));
          if (port.find()) {
            baseUrl = "http://127.0.0.1:" + port.group(1);
            ready = get(client, baseUrl + "/").statusCode() == 200;
          }
          if (ready) break;
        } catch (java.io.IOException ignored) {
          // Wait for the child JVM to bind its HTTP port.
        }
        Thread.sleep(100);
      }
      assertTrue("Fat JAR failed to start:\n" + Files.readString(log), ready);
      for (String route : new String[] {"/", "/Home"}) {
        var response = get(client, baseUrl + route);
        assertEquals(200, response.statusCode());
        assertEquals(!production, response.headers().firstValue("X-ExampleFilter").isPresent());
        assertTrue(response.body().contains("Your new application is ready."));
        var stylesheets =
            Pattern.compile("<link\\b[^>]*rel=\"stylesheet\"[^>]*href=\"([^\"]+)\"")
                .matcher(response.body());
        int stylesheetCount = 0;
        while (stylesheets.find()) {
          String stylesheetUrl = baseUrl + stylesheets.group(1);
          var css = get(client, stylesheetUrl);
          assertEquals(stylesheetUrl, 200, css.statusCode());
          try (var expected = getClass().getResourceAsStream("/public/stylesheets/main.css")) {
            assertNotNull(expected);
            assertArrayEquals(
                expected.readAllBytes(),
                css.body().getBytes(java.nio.charset.StandardCharsets.UTF_8));
          }
          stylesheetCount++;
        }
        assertTrue("Page should link to a stylesheet", stylesheetCount > 0);
      }
      var stylesheet = get(client, baseUrl + "/assets/stylesheets/main.css");
      assertEquals(200, stylesheet.statusCode());
      try (var expected = getClass().getResourceAsStream("/public/stylesheets/main.css")) {
        assertNotNull(expected);
        assertArrayEquals(
            expected.readAllBytes(),
            stylesheet.body().getBytes(java.nio.charset.StandardCharsets.UTF_8));
      }
      assertEquals("0", get(client, baseUrl + "/count").body());
      assertEquals("1", get(client, baseUrl + "/count").body());
      assertEquals("Hi!", get(client, baseUrl + "/message").body());
    } finally {
      server.destroy();
      if (!server.waitFor(10, TimeUnit.SECONDS)) {
        server.destroyForcibly();
        server.waitFor(5, TimeUnit.SECONDS);
      }
    }
  }

  private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }
}
