package demo.demo2;

import com.arjuna.ats.arjuna.AtomicAction;
import demo.stm.Activity;
import demo.stm.TaxiService;
import demo.stm.TaxiServiceImpl;
import demo.stm.TheatreService;
import demo.stm.TheatreServiceImpl;
import demo.verticle.ServiceResult;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.jboss.stm.Container;

public class TripSTMVerticle extends AbstractVerticle {
    private static ProgArgs options;

    private static Container<TheatreService> theatreContainer;
    private static Container<TaxiService> taxiContainer;

    private static TaxiService taxiService;
    private static TaxiService altTaxiService;
    private static TheatreService theatreService;

    private Activity theatreServiceClone;
    private TaxiService taxiServiceClone;
    private TaxiService altTaxiServiceClone;

    private static int tripServicePort = 8080;

    public static void main(String[] args) {
        options = new ProgArgs(args);

        tripServicePort = options.getIntOption("trip.port", tripServicePort);

        theatreContainer = new Container<>(Container.TYPE.RECOVERABLE, Container.MODEL.EXCLUSIVE);
        taxiContainer = new Container<>(Container.TYPE.RECOVERABLE, Container.MODEL.EXCLUSIVE);

        theatreService = theatreContainer.create(new TheatreServiceImpl());
        taxiService = taxiContainer.create(new TaxiServiceImpl("ABC"));
        altTaxiService = taxiContainer.create(new TaxiServiceImpl("Coast"));

        initSTMMemory(theatreService);
        initSTMMemory(taxiService);
        initSTMMemory(altTaxiService);

        Vertx vertx = Vertx.vertx();

        DeploymentOptions opts = new DeploymentOptions()
                .setInstances(options.getIntOption("parallelism", 10))
                .setConfig(new JsonObject()
                        .put("name", "trip")
                        .put("trip.port", tripServicePort));

        vertx.deployVerticle(TripSTMVerticle.class.getName(), opts);
    }

    public String getServiceName() {
        return "trip";
    }

    @Override
    public void start(Future<Void> future) throws Exception {
        theatreServiceClone = theatreContainer.clone(new TheatreServiceImpl(), theatreService);
        taxiServiceClone = taxiContainer.clone(new TaxiServiceImpl(), taxiService);
        altTaxiServiceClone = taxiContainer.clone(new TaxiServiceImpl(), taxiService);

        startServer(future, config().getInteger("trip.port"));
    }

    void getRoutes(Router router) {
        router.post(String.format("/api/%s/:name/:taxi", getServiceName())).handler(this::bookTrip);

        router.get(String.format("/api/%s/theatre", getServiceName())).handler(this::listTheatreBookings);
        router.get(String.format("/api/%s/taxi", getServiceName())).handler(this::listTaxiBookings);
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

    private void listTaxiBookings(RoutingContext routingContext) {
        listBookings(routingContext, taxiServiceClone, "taxi");
    }

    private void listTheatreBookings(RoutingContext routingContext) {
        listBookings(routingContext, theatreServiceClone, "theatre");
    }

    private void listBookings(RoutingContext routingContext, Activity activity, String serviceName) {
        try {
            AtomicAction A = new AtomicAction();

            A.begin();
            int activityCount = activity.getValue();
            A.commit();

            routingContext.response()
                    .setStatusCode(201)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(new ServiceResult(serviceName, Thread.currentThread().getName(), activityCount)));
        } catch (Exception e) {
            routingContext.response()
                    .setStatusCode(406)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(new JsonObject().put("Status", e.getMessage()).encode());
        }
    }

    private void bookTrip(RoutingContext routingContext) {
        String showName =  routingContext.request().getParam("name");
        String taxiName =  routingContext.request().getParam("taxi");

        try {
            int theatreBookings;
            int taxiBookings;
            int altTaxiBookings;

            AtomicAction A = new AtomicAction();

            A.begin();
            theatreServiceClone.activity(); // done as a sub transaction of A since mandatory is annotated wiht @Nested
            try {
                taxiServiceClone.failingActivity();
            } catch (Exception e) {
                altTaxiServiceClone.activity();
            }

            theatreBookings = theatreServiceClone.getValue();
            taxiBookings = taxiServiceClone.getValue();
            altTaxiBookings = altTaxiServiceClone.getValue();
            A.commit();

            String res = String.format("%d bookings with alt taxi service", altTaxiBookings);

            routingContext.response()
                    .setStatusCode(201)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(new ServiceResult(getServiceName(), Thread.currentThread().getName(), res, theatreBookings, taxiBookings)));
        } catch (Exception e) {
            routingContext.response()
                    .setStatusCode(406)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(new JsonObject().put("Status", e.getMessage()).encode());
        }
    }

    static void initSTMMemory(Activity service) {
        AtomicAction A = new AtomicAction();

        A.begin();
        service.init();
        A.commit();
    }
}
