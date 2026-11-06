import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.Test;
import play.inject.guice.GuiceApplicationBuilder;
import play.test.TestServer;

import static org.junit.Assert.*;
import static play.test.Helpers.*;

public class IntegrationTest {
    @Test
    public void routesWorkWithScala3() {
        TestServer server = testServer(0, new GuiceApplicationBuilder().build());
        running(server, () -> {
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
            assertEquals(200, get(client, baseUrl + "/assets/stylesheets/main.css").statusCode());
            assertEquals(404, get(client, baseUrl + "/missing").statusCode());
        });
    }

    private static HttpResponse<String> get(HttpClient client, String url) {
        try {
            return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new AssertionError("Request failed: " + url, e);
        }
    }
}
