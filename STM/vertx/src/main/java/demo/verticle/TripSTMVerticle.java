package demo.verticle;

import com.arjuna.ats.arjuna.AtomicAction;
import demo.stm.Activity;
import demo.stm.TaxiService;
import demo.stm.TaxiServiceImpl;
import demo.stm.TheatreServiceImpl;
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

    private static Container<Activity> container;
    private static Container<TaxiService> taxiContainer;

    private static TaxiService taxiService;
    private static TaxiService altTaxiService;
    private static Activity theatreService;

    private Activity theatreServiceClone;
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
    Activity initService(Activity service) {
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

    private void listBookings(RoutingContext routingContext, Activity activity) {
        try {
            AtomicAction A = new AtomicAction();

            A.begin();
            int activityCount = activity.getValue();
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
            theatreServiceClone.activity(); // done as a sub transaction of A since mandatory is annotated wiht @Nested
            try {
                taxiServiceClone.failingActivity();
            } catch (Exception e) {
                altTaxiServiceClone.activity();
            }

            theatreBookings = theatreService.getValue();
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
}
