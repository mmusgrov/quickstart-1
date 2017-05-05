package demo.verticle;

public class TheatreVolatileVerticle extends TheatreVerticle {
    public static void main(String[] args) {
        deployVerticle(args, true, TheatreVolatileVerticle.class.getName());
    }
}
