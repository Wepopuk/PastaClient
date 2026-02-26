package fun.pasta.module.settings;

import fun.pasta.ui.clickGui.settings.Setting;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class ModeOption extends Setting {

    public List<String> modes;
    public String currentMode;
    public int index;

    public ModeOption(String name, String currentMode, String... modes) {
        super(name);
        this.modes = Arrays.asList(modes);
        this.currentMode = currentMode;
        this.index = this.modes.indexOf(currentMode);
    }

    public String get() {
        return currentMode;
    }

    public void set(String mode) {
        this.currentMode = mode;
        this.index = modes.indexOf(mode);
    }

    public void cycle() {
        index++;
        if (index >= modes.size()) {
            index = 0;
        }
        currentMode = modes.get(index);
    }

    public ModeOption setVisible(Supplier<Boolean> bool) {
        this.visible = bool;
        return this;
    }

    @Override
    public String getValueAsString() {
        return currentMode;
    }

    @Override
    public void setValueFromString(String value) {
        if (modes.contains(value)) {
            set(value);
        }
    }
}
