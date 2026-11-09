import java.io.File;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import play.Mode;
import play.core.server.NettyServer;
import play.core.server.ServerConfig;
import play.inject.guice.GuiceApplicationBuilder;
import scala.Option;

/** Runs the compiled application in the mode selected at startup, without hot reloading. */
public final class DevServer {
  public static void main(String[] args) throws InterruptedException {
    String requestedMode = System.getProperty("play.mode", "DEV").trim().toUpperCase(Locale.ROOT);
    Mode mode =
        switch (requestedMode) {
          case "DEV" -> Mode.DEV;
          case "PROD" -> Mode.PROD;
          default -> throw new IllegalArgumentException("play.mode must be DEV or PROD");
        };
    var application = new GuiceApplicationBuilder().in(mode).build();
    var config =
        ServerConfig.apply(
            DevServer.class.getClassLoader(),
            new File("."),
            Option.apply(Integer.getInteger("http.port", 9000)),
            Option.empty(),
            System.getProperty("http.address", "0.0.0.0"),
            application.asScala().mode(),
            System.getProperties());
    var server = NettyServer.fromApplication(application.asScala(), config);
    Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    new CountDownLatch(1).await();
  }
}
