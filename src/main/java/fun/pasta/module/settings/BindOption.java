package fun.pasta.module.settings;

import fun.pasta.ui.clickGui.settings.Setting;
import java.util.function.Supplier;

public class BindOption extends Setting {

    public int key;

    public BindOption(String name, int key) {
        super(name);
        this.key = key;
    }

    public BindOption setVisible(Supplier<Boolean> bool) {
        visible = bool;
        return this;
    }

    @Override
    public String getValueAsString() {
        return String.valueOf(key);
    }

    @Override
    public void setValueFromString(String value) {
        key = Integer.parseInt(value);
    }

}
