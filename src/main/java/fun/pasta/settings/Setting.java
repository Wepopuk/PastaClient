package fun.pasta.settings;

public abstract class Setting {

    protected String name;

    public Setting(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
