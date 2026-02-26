package fun.pasta.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MainWindow;

public interface IMinecraft {

    Minecraft mc = initMc();
    MainWindow sr = initSr();

    static Minecraft initMc() {
        Minecraft instance = Minecraft.getInstance();
        if (instance == null) {
            throw new IllegalStateException("Minecraft еще не инициализирован!");
        }
        return instance;
    }
    static MainWindow initSr() {
        Minecraft instance = Minecraft.getInstance();
        if (instance == null) {
            throw new IllegalStateException("Minecraft еще не инициализирован!");
        }
        return instance.getMainWindow();
    }
}
