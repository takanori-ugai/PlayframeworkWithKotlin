import java.io.File;
import java.util.List;
import play.routes.compiler.RoutesCompiler;
import play.routes.compiler.InjectedRoutesGenerator$;
import scala.jdk.javaapi.CollectionConverters;

/** Invokes Play's routes compiler during Maven generate-sources. */
class GenerateRoutes {
    public static void main(String[] args) {
        var imports = CollectionConverters.asScala(List.of("controllers.Assets.Asset")).toSeq();
        var task = new RoutesCompiler.RoutesCompilerTask(
                new File(args[0]), imports, true, true, false, false);
        var result = RoutesCompiler.compile(task, InjectedRoutesGenerator$.MODULE$, new File(args[1]));
        if (result.isLeft()) {
            throw new IllegalArgumentException("Invalid routes: " + result.swap().toOption().get());
        }
    }
}
