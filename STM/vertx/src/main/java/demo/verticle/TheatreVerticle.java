package demo.verticle;

import demo.stm.Activity;
import demo.stm.TaxiServiceImpl;
import demo.stm.TheatreServiceImpl;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class TheatreVerticle extends BaseVerticle {
    static void deployVerticle(String[] args, boolean isVolatile, String verticleClassName) {
        deployVerticle(args, isVolatile, verticleClassName, new TheatreServiceImpl(), "theatre");
    }

    Activity initService(Activity service) {
        assert service != null;

        return container.clone(new TaxiServiceImpl(), service);
    }

    void initRoutes(Router router) {
        super.initRoutes(router);

        router.get(String.format("/api/%s", getServiceName())).handler(this::listBookings);
        router.post(String.format("/api/%s/:seats", getServiceName())).handler(this::bookTaxi);
    }

    private void listBookings(RoutingContext routingContext) {
        super.getActivity(routingContext);
    }

    private void bookTaxi(RoutingContext routingContext) {
        super.performActivity(routingContext);
    }
}
