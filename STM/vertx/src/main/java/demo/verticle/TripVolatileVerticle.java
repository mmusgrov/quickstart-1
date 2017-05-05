package demo.verticle;

public class TripVolatileVerticle extends TripVerticle {
    public static void main(String[] args) {
        deployVerticle(args, true, TripVolatileVerticle.class.getName());
    }
}
