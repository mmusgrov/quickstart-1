package demo.verticle;

public class TripNonVolatileVerticle extends TripVerticle {
    public static void main(String[] args) {
        deployVerticle(args, false, TripNonVolatileVerticle.class.getName());
    }
}