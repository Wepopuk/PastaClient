package fun.pasta.util.math;

import fun.pasta.util.IMinecraft;

public class SensitivityUtil implements IMinecraft {

    public static float getSensitivity(float rot) {
        return getDeltaMouse(rot) * getSensValue();
    }

    public static float getSensValue() {
        return getSens() * 0.15f;
    }

    public static float getSens() {
        float sens1;
        return (sens1 = (float) (mc.gameSettings.mouseSensitivity * 0.6 + 0.2f)) * sens1 * sens1 * 8;
    }

    public static float getDeltaMouse(float delta) {
        return Math.round(delta / getSensValue());
    }
}