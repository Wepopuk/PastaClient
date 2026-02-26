package fun.pasta.config;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigManager {

    public File dir = new File("pasta/configs");

    public ConfigManager() {
        File dir = new File(Minecraft.getInstance().gameDir, "Pasta/files/configs");
        if (!dir.exists()) dir.mkdirs(); // создаём директорию, если нет
        this.dir = dir;
    }


    public Config get(String name) {
        return new Config(new File(dir, name + ".pasta"));
    }

    public boolean exists(String name) {
        return new File(dir, name + ".pasta").exists();
    }

    public List<String> list() {
        return Arrays.stream(dir.listFiles())
                .filter(f -> f.getName().endsWith(".pasta"))
                .map(f -> f.getName().replace(".pasta", ""))
                .collect(Collectors.toList());
    }

    public void delete(String name) {
        new File(dir, name + ".pasta").delete();
    }
}
