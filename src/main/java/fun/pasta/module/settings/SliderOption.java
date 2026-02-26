package fun.pasta.module.settings;

import java.util.function.Supplier;

import fun.pasta.ui.clickGui.settings.Setting;
import fun.pasta.util.IMinecraft;
import net.minecraft.util.math.MathHelper;

public class SliderOption extends Setting implements IMinecraft {

    public float value;
    public final float max;
    public final float min;
    public final float increment;

    public SliderOption(String name, float value, float min, float max, float increment) {
        super(name);
        this.value = value;
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public SliderOption setVisible(Supplier<Boolean> bool) {
        visible = bool;
        return this;
    }

    public Number getValue() {
        return MathHelper.clamp(value, min, max);
    }

    public void setValue(float value) {
        this.value = MathHelper.clamp(value, min, max);
    }

    @Override
    public String getValueAsString() {
        return String.valueOf(value);
    }

    @Override
    public void setValueFromString(String value) {
        setValue(Float.parseFloat(value));
    }

}