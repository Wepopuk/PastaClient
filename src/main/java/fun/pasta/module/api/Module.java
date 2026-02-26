package fun.pasta.module.api;

import fun.pasta.Pasta;
import fun.pasta.ui.clickGui.settings.Setting;
import fun.pasta.util.IMinecraft;
import fun.pasta.util.NotificationManager;

import java.util.ArrayList;
import java.util.Arrays;


public class Module implements IMinecraft {

    public String name;
    public int key;
    public boolean enable;
    public Category category;

    public String getName() {
        return name;
    }

    public float animAlpha;

    public ArrayList<Setting> settingList = new ArrayList<>();

    public Module(String name, Category category) {
        this.name = name;
        this.key = 0;
        this.category = category;
        this.enable = false;
    }

    public final void add(Setting... options) {
        settingList.addAll(Arrays.asList(options));
    }

    public void onEnable() {
        enable = true;
        Pasta.instance.eventBus.register(this);
    }

    public void onDisable() {
        enable = false;
        Pasta.instance.eventBus.unregister(this);
    }

    public void toggle() {
        if (enable) {
            onDisable();
            NotificationManager.push(name + " Disabled", false, 2500L);
        } else {
            onEnable();
            NotificationManager.push(name + " Enabled", true, 2500L);
        }
    }
}
