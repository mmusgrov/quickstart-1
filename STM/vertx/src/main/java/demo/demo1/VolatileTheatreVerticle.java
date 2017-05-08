package demo.demo1;

import demo.stm.TheatreServiceImpl;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import org.jboss.stm.Container;

import demo.stm.TheatreService;

public class VolatileTheatreVerticle extends TheatreVerticle {
    private static int port = 8080;
    private static TheatreService service;
    private static Container<TheatreService> container;

    public static void main(String[] args) {
        container = new Container<>(Container.TYPE.RECOVERABLE, Container.MODEL.EXCLUSIVE);

        service = container.create(new TheatreServiceImpl());
        initSTMMemory(service);

        DeploymentOptions opts = new DeploymentOptions().
                setInstances(10).
                setConfig(new JsonObject().put("name", "demo1").put("port", port));

        Vertx.vertx().deployVerticle(VolatileTheatreVerticle.class.getName(), opts);

        /*
        Make theatre bookings: curl -X POST http://localhost:8080/api/theatre/Odeon
        Count theatre bookings: curl -X GET http://localhost:8080/api/theatre
         */
    }

    TheatreService getClone() {
        return container.clone(new TheatreServiceImpl(), service);
    }
}
