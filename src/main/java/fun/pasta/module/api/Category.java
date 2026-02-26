package fun.pasta.module.api;

public enum Category {

    COMBAT("Combat", "A"),
    MOVE("Movement", "B"),
    VISUAL("Visual", "C"),
    PLAYER("Player", "D"),
    MISC("Misc", "E");

    public final String name;
    public final String icon;

    Category(String name, String icon) {
        this.name = name;
        this.icon = icon;
    }
}
