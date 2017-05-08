package demo.demo1;

import com.arjuna.ats.arjuna.AtomicAction;
import com.arjuna.ats.arjuna.common.Uid;
import demo.stm.TheatreServiceImpl;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import org.jboss.stm.Container;

import demo.stm.TheatreService;

public class NonVolatileTheatreVerticle extends TheatreVerticle {
    private static int port = 8080;
    private static String uid;
    private static Container<TheatreService> container;

    public static void main(String[] args) {
        container = new Container<>(Container.TYPE.PERSISTENT, Container.MODEL.SHARED);

        if (args.length != 0) {
            port = 8082;
            uid = args[0];
        } else {
            TheatreService service = container.create(new TheatreServiceImpl());
            uid = container.getIdentifier(service).toString();
            initSTMMemory(service);

            System.out.printf("Theatre STM uid: %s%n", uid);
        }

        DeploymentOptions opts = new DeploymentOptions().
                setInstances(10).
                setConfig(new JsonObject().put("name", "demo1").put("port", port));

        Vertx.vertx().deployVerticle(NonVolatileTheatreVerticle.class.getName(), opts);

        /*
        Make theatre bookings: curl -X POST http://localhost:8080/api/theatre/Odeon
        Count theatre bookings: curl -X GET http://localhost:8080/api/theatre
         */
    }

    TheatreService getClone() {
        return container.clone(new TheatreServiceImpl(), new Uid(uid));
    }
}
