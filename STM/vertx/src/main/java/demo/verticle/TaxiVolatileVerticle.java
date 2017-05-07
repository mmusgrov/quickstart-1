package demo.verticle;

public class TaxiVolatileVerticle extends TaxiVerticle {
    public static void main(String[] args) {
        deployVerticle(args, true, TaxiVolatileVerticle.class.getName());
    }
}
