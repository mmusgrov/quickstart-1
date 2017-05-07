package demo.verticle;

public class TheatreNonVolatileVerticle extends TheatreVerticle {
    public static void main(String[] args) {
        deployVerticle(args, false, TheatreNonVolatileVerticle.class.getName());
    }
}
