package demo.verticle;

public class TripNonVolatileVerticle extends TripVerticle {
    public static void main(String[] args) {
        TripVerticle.main(args);
//        deployVerticle(args, false, TripNonVolatileVerticle.class.getName());
    }
}