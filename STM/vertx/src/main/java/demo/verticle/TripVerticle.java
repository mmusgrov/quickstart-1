package demo.verticle;

import com.arjuna.ats.arjuna.AtomicAction;
import demo.stm.Activity;

import demo.stm.TheatreServiceImpl;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

class TripVerticle extends BaseVerticle {
    private static int taxiServicePort = 8080;
    private HttpClient taxiClient;

    private Activity theatreService;

    static void deployVerticle(String[] args, boolean isVolatile, String verticleClassName) {
        deployVerticle(args, isVolatile, verticleClassName, new TheatreServiceImpl(), "theatre");
    }

    @Override
    Activity initService(Activity service) {
        assert service != null;

        taxiServicePort = getIntOption("taxi.port", 8080);

        taxiClient = vertx.createHttpClient();

        theatreService = container.clone(new TheatreServiceImpl(), service); // TheatreService is Nested

        return theatreService;
    }

    void initRoutes(Router router) {
        router.post(String.format("/api/%s/:name/:taxi", getServiceName())).handler(this::bookTrip);

        super.initRoutes(router);
    }

    private void bookTrip(RoutingContext routingContext) {
        String showName =  routingContext.request().getParam("name");
        String taxiName =  routingContext.request().getParam("taxi");

        try {
            AtomicAction A = new AtomicAction();

            A.begin();
            theatreService.activity(); // done as a sub transaction of A since mandatory is annotated wiht @Nested
            int activityCount = theatreService.getValue();
            // book the taxi too
            bookTaxi(taxiClient, taxiServicePort);
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

    private void bookTaxi(HttpClient taxiClient, int taxiServicePort) {
        taxiClient.post(taxiServicePort, "localhost", "/api/taxi/1")
                .exceptionHandler(e -> System.out.printf("Taxi booking request failed: " + e.getLocalizedMessage()))
                .handler(h -> System.out.printf("Taxi booking request ok"))
                .end();
    }
}
