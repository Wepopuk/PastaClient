package fun.pasta.util.anim;

public final class Easings {
    private Easings() {
    }

    public static final Easing LINEAR = x -> x;
    public static final Easing QUAD_IN = x -> x * x;
    public static final Easing QUAD_OUT = x -> 1 - (1 - x) * (1 - x);
    public static final Easing QUINT_OUT = x -> 1 - Math.pow(1 - x, 5);
}
