package demo.demo12;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import com.arjuna.ats.arjuna.AtomicAction;

import demo.domain.TheatreService;
import demo.domain.ServiceResult;

public abstract class TheatreVerticle extends AbstractVerticle {
    TheatreService serviceClone;

    public TheatreVerticle() {
    }

    abstract TheatreService getClone();

    @Override
    public void start(Future<Void> future) throws Exception {
        serviceClone = getClone();

        startServer(future, config().getInteger("port"));
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

    public String getServiceName() {
        return "theatre";
    }

    void getBookings(RoutingContext routingContext) {
        try {
            AtomicAction A = new AtomicAction();

            A.begin();
            int activityCount = serviceClone.getBookings();

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

    void makeBooking(RoutingContext routingContext) {
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

    // workaround for JBTM-
    static void initSTMMemory(TheatreService service) {
        AtomicAction A = new AtomicAction();

        A.begin();
        service.init();
        A.commit();
    }
}
