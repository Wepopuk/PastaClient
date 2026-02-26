package fun.pasta.settings;

import fun.pasta.settings.Setting;

public class TextSetting extends Setting {

    public String value;
    public boolean focused = false;

    public TextSetting(String name, String defaultValue) {
        super(name); // ← добавили вызов конструктора родителя
        this.value = defaultValue;
    }

    public String get() {
        return value;
    }

    public void set(String text) {
        this.value = text;
    }
}
