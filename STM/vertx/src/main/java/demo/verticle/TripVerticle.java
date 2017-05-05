package demo.verticle;

import com.arjuna.ats.arjuna.AtomicAction;
import demo.stm.Activity;

import demo.stm.TheatreServiceImpl;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class TripVerticle extends BaseVerticle {
    private static int tripServicePort = 8080;
    private static int taxiServicePort = 8082;
    private static int theatreServicePort = 8084;

    private HttpClient httpClient;

    private Activity taxService;
    private Activity theatreService;

    public static void main(String[] args) {
        parseArgs(args);
        Vertx vertx = Vertx.vertx();

        DeploymentOptions opts = new DeploymentOptions()
                .setInstances(getNumberOfServiceInstances())
                .setConfig(new JsonObject()
                        .put("name", "trip")
                        .put("port", getIntOption("port", tripServicePort)));

        taxiServicePort = getIntOption("taxi.port", taxiServicePort);
        theatreServicePort = getIntOption("theatre.port", taxiServicePort);

        vertx.deployVerticle(TripVerticle.class.getName(), opts);
    }

    @Override
    Activity initService(Activity service) {
        httpClient = vertx.createHttpClient();

        return null;
    }

    void initRoutes(Router router) {
        router.post(String.format("/api/%s/:name/:taxi", getServiceName())).handler(this::bookTrip);

        super.initRoutes(router);
    }

    private void bookTrip(RoutingContext routingContext) {
        String showName =  routingContext.request().getParam("name");
        String taxiName =  routingContext.request().getParam("taxi");

        try {
            bookTheatre(httpClient, theatreServicePort);
            bookTaxi(httpClient, taxiServicePort);

            int activityCount = 0;

/*            AtomicAction A = new AtomicAction();

            A.begin();
            theatreService.activity(); // done as a sub transaction of A since mandatory is annotated wiht @Nested
            int activityCount = theatreService.getValue();
            // book the taxi too
            bookTaxi(httpClient, taxiServicePort);
            A.commit();*/

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

    private void bookTheatre(HttpClient client, int servicePort) {
        httpClient.post(servicePort, "localhost", "/api/theatre/1")
                .exceptionHandler(e -> System.out.printf("Theatre booking request failed: " + e.getLocalizedMessage()))
                .handler(h -> System.out.printf("Taxi booking request ok"))
                .end();
    }

    private void bookTaxi(HttpClient client, int servicePort) {
        httpClient.post(servicePort, "localhost", "/api/taxi/1")
                .exceptionHandler(e -> System.out.printf("Taxi booking request failed: " + e.getLocalizedMessage()))
                .handler(h -> System.out.printf("Taxi booking request ok"))
                .end();
    }
}
