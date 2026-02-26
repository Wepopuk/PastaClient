package fun.pasta.util.color;

public class ColorUtil {

    public static int getRed(final int hex) {
        return hex >> 16 & 255;
    }

    public static int getGreen(final int hex) {
        return hex >> 8 & 255;
    }

    public static int getBlue(final int hex) {
        return hex & 255;
    }

    public static int rgba(int r, int g, int b, int a) {return a << 24 | r << 16 | g << 8 | b;}

    public static int rgba(int r, int g, int b) {return 255 << 24 | r << 16 | g << 8 | b;}

    public static int getAlpha(final int hex) {
        return hex >> 24 & 255;
    }

    public static int defaultRed = ColorUtil.rgba(255, 72, 72);

    public static int defaultRed(int alpha) {
        return ColorUtil.rgba(255, 72, 72, alpha);
    }

    public static float[] rgba(int color) {
        return new float[] {
                (color >> 16 & 0xFF) / 255f,
                (color >> 8 & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                (color >> 24 & 0xFF) / 255f
        };
    }

    public static int interpolateColor(int color1, int color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));

        int red1 = getRed(color1);
        int green1 = getGreen(color1);
        int blue1 = getBlue(color1);
        int alpha1 = getAlpha(color1);

        int red2 = getRed(color2);
        int green2 = getGreen(color2);
        int blue2 = getBlue(color2);
        int alpha2 = getAlpha(color2);

        int interpolatedRed = interpolateInt(red1, red2, amount);
        int interpolatedGreen = interpolateInt(green1, green2, amount);
        int interpolatedBlue = interpolateInt(blue1, blue2, amount);
        int interpolatedAlpha = interpolateInt(alpha1, alpha2, amount);

        return (interpolatedAlpha << 24) | (interpolatedRed << 16) | (interpolatedGreen << 8) | interpolatedBlue;
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return (int) (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static int interpolate(int start, int end, float value) {
        float[] startColor = rgba(start);
        float[] endColor = rgba(end);

        return rgba((int) interpolate(startColor[0] * 255, endColor[0] * 255, value),
                (int) interpolate(startColor[1] * 255, endColor[1] * 255, value),
                (int) interpolate(startColor[2] * 255, endColor[2] * 255, value),
                (int) interpolate(startColor[3] * 255, endColor[3] * 255, value));
    }

    private static double interpolate(double current, double old, double scale) {
        return old + (current - old) * scale;
    }
}