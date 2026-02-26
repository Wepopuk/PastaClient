package fun.pasta.settings;

public class NumberSetting extends Setting {

    private double value, min, max, increment;

    public NumberSetting(String name, double value, double min, double max, double increment) {
        super(name);
        this.value = value;
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        if (value < min) value = min;
        if (value > max) value = max;
        this.value = Math.round(value / increment) * increment;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getIncrement() {
        return increment;
    }
}
