package demo.verticle;

import com.arjuna.ats.arjuna.AtomicAction;
import com.arjuna.ats.arjuna.common.Uid;
import demo.domain.Booking;
import demo.domain.ServiceResult;
import demo.util.ProgArgs;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.jboss.stm.Container;

public abstract class BaseVerticle<T extends Booking> extends AbstractVerticle {
    static Container<Booking> container;
    private static Booking theService;

    private static int numberOfServiceInstances = 1;
    private static int httpPort = 8080;

    private static Uid uid = null;

    private static ProgArgs options;

    private Booking mandatory;
    private String serviceName;

    String getServiceName() {
        return serviceName;
    }

    public static int getNumberOfServiceInstances() {
        return numberOfServiceInstances;
    }

    static void deployVerticle(String[] args, boolean isVolatile, String verticleClassName, Booking booking, String verticleName) {
        container = isVolatile ?
                new Container<>(Container.TYPE.RECOVERABLE, Container.MODEL.EXCLUSIVE) :
                new Container<>(Container.TYPE.PERSISTENT, Container.MODEL.SHARED);

        parseArgs(args);

        if (uid == null) {
            theService = container.create(booking);
            initializeSTMObject(theService);
            uid = container.getIdentifier(theService);
            System.out.printf("CREATED uid=%s%n", uid == null ? "null" : uid.toString());
        }

        Vertx vertx = Vertx.vertx();

        DeploymentOptions opts = new DeploymentOptions().
                setInstances(numberOfServiceInstances).
                setConfig(new JsonObject().put("name", verticleName).put("port", httpPort));

        vertx.deployVerticle(verticleClassName, opts);
    }

    static int getIntOption(String optionName, int defaultValue) {
        return options.getIntOption(optionName, defaultValue);
    }

    private static String getStringOption(String optionName, String defaultValue) {
        return options.getStringOption(optionName, defaultValue);
    }

    static void parseArgs(String[] args) {
        options = new ProgArgs(args);

        httpPort = options.getIntOption("port", 8080);
        numberOfServiceInstances = getIntOption("count", 1);
        String uidStr = options.getStringOption("uid", null);

        if (uidStr != null)
            uid = new Uid(uidStr);

        if (httpPort <= 0 || numberOfServiceInstances <= 0)
            throw new IllegalArgumentException("syntax: instance count and http port must be greater than zero%n");


        System.out.printf("Running %d vertx event listeners on http port %d%n", numberOfServiceInstances, httpPort);
    }

    @Override
    public void start(Future<Void> future) {
        int listenerPort = config().getInteger("port", 8080);

        serviceName = config().getString("name", "book");
        mandatory = initService(theService);

        startServer(future, listenerPort);
    }

    private void startServer(Future<Void> future, int listenerPort) {
        Router router = Router.router(vertx);

        initRoutes(router);

        // Create the HTTP server and pass the "accept" method to the request handler.
        vertx
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(listenerPort,
                        result -> {
                            if (result.succeeded()) {
                                future.complete(); // tell the caller the server is ready
                            } else {
                                result.cause().printStackTrace(System.out);
                                future.fail(result.cause()); // tell the caller that server failed to start
                            }
                        }
                );

        assert router.getRoutes().size() > 0;

        String route1 = router.getRoutes().get(0).getPath();

        System.out.printf("%s service listening on http://localhost:%d%s%n" , getServiceName(), listenerPort, route1);
    }

    void initRoutes(Router router) {
        router.route("/api/" + getServiceName() + "*").handler(BodyHandler.create());

        router.get("/api/" + getServiceName() + "/uid").handler(this::getUid);
        router.get("/api/" + getServiceName()).handler(this::getActivity);

        router.post("/api/" + getServiceName() + "/:name").handler(this::performActivity);
    }


    abstract Booking initService(Booking service);

    private void getUid(RoutingContext routingContext) {
        routingContext.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(uid.toString()));
    }

    void getActivity(RoutingContext routingContext) {
        try {
            AtomicAction A = new AtomicAction();

            A.begin();
            int activityCount = mandatory.getBookings(); // done as a sub transaction of A since mandatory is annotated wiht @Nested
            A.commit();

            System.out.printf("%s: cnt: %d%n", getServiceName(), mandatory.getBookings());
            routingContext.response()
                    .setStatusCode(201)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(new ServiceResult(getServiceName(), Thread.currentThread().getName(), activityCount)));
        } catch (Exception e) {
            routingContext.response()
                    .setStatusCode(406)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(new JsonObject().put("Status", e.getMessage()).encode());
        }
    }

    void performActivity(RoutingContext routingContext) {
        try {
            AtomicAction A = new AtomicAction();

            A.begin();
            mandatory.book(); // done as a sub transaction of A since mandatory is annotated wiht @Nested
            int activityCount = mandatory.getBookings();
            A.commit();

            routingContext.response()
                    .setStatusCode(201)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(new ServiceResult(getServiceName(), Thread.currentThread().getName(), activityCount)));
        } catch (Exception e) {
            routingContext.response()
                    .setStatusCode(406)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(new JsonObject().put("Status", e.getMessage()).encode());
        }
    }

    // workaround for JBTM-1732
    static void initializeSTMObject(Booking booking) {
        AtomicAction A = new AtomicAction();

        A.begin();
        booking.init();
        A.commit();
    }
}
