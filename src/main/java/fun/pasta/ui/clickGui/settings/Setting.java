package fun.pasta.ui.clickGui.settings;

import java.util.function.Supplier;

public abstract class Setting {

    public String name;

    // ← ВАЖНО: ВОЗВРАЩАЕМ visible
    public Supplier<Boolean> visible = () -> true;

    public Setting(String name) {
        this.name = name;
    }

    public boolean visible() {
        return visible.get();
    }
    public String getName() {
        return name;
    }
    public abstract String getValueAsString();
    public abstract void setValueFromString(String value);
}
