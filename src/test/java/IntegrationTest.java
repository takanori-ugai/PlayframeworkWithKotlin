import static org.junit.Assert.*;
import static play.test.Helpers.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import play.Mode;
import play.inject.guice.GuiceApplicationBuilder;
import play.test.TestServer;

public class IntegrationTest {
  /** Verifies routes, generated templates, and packaged assets through HTTP. */
  @Test
  public void routesWorkWithScala3() throws IOException {
    String expectedStylesheet =
        Files.readString(
            Path.of("src/main/resources/public/stylesheets/main.css"), StandardCharsets.UTF_8);
    TestServer server = testServer(0, new GuiceApplicationBuilder().build());
    running(
        server,
        () -> {
          HttpClient client = HttpClient.newHttpClient();
          String baseUrl = "http://localhost:" + server.getRunningHttpPort().getAsInt();
          for (String path : new String[] {"/", "/Home"}) {
            HttpResponse<String> response = get(client, baseUrl + path);
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("Your new application is ready."));
          }
          assertEquals("0", get(client, baseUrl + "/count").body());
          assertEquals("1", get(client, baseUrl + "/count").body());
          HttpResponse<String> message = get(client, baseUrl + "/message");
          assertEquals(200, message.statusCode());
          assertEquals("Hi!", message.body());
          HttpResponse<String> stylesheet = get(client, baseUrl + "/assets/stylesheets/main.css");
          assertEquals(200, stylesheet.statusCode());
          assertEquals(
              "text/css",
              stylesheet.headers().firstValue("Content-Type").orElse("").split(";", 2)[0].trim());
          assertEquals(expectedStylesheet, stylesheet.body());
          assertEquals(404, get(client, baseUrl + "/missing").statusCode());
        });
  }

  /** Verifies that DEV mode registers and executes the example HTTP filter. */
  @Test
  public void developmentModeAddsExampleFilterHeader() {
    TestServer server = testServer(0, new GuiceApplicationBuilder().in(Mode.DEV).build());
    running(
        server,
        () -> {
          String url = "http://localhost:" + server.getRunningHttpPort().getAsInt() + "/Home";
          HttpResponse<String> response = get(HttpClient.newHttpClient(), url);
          assertEquals(200, response.statusCode());
          assertEquals("foo", response.headers().firstValue("X-ExampleFilter").orElse(""));
        });
  }

  /** Sends a GET request and reports transport failures as test assertion failures. */
  private static HttpResponse<String> get(HttpClient client, String url) {
    try {
      return client.send(
          HttpRequest.newBuilder(URI.create(url)).GET().build(),
          HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      throw new AssertionError("Request failed: " + url, e);
    }
  }
}
