package demo.verticle;

import demo.stm.Activity;
import demo.stm.TaxiService;
import demo.stm.TaxiServiceImpl;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.jboss.stm.Container;

public class TaxiVolatileVerticle extends TaxiVerticle {
    public static void main(String[] args) {
        deployVerticle(args, true, TaxiVolatileVerticle.class.getName());
    }
}
