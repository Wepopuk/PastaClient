package fun.pasta.module.settings;

import fun.pasta.ui.clickGui.settings.Setting;
import fun.pasta.util.IMinecraft;

import java.util.function.Supplier;

public class BooleanOption extends Setting implements IMinecraft {

    private boolean value;

    public BooleanOption(String name, boolean value) {
        super(name);
        this.value = value;
    }

    public boolean get() {
        return value;
    }

    public void toggle() {
        value = !value;
    }

    public void set(boolean value) {
        this.value = value;
    }

    public BooleanOption setVisible(Supplier<Boolean> bool) {
        visible = bool;
        return this;
    }

    @Override
    public String getValueAsString() {
        return String.valueOf(value);
    }

    @Override
    public void setValueFromString(String value) {
        this.value = Boolean.parseBoolean(value);
    }

}