import java.io.File;
import java.util.concurrent.CountDownLatch;
import play.Mode;
import play.core.server.NettyServer;
import play.core.server.ServerConfig;
import play.inject.guice.GuiceApplicationBuilder;
import scala.Option;

/** Runs the compiled application locally through Maven, without hot reloading. */
public final class DevServer {
  public static void main(String[] args) throws InterruptedException {
    var application = new GuiceApplicationBuilder().in(Mode.DEV).build();
    var config =
        ServerConfig.apply(
            DevServer.class.getClassLoader(),
            new File("."),
            Option.apply(Integer.getInteger("http.port", 9000)),
            Option.empty(),
            "127.0.0.1",
            application.asScala().mode(),
            System.getProperties());
    var server = NettyServer.fromApplication(application.asScala(), config);
    Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    new CountDownLatch(1).await();
  }
}
