package demo.verticle;

import com.arjuna.ats.arjuna.AtomicAction;
import demo.domain.Booking;
import demo.domain.ServiceResult;
import demo.domain.TaxiService;
import demo.domain.TaxiServiceImpl;
import demo.domain.TheatreServiceImpl;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.jboss.stm.Container;

public class TripSTMVerticle extends BaseVerticle {
    private static int tripServicePort = 8080;
    private static int taxiServicePort = 8082;
    private static int theatreServicePort = 8084;

    private HttpClient httpClient;

    private static Container<Booking> container;
    private static Container<TaxiService> taxiContainer;

    private static TaxiService taxiService;
    private static TaxiService altTaxiService;
    private static Booking theatreService;

    private Booking theatreServiceClone;
    private TaxiService taxiServiceClone;
    private TaxiService altTaxiServiceClone;

    public static void main(String[] args) {
        parseArgs(args);
        Vertx vertx = Vertx.vertx();

        DeploymentOptions opts = new DeploymentOptions()
                .setInstances(getNumberOfServiceInstances())
                .setConfig(new JsonObject()
                        .put("name", "trip")
                        .put("port", getIntOption("port", tripServicePort)));

        container = new Container<>(Container.TYPE.RECOVERABLE, Container.MODEL.EXCLUSIVE);
        taxiContainer = new Container<>(Container.TYPE.RECOVERABLE, Container.MODEL.EXCLUSIVE);

        theatreService = container.create(new TheatreServiceImpl());
        taxiService = taxiContainer.create(new TaxiServiceImpl("ABC"));
        altTaxiService = taxiContainer.create(new TaxiServiceImpl("Coast"));

        initializeSTMObject(theatreService);
        initializeSTMObject(taxiService);
        initializeSTMObject(altTaxiService);

        vertx.deployVerticle(TripSTMVerticle.class.getName(), opts);
    }

    @Override
    Booking initService(Booking service) {
        httpClient = vertx.createHttpClient();

        theatreServiceClone = container.clone(new TheatreServiceImpl(), theatreService);
        taxiServiceClone = taxiContainer.clone(new TaxiServiceImpl(), taxiService);
        altTaxiServiceClone = taxiContainer.clone(new TaxiServiceImpl(), taxiService);

        return null;
    }

    void initRoutes(Router router) {
        router.post(String.format("/api/%s/:name/:taxi", getServiceName())).handler(this::bookTrip);
        router.get(String.format("/api/%s/theatre", getServiceName())).handler(this::listTheatreBookings);
        router.get(String.format("/api/%s/taxi", getServiceName())).handler(this::listTaxiBookings);

        super.initRoutes(router);
    }

    private void listTaxiBookings(RoutingContext routingContext) {
        listBookings(routingContext, taxiServiceClone);
    }

    private void listTheatreBookings(RoutingContext routingContext) {
        listBookings(routingContext, theatreServiceClone);
    }

    private void listBookings(RoutingContext routingContext, Booking booking) {
        try {
            AtomicAction A = new AtomicAction();

            A.begin();
            int activityCount = booking.getBookings();
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

    private void bookTrip(RoutingContext routingContext) {
        String showName =  routingContext.request().getParam("name");
        String taxiName =  routingContext.request().getParam("taxi");

        try {
            int theatreBookings;
            int taxiBookings;
            int altTaxiBookings;

            AtomicAction A = new AtomicAction();

            A.begin();
            theatreServiceClone.book(); // done as a sub transaction of A since mandatory is annotated wiht @Nested
            try {
                taxiServiceClone.failingActivity();
            } catch (Exception e) {
                altTaxiServiceClone.book();
            }

            theatreBookings = theatreService.getBookings();
            taxiBookings = taxiServiceClone.getBookings();
            altTaxiBookings = altTaxiServiceClone.getBookings();
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
}
