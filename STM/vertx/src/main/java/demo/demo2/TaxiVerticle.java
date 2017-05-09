package demo.demo2;

import com.arjuna.ats.arjuna.AtomicAction;
import demo.domain.TaxiService;
import demo.domain.TaxiServiceImpl;
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
import org.jboss.stm.Container;

public class TaxiVerticle extends AbstractVerticle {
    private static ProgArgs options;
    private static int port = 8084;

    private TaxiService serviceClone;
    private static TaxiService service;
    private static Container<TaxiService> container;

    public static void main(String[] args) {
        options = new ProgArgs(args);
        port = options.getIntOption("taxi.port", port);
        container = new Container<>(Container.TYPE.RECOVERABLE, Container.MODEL.EXCLUSIVE);

        service = container.create(new TaxiServiceImpl());
        initSTMMemory(service);

        DeploymentOptions opts = new DeploymentOptions().
                setInstances(10).
                setConfig(new JsonObject().put("name", "demo1").put("taxi.port", port));

        Vertx.vertx().deployVerticle(TaxiVerticle.class.getName(), opts);

        /*
        Make theatre bookings: curl -X POST http://localhost:8080/api/theatre/Odeon
        Count theatre bookings: curl -X GET http://localhost:8080/api/theatre
         */
    }

    public TaxiVerticle() {
    }

    TaxiService getClone() {
        return container.clone(new TaxiServiceImpl(), service);
    }

    @Override
    public void start(Future<Void> future) throws Exception {
        serviceClone = getClone();

        startServer(future, config().getInteger("taxi.port"));
    }

    private void startServer(Future<Void> future, int listenerPort) {
        Router router = Router.router(vertx);

        getRoutes(router);

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

    void getRoutes(Router router) {
        router.get(String.format("/api/%s", getServiceName())).handler(this::getBookings);
        router.post(String.format("/api/%s/:seats", getServiceName())).handler(this::makeBooking);
    }

    private void getBookings(RoutingContext routingContext) {
        getActivity(routingContext);
    }

    private void makeBooking(RoutingContext routingContext) {
        performActivity(routingContext);
    }

    public String getServiceName() {
        return "taxi";
    }

    void getActivity(RoutingContext routingContext) {
        try {
            AtomicAction A = new AtomicAction();

            A.begin();
            int activityCount = serviceClone.getBookings(); // done as a sub transaction of A since mandatory is annotated wiht @Nested
            A.commit();

            System.out.printf("%s: cnt: %d%n", getServiceName(), serviceClone.getBookings());
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
            serviceClone.book(); // done as a sub transaction of A since mandatory is annotated wiht @Nested
            int activityCount = serviceClone.getBookings();
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

    static void initSTMMemory(TaxiService service) {
        AtomicAction A = new AtomicAction();

        A.begin();
        service.init();
        A.commit();
    }
}
