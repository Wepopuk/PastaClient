package fun.pasta.module.settings;

import fun.pasta.ui.clickGui.settings.Setting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MultiBoxOption extends Setting {

    public List<BooleanOption> options;
    public int index;

    public MultiBoxOption(String name, BooleanOption... options) {
        super(name);
        this.options = List.of(options);
    }

    public boolean get(String name) {
        return Objects.requireNonNull(this.options.stream().filter((option) -> option.name.equalsIgnoreCase(name)).findFirst().orElse(null)).get();
    }

    public void set(String name, boolean value) {
        Objects.requireNonNull(this.options.stream().filter((option) -> option.name .equalsIgnoreCase(name)).findFirst().orElse(null)).set(value);
    }

    public String get() {
        List<String> includedOptions = new ArrayList<>();
        for (BooleanOption option : options) {
            if (option.get()) {
                includedOptions.add(option.name);
            }
        }
        return String.join(", ", includedOptions);
    }

    public void set(int index, boolean value) {
        this.options.get(index).set(value);
    }

    public boolean get(int index) {
        return this.options.get(index).get();
    }

    public MultiBoxOption setVisible(Supplier<Boolean> bool) {
        this.visible = bool;
        return this;
    }

    @Override
    public String getValueAsString() {
        return get();
    }

    @Override
    public void setValueFromString(String value) {
        for (BooleanOption option : options) {
            option.set(false);
        }

        if (value.isEmpty()) return;

        String[] split = value.split(",");
        for (String s : split) {
            for (BooleanOption option : options) {
                if (option.name.equalsIgnoreCase(s.trim())) {
                    option.set(true);
                }
            }
        }
    }

}