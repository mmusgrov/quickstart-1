package demo.verticle;

import demo.domain.Booking;

import demo.domain.ServiceResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class TripVerticle extends BaseVerticle {
    private static int tripServicePort = 8080;
    private static int taxiServicePort = 8082;
    private static int theatreServicePort = 8084;

    private HttpClient httpClient;

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
    Booking initService(Booking service) {
        int idleTimeoutSecs = 1;  // TimeUnit.SECONDS
        int connectTimeoutMillis = 1000; // TimeUnit.MILLISECONDS

        httpClient = vertx.createHttpClient(
                new HttpClientOptions().setIdleTimeout(idleTimeoutSecs)
                        .setConnectTimeout(connectTimeoutMillis));;

        return null;
    }

    void initRoutes(Router router) {
        router.post(String.format("/api/%s/:name/:taxi", getServiceName())).handler(this::bookTrip);

        super.initRoutes(router);
    }

    private void bookTrip(RoutingContext routingContext) {
        String showName =  routingContext.request().getParam("name");
        String taxiName =  routingContext.request().getParam("taxi");

        Future<String> theatreFuture = Future.future();
        Future<String> taxiFuture = Future.future();

        try {
            bookTheatre(theatreFuture, httpClient, theatreServicePort, showName);
            bookTaxi(taxiFuture, httpClient, taxiServicePort, taxiName);

            CompositeFuture.all(theatreFuture, taxiFuture).setHandler(result -> {
                int status = result.succeeded() ? 201 : 500;
                String msg = result.failed() ? result.cause().getMessage() : "";

                routingContext.response()
                        .setStatusCode(status)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(new ServiceResult(getServiceName(),
                                Thread.currentThread().getName(), msg, 0, 0)));
            });
        } catch (Exception e) {
            routingContext.response()
                    .setStatusCode(406)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(new JsonObject().put("Status", e.getMessage()).encode());
        }
    }

    private void bookTheatre(Future<String> result, HttpClient client, int servicePort, String name) {
        httpClient.post(servicePort, "localhost", "/api/theatre/1")
                .exceptionHandler(e -> result.fail("Theatre booking request failed: " + e.getLocalizedMessage()))
                .handler(h -> result.complete("Theatre booked"))
                .end();
    }

    private void bookTaxi(Future<String> result, HttpClient client, int servicePort, String name) {
        httpClient.post(servicePort, "localhost", "/api/taxi/1")
                .exceptionHandler(e -> result.fail("Taxi booking request failed: " + e.getLocalizedMessage()))
                .handler(h -> result.complete("Taxi booked"))
                .end();
    }
}
